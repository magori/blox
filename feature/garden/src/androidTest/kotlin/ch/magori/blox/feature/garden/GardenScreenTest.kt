package ch.magori.blox.feature.garden

import androidx.annotation.StringRes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.garden.Garden
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class GardenScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val empty = GardenUiState(Garden.EMPTY)

    @Test
    @DisplayName("the title of the bar shows JARDIN MUSICAL and is read as a heading")
    fun theTitleOfTheBarIsReadAsAHeading() = extension.use {
        // Given / When
        showGarden()

        // Then
        val title = string(R.string.feature_garden_title)
        onNodeWithText(title.uppercase()).assertExists()
        onNodeWithContentDescription(title).assert(isHeading())
    }

    @Test
    @DisplayName("Changer de jeu is handed to app")
    fun changerDeJeuIsHandedToApp() = extension.use {
        // Given
        var changes = 0
        showGarden(GardenActions(onChangeGame = { changes++ }))

        // When
        onNodeWithContentDescription(string(R.string.feature_garden_change_game)).performClick()

        // Then
        runOnIdle { assertThat(changes).isEqualTo(1) }
    }

    @Test
    @DisplayName("Changer de jeu counts only while the garden is resumed")
    fun changerDeJeuCountsOnlyWhileResumed() = extension.use {
        // Given the garden no longer resumed, as once a first tap left it (its transition under way)
        var changes = 0
        val owner = PausedLifecycleOwner()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                    GardenScreen(uiState = empty, actions = GardenActions(onChangeGame = { changes++ }))
                }
            }
        }

        // When
        onNodeWithContentDescription(string(R.string.feature_garden_change_game)).performClick()

        // Then nothing is handed to app
        runOnIdle { assertThat(changes).isZero() }
    }

    /** A screen started but not resumed: paused, as during the transition to the next one. */
    private class PausedLifecycleOwner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.STARTED }
    }

    /** The garden under the Original theme of the application: it draws itself in the Colorful theme. */
    private fun ComposeContext.showGarden(actions: GardenActions = GardenActions.None) {
        setContent {
            BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                GardenScreen(uiState = empty, actions = actions)
            }
        }
    }

    private fun string(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
