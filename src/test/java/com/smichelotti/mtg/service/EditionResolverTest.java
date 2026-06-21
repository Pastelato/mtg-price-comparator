package com.smichelotti.mtg.service;

import com.smichelotti.mtg.client.ScryfallClient;
import com.smichelotti.mtg.dto.ResolvedEdition;
import com.smichelotti.mtg.dto.ScryfallSetDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EditionResolverTest {

    private final ScryfallClient client = mock(ScryfallClient.class);

    private EditionResolver resolver;

    @BeforeEach
    void setUp() {

        ScryfallSetDto eoe = new ScryfallSetDto();
        eoe.setCode("eoe");
        eoe.setName("Edge of Eternities");

        when(client.getSets()).thenReturn(List.of(eoe));

        resolver = new EditionResolver(client);
    }

    @Test
    void resolvesSetCodeInput() {

        ResolvedEdition resolved = resolver.resolve("eoe");

        assertThat(resolved.setCode()).isEqualTo("eoe");
        assertThat(resolved.setName()).isEqualTo("Edge of Eternities");
    }

    @Test
    void resolvesSetNameInput() {

        ResolvedEdition resolved = resolver.resolve("Edge of Eternities");

        assertThat(resolved.setCode()).isEqualTo("eoe");
        assertThat(resolved.setName()).isEqualTo("Edge of Eternities");
    }

    @Test
    void resolvesCaseInsensitiveNameInput() {

        ResolvedEdition resolved = resolver.resolve("EDGE OF ETERNITIES");

        assertThat(resolved.setCode()).isEqualTo("eoe");
        assertThat(resolved.setName()).isEqualTo("Edge of Eternities");
    }

    @Test
    void resolvesCaseInsensitiveCodeInput() {

        ResolvedEdition resolved = resolver.resolve("EOE");

        assertThat(resolved.setCode()).isEqualTo("eoe");
        assertThat(resolved.setName()).isEqualTo("Edge of Eternities");
    }

    @Test
    void returnsNullFieldsForBlankInput() {

        ResolvedEdition resolved = resolver.resolve("   ");

        assertThat(resolved.setCode()).isNull();
        assertThat(resolved.setName()).isNull();
    }

    @Test
    void returnsNullFieldsForNullInput() {

        ResolvedEdition resolved = resolver.resolve(null);

        assertThat(resolved.setCode()).isNull();
        assertThat(resolved.setName()).isNull();
    }

    @Test
    void fallsBackToOriginalValueWhenUnmatched() {

        ResolvedEdition resolved = resolver.resolve("Some Unknown Set");

        assertThat(resolved.setCode()).isEqualTo("Some Unknown Set");
        assertThat(resolved.setName()).isEqualTo("Some Unknown Set");
    }
}
