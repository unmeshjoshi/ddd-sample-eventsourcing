package com.ddd_bootcamp.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemRemovedFromCartEvent implements DomainEvent {
    private final String productName;

    @JsonCreator
    public ItemRemovedFromCartEvent(@JsonProperty("productName") String productName) {
        this.productName = productName;
    }

    @JsonProperty("productName")
    public String getProductName() {
        return productName;
    }
}
