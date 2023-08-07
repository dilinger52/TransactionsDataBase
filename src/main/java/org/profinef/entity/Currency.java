package org.profinef.entity;

import java.io.Serializable;
import java.util.Objects;

public class Currency implements Comparable<Currency>, Serializable {
    Integer id;
    String name;
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
