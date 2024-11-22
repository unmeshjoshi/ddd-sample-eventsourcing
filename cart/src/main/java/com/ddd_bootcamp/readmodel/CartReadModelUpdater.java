package com.ddd_bootcamp.readmodel;

import java.math.BigDecimal;
import java.util.Currency;

// Write interface for event processor
public interface CartReadModelUpdater {
    void createCart(String cartId);
    void addCartItem(String cartId, String productName, int quantity, BigDecimal price, Currency currency);
    void markItemAsRemoved(String cartId, String productName);
    void markCartAsCheckedOut(String cartId);
} 