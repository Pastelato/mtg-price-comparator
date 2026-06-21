package com.smichelotti.mtg.provider;

import com.smichelotti.mtg.dto.ResolvedEdition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EditionNameNormalizerTest {

    private final EditionNameNormalizer normalizer = new EditionNameNormalizer();

    @Test
    void candidatesFor_includesOriginalNameFirst() {

        ResolvedEdition edition = new ResolvedEdition("Bloomburrow", "blb", "Bloomburrow");

        assertThat(normalizer.candidatesFor(edition)).startsWith("Bloomburrow");
    }

    @Test
    void candidatesFor_commanderSuffixGeneratesPrefixCandidate() {

        ResolvedEdition edition = new ResolvedEdition(
                "Lorwyn Eclipsed Commander", "ecl", "Lorwyn Eclipsed Commander");

        assertThat(normalizer.candidatesFor(edition))
                .contains("Commander: Lorwyn Eclipsed");
    }

    @Test
    void candidatesFor_artSeriesSuffixGeneratesPrefixCandidate() {

        ResolvedEdition edition = new ResolvedEdition(
                "Edge of Eternities Art Series", "eos", "Edge of Eternities Art Series");

        assertThat(normalizer.candidatesFor(edition))
                .contains("Art Series: Edge of Eternities");
    }

    @Test
    void candidatesFor_planesTrailingQualifierIsDropped() {

        ResolvedEdition edition = new ResolvedEdition(
                "Planechase 2012 Planes", "pc2", "Planechase 2012 Planes");

        assertThat(normalizer.candidatesFor(edition))
                .contains("Planechase 2012");
    }

    @Test
    void candidatesFor_schemesTrailingQualifierIsDropped() {

        ResolvedEdition edition = new ResolvedEdition("Archenemy Schemes", "arc", "Archenemy Schemes");

        assertThat(normalizer.candidatesFor(edition))
                .contains("Archenemy");
    }

    @Test
    void candidatesFor_codeSuffixCandidateUsesScryfallCode() {

        ResolvedEdition edition = new ResolvedEdition("Magic 2015", "m15", "Magic 2015");

        assertThat(normalizer.candidatesFor(edition))
                .contains("Magic 2015 (M15)");
    }

    @Test
    void candidatesFor_hasNoDuplicates() {

        ResolvedEdition edition = new ResolvedEdition("Bloomburrow", "blb", "Bloomburrow");

        List<String> candidates = normalizer.candidatesFor(edition);

        assertThat(candidates).doesNotHaveDuplicates();
    }

    @Test
    void candidatesFor_ordersByConfidence() {

        ResolvedEdition edition = new ResolvedEdition(
                "Lorwyn Eclipsed Commander", "ecl", "Lorwyn Eclipsed Commander");

        assertThat(normalizer.candidatesFor(edition))
                .containsExactly(
                        "Lorwyn Eclipsed Commander",
                        "Commander: Lorwyn Eclipsed",
                        "Lorwyn Eclipsed Commander (ECL)");
    }
}
