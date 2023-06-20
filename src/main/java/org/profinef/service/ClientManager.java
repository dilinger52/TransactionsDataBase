package org.profinef.service;

import org.profinef.dbo.ClientDbo;
import org.profinef.entity.Client;
import org.profinef.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        List<ClientDbo> clientDboList = (List<ClientDbo>) clientRepository.findAll();
        List<Client> clients = new ArrayList<>();
        for (ClientDbo clientDbo : clientDboList) {
            clients.add(formatFromDbo(clientDbo));
        }
        return clients;
    }

    private static Client formatFromDbo(ClientDbo clientDbo) {
        Client client = new Client(clientDbo.getPib());
        client.setId(clientDbo.getId());
        return client;
    }

    public Client getClient(int id) {
        ClientDbo clientDbo = clientRepository.findById(id).get();
        return formatFromDbo(clientDbo);
    }

    public void addClient(Client client) {
        ClientDbo clientDbo = new ClientDbo(client.getPib());
        clientDbo.setId(client.getId());
        clientRepository.save(clientDbo);
    }

    public void deleteClient(Client client) {
        clientRepository.deleteById(client.getId());
    }
}
