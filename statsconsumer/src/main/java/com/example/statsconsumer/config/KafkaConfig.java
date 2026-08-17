package com.example.statsconsumer.config;

import com.example.statsconsumer.model.TransactionStats;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.dead-letter-topic}")
    private String deadLetterTopic;

    @Value("${app.kafka.retry-interval-ms}")
    private long retryIntervalMs;

    @Value("${app.kafka.retry-max-attempts}")
    private long retryMaxAttempts;

   //Read Topic name from config
    @Value("${app.kafka.transaction-stats}")
    private String transactionStatsTopic;

    // Create topics at startup so broker auto-creation is not required
    @Bean
    public NewTopic transactionStatsTopic() {
        log.info("Creating topic: transaction-stats (if not exists)");
        return TopicBuilder.name(transactionStatsTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic deadLetterTopic() {
        log.info("Creating topic: {} (if not exists)", deadLetterTopic);
        return TopicBuilder.name(deadLetterTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // disable adding type info headers to match consumer config
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionStats> kafkaListenerContainerFactory(
            ConsumerFactory<String, TransactionStats> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, TransactionStats> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Route failed records to transaction-stats-dead-letter preserving partition
        BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> destResolver =
                (record, ex) -> {
                    log.error("Sending record to DLT topic '{}' for key={} partition={} offset={} due to: {}",
                            deadLetterTopic, record.key(), record.partition(), record.offset(), ex.toString());
                    return new TopicPartition(deadLetterTopic, record.partition());
                };

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, destResolver);

        // Retries configured via application.yaml
        log.info("Configuring DefaultErrorHandler with retry-interval-ms={}, retry-max-attempts={}",
                retryIntervalMs, retryMaxAttempts);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(retryIntervalMs, retryMaxAttempts));

        // Also log each failure when handler retries or recovers
        errorHandler.setRetryListeners((record, ex, attempt) ->
                log.warn("Retry attempt={} for record key={} partition={} offset={} due to: {}",
                        attempt, record.key(), record.partition(), record.offset(), ex.toString())
        );

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
