package com.smichelotti.mtg.service;

import com.smichelotti.mtg.client.ScryfallClient;
import com.smichelotti.mtg.dto.EditionDto;
import com.smichelotti.mtg.dto.ScryfallCardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardEditionService {

    private final ScryfallClient client;

    public List<EditionDto> getEditions(
            String cardName) {

        List<ScryfallCardResponse> cards =

                client.searchAllPrintings(
                        cardName);

        return cards.stream()

                .map((ScryfallCardResponse card) ->

                EditionDto.builder()

                        .setName(
                                card.getSetName())

                        .setCode(
                                card.getSet())

                        .collectorNumber(
                                card.getCollectorNumber())

                        .imageUrl(

                                card.getImageUris() != null

                                        ?

                                        card.getImageUris()
                                                .getNormal()

                                        :

                                        null)

                        .build())

                .distinct()

                .toList();
    }
}
