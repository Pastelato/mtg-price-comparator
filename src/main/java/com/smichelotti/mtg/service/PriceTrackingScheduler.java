package com.smichelotti.mtg.service;

import com.smichelotti.mtg.dto.CardPriceResult;
import com.smichelotti.mtg.entity.WatchlistEntity;
import com.smichelotti.mtg.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceTrackingScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceTrackingScheduler.class);

    private final CardComparisonService comparisonService;
    private final HistoricalPriceService historicalPriceService;
    private final WatchlistRepository watchlistRepository;

    @Scheduled(fixedRate = 300000)
    public void updateTrackedCards() {

        log.info("=== RUNNING PRICE UPDATE ===");

        List<WatchlistEntity> watchlist = watchlistRepository.findAll();

        for (WatchlistEntity entity : watchlist) {
            try {
                track(entity.getCardName(), entity.getEdition());
            } catch (Exception e) {
                log.error("TRACK FAILED -> card='{}' edition='{}'",
                        entity.getCardName(), entity.getEdition(), e);
            }
        }
    }

    private void track(String cardName, String edition) {

        List<CardPriceResult> results = comparisonService.compare(cardName, edition);

        results.forEach(historicalPriceService::save);
    }
}
