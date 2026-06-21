package com.smichelotti.mtg.service;

import com.smichelotti.mtg.entity.WatchlistEntity;
import com.smichelotti.mtg.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistService.class);

    private final WatchlistRepository repository;

    public void addCard(
            String cardName,
            String edition) {

        if (alreadyExists(cardName, edition)) {
            log.info("WATCHLIST ADD SKIPPED (already exists) cardName={} edition={}", cardName, edition);
            return;
        }

        WatchlistEntity entity = new WatchlistEntity();

        entity.setCardName(cardName);
        entity.setEdition(edition);

        entity.setCreatedAt(
                LocalDateTime.now());

        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // Unique index (card_name, COALESCE(edition,'')) caught a concurrent
            // insert that raced past the existsBy check above. The other request
            // already persisted the row, so this one is treated as an idempotent no-op.
            log.warn("WATCHLIST ADD SKIPPED (race lost on unique constraint) cardName={} edition={}", cardName, edition);
        }
    }

    private boolean alreadyExists(String cardName, String edition) {

        return edition == null
                ? repository.existsByCardNameAndEditionIsNull(cardName)
                : repository.existsByCardNameAndEdition(cardName, edition);
    }

    @Transactional
    public long deleteCard(String cardName) {

        return repository.deleteByCardName(cardName);
    }
}
