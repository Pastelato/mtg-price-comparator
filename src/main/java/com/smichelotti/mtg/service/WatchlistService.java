package com.smichelotti.mtg.service;

import com.smichelotti.mtg.entity.WatchlistEntity;
import com.smichelotti.mtg.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository repository;

    public void addCard(
            String cardName,
            String edition) {

        WatchlistEntity entity = new WatchlistEntity();

        entity.setCardName(cardName);
        entity.setEdition(edition);

        entity.setCreatedAt(
                LocalDateTime.now());

        repository.save(entity);
    }
}
