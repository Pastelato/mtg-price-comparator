package com.smichelotti.mtg.service;

import com.smichelotti.mtg.dto.CardPriceResult;
import com.smichelotti.mtg.dto.ResolvedEdition;
import com.smichelotti.mtg.provider.CardPriceProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CardComparisonServiceTest {

    private final HistoricalPriceService historicalPriceService =
            mock(HistoricalPriceService.class);

    private final EditionResolver editionResolver =
            mock(EditionResolver.class);

    private static final ResolvedEdition RESOLVED_EOE =
            new ResolvedEdition("Edge of Eternities", "eoe", "Edge of Eternities");

    private CardPriceResult result(String source, String cardName, String edition,
                                    Boolean foil, String externalId, double price) {

        return CardPriceResult.builder()
                .source(source)
                .cardName(cardName)
                .edition(edition)
                .price(BigDecimal.valueOf(price))
                .currency("USD")
                .foil(foil)
                .externalId(externalId)
                .build();
    }

    @Test
    void keepsFoilAndNonFoilPrintingsAsDistinctRows_whenNoExternalId() {

        CardPriceProvider provider = mock(CardPriceProvider.class);

        when(editionResolver.resolve("Edge of Eternities")).thenReturn(RESOLVED_EOE);

        when(provider.search("Anticausal Vestige", RESOLVED_EOE))
                .thenReturn(List.of(
                        result("Card Kingdom", "Anticausal Vestige", "Edge of Eternities", false, null, 2.0),
                        result("Card Kingdom", "Anticausal Vestige", "Edge of Eternities", true, null, 25.0)));

        CardComparisonService service =
                new CardComparisonService(List.of(provider), historicalPriceService, editionResolver);

        List<CardPriceResult> results = service.compare("Anticausal Vestige", "Edge of Eternities");

        assertThat(results).hasSize(2);
    }

    @Test
    void deduplicatesIdenticalLegacyKey_whenFoilMatches() {

        CardPriceProvider provider = mock(CardPriceProvider.class);

        when(editionResolver.resolve("Edge of Eternities")).thenReturn(RESOLVED_EOE);

        when(provider.search("Anticausal Vestige", RESOLVED_EOE))
                .thenReturn(List.of(
                        result("Card Kingdom", "Anticausal Vestige", "Edge of Eternities", false, null, 2.0),
                        result("Card Kingdom", "Anticausal Vestige", "Edge of Eternities", false, null, 2.0)));

        CardComparisonService service =
                new CardComparisonService(List.of(provider), historicalPriceService, editionResolver);

        List<CardPriceResult> results = service.compare("Anticausal Vestige", "Edge of Eternities");

        assertThat(results).hasSize(1);
    }

    @Test
    void deduplicatesByExternalIdAndSource_whenExternalIdPresent() {

        CardPriceProvider provider = mock(CardPriceProvider.class);

        when(editionResolver.resolve("Edge of Eternities")).thenReturn(RESOLVED_EOE);

        when(provider.search("Anticausal Vestige", RESOLVED_EOE))
                .thenReturn(List.of(
                        result("Card Kingdom", "Anticausal Vestige", "Edge of Eternities",
                                false, "mtgstocks:130307", 2.0),
                        result("Card Kingdom", "Anticausal Vestige", "Edge of Eternities",
                                false, "mtgstocks:130307", 2.0)));

        CardComparisonService service =
                new CardComparisonService(List.of(provider), historicalPriceService, editionResolver);

        List<CardPriceResult> results = service.compare("Anticausal Vestige", "Edge of Eternities");

        assertThat(results).hasSize(1);
    }

    @Test
    void treatsSameExternalIdFromDifferentSourcesAsDistinct() {

        CardPriceProvider provider = mock(CardPriceProvider.class);

        when(editionResolver.resolve("Edge of Eternities")).thenReturn(RESOLVED_EOE);

        when(provider.search("Anticausal Vestige", RESOLVED_EOE))
                .thenReturn(List.of(
                        result("Card Kingdom", "Anticausal Vestige", "Edge of Eternities",
                                false, "mtgstocks:130307", 2.0),
                        result("TCGPlayer", "Anticausal Vestige", "Edge of Eternities",
                                false, "mtgstocks:130307", 2.5)));

        CardComparisonService service =
                new CardComparisonService(List.of(provider), historicalPriceService, editionResolver);

        List<CardPriceResult> results = service.compare("Anticausal Vestige", "Edge of Eternities");

        assertThat(results).hasSize(2);
    }
}
