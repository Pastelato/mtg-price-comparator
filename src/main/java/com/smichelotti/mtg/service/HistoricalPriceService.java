package com.smichelotti.mtg.service;

import com.smichelotti.mtg.dto.CardPriceResult;
import com.smichelotti.mtg.entity.CardPriceHistoryEntity;
import com.smichelotti.mtg.repository.CardPriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HistoricalPriceService {

    private final CardPriceHistoryRepository repository;

    public void save(CardPriceResult result) {

        CardPriceHistoryEntity entity = new CardPriceHistoryEntity();

        entity.setCardName(result.getCardName());
        entity.setEdition(result.getEdition());
        entity.setProvider(result.getSource());
        entity.setPrice(result.getPrice());
        entity.setCurrency(result.getCurrency());

        entity.setCapturedAt(
                LocalDateTime.now());

        repository.save(entity);
    }
}
