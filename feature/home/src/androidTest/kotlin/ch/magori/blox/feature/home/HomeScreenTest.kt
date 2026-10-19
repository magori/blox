package ch.magori.blox.feature.home

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithText
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

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
    @DisplayName("TalkBack reads each selected choice as selected, and the others as not selected")
    fun talkBackReadsEachSelectedChoiceAsSelected() = extension.use {
        // Given
        showHomeScreen()

        // Then
        onNodeWithText("Classique").assertIsSelected()
        onNodeWithText("Couleurs").assertIsSelected()
        onNodeWithText("Original").assertIsNotSelected()
        onNodeWithText("5").assertIsSelected()
        onNodeWithText("4").assertIsNotSelected()
        onNodeWithText("Menuet").assertIsSelected()
    }

    @Test
    @DisplayName("every button and choice is 48 dp wide and high at least, but the start levels")
    fun everyTargetIsLargeEnough() = extension.use {
        // Given a music that may be listened to, with its button
        showHomeScreen(uiState.copy(listenable = listOf(MusicChoice.KOROBEINIKI)))

        // Then: 48 dp visible, a rule of BloX, stricter than WCAG 2.2 (2.5.8, 24 px) and than Android, whose
        // 48 dp is a touch area that may pass beyond what is drawn. The width of the start levels is left to P5.29
        val targets = onAllNodes(hasClickAction() and !hasTestTag(START_LEVEL_TAG))
        val count = targets.fetchSemanticsNodes().size
        assertThat(count).isGreaterThan(1)
        repeat(count) { index ->
            targets[index].assertWidthIsAtLeast(
                BloxSizes.minTouchTarget,
            ).assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        }
    }

    @Test
    @DisplayName("every start level is 48 dp high at least")
    fun everyStartLevelIsHighEnough() = extension.use {
        // Given
        showHomeScreen()

        // Then: their width is left to P5.29, not their height (48 dp visible, a rule of BloX)
        val levels = onAllNodes(hasTestTag(START_LEVEL_TAG))
        assertThat(levels.fetchSemanticsNodes()).hasSize(uiState.startLevels.size)
        repeat(uiState.startLevels.size) { index -> levels[index].assertHeightIsAtLeast(BloxSizes.minTouchTarget) }
    }

    private fun ComposeContext.showHomeScreen(state: HomeUiState = uiState) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                HomeScreen(
                    uiState = state,
                    onThemeChosen = {},
                    onMusicChosen = {},
                    onListen = {},
                    onChangeMusicFile = {},
                    onStartLevelChosen = {},
                    onPlay = {},
                )
            }
        }
    }
}
