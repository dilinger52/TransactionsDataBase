package org.profinef.service;

import org.profinef.controller.WebMvcConfig;
import org.profinef.dto.AccountDto;
import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AccountManager {
    @Autowired
    private final AccountRepository accountRepository;
    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final CurrencyManager currencyManager;
    private static Logger logger = LoggerFactory.getLogger(AccountManager.class);

    public AccountManager(AccountRepository accountRepository, ClientManager clientManager,
                          CurrencyManager currencyManager) {
        this.accountRepository = accountRepository;
        this.clientManager = clientManager;
        this.currencyManager = currencyManager;
    }

    public Account getAccount(String name) {
        logger.debug("Getting account");
        if (name == null) return null;
        Client client = clientManager.getClient(name);
        if (client == null) throw new RuntimeException("Клиент не найден");
        List<AccountDto> currencyDtoList = accountRepository.findByClientId(client.getId());
        return formatFromDbo(currencyDtoList, client.getId());
    }


    private Account formatFromDbo(List<AccountDto> accountDtoList, int id) {
        logger.debug("Formatting account from dto to entity");
        Account account = new Account();
        account.setClient(clientManager.getClient(id));
        Map<Currency, Double> currencies = new TreeMap<>();
        accountDtoList = accountDtoList.stream().filter(c -> c.getClientId() == id).collect(Collectors.toList());
        for (AccountDto accountDto : accountDtoList) {
            Currency currency = currencyManager.getCurrency(accountDto.getCurrencyId());
            currencies.put(currency, accountDto.getAmount());
        }
        List<Currency> currencyList = currencyManager.getAllCurrencies();
        for (Currency currency : currencyList) {
            if (currencies.keySet().stream().noneMatch(c -> Objects.equals(c.getId(), currency.getId()))) {
                currencies.put(new Currency(currency.getId(), currency.getName()), 0.0);
            }
        }
        account.setCurrencies(currencies);
        logger.trace("Account: " + account);
        return account;
    }

    public List<Account> getAllAccounts() {
        logger.debug("Getting all accounts");
        List<AccountDto> currencyDtoList = (List<AccountDto>) accountRepository.findAll();
        List<Account> clientsCurrencies = new ArrayList<>();
        for (AccountDto currencyDto : currencyDtoList) {
            if (clientsCurrencies.stream().anyMatch(c -> Objects.equals(c.getClient().getId(),
                    currencyDto.getClientId()))) {
                continue;
            }
            clientsCurrencies.add(formatFromDbo(currencyDtoList, currencyDto.getClientId()));
        }
        clientsCurrencies =  clientsCurrencies.stream()
                .sorted(Comparator.comparing(o -> o.getClient().getPib().toLowerCase()))
                .collect(Collectors.toList());
        logger.trace("Accounts: " + clientsCurrencies);
        return clientsCurrencies;
    }

    public Account getAccountByPhone(String phone) {
        logger.debug("Getting account by phone");
        if (phone == null) return null;
        Client client = clientManager.getClientByPhone(phone);
        if (client == null) throw new RuntimeException("Клиент не найден");
        List<AccountDto> currencyDtoList = accountRepository.findByClientId(client.getId());
        return formatFromDbo(currencyDtoList, client.getId());
    }

    public Account getAccountByTelegram(String telegram) {
        logger.debug("Getting account by telegram");
        if (telegram == null) return null;
        Client client = clientManager.getClientByTelegram(telegram);
        if (client == null) throw new RuntimeException("Клиент не найден");
        List<AccountDto> currencyDtoList = accountRepository.findByClientId(client.getId());
        return formatFromDbo(currencyDtoList, client.getId());
    }

    @Transactional
    public void addClient(Client newClient) {
        logger.debug("Adding client");
        try {
            clientManager.getClient(newClient.getPib());
        } catch (Exception e) {
        int clientId = clientManager.addClient(newClient);
        AccountDto accountDto = new AccountDto();
        accountDto.setClientId(clientId);
        accountDto.setCurrencyId(980);
        accountDto.setAmount(0);
        accountRepository.save(accountDto);
        logger.debug("Client saved");
        return;
        }
        throw new RuntimeException("Клиент с таким именем уже существует");
    }
}

