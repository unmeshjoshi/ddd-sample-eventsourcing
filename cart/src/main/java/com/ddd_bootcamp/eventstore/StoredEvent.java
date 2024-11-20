package com.ddd_bootcamp.eventstore;

import com.ddd_bootcamp.domain.events.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StoredEvent {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final long eventId;
    private final String aggregateId;
    private final String eventType;
    private final String eventData;
    private final long sequenceNumber;

    public StoredEvent(long eventId, String aggregateId, String eventType, String eventData, long sequenceNumber) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.sequenceNumber = sequenceNumber;
    }

    public long getEventId() {
        return eventId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public DomainEvent toDomainEvent() {
        try {
            Class<?> eventClass = Class.forName(eventType);
            return (DomainEvent) objectMapper.readValue(eventData, eventClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
} 