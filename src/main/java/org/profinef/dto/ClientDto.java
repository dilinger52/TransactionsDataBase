package org.profinef.dto;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name="client")
public class ClientDto implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @Column
    String pib;
    @Column(name = "phone_number")
    String phone;
    @Column
    String telegram;
    @Column
    String color;

    public ClientDto() {
    }

    public ClientDto(String pib) {
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

    public void setPib(String pib) {
        this.pib = pib;
    }

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
        return "ClientDto{" +
                "id=" + id +
                ", pib='" + pib + '\'' +
                ", phone='" + phone + '\'' +
                ", telegram='" + telegram + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}
