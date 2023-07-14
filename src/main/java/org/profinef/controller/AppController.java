package org.profinef.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.entity.Transaction;
import org.profinef.service.AccountManager;
import org.profinef.service.ClientManager;
import org.profinef.service.CurrencyManager;
import org.profinef.service.TransManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;


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

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);

    public AppController(ClientManager clientManager, AccountManager accountManager, TransManager transManager,
                         CurrencyManager currencyManager) {
        this.clientManager = clientManager;
        this.accountManager = accountManager;
        this.transManager = transManager;
        this.currencyManager = currencyManager;
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
        return "uploadFiles";
    }

    /**
     * Generate page with clients transactions and balance for each currency. Also, this page contains fields for
     * creating new transaction
     * @param clientId id of client that information will be present
     * @param currencyId list of currencies ids to filter transactions. If id is present than transaction with this
     *                   currency will be shown. If currency id is empty than will be shown transactions for all
     *                   currencies
     * @param date the date to filter transactions. Transactions will be shown only for this day
     * @param session collect parameters for view
     */
    @GetMapping("/client_info")
    public String viewClientTransactions(@RequestParam(name = "client_id", required = false, defaultValue = "0")
                                             int clientId,
                                         @RequestParam(name = "currency_id", required = false) List<Integer> currencyId,
                                         @RequestParam(name = "date", required = false) Date date,
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
        if (date == null) date = Date.valueOf(LocalDate.now());
        logger.trace("date = " + date);
        Client client = clientManager.getClient(clientId);
        List<Transaction> transactions = transManager.findByClientForDate(clientId, currencyId,
                new Timestamp(date.getTime()));
        Set<String> comments = transManager.getAllComments();
        Map<String, Double> total = new HashMap<>();
        List<Currency> currencies = currencyManager.getAllCurrencies();
        for (Currency currency : currencies) {
            List<Transaction> transactionList = transactions.stream().filter(transaction -> transaction.getCurrency()
                    .getId().equals(currency.getId())).toList();
            if (transactionList.isEmpty()) {
                total.put("balance" + currency.getId(), (double) 0);
            } else {
                total.put("balance" + currency.getId(), transactionList.get(transactionList.size() - 1).getBalance());
            }
            double sum = 0;
            for (Transaction transaction : transactionList) {
                sum += transaction.getAmount();
                List<Transaction> tl = transManager.getTransaction(transaction.getId());
                StringBuilder clients = new StringBuilder();
                for (Transaction t : tl) {
                    if (!t.getClient().getId().equals(transaction.getClient().getId())) {
                        clients.append(" ");
                        clients.append(t.getClient().getPib());
                    }
                }
                transaction.setClient(new Client(clients.toString()));
            }
            total.put("amount" + currency.getId(), sum);
        }
        List<Transaction> transaction = new ArrayList<>();
        Transaction tr = new Transaction();
        tr.setClient(client);
        tr.setCurrency(new Currency());
        tr.setRate(1.0);
        tr.setCommission(0.0);
        tr.setTransportation(0.0);
        transaction.add(tr);
        logger.trace("total = " + total);
        session.setAttribute("transaction", transaction);
        session.setAttribute("comments", comments);
        session.setAttribute("path", "/client_info");
        session.setAttribute("currencies", currencies);
        session.setAttribute("total", total);
        session.setAttribute("date", date);
        session.setAttribute("client", client);
        session.setAttribute("transactions", transactions);
        session.setAttribute("currency_name", currencyName);
        logger.info("Client info page loaded");
        return "clientInfo";
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
     * @param clientName parameter to filter clients by name. If is null than filter is not uses
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
            logger.debug("Filtering clients by name");
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
            clients = accountManager.getAllAccounts();
            clients.add(transManager.addTotal(clients));
        }


        List<Currency> currencies = currencyManager.getAllCurrencies();
        List<Transaction> transactions = transManager.getAllTransactions();
        session.setAttribute("path", "/client");
        session.setAttribute("currencies", currencies);
        session.setAttribute("transactions", transactions);
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
    public String editTransactionPage(@RequestParam(name = "transaction_id") int transactionId,
                                      HttpSession session) {
        logger.info("Loading edit transaction page...");
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
     * @param currencyName new currencies name to save to transaction
     * @param rate new rates to save to transaction
     * @param commission new commissions to save to transaction
     * @param amount new amounts to save to transaction
     * @param transportation new transportations to save to transaction
     * @param session collect parameters for view
     */
    @Transactional
    @PostMapping(path = "/edit")
    public String editTransaction(@RequestParam(name = "transaction_id") List<Integer> transactionId,
                                  @RequestParam(name = "client_name") List<String> clientName,
                                  @RequestParam(name = "comment") List<String> comment,
                                  @RequestParam(name = "currency_name") List<String> currencyName,
                                  @RequestParam(name = "rate") List<Double> rate,
                                  @RequestParam(name = "commission") List<Double> commission,
                                  @RequestParam(name = "amount") List<Double> amount,
                                  @RequestParam(name = "transportation") List<Double> transportation,
                                  HttpSession session) {
        logger.info("Saving transaction after editing...");
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
            for (int i = 0; i < clientName.size(); i++) {
                if (transManager.getTransactionByClientAndByIdAndByCurrencyId(transactionId.get(i), clientId.get(i),
                        currencyId.get(i)) == null) {
                    logger.debug("Found new client in transaction id=" + clientId.get(i));
                    Transaction tr = transManager.getTransactionByClientAndByIdAndByCurrencyId(transactionId.get(i - 1),
                            clientId.get(i - 1),  currencyId.get(i - 1));

                    transManager.remittance(tr.getId(), tr.getDate(), clientName.get(i), comment.get(i),
                            currencyId.get(i), rate.get(i), commission.get(i), amount.get(i),
                            transportation.get(i), null, null, null);
                    List<Integer> cur = new ArrayList<>();
                    cur.add(currencyId.get(i));
                    transManager.updateNext(clientName.get(i), cur, amount.get(i), tr.getDate());
                } else {
                    transManager.update(transactionId.get(i), clientName.get(i), comment.get(i), currencyId.get(i), rate.get(i),
                            commission.get(i), amount.get(i), transportation.get(i));
                }
            }
        } catch (Exception e) {
            logger.info("Redirecting to error page with error: " + e.getMessage());
            session.setAttribute("error", e.getMessage());
            return "error";
        }
        session.setAttribute("path", "/edit");
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
        session.setAttribute("path", "/delete_transaction");
        logger.info("Transaction deleted");
        return "redirect:/client_info";
    }

    /**
     * Saves new transaction to database
     * @param clientName name of client that is a member of this transaction
     * @param currencyName name of currency that changed by this transaction
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
                                @RequestParam(name = "amount") List<Double> amount,
                                @RequestParam(name = "transportation") List<Double> transportation,
                                HttpSession session) {
        logger.info("Creating new transaction...");
        try {
            if (amount.stream().filter(a -> a > 0).count() > 1) {
                logger.debug("Found several positive amounts");
                if (amount.stream().filter(a -> a < 0).count() > 1) {
                    logger.debug("Found several negative amounts");
                    session.setAttribute("error", "Похоже, что данная транзакция может быть заменена на " +
                            "несколько более простых. Пожалуйста выполните их отдельно");
                    logger.info("Redirecting to error page with error: Похоже, что данная транзакция может быть" +
                            " заменена на несколько более простых. Пожалуйста выполните их отдельно");
                    return "error";
                }
            }
            if (comment.size() == 0) {
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
            int transactionId = transManager.getAllTransactions()
                    .stream()
                    .flatMapToInt(t -> IntStream.of(t.getId()))
                    .max()
                    .orElse(0) + 1;
            logger.trace("transactionId = " + transactionId);
            for (int i = 0; i < clientName.size(); i++) {
                try {
                    List<Integer> currencyId = new ArrayList<>();
                    for (String name : currencyName) {
                        currencyId.add(currencyManager.getCurrency(name).getId());
                    }
                    transManager.remittance(transactionId, null, clientName.get(i), comment.get(i),
                            currencyId.get(i), rate.get(i), commission.get(i), amount.get(i), transportation.get(i),
                            null, null, null);
                } catch (Exception e) {
                    logger.info("Redirecting to error page with error: " + Arrays.toString(e.getStackTrace()));
                    session.setAttribute("error", e.getMessage());
                    return "error";
                }
            }
        } catch (Exception e) {
            logger.info("Redirecting to error page with error: " + e.getMessage());
            session.setAttribute("error", e.getMessage());
            return "error";
        }
        session.setAttribute("path", "/transaction");
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
     * @param clientName new name for client
     * @param clientId id of client that need to be edited
     * @param phone new phone for client
     * @param telegram new telegram for client
     * @param session collect parameters for view
     */
    @PostMapping(path = "/new_client")
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
        session.setAttribute("path", "/new_client");
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
        session.setAttribute("path", "/delete_client");
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
     * @param currencyName name of the new currency
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
        session.setAttribute("path", "/new_currency");
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
        session.setAttribute("path", "/save_colors");
        logger.info("Colors saved");
        return "redirect:/client_info";
    }

    @GetMapping("/convertation")
    public String convertation() {
        return "convertation";
    }

}
