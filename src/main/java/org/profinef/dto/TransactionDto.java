package org.profinef.dto;

import jakarta.persistence.*;

@Entity
@Table(name = "transaction")
public class TransactionDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @Column(name = "client1_id")
    Integer client1Id;
    @Column(name = "currency1_id")
    Integer currency1Id;
    @Column
    Double rate1;
    @Column
    Double commission1;
    @Column
    Double amount1;
    @Column(name = "client2_id")
    Integer client2Id;
    @Column(name = "currency2_id")
    Integer currency2Id;
    @Column
    Double rate2;
    @Column
    Double commission2;
    @Column
    Double amount2;
    @Column(name = "client3_id")
    Integer client3Id;
    @Column(name = "currency3_id")
    Integer currency3Id;
    @Column
    Double rate3;
    @Column
    Double commission3;
    @Column
    Double amount3;
    @Column(name = "client4_id")
    Integer client4Id;
    @Column(name = "currency4_id")
    Integer currency4Id;
    @Column
    Double rate4;
    @Column
    Double commission4;
    @Column
    Double amount4;
    @Column(name = "client5_id")
    Integer client5Id;
    @Column(name = "currency5_id")
    Integer currency5Id;
    @Column
    Double rate5;
    @Column
    Double commission5;
    @Column
    Double amount5;
    @Column(name = "client6_id")
    Integer client6Id;
    @Column(name = "currency6_id")
    Integer currency6Id;
    @Column
    Double rate6;
    @Column
    Double commission6;
    @Column
    Double amount6;


    public TransactionDto() {
    }

    public TransactionDto(Integer client1Id, Integer currency1Id, Double rate1, Integer client2Id, Integer currency2Id, Double rate2, Double amount) {
        this.id = 0;
        this.client1Id = client1Id;
        this.currency1Id = currency1Id;
        this.client2Id = client2Id;
        this.currency2Id = currency2Id;
        this.amount1 = amount;
        this.rate1 = rate1;
        this.rate2 = rate2;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getClient1Id() {
        return client1Id;
    }

    public void setClient1Id(Integer client1Id) {
        this.client1Id = client1Id;
    }

    public Integer getCurrency1Id() {
        return currency1Id;
    }

    public void setCurrency1Id(Integer currency1Id) {
        this.currency1Id = currency1Id;
    }

    public Integer getClient2Id() {
        return client2Id;
    }

    public void setClient2Id(Integer client2Id) {
        this.client2Id = client2Id;
    }

    public Integer getCurrency2Id() {
        return currency2Id;
    }

    public void setCurrency2Id(Integer currency2Id) {
        this.currency2Id = currency2Id;
    }

    public Double getAmount1() {
        return amount1;
    }

    public void setAmount1(Double amount1) {
        this.amount1 = amount1;
    }

    public Double getRate1() {
        return rate1;
    }

    public void setRate1(Double rate1) {
        this.rate1 = rate1;
    }

    public Double getCommission1() {
        return commission1;
    }

    public void setCommission1(Double commission1) {
        this.commission1 = commission1;
    }

    public Double getRate2() {
        return rate2;
    }

    public void setRate2(Double rate2) {
        this.rate2 = rate2;
    }

    public Double getCommission2() {
        return commission2;
    }

    public void setCommission2(Double commission2) {
        this.commission2 = commission2;
    }

    public Double getAmount2() {
        return amount2;
    }

    public void setAmount2(Double amount2) {
        this.amount2 = amount2;
    }

    public Integer getClient3Id() {
        return client3Id;
    }

    public void setClient3Id(Integer client3Id) {
        this.client3Id = client3Id;
    }

    public Integer getCurrency3Id() {
        return currency3Id;
    }

    public void setCurrency3Id(Integer currency3Id) {
        this.currency3Id = currency3Id;
    }

    public Double getRate3() {
        return rate3;
    }

    public void setRate3(Double rate3) {
        this.rate3 = rate3;
    }

    public Double getCommission3() {
        return commission3;
    }

    public void setCommission3(Double commission3) {
        this.commission3 = commission3;
    }

    public Double getAmount3() {
        return amount3;
    }

    public void setAmount3(Double amount3) {
        this.amount3 = amount3;
    }

    public Integer getClient4Id() {
        return client4Id;
    }

    public void setClient4Id(Integer client4Id) {
        this.client4Id = client4Id;
    }

    public Integer getCurrency4Id() {
        return currency4Id;
    }

    public void setCurrency4Id(Integer currency4Id) {
        this.currency4Id = currency4Id;
    }

    public Double getRate4() {
        return rate4;
    }

    public void setRate4(Double rate4) {
        this.rate4 = rate4;
    }

    public Double getCommission4() {
        return commission4;
    }

    public void setCommission4(Double commission4) {
        this.commission4 = commission4;
    }

    public Double getAmount4() {
        return amount4;
    }

    public void setAmount4(Double amount4) {
        this.amount4 = amount4;
    }

    public Integer getClient5Id() {
        return client5Id;
    }

    public void setClient5Id(Integer client5Id) {
        this.client5Id = client5Id;
    }

    public Integer getCurrency5Id() {
        return currency5Id;
    }

    public void setCurrency5Id(Integer currency5Id) {
        this.currency5Id = currency5Id;
    }

    public Double getRate5() {
        return rate5;
    }

    public void setRate5(Double rate5) {
        this.rate5 = rate5;
    }

    public Double getCommission5() {
        return commission5;
    }

    public void setCommission5(Double commission5) {
        this.commission5 = commission5;
    }

    public Double getAmount5() {
        return amount5;
    }

    public void setAmount5(Double amount5) {
        this.amount5 = amount5;
    }

    public Integer getClient6Id() {
        return client6Id;
    }

    public void setClient6Id(Integer client6Id) {
        this.client6Id = client6Id;
    }

    public Integer getCurrency6Id() {
        return currency6Id;
    }

    public void setCurrency6Id(Integer currency6Id) {
        this.currency6Id = currency6Id;
    }

    public Double getRate6() {
        return rate6;
    }

    public void setRate6(Double rate6) {
        this.rate6 = rate6;
    }

    public Double getCommission6() {
        return commission6;
    }

    public void setCommission6(Double commission6) {
        this.commission6 = commission6;
    }

    public Double getAmount6() {
        return amount6;
    }

    public void setAmount6(Double amount6) {
        this.amount6 = amount6;
    }

    @Override
    public String toString() {
        return "TransactionDbo{" +
                "id=" + id +
                ", client1Id=" + client1Id +
                ", currency1Id=" + currency1Id +
                ", rate1=" + rate1 +
                ", commission1=" + commission1 +
                ", amount1=" + amount1 +
                ", client2Id=" + client2Id +
                ", currency2Id=" + currency2Id +
                ", rate2=" + rate2 +
                ", commission2=" + commission2 +
                ", amount2=" + amount2 +
                ", client3Id=" + client3Id +
                ", currency3Id=" + currency3Id +
                ", rate3=" + rate3 +
                ", commission3=" + commission3 +
                ", amount3=" + amount3 +
                ", client4Id=" + client4Id +
                ", currency4Id=" + currency4Id +
                ", rate4=" + rate4 +
                ", commission4=" + commission4 +
                ", amount4=" + amount4 +
                ", client5Id=" + client5Id +
                ", currency5Id=" + currency5Id +
                ", rate5=" + rate5 +
                ", commission5=" + commission5 +
                ", amount5=" + amount5 +
                ", client6Id=" + client6Id +
                ", currency6Id=" + currency6Id +
                ", rate6=" + rate6 +
                ", commission6=" + commission6 +
                ", amount6=" + amount6 +
                '}';
    }
}

