package org.profinef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.profinef.entity.NBUCurrency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.profinef.entity.Currency;

/**
 * Класс предназначен для получения официальных курсов НБУ
 */
@Service
public class NBUManager {

    @Autowired
    private final CurrencyManager currencyManager;
    private final Logger logger =  LoggerFactory.getLogger(NBUManager.class);

    public NBUManager(CurrencyManager currencyManager) {
        this.currencyManager = currencyManager;
    }

    /**
     * Метод посылает HTML запрос к REST приложению НБУ и извлекает из полученных данных в формате JSON актуальные курсы
     * валют имеющихся в базе
     */
    public void getTodayCurrencyExchange() {
        // формируем запрос
        String url = "https://bank.gov.ua/NBU_Exchange/exchange?json";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        ObjectMapper objectMapper = new ObjectMapper();
        List<Currency> currencys = new ArrayList<>();
        try {
            // получаем ответ
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug("Getting response");
            String responseBody = response.body();
            // преобразуем строку JSON в объект
            List<NBUCurrency> nbuCurrencies = objectMapper.readValue(responseBody, new TypeReference<>() {});
            // обновляем курсы валют
            List<Currency> currencyList = currencyManager.getAllCurrencies();
            for (Currency c : currencyList) {
                for (NBUCurrency nbuCurrency : nbuCurrencies) {
                    if (Objects.equals(nbuCurrency.getCurrencyCodeL(), c.getName())) {
                        Currency currency = new Currency();
                        currency.setId(c.getId());
                        currency.setName(nbuCurrency.getCurrencyCodeL());
                        currency.setAverageExchange(nbuCurrency.getAmount());
                        currencys.add(currency);
                    }
                }
            }
            logger.debug("Converting to entities");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        currencyManager.saveAll(currencys);
        logger.debug("Exchange rates updated");
    }
}
