package com.smichelotti.mtg.controller;

import com.smichelotti.mtg.dto.HistoricalPriceDto;
import com.smichelotti.mtg.service.HistoricalQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HistoricalPriceController {

    private final HistoricalQueryService service;

    @GetMapping("/api/cards/history")
    public List<HistoricalPriceDto> history(
            @RequestParam String name) {

        return service.getHistory(name);
    }
}
