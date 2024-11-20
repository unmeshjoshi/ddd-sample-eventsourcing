package com.ddd_bootcamp.domain.events;

import com.ddd_bootcamp.domain.CartId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CartCreatedEvent implements DomainEvent {
    private final CartId cartId;

    @JsonCreator
    public CartCreatedEvent(@JsonProperty("cartId") CartId cartId) {
        this.cartId = cartId;
    }

    @JsonProperty("cartId")
    public CartId getCartId() {
        return cartId;
    }
}
