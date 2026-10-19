package ch.magori.blox.feature.composer

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.model.TrackGrid
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class ComposerScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @JvmField
    @RegisterExtension
    val recompositions = RecompositionRecorder()

    private val viewModel = ComposerViewModel()

    @Test
    @DisplayName("touching an empty cell places a note, and touching it again erases it")
    fun touchingACellPlacesThenErasesANote() = extension.use {
        // Given
        showEditor()

        // When
        cell("Mi bas, temps 1").performTouchInput { click() }

        // Then
        cell("Mi bas, temps 1").assertState("note")

        // When
        cell("Mi bas, temps 1").performTouchInput { click() }

        // Then
        cell("Mi bas, temps 1").assertState("vide")
    }

    @Test
    @DisplayName("a touch cancelled by the system, as the back gesture does, places no note")
    fun aCancelledTouchPlacesNoNote() = extension.use {
        // Given
        showEditor()

        // When
        cell("Mi bas, temps 1").performTouchInput {
            down(center)
            cancel()
        }

        // Then
        cell("Mi bas, temps 1").assertState("vide")
    }

    @Test
    @DisplayName("on a small phone with the largest font, every cell is 24 dp wide and high at least")
    fun everyCellIsLargeEnoughOnASmallPhone() = extension.use {
        // Given a phone of 360 × 640 dp, the font twice as large
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                    ComposerScreen(viewModel = viewModel, onBack = {}, modifier = Modifier.size(360.dp, 640.dp))
                }
            }
        }

        // Then: 24 dp, the minimum of WCAG 2.2 (2.5.8); the lines that do not fit scroll
        val cells = onAllNodes(hasTestTag(CELL_TAG))
        val count = cells.fetchSemanticsNodes().size
        assertThat(count).isEqualTo(15 * 8)
        repeat(count) { index -> cells[index].assertWidthIsAtLeast(24.dp).assertHeightIsAtLeast(24.dp) }
    }

    @Test
    @DisplayName("sliding along a line places a note held up to the cell where the finger is lifted")
    fun slidingPlacesALongNote() = extension.use {
        // Given
        showEditor()
        val from = cell("Sol bas, temps 1").fetchSemanticsNode().boundsInRoot
        val to = cell("Sol bas, temps 2 et demi").fetchSemanticsNode().boundsInRoot

        // When
        cell("Sol bas, temps 1").performTouchInput {
            down(center)
            repeat(SLIDE_STEPS) { moveBy(Offset((to.center.x - from.center.x) / SLIDE_STEPS, 0f)) }
            up()
        }

        // Then
        cell("Sol bas, temps 1").assertState("note")
        cell("Sol bas, temps 1 et demi").assertState("note tenue")
        cell("Sol bas, temps 2 et demi").assertState("note tenue")
        cell("Sol bas, temps 3").assertState("vide")
    }

    @Test
    @DisplayName("the tabs open the grid of the bass or of the melody, with the notes of the other track in watermark")
    fun theTabsOpenEachTrack() = extension.use {
        // Given a note of C3 on the bass
        showEditor()
        onNodeWithText("Basse").performClick()
        cell("Do haut, temps 1").performClick()

        // When
        onNodeWithText("Mélodie").performClick()

        // Then the melody shows it in watermark, on the line of C5
        onNodeWithText("Mélodie").assertIsSelected()
        onNodeWithText("Basse").assertIsNotSelected()
        cell("Do haut, temps 1").assertState("vide, note de l'autre piste")
    }

    @Test
    @DisplayName("the menu hides the watermark of the other track")
    fun theMenuHidesTheWatermark() = extension.use {
        // Given a note of C3 on the bass, shown in watermark on the melody
        showEditor()
        onNodeWithText("Basse").performClick()
        cell("Do haut, temps 1").performClick()
        onNodeWithText("Mélodie").performClick()

        // When
        onNodeWithContentDescription("Plus d'options").performClick()
        onNodeWithText("Montrer l'autre piste en filigrane").assertIsOn().performClick()

        // Then
        cell("Do haut, temps 1").assertState("vide")
        onNodeWithContentDescription("Plus d'options").performClick()
        onNodeWithText("Montrer l'autre piste en filigrane").assertIsOff()
    }

    @Test
    @DisplayName("a toolbar button without effect is disabled")
    fun aToolbarButtonWithoutEffectIsDisabled() = extension.use {
        // Given a new music: nothing to undo nor to clear
        showEditor()
        onNodeWithText("Annuler").assertIsNotEnabled()
        onNodeWithContentDescription("Effacer la mesure").assertIsNotEnabled()
        onNodeWithContentDescription("Copier la mesure").assertIsEnabled()

        // When a note is placed, then the bar cleared
        cell("Mi bas, temps 1").performClick()
        onNodeWithContentDescription("Effacer la mesure").assertIsEnabled().performClick()

        // Then it can be undone
        onNodeWithText("Annuler").assertIsEnabled()

        // When the last bar is shown
        onNodeWithContentDescription("Mesure 8").performClick()

        // Then it has no bar after it to copy it over
        onNodeWithContentDescription("Mesure 8").assertIsSelected()
        onNodeWithContentDescription("Copier la mesure").assertIsNotEnabled()
    }

    @Test
    @DisplayName("TalkBack names each cell by its note, its octave and its beat, and says what touching it does")
    fun talkBackNamesEachCell() = extension.use {
        // Given
        showEditor()

        // Then
        cell("Sol haut, temps 3 et demi").assertState("vide").assertActionLabel("poser une note")
        onNodeWithContentDescription("Do très haut").assertExists()

        // When a note is placed, then the mode is Lengthen
        cell("Sol haut, temps 3 et demi").performClick()

        // Then
        cell("Sol haut, temps 3 et demi").assertActionLabel("effacer la note")
        onNodeWithText("Allonger").performClick()
        cell("Sol haut, temps 4").assertActionLabel("allonger jusqu'ici")
    }

    @Test
    @DisplayName("TalkBack says a line out of the scale, and two sixteenth notes in a cell")
    fun talkBackSaysOutOfScaleAndSixteenths() = extension.use {
        // Given an F sharp out of C major, and two sixteenth notes of G5
        val draft = ScoreDraft.new()
        val melody = listOf(Note(Pitch.parse("F#5"), 0.5), Note(Pitch.parse("G5"), 0.25), Note(Pitch.parse("G5"), 0.25))
        val opened = EditorState.open(draft.copy(melody = TrackGrid.playing(Track.MELODY, draft.scale, melody)))
        setContent {
            BloxTheme {
                ComposerContent(
                    uiState = ComposerUiState(opened, showOtherTrack = true),
                    onBack = {},
                    onOtherTrackShown = {},
                    onTrackChosen = {},
                    onModeChosen = {},
                    onUndo = {},
                    onCopyBar = {},
                    onClearBar = {},
                    onCellTouched = { _, _ -> },
                    onCellSlid = { _, _, _ -> },
                    onBarChosen = {},
                    onSlower = {},
                    onFaster = {},
                )
            }
        }

        // Then
        onNodeWithContentDescription("Fa dièse haut, hors gamme").assertExists()
        cell("Fa dièse haut, hors gamme, temps 1").assertState("note")
        cell("Sol haut, temps 1 et demi").assertState("double croche : note puis note")
    }

    @Test
    @DisplayName("touching a cell recomposes its line only, not the whole grid")
    fun touchingACellRecomposesItsLineOnly() = extension.use {
        // Given
        showEditor()
        waitForIdle()
        recompositions.start()

        // When
        cell("Mi bas, temps 1").performClick()
        waitForIdle()

        // Then
        assertThat(recompositions.recomposed.filter { it.contains(".GridLine ") }).hasSize(1)
    }

    @Test
    @DisplayName("the tempo buttons change the tempo shown")
    fun theTempoButtonsChangeTheTempo() = extension.use {
        // Given
        showEditor()

        // When
        onNodeWithContentDescription("Accélérer").performClick()

        // Then
        onNodeWithText("130 temps/min").assertExists()
    }

    @Test
    @DisplayName("every button is 48 dp wide and high at least, but the bars and the cells, 48 dp high for the bars")
    fun everyTargetIsLargeEnough() = extension.use {
        // Given the narrowest phone, in the pixel font, the widest
        showEditor(BloxThemeKind.ORIGINAL, Modifier.width(NARROWEST_PHONE))

        // Then: 48 dp visible, a rule of BloX. Eight bars do not fit 48 dp wide on 360 dp, nor 8 cells and 15 lines
        val targets = onAllNodes(hasClickAction() and !hasTestTag(BAR_TAG) and !hasTestTag(CELL_TAG))
        val count = targets.fetchSemanticsNodes().size
        assertThat(count).isGreaterThan(1)
        repeat(count) { index ->
            targets[index].assertWidthIsAtLeast(
                BloxSizes.minTouchTarget,
            ).assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        }
        val bars = onAllNodes(hasTestTag(BAR_TAG))
        assertThat(bars.fetchSemanticsNodes()).hasSize(8)
        repeat(8) { index -> bars[index].assertHeightIsAtLeast(BloxSizes.minTouchTarget) }
    }

    private fun ComposeContext.showEditor(
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
        modifier: Modifier = Modifier,
    ) {
        setContent {
            BloxTheme(kind = kind) {
                ComposerScreen(viewModel = viewModel, onBack = {}, modifier = modifier)
            }
        }
    }

    private fun ComposeContext.cell(name: String): SemanticsNodeInteraction = onNodeWithContentDescription(name)

    private fun SemanticsNodeInteraction.assertState(state: String): SemanticsNodeInteraction =
        assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state))

    private fun SemanticsNodeInteraction.assertActionLabel(label: String): SemanticsNodeInteraction =
        assert(SemanticsMatcher("action « $label »") { it.config[SemanticsActions.OnClick].label == label })

    private companion object {
        const val SLIDE_STEPS = 10
        const val BAR_TAG = "composer_bar"
        const val CELL_TAG = "composer_cell"
        val NARROWEST_PHONE = 360.dp
    }
}
