package org.profinef.controller;

import jakarta.servlet.http.HttpSession;
import org.profinef.entity.Account;
import org.profinef.entity.Transaction;
import org.profinef.service.ClientManager;
import org.profinef.service.AccountManager;
import org.profinef.service.TransManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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

    public AppController(ClientManager clientManager, AccountManager accountManager, TransManager transManager) {
        this.clientManager = clientManager;
        this.accountManager = accountManager;
        this.transManager = transManager;
    }

    @GetMapping("/")
    public String viewHomePage(HttpSession session) {
        return "redirect:/client";
    }

    @GetMapping("/add_transaction")
    public String addTransaction(HttpSession session) {
        return "addTransaction";
    }

    @GetMapping(path = "/client")
    public String getClientsInfo(@RequestParam(name = "client_name", required = false) String clientName, HttpSession session) {
        List<Account> clients;
        if (clientName == null || clientName.isEmpty()) {
            clients = accountManager.getAllAccounts();
        } else {
            clients = new ArrayList<>();
            clients.add(accountManager.getAccount(clientName));
        }
        List<Transaction> transactions = transManager.getAllTransactions();
        session.setAttribute("transactions", transactions);
        session.setAttribute("clients", clients);
        return "example";
    }

    @GetMapping(path = "/edit")
    public String editTransactionPage(@RequestParam(name = "transaction_id") int transactionId,
                                  HttpSession session) {
        System.out.println(transactionId);
        Transaction transaction = transManager.getTransaction(transactionId);
        session.setAttribute("transaction", transaction);
        return "editTransaction";
    }

    @PostMapping(path = "edit")
    public String editTransaction(@RequestParam(name = "transaction_id") int transactionId,
                                  @RequestParam(name = "client1_name") String client1Name,
                                  @RequestParam(name = "currency1_id") int currency1Id,
                                  @RequestParam(name = "rate1") double rate1,
                                  @RequestParam(name = "commission1") double commission1,
                                  @RequestParam(name = "amount1") double amount1,
                                  @RequestParam(name = "client2_name", required = false) String client2Name,
                                  @RequestParam(name = "currency2_id", required = false, defaultValue = "0") int currency2Id,
                                  @RequestParam(name = "rate2", required = false, defaultValue = "0") double rate2,
                                  @RequestParam(name = "commission2", required = false, defaultValue = "0") double commission2,
                                  @RequestParam(name = "amount2", required = false, defaultValue = "0") double amount2,
                                  @RequestParam(name = "client3_name", required = false) String client3Name,
                                  @RequestParam(name = "currency3_id", required = false, defaultValue = "0") int currency3Id,
                                  @RequestParam(name = "rate3", required = false, defaultValue = "0") double rate3,
                                  @RequestParam(name = "commission3", required = false, defaultValue = "0") double commission3,
                                  @RequestParam(name = "amount3", required = false, defaultValue = "0") double amount3,
                                  @RequestParam(name = "client4_name", required = false) String client4Name,
                                  @RequestParam(name = "currency4_id", required = false, defaultValue = "0") int currency4Id,
                                  @RequestParam(name = "rate4", required = false, defaultValue = "0") double rate4,
                                  @RequestParam(name = "commission4", required = false, defaultValue = "0") double commission4,
                                  @RequestParam(name = "amount4", required = false, defaultValue = "0") double amount4,
                                  @RequestParam(name = "client5_name", required = false) String client5Name,
                                  @RequestParam(name = "currency5_id", required = false, defaultValue = "0") int currency5Id,
                                  @RequestParam(name = "rate5", required = false, defaultValue = "0") double rate5,
                                  @RequestParam(name = "commission5", required = false, defaultValue = "0") double commission5,
                                  @RequestParam(name = "amount5", required = false, defaultValue = "0") double amount5,
                                  @RequestParam(name = "client6_name", required = false) String client6Name,
                                  @RequestParam(name = "currency6_id", required = false, defaultValue = "0") int currency6Id,
                                  @RequestParam(name = "rate6", required = false, defaultValue = "0") double rate6,
                                  @RequestParam(name = "commission6", required = false, defaultValue = "0") double commission6,
                                  @RequestParam(name = "amount6", required = false, defaultValue = "0") double amount6,
                                  HttpSession session) {
        try{
            transManager.undoTransaction(transactionId);
            transManager.remittance(transactionId, client1Name, currency1Id, rate1, commission1, amount1,
                    client2Name, currency2Id, rate2, commission2, amount2,
                    client3Name, currency3Id, rate3, commission3, amount3,
                    client4Name, currency4Id, rate4, commission4, amount4,
                    client5Name, currency5Id, rate5, commission5, amount5,
                    client6Name, currency6Id, rate6, commission6, amount6);
            session.setAttribute("client_id", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/client";
    }

    @RequestMapping(path = "/transaction")
    public String doTransaction(@RequestParam(name = "client1_name") String client1Name,
                                     @RequestParam(name = "currency1_id") int currency1Id,
                                     @RequestParam(name = "rate1") double rate1,
                                     @RequestParam(name = "commission1") double commission1,
                                     @RequestParam(name = "amount1") double amount1,
                                     @RequestParam(name = "client2_name", required = false) String client2Name,
                                     @RequestParam(name = "currency2_id", required = false, defaultValue = "0") int currency2Id,
                                     @RequestParam(name = "rate2", required = false, defaultValue = "0") double rate2,
                                     @RequestParam(name = "commission2", required = false, defaultValue = "0") double commission2,
                                     @RequestParam(name = "amount2", required = false, defaultValue = "0") double amount2,
                                     @RequestParam(name = "client3_name", required = false) String client3Name,
                                     @RequestParam(name = "currency3_id", required = false, defaultValue = "0") int currency3Id,
                                     @RequestParam(name = "rate3", required = false, defaultValue = "0") double rate3,
                                     @RequestParam(name = "commission3", required = false, defaultValue = "0") double commission3,
                                     @RequestParam(name = "amount3", required = false, defaultValue = "0") double amount3,
                                     @RequestParam(name = "client4_name", required = false) String client4Name,
                                     @RequestParam(name = "currency4_id", required = false, defaultValue = "0") int currency4Id,
                                     @RequestParam(name = "rate4", required = false, defaultValue = "0") double rate4,
                                     @RequestParam(name = "commission4", required = false, defaultValue = "0") double commission4,
                                     @RequestParam(name = "amount4", required = false, defaultValue = "0") double amount4,
                                     @RequestParam(name = "client5_name", required = false) String client5Name,
                                     @RequestParam(name = "currency5_id", required = false, defaultValue = "0") int currency5Id,
                                     @RequestParam(name = "rate5", required = false, defaultValue = "0") double rate5,
                                     @RequestParam(name = "commission5", required = false, defaultValue = "0") double commission5,
                                     @RequestParam(name = "amount5", required = false, defaultValue = "0") double amount5,
                                     @RequestParam(name = "client6_name", required = false) String client6Name,
                                     @RequestParam(name = "currency6_id", required = false, defaultValue = "0") int currency6Id,
                                     @RequestParam(name = "rate6", required = false, defaultValue = "0") double rate6,
                                     @RequestParam(name = "commission6", required = false, defaultValue = "0") double commission6,
                                     @RequestParam(name = "amount6", required = false, defaultValue = "0") double amount6,
                                     HttpSession session) {
        try{
            transManager.remittance(0, client1Name, currency1Id, rate1, commission1, amount1,
                    client2Name, currency2Id, rate2, commission2, amount2,
                    client3Name, currency3Id, rate3, commission3, amount3,
                    client4Name, currency4Id, rate4, commission4, amount4,
                    client5Name, currency5Id, rate5, commission5, amount5,
                    client6Name, currency6Id, rate6, commission6, amount6);
            session.setAttribute("client_id", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/client";
    }
}
