package ch.magori.blox.core.designsystem.component

import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.buttonColorsOf
import ch.magori.blox.core.designsystem.theme.glossesOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ListenFaceTest {
    // The name of the case in SUIVI.md (P5.94), kept whole: a name in backticks cannot be broken.
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `the listen button shows a triangle, and a square while playing, read Écouter or Arrêter with the name of the music`() {
        // When
        val atRest = listenFace(playing = false)
        val playing = listenFace(playing = true)

        // Then
        assertThat(atRest.icon).isEqualTo(BloxIcon.PLAY)
        assertThat(atRest.verb).isEqualTo(R.string.core_designsystem_listen)
        assertThat(atRest.named).isEqualTo(R.string.core_designsystem_listen_named)
        assertThat(playing.icon).isEqualTo(BloxIcon.STOP)
        assertThat(playing.verb).isEqualTo(R.string.core_designsystem_stop)
        assertThat(playing.named).isEqualTo(R.string.core_designsystem_stop_named)
    }

    @ParameterizedTest
    @EnumSource(BloxThemeVariant::class)
    fun `a music playing keeps its listen button in the pressed fill`(variant: BloxThemeVariant) {
        // Given the disc of the family Icône, pressed
        val colors = buttonColorsOf(variant)
        val glosses = glossesOf(variant)
        val pressed = buttonLook(ButtonFamily.ICON, colors, glosses, ButtonState(depth = 1f))

        // When at rest, the music playing or not
        val playing = buttonLook(ButtonFamily.ICON, colors, glosses, ButtonState(active = listenFace(true).active))
        val stopped = buttonLook(ButtonFamily.ICON, colors, glosses, ButtonState(active = listenFace(false).active))

        // Then
        assertThat(playing).isEqualTo(pressed)
        assertThat(stopped).isNotEqualTo(pressed)
    }
}
