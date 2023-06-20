package org.profinef.dbo;

import jakarta.persistence.*;

@Entity
@Table(name = "client_has_currency")
@IdClass(CompositeKey.class)
public class ClientsCurrencyDbo {
    @Id
    int clientId;
    @Id
    int currencyId;
    @Column
    double amount;

    public ClientsCurrencyDbo() {
    }

    public ClientsCurrencyDbo(int clientId, int currencyId, double amount) {
        this.clientId = clientId;
        this.currencyId = currencyId;
        this.amount = amount;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public int getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(int currencyId) {
        this.currencyId = currencyId;
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
                "clientId=" + clientId +
                ", currencyId=" + currencyId +
                ", amount=" + amount +
                '}';
    }
}

