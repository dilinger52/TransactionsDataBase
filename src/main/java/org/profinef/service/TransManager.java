package org.profinef.service;

import org.profinef.dto.AccountDto;
import org.profinef.dto.CurrencyDto;
import org.profinef.dto.TransactionDto;
import org.profinef.entity.*;
import org.profinef.entity.Currency;
import org.profinef.repository.AccountRepository;
import org.profinef.repository.CurrencyRepository;
import org.profinef.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
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
    @Autowired
    private final UserManager userManager;
    private static final Logger logger = LoggerFactory.getLogger(TransManager.class);

    public TransManager(TransactionRepository transactionRepository, AccountRepository accountRepository,
                        ClientManager clientManager, CurrencyManager currencyManager,
                        CurrencyRepository currencyRepository, UserManager userManager) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.clientManager = clientManager;
        this.currencyManager = currencyManager;
        this.currencyRepository = currencyRepository;
        this.userManager = userManager;
    }


    public double remittance(int transactionId, Timestamp date, String clientName, String comment, int currencyId, double rate,
                                 double commission, double amount, double transportation, String commentColor,
                                 String amountColor, int userId, Double trBalance, String inputColor,
                             String outputColor, String tarifColor, String commissionColor, String rateColor, String transportationColor) throws RuntimeException {
        logger.debug("Saving transaction");
        //if (rate <= 0) throw new RuntimeException("Неверное значение курса. Введите положительное значение");
        if (commission < -100 || commission > 100) throw new RuntimeException("Неверная величина комиссии. " +
                "Введите значение от -100 до 100 включительно");
        Client client = clientManager.getClientExactly(clientName);
        Currency currency = currencyManager.getCurrency(currencyId);
        User user = userManager.getUser(userId);
        if (date == null) date = new Timestamp(System.currentTimeMillis());

        double oldBalance = 0.0;
        if (trBalance != null) {
            if (trBalance != 0) {
                AccountDto accountDto = accountRepository.findByClientIdAndCurrencyId(client.getId(), currencyId);
                if (accountDto == null) {
                    accountRepository.save(new AccountDto(client.getId(), currencyId, 0.0));
                    accountDto = accountRepository.findByClientIdAndCurrencyId(client.getId(), currencyId);
                }
                oldBalance = accountDto.getAmount();
            }
        } else {
            AccountDto accountDto = accountRepository.findByClientIdAndCurrencyId(client.getId(), currencyId);
            if (accountDto == null) {
                accountRepository.save(new AccountDto(client.getId(), currencyId, 0.0));
                accountDto = accountRepository.findByClientIdAndCurrencyId(client.getId(), currencyId);
            }
            oldBalance = accountDto.getAmount();
            trBalance = 0.0;
        }
        double balance = updateCurrencyAmount(client.getId(), currencyId, rate, commission, amount, transportation);
        TransactionDto transactionDto = formatToDto(new Transaction(transactionId, date, client, currency, comment, rate, commission, amount,
               trBalance + balance - oldBalance,  transportation,  commentColor, amountColor,
                inputColor, outputColor, tarifColor, commissionColor, rateColor, transportationColor, user));
        transactionRepository.save(transactionDto);
        logger.debug("Transaction saved");
        return balance;
    }

    public double update(int transactionId, Timestamp date, String clientName, String comment, int currencyId, double rate,
                             double commission, double amount, double transportation, String pibColor,
                             String amountColor, int userId, Double trBalance, String inputColor,
                         String outputColor, String tarifColor, String commissionColor, String rateColor, String transportationColor) throws RuntimeException {
        logger.debug("Saving transaction");
        //if (rate <= 0) throw new RuntimeException("Неверное значение курса. Введите положительное значение");
        if (commission < -100 || commission > 100) throw new RuntimeException("Неверная величина комиссии. " +
                "Введите значение от -100 до 100 включительно");
        Client client = clientManager.getClientExactly(clientName);
        Currency currency = currencyManager.getCurrency(currencyId);
        User user = userManager.getUser(userId);
        AccountDto accountDto = accountRepository.findByClientIdAndCurrencyId(client.getId(), currencyId);
        if (accountDto == null) {
            accountRepository.save(new AccountDto(client.getId(), currencyId, 0.0));
            accountDto = accountRepository.findByClientIdAndCurrencyId(client.getId(), currencyId);
        }
        double oldBalance = accountDto.getAmount();
        double newBalance = updateCurrencyAmount(client.getId(), currencyId, rate, commission, amount, transportation);
        double result = trBalance + newBalance - oldBalance;
        TransactionDto transactionDto = formatToDto(new Transaction(transactionId, date, client, currency, comment, rate, commission, amount,
                result, transportation, pibColor, amountColor, inputColor,
                outputColor, tarifColor, commissionColor, rateColor, transportationColor, user));
        transactionRepository.save(transactionDto);
        logger.debug("Transaction saved");
        return result;
    }

    private double updateCurrencyAmount(int clientId, int currencyId, double rate, double commission, double amount,
                                            double transportation) {
        logger.debug("Updating currency amount");
        Optional<CurrencyDto> currencyOpt = currencyRepository.findById(currencyId);
        if (currencyOpt.isEmpty()) throw new RuntimeException("Валюта не найдена");
        logger.trace("Found currency by id=" + currencyId);
        AccountDto accountDto = accountRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        if (accountDto == null) {
            accountRepository.save(new AccountDto(clientId, currencyId, 0.0));
            accountDto = accountRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        }
        logger.trace("Found account by clientId=" + clientId + " and currencyId=" + currencyId);
        double balance = accountDto.getAmount() + amount * ( 1 + commission / 100) + transportation;
        accountDto.setAmount(balance);
        accountRepository.save(accountDto);
        logger.debug("Currency amount updated");
        return balance;
    }

    public double undoTransaction(int transactionId, String clientName, int currencyId) {
        logger.debug("Undoing transaction");
        Client client = clientManager.getClientExactly(clientName);
        TransactionDto transactionDto = transactionRepository
                .findByIdAndClientIdAndCurrencyIdOrderByDate(transactionId, client.getId(), currencyId);
        logger.trace("Found transaction by id=" + transactionId + " and clientId=" + client.getId());
        return updateCurrencyAmount(client.getId(), transactionDto.getCurrencyId(), transactionDto.getRate(),
                transactionDto.getCommission(), -transactionDto.getAmount(), -transactionDto.getTransportation());
    }

    public void updateNext(String clientName, List<Integer> currencyId, Double balanceDif, Timestamp date) {
        logger.debug("Updating amount of next transactions");
        Client client = clientManager.getClientExactly(clientName);
        List<TransactionDto> transactionDtoList = transactionRepository
                .findAllByClientIdAndCurrencyIdInAndDateBetweenOrderByDateAscIdAsc(
                        client.getId(), currencyId, new Timestamp(date.getTime()),
                        new Timestamp(System.currentTimeMillis()));
        logger.trace("Found transactions by clientId=" + client.getId() + " currencyId=" + currencyId +
                " and date after: " + date);
        logger.trace("next transactions: " + transactionDtoList);
        for (TransactionDto transactionDto : transactionDtoList) {
            transactionDto.setBalance(transactionDto.getBalance() + balanceDif);
            transactionRepository.save(transactionDto);
        }
        logger.debug("Transactions updated");
    }

    public void deleteTransaction(List<Transaction> transaction) {
        logger.debug("Deleting transactions");
        List<TransactionDto> transactionDtoList = new ArrayList<>();
        for (Transaction t : transaction) {
            AccountDto accountDto = accountRepository.findByClientIdAndCurrencyId(t.getClient().getId(), t.getCurrency().getId());
            if (accountDto == null) {
                accountRepository.save(new AccountDto(t.getClient().getId(), t.getCurrency().getId(), 0.0));
                accountDto = accountRepository.findByClientIdAndCurrencyId(t.getClient().getId(), t.getCurrency().getId());
            }
            double oldBalance = accountDto.getAmount();
            logger.trace("Old balance: " + oldBalance);
            double newBalance = undoTransaction(t.getId(), t.getClient().getPib(), t.getCurrency().getId());
            logger.trace("New balance: " + newBalance);
            TransactionDto transactionDto = formatToDto(t);
            List<Integer> currencyId = new ArrayList<>();
            currencyId.add(t.getCurrency().getId());
            updateNext(t.getClient().getPib(), currencyId, newBalance - oldBalance, transactionDto.getDate());
            transactionDtoList.add(transactionDto);
        }
        transactionRepository.deleteAll(transactionDtoList);
        logger.debug("Transactions deleted");
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void safeDeleteTransaction(List<Transaction> transaction) {
        logger.debug("Deleting transactions");
        List<TransactionDto> transactionDtoList = new ArrayList<>();
        for (Transaction t : transaction) {
            TransactionDto transactionDto = formatToDto(t);
            transactionDtoList.add(transactionDto);
        }
        transactionRepository.deleteAll(transactionDtoList);
        logger.debug("Transactions deleted");
    }

    public Account addTotal(List<Account> clients) {
        logger.debug("Adding account total");
        Account total = new Account();
        total.setClient(new Client("Всего"));
        Map<Currency, Account.Properties> currencyDoubleMap = new TreeMap<>((o1, o2) -> {
            if (Objects.equals(o1.getName(), "UAH") || Objects.equals(o2.getName(), "PLN")) return -1;
            if (Objects.equals(o2.getName(), "UAH") || Objects.equals(o1.getName(), "PLN")) return 1;
            if (Objects.equals(o1.getName(), "USD") || Objects.equals(o2.getName(), "RUB")) return -1;
            if (Objects.equals(o2.getName(), "USD") || Objects.equals(o1.getName(), "RUB")) return 1;
            return 0;
        });
        List<Currency> currencies = currencyManager.getAllCurrencies();
        for (Currency currency : currencies) {
            double sum = 0;
            for (Account ac : clients)  {
                sum += ac.getCurrencies().entrySet().stream()
                        .filter(c -> Objects.equals(c.getKey().getId(), currency.getId()))
                        .findFirst().orElse(new Map.Entry<>() {
                            @Override
                            public Currency getKey() {
                                return null;
                            }

                            @Override
                            public Account.Properties getValue() {
                                return new Account.Properties(0.0, null);
                            }

                            @Override
                            public Account.Properties setValue(Account.Properties value) {
                                return null;
                            }

                        }).getValue().getAmount();
            }
           currencyDoubleMap.put(currency, new Account.Properties(sum, null));
        }
        total.setCurrencies(currencyDoubleMap);
        logger.trace("Total: " + total);
        return total;
    }

    private TransactionDto formatToDto(Transaction transaction) {
        logger.debug("Formatting transaction from entity to dto");
        TransactionDto transactionDto = new TransactionDto();
        if (transaction.getId() != 0) transactionDto.setId(transaction.getId());
        transactionDto.setDate(transaction.getDate());
        transactionDto.setClientId(transaction.getClient().getId());
        transactionDto.setCurrencyId(transaction.getCurrency().getId());
        transactionDto.setComment(transaction.getComment());
        transactionDto.setRate(transaction.getRate());
        transactionDto.setCommission(transaction.getCommission());
        transactionDto.setAmount(transaction.getAmount());
        transactionDto.setBalance(transaction.getBalance());
        transactionDto.setTransportation(transaction.getTransportation());
        transactionDto.setCommentColor(transaction.getCommentColor());
        transactionDto.setAmountColor(transaction.getAmountColor());
        transactionDto.setInputColor(transaction.getInputColor());
        transactionDto.setOutputColor(transaction.getOutputColor());
        transactionDto.setTarifColor(transaction.getTarifColor());
        transactionDto.setCommissionColor(transaction.getCommissionColor());
        transactionDto.setRateColor(transaction.getRateColor());
        transactionDto.setTransportationColor(transaction.getTransportationColor());
        transactionDto.setUserId(transaction.getUser().getId());
        logger.trace("TransactionDTO: " + transactionDto);
        return transactionDto;
    }


    private Transaction formatFromDto(TransactionDto transactionDto) {
        logger.debug("Formatting from dto to entity");
        Transaction transaction = new Transaction();
        transaction.setId(transactionDto.getId());
        transaction.setDate(transactionDto.getDate());
        transaction.setClient(clientManager.getClient(transactionDto.getClientId()));
        transaction.setCurrency(currencyManager.getCurrency(transactionDto.getCurrencyId()));
        transaction.setComment(transactionDto.getComment());
        transaction.setBalance(transactionDto.getBalance());
        transaction.setRate(transactionDto.getRate());
        transaction.setCommission(transactionDto.getCommission());
        transaction.setAmount(transactionDto.getAmount());
        transaction.setTransportation(transactionDto.getTransportation());
        transaction.setCommentColor(transactionDto.getCommentColor());
        transaction.setAmountColor(transactionDto.getAmountColor());
        transaction.setUser(userManager.getUser(transactionDto.getUserId()));
        transaction.setInputColor(transactionDto.getInputColor());
        transaction.setOutputColor(transactionDto.getOutputColor());
        transaction.setTarifColor(transactionDto.getTarifColor());
        transaction.setCommissionColor(transactionDto.getCommissionColor());
        transaction.setRateColor(transactionDto.getRateColor());
        transaction.setTransportationColor(transactionDto.getTransportationColor());
        logger.trace("Transaction: " + transaction);
        return transaction;
    }

    public List<Transaction> getAllTransactions() {
        logger.debug("Getting all transactions");
        List<TransactionDto> transactionDtoList = transactionRepository.findAllByOrderByDateAsc();
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto : transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }

    public int getMaxId(){
        if (transactionRepository.getMaxId() == null) {
            return 0;
        }
        return transactionRepository.getMaxId();
    }

    public List<Transaction> getTransaction(int transactionId) {
        logger.debug("Getting transactions by id");
        List<TransactionDto> transactionDtoList = transactionRepository.findAllByIdOrderByDate(transactionId);
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto : transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }


    public List<Transaction> findByClientForDate(int clientId, List<Integer> currencyId, Timestamp startDate, Timestamp endDate) {
        logger.debug("getting transactions by clientId for date");
        List<TransactionDto> transactionDtoList = transactionRepository
                .findAllByClientIdAndCurrencyIdInAndDateBetweenOrderByDateAscIdAsc(
                        clientId, currencyId, new Timestamp(startDate.getTime() - 1), endDate);
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto : transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }

    public Transaction getTransactionByClientAndByIdAndByCurrencyId(int id, int clientId, int currencyId) {
        logger.debug("Getting transaction by id, clientId and currencyId");
        TransactionDto transactiondto = transactionRepository
                .findByIdAndClientIdAndCurrencyIdOrderByDate(id, clientId, currencyId);
        if (transactiondto == null) return null;
        return formatFromDto(transactiondto);
    }

    public void save(Transaction transaction) {
        logger.debug("Saving transaction");
        transactionRepository.save(formatToDto(transaction));
        logger.debug("Transaction saved");
    }

    public List<Transaction> findByClientAndCurrency(Client client, Currency currency) {
        logger.debug("Getting transaction by client and currency");
        List<Transaction> transactions = new ArrayList<>();
        List<TransactionDto> transactionDtoList = transactionRepository
                .findAllByClientIdAndCurrencyIdOrderByCurrencyIdAscDateAscIdAsc(client.getId(), currency.getId());
        for (TransactionDto transactionDto : transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }

    public Set<String> getAllComments() {
        logger.debug("finding comments");
        return new HashSet<>(transactionRepository.findAllComments());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteAfter(Timestamp afterDate) {
        logger.debug("deleting after " + afterDate);
        for (AccountDto accountDto : accountRepository.findAll()) {
            TransactionDto tr = transactionRepository
                    .findAllByClientIdAndCurrencyIdAndDateBetweenLimit1(
                            accountDto.getClientId(), accountDto.getCurrencyId(), afterDate, 0);
            if (tr != null) {
                logger.debug("find previous transaction");
                accountDto.setAmount(tr.getBalance());
            } else {
                logger.debug("previous transaction are not founded");
                accountDto.setAmount(0.0);
            }
            accountRepository.save(accountDto);
        }
        transactionRepository.deleteByDateBetween(afterDate, new Timestamp(System.currentTimeMillis()));
    }

    public TransactionDto findPrevious(int clientId, int currencyId, Timestamp afterDate, int trId) {
        logger.debug("finding previous");
        return transactionRepository
                .findAllByClientIdAndCurrencyIdAndDateBetweenLimit1(
                        clientId, currencyId, afterDate, trId);
    }

    public List<Transaction> findByUser(Integer managerId, Timestamp startDate, Timestamp endDate, String clientName, List<Integer> currencyId) {
        logger.debug("finding by user");
        Client client;
        try {
            client = clientManager.getClientExactly(clientName);
        } catch (RuntimeException e) {
            client = null;
        }
        List<TransactionDto> transactionDtoList;
        if (client == null) {
            transactionDtoList = transactionRepository.findAllByUserIdAndCurrencyIdInAndDateBetweenOrderByDateAsc(managerId, currencyId, startDate, endDate);
        } else {
            transactionDtoList = transactionRepository.findAllByUserIdAndClientIdAndCurrencyIdInAndDateBetweenOrderByDateAsc(managerId, client.getId(), currencyId, startDate, endDate);
        }
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto : transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }

    public List<Transaction> findById(Integer id) {
        logger.debug("finding by id");
        List<TransactionDto> transactionDtoList = transactionRepository.findAllById(id);
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto :
                transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }

    public Timestamp getAvailableDate(Timestamp date) {
        Timestamp nextDate = Timestamp.valueOf(LocalDate.ofInstant(new Date(date.getTime() + 86400000).toInstant(), ZoneId.systemDefault()).atStartOfDay());
        Timestamp currentDate = Timestamp.valueOf(LocalDate.ofInstant(new Date(date.getTime()).toInstant(), ZoneId.systemDefault()).atStartOfDay());
        Timestamp max = transactionRepository.getMaxDateForDay(currentDate, nextDate);
        if (max == null) {
            return currentDate;
        }
        return new Timestamp(max.getTime() + 1000);
    }
}
