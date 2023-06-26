package org.profinef.service;

import jakarta.transaction.Transactional;
import org.profinef.dto.CurrencyDto;
import org.profinef.dto.TransactionDto;
import org.profinef.entity.Client;
import org.profinef.entity.Transaction;
import org.profinef.repository.CurrencyRepository;
import org.profinef.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


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
    public Transaction remittance(int transactionId, Timestamp date,
                                  String clientName, int currencyId, double rate, double commission, double amount, double transportation) {

        Client client = clientManager.getClient(clientName);
        if (date == null) date = new Timestamp(System.currentTimeMillis());
        TransactionDto transactionDto = formatToDto(transactionId, date,
                currencyId, rate, commission, amount, transportation, client);

        transactionDto = transactionRepository.save(transactionDto);
        return formatFromDto(transactionDto);
    }

    private TransactionDto formatToDto(int transactionId, Timestamp date,
                                       int currencyId, double rate, double commission, double amount, double transportation,
                                       Client client) {

        TransactionDto transactionDto = new TransactionDto();
        if (transactionId != 0) transactionDto.setId(transactionId);
        transactionDto.setDate(date);
        transactionDto.setClientId(client.getId());
        transactionDto.setCurrencyId(currencyId);
        transactionDto.setRate(rate);
        transactionDto.setCommission(commission);
        transactionDto.setAmount(amount);
        transactionDto.setBalance(updateCurrencyAmount(client.getId(), currencyId, rate, commission, amount, transportation));
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
        System.out.println(transaction);
        return transaction;
    }

    public List<Transaction> findByClient(int clientId, int currencyId, Timestamp date) {
        List<TransactionDto> transactionDtoList = transactionRepository.findAllByClientIdAndCurrencyIdAndDateAfter(clientId, currencyId, date);
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto :
                transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }

    private double updateCurrencyAmount(int clientId, int currencyId, double rate, double commission, double amount, double transportation) {
        CurrencyDto currencyDto = currencyRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        double balance = currencyDto.getAmount() + amount/rate - Math.abs(amount/rate) * (commission/100) - transportation;
        currencyDto.setAmount(balance);
        currencyRepository.save(currencyDto);
        return balance;
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

    private void undoTransaction(int transactionId, String clientName) {
        Client client = clientManager.getClient(clientName);
        TransactionDto transactionDto = transactionRepository.findByIdAndClientId(transactionId, client.getId());
        updateCurrencyAmount(client.getId(), transactionDto.getCurrencyId(), transactionDto.getRate(), transactionDto.getCommission(), -transactionDto.getAmount(), transactionDto.getTransportation());
    }

    public void update(int transactionId, String clientName, int currencyId, double rate, double commission, double amount, double transportation) {
        Client client = clientManager.getClient(clientName);
        TransactionDto oldTransactionDto = transactionRepository.findByIdAndClientId(transactionId, client.getId());
        Double oldBalance = currencyRepository.findByClientIdAndCurrencyId(client.getId(), currencyId).getAmount();

        undoTransaction(transactionId, clientName);

        Double newBalance = currencyRepository.findByClientIdAndCurrencyId(client.getId(), currencyId).getAmount();

        remittance(transactionId, oldTransactionDto.getDate(), clientName, currencyId, rate, commission, amount, transportation);
        TransactionDto newTransactionDto = transactionRepository.findByIdAndClientId(transactionId, client.getId());


        if (!Objects.equals(oldBalance, newBalance)) {
            Double balanceDif = newBalance - oldBalance;
            updateNext(clientName, currencyId, balanceDif, newTransactionDto);
        }
    }

    private void updateNext(String clientName, int currencyId, Double balanceDif, TransactionDto newTransactionDto) {
        Client client = clientManager.getClient(clientName);
        List<TransactionDto> transactionDtoList = transactionRepository.findAllByClientIdAndCurrencyIdAndDateAfter(client.getId(), currencyId, newTransactionDto.getDate());
        for (TransactionDto transactionDto :
                transactionDtoList) {
            transactionDto.setBalance(transactionDto.getBalance() + balanceDif);
            transactionRepository.save(transactionDto);
        }
    }

}
