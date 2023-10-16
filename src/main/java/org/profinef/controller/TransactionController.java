package org.profinef.controller;

import jakarta.servlet.http.HttpSession;
import org.profinef.dto.TransactionDto;
import org.profinef.entity.*;
import org.profinef.entity.Currency;
import org.profinef.service.AccountManager;
import org.profinef.service.ClientManager;
import org.profinef.service.CurrencyManager;
import org.profinef.service.TransManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.ZoneId.systemDefault;
import static java.util.Collections.reverse;

@SuppressWarnings({"SameReturnValue", "UnusedReturnValue"})
@Controller
public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    @Autowired
    private final AccountManager accountManager;
    @Autowired
    private final CurrencyManager currencyManager;
    @Autowired
    private final TransManager transManager;
    @Autowired
    private final ClientManager clientManager;

    public TransactionController(AccountManager accountManager, CurrencyManager currencyManager, TransManager transManager, ClientManager clientManager) {
        this.accountManager = accountManager;
        this.currencyManager = currencyManager;
        this.transManager = transManager;
        this.clientManager = clientManager;
    }

    @GetMapping("/add_transaction")
    public String addTransaction(HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading add transaction page...");
        session.removeAttribute("transaction");
        List<Account> clients = accountManager.getAllAccounts();
        session.setAttribute("clients", clients);
        List<Currency> currencies = currencyManager.getAllCurrencies();
        List<Transaction> transaction = new ArrayList<>();
        Transaction tr = new Transaction();
        tr.setClient(new Client(""));
        tr.setCurrency(new Currency());
        tr.setRate(1.0);
        tr.setCommission(0.0);
        tr.setTransportation(0.0);
        transaction.add(tr);
        session.setAttribute("transaction", transaction);
        session.setAttribute("path", "/add_transaction");
        session.setAttribute("currencies", currencies);
        logger.info(user + " Add transaction page loaded");
        return "addTransaction";
    }

    /**
     * Generate page for editing transaction
     *
     * @param transactionId id of transaction for editing
     * @param session       collect parameters for view
     */
    @GetMapping(path = "/edit")
    public String editTransactionPage(@RequestParam(name = "transaction_id", required = false, defaultValue = "0") int transactionId,
                                      HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading edit transaction page...");
        if (transactionId == 0) {
            logger.debug("transactionId == 0");
            if (session.getAttribute("transaction") == null) {
                logger.debug("Transaction is no set");
                logger.info(user + " Redirecting to the main page");
                return "redirect:/client";
            }
            transactionId = ((List<Transaction>) session.getAttribute("transaction")).get(0).getId();
            logger.debug("clientId had get from session");
            logger.trace("transactionId = " + transactionId);
        }
        List<Account> clients = accountManager.getAllAccounts();
        session.setAttribute("clients", clients);
        List<Transaction> transaction = transManager.getTransaction(transactionId);
        List<Currency> currencies = currencyManager.getAllCurrencies();
        session.setAttribute("currencies", currencies);
        session.setAttribute("transaction", transaction);
        session.setAttribute("path", "/edit");
        logger.info(user + " Edit transaction page loaded");
        return "addTransaction";
    }

    /**
     * Edit transaction in database
     *
     * @param transactionId  ids of transactions that need to be editing
     * @param clientName     names of client which transaction need to be editing
     * @param comment        new comments to save to transaction
     * @param currencyName   new currencies names to save to transaction
     * @param session        collect parameters for view
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PostMapping(path = "/edit_transaction")
    public String editTransaction(@RequestParam(name = "transaction_id") List<Integer> transactionId,
                                  @RequestParam(name = "client_name") List<String> clientName,
                                  @RequestParam(name = "comment") List<String> comment,
                                  @RequestParam(name = "currency_name") List<String> currencyName,
                                  @RequestParam(name = "rate") List<String> rateS,
                                  @RequestParam(name = "commission") List<String> commissionS,
                                  @RequestParam(name = "positiveAmount") List<String> positiveAmountS,
                                  @RequestParam(name = "negativeAmount") List<String> negativeAmountS,
                                  @RequestParam(name = "transportation") List<String> transportationS,
                                  @RequestParam(name = "pointer", required = false) String pointer,
                                  HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Saving transaction after editing...");

        List<Double> rate = new ArrayList<>();
        for (String s : rateS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                rate.add(Double.valueOf(s));
            } else {
                rate.add(null);
            }
        }
        List<Double> commission = new ArrayList<>();
        for (String s : commissionS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                commission.add(Double.valueOf(s));
            } else {
                commission.add(null);
            }
        }
        List<Double> positiveAmount = new ArrayList<>();
        for (String s : positiveAmountS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                positiveAmount.add(Double.valueOf(s));
            } else {
                positiveAmount.add(null);
            }
        }
        List<Double> negativeAmount = new ArrayList<>();
        for (String s : negativeAmountS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                negativeAmount.add(Double.valueOf(s));
            } else {
                negativeAmount.add(null);
            }
        }
        List<Double> transportation = new ArrayList<>();
        for (String s : transportationS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                transportation.add(Double.valueOf(s));
            } else {
                transportation.add(null);
            }
        }
        List<Double> amount = new ArrayList<>();
        int size = positiveAmount.size();
        if (size < negativeAmount.size()) size = negativeAmount.size();
        if (size < 1) {
            logger.info(user + " Redirecting to error page with error: Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
            session.setAttribute("error", "Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
            return "error";
        }
        List<String> clientDouble = new ArrayList<>();
        List<String> currencyDouble = new ArrayList<>();
        for (int i = 0; i < clientName.size(); i++) {
            if (!clientDouble.contains(clientName.get(i))) {
                clientDouble.add(clientName.get(i));
                currencyDouble.add(currencyName.get(i));
            } else if (currencyDouble.contains(currencyName.get(i))) {
                session.setAttribute("error", "Обнаружено наличие как минимум двух позиций с совпадением " +
                        "значений клиента и валюты. Подобная операция не допустима");
                logger.info(user + " Redirecting to error page with error: Обнаружено наличие как минимум двух позиций " +
                        "с совпадением значений клиента и валюты. Подобная операция не допустима");
                return "error";
            }
        }
        for (int i = 0; i < size; i++) {
            if (positiveAmount.size() < size) {
                positiveAmount.add(0.0);
            }
            if (positiveAmount.get(i) == null) {
                positiveAmount.set(i, 0.0);
            }

            if (negativeAmount.size() < size) {
                negativeAmount.add(0.0);
            }
            if (negativeAmount.get(i) == null) {
                negativeAmount.set(i, 0.0);
            }

            if (positiveAmount.isEmpty() || (!Objects.equals(positiveAmount.get(i), 0.0) && positiveAmount.get(i) + negativeAmount.get(i) == 0)) {
                logger.info(user + " Redirecting to error page with error: Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
                session.setAttribute("error", "Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
                return "error.html";
            }
            amount.add(positiveAmount.get(i) + negativeAmount.get(i));


        }
        if (comment.isEmpty()) {
            logger.debug("comment is empty");
            comment = new ArrayList<>();
            for (String ignored : clientName) {
                comment.add("");
            }
        }

        try {
            List<Integer> currencyId = new ArrayList<>();
            List<Integer> clientId = new ArrayList<>();
            for (String name : currencyName) {
                currencyId.add(currencyManager.getCurrency(name).getId());
            }
            for (String name : clientName) {
                clientId.add(clientManager.getClientExactly(name).getId());
            }

            Stack<Action> undos = (Stack<Action>) session.getAttribute("undos" + clientId.get(0));
            if (undos == null) {
                undos = new Stack<>();
            }
            Stack<Action> redos = (Stack<Action>) session.getAttribute("redos" + clientId.get(0));
            if (redos == null) {
                redos = new Stack<>();
            }
            reverse(undos);
            reverse(redos);

            List<Transaction> transactionList = transManager.getTransaction(transactionId.get(0)).stream().sorted(new Comparator<>() {
                final Client client = (Client) session.getAttribute("client");

                @Override
                public int compare(Transaction o1, Transaction o2) {
                    if (Objects.equals(o1.getClient().getId(), client.getId())) return -1;
                    if (Objects.equals(o2.getClient().getId(), client.getId())) return 1;
                    return 0;
                }
            }).toList();
            Map<Client, Map<Currency, Double>> oldBalances = new TreeMap<>();
            Map<Client, Map<Currency, Double>> newBalances = new TreeMap<>();
            Transaction tr = transactionList.get(0);

            transManager.deleteTransaction(transactionList);
            for (int i = 0; i < clientName.size(); i++) {
                if (commission.size() < clientName.size()) {
                    commission.add(0.0);
                }
                if (commission.get(i) == null) {
                    commission.set(i, 0.0);
                }

                if (rate.size() < clientName.size()) {
                    rate.add(1.0);
                }
                if (rate.get(i) == null) {
                    rate.set(i, 1.0);
                }

                if (transportation.size() < clientName.size()) {
                    transportation.add(0.0);
                }
                if (transportation.get(i) == null) {
                    transportation.set(i, 0.0);
                }
            }


            for (int i = 0; i < clientName.size(); i++) {
                Map<Currency, Double> oldBalance = new TreeMap<>();
                if (oldBalances.containsKey(clientManager.getClientExactly(clientName.get(i)))) {
                    oldBalance = oldBalances.get(clientManager.getClientExactly(clientName.get(i)));
                }
                TransactionDto previous = transManager.findPrevious(clientId.get(i), currencyId.get(i), tr.getDate(), transactionId.get(0));
                Double b = 0.0;
                if (previous != null) b = previous.getBalance();
                oldBalance.put(currencyManager.getCurrency(currencyId.get(i)), b);
                oldBalances.put(clientManager.getClientExactly(clientName.get(i)), oldBalance);
            }
            for (int i = 0; i < clientName.size(); i++) {
                TransactionDto previous = transManager.findPrevious(clientId.get(i), currencyId.get(i), tr.getDate(), transactionId.get(0));
                Double balance = 0.0;
                if (previous != null) balance = previous.getBalance();
                int finalI = i;
                Transaction tran = transactionList.stream()
                        .filter(t -> Objects.equals(t.getCurrency().getId(), currencyId.get(finalI))
                                && Objects.equals(t.getClient().getId(), clientId.get(finalI))).findFirst().orElse(new Transaction());
                Double b = transManager.update(tr.getId(), tr.getDate(), clientName.get(i), comment.get(i),
                        currencyId.get(i), rate.get(i), commission.get(i), amount.get(i),
                        transportation.get(i), tran.getCommentColor(), tran.getAmountColor(), tr.getUser().getId(), balance,
                        tran.getInputColor(), tran.getOutputColor(), tran.getTarifColor(), tran.getCommissionColor(), tran.getRateColor(),
                        tran.getTransportationColor());
                Map<Currency, Double> newBalance = new TreeMap<>();
                if (newBalances.containsKey(clientManager.getClientExactly(clientName.get(i)))) {
                    newBalance = newBalances.get(clientManager.getClientExactly(clientName.get(i)));
                }
                newBalance.put(currencyManager.getCurrency(currencyId.get(i)), b);
                newBalances.put(clientManager.getClientExactly(clientName.get(i)), newBalance);
            }
            logger.trace("oldBalances: " + oldBalances);
            logger.trace("newBalances: " + newBalances);
            Set<Map.Entry<Client, Map<Currency, Double>>> set = oldBalances.entrySet();
            for (Map.Entry<Client, Map<Currency, Double>> entry : set) {
                Client key = entry.getKey();
                Map<Currency, Double> value = entry.getValue();
                Set<Map.Entry<Currency, Double>> valueSet = value.entrySet();
                for (Map.Entry<Currency, Double> e : valueSet) {
                    Currency k = e.getKey();
                    Double v = e.getValue();
                    List<Integer> o = new ArrayList<>();
                    o.add(k.getId());
                    transManager.updateNext(key.getPib(), o, newBalances.get(key).get(k) - v, tr.getDate());
                }
            }


            List<Transaction> newTransactionList = transManager.getTransaction(transactionId.get(0)).stream().sorted(new Comparator<>() {
                final Client client = (Client) session.getAttribute("client");

                @Override
                public int compare(Transaction o1, Transaction o2) {
                    if (Objects.equals(o1.getClient().getId(), client.getId())) return -1;
                    if (Objects.equals(o2.getClient().getId(), client.getId())) return 1;
                    return 0;
                }
            }).toList();
            String change = findChanges(newTransactionList, transactionList, (Client) session.getAttribute("client"));
            if (!change.isEmpty()) {
                logger.debug("found changes");
                if (session.getAttribute("path") != "undo" && session.getAttribute("path") != "redo") {
                    logger.debug("new action of editing");
                    redos.clear();
                }
                if (session.getAttribute("path") != "undo" /*&& session.getAttribute("path") != "redo"*/) {
                    logger.debug("redoing of editing");
                    Action action = new Action();
                    action.setName("Редактирование " + change);
                    //action.setName("Редактирование");
                    action.setChanges(transactionList);
                    logger.trace(String.valueOf(action));
                    if (!undos.contains(action)) {
                        logger.debug("new action");
                        undos.push(action);
                    }
                    session.removeAttribute("path");
                }
                if (session.getAttribute("path") == "undo") {
                    logger.debug("undoing of editing");
                    Action action = getAction(change, transactionList);
                    logger.trace(String.valueOf(action));
                    if (!redos.contains(action)) {
                        logger.debug("new action");
                        redos.push(action);
                    }
                    session.removeAttribute("path");
                }
            }
            reverse(undos);
            reverse(redos);
            session.setAttribute("undos" + clientId.get(0), undos);
            session.setAttribute("redos" + clientId.get(0), redos);
            /*String[] pp = pointer.split("_");
            pointer = transactionId + "_" + currencyManager.getCurrency(pp[0].substring(0, 3)).getId() + "_" + clientId.get(0) + "_" + pp[1];*/
        } catch (Exception e) {
            logger.info(user + " Redirecting to error page with error: " + e.getMessage() + Arrays.toString(e.getStackTrace()));

            session.setAttribute("error", e.getMessage());
            return "error";
        }

        logger.info(user + " Transaction saved");
        session.setAttribute("pointer", pointer);
        return "redirect:/client_info";
    }

    private static Action getAction(String change, List<Transaction> transactionList) {
        Action action = new Action();
        action.setName("Редактирование " + change);
        //action.setName("Редактирование");
                /*List<Transaction> changes = new ArrayList<>();
                for (int i = 0; i < clientName.size(); i++) {
                   Transaction change = new Transaction();
                   change.setId(tr.getId());
                   change.setClient(clientManager.getClientExactly(clientName.get(i)));
                   change.setCurrency(currencyManager.getCurrency(currencyName.get(i)));
                   change.setDate(tr.getDate());
                   change.setComment(comment.get(i));
                   change.setAmount(amount.get(i));
                   change.setCommission(commission.get(i));
                   change.setRate(rate.get(i));
                   change.setTransportation(transportation.get(i));
                   changes.add(change);
                }
                action.setChanges(changes);*/
        action.setChanges(transactionList);
        return action;
    }

    private String findChanges(List<Transaction> newTransactionList, List<Transaction> transactionList, Client client) {
        StringBuilder result = new StringBuilder();
        int length = Math.max(newTransactionList.size(), transactionList.size());
        if (newTransactionList.size() < length) {
            for (int i = 0; i < length; i++) {
                if (!newTransactionList.contains(transactionList.get(i))) {
                    if (transactionList.get(i).getClient().equals(client)) {
                        result.append("записи ").append("\"").append(transactionList.get(i).getAmount()).append("\"");
                        return result.toString();
                    }
                }
            }
            for (int i = 0; i < length; i++) {
                if (!newTransactionList.contains(transactionList.get(i))) {
                    result.append("удаление контрагента ").append("\"").append(transactionList.get(i).getClient().getPib()).append("\"");
                    return result.toString();
                }
            }
        }
        if (transactionList.size() < length) {
            for (int i = 0; i < length; i++) {
                if (!transactionList.contains(newTransactionList.get(i))) {
                    if (newTransactionList.get(i).getClient().equals(client)) {
                        result.append("записи ").append("\"").append(newTransactionList.get(i).getAmount()).append("\"");
                        return result.toString();
                    }
                }
            }
            for (int i = 0; i < length; i++) {
                if (!transactionList.contains(newTransactionList.get(i))) {
                    result.append("добавление контрагента ").append("\"").append(newTransactionList.get(i).getClient().getPib()).append("\"");
                    return result.toString();
                }
            }
        }
        for (int i = 0; i < length; i++) {
            if (!Objects.equals(transactionList.get(i).getComment(), newTransactionList.get(i).getComment())) {
                result.append("изменение комментария ").append("\"").append(newTransactionList.get(i).getComment()).append("\"");
                break;
            }
            if (!Objects.equals(transactionList.get(i).getAmount(), newTransactionList.get(i).getAmount())) {
                result.append("изменение объема ").append("\"").append(newTransactionList.get(i).getAmount()).append("\"");
                break;
            }
            if (!Objects.equals(transactionList.get(i).getCommission(), newTransactionList.get(i).getCommission())) {
                result.append("изменение тарифа ").append("\"").append(newTransactionList.get(i).getCommission()).append("\"");
                break;
            }
            if (!Objects.equals(transactionList.get(i).getRate(), newTransactionList.get(i).getRate())) {
                result.append("изменение курса ").append("\"").append(newTransactionList.get(i).getRate()).append("\"");
                break;
            }
            if (!Objects.equals(transactionList.get(i).getTransportation(), newTransactionList.get(i).getTransportation())) {
                result.append("изменение инкасации ").append("\"").append(newTransactionList.get(i).getTransportation()).append("\"");
                break;
            }
        }
        return result.toString();
    }

    /**
     * Deleting transaction from database
     *
     * @param transactionId id of transaction that need to be deleted
     * @param session       collect parameters for view
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PostMapping(path = "delete_transaction")
    public String deleteTransaction(@RequestParam(name = "transaction_id") int transactionId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Deleting transaction...");
        int clientId = ((Client) session.getAttribute("client")).getId();
        Stack<Action> undos = (Stack<Action>) session.getAttribute("undos" + clientId);
        if (undos == null) {
            undos = new Stack<>();
        }
        Stack<Action> redos = (Stack<Action>) session.getAttribute("redos" + clientId);
        if (redos == null) {
            redos = new Stack<>();
        }
        reverse(undos);
        reverse(redos);
        List<Transaction> transactionList = transManager.getTransaction(transactionId);
        transManager.deleteTransaction(transactionList);
        List<Transaction> newTransactionList = transManager.getTransaction(transactionId);
        String change = findChanges(newTransactionList, transactionList, (Client) session.getAttribute("client"));
        if (!change.isEmpty()) {
            logger.debug("found changes");
            if (session.getAttribute("path") != "undo" && session.getAttribute("path") != "redo") {
                redos.clear();
                logger.debug("new action of deleting");
            }

            if (session.getAttribute("path") != "undo" /*&& session.getAttribute("path") != "redo"*/) {
                logger.debug("redoing deleting");
                Action action = new Action();
                action.setName("Удаление " + change);
                //action.setName("Удаление");
                action.setChanges(transactionList);
                logger.trace(String.valueOf(action));
                if (!undos.contains(action)) {
                    logger.debug("new action");
                    undos.push(action);
                }
                session.removeAttribute("path");

            }
            if (session.getAttribute("path") == "undo") {
                logger.debug("undoing adding");
                Action action = new Action();
                action.setName("Добавление " + change);
                //action.setName("Добавление");
                action.setChanges(transactionList);
                logger.trace(String.valueOf(action));
                if (!redos.contains(action)) {
                    logger.debug("new action");
                    redos.push(action);
                }
                session.removeAttribute("path");

            }
        }
        reverse(undos);
        reverse(redos);
        session.setAttribute("undos" + clientId, undos);
        session.setAttribute("redos" + clientId, redos);
        logger.info(user + " Transaction deleted");
        return "redirect:/client_info";
    }

    /**
     * Saves new transaction to database
     *
     * @param clientName     names of client that is a member of this transaction
     * @param currencyName   names of currency that changed by this transaction
     * @param comment        comment about this transaction
     * @param session        collect parameters for view
     */

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PostMapping(path = "/transaction")
    public String doTransaction(@RequestParam(name = "client_name") List<String> clientName,
                                @RequestParam(name = "currency_name") List<String> currencyName,
                                @RequestParam(name = "comment", required = false) List<String> comment,
                                @RequestParam(name = "rate") List<String> rateS,
                                @RequestParam(name = "commission") List<String> commissionS,
                                @RequestParam(name = "positiveAmount") List<String> positiveAmountS,
                                @RequestParam(name = "negativeAmount") List<String> negativeAmountS,
                                @RequestParam(name = "transportation") List<String> transportationS,
                                @RequestParam(name = "date", required = false) java.sql.Date date,
                                @RequestParam(name = "pointer", required = false, defaultValue = "UAHtr0_pAmount") String pointer,
                                HttpSession session,
                                @RequestParam(required = false) List<String> commentColor,
                                @RequestParam(required = false) List<String> inputColor,
                                @RequestParam(required = false) List<String> outputColor,
                                @RequestParam(required = false) List<String> tarifColor,
                                @RequestParam(required = false) List<String> commissionColor,
                                @RequestParam(required = false) List<String> rateColor,
                                @RequestParam(required = false) List<String> transportationColor,
                                @RequestParam(required = false) List<String> amountColor) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Creating new transaction...");

        List<Double> rate = new ArrayList<>();
        for (String s : rateS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                rate.add(Double.valueOf(s));
            } else {
                rate.add(null);
            }
        }
        System.out.println(rate);
        List<Double> commission = new ArrayList<>();
        for (String s : commissionS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                commission.add(Double.valueOf(s));
            } else {
                commission.add(null);
            }
        }
        List<Double> positiveAmount = new ArrayList<>();
        for (String s : positiveAmountS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                positiveAmount.add(Double.valueOf(s));
            } else {
                positiveAmount.add(null);
            }
        }
        List<Double> negativeAmount = new ArrayList<>();
        for (String s : negativeAmountS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                negativeAmount.add(Double.valueOf(s));
            } else {
                negativeAmount.add(null);
            }
        }
        List<Double> transportation = new ArrayList<>();
        for (String s : transportationS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                transportation.add(Double.valueOf(s));
            } else {
                transportation.add(null);
            }
        }
        List<Double> amount = new ArrayList<>();
        int size = positiveAmount.size();
        if (size < negativeAmount.size()) size = negativeAmount.size();
        for (int i = 0; i < size; i++) {
            if (positiveAmount.size() < size) {
                positiveAmount.add(0.0);
            }
            if (positiveAmount.get(i) == null) {
                positiveAmount.set(i, 0.0);
            }
            if (negativeAmount.size() < size) {
                negativeAmount.add(0.0);
            }
            if (negativeAmount.get(i) == null) {
                negativeAmount.set(i, 0.0);
            }

            if (positiveAmount.isEmpty() || (positiveAmount.get(i) != 0 && positiveAmount.get(i) + negativeAmount.get(i) == 0)) {
                logger.info(user + " Redirecting to error page with error: Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
                session.setAttribute("error", "Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
                return "error";
            }
            amount.add(positiveAmount.get(i) + negativeAmount.get(i));
        }

        if (session.getAttribute("path") == "/transfer") {
            amount.set(0, (-1) * amount.get(0));
        }
        try {
            if (comment.isEmpty()) {
                comment = new ArrayList<>();
                for (String name : clientName) {
                    comment.add("");
                }
            }
            List<String> clientDouble = new ArrayList<>();
            List<String> currencyDouble = new ArrayList<>();
            for (int i = 0; i < clientName.size(); i++) {
                if (!clientDouble.contains(clientName.get(i))) {
                    clientDouble.add(clientName.get(i));
                    currencyDouble.add(currencyName.get(i));
                } else if (currencyDouble.contains(currencyName.get(i))) {
                    session.setAttribute("error", "Обнаружено наличие как минимум двух позиций с совпадением " +
                            "значений клиента и валюты. Подобная операция не допустима");
                    logger.info(user + " Redirecting to error page with error: Обнаружено наличие как минимум двух позиций " +
                            "с совпадением значений клиента и валюты. Подобная операция не допустима");
                    return "error";
                }
            }
            int transactionId = transManager.getMaxId() + 1;
            logger.trace("transactionId: " + transactionId);
            List<Transaction> oldTransactionList = transManager.getTransaction(transactionId);
            List<Integer> currencyId = new ArrayList<>();
            for (String name : currencyName) {
                currencyId.add(currencyManager.getCurrency(name).getId());
            }
            List<Integer> clientId = new ArrayList<>();
            for (String name : clientName) {
                clientId.add(clientManager.getClientExactly(name).getId());
            }
            List<String> arr = new ArrayList<>();
            for (int i = 0; i < clientName.size(); i++) {
                arr.add(null);
            }
            if (commentColor == null) commentColor = new ArrayList<>(arr);
            if (inputColor == null) inputColor = new ArrayList<>(arr);
            if (outputColor == null) outputColor = new ArrayList<>(arr);
            if (tarifColor == null) tarifColor = new ArrayList<>(arr);
            if (commissionColor == null) commissionColor = new ArrayList<>(arr);
            if (rateColor == null) rateColor = new ArrayList<>(arr);
            if (transportationColor == null) transportationColor = new ArrayList<>(arr);
            if (amountColor == null) amountColor = new ArrayList<>(arr);
            for (int i = 0; i < clientName.size(); i++) {
                if (commission.size() < clientName.size()) {
                    commission.add(0.0);
                }
                if (commission.get(i) == null) {
                    commission.set(i, 0.0);
                }

                if (rate.size() < clientName.size()) {
                    rate.add(1.0);
                }
                if (rate.get(i) == null) {
                    rate.set(i, 1.0);
                }

                if (transportation.size() < clientName.size()) {
                    transportation.add(0.0);
                }
                if (transportation.get(i) == null) {
                    transportation.set(i, 0.0);
                }

                if (amount.get(i) == 0) continue;

                try {
                    if (date != null && date.before(new java.sql.Date(LocalDate.now().atStartOfDay(systemDefault()).toInstant().toEpochMilli()))) {
                        logger.debug("Date before today date");
                        Timestamp dt = transManager.getAvailableDate(new Timestamp(date.getTime()));
                        TransactionDto previousTransaction = transManager.findPrevious(clientId.get(i), currencyId.get(i), dt, 0);
                        Double trBalance = null;
                        if (previousTransaction != null) {
                            trBalance = previousTransaction.getBalance();
                        }
                        logger.trace("trBalance: " + trBalance);
                        Double oldBalance = accountManager.getAccount(clientName.get(i)).getCurrencies().get(currencyManager.getCurrency(currencyId.get(i))).getAmount();
                        logger.trace("oldBalance: " + oldBalance);
                        Double newBalance = transManager.remittance(transactionId, dt, clientName.get(i), comment.get(i),
                                currencyId.get(i), rate.get(i), commission.get(i), amount.get(i), transportation.get(i),
                                commentColor.get(i), amountColor.get(i), user.getId(), trBalance, inputColor.get(i),
                                outputColor.get(i), tarifColor.get(i), commissionColor.get(i),
                                rateColor.get(i), transportationColor.get(i));
                        logger.trace("newBalance: " + newBalance);
                        List<Integer> list = new ArrayList<>();
                        list.add(currencyId.get(i));
                        logger.trace("list: " + list);
                        transManager.updateNext(clientName.get(i), list, newBalance - oldBalance, dt);
                    } else {
                        logger.debug("Date is today");
                        transManager.remittance(transactionId, null, clientName.get(i), comment.get(i),
                                currencyId.get(i), rate.get(i), commission.get(i), amount.get(i), transportation.get(i),
                                commentColor.get(i), amountColor.get(i), user.getId(), 0.0, inputColor.get(i),
                                outputColor.get(i), tarifColor.get(i), commissionColor.get(i),
                                rateColor.get(i), transportationColor.get(i));
                    }
                } catch (Exception e) {
                    logger.info(user + " Redirecting to error page with error: " + e.getMessage() + Arrays.toString(e.getStackTrace()));
                    session.setAttribute("error", e.getMessage());
                    return "error";
                }
            }


            Stack<Action> undos = (Stack<Action>) session.getAttribute("undos" + clientId.get(0));
            if (undos == null) {
                undos = new Stack<>();
            }
            Stack<Action> redos = (Stack<Action>) session.getAttribute("redos" + clientId.get(0));
            if (redos == null) {
                redos = new Stack<>();
            }
            reverse(undos);
            reverse(redos);
            List<Transaction> transactionList = transManager.getTransaction(transactionId);
            String change = findChanges(transactionList, oldTransactionList, (Client) session.getAttribute("client"));
            if (!change.isEmpty()) {
                logger.debug("found changes");
                if (session.getAttribute("path") != "undo" && session.getAttribute("path") != "redo") {
                    logger.debug("new action of adding");
                    redos.clear();
                }
                if (session.getAttribute("path") != "undo" /*&& session.getAttribute("path") != "redo"*/) {
                    logger.debug("redo adding");
                    Action action = new Action();
                    action.setName("Добавление " + change);
                    //action.setName("Добавление");
                    action.setChanges(transactionList);
                    logger.trace(String.valueOf(action));
                    if (!undos.contains(action)) {
                        logger.debug("new action");
                        undos.push(action);
                    }
                    session.removeAttribute("path");

                }
                if (session.getAttribute("path") == "undo") {
                    logger.debug("undo deleting");
                    Action action = new Action();
                    action.setName("Удаление " + change);
                    //action.setName("Удаление");
                    action.setChanges(transactionList);
                    logger.trace(String.valueOf(action));
                    if (!redos.contains(action)) {
                        logger.debug("new action");
                        redos.push(action);
                    }
                    session.removeAttribute("path");

                }
            }
            reverse(undos);
            reverse(redos);
            session.setAttribute("undos" + clientId.get(0), undos);
            session.setAttribute("redos" + clientId.get(0), redos);
            clientId = clientId.stream().distinct().collect(Collectors.toList());
            String[] pp = pointer.split("_");
            pointer = transactionId + "_" + currencyManager.getCurrency(pp[0].substring(0, 3)).getId() + "_" + clientId.get(Integer.parseInt(pp[0].substring(5))) + "_" + pp[1];
            java.sql.Date startDate = new Date(((Timestamp) session.getAttribute("startDate" + clientId.get(0))).getTime());
            if (startDate.after(date)) {
                startDate = date;
            }
            assert startDate != null;
            session.setAttribute("startDate" + clientId.get(0), new Timestamp(startDate.getTime()));
        } catch (Exception e) {
            logger.info(user + " Redirecting to error page with error: " + e.getMessage() + Arrays.toString(e.getStackTrace()));
            session.setAttribute("error", e.getMessage());
            return "error";
        }

        logger.info(user + " Transaction created");
        session.setAttribute("pointer", pointer);
        return "redirect:/client_info";
    }

    @PostMapping("/undo")
    public String undo(@RequestParam(name = "id", required = false, defaultValue = "-1") int id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Starting undoing...");
        logger.trace("Action id=" + id);
        int clientId = ((Client) session.getAttribute("client")).getId();
        if (id != -1) {

            for (int i = 0; i <= id; i++) {
                session.setAttribute("path", "undo");

                Stack<Action> undos = (Stack<Action>) session.getAttribute("undos" + clientId);
                if (undos == null) {
                    undos = new Stack<>();
                }
                Stack<Action> redos = (Stack<Action>) session.getAttribute("redos" + clientId);
                if (redos == null) {
                    redos = new Stack<>();
                }
                reverse(undos);
                reverse(redos);
                logger.trace("undos: " + undos);
                logger.trace("redos: " + redos);
                /*Action action = undos.get(undos.size() - i - 1);
                undos.pop();*/
                Action action = undos.pop();
                action.setChanges(action.getChanges().stream().sorted((o1, o2) -> {
                    if (o1.getClient().getId() == clientId) return -1;
                    if (o2.getClient().getId() == clientId) return 1;
                    return 0;
                }).toList());
                reverse(undos);
                reverse(redos);
                session.setAttribute("redos" + clientId, redos);
                session.setAttribute("undos" + clientId, undos);
                if (action.getName().matches("Редактирование.*")) {
                    logger.debug("found edit");
                    List<Transaction> transactions = action.getChanges();
                    List<Integer> transactionId = new ArrayList<>();
                    List<String> clientName = new ArrayList<>();
                    List<String> comment = new ArrayList<>();
                    List<String> currencyName = new ArrayList<>();
                    List<String> rate = new ArrayList<>();
                    List<String> commission = new ArrayList<>();
                    List<String> positiveAmount = new ArrayList<>();
                    List<String> negativeAmount = new ArrayList<>();
                    List<String> transportation = new ArrayList<>();
                    for (Transaction transaction : transactions) {
                        transactionId.add(transaction.getId());
                        clientName.add(transaction.getClient().getPib());
                        comment.add(transaction.getComment());
                        currencyName.add((transaction.getCurrency().getName()));
                        rate.add(String.valueOf(transaction.getRate()));
                        commission.add(String.valueOf(transaction.getCommission()));
                        positiveAmount.add(String.valueOf(transaction.getAmount()));
                        negativeAmount.add(String.valueOf(0.0));
                        transportation.add(String.valueOf(transaction.getTransportation()));
                    }
                    editTransaction(transactionId, clientName, comment, currencyName, rate, commission, positiveAmount, negativeAmount, transportation, "UAHtr0_pAmount", session);
                    //return "redirect:/client_info";
                } else if (action.getName().matches("Удаление.*")) {
                    logger.debug("found delete");
                    List<Transaction> transactions = action.getChanges();
                    List<Integer> transactionId = new ArrayList<>();
                    Timestamp date = transactions.get(0).getDate();
                    List<String> clientName = new ArrayList<>();
                    List<String> comment = new ArrayList<>();
                    List<String> currencyName = new ArrayList<>();
                    List<String> rate = new ArrayList<>();
                    List<String> commission = new ArrayList<>();
                    List<String> positiveAmount = new ArrayList<>();
                    List<String> negativeAmount = new ArrayList<>();
                    List<String> transportation = new ArrayList<>();
                    List<String> commentColor = new ArrayList<>();
                    List<String> inputColor = new ArrayList<>();
                    List<String> outputColor = new ArrayList<>();
                    List<String> tarifColor = new ArrayList<>();
                    List<String> commissionColor = new ArrayList<>();
                    List<String> rateColor = new ArrayList<>();
                    List<String> transportationColor = new ArrayList<>();
                    List<String> amountColor = new ArrayList<>();
                    for (Transaction transaction : transactions) {
                        transactionId.add(transaction.getId());
                        clientName.add(transaction.getClient().getPib());
                        comment.add(transaction.getComment());
                        currencyName.add((transaction.getCurrency().getName()));
                        rate.add(String.valueOf(transaction.getRate()));
                        commission.add(String.valueOf(transaction.getCommission()));
                        positiveAmount.add(String.valueOf(transaction.getAmount()));
                        negativeAmount.add(String.valueOf(0.0));
                        transportation.add(String.valueOf(transaction.getTransportation()));
                        commentColor.add(transaction.getCommentColor());
                        inputColor.add(transaction.getInputColor());
                        outputColor.add(transaction.getOutputColor());
                        tarifColor.add(transaction.getTarifColor());
                        commissionColor.add(transaction.getCommissionColor());
                        rateColor.add(transaction.getRateColor());
                        transportationColor.add(transaction.getTransportationColor());
                        amountColor.add(transaction.getAmountColor());
                    }
                    doTransaction(clientName, currencyName, comment, rate, commission, positiveAmount, negativeAmount,
                            transportation, new Date(date.getTime()), "UAHtr0_pAmount", session, commentColor,
                            inputColor, outputColor, tarifColor, commissionColor, rateColor, transportationColor,
                            amountColor);
                    //return "redirect:/client_info";
                } else if (action.getName().matches("Добавление.*")) {
                    logger.debug("found add");
                    List<Transaction> transactions = action.getChanges();
                    deleteTransaction(transactions.get(0).getId(), session);
                    //return "redirect:/client_info";
                }
            }

        }
        return "redirect:/client_info";
    }

    @PostMapping("/redo")
    public String redo(@RequestParam(name = "id", required = false, defaultValue = "-1") int id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Starting redoing...");
        logger.trace("Action id=" + id);
        int clientId = ((Client) session.getAttribute("client")).getId();
        if (id != -1) {

            for (int i = 0; i <= id; i++) {
                session.setAttribute("path", "redo");
                Stack<Action> undos = (Stack<Action>) session.getAttribute("undos" + clientId);
                if (undos == null) {
                    undos = new Stack<>();
                }
                Stack<Action> redos = (Stack<Action>) session.getAttribute("redos" + clientId);
                if (redos == null) {
                    redos = new Stack<>();
                }

                reverse(undos);
                reverse(redos);
                logger.trace("undos: " + undos);
                logger.trace("redos: " + redos);
                /*Action action = redos.get(redos.size() - i - 1);
                redos.pop();*/
                Action action = redos.pop();

            /*if (!undos.contains(action)) {
                undos.push(action);
            }*/
                reverse(undos);
                reverse(redos);
                session.setAttribute("undos" + clientId, undos);
                session.setAttribute("redos" + clientId, redos);
                action.setChanges(action.getChanges().stream().sorted((o1, o2) -> {
                    if (o1.getClient().getId() == clientId) return -1;
                    if (o2.getClient().getId() == clientId) return 1;
                    return 0;
                }).toList());
                if (action.getName().matches("Редактирование.*")) {
                    logger.debug("found edit");
                    List<Transaction> transactions = action.getChanges();
                    List<Integer> transactionId = new ArrayList<>();
                    List<String> clientName = new ArrayList<>();
                    List<String> comment = new ArrayList<>();
                    List<String> currencyName = new ArrayList<>();
                    List<String> rate = new ArrayList<>();
                    List<String> commission = new ArrayList<>();
                    List<String> positiveAmount = new ArrayList<>();
                    List<String> negativeAmount = new ArrayList<>();
                    List<String> transportation = new ArrayList<>();

                    for (Transaction transaction : transactions) {
                        transactionId.add(transaction.getId());
                        clientName.add(transaction.getClient().getPib());
                        comment.add(transaction.getComment());
                        currencyName.add((transaction.getCurrency().getName()));
                        rate.add(String.valueOf(transaction.getRate()));
                        commission.add(String.valueOf(transaction.getCommission()));
                        positiveAmount.add(String.valueOf(transaction.getAmount()));
                        negativeAmount.add(String.valueOf(0.0));
                        transportation.add(String.valueOf(transaction.getTransportation()));
                    }
                    editTransaction(transactionId, clientName, comment, currencyName, rate, commission, positiveAmount, negativeAmount, transportation, "UAHtr0_pAmount", session);
                    //return "redirect:/client_info";
                } else if (action.getName().matches("Удаление.*")) {
                    logger.debug("found delete");
                    List<Transaction> transactions = action.getChanges();
                    deleteTransaction(transactions.get(0).getId(), session);
                    //return "redirect:/client_info";
                } else if (action.getName().matches("Добавление.*")) {
                    logger.debug("found add");
                    List<Transaction> transactions = action.getChanges();
                    List<Integer> transactionId = new ArrayList<>();
                    Timestamp date = transactions.get(0).getDate();
                    List<String> clientName = new ArrayList<>();
                    List<String> comment = new ArrayList<>();
                    List<String> currencyName = new ArrayList<>();
                    List<String> rate = new ArrayList<>();
                    List<String> commission = new ArrayList<>();
                    List<String> positiveAmount = new ArrayList<>();
                    List<String> negativeAmount = new ArrayList<>();
                    List<String> transportation = new ArrayList<>();
                    List<String> commentColor = new ArrayList<>();
                    List<String> inputColor = new ArrayList<>();
                    List<String> outputColor = new ArrayList<>();
                    List<String> tarifColor = new ArrayList<>();
                    List<String> commissionColor = new ArrayList<>();
                    List<String> rateColor = new ArrayList<>();
                    List<String> transportationColor = new ArrayList<>();
                    List<String> amountColor = new ArrayList<>();
                    for (Transaction transaction : transactions) {
                        transactionId.add(transaction.getId());
                        clientName.add(transaction.getClient().getPib());
                        comment.add(transaction.getComment());
                        currencyName.add((transaction.getCurrency().getName()));
                        rate.add(String.valueOf(transaction.getRate()));
                        commission.add(String.valueOf(transaction.getCommission()));
                        positiveAmount.add(String.valueOf(transaction.getAmount()));
                        negativeAmount.add(String.valueOf(0.0));
                        transportation.add(String.valueOf(transaction.getTransportation()));
                        commentColor.add(transaction.getCommentColor());
                        inputColor.add(transaction.getInputColor());
                        outputColor.add(transaction.getOutputColor());
                        tarifColor.add(transaction.getTarifColor());
                        commissionColor.add(transaction.getCommissionColor());
                        rateColor.add(transaction.getRateColor());
                        transportationColor.add(transaction.getTransportationColor());
                        amountColor.add(transaction.getAmountColor());
                    }
                    doTransaction(clientName, currencyName, comment, rate, commission, positiveAmount, negativeAmount,
                            transportation, new Date(date.getTime()), "UAHtr0_pAmount", session, commentColor,
                            inputColor, outputColor, tarifColor, commissionColor, rateColor, transportationColor,
                            amountColor);
                    //return "redirect:/client_info";
                }
            }

        }
        return "redirect:/client_info";
    }

    @GetMapping("/convertation")
    public String convertation(HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading convertation page");
        session.setAttribute("path", "/convertation");
        return "convertation";
    }

    @GetMapping("/transfer")
    public String transfer(HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading transfer page");
        session.setAttribute("path", "/transfer");
        return "transfer";
    }

}
