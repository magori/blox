package ch.magori.blox.feature.home

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.designsystem.text.assertWordsWhole
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

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
    @DisplayName("TalkBack reads the record as it is, neither capped nor padded with zeros as it is shown")
    fun talkBackReadsTheRecordAsItIs() = extension.use {
        // Given a record beyond the six digits shown
        showHomeScreen(uiState.copy(record = 1_234_567))

        // Then TalkBack reads its value alone, not the text shown, capped (« Record 999999 »)
        onNodeWithContentDescription("Record 1234567")
            .assertExists()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
        onNodeWithText("Record", substring = true).assertDoesNotExist()
    }

    @Test
    @DisplayName("the Modern mode is disabled, as TalkBack reads it, beside the Classic mode, selected")
    fun theModernModeIsDisabled() = extension.use {
        // Given
        showHomeScreen()

        // Then
        onNodeWithText("Moderne (bientôt)").assertIsNotEnabled().assertIsNotSelected()
        onNodeWithText("Classique").assertIsEnabled().assertIsSelected()
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

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("in a font twice as large, on 360 dp, the label of Composer une musique is shown whole")
    fun inALargeFontComposeMusicIsShownWhole(kind: BloxThemeKind) = extension.use {
        // Given a font twice as large, on 360 dp
        showHomeScreen(width = NARROW_PHONE, fontScale = 2f, kind = kind)

        // Then its label, as laid out, is not cut
        assertThat(composeMusicLabel().hasVisualOverflow).isFalse()
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("in a normal font, on 360 dp, the label of Composer une musique stays on one line")
    fun inANormalFontComposeMusicStaysOnOneLine(kind: BloxThemeKind) = extension.use {
        // Given the normal font, on 360 dp
        showHomeScreen(width = NARROW_PHONE, kind = kind)

        // Then its label takes one line, shrinking if needed, rather than two
        val label = composeMusicLabel()
        assertThat(label.lineCount).isEqualTo(1)
        assertThat(label.hasVisualOverflow).isFalse()
    }

    @Test
    @DisplayName("in a font twice as large, in Original, each word of Composer une musique stays whole on its line")
    fun inALargeFontEachWordOfComposeMusicStaysWhole() = extension.use {
        // Given a font twice as large, on 360 dp, in the pixel font
        showHomeScreen(width = NARROW_PHONE, fontScale = 2f, kind = BloxThemeKind.ORIGINAL)

        // Then each line holds whole words only: none is cut from one line to the next
        val label = composeMusicLabel()
        val text = label.layoutInput.text.text
        val words = List(label.lineCount) { line ->
            text.substring(label.getLineStart(line), label.getLineEnd(line)).split(' ').filter { it.isNotEmpty() }
        }
        assertThat(label.lineCount).isEqualTo(2)
        assertThat(words.flatten()).containsExactly("COMPOSER", "UNE", "MUSIQUE")
    }

    /** The label of « Composer une musique », as laid out. */
    private fun ComposeContext.composeMusicLabel(): TextLayoutResult = textLayout("COMPOSER UNE MUSIQUE")

    /** The text shown as [text], as laid out. */
    private fun ComposeContext.textLayout(text: String): TextLayoutResult {
        val label = onNode(hasText(text), useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        label.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single()
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

    @Test
    @DisplayName("the link MES MUSIQUES, in the header of the music section, read as written, opens my musics")
    fun myMusicsLinkOpensMyMusics() = extension.use {
        // Given
        var opened = 0
        showHomeScreen(onMyMusics = { opened++ })

        // When
        onNodeWithText("MES MUSIQUES").assertExists()
        onNodeWithContentDescription("Mes musiques").performScrollTo().performClick()

        // Then
        runOnIdle { assertThat(opened).isEqualTo(1) }
    }

    @Test
    @DisplayName("TalkBack reads Vibrations and Effets sonores as switches, off by default")
    fun talkBackReadsTheSettingsAsSwitchesOffByDefault() = extension.use {
        // Given the settings as saved by default
        showHomeScreen()

        // Then « Vibrations, interrupteur, désactivé »
        listOf("Vibrations", "Effets sonores").forEach { setting ->
            onNodeWithText(setting)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
                .assertIsToggleable()
                .assertIsOff()
                .assertHasClickAction()
        }
    }

    @Test
    @DisplayName("TalkBack reads a setting turned on as on, the other staying off")
    fun talkBackReadsASettingTurnedOnAsOn() = extension.use {
        // Given
        showHomeScreen(uiState.copy(vibrations = false, soundEffects = true))

        // Then
        onNodeWithText("Vibrations").assertIsOff()
        onNodeWithText("Effets sonores").assertIsOn()
    }

    @Test
    @DisplayName("tapping a setting off turns it on, tapping a setting on turns it off")
    fun tappingASettingSwitchesIt() = extension.use {
        // Given the vibrations off, the sound effects on
        val vibrations = mutableListOf<Boolean>()
        val soundEffects = mutableListOf<Boolean>()
        showHomeScreen(
            uiState.copy(vibrations = false, soundEffects = true),
            onVibrationsChanged = { vibrations += it },
            onSoundEffectsChanged = { soundEffects += it },
        )

        // When
        onNodeWithText("Vibrations").performScrollTo().performClick()
        onNodeWithText("Effets sonores").performScrollTo().performClick()

        // Then
        runOnIdle {
            assertThat(vibrations).containsExactly(true)
            assertThat(soundEffects).containsExactly(false)
        }
    }

    @Test
    @DisplayName("each setting is a row as wide as the screen within its margins, 48 dp high at least")
    fun eachSettingIsAFullWidthRow() = extension.use {
        // Given a phone of 360 dp, with margins of 24 dp
        showHomeScreen(width = NARROW_PHONE)

        // Then
        listOf("Vibrations", "Effets sonores").forEach { setting ->
            onNodeWithText(setting)
                .assertWidthIsEqualTo(NARROW_PHONE - 48.dp)
                .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        }
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName(
        "in a font twice as large, on 360 dp, the label of each setting keeps its size, its words whole, in a row of 48 dp",
    )
    fun inALargeFontEachSettingLabelKeepsItsSizeAndItsWordsWhole(kind: BloxThemeKind) = extension.use {
        // Given a font twice as large, on the narrowest phone
        showHomeScreen(width = NARROW_PHONE, fontScale = 2f, kind = kind)

        // Then no word is cut from one line to the next (« Vibratio / ns »), and no label shrinks: it is as large as
        // the heading of the section, in the same style, which never shrinks; the row grows with it
        val normalSize = textLayout("Réglages").layoutInput.style.fontSize
        listOf("Vibrations", "Effets sonores").forEach { setting ->
            val label = textLayout(setting)
            assertWordsWhole(label)
            assertThat(label.hasVisualOverflow).isFalse()
            assertThat(label.layoutInput.style.fontSize.value).isGreaterThanOrEqualTo(normalSize.value)
            onNodeWithText(setting).assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        }
    }

    @Test
    @DisplayName("the settings stand between Composer une musique and Jouer, under their heading Réglages")
    fun theSettingsStandBetweenComposeAndPlay() = extension.use {
        // Given
        showHomeScreen()

        // Then
        val compose = onNodeWithText("COMPOSER UNE MUSIQUE", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val heading = onNodeWithText("Réglages").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .getUnclippedBoundsInRoot()
        val vibrations = onNodeWithText("Vibrations").getUnclippedBoundsInRoot()
        val soundEffects = onNodeWithText("Effets sonores").getUnclippedBoundsInRoot()
        val play = onNodeWithText("Jouer").getUnclippedBoundsInRoot()
        assertThat(heading.top).isGreaterThanOrEqualTo(compose.bottom)
        assertThat(vibrations.top).isGreaterThanOrEqualTo(heading.bottom)
        assertThat(soundEffects.top).isGreaterThanOrEqualTo(vibrations.bottom)
        assertThat(play.top).isGreaterThanOrEqualTo(soundEffects.bottom)
    }

    private fun ComposeContext.showHomeScreen(
        state: HomeUiState = uiState,
        width: Dp = Dp.Unspecified,
        fontScale: Float = 1f,
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
        onListenGameMusic: () -> Unit = {},
        onCompose: () -> Unit = {},
        onMyMusics: () -> Unit = {},
        onVibrationsChanged: (Boolean) -> Unit = {},
        onSoundEffectsChanged: (Boolean) -> Unit = {},
    ) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = kind) {
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
                        onMyMusics = onMyMusics,
                        onVibrationsChanged = onVibrationsChanged,
                        onSoundEffectsChanged = onSoundEffectsChanged,
                    )
                }
            }
        }
    }
}

/** A phone of 360 dp wide, as on the mockup: the narrowest of those BloX is drawn for. */
private val NARROW_PHONE = 360.dp
