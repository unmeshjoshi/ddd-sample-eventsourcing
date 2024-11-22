package com.ddd_bootcamp.readmodel;

import java.util.List;

// Read-only interface for clients
public interface CartReadRepository {
    List<CartItemReadModel> getRemovedItems(String cartId);
    // Add other read-only methods as needed
    // For example:
    // CartReadModel getCart(String cartId);
    // List<CartItemReadModel> getActiveItems(String cartId);
} 