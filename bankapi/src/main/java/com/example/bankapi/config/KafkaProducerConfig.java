package com.example.bankapi.config;

import com.example.bankapi.kafka.TransactionStatsMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${app.kafka.transaction-stats-topic}")
    private String transactionStatsTopic;

    @Bean
    public ProducerFactory<String, TransactionStatsMessage> transactionStatsProducerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, TransactionStatsMessage> transactionStatsKafkaTemplate(
            ProducerFactory<String, TransactionStatsMessage> transactionStatsProducerFactory) {
        return new KafkaTemplate<>(transactionStatsProducerFactory);
    }

    @Bean
    public NewTopic transactionStatsTopic() {
        return new NewTopic(transactionStatsTopic, 1, (short) 1);
    }
}
