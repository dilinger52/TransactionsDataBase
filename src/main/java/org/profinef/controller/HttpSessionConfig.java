package org.profinef.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class HttpSessionConfig{

    private static final Map<String, HttpSession> sessions = new HashMap<>();

    public List<HttpSession> getActiveSessions() {
        return new ArrayList<>(sessions.values());
    }

    @Bean
    public HttpSessionListener httpSessionListener() {
        return new HttpSessionListener() {
            @Override
            public void sessionCreated(HttpSessionEvent hse) {
                //hse.getSession().setAttribute("sessions", getActiveSessions());
                sessions.put(hse.getSession().getId(), hse.getSession());

            }

            @Override
            public void sessionDestroyed(HttpSessionEvent hse) {
                sessions.remove(hse.getSession().getId());
            }
        };
    }
}
