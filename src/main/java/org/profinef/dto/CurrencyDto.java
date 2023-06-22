package org.profinef.dto;

import jakarta.persistence.*;

@Entity
@Table(name = "currency")
@IdClass(CompositeKey.class)
public class CurrencyDto {
    @Id
    Integer clientId;
    @Id
    Integer currencyId;
    @Column
    String name;
    @Column
    double amount;

    public CurrencyDto() {
    }

    public CurrencyDto(Integer clientId, Integer currencyId, String name, double amount) {
        this.clientId = clientId;
        this.currencyId = currencyId;
        this.name = name;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "CurrencyDbo{" +
                "clientId=" + clientId +
                ", currencyId=" + currencyId +
                ", name='" + name + '\'' +
                ", amount=" + amount +
                '}';
    }
}

