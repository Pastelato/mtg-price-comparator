package com.smichelotti.mtg.provider;

import com.smichelotti.mtg.config.MtgStocksProperties;
import com.smichelotti.mtg.dto.CardPriceResult;
import com.smichelotti.mtg.dto.MtgStocksPrintDto;
import com.smichelotti.mtg.dto.MtgStocksSetLinkDto;
import com.smichelotti.mtg.dto.ResolvedEdition;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MtgStocksProvider implements CardPriceProvider {

    private static final Logger log = LoggerFactory.getLogger(MtgStocksProvider.class);

    private static final Pattern VARIANT_PATTERN = Pattern.compile("\\((.*?)\\)");

    private final MtgStocksProperties properties;

    private final MtgStocksScraper scraper;

    private final EditionNameNormalizer editionNameNormalizer = new EditionNameNormalizer();

    @Override
    public List<CardPriceResult> search(String cardName, ResolvedEdition resolvedEdition) {
        log.debug(
                "MTGSTOCKS INPUT -> card='{}' code='{}' name='{}'",
                cardName,
                resolvedEdition == null ? null : resolvedEdition.setCode(),
                resolvedEdition == null ? null : resolvedEdition.setName());
        List<CardPriceResult> results = new ArrayList<>();

        if (resolvedEdition == null
                || resolvedEdition.setName() == null
                || resolvedEdition.setName().isBlank()) {
            log.warn("MTGSTOCKS EARLY EXIT -> edition name missing");
            return results;
        }

        String edition = resolvedEdition.setName();

        try {
            log.debug("MTGSTOCKS SEARCH: {} | {}", cardName, edition);

            List<MtgStocksSetLinkDto> links = scraper.fetchSetsIndex();

            List<String> candidates = editionNameNormalizer.candidatesFor(resolvedEdition);

            log.debug("MTGSTOCKS CANDIDATES -> {}", candidates);

            String setUrl = null;
            String matchedCandidate = null;

            for (String candidate : candidates) {

                for (MtgStocksSetLinkDto link : links) {

                    if (link.text().equalsIgnoreCase(candidate)) {
                        setUrl = properties.baseUrl() + link.href();
                        matchedCandidate = candidate;
                        break;
                    }
                }

                if (setUrl != null) {
                    break;
                }
            }

            if (setUrl != null) {
                log.debug("MTGSTOCKS MATCHED CANDIDATE -> {}", matchedCandidate);
            }

            if (setUrl == null) {

                log.warn("SET URL NOT FOUND");
                log.warn("REQUESTED EDITION: {}", edition);

                int count = 0;

                for (MtgStocksSetLinkDto link : links) {

                    log.debug("SET FOUND: {}", link.text());

                    count++;

                    if (count >= 50) {
                        break;
                    }
                }

                log.info("MTGSTOCKS RETURNED {} RESULTS", results.size());
                return results;
            }

            log.debug("SET URL FOUND: {}", setUrl);

            List<MtgStocksPrintDto> prints = scraper.fetchSetDetail(setUrl);

            int matchesFound = 0;

            for (MtgStocksPrintDto card : prints) {

                String foundName = card.name();

                if (properties.debugCards()) {
                    log.debug("CARD: {}", foundName);
                }

                boolean foil = card.foil();

                log.debug("CHECKING PRINT -> found='{}' target='{}'", foundName, cardName);

                if (!normalizeCardName(foundName).equals(normalizeCardName(cardName))) {
                    continue;
                }

                matchesFound++;

                log.debug("MATCH FOUND -> {}", foundName);

                if (properties.debugCards()) {
                    log.debug("MATCH FOUND: {}", foundName);
                }

                double ckPrice = card.cardKingdomAvg();

                double tcgPrice = card.tcgplayerAvg();

                String productUrl = properties.printUrl(card.id());

                String externalId = "mtgstocks:" + card.id();

                String variant = extractVariant(foundName);

                if (ckPrice > 0) {

                    results.add(CardPriceResult.builder()
                            .source("Card Kingdom")
                            .cardName(foundName)
                            .edition(edition)
                            .price(BigDecimal.valueOf(ckPrice))
                            .currency("USD")
                            .productUrl(productUrl)
                            .foil(foil)
                            .variant(variant)
                            .externalId(externalId)
                            .build());
                }

                if (tcgPrice > 0) {

                    results.add(CardPriceResult.builder()
                            .source("TCGPlayer")
                            .cardName(foundName)
                            .edition(edition)
                            .price(BigDecimal.valueOf(tcgPrice))
                            .currency("USD")
                            .productUrl(productUrl)
                            .foil(foil)
                            .variant(variant)
                            .externalId(externalId)
                            .build());
                }
            }

            log.info("TOTAL MATCHES FOUND -> {}", matchesFound);

            log.info("MTGSTOCKS RETURNED {} RESULTS", results.size());

            return results;

        } catch (Exception e) {

            log.error("MTGSTOCKS PROVIDER ERROR", e);

            return List.of();
        }
    }

    @Override
    public String getSourceName() {
        return "MTGStocks";
    }

    String normalizeCardName(String name) {

        return VARIANT_PATTERN.matcher(name)
                .replaceAll("")
                .trim()
                .toLowerCase()
                .replace('\u2019', '\'')
                .replaceAll("\\s+", " ");
    }

    String extractVariant(String name) {

        Matcher matcher = VARIANT_PATTERN.matcher(name);

        List<String> parts = new ArrayList<>();

        while (matcher.find()) {
            parts.add(matcher.group(1));
        }

        return parts.isEmpty() ? null : String.join(", ", parts);
    }
}
