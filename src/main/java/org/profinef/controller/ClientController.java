package org.profinef.controller;

import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.payload.response.ClientAccountResponse;
import org.profinef.repository.AccountRepository;
import org.profinef.repository.ClientRepository;
import org.profinef.repository.CurrencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс отвечает за обработку данных клиентов полученных из базы данных перед передачей их в контроллер
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/clients")
public class ClientController {
    @Autowired
    private final ClientRepository clientRepository;
    @Autowired
    private final AccountRepository accountRepository;
    @Autowired
    private final CurrencyRepository currencyRepository;
    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    public ClientController(ClientRepository clientRepository, AccountRepository accountRepository, CurrencyRepository currencyRepository) {
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.currencyRepository = currencyRepository;
    }

    /**
     * Метод осуществляет поиск клиента в базе по его ИД
     * @param id ИД клиента
     * @return объект клиента
     */
    @GetMapping("/{id}")
    public Client getClient(@PathVariable Integer id) {
        logger.debug("Getting client by id");
        // проверка наличия ИД
        if (id == null) return null;
        Optional<Client> clientOpt = clientRepository.findByIdOrderByPib(id);
        // проверка соответствия ИД клиенту в БД
        if (clientOpt.isEmpty()) throw new RuntimeException("Клиент не найден");
        // передача результата
        return clientOpt.get();
    }

    /**
     * Метод осуществляет поиск клиентов имя которых содержит паттерн "name"
     * @param name паттерн для поиска
     * @return список клиентов, имя которых удовлетворяет паттерну
     */
    @GetMapping("/name/{name}")
    public List<Client> getClient(@PathVariable String name) {
        logger.debug("Getting client by names");
        // проверка наличия паттерна
        if (name == null) return null;
        List<Client> clientList = clientRepository.findAllByPibContainsIgnoreCaseOrderByPib(name);
        // проверка наличия совпадений в базе
        if (clientList == null) throw new RuntimeException("Клиент не найден");
        // формирование и передача результирующего списка
        return clientList;
    }

    /**
     * Метод предназначен для поиска клиента в БД, имя которого полностью совпадает с введенным
     * @param name имя клиента, которого нужно найти
     * @return клиента с заданным именем
     */
    @GetMapping("/name/{name}/exactly")
    public Client getClientExactly(String name) {
        logger.debug("Getting client by names");
        // проверка наличия имени для поиска
        if (name == null) return null;
        Client client = clientRepository.findByPibIgnoreCaseOrderByPib(name);
        // проверка наличия соответствующего клиента в БД
        if (client == null) throw new RuntimeException("Клиент не найден");
        return client;
    }

    /**
     * Метод предназначен для добавления в базу данных нового клиента
     * @param client клиент, которого необходимо добавить в базу
     * @return ИД нового клиента
     */
    //@PostMapping
    public Client addClient(@RequestBody Client client) {
        logger.debug("Adding client");
        return clientRepository.save(client);

    }

    /**
     * Метод предназначен дял удаления клиента из базы данных
     * @param client которого необходимо удалить
     */
    @DeleteMapping
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteClient(@RequestBody Client client) {
        logger.debug("Deleting client");
        clientRepository.delete(client);
    }

    /**
     * Метод предназначен для поиска клиентов по номеру телефона
     * @param phone номер телефона
     * @return список клиентов с совпадающим номером
     */
    @GetMapping("/phone/{phone}")
    public List<Client> getClientByPhone(@PathVariable String phone) {
        logger.debug("Getting client by phone");
        // проверка на наличие номера
        if (phone == null) return null;
        List<Client> clientList = clientRepository.findAllByPhoneContainsOrderByPib(phone);
        // проверка на соответствие номера какому-либо клиенту
        if (clientList == null) throw new RuntimeException("Клиент не найден");
        // формируем список
        return clientList;
    }

    /**
     * Метод предназначен для поиска клиентов по тегу Telegram
     * @param telegram тег Telegram
     * @return список клиентов с совпадающим тегом
     */
    @GetMapping("/telegram/{telegram}")
    public List<Client> getClientByTelegram(String telegram) {
        logger.debug("Getting client by telegram");
        // проверка наличия тега
        if (telegram == null) return null;
        List<Client> clientList = clientRepository.findAllByTelegramContainsOrderByPib(telegram);
        // проверка совпадения тега с каким-либо клиентом
        if (clientList == null) throw new RuntimeException("Клиент не найден");
        // формирование списка
        return clientList;
    }

    /**
     * Метод удаляет всех имеющихся в БД клиентов
     */
    /*@DeleteMapping
    public void deleteAll() {
        logger.debug("Deleting all clients");
        clientRepository.deleteAll();
    }
*/
    /**
     * Метод формирует список всех клиентов, имеющихся в базе данных
     * @return список клиентов
     */
    @GetMapping
    public List<ClientAccountResponse> findAll() {
        logger.debug("Finding all clients");
        // формируется список
        List<ClientAccountResponse> clients = new ArrayList<>();
        for (Client client : clientRepository.findAllByOrderByPib()) {
            ClientAccountResponse c = new ClientAccountResponse();
            c.setId(client.getId());
            c.setPib(client.getPib());
            c.setPhone(client.getPhone());
            c.setTelegram(client.getTelegram());
            c.setColor(client.getColor());
            List<Account> accounts = accountRepository.findByClientId(client.getId());
            for (Currency currency : currencyRepository.findAll()) {
                if (accounts.stream().noneMatch(a -> a.getCurrency().equals(currency))) {
                    accounts.add(new Account(client, currency));
                }
            }
            Collections.sort(accounts);
            c.setAccounts(accounts);
            clients.add(c);
        }
        return clients;
    }

    /**
     * Метод обновляет в базе данных параметры клиента
     * @param client клиент с новыми данными
     */
    @PutMapping
    public void updateClient(@RequestBody Client client) {
        logger.debug("Updating client");
        // сохраняем нового клиента
        clientRepository.save(client);
        logger.debug("Client updated");
    }
}
