package org.profinef.entity;

public class Currency implements Comparable<Currency> {
    Integer id;
    String name;

    public Currency() {
    }

    public Currency(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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
        return "Currency{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public int compareTo(Currency o) {
        return this.getId().compareTo(o.getId());
    }
}
