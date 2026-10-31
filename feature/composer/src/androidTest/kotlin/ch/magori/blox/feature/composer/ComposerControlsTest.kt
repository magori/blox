package ch.magori.blox.feature.composer

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.designsystem.component.isChoiceCheck
import ch.magori.blox.core.designsystem.text.assertWordsWhole
import ch.magori.blox.core.designsystem.text.textLayout
import ch.magori.blox.core.designsystem.theme.BloxButtonColors
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.feature.composer.model.EditorState
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.math.abs

/** The bar and the controls of the editor around its grid, as V33 draws them (P5.102). */
@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class ComposerControlsTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val viewModel = composerViewModel(FakeMusicPlayer(), FakeMusicLibrary())

    @Test
    @DisplayName("the bar of the editor reads Retour, the name to rename, Au hasard and the menu, in this order")
    fun theBarReadsInOrder() = extension.use {
        // Given a new music
        showComposer(viewModel)

        // Then the first four targets of the screen, from left to right on one line
        val bar = onAllNodes(hasClickAction()).fetchSemanticsNodes().take(4)
        assertThat(bar.map { it.spokenLabel() })
            .containsExactly("Retour", "Nouvelle musique, renommer", "Au hasard", "Plus d'options")
        assertThat(bar.map { it.boundsInRoot.left }).isSorted()
        val middle = bar.first().boundsInRoot.center.y
        assertThat(bar.map { it.boundsInRoot.center.y }).allSatisfy { assertThat(it).isCloseTo(middle, within(1f)) }

        // When the menu opens
        onNodeWithContentDescription("Plus d'options").performClick()

        // Then it is the menu of the design system, read as a pane, its choices checked
        val menu = SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Menu")
        onNode(menu).assertExists()
        onNode(hasText("Renommer") and hasAnyAncestor(menu)).assertExists()
        onNode(hasText("Suivre la lecture") and hasAnyAncestor(menu)).assertIsOn()
        onNode(hasText("Montrer l'autre piste en filigrane") and hasAnyAncestor(menu)).assertIsOn()
    }

    @Test
    @DisplayName("the open track is filled in its colour and checked")
    fun theOpenTrackIsFilledInItsColourAndChecked() = extension.use {
        // Given a new music, on the melody
        lateinit var palette: BloxPalette
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                palette = BloxTheme.palette
                ComposerScreen(viewModel = viewModel, onBack = {}, onDeleted = {})
            }
        }

        // Then the melody is the radio button chosen, in its colour, and checked; the bass is not
        assertTrackOpen("Mélodie", palette.melody.tab, other = "Basse")

        // When
        track("Basse").performClick()

        // Then
        assertTrackOpen("Basse", palette.bass.tab, other = "Mélodie")
    }

    @Test
    @DisplayName("the bars stand on two rows of four, the bar played marked and read en lecture")
    fun theBarsStandOnTwoRowsOfFour() = extension.use {
        // Given a music listened to, in its first bar
        lateinit var buttons: BloxButtonColors
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                buttons = BloxTheme.buttons
                ComposerScreen(viewModel = viewModel, onBack = {}, onDeleted = {})
            }
        }
        onNodeWithContentDescription("Mi bas, temps 1").performClick()
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithContentDescription("Écouter").performClick()
        mainClock.advanceTimeByFrame()

        // Then eight radio buttons, Mesure 1 to 4 on a row, 5 to 8 under them, each under the one four before
        val bars = (1..8).map { bar(it).assertRole(Role.RadioButton).getUnclippedBoundsInRoot() }
        val (top, bottom) = bars.chunked(4)
        assertThat(top.map { it.top }.distinct()).hasSize(1)
        assertThat(bottom.map { it.top }.distinct()).hasSize(1)
        assertThat(bottom.first().top).isGreaterThanOrEqualTo(top.first().bottom)
        assertThat(bottom.map { it.left }).isEqualTo(top.map { it.left })

        // And the bar played, the one selected, read « sélectionnée, en lecture » (its own state does not hide that it
        // is selected) and marked under its number, the others not
        bar(1).assertIsSelected().assertStateDescription("sélectionnée, en lecture")
        bar(2).assertIsNotSelected().assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        assertThat(markOf(bar(1))).isEqualTo(buttons.selected.ink)
        assertThat(markOf(bar(2))).isNotEqualTo(buttons.secondary.ink)

        // When another bar is chosen while the first is played
        bar(3).performClick()
        mainClock.advanceTimeByFrame()

        // Then the bar played alone is read « en lecture », the bar chosen as TalkBack reads a selected radio button
        bar(1).assertIsNotSelected().assertStateDescription("en lecture")
        bar(3).assertIsSelected().assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    @DisplayName("from the keyboard, « … » takes the focus back once the window Renommer opened closes")
    fun theFocusGoesBackToTheMenuOnceTheWindowCloses() = extension.use {
        // Given the keyboard in use, and the editor of a new music
        setContent {
            val inputMode = LocalInputModeManager.current
            LaunchedEffect(Unit) { inputMode.requestInputMode(InputMode.Keyboard) }
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                ComposerScreen(viewModel = viewModel, onBack = {}, onDeleted = {})
            }
        }
        onNodeWithContentDescription(MENU).performSemanticsAction(SemanticsActions.OnClick)

        // When Renommer opens its window, then the window is closed without renaming
        onNode(hasText("Renommer") and hasClickAction()).performSemanticsAction(SemanticsActions.OnClick)
        onNode(isDialog()).assertExists()
        onNode(hasText("Annuler") and hasAnyAncestor(isDialog())).performSemanticsAction(SemanticsActions.OnClick)
        onNode(isDialog()).assertDoesNotExist()

        // Then
        onNodeWithContentDescription(MENU).assertIsFocused()
    }

    @Test
    @DisplayName("the menu of a saved music offers Supprimer in danger; Renommer opens the window of the name")
    fun theMenuOfASavedMusicOffersSupprimerInDanger() = extension.use {
        // Given a saved music, « Fête »
        val library = FakeMusicLibrary()
        val id = ScoreId.Personal("fete")
        val draft = EditorState.new().touch(Pitch.parse("C5"), eighth = 0).draft
        runBlocking { library.save(SavedMusic(id, "Fête", draft.category, draft.scale, draft.toScore(id))) }
        lateinit var palette: BloxPalette
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                palette = BloxTheme.palette
                ComposerScreen(
                    viewModel = composerViewModel(FakeMusicPlayer(), library, musicKey = "fete"),
                    onBack = {},
                    onDeleted = {},
                )
            }
        }

        // When
        onNodeWithContentDescription(MENU).performClick()

        // Then its items, one under the other, Supprimer last, alone in danger
        val items = listOf("Renommer", "Suivre la lecture", "Montrer l'autre piste en filigrane", "Supprimer")
        val tops = items.map { onNode(hasText(it) and hasClickAction()).fetchSemanticsNode().boundsInRoot.top }
        assertThat(tops).isSorted().doesNotHaveDuplicates()
        assertThat(inkOf("Supprimer")).isEqualTo(palette.danger)
        assertThat(inkOf("Renommer")).isNotEqualTo(palette.danger)

        // When
        onNode(hasText("Renommer") and hasClickAction()).performClick()

        // Then the window of the name, on the name of the music
        onNode(hasText("Renommer la musique") and hasAnyAncestor(isDialog())).assertExists()
        onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).assert(hasText("Fête"))
    }

    @Test
    @DisplayName("no Supprimer for a new music")
    fun noDeleteForANewMusic() = extension.use {
        // Given a new music
        showComposer(viewModel)

        // When
        onNodeWithContentDescription(MENU).performClick()

        // Then
        onNode(hasText("Renommer") and hasClickAction()).assertExists()
        onNode(hasText("Supprimer")).assertDoesNotExist()
    }

    @Test
    @DisplayName("Annuler, Copier and Effacer are disabled when they have no effect")
    fun theToolsAreDisabledWithoutEffect() = extension.use {
        // Given a new music: nothing to undo nor to clear
        showComposer(viewModel)

        // Then Annuler stands after Poser and Allonger, on their line; Copier above Effacer, after the bars
        val lengthen = onNode(hasText("Allonger") and isSelectable()).getUnclippedBoundsInRoot()
        val undo = onNode(hasText("Annuler") and hasClickAction()).getUnclippedBoundsInRoot()
        assertThat(undo.left).isGreaterThanOrEqualTo(lengthen.right)
        assertThat(undo.middle()).isCloseTo(lengthen.middle(), within(1f))
        val copy = onNodeWithContentDescription("Copier la mesure").getUnclippedBoundsInRoot()
        val clear = onNodeWithContentDescription("Effacer la mesure").getUnclippedBoundsInRoot()
        val fourth = onNodeWithContentDescription("Mesure 4").getUnclippedBoundsInRoot()
        assertThat(copy.bottom).isLessThanOrEqualTo(clear.top)
        assertThat(copy.left).isEqualTo(clear.left).isGreaterThanOrEqualTo(fourth.right)
        onNode(hasText("Annuler") and hasClickAction()).assertIsNotEnabled()
        onNodeWithContentDescription("Effacer la mesure").assertIsNotEnabled()
        onNodeWithContentDescription("Copier la mesure").assertIsEnabled()

        // When a note is placed, then the bar cleared
        onNodeWithContentDescription("Mi bas, temps 1").performClick()
        onNodeWithContentDescription("Effacer la mesure").assertIsEnabled().performClick()

        // Then it can be undone
        onNode(hasText("Annuler") and hasClickAction()).assertIsEnabled()

        // When the last bar is shown
        onNodeWithContentDescription("Mesure 8").performClick()

        // Then it has no bar after it to copy it over
        onNodeWithContentDescription("Mesure 8").assertIsSelected()
        onNodeWithContentDescription("Copier la mesure").assertIsNotEnabled()
    }

    @Test
    @DisplayName("Enregistrer is the only button in accent on the editor, its dot shown while the music has changed")
    fun saveIsTheOnlyButtonInAccent() = extension.use {
        // Given a new music, not changed yet
        lateinit var palette: BloxPalette
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                palette = BloxTheme.palette
                ComposerScreen(viewModel = viewModel, onBack = {}, onDeleted = {})
            }
        }

        // Then Enregistrer alone, of all the buttons, is in accent, without its dot
        assertSaveAloneInAccent(palette.accent)
        onNode(hasTestTag(SAVE_DOT_TAG), useUnmergedTree = true).assertDoesNotExist()

        // When a note is placed
        onNodeWithContentDescription("Mi bas, temps 1").performClick()
        waitForIdle()

        // Then it is still the only one, its dot before its label
        assertSaveAloneInAccent(palette.accent)
        val dot = onNode(hasTestTag(SAVE_DOT_TAG), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val label = onNode(hasText("Enregistrer"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertThat(dot.right).isLessThanOrEqualTo(label.left)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName(
        "on 360 by 640 dp in a font twice as large every control is 48 dp and the grid keeps rows of 26 dp, scrolling",
    )
    fun everyControlHoldsOnASmallPhoneWithALargeFont(kind: BloxThemeKind) = extension.use {
        // Given a small phone, the font twice as large
        showComposer(viewModel, kind, Modifier.size(NARROWEST_PHONE, SMALL_PHONE_HEIGHT), fontScale = 2f)

        // Then every control but the cells is 48 dp wide and high at least, its label whole, within the phone
        val controls = onAllNodes(hasClickAction() and !hasTestTag(CELL_TAG))
        val count = controls.fetchSemanticsNodes().size
        assertThat(count).isGreaterThan(MIN_BUTTONS)
        repeat(count) { index ->
            controls[index].assertWidthIsAtLeast(
                BloxSizes.minTouchTarget,
            ).assertHeightIsAtLeast(BloxSizes.minTouchTarget)
            val bounds = controls[index].getUnclippedBoundsInRoot()
            assertThat(bounds.left).isGreaterThanOrEqualTo(0.dp)
            assertThat(bounds.right).isLessThanOrEqualTo(NARROWEST_PHONE)
        }
        listOf("Poser", "Allonger", "Annuler", "Copier", "Effacer", "Enregistrer").forEach { label ->
            assertWordsWhole(textLayout(label))
            val button = onNode(hasClickAction() and hasText(label)).getUnclippedBoundsInRoot()
            val text = onNode(hasText(label), useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertThat(text.left).describedAs(label).isGreaterThanOrEqualTo(button.left)
            assertThat(text.right).describedAs(label).isLessThanOrEqualTo(button.right)
            assertThat(text.top).describedAs(label).isGreaterThanOrEqualTo(button.top)
            assertThat(text.bottom).describedAs(label).isLessThanOrEqualTo(button.bottom)
        }

        // And the grid shows three lines at least, each 26 dp high, the others scrolling into sight
        // The scroll of the grid, the innermost around its cells: the page may scroll too.
        val grid = onAllNodes(hasScrollAction() and hasAnyDescendant(hasTestTag(CELL_TAG)))
            .fetchSemanticsNodes()
            .last()
            .boundsInRoot
        assertThat(with(density) { grid.height.toDp() }).isGreaterThanOrEqualTo(MIN_ROW * LINES_IN_SIGHT)
        val cells = onAllNodes(hasTestTag(CELL_TAG))
        val lowest = cells.fetchSemanticsNodes().size - 1
        cells[0].assertHeightIsAtLeast(MIN_ROW - CELL_GAP)
        cells[lowest].performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(MIN_ROW - CELL_GAP)

        // When a note is placed, Enregistrer bearing its dot
        cells[lowest].performClick()
        waitForIdle()

        // Then Enregistrer stays whole on its line, its dot before it
        val save = textLayout("Enregistrer")
        assertThat(save.lineCount).isEqualTo(1)
        assertWordsWhole(save)
    }

    /**
     * Asserts that Enregistrer is filled in [accent], where its fill shows, at 8 dp from its left edge in its middle, and
     * that no other button of the screen is there.
     */
    private fun ComposeContext.assertSaveAloneInAccent(accent: Color) {
        val save = onNode(hasText("Enregistrer") and hasClickAction())
        assertThat(fillOf(save)).allSatisfy { assertThat(it.isNear(accent)).describedAs("$it").isTrue() }
        val buttons = onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button) and !hasText("Enregistrer"),
        )
        val count = buttons.fetchSemanticsNodes().size
        assertThat(count).isGreaterThan(MIN_BUTTONS)
        repeat(count) { index ->
            assertThat(fillOf(buttons[index]))
                .describedAs(buttons[index].fetchSemanticsNode().spokenLabel())
                .noneSatisfy { assertThat(it.isNear(accent)).isTrue() }
        }
    }

    /**
     * The colours of [button] where its fill shows, clear of its label and its frame: 8 dp from its left edge, in its
     * middle, and a pixel above and below. The button is brought into sight first: the page may scroll, and a button cut
     * by the edge of the screen would be captured a pixel high.
     */
    private fun ComposeContext.fillOf(button: SemanticsNodeInteraction): List<Color> {
        button.performScrollTo()
        waitForIdle()
        val image = button.captureToImage().toPixelMap()
        val x = with(density) { FILL_INSET.roundToPx() }
        return listOf(-1, 0, 1).map { image[x, image.height / 2 + it] }
    }

    /** Whether this colour is [other], each channel within [CHANNEL_TOLERANCE]: a drawing rounds its colours. */
    private fun Color.isNear(other: Color): Boolean = listOf(red - other.red, green - other.green, blue - other.blue)
        .all { abs(it) <= CHANNEL_TOLERANCE }

    /**
     * Asserts that the track [label] is the one open, read as the radio button chosen, filled in [fill] and checked, and
     * that the track [other] is not.
     */
    private fun ComposeContext.assertTrackOpen(
        label: String,
        fill: Color,
        other: String,
    ) {
        track(label).assertIsSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        track(other).assertIsNotSelected()
        val segment = track(label).getUnclippedBoundsInRoot()
        val check = onNode(isChoiceCheck() and hasAnySibling(hasText(label)), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        onNode(isChoiceCheck() and hasAnySibling(hasText(other)), useUnmergedTree = true).assertDoesNotExist()
        // Between the left of the segment and its check, in the middle of the check: the fill of the segment.
        val (x, y) = with(density) {
            ((check.left - segment.left) / 2).roundToPx() to ((check.top + check.bottom) / 2 - segment.top).roundToPx()
        }
        assertThat(track(label).captureToImage().toPixelMap()[x, y]).isEqualTo(fill)
    }

    /** The button of the bar [number], counted from 1. */
    private fun ComposeContext.bar(number: Int): SemanticsNodeInteraction =
        onNodeWithContentDescription("Mesure $number")

    /** The colour of [bar] where the bar played bears its mark: in its middle, 6 dp above its bottom edge, 1 dp up. */
    private fun ComposeContext.markOf(bar: SemanticsNodeInteraction): Color {
        val image = bar.captureToImage().toPixelMap()
        val y = image.height - with(density) { (MARK_INSET + 1.dp).roundToPx() } - 1
        return image[image.width / 2, y]
    }

    private fun SemanticsNodeInteraction.assertStateDescription(state: String): SemanticsNodeInteraction =
        assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state))

    /** The colour of the text [text], as it is drawn. */
    private fun ComposeContext.inkOf(text: String): Color = textLayout(text).layoutInput.style.color

    private fun SemanticsNodeInteraction.assertRole(role: Role): SemanticsNodeInteraction =
        assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, role))

    /** The tab of the track [label]. */
    private fun ComposeContext.track(label: String): SemanticsNodeInteraction =
        onNode(hasText(label) and isSelectable())

    /** The height of the middle of this rectangle, in dp. */
    private fun DpRect.middle(): Float = ((top + bottom) / 2).value

    /** What names this node first for TalkBack: its description, else its text. */
    private fun SemanticsNode.spokenLabel(): String? =
        config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
            ?: config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text

    private companion object {
        /** How far above the bottom edge of a bar its mark stands. */
        val MARK_INSET = 6.dp

        /** Where the fill of a button shows, from its left edge, in its middle. */
        val FILL_INSET = 8.dp

        /** The buttons of the editor besides Enregistrer, at the least: the bar, the tools, the tempo, Écouter. */
        const val MIN_BUTTONS = 9

        /** « … », the menu of the bar. */
        const val MENU = "Plus d'options"

        /** How far a channel of a colour drawn may be from the one asked, out of 1: the rounding of the drawing. */
        const val CHANNEL_TOLERANCE = 0.02f

        /** A small phone: 640 dp high. */
        val SMALL_PHONE_HEIGHT = 640.dp

        /** The least height of a line of the grid, and the gap under its cells. */
        val MIN_ROW = 26.dp
        val CELL_GAP = 1.dp

        /** The lines of the grid in sight, at the least, however small the phone and large the font. */
        const val LINES_IN_SIGHT = 3
    }
}
