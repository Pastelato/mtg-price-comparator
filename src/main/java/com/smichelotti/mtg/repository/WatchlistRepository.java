package com.smichelotti.mtg.repository;

import com.smichelotti.mtg.entity.WatchlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository
                extends JpaRepository<WatchlistEntity, Long> {

        long deleteByCardName(
                        String cardName);

        boolean existsByCardNameAndEdition(
                        String cardName,
                        String edition);

        boolean existsByCardNameAndEditionIsNull(
                        String cardName);
}
