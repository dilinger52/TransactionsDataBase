package org.profinef.dbo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaction")
public class TransactionDbo {
    @Id
    int id;
    @Column(name = "client1_id")
    int client1Id;
    @Column(name = "currency1_id")
    int currency1Id;
    @Column(name = "client2_id")
    int client2Id;
    @Column(name = "currency2_id")
    int currency2Id;
    @Column(name = "amount")
    double amount;

    public TransactionDbo() {
    }

    public TransactionDbo(int client1Id, int currency1Id, int client2Id, int currency2Id, double amount) {
        this.id = 0;
        this.client1Id = client1Id;
        this.currency1Id = currency1Id;
        this.client2Id = client2Id;
        this.currency2Id = currency2Id;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClient1Id() {
        return client1Id;
    }

    public void setClient1Id(int client1Id) {
        this.client1Id = client1Id;
    }

    public int getCurrency1Id() {
        return currency1Id;
    }

    public void setCurrency1Id(int currency1Id) {
        this.currency1Id = currency1Id;
    }

    public int getClient2Id() {
        return client2Id;
    }

    public void setClient2Id(int client2Id) {
        this.client2Id = client2Id;
    }

    public int getCurrency2Id() {
        return currency2Id;
    }

    public void setCurrency2Id(int currency2Id) {
        this.currency2Id = currency2Id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "client1Id=" + client1Id +
                ", currency1Id=" + currency1Id +
                ", client2Id=" + client2Id +
                ", currency2Id=" + currency2Id +
                ", amount=" + amount +
                '}';
    }
}
