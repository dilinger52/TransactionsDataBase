package org.profinef.controller;

import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.payload.request.NewClientRequest;
import org.profinef.repository.AccountRepository;
import org.profinef.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Класс отвечает за обработку данных аккаунтов полученных из базы данных перед передачей их в контроллер
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    @Autowired
    private final AccountRepository accountRepository;
    @Autowired
    private final ClientController clientController;
    @Autowired
    private final CurrencyController currencyController;
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    public AccountController(AccountRepository accountRepository, ClientController clientController,
                             CurrencyController currencyController, ClientRepository clientRepository) {
        this.accountRepository = accountRepository;
        this.clientController = clientController;
        this.currencyController = currencyController;
    }

    /**
     * Метод осуществляет поиск в базе аккаунтов, имя клиента которых содержит паттерн "name"
     * @param name паттерн имени клиента для поиска
     * @return список аккаунтов клиентов
     */
    @GetMapping("/name/{name}")
    public List<Account> getAccounts(@PathVariable String name) {
        logger.debug("Getting account");
        // проверка на наличие паттерна
        if (name == null) return null;
        List<Client> clients = clientController.getClient(name);
        // проверка на совпадение паттерна с каким либо клиентом в базе
        if (clients == null) throw new RuntimeException("Клиент не найден");
        // формирование списка аккаунтов для всех найденных клиентов
        return accountRepository.findAllByClientIdIn(clients.stream().mapToInt(Client::getId).boxed().toList());
    }

    /**
     * Метод осуществляет поиск в базе аккаунтов, имя клиента которых "name"
     * @param name имя клиента
     * @return аккаунт соответствующего клиента
     */
    @GetMapping("/name/{name}/exactly")
    public List<Account> getAccountsExactly(@PathVariable String name) {
        logger.debug("Getting account");
        // проверка наличия имени для поиска
        if (name == null) return null;
        Client client = clientController.getClientExactly(name);
        // проверка наличия совпадения с клиентом
        if (client == null) throw new RuntimeException("Клиент не найден");
        // формируем аккаунт клиента
        return accountRepository.findByClientId(client.getId());
    }

    /**
     * Метод выполняет сбор информации обо всех аккаунтов и формирует из них список
     * @return список всех аккаунтов
     */
    @GetMapping
    public List<Account> getAllAccounts() {
        logger.debug("Getting all accounts");
        // запрос к БД
        List<Account> clientsCurrencies = accountRepository.findAllByOrderByClientPib();
        // формируем список аккаунтов
        logger.trace("Accounts: " + clientsCurrencies);
        return clientsCurrencies;
    }

    /**
     * Метод осуществляет поиск аккаунтов по номеру телефона клиента
     * @param phone номер телефона для поиска
     * @return список аккаунтов
     */
    @GetMapping("/phone/{phone}")
    public List<Account> getAccountsByPhone(@PathVariable String phone) {
        logger.debug("Getting account by phone");
        // проверка на наличие номера для поиска
        if (phone == null) return null;
        List<Client> clients = clientController.getClientByPhone(phone);
        // проверка на соответствие номера какому-либо клиенту
        if (clients == null) throw new RuntimeException("Клиент не найден");
        // формируем и возвращаем список
        return accountRepository.findAllByClientIdIn(clients.stream().mapToInt(Client::getId).boxed().toList());
    }

    /**
     * Метод осуществляет поиск аккаунтов пользователей по Telegram тегу
     * @param telegram тег Telegram
     * @return список аккаунтов
     */
    @GetMapping("/telegram/{telegram}")
    public List<Account> getAccountsByTelegram(@PathVariable String telegram) {
        logger.debug("Getting account by telegram");
        // проверка на наличие тега
        if (telegram == null) return null;
        List<Client> clients = clientController.getClientByTelegram(telegram);
        // проверка на соответствие тегу одного из клиентов
        if (clients == null) throw new RuntimeException("Клиент не найден");
        // формируем и возвращаем список
        List<Account> accounts = new ArrayList<>();
        for (Client client : clients) {
            List<Account> currencyDtoList = accountRepository.findByClientId(client.getId());
            accounts.addAll(currencyDtoList);
        }
        return accounts;
    }

    /**
     * Метод сохраняет в БД нового клиента с балансом гривны равной 0
     * @param newClient новый клиент
     */
    @PostMapping
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void addClient(@RequestBody NewClientRequest request) {
        logger.debug("Adding client");
        // проверка на наличие в базе клиента с тем же именем
        try {
            clientController.getClientExactly(request.getPib());
        } catch (Exception e) {
            // добавляем клиента в базу
        Client client = new Client();
        client.setPib(request.getPib());
        client.setPhone(request.getPhone());
        client.setTelegram(request.getTelegram());
        client = clientController.addClient(client);
        // создаем аккаунт по гривне
        Account account = new Account();
        account.setClient(client);
        account.setCurrency(currencyController.getCurrency(980));
        account.setBalance(0.0);
        // добавляем аккаунт в базу
        accountRepository.save(account);
        logger.debug("Client saved");
        return;
        }
        throw new RuntimeException("Клиент с таким именем уже существует");
    }

    /**
     * Метод сохраняет в БД аккаунт клиента
     * @param account который необходимо сохранить
     */
    @PutMapping
    public void save(@RequestBody Account account) {
        accountRepository.save(account);
    }

    @GetMapping("/client/{clientId}/currencyIds")
    public List<Account> getAccounts(@PathVariable int clientId, @RequestBody List<Integer> currencyIds) {
        return accountRepository.findAllByClientIdAndCurrencyIdIn(clientId, currencyIds);
    }
    @GetMapping("/client/{clientId}/currency/{currencyId}")
    public Account getAccount(@PathVariable int clientId, @PathVariable int currencyId) {
        Account account = accountRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        if (account == null) {
            account = new Account();
            account.setClient(clientController.getClient(clientId));
            account.setCurrency(currencyController.getCurrency(currencyId));
        }
        return account;
    }
}

