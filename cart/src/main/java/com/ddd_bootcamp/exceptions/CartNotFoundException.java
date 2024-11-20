package com.ddd_bootcamp.exceptions;

import com.ddd_bootcamp.domain.CartId;

public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException(CartId cartId) {
        super("Cart not found with id: " + cartId);
    }
} 