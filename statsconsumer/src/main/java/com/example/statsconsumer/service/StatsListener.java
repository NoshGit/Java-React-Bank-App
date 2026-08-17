package com.example.statsconsumer.service;

import com.example.statsconsumer.model.TransactionStats;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class StatsListener {

    private static final Logger log = LoggerFactory.getLogger(StatsListener.class);

    private final StatisticsCollector collector;

    public StatsListener(StatisticsCollector collector) {
        this.collector = collector;
    }

    @KafkaListener(topics = "transaction-stats")
    public void handle(ConsumerRecord<String, TransactionStats> record) {
        TransactionStats transactionStats = record.value();

        log.info("Received key={} partition={} offset={}",
                record.key(), record.partition(), record.offset());

        TransactionStats statistic = new TransactionStats(
                transactionStats.type(),
                transactionStats.amount()
        );

        collector.collect(statistic);
    }
}

