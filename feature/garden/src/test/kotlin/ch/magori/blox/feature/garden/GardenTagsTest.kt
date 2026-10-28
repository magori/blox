package ch.magori.blox.feature.garden

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GardenTagsTest {
    @Test
    fun `the tag of Changer de jeu stays garden_change_game, which the journey of benchmark copies as it is`() {
        // Given / When / Then: `:benchmark` does not see `feature:garden`, and knows the garden open by this text
        // (`By.res`)
        assertThat(GARDEN_CHANGE_GAME_TAG).isEqualTo("garden_change_game")
    }
}
