package ch.magori.blox.feature.game.pause

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithText
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.music.LiveMood
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/** The card of the pause (V29), and the live music it names (V37). */
@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class PauseCardTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("the card of a live game on its calm music names it « Vivante · calme », read whole by TalkBack")
    fun theCardOfALiveGameNamesItsCalmMood() = extension.use {
        // Given / When
        showCard(LiveMood.CALM)

        // Then the music stands under the values, above Reprendre, read as one sentence, without its dot (the words
        // « Vivante · calme » are drawn, as the screenshots show)
        val music = onNode(hasContentDescription("Musique : Vivante, calme"))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
            .getUnclippedBoundsInRoot()
        val lines = onNode(hasContentDescription("Lignes : 52")).getUnclippedBoundsInRoot()
        assertThat(music.top).isGreaterThanOrEqualTo(lines.bottom)
        assertThat(music.bottom).isLessThanOrEqualTo(onNodeWithText("Reprendre").getUnclippedBoundsInRoot().top)
    }

    @Test
    @DisplayName("the card of a live game in danger names it « Vivante · tendue »")
    fun theCardOfALiveGameInDangerNamesItsTenseMood() = extension.use {
        // Given / When
        showCard(LiveMood.TENSE)

        // Then
        onNode(hasContentDescription("Musique : Vivante, tendue")).assertExists()
    }

    @Test
    @DisplayName("the card of a game of another music names none")
    fun theCardOfAnotherMusicNamesNone() = extension.use {
        // Given / When
        showCard(liveMood = null)

        // Then
        onNode(hasContentDescription("Musique", substring = true)).assertDoesNotExist()
    }

    /** Shows the card of a game in Colorful, of the live music in [liveMood], if any. */
    private fun ComposeContext.showCard(liveMood: LiveMood?) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                PauseCard(
                    score = Points(12_340),
                    level = Level(5),
                    lines = Lines(52),
                    liveMood = liveMood,
                    onResume = {},
                    onMenu = {},
                )
            }
        }
    }
}
