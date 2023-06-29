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

import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;
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
    public String viewHomePage(HttpSession session) {
        return "redirect:/client";
    }

    @GetMapping("/client_info")
    public String viewClientTransactions(@RequestParam(name = "client_id", required = false, defaultValue = "0") int clientId,
                                         @RequestParam(name = "currency_id") int currencyId,
                                         @RequestParam(name = "date") Date date,
                                         HttpSession session) {
        if (clientId == 0) {
            clientId = ((Client)session.getAttribute("client")).getId();
        }
        Client client = clientManager.getClient(clientId);
        List<Transaction> transactions = transManager.findByClientForDate(clientId, currencyId, new Timestamp(date.getTime()));
        Map<String, Double> total = new HashMap<>();
        double sum = 0;
        for (Transaction transaction :
                transactions) {
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
        if (transactions.isEmpty()) {
            total.put("balance", (double) 0);
        } else {
            total.put("balance", transactions.get(transactions.size() - 1).getBalance());
        }
        total.put("amount", sum);
        List<Currency> currencies = currencyManager.getAllCurrencies();
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
        List<Account> clients;
        if (clientName != null && !clientName.isEmpty()) {
            clients = new ArrayList<>();
            clients.add(accountManager.getAccount(clientName));
        } else if(clientPhone != null && !clientPhone.isEmpty()){
            clients = new ArrayList<>();
            clients.add(accountManager.getAccountByPhone(clientPhone));
        } else if(clientTelegram != null && !clientTelegram.isEmpty()) {
            clients = new ArrayList<>();
            clients.add(accountManager.getAccountByTelegram(clientTelegram));
        } else {
            clients = accountManager.getAllAccounts();
            clients.add(transManager.addTotal(clients));
        }
        List<Currency> currencies = currencyManager.getAllCurrencies();
        List<Transaction> transactions = transManager.getAllTransactions();
        session.setAttribute("currencies", currencies);
        session.setAttribute("transactions", transactions);
        session.setAttribute("clients", clients);
        System.out.println(clients);
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
    @PostMapping(path = "edit")
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

    @RequestMapping(path = "/transaction")
    public String doTransaction(@RequestParam(name = "client_name") List<String> clientName,
                                     @RequestParam(name = "currency_id") List<Integer> currencyId,
                                     @RequestParam(name = "rate") List<Double> rate,
                                     @RequestParam(name = "commission") List<Double> commission,
                                     @RequestParam(name = "amount") List<Double> amount,
                                     @RequestParam(name = "transportation") List<Double> transportation,
                                     HttpSession session) {

        System.out.println(amount);
        try{
            int transactionId = transManager.getAllTransactions()
                    .stream()
                    .flatMapToInt(t -> IntStream.of(t.getId()))
                    .max()
                    .orElse(0) + 1;

            for (int i = 0; i < clientName.size(); i++) {
                transManager.remittance(transactionId, null, clientName.get(i), currencyId.get(i), rate.get(i),
                        commission.get(i), amount.get(i), transportation.get(i));
            }
            session.setAttribute("client_id", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/client";
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
        if (phone != null) newClient.setPhone(phone);
        if (telegram != null) newClient.setTelegram(telegram);
        try {
            accountManager.addClient(newClient);
        } catch (Exception e) {
            session.setAttribute("error", e.getMessage());
            return "error";
        }

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

}
