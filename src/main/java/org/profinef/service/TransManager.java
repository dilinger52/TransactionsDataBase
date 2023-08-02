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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    @Autowired
    private final UserManager userManager;
    private static Logger logger = LoggerFactory.getLogger(TransManager.class);

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

    @Transactional
    public double remittance(int transactionId, Timestamp date, String clientName, String comment, int currencyId, double rate,
                             double commission, double amount, double transportation, String pibColor,
                             String amountColor, String balanceColor, int userId) throws RuntimeException {
        logger.debug("Saving transaction");
        //if (rate <= 0) throw new RuntimeException("Неверное значение курса. Введите положительное значение");
        if (commission < -100 || commission > 100) throw new RuntimeException("Неверная величина комиссии. " +
                "Введите значение от -100 до 100 включительно");
        Client client = clientManager.getClient(clientName);
        if (date == null) date = new Timestamp(System.currentTimeMillis());
        double balance = updateCurrencyAmount(client.getId(), currencyId, rate, commission, amount, transportation);
        TransactionDto transactionDto = formatToDto(transactionId, date, currencyId, rate, commission, amount,
                transportation, client, comment, balance, pibColor, amountColor, balanceColor, userId);
        transactionRepository.save(transactionDto);
        logger.debug("Transaction saved");
        return balance;
    }

    private double updateCurrencyAmount(int clientId, int currencyId, double rate, double commission, double amount,
                                        double transportation) {
        logger.debug("Updating currency amount");
        Optional<CurrencyDto> currencyOpt = currencyRepository.findById(currencyId);
        if (currencyOpt.isEmpty()) throw new RuntimeException("Валюта не найдена");
        logger.trace("Found currency by id=" + currencyId);
        AccountDto accountDto = accountRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        if (accountDto == null) {
            accountRepository.save(new AccountDto(clientId, currencyId, 0));
            accountDto = accountRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        }
        logger.trace("Found account by clientId=" + clientId + " and currencyId=" + currencyId);
        double balance = accountDto.getAmount() + amount * (1 + commission/100) + transportation;
        accountDto.setAmount(balance);
        accountRepository.save(accountDto);
        logger.debug("Currency amount updated");
        return balance;
    }

    public double undoTransaction(int transactionId, String clientName, int currencyId) {
        logger.debug("Undoing transaction");
        Client client = clientManager.getClient(clientName);
        TransactionDto transactionDto = transactionRepository
                .findByIdAndClientIdAndCurrencyIdOrderByDate(transactionId, client.getId(), currencyId);
        logger.trace("Found transaction by id=" + transactionId + " and clientId=" + client.getId());
        return updateCurrencyAmount(client.getId(), transactionDto.getCurrencyId(), transactionDto.getRate(),
                transactionDto.getCommission(), -transactionDto.getAmount(), transactionDto.getTransportation());
    }
    @Transactional
    public void update(int transactionId, String clientName, String comment, int currencyId, double rate, double commission,
                       double amount, double transportation, int userId) {
        logger.debug("Updating transaction");
        Client client = clientManager.getClient(clientName);
        if (rate <= 0) throw new RuntimeException("Неверное значение курса. Введите положительное значение");
        if (commission < -100 || commission > 100) throw new RuntimeException("Неверная величина комиссии. " +
                "Введите значение от -100 до 100 включительно");
        if (transportation < 0) throw new RuntimeException("Неверная стоимость инкасации. " +
                "Введите положительное значение");

        TransactionDto oldTransactionDto = transactionRepository
                .findByIdAndClientIdAndCurrencyIdOrderByDate(transactionId, client.getId(), currencyId);
        logger.trace("Found transaction by id=" + transactionId + " and clientId=" + client.getId());
        double oldTransactionBalance = oldTransactionDto.getBalance();
        logger.trace("Old transaction balance: " + oldTransactionBalance);
        double oldBalance = accountRepository.findByClientIdAndCurrencyId(client.getId(), currencyId).getAmount();
        logger.trace("Old balance: " + oldBalance);
        undoTransaction(transactionId, clientName, currencyId);
        double newBalance = remittance(transactionId, oldTransactionDto.getDate(), clientName, comment, currencyId, rate,
                commission, amount, transportation, oldTransactionDto.getPibColor(), oldTransactionDto.getAmountColor(),
                oldTransactionDto.getBalanceColor(), userId);
        logger.trace("New balance: " + newBalance);
        TransactionDto newTransactionDto = transactionRepository
                .findByIdAndClientIdOrderByDate(transactionId, client.getId());
        logger.trace("Found transaction by id=" + transactionId + " and clientId=" + client.getId());
        double balanceDif = newBalance - oldBalance;

        if (balanceDif != 0) {
            newTransactionDto.setBalance(oldTransactionBalance + balanceDif);
            transactionRepository.save(newTransactionDto);
            logger.debug("Transaction updated");
            List<Integer> cur = new ArrayList<>();
            cur.add(currencyId);
            updateNext(clientName, cur, balanceDif, newTransactionDto.getDate());
        } else {
            logger.debug("Transaction do not need to be updated");
        }
    }

    public void updateNext(String clientName, List<Integer> currencyId, Double balanceDif, Timestamp date) {
        logger.debug("Updating amount of next transactions");
        Client client = clientManager.getClient(clientName);
        List<TransactionDto> transactionDtoList = transactionRepository
                .findAllByClientIdAndCurrencyIdInAndDateBetweenOrderByCurrencyIdAscDateAsc(
                        client.getId(), currencyId, new Timestamp(date.getTime() + 1),
                        new Timestamp(System.currentTimeMillis()));
        logger.trace("Found transactions by clientId=" + client.getId() + " currencyId=" + currencyId +
                " and date after: " + date);
        for (TransactionDto transactionDto : transactionDtoList) {
            System.out.println(transactionDto.getBalance());
            System.out.println(balanceDif);
            transactionDto.setBalance(transactionDto.getBalance() + balanceDif);
            transactionRepository.save(transactionDto);
        }
        logger.debug("Transactions updated");
    }

    @Transactional
    public void deleteTransaction(List<Transaction> transaction) {
        logger.debug("Deleting transactions");
        List<TransactionDto> transactionDtoList = new ArrayList<>();
        for (Transaction t : transaction) {
            double oldBalance = accountRepository
                    .findByClientIdAndCurrencyId(t.getClient().getId(), t.getCurrency().getId()).getAmount();
            logger.trace("Old balance: " + oldBalance);
            double newBalance = undoTransaction(t.getId(), t.getClient().getPib(), t.getCurrency().getId());
            logger.trace("New balance: " + newBalance);
            TransactionDto transactionDto = formatToDto(t.getId(), t.getDate(), t.getCurrency().getId(), t.getRate(),
                    t.getCommission(), t.getAmount(), t.getTransportation(), t.getClient(), t.getComment(), t.getBalance(), t.getPibColor(),
            t.getAmountColor(), t.getBalanceColor(), t.getUser().getId());
            List<Integer> currencyId = new ArrayList<>();
            currencyId.add(t.getCurrency().getId());
            updateNext(t.getClient().getPib(), currencyId, newBalance - oldBalance, transactionDto.getDate());
            transactionDtoList.add(transactionDto);
        }
        transactionRepository.deleteAll(transactionDtoList);
        logger.debug("Transactions deleted");
    }

    @Transactional
    public void safeDeleteTransaction(List<Transaction> transaction) {
        logger.debug("Deleting transactions");
        List<TransactionDto> transactionDtoList = new ArrayList<>();
        for (Transaction t : transaction) {
            TransactionDto transactionDto = formatToDto(t.getId(), t.getDate(), t.getCurrency().getId(), t.getRate(),
                    t.getCommission(), t.getAmount(), t.getTransportation(), t.getClient(), t.getComment(), t.getBalance(), t.getPibColor(),
                    t.getAmountColor(), t.getBalanceColor(), t.getUser().getId());
            transactionDtoList.add(transactionDto);
        }
        transactionRepository.deleteAll(transactionDtoList);
        logger.debug("Transactions deleted");
    }

    public Account addTotal(List<Account> clients) {
        logger.debug("Adding account total");
        Account total = new Account();
        total.setClient(new Client("Всего"));
        Map<Currency, Double> currencyDoubleMap = new TreeMap<>();
        List<Currency> currencies = currencyManager.getAllCurrencies();
        for (Currency currency :
                currencies) {
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
        logger.trace("Total: " + total);
        return total;
    }

    private TransactionDto formatToDto(int transactionId, Timestamp date, int currencyId, double rate,
                                       double commission, double amount, double transportation, Client client,
                                       String comment, double balance, String pibColor, String amountColor,
                                       String balanceColor, int userId) {
        logger.debug("Formatting transaction from entity to dto");
        TransactionDto transactionDto = new TransactionDto();
        if (transactionId != 0) transactionDto.setId(transactionId);
        transactionDto.setDate(date);
        transactionDto.setClientId(client.getId());
        transactionDto.setCurrencyId(currencyId);
        transactionDto.setComment(comment);
        transactionDto.setRate(rate);
        transactionDto.setCommission(commission);
        transactionDto.setAmount(amount);
        transactionDto.setBalance(balance);
        transactionDto.setTransportation(transportation);
        transactionDto.setPibColor(pibColor);
        transactionDto.setAmountColor(amountColor);
        transactionDto.setBalanceColor(balanceColor);
        transactionDto.setUserId(userId);
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
        transaction.setPibColor(transactionDto.getPibColor());
        transaction.setAmountColor(transactionDto.getAmountColor());
        transaction.setBalanceColor(transactionDto.getBalanceColor());
        transaction.setUser(userManager.getUser(transactionDto.getUserId()));
        logger.trace("Transaction: " + transaction);
        return transaction;
    }

    public List<Transaction> getAllTransactions() {
        logger.debug("Getting all transactions");
        List<TransactionDto> transactionDtoList = transactionRepository.findAllByOrderByDateDesc();
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
                .findAllByClientIdAndCurrencyIdInAndDateBetweenOrderByCurrencyIdAscDateAsc(
                        clientId, currencyId, startDate, endDate);
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
        transactionRepository.save(formatToDto(transaction.getId(), transaction.getDate(),
                transaction.getCurrency().getId(), transaction.getRate(), transaction.getCommission(),
                transaction.getAmount(), transaction.getTransportation(), transaction.getClient(),
                transaction.getComment(), transaction.getBalance(), transaction.getPibColor(),
                transaction.getAmountColor(), transaction.getBalanceColor(), transaction.getUser().getId()));
        logger.debug("Transaction saved");
    }

    public List<Transaction> findByClientAndCurrency(Client client, Currency currency) {
        logger.debug("Getting transaction by client and currency");
        List<Transaction> transactions = new ArrayList<>();
        List<TransactionDto> transactionDtoList = transactionRepository
                .findAllByClientIdAndCurrencyIdOrderByCurrencyIdAscDateAsc(client.getId(), currency.getId());
        for (TransactionDto transactionDto : transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }

    public Set<String> getAllComments() {
        return new HashSet<>(transactionRepository.findAllComments());
    }

    public Transaction findByIdAndClientIdOrderByDate(Integer transactionId, int id) {
        return formatFromDto(transactionRepository.findByIdAndClientIdOrderByDate(transactionId, id));
    }
    @Transactional
    public void deleteAfter(Timestamp afterDate) {
        for (AccountDto accountDto : accountRepository.findAll()) {
            TransactionDto tr = transactionRepository
                    .findAllByClientIdAndCurrencyIdAndDateBetweenLimit1(
                            accountDto.getClientId(), accountDto.getCurrencyId(), afterDate);
            if (tr != null) {
                accountDto.setAmount(tr.getBalance());
            } else {
                accountDto.setAmount(0.0);
            }
            accountRepository.save(accountDto);
        }
        transactionRepository.deleteByDateBetween(afterDate, new Timestamp(System.currentTimeMillis()));
    }

    public TransactionDto findPrevious(int clientId, int currencyId, Timestamp afterDate) {
        return transactionRepository
                .findAllByClientIdAndCurrencyIdAndDateBetweenLimit1(
                        clientId, currencyId, afterDate);
    }

    public List<Transaction> findByUser(Integer managerId, Timestamp startDate, Timestamp endDate, String clientName, List<Integer> currencyId) {
        Client client = clientManager.getClient(clientName);
        List<TransactionDto> transactionDtoList = transactionRepository.findAllByUserIdAndClientIdAndCurrencyIdInAndDateBetween(managerId, client.getId(), currencyId, startDate, endDate);
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto : transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }
}
