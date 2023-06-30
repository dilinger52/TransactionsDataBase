package org.profinef.service;

import jakarta.transaction.Transactional;
import org.profinef.dto.AccountDto;
import org.profinef.dto.CurrencyDto;
import org.profinef.dto.TransactionDto;
import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.entity.Transaction;
import org.profinef.repository.AccountRepository;
import org.profinef.repository.CurrencyRepository;
import org.profinef.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;


@Service
public class TransManager {
    @Autowired
    private final TransactionRepository transactionRepository;
    @Autowired
    private final AccountRepository accountRepository;
    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final CurrencyManager currencyManager;
    @Autowired
    private final CurrencyRepository currencyRepository;

    public TransManager(TransactionRepository transactionRepository, AccountRepository accountRepository, ClientManager clientManager, CurrencyManager currencyManager, CurrencyRepository currencyRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.clientManager = clientManager;
        this.currencyManager = currencyManager;
        this.currencyRepository = currencyRepository;
    }

    @Transactional
    public double remittance(int transactionId, Timestamp date,
                                  String clientName, int currencyId, double rate, double commission, double amount, double transportation) throws RuntimeException {
        if (rate <= 0) throw new RuntimeException("Неверное значение курса. Введите положительное значение");
        if (commission < 0 || commission > 100) throw new RuntimeException("Неверная величина комиссии. Введите значение от 0 до 100 включительно");
        if (transportation < 0) throw new RuntimeException("Неверная стоимость инкасации. Введите положительное значение");
        Client client = clientManager.getClient(clientName);
        if (date == null) date = new Timestamp(System.currentTimeMillis());
        double balance = updateCurrencyAmount(client.getId(), currencyId, rate, commission, amount, transportation);
        TransactionDto transactionDto = formatToDto(transactionId, date,
                currencyId, rate, commission, amount, transportation, client, balance);
        transactionRepository.save(transactionDto);
        return balance;
    }

    private double updateCurrencyAmount(int clientId, int currencyId, double rate, double commission, double amount, double transportation) {
        Optional<CurrencyDto> currencyOpt = currencyRepository.findById(currencyId);
        if (currencyOpt.isEmpty()) throw new RuntimeException("Валюта не найдена");
        AccountDto accountDto = accountRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        if (accountDto == null) {
            accountRepository.save(new AccountDto(clientId, currencyId, 0));
            accountDto = accountRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        }
        double balance = accountDto.getAmount() + amount * (1 - commission/100) - transportation/rate;
        accountDto.setAmount(balance);
        accountRepository.save(accountDto);
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
        if (rate <= 0) throw new RuntimeException("Неверное значение курса. Введите положительное значение");
        if (commission < 0 || commission > 100) throw new RuntimeException("Неверная величина комиссии. Введите значение от 0 до 100 включительно");
        if (transportation < 0) throw new RuntimeException("Неверная стоимость инкасации. Введите положительное значение");

        TransactionDto oldTransactionDto = transactionRepository.findByIdAndClientId(transactionId, client.getId());
        double oldTransactionBalance = oldTransactionDto.getBalance();
        System.out.println("oldTransactionBalance " + oldTransactionBalance);
        double oldBalance = accountRepository.findByClientIdAndCurrencyId(client.getId(), currencyId).getAmount();
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

    @Transactional
    public void deleteTransaction(List<Transaction> transaction) {
        List<TransactionDto> transactionDtoList = new ArrayList<>();
        for (Transaction t : transaction) {
            double oldBalance = accountRepository.findByClientIdAndCurrencyId(t.getClient().getId(), t.getCurrency().getId()).getAmount();
            double newBalance = undoTransaction(t.getId(), t.getClient().getPib());
            TransactionDto transactionDto = formatToDto(t.getId(), t.getDate(), t.getCurrency().getId(), t.getRate(), t.getCommission(), t.getAmount(), t.getTransportation(), t.getClient(), t.getBalance());
            updateNext(t.getClient().getPib(), t.getCurrency().getId(), newBalance - oldBalance, transactionDto);
            transactionDtoList.add(transactionDto);
        }
        transactionRepository.deleteAll(transactionDtoList);
    }

    public Account addTotal(List<Account> clients) {
        Account total = new Account();
        total.setClient(new Client("Всего"));
        Map<Currency, Double> currencyDoubleMap = new TreeMap<>();
        List<Currency> currencies = currencyManager.getAllCurrencies();
        for (Currency currency :
                currencies) {
            double sum = 0;
            for (Account ac : clients)  {
                sum += ac.getCurrencies().entrySet().stream().filter(c -> Objects.equals(c.getKey().getId(), currency.getId())).findFirst().orElse(new Map.Entry<Currency, Double>() {
                    @Override
                    public Currency getKey() {
                        return null;
                    }

                    @Override
                    public Double getValue() {
                        return 0.0;
                    }

                    @Override
                    public Double setValue(Double value) {
                        return null;
                    }
                }).getValue();
            }
           currencyDoubleMap.put(currency, sum);
        }
        total.setCurrencies(currencyDoubleMap);
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
        transaction.setCurrency(currencyManager.getCurrency(transactionDto.getCurrencyId()));
        transaction.setBalance(transactionDto.getBalance());
        transaction.setRate(transactionDto.getRate());
        transaction.setCommission(transactionDto.getCommission());
        transaction.setAmount(transactionDto.getAmount());
        transaction.setTransportation(transactionDto.getTransportation());
        return transaction;
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
