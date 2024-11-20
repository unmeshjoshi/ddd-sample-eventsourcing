package com.ddd_bootcamp.eventstore;

import com.ddd_bootcamp.domain.CartId;
import com.ddd_bootcamp.domain.events.CartCreatedEvent;
import com.ddd_bootcamp.domain.events.DomainEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class EventStorePollerTest {
    private static final String TOPIC = "cart-events";


    private static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    private EventStore eventStore;
    private EventStorePoller poller;
    private KafkaProducer<String, DomainEvent> producer;
    private KafkaConsumer<String, DomainEvent> consumer;
    private final List<DomainEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean keepConsuming = new AtomicBoolean(true);
    private Thread consumerThread;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = new JDBCDataSource();
        ((JDBCDataSource) dataSource).setUrl("jdbc:hsqldb:mem:testdb;DB_CLOSE_DELAY=-1");
        ((JDBCDataSource) dataSource).setUser("sa");
        ((JDBCDataSource) dataSource).setPassword("");
        // Set up EventStore
        eventStore = new HSQLDBEventStore(dataSource);

        kafka.start();
        // Set up Kafka Producer
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "com.ddd_bootcamp.kafka.DomainEventSerializer");
        producer = new KafkaProducer<>(producerProps);

        // Set up Kafka Consumer
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "com.ddd_bootcamp.kafka.DomainEventDeserializer");
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TOPIC));

        // Start consumer thread
        startConsumerThread();

        // Create and start the poller
        poller = new EventStorePoller(eventStore, producer, 100); // 100ms polling interval
        poller.start();
    }

    private void startConsumerThread() {
        consumerThread = new Thread(() -> {
            while (keepConsuming.get()) {
                consumer.poll(Duration.ofMillis(100))
                        .forEach(record -> receivedEvents.add(record.value()));
            }
        });
        consumerThread.start();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        poller.stop();
        keepConsuming.set(false);
        consumerThread.join(5000); // Wait for consumer thread to finish
        producer.close();
        consumer.close();
        receivedEvents.clear();
    }

    @Test
    void shouldPublishAndConsumeMultipleEvents() throws Exception {
        // Given
        CartId cartId1 = new CartId(UUID.randomUUID().toString());
        CartId cartId2 = new CartId(UUID.randomUUID().toString());
        CartCreatedEvent event1 = new CartCreatedEvent(cartId1);
        CartCreatedEvent event2 = new CartCreatedEvent(cartId2);

        // When
        eventStore.saveEvents(cartId1.toString(), List.of(event1));
        eventStore.saveEvents(cartId2.toString(), List.of(event2));

        // Then
        // Wait for events to be processed with timeout
        waitUntilTrue(()->{
            return receivedEvents.size() == 2;

        }, "waiting for events to be received", Duration.ofMillis(2000));

        // Verify
        assertEquals(2, receivedEvents.size(), "Should have received exactly two events");

        // Verify first event
        assertTrue(receivedEvents.get(0) instanceof CartCreatedEvent);
        // Verify second event
        assertTrue(receivedEvents.get(1) instanceof CartCreatedEvent);

        // Verify the cart IDs are present in the received events
        List<CartId> receivedCartIds = receivedEvents.stream()
                .map(e -> ((CartCreatedEvent) e).getCartId())
                .toList();
        assertTrue(receivedCartIds.contains(cartId1));
        assertTrue(receivedCartIds.contains(cartId2));
    }


    private static void waitUntilTrue(Supplier<Boolean> condition, String msg, Duration waitTime) {
        try {
            var startTime = System.nanoTime();
            while (true) {
                if (condition.get())
                    return;

                if (System.nanoTime() > (startTime + waitTime.toNanos())) {
                    fail(msg);
                }

                Thread.sleep(100L);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}