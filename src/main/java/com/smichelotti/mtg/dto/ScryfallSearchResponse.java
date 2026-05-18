package com.smichelotti.mtg.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScryfallSearchResponse {

    private List<ScryfallCardResponse> data;
}
