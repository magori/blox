package ch.magori.blox.feature.home

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ListenButtonTest {
    @Test
    fun `a music not being previewed offers to listen to it, and TalkBack says which`() {
        // Given / When
        val texts = listenTexts(previewed = false)

        // Then
        assertThat(texts).isEqualTo(
            ListenTexts(
                label = R.string.feature_home_listen,
                description = R.string.feature_home_listen_description,
                glyph = ListenGlyph.PLAY,
            ),
        )
    }

    @Test
    fun `the music being previewed offers to stop it, and TalkBack says which`() {
        // Given / When
        val texts = listenTexts(previewed = true)

        // Then
        assertThat(texts).isEqualTo(
            ListenTexts(
                label = R.string.feature_home_stop,
                description = R.string.feature_home_stop_description,
                glyph = ListenGlyph.STOP,
            ),
        )
    }
}
