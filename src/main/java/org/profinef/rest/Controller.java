package org.profinef.rest;

import org.profinef.entity.ClientsCurrency;
import org.profinef.entity.Transaction;
import org.profinef.service.ClientManager;
import org.profinef.service.ClientsCurrencyManager;
import org.profinef.service.CurrencyManager;
import org.profinef.service.TransManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class Controller {
    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final ClientsCurrencyManager clientsCurrencyManager;
    @Autowired
    private final CurrencyManager currencyManager;
    @Autowired
    private final TransManager transManager;

    public Controller(ClientManager clientManager, ClientsCurrencyManager clientsCurrencyManager, CurrencyManager currencyManager, TransManager transManager) {
        this.clientManager = clientManager;
        this.clientsCurrencyManager = clientsCurrencyManager;
        this.currencyManager = currencyManager;
        this.transManager = transManager;
    }

    @GetMapping(path = "/client/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ClientsCurrency> getClientsInfo(@PathVariable int id) {
        return clientsCurrencyManager.getClientsCurrencyAmount(id);
    }
    @RequestMapping(
            path = "/transaction/client1/{client1Id}/currency1/{currency1Id}/client2/{client2Id}/currency2/{currency2Id}/amount/{amount}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Transaction doTransaction(@PathVariable int client1Id,
                                        @PathVariable int currency1Id,
                                        @PathVariable int client2Id,
                                        @PathVariable int currency2Id,
                                        @PathVariable double amount) {
        try{
        return transManager.remittance(client1Id, currency1Id, client2Id, currency2Id, amount);

        } catch (Exception e) {
            e.printStackTrace();
            return new Transaction();
        }
    }
}
