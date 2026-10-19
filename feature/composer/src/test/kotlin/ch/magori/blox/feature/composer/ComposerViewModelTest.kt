package ch.magori.blox.feature.composer

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.Playback
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.music.Music
import ch.magori.blox.feature.composer.model.Cell
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.Tempo
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.model.pitch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ComposerViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val music = FakeMusicPlayer()
    private val viewModel = ComposerViewModel(music)

    @Test
    fun `the editor opens a new music, with the other track shown in watermark`() {
        // Then
        assertThat(viewModel.uiState.value).isEqualTo(ComposerUiState(EditorState.new(), showOtherTrack = true))
    }

    @Test
    fun `touching a cell or sliding along a line edits the open track, in the selected bar`() {
        // When
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onCellSlid(pitch("G4"), fromEighth = 2, toEighth = 5)

        // Then
        val expected = EditorState.new().touch(pitch("E4"), eighth = 0).slide(pitch("G4"), fromEighth = 2, toEighth = 5)
        assertThat(viewModel.uiState.value.editor).isEqualTo(expected)
    }

    @Test
    fun `the tabs, the bar buttons and the mode choose what the grid shows and what a touch does`() {
        // When
        viewModel.onTrackChosen(Track.BASS)
        viewModel.onBarChosen(5)
        viewModel.onModeChosen(EditMode.LENGTHEN)

        // Then
        val editor = viewModel.uiState.value.editor
        assertThat(editor.track).isEqualTo(Track.BASS)
        assertThat(editor.bar).isEqualTo(5)
        assertThat(editor.mode).isEqualTo(EditMode.LENGTHEN)
    }

    @Test
    fun `the toolbar copies the bar, clears it, and undoes the last of them`() {
        // Given a note in the first bar
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        val written = viewModel.uiState.value.editor

        // When
        viewModel.onCopyBar()
        val copied = viewModel.uiState.value.editor
        viewModel.onClearBar()
        val cleared = viewModel.uiState.value.editor
        viewModel.onUndo()

        // Then
        assertThat(copied).isEqualTo(written.copyBar())
        assertThat(cleared).isEqualTo(copied.clearBar())
        assertThat(viewModel.uiState.value.editor).isEqualTo(cleared.undo())
    }

    @Test
    fun `a toolbar action without effect, its button disabled, does nothing when it comes all the same`() {
        // Given an empty music on its last bar: nothing to clear, to undo, nor a bar after it to copy over
        viewModel.onBarChosen(7)
        val before = viewModel.uiState.value

        // When
        viewModel.onCopyBar()
        viewModel.onClearBar()
        viewModel.onUndo()

        // Then
        assertThat(viewModel.uiState.value).isEqualTo(before)
    }

    @Test
    fun `the tempo buttons speed the music up and slow it down`() {
        // When
        viewModel.onFaster()
        viewModel.onFaster()
        viewModel.onSlower()

        // Then
        assertThat(viewModel.uiState.value.editor.draft.tempo).isEqualTo(Tempo(130))
    }

    @Test
    fun `the fastest tempo button, disabled, does nothing when it comes all the same, and keeps the undo`() {
        // Given the fastest tempo, then a cleared bar that can be undone
        repeat(TEMPO_STEPS) { viewModel.onFaster() }
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onClearBar()
        val fastest = viewModel.uiState.value

        // When
        viewModel.onFaster()

        // Then
        assertThat(fastest.editor.draft.tempo).isEqualTo(Tempo(Tempo.FASTEST))
        assertThat(viewModel.uiState.value).isEqualTo(fastest)
    }

    @Test
    fun `the slowest tempo button, disabled, does nothing when it comes all the same, and keeps the undo`() {
        // Given the slowest tempo, then a cleared bar that can be undone
        repeat(TEMPO_STEPS) { viewModel.onSlower() }
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onClearBar()
        val slowest = viewModel.uiState.value

        // When
        viewModel.onSlower()

        // Then
        assertThat(slowest.editor.draft.tempo).isEqualTo(Tempo(Tempo.SLOWEST))
        assertThat(viewModel.uiState.value).isEqualTo(slowest)
    }

    @Test
    fun `the menu hides the other track in watermark, and shows it again`() {
        // When
        viewModel.onOtherTrackShown(false)
        val hidden = viewModel.uiState.value.showOtherTrack
        viewModel.onOtherTrackShown(true)

        // Then
        assertThat(hidden).isFalse()
        assertThat(viewModel.uiState.value.showOtherTrack).isTrue()
    }

    @Test
    fun `listening plays the music being edited, from its first cell`() {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)

        // When
        viewModel.onListen()

        // Then
        val state = viewModel.uiState.value
        assertThat(music.playedScores).containsExactly(state.editor.draft.toScore(EDITED_SCORE_ID))
        assertThat(state.played).isEqualTo(Cell(bar = 0, eighth = 0))
    }

    @Test
    fun `listening again stops the music, and no cell is played`() {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onListen()

        // When
        viewModel.onListen()

        // Then
        assertThat(music.playing).isNull()
        assertThat(viewModel.uiState.value.played).isNull()
    }

    @Test
    fun `the played cell follows the frames of the screen, a cell each eighth note at the tempo of the music`() {
        // Given a music at 120 beats per minute, listened to
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onListen()

        // When the first frame, then a quarter of a second later, and two seconds and a half more
        viewModel.onFrame(1_000_000_000)
        viewModel.onFrame(1_250_000_000)
        val second = viewModel.uiState.value.played
        viewModel.onFrame(3_750_000_000)

        // Then
        assertThat(second).isEqualTo(Cell(bar = 0, eighth = 1))
        assertThat(viewModel.uiState.value.played).isEqualTo(Cell(bar = 1, eighth = 3))
    }

    @Test
    fun `following the playback, as it does at first, the grid shows the bar being played`() {
        // Given a note in the fourth bar, shown on the grid, listened to from the start
        viewModel.onBarChosen(3)
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onListen()
        val atStart = viewModel.uiState.value

        // When two seconds later, at 120 beats per minute: the second bar
        viewModel.onFrame(0)
        viewModel.onFrame(2_000_000_000)

        // Then
        assertThat(atStart.followPlayback).isTrue()
        assertThat(atStart.editor.bar).isEqualTo(0)
        assertThat(viewModel.uiState.value.editor.bar).isEqualTo(1)
    }

    @Test
    fun `choosing another bar than the one played stops following the playback, and a message says so`() {
        // Given a music listened to, in its first bar
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onListen()
        viewModel.onFrame(0)

        // When the bar played is chosen, then another one, and the music goes on to its second bar
        viewModel.onBarChosen(0)
        val samePlayed = viewModel.uiState.value
        viewModel.onBarChosen(5)
        viewModel.onFrame(2_000_000_000)

        // Then
        assertThat(samePlayed.followPlayback).isTrue()
        assertThat(samePlayed.message).isNull()
        val state = viewModel.uiState.value
        assertThat(state.followPlayback).isFalse()
        assertThat(state.message).isEqualTo(ComposerMessage.FOLLOW_STOPPED)
        assertThat(state.editor.bar).isEqualTo(5)
        assertThat(state.played).isEqualTo(Cell(bar = 1, eighth = 0))
    }

    @Test
    fun `a message shown goes away`() {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onListen()
        viewModel.onBarChosen(5)

        // When
        viewModel.onMessageShown()

        // Then
        assertThat(viewModel.uiState.value.message).isNull()
    }

    @Test
    fun `the menu stops following the playback, and follows it again from the bar played`() {
        // Given a music listened to, in its first bar
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onListen()
        viewModel.onFrame(0)

        // When following is unchecked, the music goes on to its second bar, then following is checked again
        viewModel.onFollowPlayback(false)
        viewModel.onFrame(2_000_000_000)
        val unfollowed = viewModel.uiState.value
        viewModel.onFollowPlayback(true)

        // Then
        assertThat(unfollowed.followPlayback).isFalse()
        assertThat(unfollowed.editor.bar).isEqualTo(0)
        assertThat(viewModel.uiState.value.followPlayback).isTrue()
        assertThat(viewModel.uiState.value.editor.bar).isEqualTo(1)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(NotesChange::class)
    fun `changing the notes while listening stops the music, never playing a version gone`(change: NotesChange) {
        // Given a note in the first bar, the second bar cleared, which can be undone, and the music listened to, the
        // first bar shown
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onBarChosen(1)
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onClearBar()
        viewModel.onListen()

        // When
        change.apply(viewModel)

        // Then
        assertThat(music.calls).containsExactly(PLAY, "stop")
        assertThat(viewModel.uiState.value.played).isNull()
    }

    @Test
    fun `the tabs, the mode and the watermark leave the music playing`() {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onListen()

        // When
        viewModel.onTrackChosen(Track.BASS)
        viewModel.onModeChosen(EditMode.LENGTHEN)
        viewModel.onOtherTrackShown(false)

        // Then
        assertThat(music.calls).containsExactly(PLAY)
        assertThat(viewModel.uiState.value.played).isNotNull()
    }

    @Test
    fun `changing the tempo while listening plays the music on, at the new tempo`() {
        // Given a music at 120 beats per minute, listened to
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onListen()

        // When
        viewModel.onFaster()
        val faster = music.tempo
        viewModel.onSlower()
        viewModel.onSlower()

        // Then
        assertThat(faster).isEqualTo(130.0 / 120)
        assertThat(music.tempo).isEqualTo(110.0 / 120)
        assertThat(music.playing).isEqualTo(EDITED_SCORE_ID)
        assertThat(viewModel.uiState.value.played).isNotNull()
    }

    @Test
    fun `a music that stops as soon as it is played, as when the audio focus is refused, offers to listen again`() {
        // Given a player whose playbacks have stopped already
        val refusing = object : MusicPlayer {
            override fun play(music: Music): Playback = EndedPlayback

            override fun setTempoFactor(factor: Double) = Unit

            override fun prepareTempo(factor: Double) = Unit
        }
        val editor = ComposerViewModel(refusing)
        editor.onCellTouched(pitch("E4"), eighth = 0)

        // When
        editor.onListen()
        editor.onFrame(0)
        editor.onFrame(1_000_000_000)

        // Then
        assertThat(editor.uiState.value.played).isNull()
    }

    @Test
    fun `after a change of tempo while listening, the played cell moves on at the new tempo`() {
        // Given a music listened to, sped up from 120 to 180 beats per minute: 10 ticks a cell
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onListen()
        repeat(6) { viewModel.onFaster() }

        // When a second goes by, 60 ticks
        viewModel.onFrame(0)
        viewModel.onFrame(1_000_000_000)

        // Then six cells further, not four as at 120 beats per minute
        assertThat(viewModel.uiState.value.played).isEqualTo(Cell(bar = 0, eighth = 6))
    }

    @Test
    fun `following stopped by choosing another bar comes back with the next listening, unlike following unchecked`() {
        // Given a music listened to, following stopped by choosing another bar, then the music stopped
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onListen()
        viewModel.onBarChosen(5)
        viewModel.onListen()

        // When listened to again, then following unchecked in the menu and the music listened to once more
        viewModel.onListen()
        val again = viewModel.uiState.value
        viewModel.onFollowPlayback(false)
        viewModel.onListen()
        viewModel.onListen()

        // Then
        assertThat(again.followPlayback).isTrue()
        assertThat(again.editor.bar).isEqualTo(0)
        assertThat(viewModel.uiState.value.followPlayback).isFalse()
    }

    @Test
    fun `Listen, disabled on an empty music, plays nothing when it comes all the same`() {
        // When
        viewModel.onListen()

        // Then
        assertThat(music.calls).isEmpty()
        assertThat(viewModel.uiState.value.played).isNull()
    }

    @Test
    fun `the music stops when the editor goes to the background, and plays on behind a dialog`() {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onListen()

        // When a dialog shows, then the application goes to the background
        viewModel.onLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        val paused = music.playing
        viewModel.onLifecycleEvent(Lifecycle.Event.ON_STOP)

        // Then
        assertThat(paused).isEqualTo(EDITED_SCORE_ID)
        assertThat(music.calls).containsExactly(PLAY, "stop")
        assertThat(viewModel.uiState.value.played).isNull()
    }

    @Test
    fun `leaving the editor stops the music`() {
        // Given a music listened to, in a view model store like the navigation entry's, built by the factory
        val store = ViewModelStore()
        val provider = ViewModelProvider.create(store, ComposerViewModel.Factory(music))
        val editor = provider[ComposerViewModel::class]
        editor.onCellTouched(pitch("E4"), eighth = 0)
        editor.onListen()

        // When the entry leaves the stack
        store.clear()

        // Then
        assertThat(music.calls).containsExactly(PLAY, "stop")
    }

    @Test
    fun `a music that stops by itself, as the audio output fails, offers to listen again`() {
        // Given
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onListen()

        // When
        music.fail()
        val failed = viewModel.uiState.value
        viewModel.onListen()

        // Then
        assertThat(failed.played).isNull()
        assertThat(music.calls).containsExactly(PLAY, "failure", "stop", PLAY)
    }

    @Test
    fun `the factory that app builds makes an editor on a new music`() {
        // When
        val made = ComposerViewModel.Factory(music).create(ComposerViewModel::class, CreationExtras.Empty)

        // Then
        assertThat(made.uiState.value.editor).isEqualTo(EditorState.new())
    }

    /** A change of the notes, each possible with a note in the first bar, shown on the grid, and the second cleared. */
    enum class NotesChange(val apply: ComposerViewModel.() -> Unit) {
        TOUCH({ onCellTouched(pitch("G4"), eighth = 2) }),
        SLIDE({ onCellSlid(pitch("G4"), fromEighth = 2, toEighth = 4) }),
        COPY({ onCopyBar() }),
        CLEAR({ onClearBar() }),
        UNDO({ onUndo() }),
    }

    private companion object {
        /** Enough steps of 10 to reach either end of the tempos, from anywhere between them. */
        const val TEMPO_STEPS = (Tempo.FASTEST - Tempo.SLOWEST) / Tempo.STEP

        /** What the music player notes when it is told to play the music being edited. */
        val PLAY = "play ${EDITED_SCORE_ID}"
    }
}

/** A playback that no longer plays from the start, as one refused the audio focus. */
private object EndedPlayback : Playback {
    override val isPlaying: Flow<Boolean> = flowOf(false)

    override fun stop() = Unit

    override fun resume(): Playback = this
}
