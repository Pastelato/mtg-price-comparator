package com.smichelotti.mtg.provider;

import com.smichelotti.mtg.client.ScryfallClient;
import com.smichelotti.mtg.dto.CardPriceResult;
import com.smichelotti.mtg.dto.ResolvedEdition;
import com.smichelotti.mtg.dto.ScryfallCardResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScryfallPriceProvider
        implements CardPriceProvider {

    private static final Logger log = LoggerFactory.getLogger(ScryfallPriceProvider.class);

    private final ScryfallClient client;

    @Override
    public List<CardPriceResult> search(
            String cardName,
            ResolvedEdition edition) {

        log.debug("=== SCRYFALL PROVIDER ===");

        ScryfallCardResponse card;

        if (edition != null &&
                edition.setCode() != null &&
                !edition.setCode().isBlank()) {

            card = client.searchCardByEdition(
                    cardName,
                    edition.setCode());

        } else {

            card = client.searchCard(
                    cardName);
        }

        if (card == null) {

            log.warn("SCRYFALL PROVIDER: CARD IS NULL FOR cardName='{}'", cardName);

            return Collections.emptyList();
        }

        if (card.getPrices() == null) {

            log.warn("SCRYFALL PROVIDER: PRICES IS NULL FOR card='{}'", card.getName());

            return Collections.emptyList();
        }

        if (card.getPrices().getUsd() == null) {

            log.warn("SCRYFALL PROVIDER: USD PRICE IS NULL FOR card='{}'", card.getName());

            return Collections.emptyList();
        }

        return List.of(
                CardPriceResult.builder()
                        .source("Scryfall")
                        .cardName(card.getName())
                        .edition(card.getSetName())
                        .price(
                                new BigDecimal(
                                        card.getPrices().getUsd()))
                        .currency("USD")
                        .stock(null)
                        .productUrl("https://scryfall.com")
                        .build());
    }

    @Override
    public String getSourceName() {
        return "Scryfall";
    }
}
