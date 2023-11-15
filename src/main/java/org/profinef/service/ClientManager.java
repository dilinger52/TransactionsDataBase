package org.profinef.service;

import org.profinef.dto.ClientDto;
import org.profinef.entity.Client;
import org.profinef.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Класс отвечает за обработку данных клиентов полученных из базы данных перед передачей их в контроллер
 */
@Service
public class ClientManager {
    @Autowired
    private final ClientRepository clientRepository;
    private static final Logger logger = LoggerFactory.getLogger(ClientManager.class);

    public ClientManager(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /**
     * Метод преобразует объект клиента, содержащего ИД связанных объектов, в объект содержащий ссылки на них
     * @param clientDto объект клиента для преобразования
     * @return объект после преобразования
     */
    private Client formatFromDbo(ClientDto clientDto) {
        logger.debug("Formatting client from dto to entity");
        if (clientDto == null) return null;
        Client client = new Client(clientDto.getPib());
        client.setId(clientDto.getId());
        client.setPhone(clientDto.getPhone());
        client.setTelegram(clientDto.getTelegram());
        client.setColor(clientDto.getColor());
        logger.trace("Client: " + client);
        return client;
    }

    /**
     * Метод осуществляет поиск клиента в базе по его ИД
     * @param id ИД клиента
     * @return объект клиента
     */
    public Client getClient(Integer id) {
        logger.debug("Getting client by id");
        // проверка наличия ИД
        if (id == null) return null;
        Optional<ClientDto> clientOpt = clientRepository.findByIdOrderByPib(id);
        // проверка соответствия ИД клиенту в БД
        if (clientOpt.isEmpty()) throw new RuntimeException("Клиент не найден");
        // передача результата
        ClientDto clientDto = clientOpt.get();
        return formatFromDbo(clientDto);
    }

    /**
     * Метод осуществляет поиск клиентов имя которых содержит паттерн "name"
     * @param name паттерн для поиска
     * @return список клиентов, имя которых удовлетворяет паттерну
     */
    public List<Client> getClient(String name) {
        logger.debug("Getting client by names");
        // проверка наличия паттерна
        if (name == null) return null;
        List<ClientDto> clientDtoList = clientRepository.findAllByPibContainsIgnoreCaseOrderByPib(name);
        // проверка наличия совпадений в базе
        if (clientDtoList == null) throw new RuntimeException("Клиент не найден");
        // формирование и передача результирующего списка
        List<Client> clients = new ArrayList<>();
        for (ClientDto clientDto : clientDtoList) {
            clients.add(formatFromDbo(clientDto));
        }
        return clients;
    }

    /**
     * Метод предназначен для поиска клиента в БД, имя которого полностью совпадает с введенным
     * @param name имя клиента, которого нужно найти
     * @return клиента с заданным именем
     */
    public Client getClientExactly(String name) {
        logger.debug("Getting client by names");
        // проверка наличия имени для поиска
        if (name == null) return null;
        ClientDto clientDto = clientRepository.findByPibIgnoreCaseOrderByPib(name);
        // проверка наличия соответствующего клиента в БД
        if (clientDto == null) throw new RuntimeException("Клиент не найден");
        return formatFromDbo(clientDto);
    }

    /**
     * Метод предназначен для добавления в базу данных нового клиента
     * @param client клиент, которого необходимо добавить в базу
     * @return ИД нового клиента
     */
    public int addClient(Client client) {
        logger.debug("Adding client");
        ClientDto clientDto = new ClientDto(client.getPib());
        clientDto.setId(0);
        clientDto.setPhone(client.getPhone());
        clientDto.setTelegram(client.getTelegram());
        int id = clientRepository.save(clientDto).getId();
        logger.debug("Client saved");
        return id;

    }

    /**
     * Метод предназначен дял удаления клиента из базы данных
     * @param client которого необходимо удалить
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteClient(Client client) {
        logger.debug("Deleting client");
        clientRepository.deleteById(client.getId());
    }

    /**
     * Метод предназначен для поиска клиентов по номеру телефона
     * @param phone номер телефона
     * @return список клиентов с совпадающим номером
     */
    public List<Client> getClientByPhone(String phone) {
        logger.debug("Getting client by phone");
        // проверка на наличие номера
        if (phone == null) return null;
        List<ClientDto> clientDtoList = clientRepository.findAllByPhoneContainsOrderByPib(phone);
        // проверка на соответствие номера какому-либо клиенту
        if (clientDtoList == null) throw new RuntimeException("Клиент не найден");
        // формируем список
        List<Client> clients = new ArrayList<>();
        for (ClientDto clientDto : clientDtoList) {
            clients.add(formatFromDbo(clientDto));
        }
        return clients;
    }

    /**
     * Метод предназначен для поиска клиентов по тегу Telegram
     * @param telegram тег Telegram
     * @return список клиентов с совпадающим тегом
     */
    public List<Client> getClientByTelegram(String telegram) {
        logger.debug("Getting client by telegram");
        // проверка наличия тега
        if (telegram == null) return null;
        List<ClientDto> clientDtoList = clientRepository.findAllByTelegramContainsOrderByPib(telegram);
        // проверка совпадения тега с каким-либо клиентом
        if (clientDtoList == null) throw new RuntimeException("Клиент не найден");
        // формирование списка
        List<Client> clients = new ArrayList<>();
        for (ClientDto clientDto : clientDtoList) {
            clients.add(formatFromDbo(clientDto));
        }
        return clients;
    }

    /**
     * Метод удаляет всех имеющихся в БД клиентов
     */
    public void deleteAll() {
        logger.debug("Deleting all clients");
        clientRepository.deleteAll();
    }

    /**
     * Метод формирует список всех клиентов, имеющихся в базе данных
     * @return список клиентов
     */
    public List<Client> findAll() {
        logger.debug("Finding all clients");
        // формируется список
        List<Client> clients = new ArrayList<>();
        Iterable<ClientDto> clientIterable = clientRepository.findAll();
        for (ClientDto clientDto : clientIterable) {
            clients.add(formatFromDbo(clientDto));
        }
        // сортируем список по имени клиента
        clients = clients.stream()
                .sorted(Comparator.comparing(o -> o.getPib().toLowerCase()))
                .collect(Collectors.toList());
        return clients;
    }

    /**
     * Метод обновляет в базе данных параметры клиента
     * @param client клиент с новыми данными
     */
    public void updateClient(Client client) {
        logger.debug("Updating client");
        // ищем клиента по ИД
        Optional<ClientDto> clientDtoOpt = clientRepository.findById(client.getId());
        if (clientDtoOpt.isEmpty()) throw new RuntimeException("Клиент не найден");
        ClientDto clientDto = clientDtoOpt.get();
        // меняем параметры на новые
        clientDto.setPib(client.getPib());
        clientDto.setPhone(client.getPhone());
        clientDto.setTelegram(client.getTelegram());
        // сохраняем нового клиента
        clientRepository.save(clientDto);
        logger.debug("Client updated");
    }

    /**
     * Метод сохраняет клиента в базе данных
     * @param client клиент, которого необходимо сохранить
     */
    public void save(Client client) {
        clientRepository.save(formatToDbo(client));
    }

    /**
     * Метод преобразует объект клиента, содержащий ссылки на связанные объекты, в объект клиента, содержащий ИД
     * связанных объектов
     * @param client объект для преобразования
     * @return преобразованный объект
     */
    private ClientDto formatToDbo(Client client) {
        ClientDto clientDto = new ClientDto();
        clientDto.setId(client.getId());
        clientDto.setPib(client.getPib());
        clientDto.setPhone(client.getPhone());
        clientDto.setTelegram(client.getTelegram());
        clientDto.setColor(client.getColor());
        return clientDto;
    }
}
