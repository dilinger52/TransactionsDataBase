package org.profinef.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.profinef.dto.TransactionDto;
import org.profinef.entity.*;
import org.profinef.entity.Currency;
import org.profinef.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

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
                                         @RequestParam(name = "currency_id", required = false) List<Integer> currencyId,
                                         @RequestParam(name = "startDate", required = false) Date startDate,
                                         @RequestParam(name = "endDate", required = false) Date endDate,
                                         HttpSession session) {
        logger.info("Loading client info page...");
        session.removeAttribute("transaction");
        if (clientId == 0) {
            logger.debug("clientId = 0");
            if (session.getAttribute("client") == null) {
                logger.debug("client = null");
                logger.info("Redirecting to main page");
                return "redirect:/client";
            }
            clientId = ((Client) session.getAttribute("client")).getId();
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
        if (startDate == null) startDate = Date.valueOf(LocalDate.now());
        if (endDate == null) endDate = new Date(startDate.getTime() + 86400000);
        logger.trace("startDate = " + startDate);
        logger.trace("endDate = " + endDate);
        Client client = clientManager.getClient(clientId);
        List<Transaction> transactions = transManager.findByClientForDate(clientId, currencyId,
                new Timestamp(startDate.getTime()), new Timestamp(endDate.getTime() - 1));
        System.out.println(transactions);
        Set<String> comments = transManager.getAllComments();
        Map<String, Double> total = new HashMap<>();
        List<Currency> currencies = currencyManager.getAllCurrencies();
        Map<Integer, List<Integer>> transactionIds = new HashMap<>();
        for (Currency currency : currencies) {
            List<Integer> list = transactions.stream().filter(transaction -> transaction.getCurrency()
                    .getId().equals(currency.getId())).map(Transaction::getId).sorted().collect(Collectors.toList());
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

        System.out.println(transactionIds);
        List<Transaction> list = new ArrayList<>();
        for (Transaction tr : transactions) {

            List<Transaction> l = transManager.findById(tr.getId());
            for (Transaction t : l) {
                if (!list.contains(t)) {
                    list.add(t);
                }
            }

        }
        System.out.println(list);
        list.sort((o1, o2) -> {
            if (Objects.equals(o1.getId(), o2.getId())) {
                if (Objects.equals(o1.getClient().getId(), client.getId())) return -1;
                if (Objects.equals(o2.getClient().getId(), client.getId())) return 1;
            }
            return 0;
        });
        System.out.println(list);
        transactions = list;

        logger.trace("total = " + total);
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
        //List<Transaction> transactions = transManager.getAllTransactions();
        session.setAttribute("path", "/client");
        session.setAttribute("currencies", currencies);
        //session.setAttribute("transactions", transactions);
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
        for (int i = 0; i < positiveAmount.size(); i++) {
            amount.add(positiveAmount.get(i) + negativeAmount.get(i));
        }
        if (comment.size() == 0) {
            comment = new ArrayList<>();
            for (String name : clientName) {
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
            for (Transaction transaction : transactionList) {
                Map<Currency, Double> oldBalance = new TreeMap<>();
                if (oldBalances.containsKey(transaction.getClient())) {
                    oldBalance = oldBalances.get(transaction.getClient());
                }
                Double b = accountManager.getAccount(transaction.getClient().getPib()).getCurrencies().get(transaction.getCurrency());
                System.out.println("oldbalance" + transaction.getClient().getPib() + " " + transaction.getCurrency().getName() + " " +  b);
                oldBalance.put(transaction.getCurrency(), b);
                oldBalances.put(transaction.getClient(), oldBalance);
            }
            for (int i = 0; i < clientName.size(); i++) {
                Map<Currency, Double> oldBalance = new TreeMap<>();
                if (oldBalances.containsKey(clientManager.getClient(clientName.get(i)))) {
                    oldBalance = oldBalances.get(clientManager.getClient(clientName.get(i)));
                }
                Double b = accountManager.getAccount(clientName.get(i)).getCurrencies().get(currencyManager.getCurrency(currencyId.get(i)));
                oldBalance.put(currencyManager.getCurrency(currencyId.get(i)), b);
                oldBalances.put(clientManager.getClient(clientName.get(i)), oldBalance);
            }
            for (int i = 0; i < clientName.size(); i++) {
                Double balance = transManager.findPrevious(clientId.get(i), currencyId.get(i), tr.getDate()).getBalance();
                transManager.update(tr.getId(), tr.getDate(), clientName.get(i), comment.get(i),
                        currencyId.get(i), rate.get(i), commission.get(i), amount.get(i),
                        transportation.get(i), null, null, null, tr.getUser().getId(), balance);
            }
            for (int i = 0; i < clientName.size(); i++) {
                Map<Currency, Double> newBalance = new TreeMap<>();
                if (newBalances.containsKey(clientManager.getClient(clientName.get(i)))) {
                    newBalance = newBalances.get(clientManager.getClient(clientName.get(i)));
                }
                Double b = accountManager.getAccount(clientName.get(i)).getCurrencies().get(currencyManager.getCurrency(currencyId.get(i)));
                System.out.println("newbalance" + clientName.get(i) + " " + currencyName.get(i) + " " +  b);
                newBalance.put(currencyManager.getCurrency(currencyId.get(i)), b);
                newBalances.put(clientManager.getClient(clientName.get(i)), newBalance);
                /*if (transManager.getTransactionByClientAndByIdAndByCurrencyId(transactionId.get(i), clientId.get(i),
                        currencyId.get(i)) == null) {
                    logger.debug("Found new client in transaction id=" + clientId.get(i));
                    //clientCurrency.put(clientId.get(i), currencyId.get(i));
                    Transaction tr = transManager.getTransaction(transactionId.get(i)).get(0);

                    transManager.remittance(tr.getId(), tr.getDate(), clientName.get(i), comment.get(i),
                            currencyId.get(i), rate.get(i), commission.get(i), amount.get(i),
                            transportation.get(i), null, null, null);
                    List<Integer> cur = new ArrayList<>();
                    cur.add(currencyId.get(i));
                    transManager.updateNext(clientName.get(i), cur, amount.get(i), tr.getDate());
                } else {
                    transManager.update(transactionId.get(i), clientName.get(i), comment.get(i), currencyId.get(i), rate.get(i),
                            commission.get(i), amount.get(i), transportation.get(i));
                }*/

            }
            for (Transaction transaction : transactionList) {
                Map<Currency, Double> newBalance = new TreeMap<>();
                if (newBalances.containsKey(transaction.getClient())) {
                    newBalance = newBalances.get(transaction.getClient());
                }
                Double b = accountManager.getAccount(transaction.getClient().getPib()).getCurrencies().get(transaction.getCurrency());
                newBalance.put(transaction.getCurrency(), b);
                newBalances.put(transaction.getClient(), newBalance);

            }
            System.out.println("oldBalances" + oldBalances);
            System.out.println("newBalances" + newBalances);
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
                    System.out.println("old balance" + v);
                    System.out.println("new balance" + newBalances.get(key).get(k));
                    transManager.updateNext(key.getPib(), o, newBalances.get(key).get(k) - v, tr.getDate());
                }
            }
            /*int[] ids = transManager.getTransaction(transactionId.get(0)).stream()
                    .flatMapToInt(t -> IntStream.of(t.getClient().getId())).filter(id -> !clientId.contains(id)).toArray();
            List<Transaction> transactionArrayList = new ArrayList<>();
            for (int id : ids) {
                transactionArrayList.add(transManager.findByIdAndClientIdOrderByDate(transactionId.get(0), id));
            }
            transManager.deleteTransaction(transactionArrayList);*/
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
        List<Double> amount = new ArrayList<>();
        for (int i = 0; i < positiveAmount.size(); i++) {
            amount.add(positiveAmount.get(i) + negativeAmount.get(i));
        }
        User user = (User) session.getAttribute("user");
        logger.info("Creating new transaction...");
        if (session.getAttribute("path") == "/convertation" ||
                session.getAttribute("path") == "/transfer") {
            logger.debug("Currency convertation found");
            amount.set(0, amount.get(0) * -1);
        }
        System.out.println(amount);
        try {
            /*if (amount.stream().filter(a -> a > 0).count() > 1) {
                logger.debug("Found several positive amounts");
                if (amount.stream().filter(a -> a < 0).count() > 1) {
                    logger.debug("Found several negative amounts");
                    session.setAttribute("error", "Похоже, что данная транзакция может быть заменена на " +
                            "несколько более простых. Пожалуйста выполните их отдельно");
                    logger.info("Redirecting to error page with error: Похоже, что данная транзакция может быть" +
                            " заменена на несколько более простых. Пожалуйста выполните их отдельно");
                    return "error";
                }
            }*/
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
            /*int transactionId = transManager.getAllTransactions()
                    .stream()
                    .flatMapToInt(t -> IntStream.of(t.getId()))
                    .max()
                    .orElse(0) + 1;
            logger.trace("transactionId = " + transactionId);*/
            List<Integer> currencyId = new ArrayList<>();
            for (String name : currencyName) {
                currencyId.add(currencyManager.getCurrency(name).getId());
            }
            System.out.println("date" + date);
            System.out.println(new Date(LocalDate.now().atStartOfDay(systemDefault()).toInstant().toEpochMilli()));
            System.out.println(date.before(new Date(LocalDate.now().atStartOfDay(systemDefault()).toInstant().toEpochMilli())));
            for (int i = 0; i < clientName.size(); i++) {
                if (amount.get(i) == 0) continue;
                try {
                    if (date != null && date.before(new Date(LocalDate.now().atStartOfDay(systemDefault()).toInstant().toEpochMilli()))) {
                        Double oldBalance = accountManager.getAccount(clientName.get(i)).getCurrencies().get(currencyManager.getCurrency(currencyId.get(i)));
                        Double newBalance = transManager.remittance(transactionId, new Timestamp(date.getTime()), clientName.get(i), comment.get(i),
                                currencyId.get(i), rate.get(i), commission.get(i), amount.get(i), transportation.get(i),
                                null, null, null, user.getId());
                        List<Integer> list = new ArrayList<>();
                        list.add(currencyId.get(i));
                        transManager.updateNext(clientName.get(i), list, newBalance - oldBalance, new Timestamp(date.getTime()));
                    } else {
                        transManager.remittance(transactionId, null, clientName.get(i), comment.get(i),
                                currencyId.get(i), rate.get(i), commission.get(i), amount.get(i), transportation.get(i),
                                null, null, null, user.getId());
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
            String[] arrList = list.get(0).split("_");
            Transaction transaction = transManager.getTransactionByClientAndByIdAndByCurrencyId(
                    Integer.parseInt(arrList[1]), Integer.parseInt(arrList[2]), Integer.parseInt(arrList[3]));
            switch (arrList[0]) {
                case "pib" -> transaction.setPibColor(list.get(1));
                case "amount" -> transaction.setAmountColor(list.get(1));
                case "balance" -> transaction.setBalanceColor(list.get(1));
            }
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
        session.removeAttribute("user");
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
                                 @RequestParam(name = "startDate", required = false) Date startDate,
                                 @RequestParam(name = "endDate", required = false) Date endDate,
                                 @RequestParam(name = "client_name", required = false) String clientName,
                                 @RequestParam(name = "currencyId", required = false) List<Integer> currencyId,
                                 HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        if (managerId == null) {
            managerId = ((User) session.getAttribute("manager")).getId();
        }
        if (startDate == null) startDate = Date.valueOf(LocalDate.now());
        if (endDate == null) endDate = new Date(startDate.getTime() + 86400000);
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
        for (Integer curId :
                currencyId) {
            currencyName.add(currencyManager.getCurrency(curId).getName());
        }
        List<Transaction> transactions = transManager.findByUser(managerId, new Timestamp(startDate.getTime()), new Timestamp(endDate.getTime() - 1), clientName, currencyId);
        User manager = userManager.getUser(managerId);
        session.setAttribute("startDate", startDate);
        session.setAttribute("endDate1", endDate);
        session.setAttribute("manager", manager);
        session.setAttribute("transactions", transactions);
        session.setAttribute("currencies", currencies);
        session.setAttribute("currency_name", currencyName);
        return "managerHistory";
    }

}
