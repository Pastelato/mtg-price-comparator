package com.smichelotti.mtg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScryfallCardResponse {

    private String id;

    private String name;

    @JsonProperty("set_name")
    private String setName;

    private String rarity;

    @JsonProperty("collector_number")
    private String collectorNumber;

    @JsonProperty("image_uris")
    private ImageUris imageUris;

    @JsonProperty("prices")
    private ScryfallPricesDto prices;

    @JsonProperty("set")
    private String set;

}
