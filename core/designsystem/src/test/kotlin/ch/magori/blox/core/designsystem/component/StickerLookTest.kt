package ch.magori.blox.core.designsystem.component

import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class StickerLookTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("colorful")
    fun `a sticker is accent-ink on accent, framed in the colour of the page`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val look = stickerLook(palette, BloxThemeKind.COLORFUL, tilt = STICKER_TILT, framed = true)

        // Then
        assertThat(look.fill).isEqualTo(palette.accent)
        assertThat(look.ink).isEqualTo(palette.accentInk)
        assertThat(look.frame).isEqualTo(palette.page)
    }

    @Test
    fun `a sticker not framed has no frame`() {
        // When
        val look = stickerLook(BloxPalettes.ColorfulLight, BloxThemeKind.COLORFUL, tilt = STICKER_TILT, framed = false)

        // Then
        assertThat(look.frame).isNull()
    }

    @Test
    fun `in Couleurs a sticker leans by its tilt, 4 degrees anticlockwise unless told otherwise`() {
        // When
        val usual = stickerLook(BloxPalettes.ColorfulLight, BloxThemeKind.COLORFUL, tilt = STICKER_TILT, framed = true)
        val steeper = stickerLook(BloxPalettes.ColorfulLight, BloxThemeKind.COLORFUL, tilt = -6f, framed = true)

        // Then
        assertThat(STICKER_TILT).isEqualTo(-4f)
        assertThat(usual.tilt).isEqualTo(-4f)
        assertThat(steeper.tilt).isEqualTo(-6f)
    }

    @Test
    fun `in Original a sticker never leans, whose pixels would blur`() {
        // When
        val look = stickerLook(BloxPalettes.Original, BloxThemeKind.ORIGINAL, tilt = STICKER_TILT, framed = false)

        // Then
        assertThat(look.tilt).isZero()
    }

    companion object {
        @JvmStatic
        fun colorful(): List<Arguments> = listOf(
            Arguments.of("Couleurs, jour", BloxPalettes.ColorfulLight),
            Arguments.of("Couleurs, nuit", BloxPalettes.ColorfulDark),
        )
    }
}
