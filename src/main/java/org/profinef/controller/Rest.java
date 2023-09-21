package org.profinef.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Rest {

    @GetMapping(path = "/check_connection", produces = MediaType.APPLICATION_JSON_VALUE)
    public int checkConnection(@RequestParam(name = "id") int id) {
        return id;
    }
}
