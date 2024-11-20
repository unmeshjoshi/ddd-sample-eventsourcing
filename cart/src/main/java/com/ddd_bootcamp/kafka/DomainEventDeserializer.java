package com.ddd_bootcamp.kafka;

import com.ddd_bootcamp.domain.events.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

public class DomainEventDeserializer implements Deserializer<DomainEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DomainEvent deserialize(String topic, byte[] data) {
        try {
            EventWrapper wrapper = objectMapper.readValue(data, EventWrapper.class);
            Class<?> eventClass = Class.forName(wrapper.getType());
            return (DomainEvent) objectMapper.readValue(wrapper.getData(), eventClass);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing DomainEvent", e);
        }
    }
} 