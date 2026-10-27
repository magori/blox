package ch.magori.blox.feature.daily

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DailyTagsTest {
    @Test
    fun `the tag of Jouer stays daily_play, which the journey of benchmark copies as it is`() {
        // Given / When / Then: `:benchmark` does not see `feature:daily`, and finds Jouer by this text (`By.res`)
        assertThat(DAILY_PLAY_TAG).isEqualTo("daily_play")
    }
}
