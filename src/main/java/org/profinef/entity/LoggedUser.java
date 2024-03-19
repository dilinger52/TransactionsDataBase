package org.profinef.entity;

import jakarta.persistence.Entity;

import java.io.Serializable;
import java.util.Objects;
public class LoggedUser implements Serializable {
    private int userId;
    private int counter;

    public LoggedUser(int userId, int counter) {
        this.userId = userId;
        this.counter = counter;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoggedUser that)) return false;
        return userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "LoggedUser{" +
                "userId=" + userId +
                ", counter=" + counter +
                '}';
    }
}
