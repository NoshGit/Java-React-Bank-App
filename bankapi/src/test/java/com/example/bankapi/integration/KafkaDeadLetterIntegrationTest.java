package com.example.bankapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@EmbeddedKafka(partitions = 1, topics = {"test-topic", "test-topic.DLT"})
@SpringBootTest
public class KafkaDeadLetterIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private ConcurrentMessageListenerContainer<String, String> container;

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    void failingListenerMessageIsPublishedToDlt() throws Exception {
        String topic = "test-topic";
        String dlt = topic + ".DLT";

        // Producer
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new StringSerializer());
        KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(pf);
        kafkaTemplate.setDefaultTopic(topic);

        // DeadLetterPublishingRecoverer -> always route to the DLT topic partition 0
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (r, e) -> new TopicPartition(dlt, 0));

        // Error handler that immediately routes to recoverer (no retries)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));

        // Consumer factory for the listener container
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "false", embeddedKafkaBroker);
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());

        // Container with a listener that throws for any message
        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setMessageListener((MessageListener<String, String>) record -> {
            throw new RuntimeException("simulated handler failure");
        });

        container = new ConcurrentMessageListenerContainer<>(cf, containerProps);
        container.setCommonErrorHandler(errorHandler);
        container.start();

        // Send a message that will cause the listener to fail
        kafkaTemplate.send(new ProducerRecord<>(topic, "key-1", "payload-1")).get(5, TimeUnit.SECONDS);

        // Create a consumer to read from the DLT and assert the failed message arrives
        Map<String, Object> dltConsumerProps = KafkaTestUtils.consumerProps("dltGroup", "false", embeddedKafkaBroker);
        DefaultKafkaConsumerFactory<String, String> dltCf = new DefaultKafkaConsumerFactory<>(dltConsumerProps, new StringDeserializer(), new StringDeserializer());
        Consumer<String, String> dltConsumer = dltCf.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(dltConsumer, dlt);

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(dltConsumer, Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThan(0);

        // Optionally assert content
        assertThat(records.iterator().next().value()).isEqualTo("payload-1");

        dltConsumer.close();
    }
}
