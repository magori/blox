package ch.magori.blox.feature.home

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
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
    @DisplayName("each selected choice bears a check, the others none, and TalkBack does not read it")
    fun eachSelectedChoiceBearsACheckThatTalkBackDoesNotRead() = extension.use {
        // Given « Classique », « Couleurs », 5 and the music of the player for the games, which has a description
        showHomeScreen(uiState.copy(gameMusic = GameMusicUi(name = "Thème du soir", previewed = false)))

        // Then a cue other than colour (WCAG 2.1, 1.4.1), in the selected choices only
        val checks = onAllNodes(hasTestTag(CHOICE_CHECK_TAG), useUnmergedTree = true)
        assertThat(checks.fetchSemanticsNodes()).hasSize(4)
        onAllNodes(hasTestTag(CHOICE_CHECK_TAG) and hasParent(isSelected()), useUnmergedTree = true)
            .assertCountEquals(4)
        // And decorative: TalkBack already says « sélectionné », it reads nothing more
        checks.assertAll(
            SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription) and
                SemanticsMatcher.keyNotDefined(SemanticsProperties.Text),
        )
        // nor adds anything to the description of a selected choice
        onNodeWithContentDescription("Thème du soir, en jeu")
            .assertIsSelected()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Thème du soir, en jeu")),
            )
    }

    @Test
    @DisplayName("every button and choice, start levels included, is 48 dp wide and high at least on 360 dp")
    fun everyTargetIsLargeEnough() = extension.use {
        // Given a music that may be listened to, with its button, on a phone of 360 dp, the width of the mockup
        showHomeScreen(uiState.copy(listenable = listOf(MusicChoice.KOROBEINIKI)), width = NARROW_PHONE)

        // Then: 48 dp visible, a rule of BloX, stricter than WCAG 2.2 (2.5.8, 24 px) and than Android, whose
        // 48 dp is a touch area that may pass beyond what is drawn
        val targets = onAllNodes(hasClickAction())
        val count = targets.fetchSemanticsNodes().size
        onAllNodes(hasClickAction() and hasTestTag(START_LEVEL_TAG)).assertCountEquals(uiState.startLevels.size)
        repeat(count) { index ->
            targets[index].assertWidthIsAtLeast(
                BloxSizes.minTouchTarget,
            ).assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        }
    }

    @Test
    @DisplayName("the start levels stand on two rows of five, 0 to 4 above 5 to 9, in cells as wide as one another")
    fun theStartLevelsStandOnTwoRowsOfFive() = extension.use {
        // Given
        showHomeScreen(width = NARROW_PHONE)

        // Then
        val cells = startLevelCells()
        assertThat(cells.map { it.text }).containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        val (first, second) = cells.chunked(5)
        assertThat(first.map { it.bounds.top }.distinct()).hasSize(1)
        assertThat(second.map { it.bounds.top }.distinct()).hasSize(1)
        assertThat(second.first().bounds.top).isGreaterThanOrEqualTo(first.first().bounds.bottom)
        assertThat(second.map { it.bounds.left }).isEqualTo(first.map { it.bounds.left })
        assertCellsAsWideAsOneAnother(cells)
    }

    @Test
    @DisplayName("a last row of fewer than five start levels is aligned on the cells of the row above, as wide")
    fun aShortLastRowIsAlignedOnTheRowAbove() = extension.use {
        // Given a mode of 8 start levels
        showHomeScreen(uiState.copy(startLevels = (0..7).toList()), width = NARROW_PHONE)

        // Then 5, 6 and 7 stand under 0, 1 and 2, in cells as wide
        val cells = startLevelCells()
        assertThat(cells.map { it.text }).containsExactly("0", "1", "2", "3", "4", "5", "6", "7")
        assertThat(cells.drop(5).map { it.bounds.left }).isEqualTo(cells.take(3).map { it.bounds.left })
        assertThat(cells[5].bounds.top).isGreaterThanOrEqualTo(cells[0].bounds.bottom)
        assertCellsAsWideAsOneAnother(cells)
    }

    @Test
    @DisplayName("in a normal font, every start level is 48 dp high, no more")
    fun inANormalFontEveryStartLevelIs48DpHigh() = extension.use {
        // Given level 5 selected, on 360 dp
        showHomeScreen(width = NARROW_PHONE)

        // Then the cells take no room above or under their digit beyond the 48 dp of a finger
        val levels = onAllNodes(hasTestTag(START_LEVEL_TAG))
        repeat(uiState.startLevels.size) { index -> levels[index].assertHeightIsEqualTo(BloxSizes.minTouchTarget) }
    }

    @Test
    @DisplayName("in a large font, every start level is as tall as the other choices, its check clear of its digit")
    fun inALargeFontEveryStartLevelIsAsTallAsTheOtherChoices() = extension.use {
        // Given level 5 selected, in a font twice as large, on 360 dp
        showHomeScreen(width = NARROW_PHONE, fontScale = 2f)

        // Then the cells, selected (5) or not (4), take no more height than their digit needs, as the other choices
        val level = onNode(hasTestTag(START_LEVEL_TAG) and hasText("5")).getUnclippedBoundsInRoot()
        val choice = onNodeWithText("Original").getUnclippedBoundsInRoot()
        assertThat(level.bottom - level.top).isEqualTo(choice.bottom - choice.top)
        val unselected = onNode(hasTestTag(START_LEVEL_TAG) and hasText("4")).getUnclippedBoundsInRoot()
        assertThat(level.bottom - level.top).isEqualTo(unselected.bottom - unselected.top)
        // And the check, in the corner, touches neither the digit nor the frame
        val inLevel = hasParent(hasTestTag(START_LEVEL_TAG))
        val check = onNode(hasTestTag(CHOICE_CHECK_TAG) and inLevel, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val digit = onNode(hasText("5") and inLevel, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertThat(check.left).isGreaterThanOrEqualTo(digit.right)
        assertThat(check.right).isLessThan(level.right)
        assertThat(check.top).isGreaterThan(level.top)
    }

    @Test
    @DisplayName("TalkBack reads the start levels as one group of radio buttons, from 0 to 9")
    fun theStartLevelsAreOneGroupOfRadioButtons() = extension.use {
        // Given
        showHomeScreen(width = NARROW_PHONE)

        // Then every level is a child of the same selectable group, in order, read as a radio button
        val group = SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)
        val levels = onAllNodes(hasTestTag(START_LEVEL_TAG) and hasParent(group)).fetchSemanticsNodes()
        assertThat(levels.map { it.config[SemanticsProperties.Text].joinToString() })
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        assertThat(levels.map { it.config[SemanticsProperties.Role] }).containsOnly(Role.RadioButton)
    }

    @Test
    @DisplayName("a music of the player for the games is read as such, the only music selected, a 48 dp radio button")
    fun theMusicOfThePlayerIsTheOnlyMusicSelected() = extension.use {
        // Given
        showHomeScreen(uiState.copy(gameMusic = GameMusicUi(name = "Thème du soir", previewed = false)))

        // Then TalkBack says it is the music of the games, not only its name
        onNodeWithContentDescription("Thème du soir, en jeu")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        onNodeWithText("Menuet").assertIsNotSelected()
    }

    @Test
    @DisplayName("listening to the music of the player for the games calls for it")
    fun listeningToTheMusicOfThePlayerCallsForIt() = extension.use {
        // Given
        var listened = 0
        showHomeScreen(
            uiState.copy(gameMusic = GameMusicUi(name = "Thème du soir", previewed = false)),
            onListenGameMusic = { listened++ },
        )

        // When
        onNodeWithContentDescription("Écouter Thème du soir").performClick()

        // Then
        assertThat(listened).isEqualTo(1)
    }

    @Test
    @DisplayName("the music of the player being listened to offers to stop it")
    fun theMusicOfThePlayerBeingListenedToOffersToStopIt() = extension.use {
        // Given
        showHomeScreen(uiState.copy(gameMusic = GameMusicUi(name = "Thème du soir", previewed = true)))

        // Then
        onNodeWithContentDescription("Arrêter Thème du soir").assertHasClickAction()
        onNodeWithContentDescription("Écouter Thème du soir").assertDoesNotExist()
    }

    @Test
    @DisplayName("the button Composer une musique, shown in capitals and read as written, opens the music editor")
    fun composeButtonOpensTheEditor() = extension.use {
        // Given
        var composing = 0
        showHomeScreen(onCompose = { composing++ })

        // When
        onNodeWithText("COMPOSER UNE MUSIQUE").assertExists()
        onNodeWithContentDescription("Composer une musique").performScrollTo().performClick()

        // Then
        runOnIdle { assertThat(composing).isEqualTo(1) }
    }

    /** A start level cell: its digit, and where it stands. */
    private data class Cell(val text: String, val bounds: DpRect)

    /** The start level cells, in the order of the tree, the order TalkBack reads them in. */
    private fun ComposeContext.startLevelCells(): List<Cell> {
        val nodes = onAllNodes(hasTestTag(START_LEVEL_TAG))
        return List(nodes.fetchSemanticsNodes().size) { index ->
            val node = nodes[index]
            Cell(
                text = node.fetchSemanticsNode().config[SemanticsProperties.Text].joinToString(),
                bounds = node.getUnclippedBoundsInRoot(),
            )
        }
    }

    /** All [cells] as wide as one another, but for the rounding to a pixel. */
    private fun assertCellsAsWideAsOneAnother(cells: List<Cell>) {
        val widths = cells.map { it.bounds.right - it.bounds.left }
        assertThat(widths.max() - widths.min()).isLessThanOrEqualTo(1.dp)
    }

    private fun ComposeContext.showHomeScreen(
        state: HomeUiState = uiState,
        width: Dp = Dp.Unspecified,
        fontScale: Float = 1f,
        onListenGameMusic: () -> Unit = {},
        onCompose: () -> Unit = {},
    ) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = BloxThemeKind.COLORFUL) {
                    HomeScreen(
                        modifier = Modifier.width(width),
                        uiState = state,
                        onThemeChosen = {},
                        onMusicChosen = {},
                        onListen = {},
                        onListenGameMusic = onListenGameMusic,
                        onChangeMusicFile = {},
                        onStartLevelChosen = {},
                        onPlay = {},
                        onCompose = onCompose,
                    )
                }
            }
        }
    }
}

/** A phone of 360 dp wide, as on the mockup: the narrowest of those BloX is drawn for. */
private val NARROW_PHONE = 360.dp
