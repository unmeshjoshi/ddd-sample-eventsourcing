package com.ddd_bootcamp.domain;    

import com.ddd_bootcamp.domain.events.*;
import com.ddd_bootcamp.exceptions.ItemNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Cart implements Aggregate {

    private CartId cartId;
    private List<Item> items = new ArrayList<>();
    private boolean isCheckedOut;
    private long version = 0;

    public Cart(CartId cartId) {
        this.cartId = cartId;
    }

    public Cart() {
        this(CartId.generateCartId());
    }

    public static Cart recreateFrom(CartId cartId, List<DomainEvent> events) {
        Cart cart = new Cart(cartId);
        events.forEach(cart::applyEvent);
        return cart;
    }

    public void applyEvent(DomainEvent event) {
        if (event instanceof ItemAddedToCartEvent) {
            ItemAddedToCartEvent itemAdded = (ItemAddedToCartEvent) event;
            apply(itemAdded);
        } else if (event instanceof ItemRemovedFromCartEvent) {
            ItemRemovedFromCartEvent itemRemoved = (ItemRemovedFromCartEvent) event;
            apply(itemRemoved);
        } else if (event instanceof CartCheckedOutEvent) {
            CartCheckedOutEvent cartCheckedOut = (CartCheckedOutEvent) event;
            apply(cartCheckedOut);
        }
        version++;
    }

    private void apply(CartCheckedOutEvent cartCheckedOut) {
        isCheckedOut = true;
    }

    private void apply(ItemRemovedFromCartEvent itemRemoved) {
        items.remove(items.stream()
            .filter(item -> item.getProductName().equals(itemRemoved.getProductName()))
            .findFirst()
            .orElseThrow(() -> new ItemNotFoundException(itemRemoved.getProductName())));
    }

    private void apply(ItemAddedToCartEvent itemAdded) {
        Item newItem = new Item(
            new Product(itemAdded.getProductName(), itemAdded.getPrice()),
            itemAdded.getQuantity()
        );
        items.add(newItem);
    }

    public CartId getCartId() {
        return cartId;
    }

    public List<Item> getItems() {
        return new ArrayList<>(items);
    }

    public long getVersion() {
        return version;
    }

    public boolean isCheckedOut() {
        return isCheckedOut;
    }

    @Override
    public String toString() {
        return items.toString();
    }

    public void remove(Item item) {
        ItemRemovedFromCartEvent itemRemovedFromCartEvent =
                new ItemRemovedFromCartEvent(item.getProductName(), cartId.getId());

        apply(itemRemovedFromCartEvent);
    }

    public void checkOut() {
        List<CartItem> cartItems = items.stream().map(item ->
                new CartItem(item.getProductName(),
                        item.getProductPrice(),
                        item.getQuantity())).collect(Collectors.toList());
        //publish on messaging system pub/sub
        apply(new CartCheckedOutEvent(cartId, cartItems));
    }

    public List<Product> getFlattenedProducts() {
        return items.stream().flatMap(item ->
                item.getFlattenedProducts().stream()).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cart cart = (Cart) o;
        return cartId.equals(cart.cartId);
    }

    @Override
    public int hashCode() {
        return cartId.hashCode();
    }

    public void add(Item item) {
        ItemAddedToCartEvent itemAddedEvent =
                new ItemAddedToCartEvent(item.getProductName(),
                        item.getQuantity(), item.getProductPrice(), cartId.getId());

        apply(itemAddedEvent);
    }
}