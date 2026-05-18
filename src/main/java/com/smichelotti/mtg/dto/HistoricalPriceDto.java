package com.smichelotti.mtg.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class HistoricalPriceDto {

    private BigDecimal price;

    private String provider;

    private LocalDateTime capturedAt;
}
