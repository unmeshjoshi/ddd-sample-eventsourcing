package com.ddd_bootcamp.service;

import com.ddd_bootcamp.domain.Cart;
import com.ddd_bootcamp.domain.CartId;
import com.ddd_bootcamp.domain.Item;
import com.ddd_bootcamp.domain.events.CartCheckedOutEvent;
import com.ddd_bootcamp.domain.events.CartItem;
import com.ddd_bootcamp.domain.events.ItemAddedToCartEvent;
import com.ddd_bootcamp.domain.events.ItemRemovedFromCartEvent;
import com.ddd_bootcamp.repository.CartRepository;

import java.util.List;
import java.util.stream.Collectors;

public class CartService {
    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public void addItem(CartId cartId, Item item) {
        Cart cart = cartRepository.findById(cartId);
        ItemAddedToCartEvent event = new ItemAddedToCartEvent(
            item.getProductName(),
            item.getQuantity(),
            item.getProductPrice(),
            cartId.getId()
        );
        cartRepository.save(cart, event);
    }

    public void removeItem(CartId cartId, Item item) {
        Cart cart = cartRepository.findById(cartId);
        ItemRemovedFromCartEvent event = new ItemRemovedFromCartEvent(
            item.getProductName(), cartId.getId()
        );
        cartRepository.save(cart, event);
    }

    public void checkOut(CartId cartId) {
        Cart cart = cartRepository.findById(cartId);
        List<CartItem> cartItems = cart.getItems().stream()
            .map(item -> new CartItem(
                item.getProductName(),
                item.getProductPrice(),
                item.getQuantity()))
            .collect(Collectors.toList());
            
        CartCheckedOutEvent event = new CartCheckedOutEvent(cartId, cartItems);
        cartRepository.save(cart, event);
    }
} 