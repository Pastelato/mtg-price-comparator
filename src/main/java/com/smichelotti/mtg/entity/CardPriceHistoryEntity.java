package com.smichelotti.mtg.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_price_history")
@Data
public class CardPriceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cardName;

    private String edition;

    private String provider;

    private BigDecimal price;

    private String currency;

    private LocalDateTime capturedAt;
}
