package com.ddd_bootcamp.domain.events;

import java.util.List;

import com.ddd_bootcamp.domain.CartId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CartCheckedOutEvent implements DomainEvent {
    private final CartId cartId;
    private final List<CartItem> cartItems;

    @JsonCreator
    public CartCheckedOutEvent(@JsonProperty("cartId") CartId cartId, @JsonProperty("cartItems")List<CartItem> cartItems) {
        this.cartId = cartId;
        this.cartItems = cartItems;
    }

    @JsonProperty("cartId")
    public CartId getCartId() {
        return cartId;
    }

    @JsonProperty("cartItems")
    public List<CartItem> getCartItems() {
        return cartItems;
    }
}
