package com.smichelotti.mtg.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_searches")
@Data
public class CardSearchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cardName;

    private String edition;

    private LocalDateTime searchedAt;
}
