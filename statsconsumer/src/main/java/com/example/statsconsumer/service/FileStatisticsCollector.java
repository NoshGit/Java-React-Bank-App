package com.example.statsconsumer.service;

import com.example.statsconsumer.model.TransactionStats;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

@Service
@ConditionalOnProperty(name = "app.collector", havingValue = "file")
public class FileStatisticsCollector implements StatisticsCollector {

    private static final Logger log = LoggerFactory.getLogger(FileStatisticsCollector.class);
    private static final Path OUTPUT_PATH = Paths.get("C:/kafka-data/transactions-output.csv");

    private final BufferedWriter writer;

    public FileStatisticsCollector() throws IOException {
        Files.createDirectories(OUTPUT_PATH.getParent());
        boolean fileIsNew = !Files.exists(OUTPUT_PATH);

        this.writer = Files.newBufferedWriter(
                OUTPUT_PATH,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );

        if (fileIsNew) {
            writer.write("timestamp,type,amount");
            writer.newLine();
            writer.flush();
        }

        log.info("FileStatisticsCollector writing to {}", OUTPUT_PATH);
    }

    @Override
    public void collect(TransactionStats statistic) {
        try {
            writer.write(String.format("%s,%s,%s",
                    Instant.now(),
                    statistic.type(),
                    statistic.amount()));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            log.error("Failed to write statistic to file", e);
        }
    }

    @PreDestroy
    public void close() throws IOException {
        log.info("Closing file output writer");
        writer.close();
    }
}