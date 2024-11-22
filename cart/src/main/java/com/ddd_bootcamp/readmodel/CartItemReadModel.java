package com.ddd_bootcamp.readmodel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

public record CartItemReadModel(
    String productName,
    int quantity,
    BigDecimal price,
    Currency currency,
    Instant createdAt,
    Instant removedAt
) {} 