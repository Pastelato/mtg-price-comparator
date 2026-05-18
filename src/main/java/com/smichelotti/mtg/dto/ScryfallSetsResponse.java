package com.smichelotti.mtg.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScryfallSetsResponse {

    private List<ScryfallSetDto> data;
}
