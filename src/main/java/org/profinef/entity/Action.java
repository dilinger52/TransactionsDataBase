package org.profinef.entity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Action {
    private final UUID id;
    private String name;
    private List<Transaction> changes;

    public Action() {
        this.id = UUID.randomUUID();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Transaction> getChanges() {
        return changes;
    }

    public void setChanges(List<Transaction> changes) {
        this.changes = changes;
    }


    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Action{" +
                "name='" + name + '\'' +
                ", changes=" + changes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Action action)) return false;
        return id == action.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
