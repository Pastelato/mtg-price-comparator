package com.smichelotti.mtg.provider;

import com.smichelotti.mtg.dto.CardPriceResult;
import com.smichelotti.mtg.dto.ResolvedEdition;

import java.util.List;

public interface CardPriceProvider {

    List<CardPriceResult> search(
            String cardName,
            ResolvedEdition edition);

    String getSourceName();
}
