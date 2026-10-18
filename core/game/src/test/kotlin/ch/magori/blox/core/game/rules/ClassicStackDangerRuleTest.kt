package ch.magori.blox.core.game.rules

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ClassicStackDangerRuleTest {
    private val rule: StackDangerRule = ClassicStackDangerRule

    @ParameterizedTest(name = "out of danger, height {0} -> in danger: {1}")
    @CsvSource("14, false", "15, true")
    fun `out of danger the stack enters danger when its height reaches 15`(
        height: Int,
        expectedInDanger: Boolean,
    ) {
        assertThat(inDangerAfter(wasInDanger = false, height)).isEqualTo(expectedInDanger)
    }

    @ParameterizedTest(name = "in danger, height {0} -> in danger: {1}")
    @CsvSource("13, true", "12, false")
    fun `in danger the stack leaves danger only when its height falls to 12 or less`(
        height: Int,
        expectedInDanger: Boolean,
    ) {
        assertThat(inDangerAfter(wasInDanger = true, height)).isEqualTo(expectedInDanger)
    }

    @ParameterizedTest(name = "in danger before: {0}, height {1} -> in danger: {2}")
    @CsvSource("false, 13, false", "true, 13, true", "false, 14, false", "true, 14, true")
    fun `the same height gives a different answer depending on whether the stack was in danger`(
        wasInDanger: Boolean,
        height: Int,
        expectedInDanger: Boolean,
    ) {
        assertThat(inDangerAfter(wasInDanger, height)).isEqualTo(expectedInDanger)
    }

    @ParameterizedTest(name = "height {0} is refused")
    @CsvSource("-1", "21")
    fun `a stack height outside 0 to 20 is refused`(height: Int) {
        assertThatIllegalArgumentException().isThrownBy { inDangerAfter(wasInDanger = false, height) }
    }

    /** When: the stack reaches [height], having been in danger or not before. */
    private fun inDangerAfter(
        wasInDanger: Boolean,
        height: Int,
    ): Boolean = rule.inDanger(wasInDanger, height)
}
