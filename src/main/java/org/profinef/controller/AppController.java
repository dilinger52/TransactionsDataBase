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
import org.profinef.service.ClientManager;
import org.profinef.service.AccountManager;
import org.profinef.service.CurrencyManager;
import org.profinef.service.TransManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/excel")
    public String loadPage(HttpSession session) {
        session.setAttribute("path", "/excel");
        return "uploadFiles";
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
        List<String> currencyName = new ArrayList<>();
        if (currencyId == null) {
            currencyId = new ArrayList<>();
            List<Currency> currencies = currencyManager.getAllCurrencies();
            for (Currency currency : currencies) {
                currencyName.add(currency.getName());
                currencyId.add(currency.getId());
            }
        } else {
            for (int id : currencyId) {
                currencyName.add(currencyManager.getCurrency(id).getName());
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
        session.setAttribute("path", "/client_info");
        session.setAttribute("currencies", currencies);
        session.setAttribute("total", total);
        session.setAttribute("date", date);
        session.setAttribute("client", client);
        session.setAttribute("transactions", transactions);
        session.setAttribute("currency_name", currencyName);
        return "clientInfo";
    }



    @GetMapping("/add_transaction")
    public String addTransaction(HttpSession session) {
        List<Account> clients = accountManager.getAllAccounts();
        session.setAttribute("clients", clients);
        List<Currency> currencies = currencyManager.getAllCurrencies();
        session.setAttribute("path", "/add_transaction");
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
        session.setAttribute("path", "/client");
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
        session.setAttribute("path", "/edit");
        return "editTransaction";
    }
    @PostMapping(path = "/edit")
    public String editTransaction(@RequestParam(name = "transaction_id") List<Integer> transactionId,
                                  @RequestParam(name = "client_name") List<String> clientName,
                                  @RequestParam(name = "currency_name") List<String> currencyName,
                                  @RequestParam(name = "rate") List<Double> rate,
                                  @RequestParam(name = "commission") List<Double> commission,
                                  @RequestParam(name = "amount") List<Double> amount,
                                  @RequestParam(name = "transportation") List<Double> transportation,
                                  HttpSession session) {
        try{
            List<Integer> currencyId = new ArrayList<>();
            for (String name : currencyName) {
                currencyId.add(currencyManager.getCurrency(name).getId());
            }
            for (int i = 0; i < clientName.size(); i++) {
                transManager.update(transactionId.get(i), clientName.get(i), currencyId.get(i), rate.get(i), commission.get(i), amount.get(i), transportation.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        session.setAttribute("path", "/edit");
        return "redirect:/client";
    }

    @PostMapping(path = "delete_transaction")
    public String deleteTransaction(@RequestParam(name = "transaction_id") int transactionId, HttpSession session) {
        List<Transaction> transaction = transManager.getTransaction(transactionId);
        transManager.deleteTransaction(transaction);
        session.setAttribute("path", "/delete_transaction");
        return "redirect:/client";
    }

    @PostMapping(path = "/transaction")
    public String doTransaction(@RequestParam(name = "client_name") List<String> clientName,
                                     @RequestParam(name = "currency_name") List<String> currencyName,
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
                    List<Integer> currencyId = new ArrayList<>();
                    for (String name : currencyName) {
                        currencyId.add(currencyManager.getCurrency(name).getId());
                    }
                    transManager.remittance(transactionId, null, clientName.get(i), currencyId.get(i), rate.get(i),
                            commission.get(i), amount.get(i), transportation.get(i), null, null, null);
                } catch (Exception e) {
                    session.setAttribute("error", e.getMessage());
                    return "error";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        session.setAttribute("path", "/transaction");
        return "redirect:/client_info";
    }

    @GetMapping(path = "/new_client")
    public String newClient(HttpSession session) {
        session.setAttribute("path", "/new_client");
        return "addClient";
    }

    @GetMapping(path = "/edit_client")
    public String editClient(HttpSession session, @RequestParam(name = "client_id") int clientId) {
        Client client = clientManager.getClient(clientId);
        session.setAttribute("client", client);
        session.setAttribute("path", "/edit_client");
        return "addClient";
    }
    @PostMapping(path = "/new_client")
    public String saveNewClient(@RequestParam(name = "client_name") String clientName,
                                @RequestParam(name = "client_phone", required = false) String phone,
                                @RequestParam(name = "client_telegram", required = false) String telegram,
                                HttpSession session) {
        Client client = new Client(clientName);
        if (phone != null) {
            if (!phone.matches("\\+38 \\(0[0-9]{2}\\) [0-9]{3}-[0-9]{2}-[0-9]{2}")) {
                session.setAttribute("error", "Номер телефона должен соответсвовать паттерну: +38 (011) 111-11-11");
                return "error";
            }
            client.setPhone(phone);
        }
        if (telegram != null) {
            if (!telegram.matches("@[a-z._0-9]+")) {
                session.setAttribute("error", "Телеграм тег должен начинаться с @ содержать латинские буквы нижнего регистра, цифры, \".\" или \"_\"");
                return "error";
            }
            client.setTelegram(telegram);
        }
        try {
            if (session.getAttribute("path") == "/edit_client") {
                clientManager.updateClient(client);
            }
            if (session.getAttribute("path") == "/new_client") {
                accountManager.addClient(client);
            }
        } catch (Exception e) {
            session.setAttribute("error", e.getMessage());
            return "error";
        }
        session.setAttribute("path", "/new_client");
        return "redirect:/client";
    }

    @PostMapping(path = "delete_client")
    public String deleteClient(@RequestParam(name = "id") int clientId, HttpSession session) {
        Client client = clientManager.getClient(clientId);
        clientManager.deleteClient(client);
        session.setAttribute("path", "/delete_client");
        return "redirect:/client";
    }

    @GetMapping(path = "/new_currency")
    public String newCurrency(HttpSession session) {
        session.setAttribute("path", "/new_currency");
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
        session.setAttribute("path", "/new_currency");
        return "redirect:/client";
    }
    @Transactional
    @PostMapping(path = "/save_colors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String saveColors(@RequestBody String colors, HttpSession session) throws JsonProcessingException {
        System.out.println("success: " + colors);
        List<List<String>> entryList = new ObjectMapper().readValue(colors, new TypeReference<>() {});
        System.out.println(entryList);
        for (List<String> list : entryList) {
            String[] arrList = list.get(0).split("_");
            Transaction transaction = transManager.getTransactionByClientAndByIdAndByCurrencyId(Integer.parseInt(arrList[1]), Integer.parseInt(arrList[2]), Integer.parseInt(arrList[3]));
            switch (arrList[0]) {
                case "pib" -> transaction.setPibColor(list.get(1));
                case "amount" -> transaction.setAmountColor(list.get(1));
                case "balance" -> transaction.setBalanceColor(list.get(1));
            }
            System.out.println(transaction);
            transManager.save(transaction);
        }
        session.setAttribute("path", "/save_colors");
        return "redirect:/client_info";
    }


}
