package org.profinef.dto;

import java.util.Objects;

public class CompositeKey2 {
    private Integer clientId;
    private Integer id;
    private Integer currencyId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompositeKey2 that)) return false;
        return Objects.equals(clientId, that.clientId) && Objects.equals(id, that.id) && Objects.equals(currencyId, that.currencyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, id, currencyId);
    }
}
