package com.smichelotti.mtg.service;

import com.smichelotti.mtg.dto.CardPriceResult;
import com.smichelotti.mtg.provider.CardPriceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class CardComparisonService {

        private final List<CardPriceProvider> providers;
        private final HistoricalPriceService historicalPriceService;

        public List<CardPriceResult> compare(

                        String cardName,

                        String edition) {

                List<CompletableFuture<List<CardPriceResult>>>

                futures =

                                providers.stream()

                                                .map(provider ->

                                                CompletableFuture
                                                                .supplyAsync(

                                                                                () -> {

                                                                                        System.out.println(

                                                                                                        "RUNNING PROVIDER: "
                                                                                                                        +

                                                                                                                        provider
                                                                                                                                        .getSourceName());

                                                                                        return provider.search(
                                                                                                        cardName,
                                                                                                        edition);
                                                                                }))

                                                .toList();

                List<CardPriceResult> results =

                                futures.stream()

                                                .flatMap(future -> {

                                                        try {

                                                                return future
                                                                                .get()
                                                                                .stream();

                                                        } catch (
                                                                        InterruptedException |

                                                                        ExecutionException e) {

                                                                e.printStackTrace();

                                                                return List
                                                                                .<CardPriceResult>of()

                                                                                .stream();
                                                        }
                                                })

                                                .toList();

                results.forEach(

                                historicalPriceService::save);

                List<CardPriceResult> uniqueResults =

                                new ArrayList<>();

                for (CardPriceResult result : results) {

                        boolean alreadyExists =

                                        uniqueResults.stream()

                                                        .anyMatch(existing ->

                                                        existing.getSource()
                                                                        .equalsIgnoreCase(
                                                                                        result.getSource())
                                                                        &&
                                                                        existing.getCardName()
                                                                                        .equalsIgnoreCase(
                                                                                                        result.getCardName())
                                                                        &&
                                                                        existing.getEdition()
                                                                                        .equalsIgnoreCase(
                                                                                                        result.getEdition()));

                        if (!alreadyExists) {

                                uniqueResults.add(
                                                result);
                        }
                }

                uniqueResults.sort(

                                (a, b) ->

                                a.getPrice()
                                                .compareTo(
                                                                b.getPrice()));

                return uniqueResults;
        }
}
