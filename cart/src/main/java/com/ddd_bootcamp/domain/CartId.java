package com.ddd_bootcamp.domain;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class CartId {
    private final String id;

    @JsonCreator
    public CartId(String id) {
        this.id = id;
    }

    public static CartId generateCartId() {
        return new CartId(UUID.randomUUID().toString());
    }

    @JsonValue
    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CartId cartId = (CartId) o;
        return id.equals(cartId.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
