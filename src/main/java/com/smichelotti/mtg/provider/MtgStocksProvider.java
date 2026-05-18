package com.smichelotti.mtg.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smichelotti.mtg.dto.CardPriceResult;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MtgStocksProvider
        implements CardPriceProvider {

    @Override
    public List<CardPriceResult> search(

            String cardName,

            String edition) {

        List<CardPriceResult> results = new ArrayList<>();

        if (

        edition == null ||

                edition.isBlank()) {

            return results;
        }

        try {

            System.out.println(
                    "MTGSTOCKS SEARCH: " +
                            cardName +
                            " | " +
                            edition);

            String setsUrl = "https://www.mtgstocks.com/sets";

            Document setsDocument =

                    Jsoup.connect(
                            setsUrl)

                            .userAgent(
                                    "Mozilla/5.0")

                            .timeout(10000)

                            .get();

            String setUrl = null;

            for (Element link : setsDocument.select("a")) {

                String text = link.text();

                if (

                text.equalsIgnoreCase(
                        edition)) {

                    setUrl = "https://www.mtgstocks.com" +

                            link.attr(
                                    "href");

                    break;
                }
            }

            if (setUrl == null) {

                System.out.println(
                        "SET URL NOT FOUND");

                return results;
            }

            System.out.println(
                    "SET URL: " +
                            setUrl);

            Document setDocument =

                    Jsoup.connect(
                            setUrl)

                            .userAgent(
                                    "Mozilla/5.0")

                            .timeout(10000)

                            .get();

            Element jsonElement =

                    setDocument.selectFirst(
                            "#ng-state");

            if (jsonElement == null) {

                System.out.println(
                        "NG STATE NOT FOUND");

                return results;
            }

            String json = jsonElement.html();

            ObjectMapper mapper = new ObjectMapper();

            JsonNode root = mapper.readTree(json);

            JsonNode printsNode = null;

            Iterator<JsonNode> values = root.elements();

            while (values.hasNext()) {

                JsonNode node = values.next();

                JsonNode prints = node.path("b")
                        .path("prints");

                if (prints.isArray()) {

                    printsNode = prints;

                    break;
                }
            }

            if (printsNode == null) {

                System.out.println(
                        "PRINTS NODE NOT FOUND");

                return results;
            }

            System.out.println(
                    "TOTAL PRINTS: " +
                            printsNode.size());

            for (JsonNode card : printsNode) {

                String foundName =

                        card.path("name")
                                .asText();
                boolean foil =

                        card.path("foil")
                                .asBoolean(false);
                if (

                !foundName

                        .toLowerCase()

                        .contains(

                                cardName
                                        .toLowerCase())) {

                    continue;
                }

                System.out.println(
                        "MATCH FOUND: " +
                                foundName);

                double ckPrice =

                        card.path("cardkingdom")
                                .path("latestPrice")
                                .path("avg")
                                .asDouble(0.0);

                double tcgPrice =

                        card.path("tcgplayer")
                                .path("latestPrice")
                                .path("avg")
                                .asDouble(0.0);

                String productUrl =

                        "https://www.mtgstocks.com/prints/" +

                                card.path("id")
                                        .asText();

                if (ckPrice > 0) {

                    results.add(

                            CardPriceResult.builder()

                                    .source(
                                            "Card Kingdom")

                                    .cardName(
                                            foundName)

                                    .edition(
                                            edition)

                                    .price(
                                            BigDecimal.valueOf(
                                                    ckPrice))

                                    .currency(
                                            "USD")

                                    .productUrl(
                                            productUrl)
                                    .foil(foil)
                                    .build());
                }

                if (tcgPrice > 0) {

                    results.add(

                            CardPriceResult.builder()

                                    .source(
                                            "TCGPlayer")

                                    .cardName(
                                            foundName)

                                    .edition(
                                            edition)

                                    .price(
                                            BigDecimal.valueOf(
                                                    tcgPrice))

                                    .currency(
                                            "USD")

                                    .productUrl(
                                            productUrl)

                                    .build());
                }
            }

            return results;

        } catch (Exception e) {

            e.printStackTrace();

            return List.of();
        }
    }

    @Override
    public String getSourceName() {

        return "MTGStocks";
    }
}
