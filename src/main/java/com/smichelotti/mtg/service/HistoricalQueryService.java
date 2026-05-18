package com.smichelotti.mtg.service;

import com.smichelotti.mtg.dto.HistoricalPriceDto;
import com.smichelotti.mtg.entity.CardPriceHistoryEntity;
import com.smichelotti.mtg.repository.CardPriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoricalQueryService {

        private final CardPriceHistoryRepository repository;

        public List<HistoricalPriceDto> getHistory(
                        String cardName) {

                List<CardPriceHistoryEntity> entities = repository.findByCardNameIgnoreCaseOrderByCapturedAtAsc(
                                cardName);

                return entities.stream()
                                .map(entity -> HistoricalPriceDto.builder()
                                                .price(entity.getPrice())
                                                .provider(entity.getProvider())
                                                .capturedAt(entity.getCapturedAt())
                                                .build())
                                .toList();
        }
}
