package org.profinef.entity;


import java.io.Serializable;
import java.util.Objects;

public class Client implements Serializable, Comparable<Client>{

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

    @Override
    public int compareTo(Client o) {
        return this.getId().compareTo(o.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Client client)) return false;
        return Objects.equals(getId(), client.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
