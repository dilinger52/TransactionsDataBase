package org.profinef.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.profinef.dto.TransactionDto;
import org.profinef.entity.Currency;
import org.profinef.entity.*;
import org.profinef.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.time.ZoneId.systemDefault;
import static java.util.Collections.reverse;


/**
 * This is the main class that work with visuals
 **/
@Controller
public class AppController {
    private static final Logger logger = LoggerFactory.getLogger(AppController.class);
    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final AccountManager accountManager;
    @Autowired
    private final TransManager transManager;
    @Autowired
    private final CurrencyManager currencyManager;
    @Autowired
    private final UserManager userManager;
    @Autowired
    private final Scheduler scheduler;

    public AppController(ClientManager clientManager, AccountManager accountManager, TransManager transManager,
                         CurrencyManager currencyManager, UserManager userManager, Scheduler scheduler) {
        this.clientManager = clientManager;
        this.accountManager = accountManager;
        this.transManager = transManager;
        this.currencyManager = currencyManager;
        this.userManager = userManager;
        this.scheduler = scheduler;
    }

    /**
     * Method need just to redirect to main page
     */
    @GetMapping("/")
    public String viewHomePage() {
        logger.info("Redirecting to main page");
        return "redirect:/client";
    }

    /**
     * Method return page that contains input for file. This page uses for get data from Excel workbook
     *
     * @param session needed for set path parameter. It uses for back button
     */
    @GetMapping("/excel")
    public String loadPage(HttpSession session) {
        logger.info("Loading load files page");
        session.setAttribute("path", "/excel");
        session.setAttribute("date", new Date(System.currentTimeMillis()));
        return "uploadFiles";
    }

    /**
     * Generate page with clients transactions and balance for each currency. Also, this page contains fields for
     * creating new transaction
     *
     * @param clientId   id of client that information will be present
     * @param currencyId list of currencies ids to filter transactions. If id is present than transaction with this
     *                   currency will be shown. If currency id is empty than will be shown transactions for all
     *                   currencies
     * @param startDate  the date to filter transactions. Transactions will be shown from this day
     * @param endDate    the date to filter transactions. Transactions will be shown to this day
     * @param session    collect parameters for view
     */
    @GetMapping("/client_info")
    public String viewClientTransactions(@RequestParam(name = "client_id", required = false, defaultValue = "0")
                                         int clientId,
                                         @RequestParam(name = "client_name", required = false)
                                         String clientName,
                                         @RequestParam(name = "currency_id", required = false) List<Integer> currencyId,
                                         @RequestParam(name = "startDate", required = false) Date startDate,
                                         @RequestParam(name = "endDate", required = false) Date endDate,
                                         HttpSession session) throws InterruptedException {
        logger.info("Loading client info page...");
        session.removeAttribute("transaction");
        session.removeAttribute("client_alert");
        if (clientId == 0) {
            logger.debug("clientId = 0");
            if (clientName != null) {
                clientId = clientManager.getClientExactly(clientName).getId();
            } else if (session.getAttribute("client") == null) {
                logger.debug("client = null");
                logger.info("Redirecting to main page");
                return "redirect:/client";
            } else {
                clientId = ((Client) session.getAttribute("client")).getId();
            }
            logger.debug("clientId had get from session");
            logger.trace("clientId = " + clientId);
        }
        List<String> currencyName = new ArrayList<>();
        if (currencyId == null) {
            logger.debug("currencyId = null");

            currencyId = new ArrayList<>();
            List<Currency> currencies = currencyManager.getAllCurrencies();
            for (Currency currency : currencies) {
                currencyName.add(currency.getName());
                currencyId.add(currency.getId());
            }
            logger.trace("currencyId = " + currencyId);
            logger.trace("currencyName = " + currencyName);
        } else {
            logger.debug("currencyId != null");
            for (int id : currencyId) {
                currencyName.add(currencyManager.getCurrency(id).getName());
            }
            logger.trace("currencyName = " + currencyName);
        }
        if (startDate == null) {
            logger.debug("requested start date is null");
            if (session.getAttribute("startDate" + clientId) == null) {
                logger.debug("session start date is null");
                startDate = Date.valueOf(LocalDate.now());
            } else {
                startDate = new Date(((Timestamp) session.getAttribute("startDate" + clientId)).getTime());
            }
        }
        if (endDate == null) {
            logger.debug("requested end date is null");
            if (session.getAttribute("endDate1" + clientId) == null) {
                logger.debug("requested end date is null");
                endDate = new Date(startDate.getTime() + 86400000);
            } else {
                endDate = new Date(((Timestamp) session.getAttribute("endDate1" + clientId)).getTime());
            }
        }
        logger.trace("startDate = " + startDate);
        logger.trace("endDate = " + endDate);
        Client client = clientManager.getClient(clientId);
        List<Transaction> transactions = transManager.findByClientForDate(clientId, currencyId,
                new Timestamp(startDate.getTime()), new Timestamp(endDate.getTime()));
        Set<String> comments = transManager.getAllComments();
        Map<String, Double> total = new HashMap<>();
        List<Currency> currencies = currencyManager.getAllCurrencies();

        Map<Integer, List<Integer>> transactionIds = new HashMap<>();
        for (Currency currency : currencies) {
            List<Integer> list = transactions.stream()
                    .filter(transaction -> transaction.getCurrency().getId().equals(currency.getId()))
                    .map(Transaction::getId).collect(Collectors.toList());
            transactionIds.put(currency.getId(), list);
            List<Transaction> transactionList = transactions.stream().filter(transaction -> transaction.getCurrency()
                    .getId().equals(currency.getId())).toList();
            TransactionDto transactionDto = transManager.findPrevious(clientId, currency.getId(), new Timestamp(startDate.getTime()), 0);
            if (transactionDto == null) {
                total.put("amount" + currency.getId(), 0.0);
            } else {
                total.put("amount" + currency.getId(), transactionDto.getBalance());
            }
            if (transactionList.isEmpty()) {
                total.put("balance" + currency.getId(), total.get("amount" + currency.getId()));
            } else {
                total.put("balance" + currency.getId(), transactionList.get(transactionList.size() - 1).getBalance());
            }
        }
        logger.trace("total: " + total);

        List<Transaction> list = new ArrayList<>();
        for (Transaction tr : transactions) {

            List<Transaction> l = transManager.findById(tr.getId());
            for (Transaction t : l) {
                if (!list.contains(t)) {
                    list.add(t);
                }
            }

        }
        list.sort((o1, o2) -> {
            if (Objects.equals(o1.getId(), o2.getId())) {
                if (Objects.equals(o1.getClient().getId(), client.getId())) return -1;
                if (Objects.equals(o2.getClient().getId(), client.getId())) return 1;
            }
            return 0;
        });
        transactions = list;
        logger.trace("transactions: " + transactions);
        List<HttpSession> sessions = new HttpSessionConfig().getActiveSessions();
        for (HttpSession ses : sessions) {
            if (ses != null) {
                if (ses.getAttribute("path") == "/client_info" && ses.getAttribute("client").equals(client) && ses.getAttribute("user") != session.getAttribute("user")) {
                    session.setAttribute("client_alert", "Другой пользователь просматривает и/или редактирует данного клиента. Для получения актуальных данных дождитесь окончания работы данного пользователя");
                }
            }
        }
        List<Client> clients = clientManager.findAll();
        clients.remove(client);
        clients.sort(Comparator.comparing(Client::getPib));
        logger.trace("total = " + total);
        session.setAttribute("clients", clients);
        session.setAttribute("comments", comments);
        session.setAttribute("path", "/client_info");
        session.setAttribute("currencies", currencies);
        session.setAttribute("total", total);
        session.setAttribute("startDate" + clientId, new Timestamp(startDate.getTime()));
        session.setAttribute("endDate" + clientId, new Timestamp(endDate.getTime() - 1));
        session.setAttribute("endDate1" + clientId, new Timestamp(endDate.getTime()));
        session.setAttribute("client", client);
        session.setAttribute("transactions", transactions);
        session.setAttribute("transactionIds", transactionIds);
        session.setAttribute("currency_name", currencyName);
        logger.info("Client info page loaded");
        return "clientInfo2";
    }

    /**
     * Generate page that contains fields to collect information about new transaction
     *
     * @param session collect parameters for view
     */
    @GetMapping("/add_transaction")
    public String addTransaction(HttpSession session) {
        logger.info("Loading add transaction page...");
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
        logger.info("Add transaction page loaded");
        return "addTransaction";
    }

    /**
     * Generate main page that contains information about each client, their balances for each currency
     *
     * @param clientName     parameter to filter clients by names. If is null than filter is not uses
     * @param clientPhone    parameter to filter clients by phone. If is null than filter is not uses
     * @param clientTelegram parameter to filter clients by telegram. If is null than filter is not uses
     * @param session        collect parameters for view
     */
    @GetMapping(path = "/client")
    public String getClientsInfo(@RequestParam(name = "client_name", required = false) String clientName,
                                 @RequestParam(name = "client_phone", required = false) String clientPhone,
                                 @RequestParam(name = "client_telegram", required = false) String clientTelegram,
                                 @RequestParam(name = "search_exactly", required = false) boolean searchExactly,
                                 HttpSession session) {
        logger.info("Loading main page...");
        session.removeAttribute("error");
        session.removeAttribute("client");
        List<Account> clients;
        if (clientName != null && !clientName.isEmpty()) {
            logger.debug("Filtering clients by names");
            clients = new ArrayList<>();
            try {
                if (searchExactly) {
                    clients.add(accountManager.getAccount(clientName));
                } else {
                    clients.addAll(accountManager.getAccounts(clientName));
                }
            } catch (Exception e) {
                session.setAttribute("error", e.getMessage());
                logger.info("Redirected to error page with error: " + e.getMessage());
                return "error";
            }
        } else if (clientPhone != null && !clientPhone.isEmpty()) {
            logger.debug("Filtering clients by phone");
            clients = new ArrayList<>();
            try {
                clients.addAll(accountManager.getAccountsByPhone(clientPhone));
            } catch (Exception e) {
                session.setAttribute("error", e.getMessage());
                logger.info("Redirected to error page with error: " + e.getMessage());
                return "error";
            }
        } else if (clientTelegram != null && !clientTelegram.isEmpty()) {
            logger.debug("Filtering clients by telegram");
            clients = new ArrayList<>();
            try {
                clients.addAll(accountManager.getAccountsByTelegram(clientTelegram));
            } catch (Exception e) {
                session.setAttribute("error", e.getMessage());
                logger.info("Redirected to error page with error: " + e.getMessage());
                return "error";
            }
        } else {
            logger.debug("Filters are not present");
            clients = new ArrayList<>();
            List<Account> c = accountManager.getAllAccounts();
            clients.add(transManager.addTotal(c));
            clients.addAll(c);
        }

        List<Currency> currencies = currencyManager.getAllCurrencies();
        session.setAttribute("path", "/client");
        session.setAttribute("currencies", currencies);
        session.setAttribute("clients", clients);
        logger.info("Main page loaded");
        return "example";
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
        logger.info("Loading edit transaction page...");
        if (transactionId == 0) {
            logger.debug("transactionId == 0");
            if (session.getAttribute("transaction") == null) {
                logger.debug("Transaction is no set");
                logger.info("Redirecting to the main page");
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
        logger.info("Edit transaction page loaded");
        return "addTransaction";
    }

    /**
     * Edit transaction in database
     *
     * @param transactionId  ids of transactions that need to be editing
     * @param clientName     names of client which transaction need to be editing
     * @param comment        new comments to save to transaction
     * @param currencyName   new currencies names to save to transaction
     * @param rate           new rates to save to transaction
     * @param commission     new commissions to save to transaction
     * @param amount         new amounts to save to transaction
     * @param transportation new transportations to save to transaction
     * @param session        collect parameters for view
     */
    @Transactional
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
        logger.info("Saving transaction after editing...");

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
            logger.info("Redirecting to error page with error: Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
            session.setAttribute("error", "Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
            return "error";
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
                logger.info("Redirecting to error page with error: Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
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
            logger.info("Redirecting to error page with error: " + e.getMessage() + Arrays.toString(e.getStackTrace()));

            session.setAttribute("error", e.getMessage());
            return "error";
        }

        logger.info("Transaction saved");
        session.setAttribute("pointer", pointer);
        return "redirect:/client_info";
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
    @Transactional
    @PostMapping(path = "delete_transaction")
    public String deleteTransaction(@RequestParam(name = "transaction_id") int transactionId, HttpSession session) {
        logger.info("Deleting transaction...");
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
        logger.info("Transaction deleted");
        return "redirect:/client_info";
    }

    /**
     * Saves new transaction to database
     *
     * @param clientName     names of client that is a member of this transaction
     * @param currencyName   names of currency that changed by this transaction
     * @param comment        comment about this transaction
     * @param rate           exchange rate of currency for this transaction and this client
     * @param commission     commission for this client for this transaction
     * @param amount         amount of currency to be changed
     * @param transportation external commission if money will be transferred for client
     * @param session        collect parameters for view
     */

    @Transactional
    @PostMapping(path = "/transaction")
    public String doTransaction(@RequestParam(name = "client_name") List<String> clientName,
                                @RequestParam(name = "currency_name") List<String> currencyName,
                                @RequestParam(name = "comment", required = false) List<String> comment,
                                @RequestParam(name = "rate") List<String> rateS,
                                @RequestParam(name = "commission") List<String> commissionS,
                                @RequestParam(name = "positiveAmount") List<String> positiveAmountS,
                                @RequestParam(name = "negativeAmount") List<String> negativeAmountS,
                                @RequestParam(name = "transportation") List<String> transportationS,
                                @RequestParam(name = "date", required = false) Date date,
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
        logger.info("Creating new transaction...");

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
                logger.info("Redirecting to error page with error: Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
                session.setAttribute("error", "Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
                return "error";
            }
            amount.add(positiveAmount.get(i) + negativeAmount.get(i));
        }

        if (session.getAttribute("path") == "/transfer") {
            amount.set(0, (-1) * amount.get(0));
        }
        User user = (User) session.getAttribute("user");
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
                    logger.info("Redirecting to error page with error: Обнаружено наличие как минимум двух позиций " +
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
                    if (date != null && date.before(new Date(LocalDate.now().atStartOfDay(systemDefault()).toInstant().toEpochMilli()))) {
                        logger.debug("Date before today date");
                        Timestamp dt = transManager.getAvailableDate(new Timestamp(date.getTime()));
                        TransactionDto previousTransaction = transManager.findPrevious(clientId.get(i), currencyId.get(i), dt, 0);
                        Double trBalance = null;
                        if (previousTransaction != null) {
                            trBalance = previousTransaction.getBalance();
                        }
                        logger.trace("trBalance: " + trBalance);
                        Double oldBalance = accountManager.getAccounts(clientName.get(i)).get(0).getCurrencies().get(currencyManager.getCurrency(currencyId.get(i))).getAmount();
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
                    logger.info("Redirecting to error page with error: " + e.getMessage() + Arrays.toString(e.getStackTrace()));
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
            Date startDate = new Date(((Timestamp) session.getAttribute("startDate" + clientId.get(0))).getTime());
            if (startDate.after(date)) {
                startDate = date;
            }
            assert startDate != null;
            session.setAttribute("startDate" + clientId.get(0), new Timestamp(startDate.getTime()));
        } catch (Exception e) {
            logger.info("Redirecting to error page with error: " + e.getMessage() + Arrays.toString(e.getStackTrace()));
            session.setAttribute("error", e.getMessage());
            return "error";
        }

        logger.info("Transaction created");
        session.setAttribute("pointer", pointer);
        return "redirect:/client_info";
    }

    /**
     * Generate page for adding new client
     *
     * @param session collect parameters for view
     */
    @GetMapping(path = "/new_client")
    public String newClient(HttpSession session) {
        logger.info("Loading new client page...");
        session.setAttribute("path", "/new_client");
        logger.info("New client page loaded");
        return "addClient";
    }

    /**
     * Generate page for editing client
     *
     * @param session  collect parameters for view
     * @param clientId id of client that need to be edited
     */
    @GetMapping(path = "/edit_client")
    public String editClient(HttpSession session, @RequestParam(name = "client_id") int clientId) {
        logger.info("Loading edit client page...");
        Client client = clientManager.getClient(clientId);
        session.setAttribute("client", client);
        session.setAttribute("path", "/edit_client");
        logger.info("Edit client page loaded");
        return "addClient";
    }

    /**
     * Save information of new o present client
     *
     * @param clientName new names for client
     * @param clientId   id of client that need to be edited
     * @param phone      new phone for client
     * @param telegram   new telegram for client
     * @param session    collect parameters for view
     */
    @PostMapping(path = "/add_client")
    public String saveNewClient(@RequestParam(name = "client_name") String clientName,
                                @RequestParam(name = "client_id", required = false) int clientId,
                                @RequestParam(name = "client_phone", required = false) String phone,
                                @RequestParam(name = "client_telegram", required = false) String telegram,
                                HttpSession session) {
        logger.info("Saving client...");
        Client client;
        if (clientId <= 0) {
            logger.debug("Client is new");
            client = new Client(clientName);
        } else {
            logger.debug("Client already present");
            client = clientManager.getClient(clientId);
            client.setPib(clientName);
        }
        if (!phone.isEmpty()) {
            logger.debug("Validating phone...");
            if (!phone.matches("\\+38 \\(0[0-9]{2}\\) [0-9]{3}-[0-9]{2}-[0-9]{2}")) {
                logger.info("Redirecting to error page with error: Номер телефона должен соответсвовать паттерну: " +
                        "+38 (011) 111-11-11");
                session.setAttribute("error", "Номер телефона должен соответсвовать паттерну: " +
                        "+38 (011) 111-11-11");
                return "error";
            }
            client.setPhone(phone);
        }
        if (!telegram.isEmpty()) {
            logger.debug("Validating telegram...");
            if (!telegram.matches("@[a-z._0-9]+")) {
                logger.info("Redirecting to error page with error: Телеграм тег должен начинаться с @ содержать " +
                        "латинские буквы нижнего регистра, цифры, \".\" или \"_\"");
                session.setAttribute("error", "Телеграм тег должен начинаться с @ содержать латинские буквы " +
                        "нижнего регистра, цифры, \".\" или \"_\"");
                return "error";
            }
            client.setTelegram(telegram);
        }
        try {
            if (session.getAttribute("path") == "/edit_client") {
                logger.debug("Updating client");
                clientManager.updateClient(client);
            }
            if (session.getAttribute("path") == "/new_client") {
                logger.debug("Adding client");
                accountManager.addClient(client);
            }
        } catch (Exception e) {
            logger.info("Redirecting to error page with error: " + e.getMessage());
            session.setAttribute("error", e.getMessage());
            return "error";
        }
        logger.info("Client saved");
        return "redirect:/client";
    }

    /**
     * Deleting client
     *
     * @param clientId id of client that need to be deleted
     * @param session  collect parameters for view
     */
    @PostMapping(path = "delete_client")
    public String deleteClient(@RequestParam(name = "id") int clientId, HttpSession session) {
        logger.info("Deleting client...");
        Client client = clientManager.getClient(clientId);
        clientManager.deleteClient(client);
        logger.info("Client deleted");
        return "redirect:/client";
    }

    /**
     * Generate page for creating new currency
     *
     * @param session collect parameters for view
     */
    @GetMapping(path = "/new_currency")
    public String newCurrency(HttpSession session) {
        logger.info("Loading new currency page...");
        session.setAttribute("path", "/new_currency");
        logger.info("New currency page loaded");
        return "addCurrency";
    }

    /**
     * Creates new currency
     *
     * @param currencyName names of the new currency
     * @param currencyId   code/id of new currency
     * @param session      collect parameters for view
     */
    @PostMapping(path = "/new_currency")
    public String saveNewCurrency(@RequestParam(name = "currency_name") String currencyName,
                                  @RequestParam(name = "currency_id") int currencyId,
                                  HttpSession session) {
        logger.info("Saving new currency...");
        if (!currencyName.matches("[A-Z]{3}")) {
            logger.info("Redirecting to error page with error: В названии должны быть три заглавные латинские буквы");
            session.setAttribute("error", "В названии должны быть три заглавные латинские буквы");
            return "error";
        }
        if (currencyId < 100 || currencyId > 999) {
            logger.info("Redirecting to error page with error: Код должен состоять из трех цифр от 0 до 9");
            session.setAttribute("error", "Код должен состоять из трех цифр от 0 до 9");
            return "error";
        }
        Currency newCurrency = new Currency();
        newCurrency.setName(currencyName);
        newCurrency.setId(currencyId);
        try {
            currencyManager.addCurrency(newCurrency);
        } catch (Exception e) {
            logger.info("Redirecting to error page with error: " + e.getMessage());
            session.setAttribute("error", e.getMessage());
            return "error";
        }
        logger.info("Currency saved");
        return "redirect:/client";
    }

    /**
     * Saved colors of some values of transaction. Data for saving gained by javascript in JSON format
     *
     * @param colors  String in JSON format that contains information about values and their colors that need to be saved
     * @param session collect parameters for view
     * @throws JsonProcessingException thrown while could not read string colors
     */
    @Transactional
    @PostMapping(path = "/save_colors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String saveColors(@RequestBody String colors, HttpSession session) throws JsonProcessingException {
        logger.info("Saving colors...");
        List<List<String>> entryList = new ObjectMapper().readValue(colors, new TypeReference<>() {
        });
        for (List<String> list : entryList) {
            String[] arr = list.get(0).split("_");
            int id = Integer.parseInt(arr[0]);
            int clientId = Integer.parseInt(arr[2]);
            int currencyId = Integer.parseInt(arr[1]);
            Transaction transaction = transManager.getTransactionByClientAndByIdAndByCurrencyId(id, clientId, currencyId);
            switch (arr[3]) {
                case "comment" -> transaction.setCommentColor(list.get(1));
                case "pAmount" -> transaction.setInputColor(list.get(1));
                case "nAmount" -> transaction.setOutputColor(list.get(1));
                case "commission" -> transaction.setTarifColor(list.get(1));
                case "com" -> transaction.setCommissionColor(list.get(1));
                case "rate" -> transaction.setRateColor(list.get(1));
                case "transportation" -> transaction.setTransportationColor(list.get(1));
                case "total" -> transaction.setAmountColor(list.get(1));
            }

            transManager.save(transaction);
        }
        logger.info("Colors saved");
        return "redirect:/client_info";
    }

    @Transactional
    @PostMapping(path = "/save_main_colors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String saveMainColors(@RequestBody String colors, HttpSession session) throws JsonProcessingException {
        logger.info("Saving colors...");
        List<List<String>> entryList = new ObjectMapper().readValue(colors, new TypeReference<>() {
        });
        for (List<String> list : entryList) {
            String[] arr = list.get(0).split("_");
            int clientId = Integer.parseInt(arr[0]);
            Client client = clientManager.getClient(clientId);
            int currencyId = -1;
            if (arr[1].matches("[0-9]{3}")) {
                currencyId = Integer.parseInt(arr[1]);
                Account account = accountManager.getAccounts(client.getPib()).get(0);
                Currency currency = currencyManager.getCurrency(currencyId);
                Map<Currency, Account.Properties> currencies = account.getCurrencies();
                Account.Properties prop = currencies.get(currency);
                prop.setColor(list.get(1));
                currencies.put(currency, prop);
                account.setCurrencies(currencies);
                accountManager.save(account);
            }
            if (currencyId == -1) {
                client.setColor(list.get(1));
                clientManager.save(client);
            }


        }
        logger.info("Colors saved");
        return "redirect:/client_info";
    }

    @GetMapping("/convertation")
    public String convertation(HttpSession session) {
        logger.info("Loading convertation page");
        session.setAttribute("path", "/convertation");
        return "convertation";
    }

    @GetMapping("/transfer")
    public String transfer(HttpSession session) {
        logger.info("Loading transfer page");
        session.setAttribute("path", "/transfer");
        return "transfer";
    }

    @GetMapping("/log_in")
    public String getLoginPage() {
        logger.info("Loading log in page");
        return "login";
    }

    @PostMapping("/log_in")
    public String login(HttpSession session, @RequestParam String login, @RequestParam String password) {
        logger.info("Checking user authorisation information...");
        User user;
        try {
            user = userManager.getUser(login);
        } catch (RuntimeException e) {
            session.setAttribute("error", e.getMessage());
            logger.info("Redirecting to error page with error: " + e.getMessage());
            return "error";
        }

        String sha256hex = Hashing.sha256()
                .hashString(password, StandardCharsets.UTF_8)
                .toString();
        if (!sha256hex.equals(user.getPassword())) {
            session.setAttribute("error", "Пароль не верный");
            logger.info("Redirecting to error page with error: Пароль не верный");
            return "error";
        }

        session.setAttribute("user", user);
        logger.info("User accepted");
        return "redirect:/client";
    }

    @PostMapping("/log_out")
    public String logout(HttpSession session) {
        logger.info("Logging out");
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/users")
    public String getUsersPage(HttpSession session) {
        logger.info("Loading users page...");
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        List<User> users = userManager.getAllUsers();
        Map<User, Integer> active = new HashMap<>();
        for (User user : users) {
            active.put(user, 0);
        }
        List<HttpSession> sessions = new HttpSessionConfig().getActiveSessions();
        for (HttpSession ses : sessions) {
            if (ses != null) {
                User user = (User) ses.getAttribute("user");
                if (user != null) {
                    active.put(user, active.get(user) + 1);
                }
            }
        }
        session.setAttribute("active", active);
        session.setAttribute("users", users);
        logger.info("Users page loaded");
        return "users";
    }

    @PostMapping("/create_user")
    public String createUser(@RequestParam(name = "login") String login, HttpSession session) {
        logger.info("Creating new user...");
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        try {
            userManager.getUser(login);
            logger.info("Redirecting to error page with error: Логин занят. Придумайте другой");
            session.setAttribute("error", "Логин занят. Придумайте другой");
            return "error";
        } catch (RuntimeException e) {
            User user = new User();
            user.setLogin(login);
            user.setPassword("d17f25ecfbcc7857f7bebea469308be0b2580943e96d13a3ad98a13675c4bfc2");
            user.setRole(Role.Manager);
            userManager.save(user);
        }
        logger.info("User created");
        return "redirect:/users";
    }

    @PostMapping("/restore_pass")
    public String restorePass(@RequestParam(name = "id") int id, HttpSession session) {
        logger.info("Restoring password...");
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        User user = userManager.getUser(id);

        String sha256hex = Hashing.sha256()
                .hashString("11111", StandardCharsets.UTF_8)
                .toString();

        user.setPassword(sha256hex);

        userManager.save(user);
        logger.info("Password restored");
        return "redirect:/users";
    }

    @PostMapping("/delete_user")
    public String deleteUser(@RequestParam(name = "id") int id, HttpSession session) {
        logger.info("Deleting user");
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        User user = userManager.getUser(id);
        if (user.getRole() == Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        userManager.delete(id);
        logger.info("User deleted");
        return "redirect:/users";
    }

    @GetMapping("/user_settings")
    public String getUserSettingsPage() {
        logger.info("Loading user settings page");
        return "userSettings";
    }

    @PostMapping("/change_login")
    public String changeLogin(@RequestParam(name = "login") String login, HttpSession session) {
        logger.info("Changing user login...");
        User user = (User) session.getAttribute("user");
        user = userManager.getUser(user.getId());
        try {
            userManager.getUser(login);
            logger.info("Redirecting to error page with error: Логин занят. Придумайте другой");
            session.setAttribute("error", "Логин занят. Придумайте другой");
            return "error";
        } catch (RuntimeException e) {
            user.setLogin(login);
            userManager.save(user);
        }

        session.setAttribute("user", user);
        logger.info("Login changed");
        return "redirect:/user_settings";
    }

    @PostMapping("/change_password")
    public String changePassword(@RequestParam(name = "old_password") String oldPassword,
                                 @RequestParam(name = "new_password") String newPassword,
                                 @RequestParam(name = "check_password") String checkPassword,
                                 HttpSession session) {
        logger.info("Changing password...");
        User user = (User) session.getAttribute("user");
        user = userManager.getUser(user.getId());
        String oldPassSha = Hashing.sha256()
                .hashString(oldPassword, StandardCharsets.UTF_8)
                .toString();
        if (!Objects.equals(user.getPassword(), oldPassSha)) {
            logger.info("Redirecting to error page with error: Неверный пароль");
            session.setAttribute("error", "Неверный пароль");
            return "error";
        }

        if (!Objects.equals(newPassword, checkPassword)) {
            logger.info("Redirecting to error page with error: Пароли не совпадают");
            session.setAttribute("error", "Пароли не совпадают");
            return "error";
        }

        String newPassSha = Hashing.sha256()
                .hashString(newPassword, StandardCharsets.UTF_8)
                .toString();
        user.setPassword(newPassSha);
        userManager.save(user);
        session.setAttribute("user", user);
        logger.info("Password changed");
        return "redirect:/user_settings";
    }

    @GetMapping("/manager_history")
    public String managerHistory(@RequestParam(name = "manager_id", required = false) Integer managerId,
                                 @RequestParam(name = "strDate", required = false) Date strDate,
                                 @RequestParam(name = "edDate", required = false) Date edDate,
                                 @RequestParam(name = "client_name", required = false) String clientName,
                                 @RequestParam(name = "currencyId", required = false) List<Integer> currencyId,
                                 HttpSession session) {
        logger.info("Loading manger history page...");
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        if (managerId == null) {
            managerId = ((User) session.getAttribute("manager")).getId();
        }
        logger.trace("managerId: " + managerId);
        if (strDate == null) strDate = Date.valueOf(LocalDate.now());
        logger.trace("strDate: " + strDate);
        if (edDate == null) edDate = new Date(strDate.getTime() + 86400000);
        logger.trace("endDate: " + edDate);
        List<Currency> currencies = currencyManager.getAllCurrencies();
        if (currencyId == null) {
            logger.debug("currencyId = null");
            currencyId = new ArrayList<>();

            for (Currency currency : currencies) {
                currencyId.add(currency.getId());
            }
            logger.trace("currencyId = " + currencyId);
        }
        List<String> currencyName = new ArrayList<>();
        for (Integer curId : currencyId) {
            currencyName.add(currencyManager.getCurrency(curId).getName());
        }
        List<Transaction> transactions = transManager.findByUser(managerId, new Timestamp(strDate.getTime()), new Timestamp(edDate.getTime() - 1), clientName, currencyId);
        User manager = userManager.getUser(managerId);
        session.setAttribute("strDate", new Timestamp(strDate.getTime()));
        session.setAttribute("edDate", new Timestamp(edDate.getTime()));
        session.setAttribute("manager", manager);
        session.setAttribute("transactions", transactions);
        session.setAttribute("currencies", currencies);
        session.setAttribute("currency_name", currencyName);
        logger.info("Manager history page loaded");
        return "managerHistory";
    }

    @GetMapping("/database")
    public String makeBackUp(HttpSession session, HttpServletResponse response) throws Exception {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        scheduler.makeBackUp();
        return "redirect:/users";
    }

    @PostMapping("/database")
    public String restoreBackUp(HttpSession session, HttpServletResponse response) throws Exception {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        scheduler.restoreFromBackUp();
        return "redirect:/users";
    }

    private void getZip(File file, ZipOutputStream zout) throws IOException {
        ZipEntry entry1 = new ZipEntry(file.getName());
        zout.putNextEntry(entry1);
        if (file.isDirectory()) {
            File[] arFiles = file.listFiles();
            assert arFiles != null;
            for (File f : arFiles) {
                getZip(f, zout);
            }
        } else {
            FileInputStream fis = new FileInputStream(file);
            // считываем содержимое файла в массив byte
            byte[] buffer = new byte[fis.available()];
            int i = fis.read(buffer);
            // добавляем содержимое к архиву
            zout.write(buffer);
            zout.closeEntry();
        }
    }

    @GetMapping("/recashe")
    public String getRefreshPage(HttpSession session) {
        logger.info("Loading refresh cashe page...");
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        session.setAttribute("clients", clientManager.findAll());
        logger.info("Refresh page loaded");
        return "recashe";
    }

    @PostMapping("/recashe")
    public String refreshCashe(@RequestParam(name = "date") Date date,
                               @RequestParam(name = "client", required = false) String clientName,
                               HttpSession session) {
        logger.info("Updating balance values");
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        List<Account> accounts = new ArrayList<>();
        if (clientName.isEmpty()) {
            logger.debug("client name is empty");
            accounts = accountManager.getAllAccounts();
        } else {
            logger.debug("client name found");
            accounts.add(accountManager.getAccounts(clientName).get(0));
        }
        logger.info("Found " + accounts.size() + " accounts");
        for (Account account : accounts) {
            logger.info("Updating " + account.getClient().getPib());
            Map<Currency, Account.Properties> currencies = account.getCurrencies();

            List<Integer> currenciesId = currencies.keySet().stream().map(Currency::getId).toList();
            List<Transaction> transactions = transManager.findByClientForDate(account.getClient().getId(), currenciesId, new Timestamp(date.getTime()), new Timestamp(System.currentTimeMillis()));
            logger.trace("transactions: " + transactions);
            for (Currency currency : currencies.keySet()) {
                Optional<Transaction> trOpt = transactions.stream().filter(t -> Objects.equals(t.getCurrency().getId(), currency.getId())).findFirst();
                if (trOpt.isEmpty()) continue;
                Transaction first = trOpt.get();
                logger.trace("first: " + first);
                TransactionDto previous = transManager.findPrevious(account.getClient().getId(), currency.getId(), new Timestamp(date.getTime() - 1), first.getId());
                logger.trace("previous: " + previous);
                Double previousBalance = 0.0;
                if (previous != null) {
                    previousBalance = previous.getBalance();
                }
                logger.trace("previousBalance: " + previousBalance);
                currencies.put(currency, new Account.Properties(previousBalance, null));
                account.setCurrencies(currencies);
                accountManager.save(account);
            }
            for (Transaction t : transactions) {
                transManager.remittance(t.getId(), t.getDate(), t.getClient().getPib(), t.getComment(), t.getCurrency().getId(), t.getRate(), t.getCommission(),
                        t.getAmount(), t.getTransportation(), t.getCommentColor(), t.getAmountColor(), t.getUser().getId(), 0.0,
                        t.getInputColor(), t.getOutputColor(), t.getTarifColor(), t.getCommissionColor(), t.getRateColor(), t.getTransportationColor());
            }

        }
        logger.info("Data updated");
        return "redirect:/client";
    }

    @PostMapping("/undo")
    public String undo(@RequestParam(name = "id", required = false, defaultValue = "-1") int id, HttpSession session) {
        logger.info("Starting undoing...");
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
        logger.info("Starting redoing...");
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
}
