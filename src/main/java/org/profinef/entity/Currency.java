package org.profinef.entity;

import jakarta.persistence.*;

public class Currency {

    int id;
    String name;
    double exchangeRate;

    public Currency() {
    }

    public Currency(String name, double exchangeRate) {
        this.id = 0;
        this.name = name;
        this.exchangeRate = exchangeRate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(double exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    @Override
    public String toString() {
        return "Currency{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
