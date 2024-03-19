package org.profinef.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.profinef.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
public class Client implements Serializable, Comparable<Client>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    String pib;
    @Column(name = "phone_number")
    String phone;
    String telegram;
    String color;

    public Client() {
    }

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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", pib='" + pib + '\'' +
                ", phone='" + phone + '\'' +
                ", telegram='" + telegram + '\'' +
                ", color='" + color + '\'' +
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
