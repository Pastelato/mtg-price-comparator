package com.smichelotti.mtg.controller;

import com.smichelotti.mtg.entity.WatchlistEntity;
import com.smichelotti.mtg.repository.WatchlistRepository;
import com.smichelotti.mtg.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private static final Logger log = LoggerFactory.getLogger(WatchlistController.class);

    private final WatchlistService service;

    private final WatchlistRepository repository;

    @PostMapping
    public void addCard(

            @RequestParam String cardName,

            @RequestParam(required = false) String edition) {

        service.addCard(
                cardName,
                edition);
    }

    @GetMapping
    public List<WatchlistEntity> getAll() {

        return repository.findAll();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteCard(

            @RequestParam String cardName) {

        long rowsDeleted = service.deleteCard(cardName);

        log.info("WATCHLIST DELETE cardName={} rowsDeleted={}", cardName, rowsDeleted);

        return ResponseEntity.noContent().build();
    }

}
