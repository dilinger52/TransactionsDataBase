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
    public Transaction remittance(int transactionId,
                                  String client1Name, int currency1Id, double rate1, double commission1, double amount1,
                                  String client2Name, int currency2Id, double rate2, double commission2, double amount2,
                                  String client3Name, int currency3Id, double rate3, double commission3, double amount3,
                                  String client4Name, int currency4Id, double rate4, double commission4, double amount4,
                                  String client5Name, int currency5Id, double rate5, double commission5, double amount5,
                                  String client6Name, int currency6Id, double rate6, double commission6, double amount6) {

        Client client1 = clientManager.getClient(client1Name);
        Client client2 = clientManager.getClient(client2Name);
        Client client3 = clientManager.getClient(client3Name);
        Client client4 = clientManager.getClient(client4Name);
        Client client5 = clientManager.getClient(client5Name);
        Client client6 = clientManager.getClient(client6Name);

        TransactionDto transactionDto = formatToDto(transactionId,
                currency1Id, rate1, commission1, amount1,
                currency2Id, rate2, commission2, amount2,
                currency3Id, rate3, commission3, amount3,
                currency4Id, rate4, commission4, amount4,
                currency5Id, rate5, commission5, amount5,
                currency6Id, rate6, commission6, amount6,
                client1, client2, client3, client4, client5, client6);

        transactionDto = transactionRepository.save(transactionDto);
        return formatFromDto(transactionDto);
    }

    private TransactionDto formatToDto(int transactionId,
                                       int currency1Id, double rate1, double commission1, double amount1,
                                       int currency2Id, double rate2, double commission2, double amount2,
                                       int currency3Id, double rate3, double commission3, double amount3,
                                       int currency4Id, double rate4, double commission4, double amount4,
                                       int currency5Id, double rate5, double commission5, double amount5,
                                       int currency6Id, double rate6, double commission6, double amount6,
                                       Client client1, Client client2, Client client3, Client client4, Client client5, Client client6) {

        TransactionDto transactionDto = new TransactionDto();
        if (transactionId != 0) transactionDto.setId(transactionId);

        transactionDto.setClient1Id(client1.getId());
        transactionDto.setCurrency1Id(currency1Id);
        transactionDto.setRate1(rate1);
        transactionDto.setCommission1(commission1);
        transactionDto.setAmount1(amount1);
        transactionDto.setBalance1(updateCurrencyAmount(client1.getId(), currency1Id, rate1, commission1, amount1));

        if (client2 != null) {
            transactionDto.setClient2Id(client2.getId());
            transactionDto.setCurrency2Id(currency2Id);
            transactionDto.setRate2(rate2);
            transactionDto.setCommission2(commission2);
            transactionDto.setAmount2(amount2);
            transactionDto.setBalance2(updateCurrencyAmount(client2.getId(), currency2Id, rate2, commission2, amount2));

            if (client3 != null) {
                transactionDto.setClient3Id(client3.getId());
                transactionDto.setCurrency3Id(currency3Id);
                transactionDto.setRate3(rate3);
                transactionDto.setCommission3(commission3);
                transactionDto.setAmount3(amount3);
                transactionDto.setBalance3(updateCurrencyAmount(client3.getId(), currency3Id, rate3, commission3, amount3));

                if (client4 != null) {
                    transactionDto.setClient4Id(client4.getId());
                    transactionDto.setCurrency4Id(currency4Id);
                    transactionDto.setRate4(rate4);
                    transactionDto.setCommission4(commission4);
                    transactionDto.setAmount4(amount4);
                    transactionDto.setBalance4(updateCurrencyAmount(client4.getId(), currency4Id, rate4, commission4, amount4));

                    if (client5 != null) {
                        transactionDto.setClient5Id(client5.getId());
                        transactionDto.setCurrency5Id(currency5Id);
                        transactionDto.setRate5(rate5);
                        transactionDto.setCommission5(commission5);
                        transactionDto.setAmount5(amount5);
                        transactionDto.setBalance5(updateCurrencyAmount(client5.getId(), currency5Id, rate5, commission5, amount5));

                        if (client6 != null) {
                            transactionDto.setClient6Id(client6.getId());
                            transactionDto.setCurrency6Id(currency6Id);
                            transactionDto.setRate6(rate6);
                            transactionDto.setCommission6(commission6);
                            transactionDto.setAmount6(amount6);
                            transactionDto.setBalance6(updateCurrencyAmount(client6.getId(), currency6Id, rate6, commission6, amount6));
                        }
                    }
                }
            }
        }
        return transactionDto;
    }


    private Transaction formatFromDto(TransactionDto transactionDto) {
        Transaction transaction = new Transaction();
        transaction.setId(transactionDto.getId());
        transaction.setClient1(clientManager.getClient(transactionDto.getClient1Id()));
        transaction.setCurrency1(accountManager.getCurrency(transactionDto.getClient1Id(), transactionDto.getCurrency1Id()));
        transaction.setBalance1(transactionDto.getBalance1());
        transaction.setRate1(transactionDto.getRate1());
        transaction.setCommission1(transactionDto.getCommission1());
        transaction.setAmount1(transactionDto.getAmount1());
        transaction.setClient2(clientManager.getClient(transactionDto.getClient2Id()));
        transaction.setCurrency2(accountManager.getCurrency(transactionDto.getClient2Id(), transactionDto.getCurrency2Id()));
        transaction.setBalance2(transactionDto.getBalance2());
        transaction.setRate2(transactionDto.getRate2());
        transaction.setCommission2(transactionDto.getCommission2());
        transaction.setAmount2(transactionDto.getAmount2());
        transaction.setClient3(clientManager.getClient(transactionDto.getClient3Id()));
        transaction.setCurrency3(accountManager.getCurrency(transactionDto.getClient3Id(), transactionDto.getCurrency3Id()));
        transaction.setBalance3(transactionDto.getBalance3());
        transaction.setRate3(transactionDto.getRate3());
        transaction.setCommission3(transactionDto.getCommission3());
        transaction.setAmount3(transactionDto.getAmount3());
        transaction.setClient4(clientManager.getClient(transactionDto.getClient4Id()));
        transaction.setCurrency4(accountManager.getCurrency(transactionDto.getClient4Id(), transactionDto.getCurrency4Id()));
        transaction.setBalance4(transactionDto.getBalance4());
        transaction.setRate4(transactionDto.getRate4());
        transaction.setCommission4(transactionDto.getCommission4());
        transaction.setAmount4(transactionDto.getAmount4());
        transaction.setClient5(clientManager.getClient(transactionDto.getClient5Id()));
        transaction.setCurrency5(accountManager.getCurrency(transactionDto.getClient5Id(), transactionDto.getCurrency5Id()));
        transaction.setBalance5(transactionDto.getBalance5());
        transaction.setRate5(transactionDto.getRate5());
        transaction.setCommission5(transactionDto.getCommission5());
        transaction.setAmount5(transactionDto.getAmount5());
        transaction.setClient6(clientManager.getClient(transactionDto.getClient6Id()));
        transaction.setCurrency6(accountManager.getCurrency(transactionDto.getClient6Id(), transactionDto.getCurrency6Id()));
        transaction.setBalance6(transactionDto.getBalance6());
        transaction.setRate6(transactionDto.getRate6());
        transaction.setCommission6(transactionDto.getCommission6());
        transaction.setAmount6(transactionDto.getAmount6());
        System.out.println(transaction);
        return transaction;
    }

    public void findByClient1(int clientId, int currencyId) {
        System.out.println("test" +clientId + currencyId + transactionRepository.findAllByClient1IdAndCurrency1Id(clientId, currencyId));
    }

    private double updateCurrencyAmount(int clientId, int currencyId, double rate, double commission, double amount) {
        CurrencyDto currencyDto = currencyRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        double balance = currencyDto.getAmount() + amount/rate - Math.abs(amount/rate) * (commission/100);
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

    public Transaction getTransaction(int transactionId) {
        TransactionDto transactionDto = transactionRepository.findById(transactionId).get();
        return formatFromDto(transactionDto);
    }

    public void undoTransaction(int transactionId) {
        TransactionDto transactionDto = transactionRepository.findById(transactionId).get();
        transactionDto.setBalance1(updateCurrencyAmount(transactionDto.getClient1Id(), transactionDto.getCurrency1Id(), transactionDto.getRate1(), transactionDto.getCommission1(), -transactionDto.getAmount1()));
        if (transactionDto.getClient2Id() != null) {
            transactionDto.setBalance2(updateCurrencyAmount(transactionDto.getClient2Id(), transactionDto.getCurrency2Id(), transactionDto.getRate2(), transactionDto.getCommission2(), -transactionDto.getAmount2()));
            if (transactionDto.getClient3Id() != null) {
                transactionDto.setBalance3(updateCurrencyAmount(transactionDto.getClient3Id(), transactionDto.getCurrency3Id(), transactionDto.getRate3(), transactionDto.getCommission3(), -transactionDto.getAmount3()));
                if (transactionDto.getClient4Id() != null) {
                    transactionDto.setBalance4(updateCurrencyAmount(transactionDto.getClient4Id(), transactionDto.getCurrency4Id(), transactionDto.getRate4(), transactionDto.getCommission4(), -transactionDto.getAmount4()));
                    if (transactionDto.getClient4Id() != null) {
                        transactionDto.setBalance5(updateCurrencyAmount(transactionDto.getClient5Id(), transactionDto.getCurrency5Id(), transactionDto.getRate5(), transactionDto.getCommission5(), -transactionDto.getAmount5()));
                        if (transactionDto.getClient6Id() != null) {
                            transactionDto.setBalance6(updateCurrencyAmount(transactionDto.getClient6Id(), transactionDto.getCurrency6Id(), transactionDto.getRate6(), transactionDto.getCommission6(), -transactionDto.getAmount6()));
                        }
                    }
                }
            }
        }

    }
}
