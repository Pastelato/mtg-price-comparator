package com.smichelotti.mtg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScryfallPricesDto {

    private String usd;

    @JsonProperty("usd_foil")
    private String usdFoil;

    private String eur;

    @JsonProperty("eur_foil")
    private String eurFoil;
}
