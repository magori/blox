package ch.magori.blox.feature.home

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlayTagTest {
    @Test
    fun `the tag of the play button stays home_play, which the journey of benchmark copies as it is`() {
        // Given / When / Then: `:benchmark` does not see `feature:home`, and finds the button by this text (`By.res`)
        assertThat(PLAY_TAG).isEqualTo("home_play")
    }
}
