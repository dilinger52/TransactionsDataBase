package org.profinef.entity;

import java.io.Serializable;
import java.util.Map;

public class Account implements Serializable {
    Client client;
    Map<Currency, Properties> currencies;


    public Account() {
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Map<Currency, Properties> getCurrencies() {
        return currencies;
    }

    public void setCurrencies(Map<Currency, Properties> currencies) {
        this.currencies = currencies;
    }

    @Override
    public String toString() {
        return "Account{" +
                "client=" + client +
                ", currencies=" + currencies +
                '}';
    }

    public static class Properties {
        Double amount;
        String color;

        public Properties(double amount, String amountColor) {
            this.amount = amount;
            this.color = amountColor;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        @Override
        public String toString() {
            return "Properties{" +
                    "amount=" + amount +
                    ", color='" + color + '\'' +
                    '}';
        }
    }
}


