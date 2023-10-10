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
    String commentColor;
    String amountColor;
    String inputColor;
    String outputColor;
    String tarifColor;
    String commissionColor;
    String rateColor;
    String transportationColor;
    User user;


    public Transaction() {
    }

    public Transaction(Integer id, Timestamp date, Client client, Currency currency, String comment, Double rate, Double commission, Double amount, Double balance, Double transportation, User user) {
        this.id = id;
        this.date = date;
        this.client = client;
        this.currency = currency;
        this.comment = comment;
        this.rate = rate;
        this.commission = commission;
        this.amount = amount;
        this.balance = balance;
        this.transportation = transportation;
        this.user = user;
    }

    public Transaction(Integer id, Timestamp date, Client client, Currency currency, String comment, Double rate, Double commission, Double amount, Double balance, Double transportation, String commentColor, String amountColor, String inputColor, String outputColor, String tarifColor, String commissionColor, String rateColor, String transportationColor, User user) {
        this.id = id;
        this.date = date;
        this.client = client;
        this.currency = currency;
        this.comment = comment;
        this.rate = rate;
        this.commission = commission;
        this.amount = amount;
        this.balance = balance;
        this.transportation = transportation;
        this.commentColor = commentColor;
        this.amountColor = amountColor;
        this.inputColor = inputColor;
        this.outputColor = outputColor;
        this.tarifColor = tarifColor;
        this.commissionColor = commissionColor;
        this.rateColor = rateColor;
        this.transportationColor = transportationColor;
        this.user = user;
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

    public String getCommentColor() {
        return commentColor;
    }

    public void setCommentColor(String commentColor) {
        this.commentColor = commentColor;
    }

    public String getAmountColor() {
        return amountColor;
    }

    public void setAmountColor(String amountColor) {
        this.amountColor = amountColor;
    }


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getInputColor() {
        return inputColor;
    }

    public void setInputColor(String inputColor) {
        this.inputColor = inputColor;
    }

    public String getOutputColor() {
        return outputColor;
    }

    public void setOutputColor(String outputColor) {
        this.outputColor = outputColor;
    }

    public String getTarifColor() {
        return tarifColor;
    }

    public void setTarifColor(String tarifColor) {
        this.tarifColor = tarifColor;
    }

    public String getCommissionColor() {
        return commissionColor;
    }

    public void setCommissionColor(String commissionColor) {
        this.commissionColor = commissionColor;
    }

    public String getRateColor() {
        return rateColor;
    }

    public void setRateColor(String rateColor) {
        this.rateColor = rateColor;
    }

    public String getTransportationColor() {
        return transportationColor;
    }

    public void setTransportationColor(String transportationColor) {
        this.transportationColor = transportationColor;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", date=" + date +
                ", client=" + client +
                ", currency=" + currency +
                ", comment='" + comment + '\'' +
                ", rate=" + rate +
                ", commission=" + commission +
                ", amount=" + amount +
                ", balance=" + balance +
                ", transportation=" + transportation +
                ", commentColor='" + commentColor + '\'' +
                ", amountColor='" + amountColor + '\'' +
                ", inputColor='" + inputColor + '\'' +
                ", outputColor='" + outputColor + '\'' +
                ", tarifColor='" + tarifColor + '\'' +
                ", commissionColor='" + commissionColor + '\'' +
                ", rateColor='" + rateColor + '\'' +
                ", transportationColor='" + transportationColor + '\'' +
                ", user=" + user +
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
