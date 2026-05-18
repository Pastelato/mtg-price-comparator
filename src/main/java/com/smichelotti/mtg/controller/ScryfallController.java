package com.smichelotti.mtg.controller;

import com.smichelotti.mtg.client.ScryfallClient;
import com.smichelotti.mtg.dto.ScryfallCardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.smichelotti.mtg.dto.ScryfallSetDto;
import com.smichelotti.mtg.service.CardSearchPersistenceService;

@RestController
@RequiredArgsConstructor
public class ScryfallController {

    private final ScryfallClient scryfallClient;
    private final CardSearchPersistenceService persistenceService;

    @GetMapping("/api/cards/scryfall/search")
    public ScryfallCardResponse search(
            @RequestParam String name) {

        persistenceService.saveSearch(name, null);

        return scryfallClient.searchCard(name);
    }

    @GetMapping("/api/cards/autocomplete")
    public List<String> autocomplete(
            @RequestParam String q) {

        return scryfallClient.autocomplete(q);
    }

    @GetMapping("/api/sets")
    public List<ScryfallSetDto> getSets() {

        return scryfallClient.getSets();
    }
}
