package org.profinef.controller;

import org.profinef.entity.Currency;
import org.profinef.repository.CurrencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Класс отвечает за обработку данных валют полученных из базы данных перед передачей их в контроллер
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/currencies")
public class CurrencyController {
    @Autowired
    private final CurrencyRepository currencyRepository;
    private static final Logger logger = LoggerFactory.getLogger(CurrencyController.class);

    public CurrencyController(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    /**
     * Метод осуществляет поиск валюты в БД по ее ИД
     * @param currencyId ИД валюты, которую необходимо найти
     * @return найденную валюту
     */
    @GetMapping("/{currencyId}")
    public Currency getCurrency(@PathVariable Integer currencyId) {
        logger.debug("Getting currency by id");
        if (currencyId == null) return null;
        Optional<Currency> currencyOpt = currencyRepository.findById(currencyId);
        if (currencyOpt.isEmpty()) throw new RuntimeException("Валюта не найдена");
        return currencyOpt.get();
    }

    /**
     * Метод осуществляет поиск валюты в БД по ее названию
     * @param currencyName название валюты
     * @return найденную валюту
     */
    @GetMapping("/name/{currencyName}")
    public Currency getCurrency(@PathVariable String currencyName) {
        logger.debug("Getting currency by names");
        if (currencyName == null) return null;
        Currency currency = currencyRepository.findByName(currencyName);
        if (currency == null) throw new RuntimeException("Валюта не найдена");
        return currency;
    }

    /**
     * Метод собирает все валюты имеющиеся в БД в список
     * @return список всех валют
     */
    @GetMapping
    public List<Currency> getAllCurrencies() {
        logger.debug("Getting all currencies");
        List<Currency> currencies = (List<Currency>) currencyRepository.findAll();
        // сортируем в порядке "UAH", "USD", "EUR", "RUB", "PLN"
        currencies.sort((o1, o2) -> {
            if (Objects.equals(o1.getName(), "UAH") || Objects.equals(o2.getName(), "PLN")) return -1;
            if (Objects.equals(o2.getName(), "UAH") || Objects.equals(o1.getName(), "PLN")) return 1;
            if (Objects.equals(o1.getName(), "USD") || Objects.equals(o2.getName(), "RUB")) return -1;
            if (Objects.equals(o2.getName(), "USD") || Objects.equals(o1.getName(), "RUB")) return 1;
            return 0;
        });
        return currencies;
    }

    /**
     * Метод добавляет новую валюту в БД
     * @param newCurrency валюта для добавления
     */
    @PostMapping
    public void addCurrency(@RequestBody Currency newCurrency) {
        logger.debug("Adding currency");
        // проверка наличия такой же валюты в БД
        if (currencyRepository.findById(newCurrency.getId()).isPresent()
        || currencyRepository.findByName(newCurrency.getName()) != null) {
            throw new RuntimeException("Данная валюта уже присутствует");
        }
        currencyRepository.save(newCurrency);
        logger.debug("Currency saved");
    }

    /**
     * Метод обновляет параметры валюты в БД
     * @param newCurrency валюта сновыми параметрами
     */
    @PutMapping
    public void updateCurrency(@RequestBody Currency newCurrency) {
        logger.debug("Updating currency");
        currencyRepository.save(newCurrency);
        logger.debug("Currency updated");
    }

    /**
     * Метод сохраняет в БД несколько валют, которые содержатся в списке
     * @param newCurrencys список валют для сохранения
     */
    /*
    @PostMapping*/
    public void saveAll(@RequestBody List<Currency> newCurrencys) {
        for (Currency newCurrency : newCurrencys) {
            updateCurrency(newCurrency);
        }
    }
}
