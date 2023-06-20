package org.profinef.entity;

import jakarta.persistence.*;
import org.profinef.dbo.CompositeKey;

public class ClientsCurrency {
    Client client;
    Currency currency;
    double amount;

    public ClientsCurrency() {
    }

    public ClientsCurrency(Client client, Currency currency, double amount) {
        this.client = client;
        this.currency = currency;
        this.amount = amount;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "ClientsCurrency{" +
                "client=" + client +
                ", currency=" + currency +
                ", amount=" + amount +
                '}';
    }
}

