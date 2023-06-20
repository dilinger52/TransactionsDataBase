package org.profinef.dbo;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name="client")
public class ClientDbo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @Column
    String pib;

    public ClientDbo() {
    }

    public ClientDbo(String pib) {
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
