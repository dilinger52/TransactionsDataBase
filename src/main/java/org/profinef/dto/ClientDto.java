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

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", pib='" + pib + '\'' +
                '}';
    }
}
