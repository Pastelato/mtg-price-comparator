package com.smichelotti.mtg.dto;

public record MtgStocksPrintDto(
        String id,
        String name,
        boolean foil,
        double cardKingdomAvg,
        double tcgplayerAvg) {
}
