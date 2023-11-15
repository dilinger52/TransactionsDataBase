package org.profinef.service;

import org.profinef.dto.AccountDto;
import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс отвечает за обработку данных аккаунтов полученных из базы данных перед передачей их в контроллер
 */
@Service
public class AccountManager {
    @Autowired
    private final AccountRepository accountRepository;
    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final CurrencyManager currencyManager;
    private static final Logger logger = LoggerFactory.getLogger(AccountManager.class);

    public AccountManager(AccountRepository accountRepository, ClientManager clientManager,
                          CurrencyManager currencyManager) {
        this.accountRepository = accountRepository;
        this.clientManager = clientManager;
        this.currencyManager = currencyManager;
    }

    /**
     * Метод осуществляет поиск в базе аккаунтов, имя клиента которых содержит паттерн "name"
     * @param name паттерн имени клиента для поиска
     * @return список аккаунтов клиентов
     */
    public List<Account> getAccounts(String name) {
        logger.debug("Getting account");
        // проверка на наличие паттерна
        if (name == null) return null;
        List<Client> clients = clientManager.getClient(name);
        // проверка на совпадение паттерна с каким либо клиентом в базе
        if (clients == null) throw new RuntimeException("Клиент не найден");
        // формирование списка аккаунтов для всех найденных клиентов
        List<Account> accounts = new ArrayList<>();
        for (Client client : clients) {
            List<AccountDto> currencyDtoList = accountRepository.findByClientId(client.getId());
            accounts.add(formatFromDbo(currencyDtoList, client.getId()));
        }
        return accounts;
    }

    /**
     * Метод осуществляет поиск в базе аккаунтов, имя клиента которых "name"
     * @param name имя клиента
     * @return аккаунт соответствующего клиента
     */
    public Account getAccount(String name) {
        logger.debug("Getting account");
        // проверка наличия имени для поиска
        if (name == null) return null;
        Client client = clientManager.getClientExactly(name);
        // проверка наличия совпадения с клиентом
        if (client == null) throw new RuntimeException("Клиент не найден");
        // формируем аккаунт клиента
        List<AccountDto> currencyDtoList = accountRepository.findByClientId(client.getId());
        return formatFromDbo(currencyDtoList, client.getId());
    }

    /**
     * Вспомогательный метод, преобразующий объект аккаунта поля которого соответствуют столбцам в базе данных (содержит
     * ИД связанных сущностей, вместо ссылок на них) в объект аккаунта удобного для использования в приложении (содержит
     * ссылки на связанные сущности, вместо их ИД). Объединяет несколько аккаунтов клиента по каждой из валют в единый
     * аккаунт со всей информацией
     * @param accountDtoList список аккаунтов клиента по каждой из валют
     * @param id ИД клиента, аккаунт которого необходимо преобразовать
     * @return объект аккаунта
     */
    private Account formatFromDbo(List<AccountDto> accountDtoList, int id) {
        logger.debug("Formatting account from dto to entity");
        // создаем объект аккаунта для клиента
        Account account = new Account();
        account.setClient(clientManager.getClient(id));
        // создаем список балансов валют и задаем порядок сортировки ("UAH", "USD", "EUR", "RUB", "PLN")
        Map<Currency, Account.Properties> currencies = new TreeMap<>((o1, o2) -> {
            if (Objects.equals(o1.getName(), o2.getName())) return 0;
            if (Objects.equals(o1.getName(), "UAH") || Objects.equals(o2.getName(), "PLN")) return -1;
            if (Objects.equals(o2.getName(), "UAH") || Objects.equals(o1.getName(), "PLN")) return 1;
            if (Objects.equals(o1.getName(), "USD") || Objects.equals(o2.getName(), "RUB")) return -1;
            if (Objects.equals(o2.getName(), "USD") || Objects.equals(o1.getName(), "RUB")) return 1;
            return 0;
        });
        // из списка аккаунтов выбираем относящиеся к данному клиенту
        accountDtoList = accountDtoList.stream().filter(c -> c.getClientId() == id).collect(Collectors.toList());
        // информацию по балансу каждой из валют добавляем в общий список
        for (AccountDto accountDto : accountDtoList) {
            Currency currency = currencyManager.getCurrency(accountDto.getCurrencyId());
            currencies.put(currency, new Account.Properties(accountDto.getAmount(), accountDto.getAmountColor()));
        }
        // при отсутствии данных по какой-либо из валют добавляем баланс со значением 0
        List<Currency> currencyList = currencyManager.getAllCurrencies();
        for (Currency currency : currencyList) {
            if (currencies.keySet().stream().noneMatch(c -> Objects.equals(c.getId(), currency.getId()))) {
                currencies.put(new Currency(currency.getId(), currency.getName()), new Account.Properties(0.0, null));
            }
        }
        // передаем результат
        account.setCurrencies(currencies);
        logger.trace("Account: " + account);
        return account;
    }

    /**
     * Метод выполняет сбор информации обо всех аккаунтов и формирует из них список
     * @return список всех аккаунтов
     */
    public List<Account> getAllAccounts() {
        logger.debug("Getting all accounts");
        // запрос к БД
        List<AccountDto> currencyDtoList = (List<AccountDto>) accountRepository.findAll();
        // формируем список аккаунтов
        List<Account> clientsCurrencies = new ArrayList<>();
        for (AccountDto currencyDto : currencyDtoList) {
            if (clientsCurrencies.stream().anyMatch(c -> Objects.equals(c.getClient().getId(),
                    currencyDto.getClientId()))) {
                continue;
            }
            clientsCurrencies.add(formatFromDbo(currencyDtoList, currencyDto.getClientId()));
        }
        // сортируем по имени клиента и передаем список
        clientsCurrencies =  clientsCurrencies.stream()
                .sorted(Comparator.comparing(o -> o.getClient().getPib().toLowerCase()))
                .collect(Collectors.toList());
        logger.trace("Accounts: " + clientsCurrencies);
        return clientsCurrencies;
    }

    /**
     * Метод осуществляет поиск аккаунтов по номеру телефона клиента
     * @param phone номер телефона для поиска
     * @return список аккаунтов
     */
    public List<Account> getAccountsByPhone(String phone) {
        logger.debug("Getting account by phone");
        // проверка на наличие номера для поиска
        if (phone == null) return null;
        List<Client> clients = clientManager.getClientByPhone(phone);
        // проверка на соответствие номера какому-либо клиенту
        if (clients == null) throw new RuntimeException("Клиент не найден");
        // формируем и возвращаем список
        List<Account> accounts = new ArrayList<>();
        for (Client client : clients) {
            List<AccountDto> currencyDtoList = accountRepository.findByClientId(client.getId());
            accounts.add(formatFromDbo(currencyDtoList, client.getId()));

        }

        return accounts;
    }

    /**
     * Метод осуществляет поиск аккаунтов пользователей по Telegram тегу
     * @param telegram тег Telegram
     * @return список аккаунтов
     */
    public List<Account> getAccountsByTelegram(String telegram) {
        logger.debug("Getting account by telegram");
        // проверка на наличие тега
        if (telegram == null) return null;
        List<Client> clients = clientManager.getClientByTelegram(telegram);
        // проверка на соответствие тегу одного из клиентов
        if (clients == null) throw new RuntimeException("Клиент не найден");
        // формируем и возвращаем список
        List<Account> accounts = new ArrayList<>();
        for (Client client : clients) {
            List<AccountDto> currencyDtoList = accountRepository.findByClientId(client.getId());
            accounts.add(formatFromDbo(currencyDtoList, client.getId()));
        }
        return accounts;
    }

    /**
     * Метод сохраняет в БД нового клиента с балансом гривны равной 0
     * @param newClient новый клиент
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void addClient(Client newClient) {
        logger.debug("Adding client");
        // проверка на наличие в базе клиента с тем же именем
        try {
            clientManager.getClientExactly(newClient.getPib());
        } catch (Exception e) {
            // добавляем клиента в базу
        int clientId = clientManager.addClient(newClient);
        // создаем аккаунт по гривне
        AccountDto accountDto = new AccountDto();
        accountDto.setClientId(clientId);
        accountDto.setCurrencyId(980);
        accountDto.setAmount(0);
        // добавляем аккаунт в базу
        accountRepository.save(accountDto);
        logger.debug("Client saved");
        return;
        }
        throw new RuntimeException("Клиент с таким именем уже существует");
    }

    /**
     * Метод сохраняет в БД аккаунт клиента
     * @param account который необходимо сохранить
     */
    public void save(Account account) {
        accountRepository.saveAll(formatToDto(account));
    }

    /**
     * Вспомогательный метод, выполняющий преобразование объекта аккаунта со ссылками на связанные объекты в список
     * аккаунтов с ИД связанных объектов
     * @param account аккаунт для преобразования
     * @return список аккаунтов
     */
    private List<AccountDto> formatToDto(Account account) {
        List<AccountDto> result = new ArrayList<>();
        Map<Currency, Account.Properties> currencies = account.getCurrencies();
        for (Currency currency : currencies.keySet()) {
            AccountDto accountDto = new AccountDto();
            accountDto.setClientId(account.getClient().getId());
            accountDto.setCurrencyId(currency.getId());
            accountDto.setAmount(currencies.get(currency).getAmount());
            accountDto.setAmountColor(currencies.get(currency).getColor());
            result.add(accountDto);
        }
        return result;
    }
}

