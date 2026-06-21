package com.smichelotti.mtg.service;

import com.smichelotti.mtg.dto.CardPriceResult;
import com.smichelotti.mtg.dto.ResolvedEdition;
import com.smichelotti.mtg.provider.CardPriceProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class CardComparisonService {

        private static final Logger log = LoggerFactory.getLogger(CardComparisonService.class);

        private final List<CardPriceProvider> providers;
        private final HistoricalPriceService historicalPriceService;
        private final EditionResolver editionResolver;

        public List<CardPriceResult> compare(

                        String cardName,

                        String edition) {

                log.info("COMPARE REQUEST -> card='{}' edition='{}'", cardName, edition);

                ResolvedEdition resolvedEdition = editionResolver.resolve(edition);

                log.info("COMPARE RESOLVED -> card='{}' code='{}' name='{}'",
                                cardName,
                                resolvedEdition == null ? null : resolvedEdition.setCode(),
                                resolvedEdition == null ? null : resolvedEdition.setName());

                List<CompletableFuture<List<CardPriceResult>>>

                futures =

                                providers.stream()

                                                .map(provider ->

                                                CompletableFuture
                                                                .supplyAsync(() -> {

                                                                        log.debug(
                                                                                        "RUNNING PROVIDER: {}",
                                                                                        provider.getSourceName());

                                                                        List<CardPriceResult> result = provider.search(
                                                                                        cardName, resolvedEdition);

                                                                        log.debug(
                                                                                        "{} RETURNED {} RESULTS",
                                                                                        provider.getSourceName(),
                                                                                        result.size());

                                                                        return result;
                                                                }))
                                                .toList();

                List<CardPriceResult> results = futures.stream()
                                .flatMap(future -> {
                                        try {
                                                return future
                                                                .get()
                                                                .stream();
                                        } catch (InterruptedException | ExecutionException e) {
                                                log.error("PROVIDER EXECUTION FAILED", e);
                                                return List.<CardPriceResult>of()
                                                                .stream();
                                        }
                                })
                                .toList();

                List<CardPriceResult> uniqueResults = new ArrayList<>();

                for (CardPriceResult result : results) {
                        boolean alreadyExists = uniqueResults.stream()
                                        .anyMatch(existing -> (existing.getExternalId() != null
                                                        && existing.getExternalId()
                                                                        .equals(result.getExternalId())
                                                        && existing.getSource()
                                                                        .equalsIgnoreCase(
                                                                                        result.getSource()))
                                                        ||
                                                        (existing.getSource()
                                                                        .equalsIgnoreCase(
                                                                                        result.getSource())
                                                                        &&
                                                                        existing.getCardName()
                                                                                        .equalsIgnoreCase(
                                                                                                        result.getCardName())
                                                                        &&
                                                                        Objects.equals(
                                                                                        existing.getEdition(),
                                                                                        result.getEdition())
                                                                        &&
                                                                        Objects.equals(
                                                                                        existing.getFoil(),
                                                                                        result.getFoil())));

                        if (!alreadyExists) {
                                uniqueResults.add(result);
                        }
                }
                uniqueResults.sort((a, b) -> a.getPrice().compareTo(b.getPrice()));
                return uniqueResults;
        }
}
