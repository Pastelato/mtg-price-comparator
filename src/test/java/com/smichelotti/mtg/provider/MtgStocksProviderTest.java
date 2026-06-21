package com.smichelotti.mtg.provider;

import com.smichelotti.mtg.config.MtgStocksProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MtgStocksProviderTest {

    private final MtgStocksProvider provider =
            new MtgStocksProvider(mock(), new MtgStocksScraper(mock()));

    private static MtgStocksProperties mock() {
        return new MtgStocksProperties(
                "https://www.mtgstocks.com",
                "/sets",
                "/prints",
                new MtgStocksProperties.RequestConfig("Mozilla/5.0", 10000, true),
                new MtgStocksProperties.RequestConfig("Mozilla/5.0", 60000, true),
                false,
                false);
    }

    @ParameterizedTest
    @CsvSource({
            "Anticausal Vestige,Anticausal Vestige",
            "Anticausal Vestige (Showcase),Anticausal Vestige",
            "Anticausal Vestige (Extended Art),Anticausal Vestige",
            "Anticausal Vestige (Showcase) (Fracture Foil),Anticausal Vestige"
    })
    void normalizeCardName_matchesAllVariantsOfSameCard(String foundName, String searchTerm) {

        assertThat(provider.normalizeCardName(foundName))
                .isEqualTo(provider.normalizeCardName(searchTerm));
    }

    @ParameterizedTest
    @CsvSource({
            "Lightning Bolt,Bolt",
            "Angelic Skirmisher,Angel",
            "Shivan Dragon,Dragon",
            "Llanowar Elves,Elf"
    })
    void normalizeCardName_rejectsGenericSubstringFalsePositives(String foundName, String searchTerm) {

        assertThat(provider.normalizeCardName(foundName))
                .isNotEqualTo(provider.normalizeCardName(searchTerm));
    }

    @Test
    void normalizeCardName_handlesCurlyApostrophe() {

        assertThat(provider.normalizeCardName("Urza’s Tower"))
                .isEqualTo(provider.normalizeCardName("Urza's Tower"));
    }

    @Test
    void extractVariant_returnsNullForBasePrinting() {

        assertThat(provider.extractVariant("Anticausal Vestige")).isNull();
    }

    @Test
    void extractVariant_returnsSingleVariant() {

        assertThat(provider.extractVariant("Anticausal Vestige (Showcase)"))
                .isEqualTo("Showcase");
    }

    @Test
    void extractVariant_returnsJoinedVariantsInOrder() {

        assertThat(provider.extractVariant("Anticausal Vestige (Showcase) (Fracture Foil)"))
                .isEqualTo("Showcase, Fracture Foil");
    }
}
