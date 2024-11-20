package com.ddd_bootcamp.domain.events;

import com.ddd_bootcamp.domain.Price;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CartItem {
    private final String name;
    private final int quantity;
    private final Price price;

    @JsonCreator
    public CartItem(
        @JsonProperty("name") String name,
        @JsonProperty("price") Price price,
        @JsonProperty("quantity") int quantity
    ) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
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
