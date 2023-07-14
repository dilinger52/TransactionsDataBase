package org.profinef.service;

import org.profinef.dto.CurrencyDto;
import org.profinef.entity.Currency;
import org.profinef.repository.CurrencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CurrencyManager {
    @Autowired
    private final CurrencyRepository currencyRepository;
    private static Logger logger = LoggerFactory.getLogger(CurrencyManager.class);

    public CurrencyManager(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    public Currency getCurrency(Integer currencyId) {
        logger.debug("Getting currency by id");
        if (currencyId == null) return null;
        Optional<CurrencyDto> currencyOpt = currencyRepository.findById(currencyId);
        if (currencyOpt.isEmpty()) throw new RuntimeException("Валюта не найдена");
        CurrencyDto currencyDto =currencyOpt.get();
        return formatFromDto(currencyDto);
    }

    public Currency getCurrency(String currencyName) {
        logger.debug("Getting currency by name");
        if (currencyName == null) return null;
        CurrencyDto currencyDto = currencyRepository.findByName(currencyName);
        if (currencyDto == null) throw new RuntimeException("Валюта не найдена");
        return formatFromDto(currencyDto);
    }

    private static Currency formatFromDto(CurrencyDto currencyDto) {
        logger.debug("Formatting currency from dto to entity");
        Currency currency = new Currency();
        currency.setId(currencyDto.getId());
        currency.setName(currencyDto.getName());
        logger.trace("Currency: " + currency);
        return currency;
    }

    public List<Currency> getAllCurrencies() {
        logger.debug("Getting all currencies");
        Iterable<CurrencyDto> it = currencyRepository.findAll();
        List<Currency> currencies = new ArrayList<>();
        for (CurrencyDto currencyDto : it) {
            currencies.add(formatFromDto(currencyDto));
        }
        return currencies;
    }

    public void addCurrency(Currency newCurrency) throws Exception {
        logger.debug("Adding currency");
        if (currencyRepository.findById(newCurrency.getId()).isPresent()
        || currencyRepository.findByName(newCurrency.getName()) != null) {
            throw new Exception("Данная валюта уже присутствует");
        }
        currencyRepository.save(formatToDto(newCurrency));
        logger.debug("Currency saved");
    }

    private CurrencyDto formatToDto(Currency newCurrency) {
        logger.debug("Formatting from entity to dto");
        CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setName(newCurrency.getName());
        currencyDto.setId(newCurrency.getId());
        logger.trace("CurrencyDTO: " + currencyDto);
        return currencyDto;
    }
}
