package org.profinef.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
@Entity
public class Currency implements Comparable<Currency>, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    String name;
    @Column(name = "average_exchange")
    Double averageExchange;

    public Currency() {
    }

    public Currency(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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
        return "Currency{" +
                "id=" + id +
                ", names='" + name + '\'' +
                ", averageExchange='" + averageExchange + '\'' +
                '}';
    }

    @Override
    public int compareTo(Currency o) {
        if (this.name.equals("RUB") || o.name.equals("UAH")) return 1;
        if (this.name.equals("UAH") || o.name.equals("RUB")) return -1;
        if (this.name.equals("PLN") || o.name.equals("USD")) return 1;
        if (this.name.equals("USD") || o.name.equals("PLN")) return -1;
        return this.getId().compareTo(o.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Currency currency)) return false;
        return Objects.equals(getId(), currency.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
