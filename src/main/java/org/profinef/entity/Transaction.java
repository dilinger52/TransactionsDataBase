package org.profinef.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

public class Transaction {

    int id;
    Client client1;
    Currency currency1;
    Client client2;
    Currency currency2;
    double amount;

    public Transaction() {
    }

    public Transaction(int id, Client client1, Currency currency1, Client client2, Currency currency2, double amount) {
        this.id = id;
        this.client1 = client1;
        this.currency1 = currency1;
        this.client2 = client2;
        this.currency2 = currency2;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Client getClient1() {
        return client1;
    }

    public void setClient1(Client client1) {
        this.client1 = client1;
    }

    public Currency getCurrency1() {
        return currency1;
    }

    public void setCurrency1(Currency currency1) {
        this.currency1 = currency1;
    }

    public Client getClient2() {
        return client2;
    }

    public void setClient2(Client client2) {
        this.client2 = client2;
    }

    public Currency getCurrency2() {
        return currency2;
    }

    public void setCurrency2(Currency currency2) {
        this.currency2 = currency2;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", client1=" + client1 +
                ", currency1=" + currency1 +
                ", client2=" + client2 +
                ", currency2=" + currency2 +
                ", amount=" + amount +
                '}';
    }
}
