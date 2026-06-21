package com.smichelotti.mtg.provider;

import com.smichelotti.mtg.dto.ResolvedEdition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EditionNameNormalizer {

    private static final Set<String> TRAILING_QUALIFIERS_TO_DROP = Set.of("Planes", "Schemes");

    private interface CandidateStrategy {
        List<String> generate(ResolvedEdition edition);
    }

    // Ordered by confidence: original name first, then each independent pattern strategy.
    // Phase 2 will append strategies for Universes Beyond, parent/child compounds and
    // historical aliases here without touching the strategies already in this list.
    private static final List<CandidateStrategy> STRATEGIES = List.of(
            EditionNameNormalizer::originalName,
            edition -> suffixToPrefix(edition.setName(), "Commander"),
            edition -> suffixToPrefix(edition.setName(), "Art Series"),
            EditionNameNormalizer::trailingQualifierRemoval,
            EditionNameNormalizer::withCodeSuffix);

    public List<String> candidatesFor(ResolvedEdition edition) {

        Set<String> candidates = new LinkedHashSet<>();

        for (CandidateStrategy strategy : STRATEGIES) {
            candidates.addAll(strategy.generate(edition));
        }

        return List.copyOf(candidates);
    }

    private static List<String> originalName(ResolvedEdition edition) {
        return List.of(edition.setName());
    }

    // Pattern A / B: Scryfall suffixes the qualifier ("<Set> Commander"),
    // MTGStocks prefixes it instead ("Commander: <Set>").
    private static List<String> suffixToPrefix(String name, String qualifier) {

        String suffix = " " + qualifier;

        if (name.length() <= suffix.length()
                || !name.regionMatches(true, name.length() - suffix.length(), suffix, 0, suffix.length())) {
            return List.of();
        }

        String base = name.substring(0, name.length() - suffix.length()).trim();
        return List.of(qualifier + ": " + base);
    }

    // Pattern E: MTGStocks drops a generic trailing category noun that Scryfall keeps
    // ("Planechase 2012 Planes" -> "Planechase 2012", "Archenemy Schemes" -> "Archenemy").
    private static List<String> trailingQualifierRemoval(ResolvedEdition edition) {

        String name = edition.setName();

        for (String qualifier : TRAILING_QUALIFIERS_TO_DROP) {

            String suffix = " " + qualifier;

            if (name.length() > suffix.length()
                    && name.regionMatches(true, name.length() - suffix.length(), suffix, 0, suffix.length())) {
                return List.of(name.substring(0, name.length() - suffix.length()).trim());
            }
        }

        return List.of();
    }

    // Pattern F: MTGStocks appends the set's own Scryfall code in parentheses for
    // older core sets ("Magic 2015" -> "Magic 2015 (M15)").
    private static List<String> withCodeSuffix(ResolvedEdition edition) {

        String code = edition.setCode();

        if (code == null || code.isBlank()) {
            return List.of();
        }

        return List.of(edition.setName() + " (" + code.toUpperCase() + ")");
    }
}
