package org.profinef.dto;

import jakarta.persistence.*;

@Entity
@Table(name = "currency")
public class CurrencyDto {
    @Id
    int id;
    @Column
    String name;
    @Column(name = "average_exchange")
    Double averageExchange;

    public CurrencyDto() {
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

    public Double getAverageExchange() {
        return averageExchange;
    }

    public void setAverageExchange(Double averageExchange) {
        this.averageExchange = averageExchange;
    }

    @Override
    public String toString() {
        return "CurrencyDto{" +
                "id=" + id +
                ", names='" + name + '\'' +
                ", averageExchange='" + averageExchange + '\'' +
                '}';
    }
}
