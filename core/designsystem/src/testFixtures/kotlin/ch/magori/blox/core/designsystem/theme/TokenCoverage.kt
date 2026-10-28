package ch.magori.blox.core.designsystem.theme

import org.assertj.core.api.Assertions.assertThat

/**
 * Checks that each of [tokens], read from tokens json, is [coded] or left out on purpose in [notCoded], with its reason,
 * and that nothing is coded or listed that tokens json does not have (ADR 0019).
 */
fun assertCodedOrListed(
    tokens: Collection<String>,
    coded: Collection<String>,
    notCoded: Map<String, String> = emptyMap(),
) {
    assertThat(tokens).containsExactlyInAnyOrderElementsOf(coded.toSet() + notCoded.keys)
    assertThat(notCoded.keys).doesNotContainAnyElementsOf(coded)
    assertThat(notCoded.values).allSatisfy { reason -> assertThat(reason).isNotBlank() }
}
