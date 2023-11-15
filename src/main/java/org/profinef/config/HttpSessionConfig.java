package org.profinef.config;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс хранит сслыки на все созданные сесии. Необходим для сравнеия информации хранящейся в разных сессиях, такой как
 * авторизованнный пользователь или открытая страница клиента.
 */
@Configuration
public class HttpSessionConfig{
    /**
     * Хранилище сессий. Доступ к ссылке осуществляется по ИД (только внутри класса)
     */
    private static final Map<String, HttpSession> sessions = new HashMap<>();

    /**
     * Метод для получения полного списка открытых сессий извне данного класса
     * @return список сессий
     */
    public List<HttpSession> getActiveSessions() {
        return new ArrayList<>(sessions.values());
    }

    /**
     * Создает слушатель событий для сессий
     * @return слушатель событий для сессий
     */
    @Bean
    public HttpSessionListener httpSessionListener() {
        return new HttpSessionListener() {
            /**
             * Отслеживает создание сессий и добавляет ссылку на созданную сессию в хранилище
             * @param hse событие сессии
             */
            @Override
            public void sessionCreated(HttpSessionEvent hse) {
                sessions.put(hse.getSession().getId(), hse.getSession());
            }

            /**
             * Отслеживает уничтожение сессии и удаляет ссылку на нее из хранилища
             * @param hse событие сессии
             */
            @Override
            public void sessionDestroyed(HttpSessionEvent hse) {
                sessions.remove(hse.getSession().getId());
            }
        };
    }
}
