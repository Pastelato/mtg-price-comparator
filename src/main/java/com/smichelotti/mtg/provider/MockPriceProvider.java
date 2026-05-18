package com.smichelotti.mtg.provider;

import com.smichelotti.mtg.dto.CardPriceResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Component
public class MockPriceProvider
        implements CardPriceProvider {

    private final Random random = new Random();

    @Override
    public List<CardPriceResult> search(
            String cardName,
            String edition) {

        double randomPrice = 2.0 + (5.0 - 2.0)
                * random.nextDouble();

        return List.of(

                CardPriceResult.builder()
                        .source("MockTrader")
                        .cardName(cardName)
                        .edition(edition)
                        .price(
                                BigDecimal.valueOf(
                                        randomPrice))
                        .currency("USD")
                        .stock(5)
                        .productUrl(
                                "https://mocktrader.com")
                        .build());
    }

    @Override
    public String getSourceName() {

        return "MockTrader";
    }
}
