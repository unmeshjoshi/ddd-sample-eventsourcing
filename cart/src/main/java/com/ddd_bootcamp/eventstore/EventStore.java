package com.ddd_bootcamp.eventstore;

import com.ddd_bootcamp.domain.events.DomainEvent;

import java.util.List;

public interface EventStore {
    void saveEvents(String aggregateId, List<DomainEvent> events);
    List<DomainEvent> getEvents(String aggregateId);
    List<StoredEvent> getEventsAfter(long l);

}