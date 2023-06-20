package org.profinef.service;

import jakarta.transaction.Transactional;
import org.profinef.dbo.ClientsCurrencyDbo;
import org.profinef.dbo.TransactionDbo;
import org.profinef.entity.Currency;
import org.profinef.entity.Transaction;
import org.profinef.repository.ClientsCurrencyRepository;
import org.profinef.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class TransManager {
    @Autowired
    private final TransactionRepository transactionRepository;
    @Autowired
    private final ClientsCurrencyRepository clientsCurrencyRepository;
    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final CurrencyManager currencyManager;

    public TransManager(TransactionRepository transactionRepository, ClientsCurrencyRepository clientsCurrencyRepository, ClientManager clientManager, CurrencyManager currencyManager) {
        this.transactionRepository = transactionRepository;
        this.clientsCurrencyRepository = clientsCurrencyRepository;
        this.clientManager = clientManager;
        this.currencyManager = currencyManager;
    }
    @Transactional
    public Transaction remittance(int client1Id, int currency1Id, int client2Id, int currency2Id, double amount) {
        TransactionDbo transactionDbo = transactionRepository.save(new TransactionDbo(client1Id, currency1Id, client2Id, currency2Id, amount));
        updateCurrencyAmount(client1Id, currency1Id, amount);
        updateCurrencyAmount(client2Id, currency2Id, -amount);
        Transaction transaction = new Transaction();
        transaction.setId(transactionDbo.getId());
        transaction.setClient1(clientManager.getClient(transactionDbo.getClient1Id()));
        transaction.setCurrency1(currencyManager.getCurrency(transactionDbo.getCurrency1Id()));
        transaction.setClient2(clientManager.getClient(transactionDbo.getClient2Id()));
        transaction.setCurrency2(currencyManager.getCurrency(transactionDbo.getCurrency2Id()));
        return transaction;
    }

    private void updateCurrencyAmount(int clientId, int currencyId, double amount) {
        ClientsCurrencyDbo clientsCurrency = clientsCurrencyRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        Currency currency = currencyManager.getCurrency(currencyId);
        clientsCurrency.setAmount(clientsCurrency.getAmount() + amount / currency.getExchangeRate());
        clientsCurrencyRepository.save(clientsCurrency);
    }
}
