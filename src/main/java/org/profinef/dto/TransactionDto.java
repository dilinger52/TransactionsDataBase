package org.profinef.dto;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "transaction")
@IdClass(CompositeKey2.class)
public class TransactionDto {
    @Id
    Integer id;
    @Column
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    Timestamp date;
    @Id
    @Column(name = "client_id")
    Integer clientId;
    @Id
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
    @Column(name = "pib_color")
    String pibColor;
    @Column(name = "amount_color")
    String amountColor;
    @Column(name = "balance_color")
    String balanceColor;

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

    public String getPibColor() {
        return pibColor;
    }

    public void setPibColor(String pibColor) {
        this.pibColor = pibColor;
    }

    public String getAmountColor() {
        return amountColor;
    }

    public void setAmountColor(String amountColor) {
        this.amountColor = amountColor;
    }

    public String getBalanceColor() {
        return balanceColor;
    }

    public void setBalanceColor(String balanceColor) {
        this.balanceColor = balanceColor;
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
                ", pibColor='" + pibColor + '\'' +
                ", amountColor='" + amountColor + '\'' +
                ", balanceColor='" + balanceColor + '\'' +
                '}';
    }
}

