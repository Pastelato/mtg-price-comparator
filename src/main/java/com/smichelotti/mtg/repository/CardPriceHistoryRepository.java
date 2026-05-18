package com.smichelotti.mtg.repository;

import com.smichelotti.mtg.entity.CardPriceHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardPriceHistoryRepository
                extends JpaRepository<CardPriceHistoryEntity, Long> {

        List<CardPriceHistoryEntity> findByCardNameIgnoreCaseOrderByCapturedAtAsc(
                        String cardName);
}
