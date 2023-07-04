package org.profinef.controller;

import jakarta.servlet.http.HttpSession;
import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.entity.Transaction;
import org.profinef.service.ClientManager;
import org.profinef.service.AccountManager;
import org.profinef.service.CurrencyManager;
import org.profinef.service.TransManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import javax.swing.text.html.CSS;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


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


    public AppController(ClientManager clientManager, AccountManager accountManager, TransManager transManager, CurrencyManager currencyManager) {
        this.clientManager = clientManager;
        this.accountManager = accountManager;
        this.transManager = transManager;
        this.currencyManager = currencyManager;
    }

    @GetMapping("/")
    public String viewHomePage() {
        return "redirect:/client";
    }

    @GetMapping("/client_info")
    public String viewClientTransactions(@RequestParam(name = "client_id", required = false, defaultValue = "0") int clientId,
                                         @RequestParam(name = "currency_id", required = false) List<Integer> currencyId,
                                         @RequestParam(name = "date", required = false) Date date,
                                         HttpSession session) {
        if (clientId == 0) {
            if (session.getAttribute("client") == null) {
                return "redirect:/client";
            }
            clientId = ((Client)session.getAttribute("client")).getId();
        }
        if (currencyId == null) {
            currencyId = new ArrayList<>();
            List<Currency> currencies = currencyManager.getAllCurrencies();
            for (Currency currency : currencies) {
                currencyId.add(currency.getId());
            }
        }
        if (date == null) date = Date.valueOf(LocalDate.now());
        Client client = clientManager.getClient(clientId);
        List<Transaction> transactions = transManager.findByClientForDate(clientId, currencyId, new Timestamp(date.getTime()));
        Map<String, Double> total = new HashMap<>();
        List<Currency> currencies = currencyManager.getAllCurrencies();
        for (Currency currency : currencies) {
            List <Transaction> transactionList = transactions.stream().filter(transaction -> transaction.getCurrency().getId().equals(currency.getId())).collect(Collectors.toList());
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
        session.setAttribute("currencies", currencies);
        session.setAttribute("total", total);
        session.setAttribute("date", date);
        session.setAttribute("client", client);
        session.setAttribute("transactions", transactions);
        session.setAttribute("currency_id", currencyId);
        return "clientInfo";
    }

    @GetMapping("/add_transaction")
    public String addTransaction(HttpSession session) {
        List<Account> clients = accountManager.getAllAccounts();
        session.setAttribute("clients", clients);
        List<Currency> currencies = currencyManager.getAllCurrencies();
        session.setAttribute("currencies", currencies);
        return "addTransaction";
    }

    @GetMapping(path = "/client")
    public String getClientsInfo(@RequestParam(name = "client_name", required = false) String clientName,
                                 @RequestParam(name = "client_phone", required = false) String clientPhone,
                                 @RequestParam(name = "client_telegram", required = false) String clientTelegram,
                                 HttpSession session) {
        session.removeAttribute("error");
        session.removeAttribute("client");
        List<Account> clients;
        if (clientName != null && !clientName.isEmpty()) {
            clients = new ArrayList<>();
            try {
                clients.add(accountManager.getAccount(clientName));
            } catch (Exception e) {
                session.setAttribute("error", e.getMessage());
                return "error";
            }
        } else if(clientPhone != null && !clientPhone.isEmpty()){
            clients = new ArrayList<>();
            try {
                clients.add(accountManager.getAccountByPhone(clientPhone));
        } catch (Exception e) {
            session.setAttribute("error", e.getMessage());
            return "error";
        }
        } else if(clientTelegram != null && !clientTelegram.isEmpty()) {
            clients = new ArrayList<>();
            try {
                clients.add(accountManager.getAccountByTelegram(clientTelegram));
            } catch (Exception e) {
        session.setAttribute("error", e.getMessage());
        return "error";
    }
        } else {
            clients = accountManager.getAllAccounts();
            clients.add(transManager.addTotal(clients));
        }
        List<Currency> currencies = currencyManager.getAllCurrencies();
        List<Transaction> transactions = transManager.getAllTransactions();
        session.setAttribute("currencies", currencies);
        session.setAttribute("transactions", transactions);
        session.setAttribute("clients", clients);
        return "example";
    }



    @GetMapping(path = "/edit")
    public String editTransactionPage(@RequestParam(name = "transaction_id") int transactionId,
                                  HttpSession session) {
        List<Account> clients = accountManager.getAllAccounts();
        session.setAttribute("clients", clients);
        List<Transaction> transactions = transManager.getTransaction(transactionId);
        List<Currency> currencies = currencyManager.getAllCurrencies();
        session.setAttribute("currencies", currencies);
        session.setAttribute("transactions", transactions);
        return "editTransaction";
    }
    @PostMapping(path = "/edit")
    public String editTransaction(@RequestParam(name = "transaction_id") List<Integer> transactionId,
                                  @RequestParam(name = "client_name") List<String> clientName,
                                  @RequestParam(name = "currency_id") List<Integer> currencyId,
                                  @RequestParam(name = "rate") List<Double> rate,
                                  @RequestParam(name = "commission") List<Double> commission,
                                  @RequestParam(name = "amount") List<Double> amount,
                                  @RequestParam(name = "transportation") List<Double> transportation) {
        try{
            for (int i = 0; i < clientName.size(); i++) {
                transManager.update(transactionId.get(i), clientName.get(i), currencyId.get(i), rate.get(i), commission.get(i), amount.get(i), transportation.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/client";
    }

    @PostMapping(path = "delete_transaction")
    public String deleteTransaction(@RequestParam(name = "transaction_id") int transactionId) {
        List<Transaction> transaction = transManager.getTransaction(transactionId);
        transManager.deleteTransaction(transaction);
        return "redirect:/client";
    }

    @PostMapping(path = "/transaction")
    public String doTransaction(@RequestParam(name = "client_name") List<String> clientName,
                                     @RequestParam(name = "currency_id") List<Integer> currencyId,
                                     @RequestParam(name = "rate") List<Double> rate,
                                     @RequestParam(name = "commission") List<Double> commission,
                                     @RequestParam(name = "amount") List<Double> amount,
                                     @RequestParam(name = "transportation") List<Double> transportation,
                                     HttpSession session) {

        try{
            int transactionId = transManager.getAllTransactions()
                    .stream()
                    .flatMapToInt(t -> IntStream.of(t.getId()))
                    .max()
                    .orElse(0) + 1;

            for (int i = 0; i < clientName.size(); i++) {
                try {
                    transManager.remittance(transactionId, null, clientName.get(i), currencyId.get(i), rate.get(i),
                            commission.get(i), amount.get(i), transportation.get(i));
                } catch (Exception e) {
                    session.setAttribute("error", e.getMessage());
                    return "error";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/client_info";
    }

    @GetMapping(path = "/new_client")
    public String newClient() {
        return "addClient";
    }
    @PostMapping(path = "/new_client")
    public String saveNewClient(@RequestParam(name = "client_name") String clientName,
                                @RequestParam(name = "client_phone", required = false) String phone,
                                @RequestParam(name = "client_telegram", required = false) String telegram,
                                HttpSession session) {
        Client newClient = new Client(clientName);
        if (phone != null) {
            if (!phone.matches("\\+38 \\(0[0-9]{2}\\) [0-9]{3}-[0-9]{2}-[0-9]{2}")) {
                session.setAttribute("error", "Номер телефона должен соответсвовать паттерну: +38 (011) 111-11-11");
                return "error";
            }
            newClient.setPhone(phone);
        }
        if (telegram != null) {
            if (!telegram.matches("@[a-z._0-9]+")) {
                session.setAttribute("error", "Телеграм тег должен начинаться с @ содержать латинские буквы нижнего регистра, цифры, \".\" или \"_\"");
                return "error";
            }
            newClient.setTelegram(telegram);
        }
        try {
            accountManager.addClient(newClient);
        } catch (Exception e) {
            session.setAttribute("error", e.getMessage());
            return "error";
        }

        return "redirect:/client";
    }

    @PostMapping(path = "delete_client")
    public String deleteClient(@RequestParam(name = "id") int clientId) {
        Client client = clientManager.getClient(clientId);
        clientManager.deleteClient(client);
        return "redirect:/client";
    }

    @GetMapping(path = "/new_currency")
    public String newCurrency() {
        return "addCurrency";
    }
    @PostMapping(path = "/new_currency")
    public String saveNewCurrency(@RequestParam(name = "currency_name") String currencyName,
                                @RequestParam(name = "currency_id") int currencyId,
                                HttpSession session) {
        if (!currencyName.matches("[A-Z]{3}")) {
            session.setAttribute("error", "В названии должны быть три заглавные латинские буквы");
            return "error";
        }
        Currency newCurrency = new Currency();
        newCurrency.setName(currencyName);
        newCurrency.setId(currencyId);
        try {
            currencyManager.addCurrency(newCurrency);
        } catch (Exception e) {
            session.setAttribute("error", e.getMessage());
            return "error";
        }

        return "redirect:/client";
    }

    @PostMapping(path = "/save_colors")
    public String saveColors(Map<String, String> color) {
        System.out.println("success: " + color);
        return "redirect:/client_info";
    }
}
