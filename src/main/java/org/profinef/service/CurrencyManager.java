package org.profinef.service;

import org.profinef.dto.CurrencyDto;
import org.profinef.entity.Currency;
import org.profinef.repository.CurrencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Класс отвечает за обработку данных валют полученных из базы данных перед передачей их в контроллер
 */
@Service
public class CurrencyManager {
    @Autowired
    private final CurrencyRepository currencyRepository;
    private static final Logger logger = LoggerFactory.getLogger(CurrencyManager.class);

    public CurrencyManager(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    /**
     * Метод осуществляет поиск валюты в БД по ее ИД
     * @param currencyId ИД валюты, которую необходимо найти
     * @return найденную валюту
     */
    public Currency getCurrency(Integer currencyId) {
        logger.debug("Getting currency by id");
        if (currencyId == null) return null;
        Optional<CurrencyDto> currencyOpt = currencyRepository.findById(currencyId);
        if (currencyOpt.isEmpty()) throw new RuntimeException("Валюта не найдена");
        CurrencyDto currencyDto =currencyOpt.get();
        return formatFromDto(currencyDto);
    }

    /**
     * Метод осуществляет поиск валюты в БД по ее названию
     * @param currencyName название валюты
     * @return найденную валюту
     */
    public Currency getCurrency(String currencyName) {
        logger.debug("Getting currency by names");
        if (currencyName == null) return null;
        CurrencyDto currencyDto = currencyRepository.findByName(currencyName);
        if (currencyDto == null) throw new RuntimeException("Валюта не найдена");
        return formatFromDto(currencyDto);
    }

    /**
     * Метод преобразует объект валюты с ИД связанных объектов в объект валюты со ссылками на связанные объекты
     * @param currencyDto объект валюты для преобразования
     * @return объект валюты после преобразования
     */
    private static Currency formatFromDto(CurrencyDto currencyDto) {
        logger.debug("Formatting currency from dto to entity");
        Currency currency = new Currency();
        currency.setId(currencyDto.getId());
        currency.setName(currencyDto.getName());
        currency.setAverageExchange(currencyDto.getAverageExchange());
        logger.trace("Currency: " + currency);
        return currency;
    }

    /**
     * Метод собирает все валюты имеющиеся в БД в список
     * @return список всех валют
     */
    public List<Currency> getAllCurrencies() {
        logger.debug("Getting all currencies");
        Iterable<CurrencyDto> it = currencyRepository.findAll();
        List<Currency> currencies = new ArrayList<>();
        for (CurrencyDto currencyDto : it) {
            currencies.add(formatFromDto(currencyDto));
        }
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
    public void addCurrency(Currency newCurrency) {
        logger.debug("Adding currency");
        // проверка наличия такой же валюты в БД
        if (currencyRepository.findById(newCurrency.getId()).isPresent()
        || currencyRepository.findByName(newCurrency.getName()) != null) {
            throw new RuntimeException("Данная валюта уже присутствует");
        }
        currencyRepository.save(formatToDto(newCurrency));
        logger.debug("Currency saved");
    }

    /**
     * Метод обновляет параметры валюты в БД
     * @param newCurrency валюта сновыми параметрами
     */
    public void updateCurrency(Currency newCurrency) {
        logger.debug("Updating currency");
        currencyRepository.save(formatToDto(newCurrency));
        logger.debug("Currency updated");
    }

    /**
     * Метод преобразует объект валюты со ссылками на связанные объекты в объект валюты с ИД связанных объектов
     * @param newCurrency объект валюты для преобразования
     * @return объект валюты после преобразования
     */
    private CurrencyDto formatToDto(Currency newCurrency) {
        logger.debug("Formatting from entity to dto");
        CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setName(newCurrency.getName());
        currencyDto.setId(newCurrency.getId());
        currencyDto.setAverageExchange(newCurrency.getAverageExchange());
        logger.trace("CurrencyDTO: " + currencyDto);
        return currencyDto;
    }

    /**
     * Метод сохраняет в БД несколько валют, которые содержатся в списке
     * @param newCurrencys список валют для сохранения
     */
    public void saveAll(List<Currency> newCurrencys) {
        for (Currency newCurrency : newCurrencys) {
            updateCurrency(newCurrency);
        }
    }
}
