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

    public List<Account> getAccounts(String name) {
        logger.debug("Getting account");
        if (name == null) return null;
        List<Client> clients = clientManager.getClient(name);
        if (clients == null) throw new RuntimeException("Клиент не найден");
        List<Account> accounts = new ArrayList<>();
        for (Client client : clients) {
            List<AccountDto> currencyDtoList = accountRepository.findByClientId(client.getId());
            accounts.add(formatFromDbo(currencyDtoList, client.getId()));
        }
        return accounts;
    }


    private Account formatFromDbo(List<AccountDto> accountDtoList, int id) {
        logger.debug("Formatting account from dto to entity");
        Account account = new Account();
        account.setClient(clientManager.getClient(id));
        Map<Currency, Account.Properties> currencies = new TreeMap<>((o1, o2) -> {
            if (Objects.equals(o1.getName(), o2.getName())) return 0;
            if (Objects.equals(o1.getName(), "UAH") || Objects.equals(o2.getName(), "PLN")) return -1;
            if (Objects.equals(o2.getName(), "UAH") || Objects.equals(o1.getName(), "PLN")) return 1;
            if (Objects.equals(o1.getName(), "USD") || Objects.equals(o2.getName(), "RUB")) return -1;
            if (Objects.equals(o2.getName(), "USD") || Objects.equals(o1.getName(), "RUB")) return 1;
            return 0;
        });
        accountDtoList = accountDtoList.stream().filter(c -> c.getClientId() == id).collect(Collectors.toList());
        for (AccountDto accountDto : accountDtoList) {
            Currency currency = currencyManager.getCurrency(accountDto.getCurrencyId());
            currencies.put(currency, new Account.Properties(accountDto.getAmount(), accountDto.getAmountColor()));
        }
        List<Currency> currencyList = currencyManager.getAllCurrencies();
        for (Currency currency : currencyList) {
            if (currencies.keySet().stream().noneMatch(c -> Objects.equals(c.getId(), currency.getId()))) {
                currencies.put(new Currency(currency.getId(), currency.getName()), new Account.Properties(0.0, null));
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

    public List<Account> getAccountsByPhone(String phone) {
        logger.debug("Getting account by phone");
        if (phone == null) return null;
        List<Client> clients = clientManager.getClientByPhone(phone);
        if (clients == null) throw new RuntimeException("Клиент не найден");
        List<Account> accounts = new ArrayList<>();
        for (Client client : clients) {
            List<AccountDto> currencyDtoList = accountRepository.findByClientId(client.getId());
            accounts.add(formatFromDbo(currencyDtoList, client.getId()));

        }

        return accounts;
    }

    public List<Account> getAccountsByTelegram(String telegram) {
        logger.debug("Getting account by telegram");
        if (telegram == null) return null;
        List<Client> clients = clientManager.getClientByTelegram(telegram);
        if (clients == null) throw new RuntimeException("Клиент не найден");
        List<Account> accounts = new ArrayList<>();
        for (Client client : clients) {
            List<AccountDto> currencyDtoList = accountRepository.findByClientId(client.getId());
            accounts.add(formatFromDbo(currencyDtoList, client.getId()));
        }
        return accounts;
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

    public void save(Account account) {
        accountRepository.saveAll(formatToDto(account));
    }

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

