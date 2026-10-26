package ch.magori.blox.core.designsystem.component

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ChoiceRowsTest {
    @Test
    fun `ten choices in five columns stand on two rows of five, 0 to 4 then 5 to 9`() {
        // When
        val rows = choiceRows(count = 10, columns = 5)

        // Then
        assertThat(rows).containsExactly(listOf(0, 1, 2, 3, 4), listOf(5, 6, 7, 8, 9))
    }

    @Test
    fun `a last row of fewer choices than columns holds those left, in order`() {
        // When
        val rows = choiceRows(count = 8, columns = 5)

        // Then
        assertThat(rows).containsExactly(listOf(0, 1, 2, 3, 4), listOf(5, 6, 7))
    }

    @Test
    fun `as many choices as columns stand on a single full row`() {
        // When
        val rows = choiceRows(count = 5, columns = 5)

        // Then
        assertThat(rows).containsExactly(listOf(0, 1, 2, 3, 4))
    }

    @Test
    fun `eleven choices in five columns stand on three rows, the last holding a single choice`() {
        // When
        val rows = choiceRows(count = 11, columns = 5)

        // Then
        assertThat(rows).containsExactly(listOf(0, 1, 2, 3, 4), listOf(5, 6, 7, 8, 9), listOf(10))
    }
}
