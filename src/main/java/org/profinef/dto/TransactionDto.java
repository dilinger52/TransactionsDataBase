package org.profinef.dto;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

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
    String comment;
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
    @Column(name = "comment_color")
    String commentColor;
    @Column(name = "amount_color")
    String amountColor;
    @Column(name = "input_color")
    String inputColor;
    @Column(name = "output_color")
    String outputColor;
    @Column(name = "tarif_color")
    String tarifColor;
    @Column(name = "commission_color")
    String commisionColor;
    @Column(name = "rate_color")
    String rateColor;
    @Column(name = "transportation_color")
    String transportationColor;

    @Column(name = "user_id")
    int userId;


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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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
        return commisionColor;
    }

    public void setCommissionColor(String commisionColor) {
        this.commisionColor = commisionColor;
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
        return "TransactionDto{" +
                "id=" + id +
                ", date=" + date +
                ", clientId=" + clientId +
                ", currencyId=" + currencyId +
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
                ", commisionColor='" + commisionColor + '\'' +
                ", rateColor='" + rateColor + '\'' +
                ", transportationColor='" + transportationColor + '\'' +
                ", userId=" + userId +
                '}';
    }
}

