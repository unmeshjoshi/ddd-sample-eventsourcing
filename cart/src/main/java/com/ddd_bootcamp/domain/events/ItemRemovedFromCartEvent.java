package com.ddd_bootcamp.domain.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemRemovedFromCartEvent implements DomainEvent {
    private final String productName;
    private final String cartId;

    @JsonCreator
    public ItemRemovedFromCartEvent(
            @JsonProperty("productName") String productName,
            @JsonProperty("cartId") String cartId) {

        this.productName = productName;
        this.cartId = cartId;
    }

    @JsonProperty("productName")
    public String getProductName() {
        return productName;
    }


    @JsonProperty("cartId")
    public String getCartId() {
        return cartId;
    }
}
