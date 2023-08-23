package org.profinef.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.profinef.dto.TransactionDto;
import org.profinef.entity.*;
import org.profinef.entity.Currency;
import org.profinef.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.*;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.time.ZoneId.systemDefault;


/**
*This is the main class that work with visuals
 **/
@Controller
public class AppController {
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

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);

    public AppController(ClientManager clientManager, AccountManager accountManager, TransManager transManager,
                         CurrencyManager currencyManager, UserManager userManager) {
        this.clientManager = clientManager;
        this.accountManager = accountManager;
        this.transManager = transManager;
        this.currencyManager = currencyManager;
        this.userManager = userManager;
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
     * @param session needed for set path parameter. It uses for back button
     *
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
     * @param clientId id of client that information will be present
     * @param currencyId list of currencies ids to filter transactions. If id is present than transaction with this
     *                   currency will be shown. If currency id is empty than will be shown transactions for all
     *                   currencies
     * @param startDate the date to filter transactions. Transactions will be shown from this day
     * @param endDate the date to filter transactions. Transactions will be shown to this day
     * @param session collect parameters for view
     */
    @GetMapping("/client_info")
    public String viewClientTransactions(@RequestParam(name = "client_id", required = false, defaultValue = "0")
                                             int clientId,
                                         @RequestParam(name = "client_name", required = false)
                                         String clientName,
                                         @RequestParam(name = "currency_id", required = false) List<Integer> currencyId,
                                         @RequestParam(name = "startDate", required = false) Date startDate,
                                         @RequestParam(name = "endDate", required = false) Date endDate,
                                         HttpSession session) {
        logger.info("Loading client info page...");
        session.removeAttribute("transaction");
        session.removeAttribute("client_alert");
        if (clientId == 0) {
            logger.debug("clientId = 0");
            if (clientName != null) {
                clientId = clientManager.getClient(clientName).getId();
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
            if (session.getAttribute("startDate") == null) {
                logger.debug("session start date is null");
                startDate = Date.valueOf(LocalDate.now());
            } else {
                startDate = new Date(((Timestamp) session.getAttribute("startDate")).getTime());
            }
        }
        if (endDate == null) {
            logger.debug("requested end date is null");
            if (session.getAttribute("endDate1") == null) {
                logger.debug("requested end date is null");
                endDate = new Date(startDate.getTime() + 86400000);
            } else {
                endDate = new Date(((Timestamp) session.getAttribute("endDate1")).getTime());
            }
        }
        logger.trace("startDate = " + startDate);
        logger.trace("endDate = " + endDate);
        Client client = clientManager.getClient(clientId);
        List<Transaction> transactions = transManager.findByClientForDate(clientId, currencyId,
                new Timestamp(startDate.getTime()), new Timestamp(endDate.getTime() - 1));
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
            TransactionDto transactionDto = transManager.findPrevious(clientId, currency.getId(), new Timestamp(startDate.getTime()));
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
            if (ses.getAttribute("path") == "/client_info" && ses.getAttribute("client").equals(client) && ses.getAttribute("user") != session.getAttribute("user")) {
               session.setAttribute("client_alert", "Другой пользователь просматривает и/или редактирует данного клиента. Для получения актуальных данных дождитесь окончания работы данного пользователя");
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
        session.setAttribute("startDate", new Timestamp(startDate.getTime()));
        session.setAttribute("endDate", new Timestamp(endDate.getTime() - 1));
        session.setAttribute("endDate1", new Timestamp(endDate.getTime()));
        session.setAttribute("client", client);
        session.setAttribute("transactions", transactions);
        session.setAttribute("transactionIds", transactionIds);
        session.setAttribute("currency_name", currencyName);
        logger.info("Client info page loaded");
        return "clientInfo2";
    }

    /**
     * Generate page that contains fields to collect information about new transaction
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
     * @param clientName parameter to filter clients by names. If is null than filter is not uses
     * @param clientPhone parameter to filter clients by phone. If is null than filter is not uses
     * @param clientTelegram parameter to filter clients by telegram. If is null than filter is not uses
     * @param session collect parameters for view
     */
    @GetMapping(path = "/client")
    public String getClientsInfo(@RequestParam(name = "client_name", required = false) String clientName,
                                 @RequestParam(name = "client_phone", required = false) String clientPhone,
                                 @RequestParam(name = "client_telegram", required = false) String clientTelegram,
                                 HttpSession session) {
        logger.info("Loading main page...");
        session.removeAttribute("error");
        session.removeAttribute("client");
        List<Account> clients;
        if (clientName != null && !clientName.isEmpty()) {
            logger.debug("Filtering clients by names");
            clients = new ArrayList<>();
            try {
                clients.add(accountManager.getAccount(clientName));
            } catch (Exception e) {
                session.setAttribute("error", e.getMessage());
                logger.info("Redirected to error page with error: " + e.getMessage());
                return "error";
            }
        } else if (clientPhone != null && !clientPhone.isEmpty()) {
            logger.debug("Filtering clients by phone");
            clients = new ArrayList<>();
            try {
                clients.add(accountManager.getAccountByPhone(clientPhone));
            } catch (Exception e) {
                session.setAttribute("error", e.getMessage());
                logger.info("Redirected to error page with error: " + e.getMessage());
                return "error";
            }
        } else if (clientTelegram != null && !clientTelegram.isEmpty()) {
            logger.debug("Filtering clients by telegram");
            clients = new ArrayList<>();
            try {
                clients.add(accountManager.getAccountByTelegram(clientTelegram));
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
     * @param transactionId id of transaction for editing
     * @param session collect parameters for view
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
     * @param transactionId ids of transactions that need to be editing
     * @param clientName names of client which transaction need to be editing
     * @param comment new comments to save to transaction
     * @param currencyName new currencies names to save to transaction
     * @param rate new rates to save to transaction
     * @param commission new commissions to save to transaction
     * @param amount new amounts to save to transaction
     * @param transportation new transportations to save to transaction
     * @param session collect parameters for view
     */
    @Transactional
    @PostMapping(path = "/edit_transaction")
    public String editTransaction(@RequestParam(name = "transaction_id") List<Integer> transactionId,
                                  @RequestParam(name = "client_name") List<String> clientName,
                                  @RequestParam(name = "comment") List<String> comment,
                                  @RequestParam(name = "currency_name") List<String> currencyName,
                                  @RequestParam(name = "rate") List<Double> rate,
                                  @RequestParam(name = "commission") List<Double> commission,
                                  @RequestParam(name = "positiveAmount") List<Double> positiveAmount,
                                  @RequestParam(name = "negativeAmount") List<Double> negativeAmount,
                                  @RequestParam(name = "transportation") List<Double> transportation,
                                  HttpSession session) {
        logger.info("Saving transaction after editing...");
        List<Double> amount = new ArrayList<>();
        System.out.println(positiveAmount);
        System.out.println(negativeAmount);
        int size = positiveAmount.size();
        if (size < negativeAmount.size()) size = negativeAmount.size();
        for (int i = 0; i < size; i++) {
            if (positiveAmount.size() < size) {
                positiveAmount.add(0.0);
                if (positiveAmount.get(i) == null) {
                    positiveAmount.set(i, 0.0);
                }
            }
            if (negativeAmount.size() < size) {
                negativeAmount.add(0.0);
                if (negativeAmount.get(i) == null) {
                    negativeAmount.set(i, 0.0);
                }
            }
            if (positiveAmount.get(i) != 0 && positiveAmount.get(i) + negativeAmount.get(i) == 0) {
                logger.info("Redirecting to error page with error: Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
                session.setAttribute("error", "Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
                return "error";
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
                clientId.add(clientManager.getClient(name).getId());
            }
            List<Transaction> transactionList = transManager.getTransaction(transactionId.get(0));
            Map<Client, Map<Currency, Double>> oldBalances = new TreeMap<>();
            Map<Client, Map<Currency, Double>> newBalances = new TreeMap<>();
            Transaction tr = transactionList.get(0);

            transManager.deleteTransaction(transactionList);
            for (int i = 0; i < clientName.size(); i++) {
                if (commission.size() < clientName.size()) {
                    commission.add(0.0);
                    if (commission.get(i) == null) {
                        commission.set(i, 0.0);
                    }
                }
                if (rate.size() < clientName.size()) {
                    rate.add(0.0);
                    if (rate.get(i) == null) {
                        rate.set(i, 0.0);
                    }
                }
                if (transportation.size() < clientName.size()) {
                    transportation.add(0.0);
                    if (transportation.get(i) == null) {
                        transportation.set(i, 0.0);
                    }
                }

                Map<Currency, Double> oldBalance = new TreeMap<>();
                if (oldBalances.containsKey(clientManager.getClient(clientName.get(i)))) {
                    oldBalance = oldBalances.get(clientManager.getClient(clientName.get(i)));
                }
                TransactionDto previous = transManager.findPrevious(clientId.get(i), currencyId.get(i), tr.getDate());
                Double b = 0.0;
                if (previous != null) b = previous.getBalance();
                oldBalance.put(currencyManager.getCurrency(currencyId.get(i)), b);
                oldBalances.put(clientManager.getClient(clientName.get(i)), oldBalance);
            }
            for (int i = 0; i < clientName.size(); i++) {
                TransactionDto previous = transManager.findPrevious(clientId.get(i), currencyId.get(i), tr.getDate());
                Double balance = 0.0;
                if (previous != null) balance = previous.getBalance();
                Double b = transManager.update(tr.getId(), tr.getDate(), clientName.get(i), comment.get(i),
                        currencyId.get(i), rate.get(i), commission.get(i), amount.get(i),
                        transportation.get(i), null, null, null, tr.getUser().getId(), balance);
                Map<Currency, Double> newBalance = new TreeMap<>();
                if (newBalances.containsKey(clientManager.getClient(clientName.get(i)))) {
                    newBalance = newBalances.get(clientManager.getClient(clientName.get(i)));
                }
                newBalance.put(currencyManager.getCurrency(currencyId.get(i)), b);
                newBalances.put(clientManager.getClient(clientName.get(i)), newBalance);
            }
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
        } catch (Exception e) {
            logger.info("Redirecting to error page with error: " + Arrays.toString(e.getStackTrace()));
            session.setAttribute("error", e.getMessage());
            return "error";
        }
        logger.info("Transaction saved");
        return "redirect:/client_info";
    }

    /**
     * Deleting transaction from database
     * @param transactionId id of transaction that need to be deleted
     * @param session collect parameters for view
     */
    @Transactional
    @PostMapping(path = "delete_transaction")
    public String deleteTransaction(@RequestParam(name = "transaction_id") int transactionId, HttpSession session) {
        logger.info("Deleting transaction...");
        List<Transaction> transaction = transManager.getTransaction(transactionId);
        transManager.deleteTransaction(transaction);
        logger.info("Transaction deleted");
        return "redirect:/client_info";
    }

    /**
     * Saves new transaction to database
     * @param clientName names of client that is a member of this transaction
     * @param currencyName names of currency that changed by this transaction
     * @param comment comment about this transaction
     * @param rate exchange rate of currency for this transaction and this client
     * @param commission commission for this client for this transaction
     * @param amount amount of currency to be changed
     * @param transportation external commission if money will be transferred for client
     * @param session collect parameters for view
     */

    @PostMapping(path = "/transaction")
    public String doTransaction(@RequestParam(name = "client_name") List<String> clientName,
                                @RequestParam(name = "currency_name") List<String> currencyName,
                                @RequestParam(name = "comment", required = false) List<String> comment,
                                @RequestParam(name = "rate") List<Double> rate,
                                @RequestParam(name = "commission") List<Double> commission,
                                @RequestParam(name = "positiveAmount") List<Double> positiveAmount,
                                @RequestParam(name = "negativeAmount") List<Double> negativeAmount,
                                @RequestParam(name = "transportation") List<Double> transportation,
                                @RequestParam(name = "date", required = false) Date date,
                                HttpSession session) {
        logger.info("Creating new transaction...");
        List<Double> amount = new ArrayList<>();
        for (int i = 0; i < positiveAmount.size(); i++) {
            amount.add(positiveAmount.get(i) + negativeAmount.get(i));
        }

        if (session.getAttribute("path") == "/convertation" || session.getAttribute("path") == "/transfer") {
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
                } else if(currencyDouble.contains(currencyName.get(i))) {
                    session.setAttribute("error", "Обнаружено наличие как минимум двух позиций с совпадением " +
                            "значений клиента и валюты. Подобная операция не допустима");
                    logger.info("Redirecting to error page with error: Обнаружено наличие как минимум двух позиций " +
                            "с совпадением значений клиента и валюты. Подобная операция не допустима");
                    return "error";
                }
            }
            int transactionId = transManager.getMaxId() + 1;
            logger.trace("transactionId: " + transactionId);
            List<Integer> currencyId = new ArrayList<>();
            for (String name : currencyName) {
                currencyId.add(currencyManager.getCurrency(name).getId());
            }
            List<Integer> clientId = new ArrayList<>();
            for (String name : clientName) {
                clientId.add(clientManager.getClient(name).getId());
            }
            for (int i = 0; i < clientName.size(); i++) {
                if (amount.get(i) == 0) continue;
                try {
                    if (date != null && date.before(new Date(LocalDate.now().atStartOfDay(systemDefault()).toInstant().toEpochMilli()))) {
                        logger.debug("Date before today date");
                        date = new Date(date.getTime() + 86400000 - 1000);
                        TransactionDto previousTransaction = transManager.findPrevious(clientId.get(i), currencyId.get(i), new Timestamp(date.getTime() + 1));
                        Double trBalance = null;
                        if (previousTransaction != null) {
                            trBalance = previousTransaction.getBalance();
                        }
                        logger.trace("trBalance: " + trBalance);
                        Double oldBalance = accountManager.getAccount(clientName.get(i)).getCurrencies().get(currencyManager.getCurrency(currencyId.get(i)));
                        logger.trace("oldBalance: " + oldBalance);
                        Double newBalance = transManager.remittance(transactionId, new Timestamp(date.getTime()), clientName.get(i), comment.get(i),
                                currencyId.get(i), rate.get(i), commission.get(i), amount.get(i), transportation.get(i),
                                null, null, null, user.getId(), trBalance);
                        logger.trace("newBalance: " + newBalance);
                        List<Integer> list = new ArrayList<>();
                        list.add(currencyId.get(i));
                        logger.trace("list: " + list);
                        transManager.updateNext(clientName.get(i), list, newBalance - oldBalance, new Timestamp(date.getTime()));
                    } else {
                        logger.debug("Date is today");
                        transManager.remittance(transactionId, null, clientName.get(i), comment.get(i),
                                currencyId.get(i), rate.get(i), commission.get(i), amount.get(i), transportation.get(i),
                                null, null, null, user.getId(), 0.0);
                    }
                } catch (Exception e) {
                    logger.info("Redirecting to error page with error: " + Arrays.toString(e.getStackTrace()));
                    session.setAttribute("error", e.getMessage());
                    return "error";
                }
            }
        } catch (Exception e) {
            logger.info("Redirecting to error page with error: " + Arrays.toString(e.getStackTrace()));
            session.setAttribute("error", e.getMessage());
            return "error";
        }
        logger.info("Transaction created");
        return "redirect:/client_info";
    }

    /**
     * Generate page for adding new client
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
     * @param session collect parameters for view
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
     * @param clientName new names for client
     * @param clientId id of client that need to be edited
     * @param phone new phone for client
     * @param telegram new telegram for client
     * @param session collect parameters for view
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
     * @param clientId id of client that need to be deleted
     * @param session collect parameters for view
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
     * @param currencyName names of the new currency
     * @param currencyId code/id of new currency
     * @param session collect parameters for view
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
     * @param colors String in JSON format that contains information about values and their colors that need to be saved
     * @param session collect parameters for view
     * @throws JsonProcessingException thrown while could not read string colors
     */
    @Transactional
    @PostMapping(path = "/save_colors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String saveColors(@RequestBody String colors, HttpSession session) throws JsonProcessingException {
        logger.info("Saving colors...");
        List<List<String>> entryList = new ObjectMapper().readValue(colors, new TypeReference<>() {});

        for (List<String> list : entryList) {
            int id = Integer.parseInt(list.get(0).substring(3));
            int clientId = ((Client) session.getAttribute("client")).getId();
            int currencyId = Integer.parseInt(list.get(0).substring(0, 3));
            Transaction transaction = transManager.getTransactionByClientAndByIdAndByCurrencyId(id, clientId, currencyId);
            transaction.setPibColor(list.get(1));

            transManager.save(transaction);
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
    public String makeBackUp(HttpSession session, HttpServletResponse response) throws IOException {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        File dir = new File("C:\\Users\\O.Hryhorenko\\Documents\\Data\\transactions"); //path указывает на директорию
        //File dir = new File("pom.xml"); //path указывает на директорию
        /*for (File file :
                lst) {*/
            try {
                ZipOutputStream zout = new ZipOutputStream(new FileOutputStream("output.zip"));
                System.out.println(zout);
                getZip(dir, zout);
                zout.close();
                // закрываем текущую запись для новой записи

            }
            catch(Exception ex){
                ex.printStackTrace();
                System.out.println(ex.getMessage());
            }
        File file = new File("output.zip");
        logger.debug("File exist");
        //get the mimetype
        String mimeType = URLConnection.guessContentTypeFromName(file.getName());
        if (mimeType == null) {
            //unknown mimetype so set the mimetype to application/octet-stream
            mimeType = "application/octet-stream";
        }

        response.setContentType(mimeType);
        logger.debug("Content type set");


        response.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() + "\""));

        //Here we have mentioned it to show as attachment
        //response.setHeader("Content-Disposition", String.format("attachment; filename=\"" + file.getName() + "\""));

        response.setContentLength((int) file.length());

        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

        FileCopyUtils.copy(inputStream, response.getOutputStream());
        /*}*/
        return "redirect:/users";
    }

    private void getZip(File file, ZipOutputStream zout) throws IOException {
        System.out.println(zout);
        System.out.println(0);
        System.out.println(file.getName());
        ZipEntry entry1 = new ZipEntry(file.getName());
        zout.putNextEntry(entry1);
        if (file.isDirectory()) {
            System.out.println(1);
            File[] arFiles = file.listFiles();
            assert arFiles != null;
            for (File f : arFiles) {
                getZip(f, zout);
            }
        } else {
            System.out.println(2);
            FileInputStream fis = new FileInputStream(file);
            System.out.println(fis);
            // считываем содержимое файла в массив byte
            byte[] buffer = new byte[fis.available()];
            int i = fis.read(buffer);
            // добавляем содержимое к архиву
            zout.write(buffer);
            zout.closeEntry();
        }
    }


}
