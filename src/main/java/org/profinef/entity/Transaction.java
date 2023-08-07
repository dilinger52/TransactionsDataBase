package org.profinef.entity;


import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

public class Transaction implements Serializable {

    Integer id;
    Timestamp date;
    Client client;
    Currency currency;
    String comment;
    Double rate;
    Double commission;
    Double amount;
    Double balance;
    Double transportation;
    String pibColor;
    String amountColor;
    String balanceColor;
    User user;


    public Transaction() {
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", date=" + date +
                ", client=" + client +
                ", currency=" + currency +
                ", comment=" + comment +
                ", rate=" + rate +
                ", commission=" + commission +
                ", amount=" + amount +
                ", balance=" + balance +
                ", transportation=" + transportation +
                ", pibColor='" + pibColor + '\'' +
                ", amountColor='" + amountColor + '\'' +
                ", balanceColor='" + balanceColor + '\'' +
                ", user='" + user + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getClient(), that.getClient()) && Objects.equals(getCurrency(), that.getCurrency());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getClient(), getCurrency());
    }
}
