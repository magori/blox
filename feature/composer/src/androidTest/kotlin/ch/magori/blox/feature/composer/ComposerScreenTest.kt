package ch.magori.blox.feature.composer

import android.view.KeyEvent
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
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.model.TrackGrid
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
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

    private val music = FakeMusicPlayer()
    private val library = FakeMusicLibrary()
    private val viewModel = ComposerViewModel(music, library, musicKey = null, SavedStateHandle())

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
                    ComposerScreen(
                        viewModel = viewModel,
                        onBack = {},
                        onDeleted = {},
                        modifier = Modifier.size(360.dp, 640.dp),
                    )
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
                    onRename = {},
                    onDelete = {},
                    onOtherTrackShown = {},
                    onFollowPlayback = {},
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
                    onListen = {},
                    onSave = {},
                    onMessageShown = {},
                    dialogActions = DialogActions.None,
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

    @Test
    @DisplayName("Listen plays the music being edited, disabled while it has no note, and Stop stops it")
    fun listenPlaysThenStops() = extension.use {
        // Given a new music, with nothing to hear
        showEditor()
        onNodeWithContentDescription("Écouter").assertIsNotEnabled()
        cell("Mi bas, temps 1").performClick()
        // The play head moves on at each frame while the music plays: the clock is moved by hand.
        waitForIdle()
        mainClock.autoAdvance = false

        // When
        onNodeWithContentDescription("Écouter").assertIsEnabled().performClick()
        mainClock.advanceTimeByFrame()

        // Then
        assertThat(music.playing).isEqualTo(ScoreId.Personal("music-1"))

        // When
        onNodeWithContentDescription("Arrêter").performClick()
        mainClock.advanceTimeByFrame()

        // Then
        assertThat(music.playing).isNull()
        onNodeWithContentDescription("Écouter").assertExists()
    }

    @Test
    @DisplayName("the bar played is read as such; choosing another stops following the playback, as the menu shows")
    fun theBarPlayedAndFollowingThePlayback() = extension.use {
        // Given a music listened to, in its first bar
        showEditor()
        cell("Mi bas, temps 1").performClick()
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithContentDescription("Écouter").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithContentDescription("Mesure 1").assertState("en lecture").assertIsSelected()

        // When
        onNodeWithContentDescription("Mesure 3").performClick()
        // The message comes in with an animation.
        mainClock.advanceTimeBy(MESSAGE_SHOWN_MILLIS)

        // Then
        onNodeWithContentDescription("Mesure 3").assertIsSelected()
        onNodeWithText("La grille ne suit plus la lecture", substring = true).assertExists()
        onNodeWithContentDescription("Plus d'options").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithText("Suivre la lecture").assertIsOff()
    }

    @Test
    @DisplayName("Stop stays within reach while the message about following the playback shows")
    fun stopStaysWithinReachUnderTheMessage() = extension.use {
        // Given a music listened to, and the message shown by choosing another bar
        showEditor()
        cell("Mi bas, temps 1").performClick()
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithContentDescription("Écouter").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithContentDescription("Mesure 3").performClick()
        mainClock.advanceTimeBy(MESSAGE_SHOWN_MILLIS)
        onNodeWithText("La grille ne suit plus la lecture", substring = true).assertExists()

        // When
        onNodeWithContentDescription("Arrêter").performTouchInput { click() }
        mainClock.advanceTimeByFrame()

        // Then
        assertThat(music.playing).isNull()
    }

    @Test
    @DisplayName("Save opens the window on the name proposed, which saves the music under the name typed")
    fun saveSavesTheMusicUnderTheNameTyped() = extension.use {
        // Given a new music with a note
        showEditor()
        cell("Mi bas, temps 1").performClick()

        // When
        onNodeWithContentDescription("Enregistrer les changements").performClick()
        onNodeWithText("Ma musique 1").assertExists()
        onNode(hasSetTextAction()).performTextReplacement("Thème du soir")
        onNodeWithText("Jouer cette musique pendant les parties").performClick()
        onNodeWithText("Enregistrer").performClick()
        waitForIdle()

        // Then
        val saved = runBlocking { library.musics.first() }
        assertThat(saved.map { it.name }).containsExactly("Thème du soir")
        assertThat(runBlocking { library.gameMusic.first() }?.name).isEqualTo("Thème du soir")
        onNodeWithText("Enregistrer la musique").assertDoesNotExist()
        onNodeWithText("Thème du soir").assertExists()
        onNodeWithText("non enregistrée").assertDoesNotExist()
    }

    @Test
    @DisplayName("the back gesture on a changed music asks first: Continue stays, Leave without saving leaves")
    fun theBackGestureAsksBeforeLeavingAChangedMusic() = extension.use {
        // Given a new music with a note
        var left = 0
        showEditor(onBack = { left++ })
        cell("Mi bas, temps 1").performClick()

        // When the system goes back
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

        // Then
        onNodeWithText("Quitter sans enregistrer ?").assertExists()

        // When
        onNodeWithText("Continuer").performClick()

        // Then
        onNodeWithText("Quitter sans enregistrer ?").assertDoesNotExist()
        assertThat(left).isZero()

        // When the back button asks again, and the answer is to leave
        onNodeWithContentDescription("Retour").performClick()
        onNodeWithText("Quitter sans enregistrer").performClick()
        waitForIdle()

        // Then
        assertThat(left).isEqualTo(1)
    }

    @Test
    @DisplayName("before its music is read, the back gesture leaves the editor, as the navigation would")
    fun theBackGestureLeavesBeforeTheMusicIsRead() = extension.use {
        // Given an editor on a saved music whose musics are never read
        val unread = object : MusicLibrary by library {
            override val musics: Flow<List<SavedMusic>> = flow { awaitCancellation() }
        }
        val opening = ComposerViewModel(music, unread, musicKey = "joyeux", SavedStateHandle())
        var backs = 0
        setContent {
            // The system back, as the navigation handles it, under the editor.
            NavigationBackHandler(
                state = rememberNavigationEventState(NavigationEventInfo.None),
                onBackCompleted = { backs++ },
            )
            BloxTheme {
                ComposerScreen(viewModel = opening, onBack = {}, onDeleted = {})
            }
        }
        waitForIdle()

        // When
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

        // Then
        runOnIdle { assertThat(backs).isEqualTo(1) }
    }

    private fun ComposeContext.showEditor(
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
        modifier: Modifier = Modifier,
        onBack: () -> Unit = {},
    ) {
        setContent {
            BloxTheme(kind = kind) {
                ComposerScreen(viewModel = viewModel, onBack = onBack, onDeleted = {}, modifier = modifier)
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
        const val MESSAGE_SHOWN_MILLIS = 500L
        const val BAR_TAG = "composer_bar"
        const val CELL_TAG = "composer_cell"
        val NARROWEST_PHONE = 360.dp
    }
}
