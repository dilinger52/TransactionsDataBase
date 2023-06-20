package org.profinef.service;

import org.profinef.dbo.CurrencyDbo;
import org.profinef.entity.Currency;
import org.profinef.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CurrencyManager {
    @Autowired
    private final CurrencyRepository currencyRepository;

    public CurrencyManager(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    public List<Currency> getCurrencies() {
        List<CurrencyDbo> currencyDboList = (List<CurrencyDbo>) currencyRepository.findAll();
        List<Currency> currencies = new ArrayList<>();
        for (CurrencyDbo currencyDbo : currencyDboList) {
            Currency currency = formatFromDbo(currencyDbo);
            currencies.add(currency);
        }
        return currencies;
    }

    private static Currency formatFromDbo(CurrencyDbo currencyDbo) {
        Currency currency = new Currency();
        currency.setId(currencyDbo.getId());
        currency.setName(currencyDbo.getName());
        currency.setExchangeRate(currencyDbo.getExchangeRate());
        return currency;
    }

    public Currency getCurrency(int id) {
        CurrencyDbo currencyDbo = currencyRepository.findById(id).get();
        return formatFromDbo(currencyDbo);
    }

    public void addCurrency(Currency currency) {
        CurrencyDbo currencyDbo = new CurrencyDbo();
        currencyDbo.setId(currency.getId());
        currencyDbo.setName(currency.getName());
        currencyDbo.setExchangeRate(currency.getExchangeRate());
        currencyRepository.save(currencyDbo);
    }

    public void deleteCurrency(Currency currency) {
        currencyRepository.deleteById(currency.getId());
    }
}
