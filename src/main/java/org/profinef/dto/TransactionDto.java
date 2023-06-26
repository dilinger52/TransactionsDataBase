package org.profinef.dto;

import jakarta.persistence.*;

import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "transaction")
@IdClass(CompositeKey2.class)
public class TransactionDto {
    @Id
    Integer id;
    @Column
    Timestamp date;
    @Id
    @Column(name = "client_id")
    Integer clientId;
    @Column(name = "currency_id")
    Integer currencyId;
    @Column
    Double rate;
    @Column
    Double commission;
    @Column
    Double amount;
    @Column
    Double balance;
    @Column
    Double transportation;


    public TransactionDto() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Double getCommission() {
        return commission;
    }

    public void setCommission(Double commission) {
        this.commission = commission;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public Double getTransportation() {
        return transportation;
    }

    public void setTransportation(Double transportation) {
        this.transportation = transportation;
    }

    @Override
    public String toString() {
        return "TransactionDto{" +
                "id=" + id +
                ", date=" + date +
                ", clientId=" + clientId +
                ", currencyId=" + currencyId +
                ", rate=" + rate +
                ", commission=" + commission +
                ", amount=" + amount +
                ", balance=" + balance +
                ", transportation=" + transportation +
                '}';
    }
}

