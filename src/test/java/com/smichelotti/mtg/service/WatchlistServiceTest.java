package com.smichelotti.mtg.service;

import com.smichelotti.mtg.entity.WatchlistEntity;
import com.smichelotti.mtg.repository.WatchlistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WatchlistServiceTest {

    private final WatchlistRepository repository = mock(WatchlistRepository.class);

    private final WatchlistService service = new WatchlistService(repository);

    @Test
    void addCard_casoA_firstInsertion_savesNewRow() {

        when(repository.existsByCardNameAndEdition("Lymph Sliver", "Fallen Empires"))
                .thenReturn(false);

        service.addCard("Lymph Sliver", "Fallen Empires");

        verify(repository).save(any(WatchlistEntity.class));
    }

    @Test
    void addCard_casoB_duplicateSameCardNameAndEdition_doesNotInsertAndDoesNotThrow() {

        when(repository.existsByCardNameAndEdition("The Unagi of Kyoshi Island", "Avatar: The Last Airbender"))
                .thenReturn(true);

        assertThatCode(() -> service.addCard("The Unagi of Kyoshi Island", "Avatar: The Last Airbender"))
                .doesNotThrowAnyException();

        verify(repository, never()).save(any(WatchlistEntity.class));
    }

    @Test
    void addCard_casoC_sameCardDifferentEdition_savesNewRow() {

        when(repository.existsByCardNameAndEdition("The Unagi of Kyoshi Island", "Avatar: The Last Airbender"))
                .thenReturn(true);

        when(repository.existsByCardNameAndEdition("The Unagi of Kyoshi Island", "Promo Edition"))
                .thenReturn(false);

        service.addCard("The Unagi of Kyoshi Island", "Promo Edition");

        verify(repository).save(any(WatchlistEntity.class));
    }

    @Test
    void addCard_casoD_nullEdition_usesEditionIsNullLookup_andSkipsIfAlreadyPresent() {

        when(repository.existsByCardNameAndEditionIsNull("Joo Dee, One of Many"))
                .thenReturn(true);

        service.addCard("Joo Dee, One of Many", null);

        verify(repository, never()).save(any(WatchlistEntity.class));
        verify(repository, never()).existsByCardNameAndEdition(any(), any());
    }

    @Test
    void addCard_casoE_concurrentRequestsRaceConstraint_swallowsDataIntegrityViolation() {

        when(repository.existsByCardNameAndEdition("Lymph Sliver", "Fallen Empires"))
                .thenReturn(false);

        when(repository.save(any(WatchlistEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatCode(() -> service.addCard("Lymph Sliver", "Fallen Empires"))
                .doesNotThrowAnyException();
    }

    @Test
    void deleteCard_cardNameNotFound_returnsZeroRowsAndDoesNotThrow() {

        when(repository.deleteByCardName("Nonexistent Card XYZ")).thenReturn(0L);

        long rowsDeleted = service.deleteCard("Nonexistent Card XYZ");

        assertThat(rowsDeleted).isZero();
        verify(repository).deleteByCardName("Nonexistent Card XYZ");
    }

    @Test
    void deleteCard_singleMatchingRow_returnsOneRowDeleted() {

        when(repository.deleteByCardName("Lymph Sliver")).thenReturn(1L);

        long rowsDeleted = service.deleteCard("Lymph Sliver");

        assertThat(rowsDeleted).isEqualTo(1L);
        verify(repository).deleteByCardName("Lymph Sliver");
    }

    @Test
    void deleteCard_duplicateCardName_deletesAllMatchingRowsWithoutThrowing() {

        when(repository.deleteByCardName("Vaultguard Trooper")).thenReturn(2L);

        long rowsDeleted = service.deleteCard("Vaultguard Trooper");

        assertThat(rowsDeleted).isEqualTo(2L);
    }

    @Test
    void deleteCard_repositoryFailure_propagatesException() {

        when(repository.deleteByCardName("Broken Card"))
                .thenThrow(new RuntimeException("simulated failure"));

        assertThatCode(() -> service.deleteCard("Broken Card"))
                .isInstanceOf(RuntimeException.class);
    }
}
