package org.profinef.service;

import org.profinef.dto.ClientDto;
import org.profinef.entity.Client;
import org.profinef.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ClientManager {
    @Autowired
    private final ClientRepository clientRepository;
    private static Logger logger = LoggerFactory.getLogger(ClientManager.class);

    public ClientManager(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

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
    public Client getClient(Integer id) {
        logger.debug("Getting client by id");
        if (id == null) return null;
        Optional<ClientDto> clientOpt = clientRepository.findByIdOrderByPib(id);
        if (clientOpt.isEmpty()) throw new RuntimeException("Клиент не найден");
        ClientDto clientDto = clientOpt.get();
        return formatFromDbo(clientDto);
    }
    public List<Client> getClient(String name) {
        logger.debug("Getting client by names");
        if (name == null) return null;
        List<ClientDto> clientDtoList = clientRepository.findAllByPibContainsIgnoreCaseOrderByPib(name);
        if (clientDtoList == null) throw new RuntimeException("Клиент не найден");
        List<Client> clients = new ArrayList<>();
        for (ClientDto clientDto : clientDtoList) {
            clients.add(formatFromDbo(clientDto));
        }
        return clients;
    }

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
    @Transactional
    public void deleteClient(Client client) {
        logger.debug("Deleting client");
        clientRepository.deleteById(client.getId());
    }

    public List<Client> getClientByPhone(String phone) {
        logger.debug("Getting client by phone");
        if (phone == null) return null;
        List<ClientDto> clientDtoList = clientRepository.findAllByPhoneContainsOrderByPib(phone);
        if (clientDtoList == null) throw new RuntimeException("Клиент не найден");
        List<Client> clients = new ArrayList<>();
        for (ClientDto clientDto : clientDtoList) {
            clients.add(formatFromDbo(clientDto));
        }
        return clients;
    }

    public List<Client> getClientByTelegram(String telegram) {
        logger.debug("Getting client by telegram");
        if (telegram == null) return null;
        List<ClientDto> clientDtoList = clientRepository.findAllByTelegramContainsOrderByPib(telegram);
        if (clientDtoList == null) throw new RuntimeException("Клиент не найден");
        List<Client> clients = new ArrayList<>();
        for (ClientDto clientDto : clientDtoList) {
            clients.add(formatFromDbo(clientDto));
        }
        return clients;
    }

    public void deleteAll() {
        logger.debug("Deleting all clients");
        clientRepository.deleteAll();
    }

    public List<Client> findAll() {
        logger.debug("Finding all clients");
        List<Client> clients = new ArrayList<>();
        Iterable<ClientDto> clientIterable = clientRepository.findAll();
        for (ClientDto clientDto : clientIterable) {
            clients.add(formatFromDbo(clientDto));
        }
        return clients;
    }

    public void updateClient(Client client) {
        logger.debug("Updating client");
        Optional<ClientDto> clientDtoOpt = clientRepository.findById(client.getId());
        if (clientDtoOpt.isEmpty()) throw new RuntimeException("Клиент не найден");
        ClientDto clientDto = clientDtoOpt.get();
        clientDto.setPib(client.getPib());
        clientDto.setPhone(client.getPhone());
        clientDto.setTelegram(client.getTelegram());
        clientRepository.save(clientDto);
        logger.debug("Client updated");
    }

    public void save(Client client) {
        clientRepository.save(formatToDbo(client));
    }

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
