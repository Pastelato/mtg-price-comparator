package com.smichelotti.mtg.provider;

import com.smichelotti.mtg.client.ScryfallClient;
import com.smichelotti.mtg.dto.CardPriceResult;
import com.smichelotti.mtg.dto.ScryfallCardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScryfallPriceProvider
        implements CardPriceProvider {

    private final ScryfallClient client;

    @Override
    public List<CardPriceResult> search(
            String cardName,
            String edition) {

        System.out.println("=== SCRYFALL PROVIDER ===");

        ScryfallCardResponse card;

        if (edition != null &&
                !edition.isBlank()) {

            card = client.searchCardByEdition(
                    cardName,
                    edition);

        } else {

            card = client.searchCard(
                    cardName);
        }

        if (card == null) {

            System.out.println("CARD IS NULL");

            return Collections.emptyList();
        }

        if (card.getPrices() == null) {

            System.out.println("PRICES IS NULL");

            return Collections.emptyList();
        }

        if (card.getPrices().getUsd() == null) {

            System.out.println("USD PRICE IS NULL");

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
