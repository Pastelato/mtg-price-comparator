package com.smichelotti.mtg.service;

import com.smichelotti.mtg.entity.CardSearchEntity;
import com.smichelotti.mtg.repository.CardSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CardSearchPersistenceService {

    private final CardSearchRepository repository;

    public void saveSearch(String cardName, String edition) {

        CardSearchEntity entity = new CardSearchEntity();

        entity.setCardName(cardName);
        entity.setEdition(edition);
        entity.setSearchedAt(LocalDateTime.now());

        repository.save(entity);
    }
}
