package com.ddd_bootcamp.service;

import com.ddd_bootcamp.domain.*;
import com.ddd_bootcamp.eventstore.ConcurrencyException;
import com.ddd_bootcamp.eventstore.EventStore;
import com.ddd_bootcamp.eventstore.HSQLDBEventStore;
import com.ddd_bootcamp.repository.CartRepository;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.*;
import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class CartServiceTest {
    private CartService cartService;
    private CartRepository cartRepository;
    private DataSource dataSource;
    private EventStore eventStore;

    @BeforeEach
    void setUp() {
        // Setup HSQLDB in-memory database
        dataSource = new JDBCDataSource();
        ((JDBCDataSource) dataSource).setUrl("jdbc:hsqldb:mem:testdb;DB_CLOSE_DELAY=-1");
        ((JDBCDataSource) dataSource).setUser("sa");
        ((JDBCDataSource) dataSource).setPassword("");

        // Initialize event store and service
        eventStore = new HSQLDBEventStore(dataSource);
        cartRepository = new CartRepository(eventStore);
        cartService = new CartService(cartRepository);
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Clean up database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE event_store IF EXISTS");
        }
    }

    @Test
    void shouldAddItemToCart() {
        // Given
        CartId cartId = CartId.generateCartId();
        cartRepository.save(new Cart(cartId));

        // When
        Product product = new Product("Test Product", new Price(BigDecimal.TEN, USD()));
        Item item = new Item(product, 2);
        cartService.addItem(cartId, item);

        // Then
        Cart cart = cartRepository.findById(cartId);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0))
            .extracting("productName", "quantity")
            .containsExactly("Test Product", 2);
    }

    @Test
    void shouldRemoveItemFromCart() {
        // Given
        CartId cartId = CartId.generateCartId();
        cartRepository.save(new Cart(cartId));

        Product product = new Product("Test Product", new Price(BigDecimal.TEN, USD()));
        Item item = new Item(product, 1);
        cartService.addItem(cartId, item);

        // When
        cartService.removeItem(cartId, item);

        // Then
        Cart cart = cartRepository.findById(cartId);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void shouldCheckOutCart() {
        // Given
        CartId cartId = CartId.generateCartId();
        cartRepository.save(new Cart(cartId));

        Product product = new Product("Test Product", new Price(BigDecimal.TEN, USD()));
        Item item = new Item(product, 1);
        cartService.addItem(cartId, item);

        // When
        cartService.checkOut(cartId);

        // Then
        Cart cart = cartRepository.findById(cartId);
        assertThat(cart.isCheckedOut()).isTrue();
    }



    @Test
    void shouldReconstructCartStateFromEvents() {
        // Given
        CartId cartId = CartId.generateCartId();
        cartRepository.save(new Cart(cartId));

        Product product1 = new Product("Product 1", new Price(BigDecimal.TEN, USD()));
        Product product2 = new Product("Product 2", new Price(BigDecimal.valueOf(20), USD()));
        
        // When - Creating a sequence of events
        cartService.addItem(cartId, new Item(product1, 2));
        cartService.addItem(cartId, new Item(product2, 1));
        cartService.removeItem(cartId, new Item(product1, 2));

        // Then - Cart should be reconstructed correctly
        Cart cart = cartRepository.findById(cartId);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0))
            .extracting("productName", "quantity")
            .containsExactly("Product 2", 1);
    }

    private static Currency USD() {
        return Currency.getInstance("USD");
    }
} 