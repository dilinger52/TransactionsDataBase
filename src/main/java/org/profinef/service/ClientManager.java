package org.profinef.service;

import org.profinef.dto.ClientDto;
import org.profinef.entity.Client;
import org.profinef.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientManager {
    @Autowired
    private final ClientRepository clientRepository;

    public ClientManager(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<Client> getClients() {
        List<ClientDto> clientDtoList = (List<ClientDto>) clientRepository.findAll();
        List<Client> clients = new ArrayList<>();
        for (ClientDto clientDto : clientDtoList) {
            clients.add(formatFromDbo(clientDto));
        }
        return clients;
    }

    private static Client formatFromDbo(ClientDto clientDto) {
        if (clientDto == null) return null;
        Client client = new Client(clientDto.getPib());
        client.setId(clientDto.getId());
        client.setPhone(clientDto.getPhone());
        client.setTelegram(clientDto.getTelegram());
        return client;
    }

    public Client getClient(Integer id) {
        if (id == null) return null;
        ClientDto clientDto = clientRepository.findById(id).get();
        return formatFromDbo(clientDto);
    }
    public Client getClient(String name) {
        if (name == null) return null;
        ClientDto clientDto = clientRepository.findByPibIgnoreCase(name);
        return formatFromDbo(clientDto);
    }

    public int addClient(Client client) {
        ClientDto clientDto = new ClientDto(client.getPib());
        clientDto.setId(0);
        clientDto.setPhone(client.getPhone());
        clientDto.setTelegram(client.getTelegram());
        return clientRepository.save(clientDto).getId();

    }

    public void deleteClient(Client client) {
        clientRepository.deleteById(client.getId());
    }

    public Client getClientByPhone(String phone) {
        if (phone == null) return null;
        ClientDto clientDto = clientRepository.findByPhone(phone);
        return formatFromDbo(clientDto);
    }

    public Client getClientByTelegram(String telegram) {
        if (telegram == null) return null;
        ClientDto clientDto = clientRepository.findByTelegram(telegram);
        return formatFromDbo(clientDto);
    }
}
