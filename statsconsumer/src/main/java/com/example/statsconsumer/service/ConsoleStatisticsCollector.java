package com.example.statsconsumer.service;

import com.example.statsconsumer.model.TransactionStats;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.collector", havingValue = "console", matchIfMissing = true)
public class ConsoleStatisticsCollector implements StatisticsCollector {

    private static final Logger log = LoggerFactory.getLogger(ConsoleStatisticsCollector.class);

    @Override
    public void collect(TransactionStats statistic) {
        log.info("Statistic -> {} {}",
                String.format("%-15s", statistic.type()),
                statistic.amount());
    }
    @PostConstruct
    public void init()
    {
        System.out.println("ConsoleStatisticsCollector loaded");
    }
}