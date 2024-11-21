package com.ddd_bootcamp;

import com.ddd_bootcamp.domain.*;
import com.ddd_bootcamp.domain.events.DomainEvent;
import com.ddd_bootcamp.eventstore.EventStore;
import com.ddd_bootcamp.eventstore.EventStorePoller;
import com.ddd_bootcamp.eventstore.HSQLDBEventStore;
import com.ddd_bootcamp.readmodel.CartEventProcessor;
import com.ddd_bootcamp.readmodel.CartReadRepository;
import com.ddd_bootcamp.repository.CartRepository;
import com.ddd_bootcamp.service.CartService;
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
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Currency;
import java.util.Properties;

import static com.ddd_bootcamp.eventstore.EventStorePollerTest.waitUntilTrue;
import static org.junit.jupiter.api.Assertions.*;

class CartEndToEndTest {
    private static final String TOPIC = "cart-events";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    private CartService cartService;
    private CartReadRepository readRepository;
    private EventStorePoller poller;
    private CartEventProcessor eventProcessor;
    private DataSource writeDataSource;
    private DataSource readDataSource;
    private CartRepository cartWriteRepository;

    @BeforeEach
    void setUp() throws Exception {
        kafka.start();
        
        // Set up write-side database
        writeDataSource = createDataSource("writedb");
        EventStore eventStore = new HSQLDBEventStore(writeDataSource);
        cartWriteRepository = new CartRepository(eventStore);
        cartService = new CartService(cartWriteRepository);

        // Set up read-side database
        readDataSource = createDataSource("readdb");
        createReadSchema(readDataSource);
        readRepository = new CartReadRepository(readDataSource);
        
        // Set up Kafka producer/consumer and event processing
        KafkaProducer<String, DomainEvent> producer = createKafkaProducer();
        eventProcessor = new CartEventProcessor(readRepository);
        
        // Start the event poller
        poller = new EventStorePoller(eventStore, producer, 100);
        poller.start();
        
        // Start consuming events
        startEventConsumer();
    }

    private DataSource createDataSource(String dbName) {
        JDBCDataSource ds = new JDBCDataSource();
        ds.setUrl("jdbc:hsqldb:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createReadSchema(DataSource dataSource) throws Exception {
        try (var conn = dataSource.getConnection()) {
            conn.createStatement().execute("""
                CREATE TABLE cart_read (
                    cart_id VARCHAR(36) PRIMARY KEY,
                    status VARCHAR(20) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
            """);
            
            conn.createStatement().execute("""
                CREATE TABLE cart_item_read (
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
    }

    private KafkaProducer<String, DomainEvent> createKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "com.ddd_bootcamp.kafka.DomainEventSerializer");
        return new KafkaProducer<>(props);
    }

    private void startEventConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "com.ddd_bootcamp.kafka.DomainEventDeserializer");

        KafkaConsumer<String, DomainEvent> consumer = new KafkaConsumer<>(props);
        new Thread(() -> {
            consumer.subscribe(java.util.Collections.singletonList(TOPIC));
            while (!Thread.currentThread().isInterrupted()) {
                consumer.poll(Duration.ofMillis(100))
                        .forEach(record -> eventProcessor.process(record.value()));
            }
        }).start();
    }

    @AfterEach
    void tearDown() {
        poller.stop();
        kafka.stop();
    }

    @Test
    void shouldCreateCartAndAddItems() throws Exception {
        // Given
        CartId cartId = CartId.generateCartId();
        Cart cart = new Cart(cartId);
        cartWriteRepository.save(cart);

        Product product1 = new Product("Product 1", new Price(BigDecimal.TEN, Currency.getInstance("USD")));
        Product product2 = new Product("Product 2", new Price(BigDecimal.valueOf(20), Currency.getInstance("USD")));

        // When
        cartService.addItem(cartId, new Item(product1, 2));
        cartService.addItem(cartId, new Item(product2, 1));
        cartService.removeItem(cartId, new Item(product1, 2));

        // Then - Wait for read model to be updated
        waitUntilTrue(() -> {
            return assertCartCreated(cartId);
        }, "Waiting for read model to be updated", TIMEOUT);

        // Verify final state in read model
        assertFinalState(cartId);
    }

    private void assertFinalState(CartId cartId) throws SQLException {
        try (var conn = readDataSource.getConnection();
             var stmt = conn.prepareStatement(
                 "SELECT * FROM cart_item_read WHERE cart_id = ? AND is_removed = false"
             )) {
            stmt.setString(1, cartId.toString());
            var rs = stmt.executeQuery();

            assertTrue(rs.next(), "Should have one active item");
            assertEquals("Product 2", rs.getString("product_name"));
            assertEquals(1, rs.getInt("quantity"));
            assertEquals(20.00, rs.getDouble("price_value"));
            assertEquals("USD", rs.getString("price_currency"));
            assertFalse(rs.next(), "Should only have one active item");
        }
    }

    private boolean assertCartCreated(CartId cartId)  {
        try (var conn = readDataSource.getConnection();
             var stmt = conn.prepareStatement(
                 "SELECT COUNT(*) as count FROM cart_item_read WHERE cart_id = ? AND is_removed = false"
             )) {
            stmt.setString(1, cartId.toString());
            var rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("count") == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
} 