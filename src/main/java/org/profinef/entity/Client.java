package org.profinef.entity;

import jakarta.persistence.*;

import java.io.Serializable;

public class Client implements Serializable {

    Integer id;
    String pib;
    String phone;
    String telegram;


    public Client(String pib) {
        this.id = 0;
        this.pib = pib;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPib() {
        return pib;
    }

    public void setPib(String pib){this.pib = pib;}

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTelegram() {
        return telegram;
    }

    public void setTelegram(String telegram) {
        this.telegram = telegram;
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", pib='" + pib + '\'' +
                ", phone='" + phone + '\'' +
                ", telegram='" + telegram + '\'' +
                '}';
    }
}
