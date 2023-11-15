package org.profinef.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Класс генерирующий ответ для проверки связи клиента с сервером
 */
@RestController
public class Rest {

    @GetMapping(path = "/check_connection")
    public int checkConnection(@RequestParam(name = "id") int id) {
        return id;
    }
}
