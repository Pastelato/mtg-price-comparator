package com.smichelotti.mtg.service;

import com.smichelotti.mtg.client.ScryfallClient;
import com.smichelotti.mtg.dto.ResolvedEdition;
import com.smichelotti.mtg.dto.ScryfallSetDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EditionResolver {

    private static final Logger log = LoggerFactory.getLogger(EditionResolver.class);

    private final ScryfallClient client;

    public ResolvedEdition resolve(String rawEdition) {

        if (rawEdition == null || rawEdition.isBlank()) {
            return new ResolvedEdition(rawEdition, null, null);
        }

        String trimmed = rawEdition.trim();

        ScryfallSetDto match = findSet(trimmed);

        if (match == null) {

            log.warn("EDITION RESOLVER: NO MATCH FOR '{}', PASSING THROUGH UNRESOLVED", rawEdition);

            return new ResolvedEdition(rawEdition, rawEdition, rawEdition);
        }

        ResolvedEdition resolved = new ResolvedEdition(rawEdition, match.getCode(), match.getName());

        log.info("EDITION RESOLVER: original='{}' setCode='{}' setName='{}'",
                resolved.original(), resolved.setCode(), resolved.setName());

        return resolved;
    }

    private ScryfallSetDto findSet(String value) {

        List<ScryfallSetDto> sets = fetchSets();

        for (ScryfallSetDto set : sets) {
            if (set.getCode() != null && set.getCode().equalsIgnoreCase(value)) {
                return set;
            }
        }

        for (ScryfallSetDto set : sets) {
            if (set.getName() != null && set.getName().equalsIgnoreCase(value)) {
                return set;
            }
        }

        return null;
    }

    private List<ScryfallSetDto> fetchSets() {

        try {
            return client.getSets();
        } catch (Exception e) {
            log.error("EDITION RESOLVER: FAILED TO FETCH SCRYFALL SETS", e);
            return List.of();
        }
    }
}
