package com.ddd_bootcamp.readmodel;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CartReadRepositoryTest {
    private CartReadRepository repository;
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = createDataSource();
        createSchema();
        repository = new CartReadRepository(dataSource);
    }

    private DataSource createDataSource() {
        JDBCDataSource ds = new JDBCDataSource();
        ds.setUrl("jdbc:hsqldb:mem:testdb;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createSchema() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            createCartTable(conn);
            createCartItemTable(conn);
            createCartItemIndex(conn);
        }
    }

    private void createCartTable(Connection conn) throws Exception {
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS cart_read (
                cart_id VARCHAR(36) PRIMARY KEY,
                status VARCHAR(20) NOT NULL,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
        """);
    }

    private void createCartItemTable(Connection conn) throws Exception {
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS cart_item_read (
                id BIGINT IDENTITY PRIMARY KEY,
                cart_id VARCHAR(36) NOT NULL,
                product_name VARCHAR(255) NOT NULL,
                quantity INT NOT NULL,
                price_value DECIMAL(10,2) NOT NULL,
                price_currency VARCHAR(3) NOT NULL,
                is_removed BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                FOREIGN KEY (cart_id) REFERENCES cart_read(cart_id)
            )
        """);
    }

    private void createCartItemIndex(Connection conn) throws Exception {
        conn.createStatement().execute("""
            CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id 
            ON cart_item_read(cart_id)
        """);
    }

    @Test
    void shouldCreateCart() throws Exception {
        String cartId = UUID.randomUUID().toString();
        repository.createCart(cartId);
        assertCartExists(cartId);
    }

    private void assertCartExists(String cartId) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM cart_read WHERE cart_id = ?")) {
            
            stmt.setString(1, cartId);
            ResultSet rs = stmt.executeQuery();

            assertTrue(rs.next(), "Cart should exist in database");
            assertEquals("ACTIVE", rs.getString("status"));
            assertValidTimestamps(rs);
            assertFalse(rs.next(), "Should only have one cart record");
        }
    }

    @Test
    void shouldAddCartItem() throws Exception {
        String cartId = UUID.randomUUID().toString();
        repository.createCart(cartId);
        repository.addCartItem(cartId, "Test Product", 2, new BigDecimal("10"), Currency.getInstance("USD"));
        assertCartItemExists(cartId, "Test Product", 2, 10.00, "USD");
    }

    private void assertCartItemExists(String cartId, String productName, int quantity, 
                                    double price, String currency) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = prepareCartItemQuery(conn, cartId)) {
            
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next(), "Cart item should exist");
            assertCartItemFields(rs, productName, quantity, price, currency);
            assertValidTimestamps(rs);
            assertFalse(rs.next(), "Should only have one item");
        }
    }

    private PreparedStatement prepareCartItemQuery(Connection conn, String cartId) throws Exception {
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT * FROM cart_item_read WHERE cart_id = ?"
        );
        stmt.setString(1, cartId);
        return stmt;
    }

    private void assertCartItemFields(ResultSet rs, String productName, int quantity, 
                                    double price, String currency) throws Exception {
        assertEquals(productName, rs.getString("product_name"));
        assertEquals(quantity, rs.getInt("quantity"));
        assertEquals(price, rs.getDouble("price_value"));
        assertEquals(currency, rs.getString("price_currency"));
        assertFalse(rs.getBoolean("is_removed"));
    }

    private void assertValidTimestamps(ResultSet rs) throws Exception {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        assertNotNull(createdAt, "created_at should not be null");
        assertNotNull(updatedAt, "updated_at should not be null");
    }

    @Test
    void shouldMarkItemAsRemoved() throws Exception {
        String cartId = UUID.randomUUID().toString();
        String productName = "Test Product";
        
        repository.createCart(cartId);
        repository.addCartItem(cartId, productName, 2, new BigDecimal("10"), Currency.getInstance("USD"));
        repository.markItemAsRemoved(cartId, productName);
        
        assertItemIsRemoved(cartId, productName);
    }

    private void assertItemIsRemoved(String cartId, String productName) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT is_removed FROM cart_item_read WHERE cart_id = ? AND product_name = ?")) {
            
            stmt.setString(1, cartId);
            stmt.setString(2, productName);
            ResultSet rs = stmt.executeQuery();

            assertTrue(rs.next(), "Cart item should exist");
            assertTrue(rs.getBoolean("is_removed"), "Item should be marked as removed");
        }
    }

    @Test
    void shouldMarkCartAsCheckedOut() throws Exception {
        String cartId = UUID.randomUUID().toString();
        repository.createCart(cartId);
        repository.markCartAsCheckedOut(cartId);
        assertCartIsCheckedOut(cartId);
    }

    private void assertCartIsCheckedOut(String cartId) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT status FROM cart_read WHERE cart_id = ?")) {
            
            stmt.setString(1, cartId);
            ResultSet rs = stmt.executeQuery();

            assertTrue(rs.next(), "Cart should exist");
            assertEquals("CHECKED_OUT", rs.getString("status"));
        }
    }
} 