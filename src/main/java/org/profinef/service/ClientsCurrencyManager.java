package org.profinef.service;

import org.profinef.dbo.ClientsCurrencyDbo;
import org.profinef.entity.ClientsCurrency;
import org.profinef.repository.ClientsCurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientsCurrencyManager {
    @Autowired
    private final ClientsCurrencyRepository clientsCurrencyRepository;
    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final CurrencyManager currencyManager;

    public ClientsCurrencyManager(ClientsCurrencyRepository clientsCurrencyRepository, ClientManager clientManager, CurrencyManager currencyManager) {
        this.clientsCurrencyRepository = clientsCurrencyRepository;
        this.clientManager = clientManager;
        this.currencyManager = currencyManager;
    }

    public List<ClientsCurrency> getClientsCurrencyAmount(int id) {
        List<ClientsCurrencyDbo> clientsCurrencyDboList = clientsCurrencyRepository.findByClientId(id);
        List<ClientsCurrency> clientsCurrencies = new ArrayList<>();
        for (ClientsCurrencyDbo clientCurrencyDbo : clientsCurrencyDboList) {
            ClientsCurrency clientsCurrency = new ClientsCurrency();
            clientsCurrency.setClient(clientManager.getClient(clientCurrencyDbo.getClientId()));
            clientsCurrency.setCurrency(currencyManager.getCurrency(clientCurrencyDbo.getCurrencyId()));
            clientsCurrency.setAmount(clientCurrencyDbo.getAmount());
            clientsCurrencies.add(clientsCurrency);
        }
        return clientsCurrencies;
    }


}
