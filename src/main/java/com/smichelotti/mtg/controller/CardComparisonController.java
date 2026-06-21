package com.smichelotti.mtg.controller;

import com.smichelotti.mtg.dto.CardPriceResult;
import com.smichelotti.mtg.dto.EditionDto;
import com.smichelotti.mtg.service.CardComparisonService;
import com.smichelotti.mtg.service.CardEditionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards")
public class CardComparisonController {

    private static final Logger log = LoggerFactory.getLogger(CardComparisonController.class);

    private final CardComparisonService comparisonService;

    private final CardEditionService editionService;

    @GetMapping("/compare")
    public List<CardPriceResult> compareCards(

            @RequestParam String name,

            @RequestParam(required = false) String edition) {

        log.info("CONTROLLER REQUEST -> card='{}' edition='{}'", name, edition);

        return comparisonService
                .compare(name, edition);
    }

    @GetMapping("/editions")
    public List<EditionDto> getEditions(

            @RequestParam String name) {

        return editionService
                .getEditions(name);
    }
}
