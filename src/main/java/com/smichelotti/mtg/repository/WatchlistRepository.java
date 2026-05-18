package com.smichelotti.mtg.repository;

import com.smichelotti.mtg.entity.WatchlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository
                extends JpaRepository<WatchlistEntity, Long> {

        void deleteByCardName(
                        String cardName);
}
