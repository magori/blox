package ch.magori.blox.feature.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.designsystem.component.CHOICE_CHECK_TAG
import ch.magori.blox.core.designsystem.component.GLYPH_BUTTON_ICON_TAG
import ch.magori.blox.core.designsystem.text.assertWordsWhole
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
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

    /** A start level: a cell that may be touched, in the group of the start levels. */
    private val startLevel = hasClickAction() and hasAnyAncestor(hasTestTag(START_LEVELS_TAG))

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
        onNodeWithContentDescription("Moderne, bientôt, indisponible").assertIsNotEnabled().assertIsNotSelected()
        onNodeWithText("Classique").assertIsEnabled().assertIsSelected()
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("on 360 dp, the two modes stand on one line")
    fun theTwoModesStandOnOneLine(kind: BloxThemeKind) = extension.use {
        // Given
        showHomeScreen(width = NARROW_PHONE, kind = kind)

        // Then
        val classic = onNodeWithText("Classique").getUnclippedBoundsInRoot()
        val modern = onNodeWithContentDescription("Moderne, bientôt, indisponible").getUnclippedBoundsInRoot()
        assertThat(modern.top).isEqualTo(classic.top)
        assertThat(modern.left).isGreaterThanOrEqualTo(classic.right)
    }

    @Test
    @DisplayName("the home screen leaves 16 dp above its header, and 16 dp between two sections")
    fun theHomeScreenLeaves16DpAboveAndBetweenSections() = extension.use {
        // Given a phone of 360 dp, without the bars of the system, which push the screen down
        showHomeScreen(width = NARROW_PHONE, withoutInsets = true)

        // Then
        val gear = onNodeWithContentDescription("Paramètres").getUnclippedBoundsInRoot()
        assertThat(gear.top.value).isCloseTo(16f, within(PIXEL_ROUNDING))
        val modes = onNodeWithText("Classique").getUnclippedBoundsInRoot()
        // A section starts with its label, above its group, or with its group, beside its label and taller
        val theme = minOf(
            onNodeWithText("Thème").getUnclippedBoundsInRoot().top,
            onNodeWithText("Original").getUnclippedBoundsInRoot().top,
        )
        assertThat((theme - modes.bottom).value).isCloseTo(16f, within(PIXEL_ROUNDING))
    }

    @Test
    @DisplayName(
        "the music choices bear nothing under them: one line of actions, under them all, is for the music selected",
    )
    fun oneLineOfActionsIsForTheMusicSelected() = extension.use {
        // Given the minuet selected, among musics that may all be listened to, a music file kept
        showHomeScreen(
            uiState.copy(
                listenable = listOf(MusicChoice.KOROBEINIKI, MusicChoice.MINUET, MusicChoice.MY_FILE),
                hasMusicFile = true,
            ),
        )

        // Then only the minuet may be listened to, under every choice
        val listen = onNodeWithContentDescription("Écouter Menuet").assertHasClickAction().getUnclippedBoundsInRoot()
        onNodeWithContentDescription("Écouter Korobeïniki").assertDoesNotExist()
        onNodeWithContentDescription("Écouter Mon fichier").assertDoesNotExist()
        onNodeWithContentDescription("Changer de fichier").assertDoesNotExist()
        listOf("Korobeïniki", "Menuet", "Mon fichier", "Aucune").forEach { choice ->
            assertThat(listen.top).isGreaterThanOrEqualTo(onNodeWithText(choice).getUnclippedBoundsInRoot().bottom)
        }
    }

    @Test
    @DisplayName(
        "with Mon fichier selected, the line of actions offers to listen to the file and to change it, side by side",
    )
    fun withMyFileTheLineOfActionsOffersToListenAndToChange() = extension.use {
        // Given
        showHomeScreen(
            uiState.copy(
                music = MusicChoice.MY_FILE,
                listenable = listOf(MusicChoice.KOROBEINIKI, MusicChoice.MINUET, MusicChoice.MY_FILE),
                hasMusicFile = true,
            ),
            width = NARROW_PHONE,
        )

        // Then
        val listen = onNodeWithContentDescription("Écouter Mon fichier").getUnclippedBoundsInRoot()
        val change = onNodeWithContentDescription(
            "Changer de fichier",
        ).assertHasClickAction().getUnclippedBoundsInRoot()
        assertThat(change.top).isEqualTo(listen.top)
        assertThat(change.left).isGreaterThanOrEqualTo(listen.right)
        assertThat(listen.top).isGreaterThanOrEqualTo(onNodeWithText("Aucune").getUnclippedBoundsInRoot().bottom)
    }

    @Test
    @DisplayName("with Aucune selected, there is nothing to listen to nor to change")
    fun withNoneThereIsNoAction() = extension.use {
        // Given
        showHomeScreen(
            uiState.copy(
                music = MusicChoice.NONE,
                listenable = listOf(MusicChoice.KOROBEINIKI, MusicChoice.MINUET, MusicChoice.MY_FILE),
                hasMusicFile = true,
            ),
        )

        // Then
        onAllNodes(hasContentDescription("Écouter", substring = true)).assertCountEquals(0)
        onNodeWithContentDescription("Changer de fichier").assertDoesNotExist()
    }

    @Test
    @DisplayName("the music selected being listened to offers to stop it, under a glyph that TalkBack does not read")
    fun theMusicSelectedBeingListenedToOffersToStopIt() = extension.use {
        // Given
        showHomeScreen(uiState.copy(listenable = listOf(MusicChoice.MINUET), previewing = MusicChoice.MINUET))

        // Then
        onNodeWithContentDescription("Écouter Menuet").assertDoesNotExist()
        onNodeWithContentDescription("Arrêter Menuet")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Arrêter Menuet")))
        iconOf(hasContentDescription("Arrêter Menuet"))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName(
        "in a font twice as large, on 360 dp, the labels of the line of actions are shown whole, on one line each",
    )
    fun inALargeFontTheLabelsOfTheLineOfActionsAreShownWhole(kind: BloxThemeKind) = extension.use {
        // Given Mon fichier selected, in a font twice as large, on 360 dp
        showHomeScreen(
            uiState.copy(music = MusicChoice.MY_FILE, listenable = listOf(MusicChoice.MY_FILE), hasMusicFile = true),
            width = NARROW_PHONE,
            fontScale = 2f,
            kind = kind,
        )

        // Then
        listOf("Écouter", "Changer").forEach { label ->
            val layout = textLayout(label)
            assertWordsWhole(layout)
            assertThat(layout.lineCount).isEqualTo(1)
            // Within the screen: the label is not cut at its edge
            val shown = onNode(hasText(label), useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertThat(shown.right).isLessThanOrEqualTo(NARROW_PHONE)
        }
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName(
        "in a font twice as large, in a window too narrow for both, Changer goes under Écouter, each label on one line",
    )
    fun inALargeFontInANarrowWindowChangeGoesUnderListen(kind: BloxThemeKind) = extension.use {
        // Given Mon fichier selected, in a font twice as large, in a window where each button fits, but not both
        val width = 280.dp
        showHomeScreen(
            uiState.copy(music = MusicChoice.MY_FILE, listenable = listOf(MusicChoice.MY_FILE), hasMusicFile = true),
            width = width,
            fontScale = 2f,
            kind = kind,
        )

        // Then
        val listen = onNodeWithContentDescription("Écouter Mon fichier").getUnclippedBoundsInRoot()
        val change = onNodeWithContentDescription("Changer de fichier").getUnclippedBoundsInRoot()
        assertThat(change.top).isGreaterThanOrEqualTo(listen.bottom)
        listOf(listen, change).forEach { button ->
            assertThat(button.left).isGreaterThanOrEqualTo(0.dp)
            assertThat(button.right).isLessThanOrEqualTo(width)
        }
        listOf("Écouter", "Changer").forEach { label ->
            val layout = textLayout(label)
            assertWordsWhole(layout)
            assertThat(layout.lineCount).isEqualTo(1)
        }
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
        onAllNodes(startLevel).assertCountEquals(uiState.startLevels.size)
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
        val levels = onAllNodes(startLevel)
        repeat(uiState.startLevels.size) { index -> levels[index].assertHeightIsEqualTo(BloxSizes.minTouchTarget) }
    }

    @Test
    @DisplayName("in a large font, every start level is as tall as the other choices, its check clear of its digit")
    fun inALargeFontEveryStartLevelIsAsTallAsTheOtherChoices() = extension.use {
        // Given level 5 selected, in a font twice as large, on 360 dp
        showHomeScreen(width = NARROW_PHONE, fontScale = 2f)

        // Then the cells, selected (5) or not (4), take no more height than their digit needs, as the other choices
        val level = onNode(startLevel and hasText("5")).getUnclippedBoundsInRoot()
        val choice = onNodeWithText("Original").getUnclippedBoundsInRoot()
        // But for the rounding of their edges to the pixel, which depends on where the cells stand on the screen
        assertThat(
            (level.bottom - level.top).value,
        ).isCloseTo((choice.bottom - choice.top).value, within(PIXEL_ROUNDING))
        val unselected = onNode(startLevel and hasText("4")).getUnclippedBoundsInRoot()
        assertThat(
            (level.bottom - level.top).value,
        ).isCloseTo((unselected.bottom - unselected.top).value, within(PIXEL_ROUNDING))
        // And the check, in the corner, touches neither the digit nor the frame
        val inLevel = hasParent(startLevel)
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
        val levels = onAllNodes(startLevel and hasParent(group)).fetchSemanticsNodes()
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
        val nodes = onAllNodes(startLevel)
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
    @DisplayName("the gear button, 48 dp, read Paramètres, opens the settings")
    fun theGearButtonOpensTheSettings() = extension.use {
        // Given
        var opened = 0
        showHomeScreen(onSettings = { opened++ })

        // When
        onNodeWithContentDescription("Paramètres")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(BloxSizes.minTouchTarget)
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
            .performClick()

        // Then
        runOnIdle { assertThat(opened).isEqualTo(1) }
    }

    @Test
    @DisplayName("the gear button stands at the top right of the home screen, beside the title")
    fun theGearButtonStandsAtTheTopRight() = extension.use {
        // Given a phone of 360 dp
        showHomeScreen(width = NARROW_PHONE)

        // Then
        val gear = onNodeWithContentDescription("Paramètres").getUnclippedBoundsInRoot()
        val title = onNodeWithText("BloX").getUnclippedBoundsInRoot()
        assertThat(gear.right).isEqualTo(NARROW_PHONE - 24.dp)
        assertThat(gear.left).isGreaterThanOrEqualTo(title.right)
        assertThat(gear.top).isLessThan(title.bottom)
    }

    @Test
    @DisplayName("the logo and the title BloX stand on one line, the record under them")
    fun theLogoAndTheTitleStandOnOneLine() = extension.use {
        // Given
        showHomeScreen(width = NARROW_PHONE)

        // Then the logo before the title, centred on the same line
        val logo = onNode(hasTestTag(HOME_LOGO_TAG), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val title = onNodeWithText("BloX").getUnclippedBoundsInRoot()
        val record = onNodeWithContentDescription("Record 0").getUnclippedBoundsInRoot()
        assertThat(logo.right).isLessThanOrEqualTo(title.left)
        assertThat((logo.top + logo.bottom) / 2 - (title.top + title.bottom) / 2).isBetween((-1).dp, 1.dp)
        assertThat(record.top).isGreaterThanOrEqualTo(title.bottom)
    }

    @Test
    @DisplayName("the home screen no longer shows the settings, gone to their own page")
    fun theHomeScreenNoLongerShowsTheSettings() = extension.use {
        // Given
        showHomeScreen()

        // Then
        onNodeWithText("Réglages").assertDoesNotExist()
        onNodeWithText("Vibrations").assertDoesNotExist()
        onNodeWithText("Effets sonores").assertDoesNotExist()
    }

    @Test
    @DisplayName("Jouer, MES MUSIQUES, Écouter and Composer une musique bear an icon that TalkBack does not read")
    fun theButtonsWithALabelBearAnIconThatTalkBackDoesNotRead() = extension.use {
        // Given the minuet selected, which may be listened to
        showHomeScreen(uiState.copy(listenable = listOf(MusicChoice.MINUET)))

        // Then each bears one icon, decorative
        listOf(
            hasTestTag(PLAY_TAG),
            hasContentDescription("Mes musiques"),
            hasContentDescription("Écouter Menuet"),
            hasContentDescription("Composer une musique"),
        ).forEach { button ->
            iconOf(button)
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
        }
        // And TalkBack reads each button as before, its label once
        onNode(hasTestTag(PLAY_TAG))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString("Jouer"))))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        listOf("Mes musiques", "Écouter Menuet", "Composer une musique").forEach { description ->
            onNodeWithContentDescription(description)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(description)))
        }
    }

    @Test
    @DisplayName("the gear button is read Paramètres once, its icon adding nothing")
    fun theGearButtonIsReadOnce() = extension.use {
        // Given
        showHomeScreen()

        // Then the button alone is described, once
        onNodeWithContentDescription("Paramètres")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Paramètres")))
        onAllNodes(hasContentDescription("Paramètres"), useUnmergedTree = true).assertCountEquals(1)
        // And its icon is there, with nothing for TalkBack: neither a description nor a role
        onNode(
            hasTestTag(GLYPH_BUTTON_ICON_TAG) and hasAnyAncestor(hasContentDescription("Paramètres")),
            useUnmergedTree = true,
        )
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName(
        "in a font twice as large, on 360 dp, Jouer, MES MUSIQUES and Écouter are shown whole, after their icons",
    )
    fun inALargeFontJouerMyMusicsAndListenAreShownWholeAfterTheirIcons(kind: BloxThemeKind) = extension.use {
        // Given a font twice as large, on 360 dp, the minuet selected, which may be listened to
        showHomeScreen(
            uiState.copy(listenable = listOf(MusicChoice.MINUET)),
            width = NARROW_PHONE,
            fontScale = 2f,
            kind = kind,
        )

        // Then each label is shown whole, on one line, after its icon, within its button and the screen
        listOf(
            hasTestTag(PLAY_TAG) to "Jouer",
            hasContentDescription("Mes musiques") to "MES MUSIQUES",
            hasContentDescription("Écouter Menuet") to "Écouter",
        ).forEach { (button, label) ->
            val bounds = onNode(button).getUnclippedBoundsInRoot()
            val icon = iconOf(button).getUnclippedBoundsInRoot()
            val layout = textLayout(label)
            assertThat(layout.lineCount).isEqualTo(1)
            // Drawn within its box: its paragraph may be laid out wider than the text, so not `hasVisualOverflow`
            assertThat(layout.getLineRight(0)).isLessThanOrEqualTo(layout.size.width.toFloat())
            val shown = onNode(hasText(label), useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertThat(icon.right).isLessThanOrEqualTo(shown.left)
            assertThat(icon.left).isGreaterThanOrEqualTo(bounds.left)
            assertThat(shown.right).isLessThanOrEqualTo(bounds.right)
            assertThat(bounds.right).isLessThanOrEqualTo(NARROW_PHONE)
        }
    }

    /** The icon of the button [button] matches, alone in it. */
    private fun ComposeContext.iconOf(button: SemanticsMatcher): SemanticsNodeInteraction =
        onNode(hasTestTag(HOME_ICON_TAG) and hasAnyAncestor(button), useUnmergedTree = true)

    private fun ComposeContext.showHomeScreen(
        state: HomeUiState = uiState,
        width: Dp = Dp.Unspecified,
        fontScale: Float = 1f,
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
        onListenGameMusic: () -> Unit = {},
        onCompose: () -> Unit = {},
        onMyMusics: () -> Unit = {},
        onSettings: () -> Unit = {},
        withoutInsets: Boolean = false,
    ) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = kind) {
                    HomeScreen(
                        modifier = Modifier
                            .width(width)
                            .then(
                                if (withoutInsets) Modifier.consumeWindowInsets(WindowInsets.safeDrawing) else Modifier,
                            ),
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
                        onSettings = onSettings,
                    )
                }
            }
        }
    }
}

/** A phone of 360 dp wide, as on the mockup: the narrowest of those BloX is drawn for. */
private val NARROW_PHONE = 360.dp

/** The rounding of an edge to the pixel, in dp: far below a pixel. */
private const val PIXEL_ROUNDING = 0.01f
