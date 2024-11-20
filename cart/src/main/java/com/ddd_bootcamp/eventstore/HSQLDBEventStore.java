package com.ddd_bootcamp.eventstore;

import com.ddd_bootcamp.domain.events.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HSQLDBEventStore implements EventStore {
    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(HSQLDBEventStore.class);

    public HSQLDBEventStore(DataSource dataSource) {
        this.dataSource = dataSource;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS event_store (
                   event_id BIGINT IDENTITY PRIMARY KEY, -- Auto-incrementing primary key
                   aggregate_id VARCHAR(36) NOT NULL,
                    sequence_number BIGINT NOT NULL,
                    event_type VARCHAR(255) NOT NULL,
                    event_data CLOB NOT NULL,
                    timestamp TIMESTAMP NOT NULL
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize event store", e);
        }
    }

    @Override
    public void saveEvents(String aggregateId, List<DomainEvent> events) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long version = getCurrentVersion(conn, aggregateId);
                try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO event_store (aggregate_id, sequence_number, event_type, event_data, timestamp) VALUES (?, ?, ?, ?, ?)")) {
                    
                    for (DomainEvent event : events) {
                        ps.setString(1, aggregateId);
                        ps.setLong(2, ++version);
                        ps.setString(3, event.getClass().getName());
                        ps.setString(4, serializeEvent(event));
                        ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                        ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save events", e);
        }
    }

    @Override
    public List<DomainEvent> getEvents(String aggregateId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT event_type, event_data FROM event_store WHERE aggregate_id = ? ORDER BY sequence_number")) {
            
            ps.setString(1, aggregateId);
            List<DomainEvent> events = new ArrayList<>();
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String eventType = rs.getString("event_type");
                    String eventData = rs.getString("event_data");
                    events.add(deserializeEvent(eventType, eventData));
                }
            }
            return events;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load events", e);
        }
    }

    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    private DomainEvent deserializeEvent(String type, String data) {
        try {
            Class<?> eventClass = Class.forName(type);
            return (DomainEvent) objectMapper.readValue(data, eventClass);
        } catch (JsonProcessingException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }

    private long getCurrentVersion(Connection conn, String aggregateId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT MAX(sequence_number) FROM event_store WHERE aggregate_id = ?")) {
            ps.setString(1, aggregateId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    @Override
    public List<StoredEvent> getEventsAfter(long eventId) {
        String sql = "SELECT event_id, aggregate_id, sequence_number, event_type, event_data " +
                "FROM event_store " +
                "WHERE event_id >= ? " +
                "ORDER BY event_id ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, eventId);
            ResultSet rs = stmt.executeQuery();

            List<StoredEvent> events = new ArrayList<>();
            while (rs.next()) {
                StoredEvent event = new StoredEvent(
                        rs.getLong("event_id"),
                        rs.getString("aggregate_id"),
                        rs.getString("event_type"),
                        rs.getString("event_data"),
                        rs.getLong("sequence_number")
                );
                events.add(event);
            }

            if (!events.isEmpty()) {
                logger.debug("Retrieved {} events after event_id {}", events.size(), eventId);
            }

            return events;

        } catch (SQLException e) {
            logger.error("Error retrieving events after event_id {}", eventId, e);
            throw new RuntimeException("Failed to retrieve events from event store", e);
        }
    }
} 