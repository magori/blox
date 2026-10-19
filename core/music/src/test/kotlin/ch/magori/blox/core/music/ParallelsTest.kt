package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParallelsTest {
    @Test
    fun `two voices struck together a fifth apart, then a fifth apart again, both going up, make parallel fifths`() {
        // Given C5 over F4 on beat 1, then D5 over G4 on beat 3, then E5 over C4, a third, which loops back to C5
        val upper = listOf(struck(0, 0.0, "C5"), struck(0, 2.0, "D5"), struck(0, 3.0, "E5"))
        val lower = listOf(struck(0, 0.0, "F4"), struck(0, 2.0, "G4"), struck(0, 3.0, "C4"))

        // When
        val parallels = parallelsBetween(upper, lower)

        // Then
        assertThat(parallels).containsExactly(
            Parallel(Simultaneity(0, 0.0, pitch("C5"), pitch("F4")), Simultaneity(0, 2.0, pitch("D5"), pitch("G4"))),
        )
    }

    @Test
    fun `octaves and unisons in turn, both voices going the same way, make parallel octaves, compound ones too`() {
        // Given C5 over C3, two octaves, then D5 over D4, an octave, then E5 in unison over E5, all going up, then F5
        // over A4, a sixth, which loops back to C5
        val upper = listOf(struck(0, 0.0, "C5"), struck(0, 1.0, "D5"), struck(1, 0.0, "E5"), struck(1, 1.0, "F5"))
        val lower = listOf(struck(0, 0.0, "C3"), struck(0, 1.0, "D4"), struck(1, 0.0, "E5"), struck(1, 1.0, "A4"))

        // When
        val parallels = parallelsBetween(upper, lower)

        // Then
        assertThat(parallels.map { it.after.onset to it.after.bar }).containsExactly(1.0 to 0, 0.0 to 1)
    }

    @Test
    fun `fifths or octaves in contrary or oblique motion, or struck again on the same notes, are not parallel`() {
        // Given C5 over F4, then G5 over C4 in contrary motion, then G5 over C3 in oblique motion, then the same again
        val upper = listOf(struck(0, 0.0, "C5"), struck(0, 1.0, "G5"), struck(0, 2.0, "G5"), struck(0, 3.0, "G5"))
        val lower = listOf(struck(0, 0.0, "F4"), struck(0, 1.0, "C4"), struck(0, 2.0, "C3"), struck(0, 3.0, "C3"))

        // When
        val parallels = parallelsBetween(upper, lower)

        // Then
        assertThat(parallels).isEmpty()
    }

    @Test
    fun `each vertical moves to the next one where both voices have moved, whichever voice struck its notes`() {
        // Given C5 over F4, then E5 struck while F4 holds, then A4 struck while E5 holds: C5 over F4 moves to E5 over A4,
        // a fifth again, both voices up, then to D5 over G4, a fifth again, both down, then to E5 over C5, a third
        val upper = listOf(struck(0, 0.0, "C5"), struck(0, 1.0, "E5"), struck(0, 2.0, "D5"), struck(0, 3.0, "E5"))
        val lower = listOf(struck(0, 0.0, "F4"), struck(0, 1.5, "A4"), struck(0, 2.0, "G4"), struck(0, 3.0, "C5"))

        // When
        val parallels = parallelsBetween(upper, lower)

        // Then
        assertThat(parallels.map { it.before.onset to it.after.onset }).containsExactly(0.0 to 1.5, 1.5 to 2.0)
    }

    @Test
    fun `the music loops, so the last notes struck together lead to the first ones, and may move in parallel too`() {
        // Given C5 over F4, then E5 over G4, then D5 over G4 in the last bar, which goes back down to C5 over F4
        val upper = listOf(struck(0, 0.0, "C5"), struck(1, 0.0, "E5"), struck(7, 0.0, "D5"))
        val lower = listOf(struck(0, 0.0, "F4"), struck(1, 0.0, "G4"), struck(7, 0.0, "G4"))

        // When
        val parallels = parallelsBetween(upper, lower)

        // Then
        assertThat(parallels.map { it.before.bar to it.after.bar }).containsExactly(7 to 0)
    }

    @Test
    fun `a voice reaching an octave while the other holds, then both moving together, makes parallel octaves`() {
        // Given C2 held from beat 1 while the upper voice strikes C5 on beat 4, then F5 over F2 in bar 2: C5 over C2
        // sounds from beat 4, though C2 was struck before it, and both voices go up to the octave F5 over F2
        val upper = listOf(struck(0, 3.0, "C5"), struck(1, 0.0, "F5"))
        val lower = listOf(struck(0, 0.0, "C2"), struck(1, 0.0, "F2"))

        // When
        val parallels = parallelsBetween(upper, lower)

        // Then
        assertThat(parallels).contains(
            Parallel(Simultaneity(0, 3.0, pitch("C5"), pitch("C2")), Simultaneity(1, 0.0, pitch("F5"), pitch("F2"))),
        )
    }

    private fun struck(
        bar: Int,
        onset: Double,
        name: String,
    ): StruckNote = StruckNote(bar, onset, pitch(name))

    private fun pitch(name: String): Pitch = Pitch.parse(name)
}
