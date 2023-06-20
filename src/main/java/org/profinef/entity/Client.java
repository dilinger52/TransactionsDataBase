package org.profinef.entity;

import jakarta.persistence.*;

import java.io.Serializable;

public class Client implements Serializable {

    int id;
    String pib;

    public Client(String pib) {
        this.id = 0;
        this.pib = pib;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPib() {
        return pib;
    }

    public void setPib(String pib) {
        this.pib = pib;
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", pib='" + pib + '\'' +
                '}';
    }
}
