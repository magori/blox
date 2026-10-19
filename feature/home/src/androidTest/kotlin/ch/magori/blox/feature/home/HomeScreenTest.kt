package ch.magori.blox.feature.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.math.abs

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class HomeScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val uiState = HomeUiState(
        theme = ThemeChoice.COLORFUL,
        music = MusicChoice.MINUET,
        startLevel = 5,
        startLevels = (0..9).toList(),
        record = 0,
        listenable = emptyList(),
        previewing = null,
    )

    @Test
    @DisplayName("the selected theme and music are underlined, their unselected neighbours are not")
    fun selectedChoicesAreUnderlined() = extension.use {
        // Given
        showHomeScreen()

        // Then colour is not the only cue of the choice (WCAG 2.1, 1.4.1)
        assertThat(isUnderlined("Couleurs", selected = true)).isTrue()
        assertThat(isUnderlined("Menuet", selected = true)).isTrue()
        assertThat(isUnderlined("Original", selected = false)).isFalse()
        assertThat(isUnderlined("Mon fichier", selected = false)).isFalse()
    }

    @Test
    @DisplayName("TalkBack reads the selected start level as selected, and the others as not selected")
    fun talkBackReadsTheSelectedStartLevelAsSelected() = extension.use {
        // Given
        showHomeScreen()

        // Then
        onNodeWithText("5").assertIsSelected()
        onNodeWithText("4").assertIsNotSelected()
    }

    private fun ComposeContext.showHomeScreen() {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                HomeScreen(
                    uiState = uiState,
                    onThemeChosen = {},
                    onMusicChosen = {},
                    onListen = {},
                    onStartLevelChosen = {},
                    onPlay = {},
                )
            }
        }
    }

    /**
     * Whether the choice labelled [text] is underlined, from its pixels: among the lowest rows in the ink of its
     * label, one is a band that runs across the whole label, which no letter does on its own. The rows of the
     * rounded corners are left out: outside its corners, the page may have the colour of the ink.
     */
    private fun ComposeContext.isUnderlined(
        text: String,
        selected: Boolean,
    ): Boolean {
        val ink = choiceColors(BloxPalettes.Colorful, selected).ink
        val node = onNodeWithText(text)
        val corner = (CORNER_DP * node.fetchSemanticsNode().layoutInfo.density.density).toInt()
        val pixels = node.captureToImage().toPixelMap()
        fun isInk(
            x: Int,
            y: Int,
        ) = pixels[x, y].isCloseTo(ink)

        val inkRows = (corner until pixels.height - corner).filter { y -> (0 until pixels.width).any { isInk(it, y) } }
        val inkColumns = (0 until pixels.width).filter { x -> inkRows.any { y -> isInk(x, y) } }
        if (inkRows.isEmpty()) return false
        val labelWidth = inkColumns.last() - inkColumns.first() + 1
        return inkRows.takeLast(UNDERLINE_ROWS).any { y ->
            val run = (inkColumns.first()..inkColumns.last()).count { x -> isInk(x, y) }
            run >= labelWidth * BAND_FRACTION
        }
    }

    /** The same colour, give or take the smoothing of the edges. */
    private fun Color.isCloseTo(other: Color): Boolean =
        abs(red - other.red) < TOLERANCE && abs(green - other.green) < TOLERANCE && abs(blue - other.blue) < TOLERANCE

    private companion object {
        /** The underline is 2 dp thick: at most 6 rows of pixels on any phone up to 3 pixels a dp. */
        const val UNDERLINE_ROWS = 6

        /** The rounded corners of a choice in the Colorful theme. */
        const val CORNER_DP = 10
        const val BAND_FRACTION = 0.9
        const val TOLERANCE = 0.15f
    }
}
