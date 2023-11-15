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

/**
 * Класс предназначен для данных записей полученных из БД перед отправкой их в контролер
 */

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

    /**
     * Метод создает новую запись или обновляет существующую с предоставленными параметрами, пересчитывает и обновляет
     * баланс соответствующего клиента в соответствующей валюте
     * @param transactionId ИД новой записи
     * @param date дата новой записи
     * @param clientName имя клиента
     * @param comment комментарий
     * @param currencyId ИД валюты
     * @param rate курс валюты
     * @param commission комиссия (в %)
     * @param amount объем
     * @param transportation инкассация
     * @param commentColor цвет комментария
     * @param amountColor цвет "итого"
     * @param userId ИД пользователя создавшего запись
     * @param trBalance старый баланс записи (или 0 для новой)
     * @param inputColor цвет приема
     * @param outputColor цвет выдачи
     * @param tarifColor цвет тарифа
     * @param commissionColor цвет комиссии
     * @param rateColor цвет курса
     * @param transportationColor цвет инкассации
     * @param balanceColor цвет баланса
     * @return баланс после добавления записи
     * @throws RuntimeException выбрасывается при неверно введенных параметрах, либо отсутствии в БД необходимой
     * информации
     */
    public double remittance(int transactionId, Timestamp date, String clientName, String comment, int currencyId, double rate,
                                 double commission, double amount, double transportation, String commentColor,
                                 String amountColor, int userId, Double trBalance, String inputColor,
                             String outputColor, String tarifColor, String commissionColor, String rateColor, String transportationColor,
                             String balanceColor) throws RuntimeException {
        logger.debug("Saving transaction");
        // проверка значения курса
        //if (rate <= 0) throw new RuntimeException("Неверное значение курса. Введите положительное значение");
        // проверка величины комиссии
        if (commission < -100 || commission > 100) throw new RuntimeException("Неверная величина комиссии. " +
                "Введите значение от -100 до 100 включительно");
        Client client = clientManager.getClientExactly(clientName);
        Currency currency = currencyManager.getCurrency(currencyId);
        User user = userManager.getUser(userId);
        // если дата не задана устанавливаем текущие дату и время
        if (date == null) date = new Timestamp(System.currentTimeMillis());
        // определяем старый баланс
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
        // обновляем баланс
        double balance = updateCurrencyAmount(client.getId(), currencyId, rate, commission, amount, transportation);
        // сохраняем запись
        TransactionDto transactionDto = formatToDto(new Transaction(transactionId, date, client, currency, comment, rate, commission, amount,
               trBalance + balance - oldBalance,  transportation,  commentColor, amountColor,
                inputColor, outputColor, tarifColor, commissionColor, rateColor, transportationColor, balanceColor, user));
        transactionRepository.save(transactionDto);
        logger.debug("Transaction saved");
        return balance;
    }

    /**
     * Метод обновляет существующую запись с предоставленными параметрами, пересчитывает и обновляет
     * баланс соответствующего клиента в соответствующей валюте
     * @param transactionId ИД новой записи
     * @param date дата новой записи
     * @param clientName имя клиента
     * @param comment комментарий
     * @param currencyId ИД валюты
     * @param rate курс валюты
     * @param commission комиссия (в %)
     * @param amount объем
     * @param transportation инкассация
     * @param amountColor цвет "итого"
     * @param userId ИД пользователя создавшего запись
     * @param trBalance старый баланс записи (или 0 для новой)
     * @param inputColor цвет приема
     * @param outputColor цвет выдачи
     * @param tarifColor цвет тарифа
     * @param commissionColor цвет комиссии
     * @param rateColor цвет курса
     * @param transportationColor цвет инкассации
     * @param balanceColor цвет баланса
     * @return баланс записи после ее добавления
     * @throws RuntimeException выбрасывается при неверно введенных параметрах, либо отсутствии в БД необходимой
     * информации
     */
    public double update(int transactionId, Timestamp date, String clientName, String comment, int currencyId, double rate,
                             double commission, double amount, double transportation, String pibColor,
                             String amountColor, int userId, Double trBalance, String inputColor,
                         String outputColor, String tarifColor, String commissionColor, String rateColor, String transportationColor,
                         String balanceColor) throws RuntimeException {
        logger.debug("Saving transaction");
        // проверка на корректность курса
        //if (rate <= 0) throw new RuntimeException("Неверное значение курса. Введите положительное значение");
        // проверка на корректность тарифа
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
        // старый баланс
        double oldBalance = accountDto.getAmount();
        // новый баланс
        double newBalance = updateCurrencyAmount(client.getId(), currencyId, rate, commission, amount, transportation);
        double result = trBalance + newBalance - oldBalance;
        // сохраняем запись
        TransactionDto transactionDto = formatToDto(new Transaction(transactionId, date, client, currency, comment, rate, commission, amount,
                result, transportation, pibColor, amountColor, inputColor,
                outputColor, tarifColor, commissionColor, rateColor, transportationColor, balanceColor, user));
        transactionRepository.save(transactionDto);
        logger.debug("Transaction saved");
        return result;
    }

    /**
     * Метод рассчитывает и обновляет баланс валюты клиента на основании новой записи
     * @param clientId ИД клиента
     * @param currencyId ИД валюты
     * @param rate курс
     * @param commission тариф
     * @param amount объем
     * @param transportation инкассация
     * @return новый баланс валюты
     */
    private double updateCurrencyAmount(int clientId, int currencyId, double rate, double commission, double amount,
                                            double transportation) {
        logger.debug("Updating currency amount");
        Optional<CurrencyDto> currencyOpt = currencyRepository.findById(currencyId);
        // проверка на наличие валюты в БД
        if (currencyOpt.isEmpty()) throw new RuntimeException("Валюта не найдена");
        logger.trace("Found currency by id=" + currencyId);
        AccountDto accountDto = accountRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        if (accountDto == null) {
            accountRepository.save(new AccountDto(clientId, currencyId, 0.0));
            accountDto = accountRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        }
        logger.trace("Found account by clientId=" + clientId + " and currencyId=" + currencyId);
        // расчет нового баланса
        double balance = accountDto.getAmount() + amount * ( 1 + commission / 100 ) + transportation;
        // сохранение аккаунта с новым балансом
        accountDto.setAmount(balance);
        accountRepository.save(accountDto);
        logger.debug("Currency amount updated");
        return balance;
    }

    /**
     * Метод откатывает изменение баланса вызванного добавлением записи, для ее последующего удаления
     * @param transactionId ИД записи для отката
     * @param clientName имя клиента
     * @param currencyId ИД валюты
     * @return новый баланс после отката
     */
    public double undoTransaction(int transactionId, String clientName, int currencyId) {
        logger.debug("Undoing transaction");
        Client client = clientManager.getClientExactly(clientName);
        TransactionDto transactionDto = transactionRepository
                .findByIdAndClientIdAndCurrencyIdOrderByDate(transactionId, client.getId(), currencyId);
        logger.trace("Found transaction by id=" + transactionId + " and clientId=" + client.getId());
        return updateCurrencyAmount(client.getId(), transactionDto.getCurrencyId(), transactionDto.getRate(),
                transactionDto.getCommission(), -transactionDto.getAmount(), -transactionDto.getTransportation());
    }

    /**
     * Метод обновляет балансы в записях следующих после указанной даты
     * @param clientName имя клиента, для которого выполняется обновление
     * @param currencyId ИД валюты по которой выполняется обновление
     * @param balanceDif разница на которую необходимо изменить балансы
     * @param date дата начиная с которой необходимо обновлять балансы в записях
     */
    public void updateNext(String clientName, List<Integer> currencyId, Double balanceDif, Timestamp date) {
        logger.debug("Updating amount of next transactions");
        Client client = clientManager.getClientExactly(clientName);
        // формируем список записей для обновления
        List<TransactionDto> transactionDtoList = transactionRepository
                .findAllByClientIdAndCurrencyIdInAndDateBetweenOrderByDateAscIdAsc(
                        client.getId(), currencyId, new Timestamp(date.getTime()),
                        new Timestamp(System.currentTimeMillis()));
        logger.trace("Found transactions by clientId=" + client.getId() + " currencyId=" + currencyId +
                " and date after: " + date);
        logger.trace("next transactions: " + transactionDtoList);
        // обновляем и сохраняем записи
        for (TransactionDto transactionDto : transactionDtoList) {
            transactionDto.setBalance(transactionDto.getBalance() + balanceDif);
            transactionRepository.save(transactionDto);
        }
        logger.debug("Transactions updated");
    }

    /**
     * Метод удаляет все записи из списка. В том числе уменьшает балансы на итоговую величину записи
     * @param transaction список записей для удаления
     */
    public void deleteTransaction(List<Transaction> transaction) {
        logger.debug("Deleting transactions");
        List<TransactionDto> transactionDtoList = new ArrayList<>();
        for (Transaction t : transaction) {
            // находим старый баланс
            AccountDto accountDto = accountRepository.findByClientIdAndCurrencyId(t.getClient().getId(), t.getCurrency().getId());
            if (accountDto == null) {
                accountRepository.save(new AccountDto(t.getClient().getId(), t.getCurrency().getId(), 0.0));
                accountDto = accountRepository.findByClientIdAndCurrencyId(t.getClient().getId(), t.getCurrency().getId());
            }
            double oldBalance = accountDto.getAmount();
            logger.trace("Old balance: " + oldBalance);

            // рассчитываем новый баланс
            double newBalance = undoTransaction(t.getId(), t.getClient().getPib(), t.getCurrency().getId());
            logger.trace("New balance: " + newBalance);

            // обновляем балансы последующих записей
            TransactionDto transactionDto = formatToDto(t);
            List<Integer> currencyId = new ArrayList<>();
            currencyId.add(t.getCurrency().getId());
            updateNext(t.getClient().getPib(), currencyId, newBalance - oldBalance, transactionDto.getDate());
            transactionDtoList.add(transactionDto);
        }
        // удаляем все записи из списка
        transactionRepository.deleteAll(transactionDtoList);
        logger.debug("Transactions deleted");
    }

    /**
     * Метод удаляет все записи из списка, не меняя значения баланса клиента
     * @param transaction список записей для удаления
     */
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

    /**
     * Метод рассчитывает суммарные балансы по всем клиентам и возвращает в виде нового аккаунта
     * @param clients список аккаунтов клиентов с балансами валют
     * @return "аккаунт клиента "total"
     */
    public Account addTotal(List<Account> clients) {
        logger.debug("Adding account total");
        // создаем аккаунт total
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
            // суммируем балансы аккаунтов в соответствующей валюте
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
            // добавляем в качестве баланса нового аккаунта
           currencyDoubleMap.put(currency, new Account.Properties(sum, null));
        }
        total.setCurrencies(currencyDoubleMap);
        logger.trace("Total: " + total);
        return total;
    }

    /**
     * Метод преобразует запись со ссылками на связанные объекты в запись с ИД этих объектов
     * @param transaction запись для преобразования
     * @return запись после преобразования
     */
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
        transactionDto.setBalanceColor(transaction.getBalanceColor());
        transactionDto.setUserId(transaction.getUser().getId());
        logger.trace("TransactionDTO: " + transactionDto);
        return transactionDto;
    }

    /**
     * Метод преобразует запись с ИД связанных объектов в запись со ссылками на эти объекты
     * @param transactionDto запись для преобразования
     * @return записб после преобразования
     */
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
        transaction.setBalanceColor(transactionDto.getBalanceColor());
        logger.trace("Transaction: " + transaction);
        return transaction;
    }

    /**
     * Метод собирает все записи в базе данных в список
     * @return список всех записей
     */
    public List<Transaction> getAllTransactions() {
        logger.debug("Getting all transactions");
        List<TransactionDto> transactionDtoList = transactionRepository.findAllByOrderByDateAsc();
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto : transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }

    /**
     * Метод ищет максимальный ИД записи в базе данных
     * @return максимальный ИД записи, или 0 (если записей нет)
     */
    public int getMaxId(){
        if (transactionRepository.getMaxId() == null) {
            return 0;
        }
        return transactionRepository.getMaxId();
    }

    /**
     * Метод ищет записи в базе данных по ИД
     * @param transactionId ИД записей
     * @return список записей
     */
    public List<Transaction> getTransaction(int transactionId) {
        logger.debug("Getting transactions by id");
        List<TransactionDto> transactionDtoList = transactionRepository.findAllByIdOrderByDate(transactionId);
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionDto transactionDto : transactionDtoList) {
            transactions.add(formatFromDto(transactionDto));
        }
        return transactions;
    }

    /**
     * Метод ищет записи для указанного клиента, валюты и промежутка времени
     * @param clientId ИД клиента
     * @param currencyId ИД валюты
     * @param startDate начальная дата требуемого промежутка
     * @param endDate конечная дата требуемого промежутка
     * @return список записей удовлетворяющих условиям
     */
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

    /**
     * Метод ищет запись по трем идентификаторам, обеспечивающим уникальность: ИД записи, ИД клиента и ИД валюты
     * @param id ИД записи
     * @param clientId ИД клиента
     * @param currencyId ИД валюты
     * @return искомую запись, или null если она не существует
     */
    public Transaction getTransactionByClientAndByIdAndByCurrencyId(int id, int clientId, int currencyId) {
        logger.debug("Getting transaction by id, clientId and currencyId");
        TransactionDto transactiondto = transactionRepository
                .findByIdAndClientIdAndCurrencyIdOrderByDate(id, clientId, currencyId);
        if (transactiondto == null) return null;
        return formatFromDto(transactiondto);
    }

    /**
     * Метод сохраняет запись
     * @param transaction запись для сохранения
     */
    public void save(Transaction transaction) {
        logger.debug("Saving transaction");
        transactionRepository.save(formatToDto(transaction));
        logger.debug("Transaction saved");
    }

    /**
     * Метод ищет записи в БД по заданным клиенту и валюте
     * @param client клиент для которого нужно найти записи
     * @param currency валюта для которой нужно найти записи
     * @return список записей удовлетворяющих условию
     */
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

    /**
     * Метод ищет в БД все ранее введенные уникальные комментарии
     * @return список комментариев
     */
    public Set<String> getAllComments() {
        logger.debug("finding comments");
        return new HashSet<>(transactionRepository.findAllComments());
    }

    /**
     * Метод удаляет из БД все записи дата и время которых позднее указанной. Также меняет значение баланса соответствующих
     * аккаунтов на значение баланса в последней записи соответствующих клиента и валюты не попавшей в этот промежуток,
     * либо на 0 если такая запись не существует
     * @param afterDate дата после которой необходимо удалить записи
     */
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

    /**
     * Метод осуществляет поиск предыдущей записи для выбранных клиента и валюты, а также исключением из выборки текущей
     * записи по ИД и даты не позднее которой должна быть найденная запись
     * @param clientId ИД клиента записи
     * @param currencyId ИД валюты записи
     * @param afterDate наиболее поздняя дата записи
     * @param trId ИД записи, игнорируемой при поиске
     * @return предыдущая запись
     */
    public TransactionDto findPrevious(int clientId, int currencyId, Timestamp afterDate, int trId) {
        logger.debug("finding previous");
        return transactionRepository
                .findAllByClientIdAndCurrencyIdAndDateBetweenLimit1(
                        clientId, currencyId, afterDate, trId);
    }

    /**
     * Метод ищет все записи созданные определенным пользователем, в указанный промежуток времени, для указанного
     * клиента и валюты
     * @param managerId ИД пользователя создавшего записи
     * @param startDate начальная дата промежутка времени
     * @param endDate конечная дата промежутка времени
     * @param clientName имя клиента, участвовавшего в записях
     * @param currencyId ИД валюты, участвовавшей в записях
     * @return список записей удовлетворяющих условию
     */
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

    /**
     * Метод ищет связанные записи в БД по ИД записи
     * @param id ИД записи, для которой необходимо найти связанные записи
     * @return список связанных записей
     */
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

    /**
     * Метод осуществляет поиск в БД последней по времени записи в указанный день, и возвращает эту дату со временем
     * на секунду позднее
     * @param date дата для которой выполняется поиск
     * @return дату и время
     */
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
