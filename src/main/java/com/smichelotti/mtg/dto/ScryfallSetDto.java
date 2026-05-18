package com.smichelotti.mtg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScryfallSetDto {

    private String id;

    private String code;

    private String name;

    @JsonProperty("released_at")
    private String releasedAt;
}
