package com.smichelotti.mtg.provider;

import com.smichelotti.mtg.dto.CardPriceResult;

import java.util.List;

public interface CardPriceProvider {

    List<CardPriceResult> search(
            String cardName,
            String edition);

    String getSourceName();
}
