package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Position
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource
import java.security.MessageDigest

/**
 * The contract of generation (ADR 0014 amended by P7.28, `regles-serrurier.md`, Progression): padlock n is the same for
 * everyone and for ever. A failure here means the padlocks changed: that takes an ADR and a new number of generation,
 * never a mere update of the expected values.
 */
class PadlockContractTest {
    @ParameterizedTest
    @MethodSource("firstPadlocks")
    fun `the first padlock of each tier and of each family of the cycle never changes`(
        level: Int,
        ascii: String,
    ) {
        assertThat(asciiOf(Padlock.ofLevel(level).solution)).isEqualTo(ascii.trimIndent())
    }

    @Test
    fun `the padlocks 1 to 100 never change`() {
        // When: the SHA-256 of their solutions written one after the other, a blank line between two
        val written = (1..100).joinToString("\n\n") { level -> asciiOf(Padlock.ofLevel(level).solution) }
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(written.toByteArray())
            .joinToString("") { "%02x".format(it) }

        // Then
        assertThat(fingerprint).isEqualTo("1949084de54eaa633d4e84f79a97ba29915d0d07f4a53d9d3de0250377411649")
    }

    @Test
    fun `no two of the first 200 padlocks have the same hole, translation aside`() {
        // When
        val holes = (1..200).map { level -> anchored(Padlock.ofLevel(level).hole) }

        // Then
        assertThat(holes).doesNotHaveDuplicates()
    }

    @Test
    fun `the draws per piece, the candidates and the solutions counted never change`() {
        assertThat(PadlockGenerator.ATTEMPTS_PER_PIECE).isEqualTo(200)
        assertThat(PadlockGenerator.CANDIDATES).isEqualTo(20)
        assertThat(PadlockGenerator.SOLUTIONS_COUNTED).isEqualTo(5)
    }

    private companion object {
        /** [hole] moved so that its box starts at column 0, row 0. */
        fun anchored(hole: Set<Position>): Set<Position> {
            val corner = Position(hole.minOf(Position::column), hole.minOf(Position::row))
            return hole.map { it - corner }.toSet()
        }

        @JvmStatic
        fun firstPadlocks(): List<Arguments> = listOf(
            argumentSet(
                "padlock 1: Apprenti, 3 pieces without S or Z",
                1,
                """
                ##########
                ####1#####
                ##111#####
                ##2#######
                ##2#######
                #22#######
                ##3333####
                ##########
                ##########
                ##########
                """,
            ),
            argumentSet(
                "padlock 2: Apprenti, 4 pieces without S or Z",
                2,
                """
                ##########
                ##########
                ##########
                ##########
                #####4411#
                #####4#1##
                #####421##
                ####3#2###
                ###33322##
                ##########
                """,
            ),
            argumentSet(
                "padlock 3: Apprenti, 5 pieces",
                3,
                """
                ##########
                #2########
                #222#3####
                ##1#33####
                #1113#####
                #4##5#####
                #44455####
                #####5####
                ##########
                ##########
                """,
            ),
            argumentSet(
                "padlock 4: Compagnon, 6 pieces, sinuous",
                4,
                """
                ##########
                ##55######
                #5533#2###
                ####3#2###
                ####322###
                ##4411####
                ##4#1#6###
                ##4#166###
                ######6###
                ##########
                """,
            ),
            argumentSet(
                "padlock 7: Compagnon, 6 pieces, mixed",
                7,
                """
                ##########
                #####3####
                ##444333##
                ###54#1###
                ###5##11##
                ###55#1###
                ##66#222##
                ##66##2###
                ##########
                ##########
                """,
            ),
            argumentSet(
                "padlock 13: Artisan, 7 pieces, mixed",
                13,
                """
                ##########
                ##########
                ##1111555#
                #77#2###5#
                #77322##4#
                ##3332444#
                ###6######
                ###66#####
                ####6#####
                ##########
                """,
            ),
            argumentSet(
                "padlock 21: Maître, 8 pieces, compact",
                21,
                """
                ##########
                ##########
                ###61111##
                ##66###2##
                ##6###222#
                ##433338##
                ##4#5#888#
                ##4#55777#
                ##4##5##7#
                ##########
                """,
            ),
            argumentSet(
                "padlock 31: cycle 1, compact, unique solution",
                31,
                """
                ##########
                ######777#
                #5##337###
                #55533####
                ##661#2###
                #66#112###
                ###41#288#
                ##44##28##
                ##4####8##
                ##########
                """,
            ),
            argumentSet(
                "padlock 32: cycle 2, breathing, sinuous",
                32,
                """
                ##########
                ##66322###
                ##66332#1#
                ###7#3211#
                ##77###1##
                ###7###4##
                #88555544#
                #88#####4#
                ##########
                ##########
                """,
            ),
            argumentSet(
                "padlock 33: cycle 3, S, Z and T",
                33,
                """
                ##########
                ##########
                ###66688##
                ####67#88#
                #5##477###
                #5544#7#2#
                #534###22#
                ##33#112##
                ###311####
                ##########
                """,
            ),
            argumentSet(
                "padlock 34: cycle 4, compact, few solutions",
                34,
                """
                ##########
                #####4##7#
                ###5#4777#
                ##55#44###
                ##5222####
                ###233366#
                ##11#366##
                ##118#####
                ####888###
                ##########
                """,
            ),
            argumentSet(
                "padlock 35: cycle 0, one of each",
                35,
                """
                ##########
                #777##11##
                ##7###11##
                ##66662###
                ####332###
                ###33#22##
                ##44#5####
                ###44555##
                ##########
                ##########
                """,
            ),
        )
    }
}
