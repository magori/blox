package ch.magori.blox.feature.composer.grid

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxShapeSets
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.Gloss
import ch.magori.blox.feature.composer.grid.TrackColorsTest.Companion.colorfulGlosses
import ch.magori.blox.feature.composer.grid.TrackColorsTest.Companion.colorfulNoteLook
import ch.magori.blox.feature.composer.model.Track
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class NotePaintersTest {
    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a note under 16 dp has 2 dp corners and no gloss`(variant: BloxThemeVariant) {
        // Given the bubble of a note of 40 by 31 px at a density of 2: 15,5 dp high, a sixteenth note half as wide
        val bubble = requireNotNull(colorfulNoteLook(variant, Track.MELODY).bubble)

        // When
        val look = bubble.lookAt(Size(40f, 31f), DENSITY)

        // Then: as a cell of a piece, a gloss would weigh too much on so small a note
        assertThat(look.shape).isEqualTo(RoundedCornerShape(2.dp))
        assertThat(look.gloss).isEqualTo(Gloss.None)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a note of 16 dp and more has the corners of radius cell and the gloss of cell gloss`(
        variant: BloxThemeVariant,
    ) {
        // Given the bubble of a note of 40 by 32 px at a density of 2: 16 dp on its smaller side
        val bubble = requireNotNull(colorfulNoteLook(variant, Track.BASS).bubble)

        // When
        val look = bubble.lookAt(Size(40f, 32f), DENSITY)

        // Then
        assertThat(look.shape).isEqualTo(BloxShapeSets.Colorful.cell)
        assertThat(look.gloss).isEqualTo(colorfulGlosses(variant).cell)
    }

    @Test
    fun `the painters of the notes are built once per size and variant, not at each touch`() {
        // Given painters that count what they build, for the notes of the melody by day and by night
        val built = mutableListOf<Pair<NoteLook, Size>>()
        val painters = NotePainters { look, size -> Any().also { built += look to size } }
        val day = colorfulNoteLook(BloxThemeVariant.COLORFUL_LIGHT, Track.MELODY)
        val night = colorfulNoteLook(BloxThemeVariant.COLORFUL_DARK, Track.MELODY)
        val whole = Size(70f, 44f)
        val half = Size(35f, 44f)
        val first = painters.get(day, whole)

        // When the notes are drawn again at each touch, a whole note and a sixteenth note, by day, by night, by day again
        repeat(TOUCHES) {
            listOf(day, night, day).forEach { look ->
                painters.get(look, whole)
                painters.get(look, half)
            }
        }

        // Then each painter is built once, its path, gradient and gloss with it, and drawn again as it is
        assertThat(built).containsExactlyInAnyOrder(day to whole, night to whole, day to half, night to half)
        assertThat(painters.get(day, whole)).isSameAs(first)
    }

    private companion object {
        val DENSITY = Density(2f)
        const val TOUCHES = 5
    }
}
