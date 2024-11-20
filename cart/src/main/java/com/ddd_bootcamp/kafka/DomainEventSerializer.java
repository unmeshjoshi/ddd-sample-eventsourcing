package com.ddd_bootcamp.kafka;

import com.ddd_bootcamp.domain.events.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

public class DomainEventSerializer implements Serializer<DomainEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, DomainEvent event) {
        try {
            // Include type information for deserialization
            return objectMapper.writeValueAsBytes(new EventWrapper(
                event.getClass().getName(),
                objectMapper.writeValueAsString(event)
            ));
        } catch (Exception e) {
            throw new RuntimeException("Error serializing DomainEvent", e);
        }
    }
} 