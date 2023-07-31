/*package org.profinef.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.profinef.dto.ClientDto;
import org.profinef.entity.Client;
import org.profinef.repository.ClientRepository;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.configuration.ConfigurationType.PowerMock;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ClientDto.class)
public class ClientManagerTest {
    static ClientManager clientManager;

    @BeforeAll
    public static void before() {
        ClientDto client1 = new ClientDto("Ivan");
        client1.setId(1);
        client1.setPhone("+38 (056) 329-17-12");
        client1.setTelegram("@ivan");
        ClientDto client2 = new ClientDto("Vasil");
        client2.setId(2);
        client2.setPhone("+38 (068) 111-11-11");
        client2.setTelegram("@vasil");
        ClientRepository clientRepository = mock(ClientRepository.class);
        when(clientRepository.findByIdOrderByPib(1)).thenReturn(Optional.of(client1));
        when(clientRepository.findByPibIgnoreCaseOrderByPib("Ivan")).thenReturn(client1);
        when(clientRepository.save(mock(ClientDto.class)).getId()).thenReturn(7);
        clientManager = new ClientManager(clientRepository);

    }

    @Test
    public void testGetClientById() {
        Client client = new Client("Ivan");
        client.setId(1);
        client.setPhone("+38 (056) 329-17-12");
        client.setTelegram("@ivan");
        Assertions.assertEquals(client.toString(), clientManager.getClient(client.getId()).toString());
    }

    @Test
    public void testGetClientByIdBadCases() {
        Assertions.assertNull(clientManager.getClient((Integer) null));
        Assertions.assertThrows(RuntimeException.class, () -> clientManager.getClient(0), "Клиент не найден");
    }

    @Test
    public void testGetClientByName() {
        Client client = new Client("Ivan");
        client.setId(1);
        client.setPhone("+38 (056) 329-17-12");
        client.setTelegram("@ivan");
        Assertions.assertEquals(client.toString(), clientManager.getClient(client.getPib()).toString());
    }

    @Test
    public void testGetClientByNameBadCases() {
        Assertions.assertNull(clientManager.getClient((String) null));
        Assertions.assertThrows(RuntimeException.class, () -> clientManager.getClient("abra-cadabra"), "Клиент не найден");
    }

    @Test
    public void testAddClient() {
        Client client = new Client("Ivan");
        client.setId(1);
        client.setPhone("+38 (056) 329-17-12");
        client.setTelegram("@ivan");
        Assertions.assertEquals(7, clientManager.addClient(client));
    }
}*/
