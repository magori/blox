package ch.magori.blox.feature.game.navigation

import androidx.compose.ui.test.ExperimentalTestApi
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate

/** The theme the game entry draws its screen in, under the theme chosen by the player: Original here. */
@OptIn(ExperimentalTestApi::class)
internal class GameThemeTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val reduced = BloxEffects(reduced = true, forcedBySystem = false)

    @Test
    @DisplayName(
        "a daily game is drawn in the colorful variant whatever the theme chosen, with the effects of the player",
    )
    fun aDailyGameIsDrawnInTheColorfulVariantWhateverTheThemeChosen() = extension.use {
        // Given
        var variant: BloxThemeVariant? = null
        var effects: BloxEffects? = null

        // When the entry of a daily game draws its screen, under the Original theme chosen, by day
        setContent {
            BloxTheme(kind = BloxThemeKind.ORIGINAL, darkSystem = false, effects = reduced) {
                GameTheme(GameVariant.Daily(LocalDate.of(2026, 10, 26), id = 1L), darkSystem = false) {
                    variant = BloxTheme.variant
                    effects = BloxTheme.effects
                }
            }
        }

        // Then
        assertThat(variant).isEqualTo(BloxThemeVariant.COLORFUL_LIGHT)
        assertThat(effects).isEqualTo(reduced)
    }

    @Test
    @DisplayName("a classic game is drawn in the theme chosen")
    fun aClassicGameIsDrawnInTheThemeChosen() = extension.use {
        // Given
        var variant: BloxThemeVariant? = null

        // When the entry of a classic game draws its screen, under the Original theme chosen
        setContent {
            BloxTheme(kind = BloxThemeKind.ORIGINAL, darkSystem = false, effects = reduced) {
                GameTheme(GameVariant.Classic, darkSystem = false) { variant = BloxTheme.variant }
            }
        }

        // Then
        assertThat(variant).isEqualTo(BloxThemeVariant.ORIGINAL)
    }
}
