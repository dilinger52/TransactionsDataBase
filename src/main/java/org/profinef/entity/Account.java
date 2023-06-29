package org.profinef.entity;

import java.util.List;
import java.util.Map;

public class Account {
    Client client;
    Map<Currency, Double> currencies;

    public Account() {
    }

    public Account(Client client, Map<Currency, Double> currencies) {
        this.client = client;
        this.currencies = currencies;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Map<Currency, Double> getCurrencies() {
        return currencies;
    }

    public void setCurrencies(Map<Currency, Double> currencies) {
        this.currencies = currencies;
    }

    @Override
    public String toString() {
        return "Account{" +
                "client=" + client +
                ", currencies=" + currencies +
                '}';
    }
}


