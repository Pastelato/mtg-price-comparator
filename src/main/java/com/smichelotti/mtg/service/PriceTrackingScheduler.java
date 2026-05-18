package com.smichelotti.mtg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriceTrackingScheduler {

    private final CardComparisonService comparisonService;

    @Scheduled(fixedRate = 300000)
    public void updateTrackedCards() {

        System.out.println("=== RUNNING PRICE UPDATE ===");

        comparisonService.compare(
                "lightning bolt",
                null);

        comparisonService.compare(
                "sol ring",
                null);

        comparisonService.compare(
                "cyclonic rift",
                null);
    }
}
