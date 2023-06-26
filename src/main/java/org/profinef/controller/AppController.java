package org.profinef.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.entity.Transaction;
import org.profinef.service.ClientManager;
import org.profinef.service.AccountManager;
import org.profinef.service.TransManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


@Controller
public class AppController {
    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final AccountManager accountManager;
    @Autowired
    private final TransManager transManager;
    private int transactionId = 0;

    public AppController(ClientManager clientManager, AccountManager accountManager, TransManager transManager) {
        this.clientManager = clientManager;
        this.accountManager = accountManager;
        this.transManager = transManager;
    }

    @GetMapping("/")
    public String viewHomePage(HttpSession session) {
        return "redirect:/client";
    }

    @GetMapping("/client_info")
    public String viewClientTransactions(@RequestParam(name = "client_id") int clientId, HttpSession session) {
        List<Transaction> transactions = transManager.findByClient(clientId, 980, new Timestamp(0));
        System.out.println(transactions);
        for (Transaction transaction :
                transactions) {
            List<Transaction> tl = transManager.getTransaction(transaction.getId());
            StringBuilder clients = new StringBuilder();
            for (Transaction t : tl) {
                if (!t.getClient().getId().equals(transaction.getClient().getId())) {
                    clients.append("\n");
                    clients.append(t.getClient().getPib());
                }
            }
           transaction.setClient(new Client(clients.toString()));
        }
        System.out.println(transactions);
        session.setAttribute("transactions", transactions);
        return "clientInfo";
    }

    @GetMapping("/add_transaction")
    public String addTransaction(HttpSession session) {
        List<Account> clients = accountManager.getAllAccounts();
        session.setAttribute("clients", clients);
        return "addTransaction";
    }

    @GetMapping(path = "/client")
    public String getClientsInfo(@RequestParam(name = "client_name", required = false) String clientName, HttpSession session) {
        List<Account> clients;
        if (clientName == null || clientName.isEmpty()) {
            clients = accountManager.getAllAccounts();
            //transManager.addTotal(clients);
        } else {
            clients = new ArrayList<>();
            clients.add(accountManager.getAccount(clientName));
        }
        List<Transaction> transactions = transManager.getAllTransactions();
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
        System.out.println(transactionId);
        List<Transaction> transactions = transManager.getTransaction(transactionId);

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
        System.out.println(clientName);
        try{
            transactionId++;
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
}
