package org.profinef.entity;


import jakarta.persistence.*;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
@Entity
public class Transaction implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @Column(name = "operation_id")
    int operationId;
    Timestamp date;
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinColumn(name = "account_id", nullable = false)
    Account account;
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
    String balanceColor;
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinColumn(name = "user_id", nullable = false)
    User user;


    public Transaction() {
    }

    public Transaction(int operationId, Timestamp date, Account account, String comment, Double rate, Double commission, Double amount, Double balance, Double transportation, String commentColor, String amountColor, String inputColor, String outputColor, String tarifColor, String commissionColor, String rateColor, String transportationColor, String balanceColor, User user) {
        this.date = date;
        this.account = account;
        this.operationId = operationId;
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
        this.balanceColor = balanceColor;
        this.user = user;
    }

    public Transaction(Transaction transaction) {
        this.id = transaction.getId();
        this.date = transaction.getDate();
        this.account = transaction.getAccount();
        this.operationId = transaction.getOperationId();
        this.comment = transaction.getComment();
        this.rate = transaction.getRate();
        this.commission = transaction.getCommission();
        this.amount = transaction.getAmount();
        this.balance = transaction.getBalance();
        this.transportation = transaction.getTransportation();
        this.commentColor = transaction.getCommentColor();
        this.amountColor = transaction.getAmountColor();
        this.inputColor = transaction.getInputColor();
        this.outputColor = transaction.getOutputColor();
        this.tarifColor = transaction.getTarifColor();
        this.commissionColor = transaction.getCommissionColor();
        this.rateColor = transaction.getRateColor();
        this.transportationColor = transaction.getTransportationColor();
        this.balanceColor = transaction.getBalanceColor();
        this.user = transaction.getUser();
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

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public int getOperationId() {
        return operationId;
    }

    public void setOperationId(int operationId) {
        this.operationId = operationId;
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

    public String getBalanceColor() {
        return balanceColor;
    }

    public void setBalanceColor(String balanceColor) {
        this.balanceColor = balanceColor;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", operationId=" + operationId +
                ", date=" + date +
                ", account=" + account +
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
                ", balanceColor='" + balanceColor + '\'' +
                ", user=" + user +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return Objects.equals(getId(), that.getId()) && Objects.equals(this.account, that.account);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getAccount());
    }
}
