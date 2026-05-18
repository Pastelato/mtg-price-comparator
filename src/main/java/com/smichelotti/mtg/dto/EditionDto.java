package com.smichelotti.mtg.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EditionDto {

    private String setName;

    private String setCode;

    private String collectorNumber;

    private String imageUrl;
}
