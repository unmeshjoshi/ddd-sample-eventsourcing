package com.ddd_bootcamp.eventstore;

import com.ddd_bootcamp.domain.events.DomainEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class EventStorePoller {
    private static final Logger logger = LoggerFactory.getLogger(EventStorePoller.class);
    private static final String TOPIC_NAME = "cart-events";
    
    private final EventStore eventStore;
    private final KafkaProducer<String, DomainEvent> kafkaProducer;
    private final AtomicLong lastProcessedEventId = new AtomicLong(0);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final long pollingIntervalMs;

    public EventStorePoller(EventStore eventStore, KafkaProducer<String, DomainEvent> kafkaProducer, long pollingIntervalMs) {
        this.eventStore = eventStore;
        this.kafkaProducer = kafkaProducer;
        this.pollingIntervalMs = pollingIntervalMs;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(
            this::pollAndPublish,
            0,
            pollingIntervalMs,
            TimeUnit.MILLISECONDS
        );
        logger.info("Event store poller started with interval: {} ms", pollingIntervalMs);
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Event store poller stopped");
    }

    private void pollAndPublish() {
        try {
            List<StoredEvent> events = eventStore.getEventsAfter(lastProcessedEventId.get());
            
            for (StoredEvent event : events) {
                publishToKafka(event);
                lastProcessedEventId.set(event.getEventId());
                logger.debug("Published event {} to Kafka", event.getEventId());
            }
            
            if (!events.isEmpty()) {
                logger.info("Published {} events to Kafka", events.size());
            }
        } catch (Exception e) {
            logger.error("Error while polling and publishing events", e);
        }
    }

    private void publishToKafka(StoredEvent event) {
        ProducerRecord<String, DomainEvent> record = new ProducerRecord<>(
            TOPIC_NAME,
            event.getAggregateId(),
            event.toDomainEvent()
        );

        kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                logger.error("Failed to publish event {} to Kafka", event.getEventId(), exception);
            }
        });
    }
} 