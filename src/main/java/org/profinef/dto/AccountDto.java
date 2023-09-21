package org.profinef.dto;

import jakarta.persistence.*;

@Entity
@Table(name = "account")
@IdClass(CompositeKey.class)
public class AccountDto {
    @Id
    Integer clientId;
    @Id
    Integer currencyId;
    @Column
    double amount;
    @Column(name = "amount_color")
    String amountColor;

    public AccountDto() {
    }

    public AccountDto(Integer clientId, Integer currencyId, double amount) {
        this.clientId = clientId;
        this.currencyId = currencyId;
        this.amount = amount;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }


    public String getAmountColor() {
        return amountColor;
    }

    public void setAmountColor(String amountColor) {
        this.amountColor = amountColor;
    }

    @Override
    public String toString() {
        return "AccountDto{" +
                "clientId=" + clientId +
                ", currencyId=" + currencyId +
                ", amount=" + amount +
                ", amountColor='" + amountColor + '\'' +
                '}';
    }
}

