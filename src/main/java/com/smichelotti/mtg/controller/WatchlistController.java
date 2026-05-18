package com.smichelotti.mtg.controller;

import com.smichelotti.mtg.entity.WatchlistEntity;
import com.smichelotti.mtg.repository.WatchlistRepository;
import com.smichelotti.mtg.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/watchlist")
public class WatchlistController {

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
    public void deleteCard(

            @RequestParam String cardName) {

        repository.deleteByCardName(
                cardName);
    }

}
