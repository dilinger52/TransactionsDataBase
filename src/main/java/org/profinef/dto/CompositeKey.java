package org.profinef.dto;

import java.util.Objects;


public class CompositeKey {
    private Integer clientId;
    private Integer currencyId;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompositeKey that)) return false;
        return Objects.equals(clientId, that.clientId) && Objects.equals(currencyId, that.currencyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, currencyId);
    }
}
