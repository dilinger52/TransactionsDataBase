package org.profinef.dto;

import jakarta.persistence.*;

@Entity
@Table(name = "currency")
public class CurrencyDto {
    @Id
    int id;
    @Column
    String name;

    public CurrencyDto() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "CurrencyDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
