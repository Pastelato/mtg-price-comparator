package com.smichelotti.mtg.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smichelotti.mtg.config.MtgStocksProperties;
import com.smichelotti.mtg.dto.MtgStocksPrintDto;
import com.smichelotti.mtg.dto.MtgStocksSetLinkDto;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Isolated in its own Spring bean so @Cacheable actually applies: calling these methods
// from within MtgStocksProvider itself would bypass the caching proxy (self-invocation).
@Component
@RequiredArgsConstructor
public class MtgStocksScraper {

    private static final Logger log = LoggerFactory.getLogger(MtgStocksScraper.class);

    private final MtgStocksProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Cacheable(value = "mtgstocksSetsIndex", unless = "#result == null || #result.isEmpty()")
    public List<MtgStocksSetLinkDto> fetchSetsIndex() throws IOException {

        log.debug(">>> CALLING MTGSTOCKS SETS INDEX <<<");

        Document setsDocument = Jsoup.connect(properties.setsUrl())
                .userAgent(properties.setsIndex().userAgent())
                .timeout(properties.setsIndex().timeoutMs())
                .followRedirects(properties.setsIndex().followRedirects())
                .get();

        Elements links = setsDocument.select("a");

        List<MtgStocksSetLinkDto> result = new ArrayList<>();

        for (Element link : links) {

            String text = link.text();

            if (!text.isBlank()) {
                result.add(new MtgStocksSetLinkDto(text, link.attr("href")));
            }
        }

        return result;
    }

    @Cacheable(value = "mtgstocksSetDetail", key = "#setUrl", unless = "#result == null || #result.isEmpty()")
    public List<MtgStocksPrintDto> fetchSetDetail(String setUrl) throws IOException {

        log.debug(">>> CALLING MTGSTOCKS SET DETAIL <<<");

        long start = System.currentTimeMillis();

        Document setDocument = Jsoup.connect(setUrl)
                .userAgent(properties.setDetail().userAgent())
                .timeout(properties.setDetail().timeoutMs())
                .followRedirects(properties.setDetail().followRedirects())
                .get();

        long elapsed = System.currentTimeMillis() - start;

        log.debug("PAGE LOADED IN: {} ms | title='{}' | htmlSize={}",
                elapsed, setDocument.title(), setDocument.outerHtml().length());

        if (properties.debugJson()) {
            String html = setDocument.outerHtml();
            log.debug(html.substring(0, Math.min(2000, html.length())));
        }

        Element jsonElement = setDocument.selectFirst("#ng-state");

        if (jsonElement == null) {
            log.warn("NG STATE NOT FOUND");
            return List.of();
        }

        JsonNode root = objectMapper.readTree(jsonElement.html());

        JsonNode printsNode = null;

        Iterator<JsonNode> values = root.elements();

        while (values.hasNext()) {

            JsonNode node = values.next();

            JsonNode prints = node.path("b").path("prints");

            if (prints.isArray()) {
                printsNode = prints;
                break;
            }
        }

        if (printsNode == null) {
            log.warn("PRINTS NODE NOT FOUND");
            return List.of();
        }

        log.debug("TOTAL PRINTS: {}", printsNode.size());

        List<MtgStocksPrintDto> result = new ArrayList<>();

        for (JsonNode card : printsNode) {

            if (properties.debugJson()) {
                log.debug(card.toPrettyString());
            }

            result.add(new MtgStocksPrintDto(
                    card.path("id").asText(),
                    card.path("name").asText(),
                    card.path("foil").asBoolean(false),
                    card.path("cardkingdom").path("latestPrice").path("avg").asDouble(0.0),
                    card.path("tcgplayer").path("latestPrice").path("avg").asDouble(0.0)));
        }

        return result;
    }
}
