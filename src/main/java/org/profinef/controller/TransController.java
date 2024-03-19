package org.profinef.controller;

import org.profinef.entity.*;
import org.profinef.repository.AccountRepository;
import org.profinef.repository.CurrencyRepository;
import org.profinef.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Класс предназначен для данных записей полученных из БД перед отправкой их в контролер
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/transactions")
public class TransController {
    @Autowired
    private final TransactionRepository transactionRepository;
    @Autowired
    private final AccountRepository accountRepository;
    @Autowired
    private final ClientController clientController;
    @Autowired
    private final CurrencyRepository currencyRepository;
    @Autowired
    private final UserController userController;
    private static final Logger logger = LoggerFactory.getLogger(TransController.class);

    public TransController(TransactionRepository transactionRepository, AccountRepository accountRepository,
                           ClientController clientController, CurrencyRepository currencyRepository, UserController userController) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.clientController = clientController;
        this.currencyRepository = currencyRepository;
        this.userController = userController;
    }

    /**
     * Метод создает новую запись или обновляет существующую с предоставленными параметрами, пересчитывает и обновляет
     * баланс соответствующего клиента в соответствующей валюте

     * @param date дата новой записи

     * @param comment комментарий

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
    @PostMapping("/remittance")
    public double remittance(@RequestBody int operationId, Timestamp date, Account account, String comment, double rate,
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
        User user = userController.getUser(userId);
        // если дата не задана устанавливаем текущие дату и время
        if (date == null) date = new Timestamp(System.currentTimeMillis());
        // определяем старый баланс
        double oldBalance = 0.0;
        if (trBalance == null || trBalance != 0) {
            oldBalance = account.getBalance();
        }
        if(trBalance == null) {
            trBalance = 0.0;
        }
        // обновляем баланс
        double balance = updateCurrencyAmount(account, rate, commission, amount, transportation);
        // сохраняем запись
        Transaction transaction = new Transaction(operationId, date, account, comment, rate, commission, amount,
               trBalance + balance - oldBalance,  transportation,  commentColor, amountColor,
                inputColor, outputColor, tarifColor, commissionColor, rateColor, transportationColor, balanceColor, user);
        transactionRepository.save(transaction);
        logger.debug("Transaction saved");
        return balance;
    }

    /**
     * Метод обновляет существующую запись с предоставленными параметрами, пересчитывает и обновляет
     * баланс соответствующего клиента в соответствующей валюте
     * @param operationId ИД новой записи
     * @param date дата новой записи

     * @param comment комментарий

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
    @PostMapping("/update")
    public double update(@RequestBody int operationId, Timestamp date, Account account, String comment, double rate,
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
        User user = userController.getUser(userId);
        // старый баланс
        double oldBalance = account.getBalance();
        // новый баланс
        double newBalance = updateCurrencyAmount(account, rate, commission, amount, transportation);
        double result = trBalance + newBalance - oldBalance;
        // сохраняем запись
        Transaction transaction = new Transaction(operationId, date, account, comment, rate, commission, amount,
                result, transportation, pibColor, amountColor, inputColor,
                outputColor, tarifColor, commissionColor, rateColor, transportationColor, balanceColor, user);
        transactionRepository.save(transaction);
        logger.debug("Transaction saved");
        return result;
    }


    @PutMapping("/update")
    public List<Transaction> updateOperation(@RequestBody List<Transaction> transactions) throws RuntimeException {
        int operationId = getMaxId() + 1;
        if (transactions.size() < 2 && transactions.get(0).getOperationId() > 0) {
            List<Transaction> trs = transactionRepository.findAllByOperationId(transactions.get(0).getOperationId());
            if (trs.size() >= 2) {
                Transaction tr = new Transaction(trs.stream().filter(t -> !Objects.equals(t.getId(), transactions.get(0).getId())).findFirst().get());
                tr.setAmount(-transactions.get(0).getAmount());
                transactions.add(tr);
            }
        }

        for (Transaction transaction : transactions) {
            if (transaction.getAmount() == 0.0) continue;
            if (transaction.getOperationId() <= 0) transaction.setOperationId(operationId);
            logger.debug("Saving transaction");
            // проверка на корректность курса
            //if (rate <= 0) throw new RuntimeException("Неверное значение курса. Введите положительное значение");
            // проверка на корректность тарифа
            double commission = transaction.getCommission() != null ? transaction.getCommission() : 0;
            if (commission < -100 || commission > 100) throw new RuntimeException("Неверная величина комиссии. " +
                    "Введите значение от -100 до 100 включительно");
            Account account = accountRepository.findById(transaction.getAccount().getId()).orElse(null);
            if (account == null) {
                account = new Account();
                account.setClient(clientController.getClient(transaction.getAccount().getClient().getId()));
                account.setCurrency(currencyRepository.findById(transaction.getAccount().getCurrency().getId()).orElseThrow());
                account = accountRepository.save(account);
            }
            // старый баланс
            System.out.println(transaction);
            double oldBalance = account.getBalance();
            if (transaction.getId() > 0) {
                double res = undoTransaction(transaction.getId());
                System.out.println(res);
            }
            // новый баланс
            System.out.println(oldBalance);
            double newBalance = updateCurrencyAmount(account,
                    transaction.getRate(),
                    commission,
                    transaction.getAmount(),
                    transaction.getTransportation() == null ? 0 : transaction.getTransportation());
            System.out.println(newBalance);
            if (transaction.getBalance() == null || transaction.getBalance() == 0) {
                Optional<Transaction> trOpt = transactionRepository.findTopByAccountIdOrderByDateDesc(account.getId());
                if (trOpt.isPresent() && transaction.getDate().before(trOpt.get().getDate())) {
                    transaction.setBalance(findPrevious(account, transaction.getDate()).getBalance());
                } else {
                    transaction.setBalance(oldBalance);
                }
            }
            System.out.println(transaction);
            double result = transaction.getBalance() + newBalance - oldBalance;
            // сохраняем запись
            System.out.println(result);
            transaction.setBalance(result);
            Optional<Transaction> trOpt = transactionRepository.findTopByAccountIdOrderByDateDesc(account.getId());
            if (trOpt.isPresent() && transaction.getDate().before(trOpt.get().getDate())) {
                List<Integer> accountIds = new ArrayList<>();
                accountIds.add(account.getId());
                updateNext(accountIds, newBalance - oldBalance, transaction.getDate());
            }

            logger.debug("Transaction saved");
        }
        return transactionRepository.saveAll(transactions);
    }

    /**
     * Метод рассчитывает и обновляет баланс валюты клиента на основании новой записи


     * @param rate курс
     * @param commission тариф
     * @param amount объем
     * @param transportation инкассация
     * @return новый баланс валюты
     */
    @PostMapping("/updateCurrencyAmount")
    private double updateCurrencyAmount(@RequestBody Account account, double rate, double commission, double amount,
                                            double transportation) {
        logger.debug("Updating currency amount");
        // расчет нового баланса
        double balance = account.getBalance() + amount * ( 1 + commission / 100 ) + transportation;
        // сохранение аккаунта с новым балансом
        account.setBalance(balance);
        accountRepository.save(account);
        logger.debug("Currency amount updated");
        return balance;
    }

    /**
     * Метод откатывает изменение баланса вызванного добавлением записи, для ее последующего удаления

     * @return новый баланс после отката
     */
    @PostMapping("/undoTransaction/{id}")
    public double undoTransaction(@PathVariable int id) {
        logger.debug("Undoing transaction");
        Transaction transaction = transactionRepository.findById(id).orElseThrow();
        System.out.println(transaction);
        logger.trace("Found transaction by id=" + id);
        return updateCurrencyAmount(transaction.getAccount(), transaction.getRate(),
                transaction.getCommission() == null ? 0 : transaction.getCommission(),
                -transaction.getAmount(),
                transaction.getTransportation() == null ? 0 : -transaction.getTransportation());
    }

    /**
     * Метод обновляет балансы в записях следующих после указанной даты

     * @param balanceDif разница на которую необходимо изменить балансы
     * @param date дата начиная с которой необходимо обновлять балансы в записях
     */
    @PostMapping("/updateNext")
    public void updateNext(@RequestBody List<Integer> accountIds, Double balanceDif, Timestamp date) {
        logger.debug("Updating amount of next transactions");
        System.out.println("here");
        // формируем список записей для обновления
        List<Transaction> transactionList = transactionRepository
                .findAllByAccountIdInAndDateBetweenOrderByDateAscIdAsc(
                        accountIds, new Timestamp(date.getTime()),
                        new Timestamp(System.currentTimeMillis()));
        logger.trace("Found transactions by accountIds=" + accountIds + " and date after: " + date);
        logger.trace("next transactions: " + transactionList);
        System.out.println(transactionList);
        // обновляем и сохраняем записи
        for (Transaction transaction : transactionList) {
            transaction.setBalance(transaction.getBalance() + balanceDif);
            transactionRepository.save(transaction);
        }
        logger.debug("Transactions updated");
    }

    /**
     * Метод удаляет все записи из списка. В том числе уменьшает балансы на итоговую величину записи
     * @param transaction список записей для удаления
     */
    @DeleteMapping("/{operationId}")
    public void delete(@PathVariable int operationId) {
        logger.debug("Deleting transactions");
        List<Transaction> transactions = transactionRepository.findAllByOperationId(operationId);
        for (Transaction t : transactions) {
            // находим старый баланс
            Account account = accountRepository.findById(t.getAccount().getId()).orElseThrow();
            double oldBalance = account.getBalance();
            logger.trace("Old balance: " + oldBalance);

            // рассчитываем новый баланс
            double newBalance = undoTransaction(t.getId());
            logger.trace("New balance: " + newBalance);

            // обновляем балансы последующих записей
            List<Integer> accountIds = new ArrayList<>();
            accountIds.add(account.getId());
            updateNext(accountIds, newBalance - oldBalance, t.getDate());
        }
        // удаляем все записи из списка
        transactionRepository.deleteAll(transactions);
        logger.debug("Transactions deleted");
    }

    @DeleteMapping("/transaction/{id}")
    public void deleteTransaction(@PathVariable int id) {
        logger.debug("Deleting transactions");
        Transaction t = transactionRepository.findById(id).orElseThrow();
        // находим старый баланс
        Account account = accountRepository.findById(t.getAccount().getId()).orElseThrow();
        double oldBalance = account.getBalance();
        logger.trace("Old balance: " + oldBalance);

        // рассчитываем новый баланс
        double newBalance = undoTransaction(t.getId());
        logger.trace("New balance: " + newBalance);

        // обновляем балансы последующих записей
        List<Integer> accountIds = new ArrayList<>();
        accountIds.add(account.getId());
        updateNext(accountIds, newBalance - oldBalance, t.getDate());
        transactionRepository.deleteById(id);
        logger.debug("Transactions deleted");
    }



    /**
     * Метод удаляет все записи из списка, не меняя значения баланса клиента
     * @param transaction список записей для удаления
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @DeleteMapping("/safe")
    public void safeDeleteTransaction(@RequestBody List<Transaction> transaction) {
        logger.debug("Deleting transactions");
        transactionRepository.deleteAll(transaction);
        logger.debug("Transactions deleted");
    }

    /**
     * Метод рассчитывает суммарные балансы по всем клиентам и возвращает в виде нового аккаунта
     * @param accounts список аккаунтов клиентов с балансами валют
     * @return "аккаунт клиента "total"
     */
    /*@GetMapping("/total")
    public Client addTotal(@RequestBody List<Client> clients) {
        logger.debug("Adding account total");
        List<Account> totalList = new ArrayList<>();
        Client client = new Client("Всего");
        for (Currency currency : currencyController.getAllCurrencies()) {
            Account total = new Account();
            total.setClient(client);
            total.setCurrency(currency);
            // суммируем балансы аккаунтов в соответствующей валюте
            double sum = 0;
            for (Client c : clients)  {
                for (Account ac : accountRepository.findByClientId(c.getId()))
                if (ac.getCurrency().equals(currency)) {
                    sum += ac.getBalance();
                }
            }
            // добавляем в качестве баланса нового аккаунта
            total.setBalance(sum);
            totalList.add(total);

        }
        client.setAccounts((Set<Account>) totalList);
        logger.trace("Total: " + totalList);
        return client;
    }*/

    /**
     * Метод собирает все записи в базе данных в список
     * @return список всех записей
     */
    @GetMapping
    public List<Transaction> getAllTransactions() {
        logger.debug("Getting all transactions");
        return transactionRepository.findAllByOrderByDateAsc();
    }

    /**
     * Метод ищет максимальный ИД записи в базе данных
     * @return максимальный ИД записи, или 0 (если записей нет)
     */
    @GetMapping("/maxId")
    public int getMaxId(){
        if (transactionRepository.getMaxId() == null) {
            return 0;
        }
        return transactionRepository.getMaxId();
    }

    /**
     * Метод ищет записи в базе данных по ИД
     * @param operationId ИД записей
     * @return список записей
     */
    @GetMapping("/{operationId}")
    public List<Transaction> getTransaction(@PathVariable int operationId) {
        logger.debug("Getting transactions by id");
        return transactionRepository.findAllByOperationIdOrderByDate(operationId);
    }

    @GetMapping("/forDate")
    public List<Transaction> findByAccountForDate(@RequestBody Account account, Timestamp startDate, Timestamp endDate) {
        logger.debug("getting transactions by clientId for date");
        return transactionRepository
                .findAllByAccountIdAndDateBetweenOrderByDateAscIdAsc(
                        account.getId(),
                        new Timestamp(startDate.getTime() - 1),
                        endDate
                );
    }

    /**
     * Метод ищет запись по трем идентификаторам, обеспечивающим уникальность: ИД записи, ИД клиента и ИД валюты
     * @param operationId ИД записи

     * @return искомую запись, или null если она не существует
     */
    public Transaction getTransactionByAccountAndByOperationId(Account account, int operationId) {
        logger.debug("Getting transaction by id, clientId and currencyId");
        return transactionRepository
                .findByAccountIdAndOperationIdOrderByDate(account.getId(), operationId);
    }

    /**
     * Метод сохраняет запись
     * @param transaction запись для сохранения
     */
    @PostMapping
    public List<Transaction> save(@RequestBody List<Transaction> transactions) {
        logger.debug("Saving transaction");
        List<Transaction> result = transactionRepository.saveAll(transactions);
        logger.debug("Transaction saved");
        return result;
    }

    /**
     * Метод ищет записи в БД по заданным клиенту и валюте
     *
     * @param account
     * @return список записей удовлетворяющих условию
     */
    @GetMapping("/byAccount")
    public List<Transaction> findByAccount(@RequestBody Account account) {
        logger.debug("Getting transaction by client and currency");
        return transactionRepository
                .findAllByAccountIdOrderByAccountCurrencyIdAscDateAscIdAsc(account.getId());
    }

    /**
     * Метод ищет в БД все ранее введенные уникальные комментарии
     * @return список комментариев
     */
    @GetMapping("/comments")
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
    @DeleteMapping("/after/{afterDate}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteAfter(@PathVariable Timestamp afterDate) {
        logger.debug("deleting after " + afterDate);
        for (Account account : accountRepository.findAll()) {
            Transaction tr = transactionRepository
                    .findAllByAccountIdAndDateBetweenLimit1(
                            account.getId(), afterDate);
            if (tr != null) {
                logger.debug("find previous transaction");
                account.setBalance(tr.getBalance());
            } else {
                logger.debug("previous transaction are not founded");
                account.setBalance(0.0);
            }
            accountRepository.save(account);
        }
        transactionRepository.deleteByDateBetween(afterDate, new Timestamp(System.currentTimeMillis()));
    }

    /**
     * Метод осуществляет поиск предыдущей записи для выбранных клиента и валюты, а также исключением из выборки текущей
     * записи по ИД и даты не позднее которой должна быть найденная запись

     * @param afterDate наиболее поздняя дата записи
     * @param trId ИД записи, игнорируемой при поиске
     * @return предыдущая запись
     */
    @GetMapping("/before/{afterDate}")
    public Transaction findPrevious(Account account, Timestamp afterDate) {
        logger.debug("finding previous");
        return transactionRepository
                .findAllByAccountIdAndDateBetweenLimit1(
                        account.getId(), afterDate);
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
    @GetMapping("/byUser")
    public List<Transaction> findByUser(@RequestBody Integer managerId, Timestamp startDate, Timestamp endDate, String clientName, List<Integer> currencyId) {
        logger.debug("finding by user");
        Client client;
        try {
            client = clientController.getClientExactly(clientName);
        } catch (RuntimeException e) {
            client = null;
        }
        List<Integer> accountIds;
        if (client == null) {
            accountIds = accountRepository.findAllByCurrencyIdIn(currencyId).stream().mapToInt(Account::getId).boxed().toList();
        } else {
            accountIds = accountRepository.findAllByClientIdAndCurrencyIdIn(client.getId(), currencyId).stream().mapToInt(Account::getId).boxed().toList();
        }
        return transactionRepository.findAllByUserIdAndAccountIdInAndDateBetweenOrderByDateAsc(managerId, accountIds, startDate, endDate);
    }

    /**
     * Метод осуществляет поиск в БД последней по времени записи в указанный день, и возвращает эту дату со временем
     * на секунду позднее
     * @param date дата для которой выполняется поиск
     * @return дату и время
     */
    @GetMapping("/date/{date}")
    public Timestamp getAvailableDate(@PathVariable Timestamp date) {
        Timestamp nextDate = Timestamp.valueOf(LocalDate.ofInstant(new Date(date.getTime() + 86400000).toInstant(), ZoneId.systemDefault()).atStartOfDay());
        Timestamp currentDate = Timestamp.valueOf(LocalDate.ofInstant(new Date(date.getTime()).toInstant(), ZoneId.systemDefault()).atStartOfDay());
        Timestamp max = transactionRepository.getMaxDateForDay(currentDate, nextDate);
        if (max == null) {
            return currentDate;
        }
        return new Timestamp(max.getTime() + 1000);
    }

    @GetMapping("/client/{id}/startDate/{startDate}/endDate/{endDate}")
    public List<Transaction> getByClient(@PathVariable int id, @PathVariable Date startDate, @PathVariable Date endDate) {
        System.out.println(startDate);
        System.out.println(endDate);
        List<Transaction> transactions = new ArrayList<>();
        try {
            transactions = transactionRepository.findAllByAccountClientIdAndDateBetweenOrderByDateAsc(id, new Timestamp(startDate.getTime()), new Timestamp(endDate.getTime()));
            System.out.println(transactions);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transactions;
    }

}
