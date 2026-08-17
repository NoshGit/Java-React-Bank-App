package com.example.statsconsumer.service;

import com.example.statsconsumer.model.TransactionStats;

public interface StatisticsCollector {
    void collect(TransactionStats statistic);
}