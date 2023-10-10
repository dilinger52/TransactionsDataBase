package org.profinef.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.profinef.config.HttpSessionConfig;
import org.profinef.dto.TransactionDto;
import org.profinef.entity.*;
import org.profinef.entity.Currency;
import org.profinef.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * This is the main class that work with visuals
 **/
@SuppressWarnings("SameReturnValue")
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
     *
     * @param session needed for set path parameter. It uses for back button
     */
    @GetMapping("/excel")
    public String loadPage(HttpSession session) {
        logger.info("Loading load files page");
        User user = (User) session.getAttribute("user");
        if (user.getRole() != Role.Admin) {
            logger.info(user + " Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        session.setAttribute("path", "/excel");
        //session.setAttribute("date", new Date(System.currentTimeMillis()));
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
                                         HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading client info page...");
        session.removeAttribute("transaction");
        session.removeAttribute("client_alert");
        if (clientId == 0) {
            logger.debug("clientId = 0");
            if (clientName != null) {
                clientId = clientManager.getClientExactly(clientName).getId();
            } else if (session.getAttribute("client") == null) {
                logger.debug("client = null");
                logger.info(user + " Redirecting to main page");
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
        logger.info(user + " Client info page loaded");
        return "clientInfo2";
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
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading main page...");
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
                logger.info(user + " Redirected to error page with error: " + e.getMessage());
                return "error";
            }
        } else if (clientPhone != null && !clientPhone.isEmpty()) {
            logger.debug("Filtering clients by phone");
            clients = new ArrayList<>();
            try {
                clients.addAll(accountManager.getAccountsByPhone(clientPhone));
            } catch (Exception e) {
                session.setAttribute("error", e.getMessage());
                logger.info(user + " Redirected to error page with error: " + e.getMessage());
                return "error";
            }
        } else if (clientTelegram != null && !clientTelegram.isEmpty()) {
            logger.debug("Filtering clients by telegram");
            clients = new ArrayList<>();
            try {
                clients.addAll(accountManager.getAccountsByTelegram(clientTelegram));
            } catch (Exception e) {
                session.setAttribute("error", e.getMessage());
                logger.info(user + " Redirected to error page with error: " + e.getMessage());
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
        logger.info(user + " Main page loaded");
        return "example";
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
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading edit client page...");
        Client client = clientManager.getClient(clientId);
        session.setAttribute("client", client);
        session.setAttribute("path", "/edit_client");
        logger.info(user + " Edit client page loaded");
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
        User user = (User) session.getAttribute("user");
        logger.info(user + " Saving client...");
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
                logger.info(user + " Redirecting to error page with error: Номер телефона должен соответсвовать паттерну: " +
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
                logger.info(user + " Redirecting to error page with error: Телеграм тег должен начинаться с @ содержать " +
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
            logger.info(user + " Redirecting to error page with error: " + e.getMessage());
            session.setAttribute("error", e.getMessage());
            return "error";
        }
        logger.info(user + " Client saved");
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
        User user = (User) session.getAttribute("user");
        logger.info(user + " Deleting client...");
        Client client = clientManager.getClient(clientId);
        clientManager.deleteClient(client);
        logger.info(user + " Client deleted");
        return "redirect:/client";
    }

    /**
     * Generate page for creating new currency
     *
     * @param session collect parameters for view
     */
    @GetMapping(path = "/new_currency")
    public String newCurrency(HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading new currency page...");
        session.setAttribute("path", "/new_currency");
        logger.info(user + " New currency page loaded");
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
        User user = (User) session.getAttribute("user");
        logger.info(user + " Saving new currency...");
        if (!currencyName.matches("[A-Z]{3}")) {
            logger.info(user + " Redirecting to error page with error: В названии должны быть три заглавные латинские буквы");
            session.setAttribute("error", "В названии должны быть три заглавные латинские буквы");
            return "error";
        }
        if (currencyId < 100 || currencyId > 999) {
            logger.info(user + " Redirecting to error page with error: Код должен состоять из трех цифр от 0 до 9");
            session.setAttribute("error", "Код должен состоять из трех цифр от 0 до 9");
            return "error";
        }
        Currency newCurrency = new Currency();
        newCurrency.setName(currencyName);
        newCurrency.setId(currencyId);
        try {
            currencyManager.addCurrency(newCurrency);
        } catch (Exception e) {
            logger.info(user + " Redirecting to error page with error: " + e.getMessage());
            session.setAttribute("error", e.getMessage());
            return "error";
        }
        logger.info(user + " Currency saved");
        return "redirect:/client";
    }

    /**
     * Saved colors of some values of transaction. Data for saving gained by javascript in JSON format
     *
     * @param colors  String in JSON format that contains information about values and their colors that need to be saved
     * @param session collect parameters for view
     * @throws JsonProcessingException thrown while could not read string colors
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PostMapping(path = "/save_colors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String saveColors(@RequestBody String colors, HttpSession session) throws JsonProcessingException {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Saving colors...");
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
        logger.info(user + " Colors saved");
        return "redirect:/client_info";
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PostMapping(path = "/save_main_colors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String saveMainColors(@RequestBody String colors, HttpSession session) throws JsonProcessingException {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Saving colors...");
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
        logger.info(user + " Colors saved");
        return "redirect:/client_info";
    }

    @GetMapping("/recashe")
    public String getRefreshPage(HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading refresh cashe page...");
        User currentUser = (User) session.getAttribute("user");
        session.setAttribute("clients", clientManager.findAll());
        logger.info(user + " Refresh page loaded");
        return "recashe";
    }

    @PostMapping("/recashe")
    public String refreshCashe(@RequestParam(name = "date", required = false, defaultValue = "0001-01-01") Date date,
                               @RequestParam(name = "client", required = false) String clientName,
                               HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Updating balance values");
        List<Account> accounts = new ArrayList<>();
        if (clientName.isEmpty()) {
            logger.debug("client name is empty");
            accounts = accountManager.getAllAccounts();
        } else {
            logger.debug("client name found");
            accounts.add(accountManager.getAccounts(clientName).get(0));
        }
        logger.info(user + " Found " + accounts.size() + " accounts");
        for (Account account : accounts) {
            logger.info(user + " Updating " + account.getClient().getPib());
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
        logger.info(user + " Data updated");
        return "redirect:/client_info";
    }






}
