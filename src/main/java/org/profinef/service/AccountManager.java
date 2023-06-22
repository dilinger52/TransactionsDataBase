package org.profinef.service;

import org.profinef.dto.CurrencyDto;
import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountManager {
    @Autowired
    private final CurrencyRepository currencyRepository;
    @Autowired
    private final ClientManager clientManager;

    public AccountManager(CurrencyRepository currencyRepository, ClientManager clientManager) {
        this.currencyRepository = currencyRepository;
        this.clientManager = clientManager;
    }

    public Account getAccount(String name) {
        if (name == null) return null;
        Client client = clientManager.getClient(name);
        List<CurrencyDto> currencyDtoList = currencyRepository.findByClientId(client.getId());
        return formatFromDbo(currencyDtoList, client.getId());
    }

    public Currency getCurrency(Integer clientId, Integer currencyId) {
        if (clientId == null) return null;
        if (currencyId == null) return null;
        CurrencyDto currencyDto = currencyRepository.findByClientIdAndCurrencyId(clientId, currencyId);
        Currency currency = new Currency();
        currency.setId(currencyDto.getCurrencyId());
        currency.setName(currencyDto.getName());
        currency.setAmount(currencyDto.getAmount());
        return currency;
    }

    private Account formatFromDbo(List<CurrencyDto> currencyDtoList, int id) {
        Account account = new Account();
        account.setClient(clientManager.getClient(id));
        List<Currency> currencies = new ArrayList<>();
        currencyDtoList = currencyDtoList.stream().filter(c -> c.getClientId() == id).collect(Collectors.toList());
        for (CurrencyDto currencyDto : currencyDtoList) {
            Currency currency = new Currency();
            currency.setId(currencyDto.getCurrencyId());
            currency.setName(currencyDto.getName());
            currency.setAmount(currencyDto.getAmount());
            currencies.add(currency);
        }
        if (currencies.stream().noneMatch(c -> c.getId() == 980)) {
            currencies.add(new Currency(980, "UAH", 0));
        }
        if (currencies.stream().noneMatch(c -> c.getId() == 840)) {
            currencies.add(new Currency(840, "USD", 0));
        }
        currencies = currencies.stream()
                .sorted(Comparator.comparingInt(Currency::getId))
                .collect(Collectors.toList());
        account.setCurrencies(currencies);
        return account;
    }

    public List<Account> getAllAccounts() {
        List<CurrencyDto> currencyDtoList = (List<CurrencyDto>) currencyRepository.findAll();
        List<Account> clientsCurrencies = new ArrayList<>();
        for (CurrencyDto currencyDto : currencyDtoList) {
            if (clientsCurrencies.stream().anyMatch(c -> c.getClient().getId() == currencyDto.getClientId())) {
            continue;
            }
            clientsCurrencies.add(formatFromDbo(currencyDtoList, currencyDto.getClientId()));
        }
        return clientsCurrencies;
    }
}
