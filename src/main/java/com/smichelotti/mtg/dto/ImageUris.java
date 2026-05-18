package com.smichelotti.mtg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ImageUris {

    @JsonProperty("normal")
    private String normal;
}
