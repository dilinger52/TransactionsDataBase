package org.profinef.entity;

import java.util.List;

public class Account {
    Client client;
    List<Currency> currencies;

    public Account() {
    }

    public Account(Client client, List<Currency> currencies) {
        this.client = client;
        this.currencies = currencies;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<Currency> getCurrencies() {
        return currencies;
    }

    public void setCurrencies(List<Currency> currencies) {
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

