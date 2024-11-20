package com.ddd_bootcamp.repository;

import com.ddd_bootcamp.domain.Cart;
import com.ddd_bootcamp.domain.events.CartCreatedEvent;
import com.ddd_bootcamp.domain.CartId;
import com.ddd_bootcamp.domain.events.DomainEvent;
import com.ddd_bootcamp.exceptions.CartNotFoundException;
import com.ddd_bootcamp.eventstore.EventStore;

import java.util.List;

public class CartRepository {
    private final EventStore eventStore;

    public CartRepository(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public void save(Cart cart, DomainEvent event) {
        eventStore.saveEvents(
            cart.getCartId().toString(),
            List.of(event)
        );
        cart.applyEvent(event);
    }

    public Cart findById(CartId cartId) {
        List<DomainEvent> events = eventStore.getEvents(cartId.toString());
        if (events.isEmpty()) {
            throw new CartNotFoundException(cartId);
        }
        return Cart.recreateFrom(cartId, events);
    }

    public void save(Cart cart) {
        eventStore.saveEvents(
                cart.getCartId().toString(),
                List.of(new CartCreatedEvent(cart.getCartId()))
        );
    }
}