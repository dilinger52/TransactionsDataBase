package org.profinef.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
@Entity
public class Account implements Serializable, Comparable<Account> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    double balance;
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "client_id", nullable = false)
    Client client;
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "currency_id", nullable = false)
    Currency currency;
    @Column(name = "amount_color")
    String amountColor;


    public Account() {
    }

    public Account(Client client, Currency currency) {
        this.client = client;
        this.currency = currency;
    }

    public Account(Client client, Currency currency, double balance) {
        this.client = client;
        this.currency = currency;
        this.balance = balance;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }


    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", balance=" + balance +
                ", client=" + client +
                ", currency=" + currency +
                '}';
    }

    @Override
    public int compareTo(Account o) {
        return this.currency.compareTo(o.currency);
    }
}


