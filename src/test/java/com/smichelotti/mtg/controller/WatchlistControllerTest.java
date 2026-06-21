package com.smichelotti.mtg.controller;

import com.smichelotti.mtg.repository.WatchlistRepository;
import com.smichelotti.mtg.service.WatchlistService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WatchlistControllerTest {

    private final WatchlistService service = mock(WatchlistService.class);

    private final WatchlistRepository repository = mock(WatchlistRepository.class);

    private final WatchlistController controller = new WatchlistController(service, repository);

    @Test
    void deleteCard_cardNameNotFound_returnsNoContent() {

        when(service.deleteCard("Nonexistent Card XYZ")).thenReturn(0L);

        ResponseEntity<Void> response = controller.deleteCard("Nonexistent Card XYZ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void deleteCard_singleMatchingRow_returnsNoContent() {

        when(service.deleteCard("Lymph Sliver")).thenReturn(1L);

        ResponseEntity<Void> response = controller.deleteCard("Lymph Sliver");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).deleteCard("Lymph Sliver");
    }

    @Test
    void deleteCard_duplicateCardName_returnsNoContent() {

        when(service.deleteCard("Vaultguard Trooper")).thenReturn(2L);

        ResponseEntity<Void> response = controller.deleteCard("Vaultguard Trooper");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteCard_delegatesToServiceRatherThanRepository() {

        when(service.deleteCard("Lymph Sliver")).thenReturn(1L);

        controller.deleteCard("Lymph Sliver");

        verify(service).deleteCard("Lymph Sliver");
    }
}
