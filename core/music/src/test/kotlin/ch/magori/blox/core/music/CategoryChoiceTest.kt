package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.random.Random

class CategoryChoiceTest {
    private val seeds = 0L until 200L

    @Test
    fun `surprise may draw every category`() {
        // When
        val drawn = seeds.map { seed -> Surprise.resolve(Random(seed)) }.toSet()

        // Then
        assertThat(drawn).containsExactlyInAnyOrderElementsOf(Category.entries)
    }

    @Test
    fun `surprise draws the same category from the same seed`() {
        // When
        val first = seeds.map { seed -> Surprise.resolve(Random(seed)) }
        val second = seeds.map { seed -> Surprise.resolve(Random(seed)) }

        // Then
        assertThat(second).isEqualTo(first)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Category::class)
    fun `a category chosen by the player is played as it is, whatever the draw`(category: Category) {
        // Given
        val choice: CategoryChoice = category

        // When
        val resolved = seeds.map { seed -> choice.resolve(Random(seed)) }.toSet()

        // Then
        assertThat(resolved).containsExactly(category)
    }
}
