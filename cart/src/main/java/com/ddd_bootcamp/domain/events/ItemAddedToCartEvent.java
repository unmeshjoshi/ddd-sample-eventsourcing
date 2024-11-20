package com.ddd_bootcamp.domain.events;

import com.ddd_bootcamp.domain.Price;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemAddedToCartEvent implements DomainEvent {
    private final String productName;
    private final int quantity;
    private final Price price;

    @JsonCreator
    public ItemAddedToCartEvent(
        @JsonProperty("productName") String productName,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("price") Price price
    ) {
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    @JsonProperty("productName")
    public String getProductName() {
        return productName;
    }

    @JsonProperty("quantity")
    public int getQuantity() {
        return quantity;
    }

    @JsonProperty("price")
    public Price getPrice() {
        return price;
    }
}
