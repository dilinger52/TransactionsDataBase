package org.profinef.entity;

import java.io.Serializable;

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
}
