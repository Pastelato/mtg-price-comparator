package com.smichelotti.mtg.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CardPriceResult {

    private String source;

    private String cardName;

    private String edition;

    private BigDecimal price;

    private String currency;

    private Integer stock;

    private String productUrl;

    private Boolean foil;

    private String variant;

    private String externalId;
}
