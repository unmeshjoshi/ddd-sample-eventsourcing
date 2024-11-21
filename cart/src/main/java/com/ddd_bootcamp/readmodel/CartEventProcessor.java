package com.ddd_bootcamp.readmodel;

import com.ddd_bootcamp.domain.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CartEventProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CartEventProcessor.class);
    private final HSQLDBCartReadRepository repository;

    public CartEventProcessor(HSQLDBCartReadRepository repository) {
        this.repository = repository;
    }

    public void process(DomainEvent event) {
        if (event instanceof CartCreatedEvent) {
            processCartCreated((CartCreatedEvent) event);
        } else if (event instanceof ItemAddedToCartEvent) {
            processItemAdded((ItemAddedToCartEvent) event);
        } else if (event instanceof ItemRemovedFromCartEvent) {
            processItemRemoved((ItemRemovedFromCartEvent) event);
        } else if (event instanceof CartCheckedOutEvent) {
            processCartCheckedOut((CartCheckedOutEvent) event);
        }
    }

    private void processCartCreated(CartCreatedEvent event) {
        repository.createCart(event.getCartId().toString());
        logger.debug("Processed CartCreatedEvent for cart {}", event.getCartId());
    }

    private void processItemAdded(ItemAddedToCartEvent event) {
        repository.addCartItem(
            event.getCartId().toString(),
            event.getProductName(),
            event.getQuantity(),
            event.getPrice().getValue(),
            event.getPrice().getCurrency()
        );
        logger.debug("Processed ItemAddedToCartEvent for cart {}", event.getCartId());
    }

    private void processItemRemoved(ItemRemovedFromCartEvent event) {
        repository.markItemAsRemoved(event.getCartId(), event.getProductName());
        logger.debug("Processed ItemRemovedFromCartEvent for cart {}", event.getCartId());
    }

    private void processCartCheckedOut(CartCheckedOutEvent event) {
        repository.markCartAsCheckedOut(event.getCartId().toString());
        logger.debug("Processed CartCheckedOutEvent for cart {}", event.getCartId());
    }
} 