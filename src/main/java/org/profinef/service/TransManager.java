package org.profinef.service;

import jakarta.transaction.Transactional;
import org.profinef.dto.CurrencyDto;
import org.profinef.dto.TransactionDto;
import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.entity.Transaction;
import org.profinef.repository.CurrencyRepository;
import org.profinef.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


@Service
public class TransManager {
    @Autowired
    private final TransactionRepository transactionRepository;
    @Autowired
    private final CurrencyRepository currencyRepository;
    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final AccountManager accountManager;

    public TransManager(TransactionRepository transactionRepository, CurrencyRepository currencyRepository, ClientManager clientManager, AccountManager accountManager) {
        this.transactionRepository = transactionRepository;
        this.currencyRepository = currencyRepository;
        this.clientManager = clientManager;
        this.accountManager = accountManager;
    }

    @Transactional
    public double remittance(int transactionId, Timestamp date,
                                  String clientName, int currencyId, double rate, double commission, double amount, double transportation) {

        Client client = clientManager.getClient(clientName);
        if (date == null) date = new Timestamp(System.currentTimeMillis());
        double balance = updateCurrencyAmount(client.getId(), currencyId, rate, commission, amount, transportation);
        TransactionDto transactionDto = formatToDto(transactionId, date,
                currencyId, rate, commission, amount, transportation, client, balance);
        transactionRepository.save(transactionDto);
        return balance;
    }

    private double updateCurrencyAmount(int clientId, int currencyId, double rate, double commission, double amount, double transportation) {
        CurrencyDto currencyDto = currencyRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        if (currencyDto == null) {
            CurrencyDto currencyDto1 = currencyRepository.findFirstByCurrencyId(currencyId);
            currencyRepository.save(new CurrencyDto(clientId, currencyId, currencyDto1.getName(), 0));
            currencyDto = currencyRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        }
        double balance = currencyDto.getAmount() + amount * (1 - commission/100) - transportation/rate;
        currencyDto.setAmount(balance);
        currencyRepository.save(currencyDto);
        return balance;
    }

    private double undoTransaction(int transactionId, String clientName) {
        Client client = clientManager.getClient(clientName);
        TransactionDto transactionDto = transactionRepository.findByIdAndClientId(transactionId, client.getId());
        return updateCurrencyAmount(client.getId(), transactionDto.getCurrencyId(), transactionDto.getRate(), transactionDto.getCommission(), -transactionDto.getAmount(), transactionDto.getTransportation());
    }
    @Transactional
    public void update(int transactionId, String clientName, int currencyId, double rate, double commission, double amount, double transportation) {
        Client client = clientManager.getClient(clientName);

        TransactionDto oldTransactionDto = transactionRepository.findByIdAndClientId(transactionId, client.getId());
        double oldTransactionBalance = oldTransactionDto.getBalance();
        System.out.println("oldTransactionBalance " + oldTransactionBalance);
        double oldBalance = currencyRepository.findByClientIdAndCurrencyId(client.getId(), currencyId).getAmount();
        undoTransaction(transactionId, clientName);
        System.out.println("oldBalance " + oldBalance);
        double newBalance = remittance(transactionId, oldTransactionDto.getDate(), clientName, currencyId, rate, commission, amount, transportation);
        System.out.println("newBalance " + newBalance);
        TransactionDto newTransactionDto = transactionRepository.findByIdAndClientId(transactionId, client.getId());
        double balanceDif = newBalance - oldBalance;
        if (balanceDif != 0) {
            System.out.println("balanceDif " + balanceDif);
            newTransactionDto.setBalance(oldTransactionBalance + balanceDif);
            transactionRepository.save(newTransactionDto);
            updateNext(clientName, currencyId, balanceDif, newTransactionDto);
        }
    }

    private void updateNext(String clientName, int currencyId, Double balanceDif, TransactionDto newTransactionDto) {
        Client client = clientManager.getClient(clientName);
        List<TransactionDto> transactionDtoList = transactionRepository.findAllByClientIdAndCurrencyIdAndDateBetween(client.getId(), currencyId, newTransactionDto.getDate(), new Timestamp(System.currentTimeMillis()));
        System.out.println(transactionDtoList);
        for (TransactionDto transactionDto :
                transactionDtoList) {
            System.out.println("oldbalance " + transactionDto.getBalance());
            transactionDto.setBalance(transactionDto.getBalance() + balanceDif);
            System.out.println("newbalance " + transactionDto.getBalance());
            transactionRepository.save(transactionDto);
        }
    }

    public Account addTotal(List<Account> clients) {
        Account total = new Account();
        total.setClient(new Client("Total"));
        List<Currency> currencies = new ArrayList<>();

        double sumUsd = 0;
        double sumUah = 0;
            for (Account ac :
                    clients) {
                sumUsd += ac.getCurrencies().stream().filter(c -> c.getId()==840).findFirst().get().getAmount();
                sumUah += ac.getCurrencies().stream().filter(c -> c.getId()==980).findFirst().get().getAmount();
            }
            currencies.add(new Currency(840, "USD", sumUsd));
            currencies.add(new Currency(980, "UAH", sumUah));
        total.setCurrencies(currencies);
        return total;
    }

    private TransactionDto formatToDto(int transactionId, Timestamp date,
                                       int currencyId, double rate, double commission, double amount, double transportation,
                                       Client client, double balance) {

        TransactionDto transactionDto = new TransactionDto();
        if (transactionId != 0) transactionDto.setId(transactionId);
        transactionDto.setDate(date);
        transactionDto.setClientId(client.getId());
        transactionDto.setCurrencyId(currencyId);
        transactionDto.setRate(rate);
        transactionDto.setCommission(commission);
        transactionDto.setAmount(amount);
        transactionDto.setBalance(balance);
        transactionDto.setTransportation(transportation);
        return transactionDto;
    }


    private Transaction formatFromDto(TransactionDto transactionDto) {
        Transaction transaction = new Transaction();
        transaction.setId(transactionDto.getId());
        transaction.setDate(transactionDto.getDate());
        transaction.setClient(clientManager.getClient(transactionDto.getClientId()));
        transaction.setCurrency(accountManager.getCurrency(transactionDto.getClientId(), transactionDto.getCurrencyId()));
        transaction.setBalance(transactionDto.getBalance());
        transaction.setRate(transactionDto.getRate());
        transaction.setCommission(transactionDto.getCommission());
        transaction.setAmount(transactionDto.getAmount());
        transaction.setTransportation(transactionDto.getTransportation());
        return transaction;
    }

    public List<Transaction> findByClient(int clientId, int currencyId, Timestamp date) {
        List<TransactionDto> transactionDtoList = transactionRepository.findAllByClientIdAndCurrencyIdAndDateBetween(clientId, currencyId, date, new Timestamp(System.currentTimeMillis()));
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto :
                transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }



    public List<Transaction> getAllTransactions() {
        List<TransactionDto> transactionDtoList = (List<TransactionDto>) transactionRepository.findAll();
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto : transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }

    public List<Transaction> getTransaction(int transactionId) {
        List<TransactionDto> transactionDtoList = transactionRepository.findAllById(transactionId);
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto :
                transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }


    public List<Transaction> findByClientForDate(int clientId, int currencyId, Timestamp date) {
        List<TransactionDto> transactionDtoList = transactionRepository.findAllByClientIdAndCurrencyIdAndDateBetween(clientId, currencyId, date, new Timestamp(date.getTime() + 86400000));
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto :
                transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }
}
