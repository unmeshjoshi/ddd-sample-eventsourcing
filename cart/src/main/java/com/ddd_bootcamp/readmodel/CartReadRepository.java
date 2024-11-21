package com.ddd_bootcamp.readmodel;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.Currency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CartReadRepository {
    private static final Logger logger = LoggerFactory.getLogger(CartReadRepository.class);
    private final DataSource dataSource;

    public CartReadRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createCart(String cartId) {
        String sql = "INSERT INTO cart_read (cart_id, status, created_at, updated_at) VALUES (?, 'ACTIVE', ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            Timestamp now = Timestamp.from(Instant.now());
            stmt.setString(1, cartId);
            stmt.setTimestamp(2, now);
            stmt.setTimestamp(3, now);
            stmt.executeUpdate();
            
            logger.debug("Created cart in read model: {}", cartId);
        } catch (SQLException e) {
            throw new RuntimeException("Error creating cart in read model", e);
        }
    }

    public void addCartItem(String cartId, String productName, int quantity, BigDecimal price, Currency currency) {
        String sql = "INSERT INTO cart_item_read " +
                    "(cart_id, product_name, quantity, price_value, price_currency, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            Timestamp now = Timestamp.from(Instant.now());
            stmt.setString(1, cartId);
            stmt.setString(2, productName);
            stmt.setInt(3, quantity);
            stmt.setBigDecimal(4, price);
            stmt.setString(5, currency.getCurrencyCode());
            stmt.setTimestamp(6, now);
            stmt.setTimestamp(7, now);
            stmt.executeUpdate();
            
            logger.debug("Added item to cart in read model: {} - {}", cartId, productName);
        } catch (SQLException e) {
            throw new RuntimeException("Error adding item to cart in read model", e);
        }
    }

    public void markItemAsRemoved(String cartId, String productName) {
        String sql = "UPDATE cart_item_read SET is_removed = TRUE, updated_at = ? " +
                    "WHERE cart_id = ? AND product_name = ? AND is_removed = FALSE";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.from(Instant.now()));
            stmt.setString(2, cartId);
            stmt.setString(3, productName);
            int updatedRows = stmt.executeUpdate();
            
            logger.debug("Marked {} items as removed in cart: {} - {}", updatedRows, cartId, productName);
        } catch (SQLException e) {
            throw new RuntimeException("Error marking item as removed in read model", e);
        }
    }

    public void markCartAsCheckedOut(String cartId) {
        String sql = "UPDATE cart_read SET status = 'CHECKED_OUT', updated_at = ? WHERE cart_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.from(Instant.now()));
            stmt.setString(2, cartId);
            int updatedRows = stmt.executeUpdate();
            
            logger.debug("Marked cart as checked out: {} (rows updated: {})", cartId, updatedRows);
        } catch (SQLException e) {
            throw new RuntimeException("Error marking cart as checked out in read model", e);
        }
    }
} 