package com.smichelotti.mtg.client;

import com.smichelotti.mtg.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;

@Component
public class ScryfallClient {

 private static final Logger log = LoggerFactory.getLogger(ScryfallClient.class);

 private final WebClient webClient;

 public ScryfallClient() {

  ExchangeStrategies strategies = ExchangeStrategies.builder()
    .codecs(configurer -> configurer.defaultCodecs()
      .maxInMemorySize(10 * 1024 * 1024))
    .build();

  this.webClient = WebClient.builder()
    .baseUrl("https://api.scryfall.com")
    .exchangeStrategies(strategies)
    .build();
 }

 @Cacheable(value = "cards", key = "#name")
 public ScryfallCardResponse searchCard(String name) {

  log.debug(">>> CALLING SCRYFALL API <<<");

  return webClient.get()
    .uri(uriBuilder -> uriBuilder
      .path("/cards/named")
      .queryParam("fuzzy", name)
      .build())
    .retrieve()
    .bodyToMono(ScryfallCardResponse.class)
    .block();
 }

 public List<String> autocomplete(String query) {

  ScryfallAutocompleteResponse response = webClient.get()
    .uri(uriBuilder -> uriBuilder
      .path("/cards/autocomplete")
      .queryParam("q", query)
      .build())
    .retrieve()
    .bodyToMono(ScryfallAutocompleteResponse.class)
    .block();

  return response.getData();
 }

 @Cacheable(value = "scryfallSets")
 public List<ScryfallSetDto> getSets() {

  ScryfallSetsResponse response = webClient.get()
    .uri("/sets")
    .retrieve()
    .bodyToMono(ScryfallSetsResponse.class)
    .block();

  return response.getData();
 }

 public List<ScryfallCardResponse> searchAllPrintings(
   String cardName) {

  log.debug("SEARCH ALL PRINTINGS: {}", cardName);

  ScryfallSearchResponse response =

    webClient.get()

      .uri(uriBuilder ->

      uriBuilder

        .path("/cards/search")

        .queryParam(
          "q",
          "name:\"" +
            cardName +
            "\" unique:prints")

        .build())

      .retrieve()

      .bodyToMono(
        ScryfallSearchResponse.class)

      .block();

  if (response == null ||
    response.getData() == null) {

   return List.of();
  }

  return response.getData();
 }

 @Cacheable(
   value = "cardsByEdition",
   key = "#cardName.toLowerCase() + '|' + #edition.toLowerCase()",
   unless = "#result == null")
 public ScryfallCardResponse searchCardByEdition(
   String cardName,
   String edition) {

  log.debug("SCRYFALL SEARCH: {} | {}", cardName, edition);

  ScryfallSearchResponse response =

    webClient.get()

      .uri(uriBuilder ->

      uriBuilder

        .path("/cards/search")

        .queryParam(
          "q",
          "!" + "\"" +
            cardName +
            "\"" +
            " set:" +
            edition)

        .build())

      .retrieve()

      .bodyToMono(
        ScryfallSearchResponse.class)

      .block();

  if (

  response == null ||

    response.getData() == null ||

    response.getData().isEmpty()) {

   return null;
  }

  return response.getData().get(0);
 }
}
