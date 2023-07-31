package org.profinef.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NBUCurrency {
    @JsonProperty("CurrencyCode")
    String currencyCode;
    @JsonProperty("CurrencyCodeL")
    String currencyCodeL;
    @JsonProperty("Amount")
    double amount;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCurrencyCodeL() {
        return currencyCodeL;
    }

    public void setCurrencyCodeL(String currencyCodeL) {
        this.currencyCodeL = currencyCodeL;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "NBUCurrency{" +
                "currencyCode='" + currencyCode + '\'' +
                ", currencyCodeL='" + currencyCodeL + '\'' +
                ", amount=" + amount +
                '}';
    }
}
