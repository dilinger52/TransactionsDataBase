package org.profinef.service;

import org.profinef.dto.CurrencyDto;
import org.profinef.entity.Currency;
import org.profinef.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CurrencyManager {
    @Autowired
    private final CurrencyRepository currencyRepository;

    public CurrencyManager(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    public Currency getCurrency(Integer currencyId) {
        if (currencyId == null) return null;
        Optional<CurrencyDto> currencyOpt = currencyRepository.findById(currencyId);
        if (currencyOpt.isEmpty()) throw new RuntimeException("Валюта не найдена");
        CurrencyDto currencyDto =currencyOpt.get();
        return formatFromDto(currencyDto);
    }

    private static Currency formatFromDto(CurrencyDto currencyDto) {
        Currency currency = new Currency();
        currency.setId(currencyDto.getId());
        currency.setName(currencyDto.getName());
        return currency;
    }

    public List<Currency> getAllCurrencies() {
        Iterable<CurrencyDto> it = currencyRepository.findAll();
        List<Currency> currencies = new ArrayList<>();
        for (CurrencyDto currencyDto : it) {
            currencies.add(formatFromDto(currencyDto));
        }
        return currencies;
    }

    public void addCurrency(Currency newCurrency) throws Exception {
        if (currencyRepository.findById(newCurrency.getId()).isPresent()
        || currencyRepository.findByName(newCurrency.getName()) != null) {
            throw new Exception("Данная валюта уже присутствует");
        }
        currencyRepository.save(formatToDto(newCurrency));
    }

    private CurrencyDto formatToDto(Currency newCurrency) {
        CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setName(newCurrency.getName());
        currencyDto.setId(newCurrency.getId());
        return currencyDto;
    }
}
