package com.smichelotti.mtg.service;

import com.smichelotti.mtg.dto.CardPriceResult;
import com.smichelotti.mtg.entity.WatchlistEntity;
import com.smichelotti.mtg.repository.WatchlistRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceTrackingSchedulerTest {

    private final CardComparisonService comparisonService =
            mock(CardComparisonService.class);

    private final HistoricalPriceService historicalPriceService =
            mock(HistoricalPriceService.class);

    private final WatchlistRepository watchlistRepository =
            mock(WatchlistRepository.class);

    private final PriceTrackingScheduler scheduler =
            new PriceTrackingScheduler(comparisonService, historicalPriceService, watchlistRepository);

    private WatchlistEntity watchlistEntity(String cardName, String edition) {

        WatchlistEntity entity = new WatchlistEntity();
        entity.setCardName(cardName);
        entity.setEdition(edition);
        return entity;
    }

    private CardPriceResult result(String cardName, double price) {

        return CardPriceResult.builder()
                .source("Scryfall")
                .cardName(cardName)
                .price(BigDecimal.valueOf(price))
                .currency("USD")
                .build();
    }

    @Test
    void doesNothing_whenWatchlistIsEmpty() {

        when(watchlistRepository.findAll()).thenReturn(List.of());

        scheduler.updateTrackedCards();

        verify(comparisonService, never()).compare(any(), any());
        verify(historicalPriceService, never()).save(any());
    }

    @Test
    void tracksCard_whenEditionIsNull() {

        when(watchlistRepository.findAll())
                .thenReturn(List.of(watchlistEntity("Sol Ring", null)));

        when(comparisonService.compare("Sol Ring", null))
                .thenReturn(List.of(result("Sol Ring", 1.5)));

        scheduler.updateTrackedCards();

        verify(comparisonService).compare("Sol Ring", null);
        verify(historicalPriceService).save(result("Sol Ring", 1.5));
    }

    @Test
    void tracksCard_whenEditionIsDefined() {

        when(watchlistRepository.findAll())
                .thenReturn(List.of(watchlistEntity("Counterspell", "lea")));

        when(comparisonService.compare("Counterspell", "lea"))
                .thenReturn(List.of(result("Counterspell", 40.0)));

        scheduler.updateTrackedCards();

        verify(comparisonService).compare("Counterspell", "lea");
        verify(historicalPriceService).save(result("Counterspell", 40.0));
    }

    @Test
    void tracksAllCards_whenWatchlistHasMultipleEntries() {

        when(watchlistRepository.findAll())
                .thenReturn(List.of(
                        watchlistEntity("Sol Ring", null),
                        watchlistEntity("Lightning Bolt", null),
                        watchlistEntity("Counterspell", "lea")));

        when(comparisonService.compare(any(), any())).thenReturn(List.of());

        scheduler.updateTrackedCards();

        verify(comparisonService).compare("Sol Ring", null);
        verify(comparisonService).compare("Lightning Bolt", null);
        verify(comparisonService).compare("Counterspell", "lea");
        verify(comparisonService, times(3)).compare(any(), any());
    }

    @Test
    void continuesProcessingRemainingCards_whenOneCardFails() {

        when(watchlistRepository.findAll())
                .thenReturn(List.of(
                        watchlistEntity("Sol Ring", null),
                        watchlistEntity("Cyclonic Rift", null)));

        when(comparisonService.compare("Sol Ring", null))
                .thenThrow(new RuntimeException("provider exploded"));

        when(comparisonService.compare("Cyclonic Rift", null))
                .thenReturn(List.of(result("Cyclonic Rift", 5.0)));

        scheduler.updateTrackedCards();

        verify(comparisonService).compare("Sol Ring", null);
        verify(comparisonService).compare("Cyclonic Rift", null);
        verify(historicalPriceService).save(result("Cyclonic Rift", 5.0));
    }

    @Test
    void savesEveryResult_whenCompareReturnsMultipleResults() {

        when(watchlistRepository.findAll())
                .thenReturn(List.of(watchlistEntity("Sol Ring", null)));

        when(comparisonService.compare("Sol Ring", null))
                .thenReturn(List.of(
                        result("Sol Ring", 1.5),
                        result("Sol Ring", 2.0)));

        scheduler.updateTrackedCards();

        verify(historicalPriceService).save(result("Sol Ring", 1.5));
        verify(historicalPriceService).save(result("Sol Ring", 2.0));
        verify(historicalPriceService, times(2)).save(any());
    }
}
