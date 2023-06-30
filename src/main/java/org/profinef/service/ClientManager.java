package org.profinef.service;

import org.profinef.dto.ClientDto;
import org.profinef.entity.Client;
import org.profinef.repository.ClientRepository;
import org.profinef.repository.TransactionRepository;
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

    public ClientManager(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
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
        Optional<ClientDto> clientOpt = clientRepository.findById(id);
        if (clientOpt.isEmpty()) throw new RuntimeException("Клиент не найден");
        ClientDto clientDto = clientOpt.get();
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
    @Transactional
    public void deleteClient(Client client) {
        clientRepository.deleteById(client.getId());
    }

    public Client getClientByPhone(String phone) {
        if (phone == null) return null;
        ClientDto clientDto = clientRepository.findByPhone(phone);
        if (clientDto == null) throw new RuntimeException("Клиент не найден");
        return formatFromDbo(clientDto);
    }

    public Client getClientByTelegram(String telegram) {
        if (telegram == null) return null;
        ClientDto clientDto = clientRepository.findByTelegram(telegram);
        if (clientDto == null) throw new RuntimeException("Клиент не найден");
        return formatFromDbo(clientDto);
    }
}
