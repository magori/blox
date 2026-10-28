package ch.magori.blox.feature.garden

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.Playback
import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.GardenRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.garden.GARDEN_SCORE_ID
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.garden.toScore
import ch.magori.blox.core.music.Music
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/** Écouter / Arrêter, the score of the garden in a loop, and the column played (docs/jardin-musical.md, Son). */
class GardenListeningTest {
    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val kept = FakeGardenRepository().apply { state.value = A_GARDEN }
    private val music = FakeMusicPlayer()

    // Built in each test, once the main dispatcher is the test one: it reads the garden kept at once.
    private val viewModel by lazy { gardenViewModel(kept, music = music) }

    @Test
    fun `Écouter plays the score of the garden in a loop, from its first column`() {
        // When
        viewModel.listen()

        // Then
        assertThat(music.calls).containsExactly(PLAY)
        assertThat(music.playedScores).containsExactly(A_GARDEN.toScore())
        assertThat(viewModel.uiState.value?.playedColumn).isZero()
    }

    @Test
    fun `Arrêter stops the score of the garden, and no column is played`() {
        // Given
        viewModel.listen()

        // When
        viewModel.listen()

        // Then
        assertThat(music.calls).containsExactly(PLAY, "stop")
        assertThat(viewModel.uiState.value?.playedColumn).isNull()
    }

    @Test
    fun `nothing is listened to while the garden kept is being read`() {
        // Given the garden kept not read yet
        val unread = object : GardenRepository {
            override val garden: Flow<Garden> = flow { awaitCancellation() }

            override suspend fun keep(garden: Garden) = Unit
        }
        val viewModel = gardenViewModel(unread, music = music)

        // When
        viewModel.listen()

        // Then
        assertThat(music.calls).isEmpty()
    }

    @Test
    fun `while listening, each change of the garden goes on with its score at the bar line`() {
        // Given
        viewModel.listen()

        // When: Poser, a piece taken back, the mood, Au hasard, Tout effacer, Annuler
        viewModel.tap(Position(10, 3))
        viewModel.place()
        val placed = viewModel.uiState.value!!.garden
        viewModel.tap(Position(10, 3))
        val takenBack = viewModel.uiState.value!!.garden
        viewModel.chooseMood(GardenMood.EPIC)
        viewModel.roll()
        val rolled = viewModel.uiState.value!!.garden
        viewModel.clearAll()
        viewModel.undo()

        // Then each garden adopted is switched to, in order, without stopping the loop
        assertThat(music.switchedScores).containsExactly(
            placed.toScore(),
            takenBack.toScore(),
            takenBack.withMood(GardenMood.EPIC).toScore(),
            rolled.toScore(),
            rolled.clear().toScore(),
            rolled.toScore(),
        )
        assertThat(music.calls).doesNotContain("stop")
    }

    @Test
    fun `without listening, a change of the garden plays nothing`() {
        // When
        viewModel.chooseMood(GardenMood.EPIC)
        viewModel.clearAll()

        // Then
        assertThat(music.calls).isEmpty()
    }

    @Test
    fun `the score stops once the garden is no longer visible, not on a pause`() {
        // Given
        viewModel.listen()

        // When the garden is paused (a dialog over it), then goes to the background
        viewModel.onLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        val paused = music.calls.toList()
        viewModel.onLifecycleEvent(Lifecycle.Event.ON_STOP)

        // Then
        assertThat(paused).containsExactly(PLAY)
        assertThat(music.calls).containsExactly(PLAY, "stop")
        assertThat(viewModel.uiState.value?.playedColumn).isNull()
    }

    @Test
    fun `the score stops when the garden is left`() {
        // Given the garden of an entry, listened to
        val store = ViewModelStore()
        val factory = GardenViewModel.Factory(
            kept,
            CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
            music,
            FakeSoundEffectPlayer(),
            FakeUserPreferencesRepository(),
        )
        ViewModelProvider.create(store, factory)[GardenViewModel::class].listen()

        // When the entry leaves the stack
        store.clear()

        // Then
        assertThat(music.calls).containsExactly(PLAY, "stop")
    }

    @Test
    fun `a score that stops by itself shows Écouter again`() {
        // Given
        viewModel.listen()

        // When the music stops by itself (the audio focus lost for good, the output failing)
        music.fail()

        // Then
        assertThat(viewModel.uiState.value?.playedColumn).isNull()
    }

    @Test
    fun `a change of the garden after the score stopped by itself switches nothing`() {
        // Given the score listened to, then stopped by itself
        viewModel.listen()
        music.fail()

        // When
        viewModel.chooseMood(GardenMood.EPIC)

        // Then
        assertThat(music.switchedScores).isEmpty()
    }

    @Test
    fun `a score refused at once shows Écouter again`() {
        // Given a music player whose score stops as soon as it is played, as when the audio focus is refused
        val refusing = object : MusicPlayer by music {
            override fun play(music: Music): Playback = this@GardenListeningTest.music.play(music).also {
                this@GardenListeningTest.music.fail()
            }
        }
        val viewModel = gardenViewModel(kept, music = refusing)

        // When
        viewModel.listen()

        // Then
        assertThat(viewModel.uiState.value?.playedColumn).isNull()
    }

    @Test
    fun `the play head moves on, frame after frame, at the tempo of the mood`() {
        // Given a joyful garden listened to, at 132 bpm: an eighth lasts 13.6 ticks
        viewModel.chooseMood(GardenMood.JOYFUL)
        viewModel.listen()
        viewModel.onFrame(0)

        // When 13 ticks, then 14, have gone by
        viewModel.onFrame(ticks(13))
        val before = viewModel.uiState.value?.playedColumn
        viewModel.onFrame(ticks(14))

        // Then
        assertThat(before).isZero()
        assertThat(viewModel.uiState.value?.playedColumn).isEqualTo(1)
    }

    @Test
    fun `the play head loops with the score, after its 16th column`() {
        // Given a joyful garden listened to: the loop of 16 eighths lasts 218.2 ticks
        viewModel.chooseMood(GardenMood.JOYFUL)
        viewModel.listen()
        viewModel.onFrame(0)

        // When
        viewModel.onFrame(ticks(219))

        // Then
        assertThat(viewModel.uiState.value?.playedColumn).isZero()
    }

    @Test
    fun `a new mood moves the play head at its tempo from the next bar, as the score switched to`() {
        // Given a calm garden listened to, at 84 bpm (an eighth lasts 21.4 ticks), then the joyful mood (132 bpm)
        viewModel.listen()
        viewModel.onFrame(0)
        viewModel.chooseMood(GardenMood.JOYFUL)

        // When 14 ticks go by, then the rest of the first bar, then 14 ticks more
        viewModel.onFrame(ticks(14))
        val inTheFirstBar = viewModel.uiState.value?.playedColumn
        viewModel.onFrame(ticks(172))
        val atTheBarLine = viewModel.uiState.value?.playedColumn
        viewModel.onFrame(ticks(186))

        // Then the first bar goes on at 84 bpm, the second at 132 bpm
        assertThat(inTheFirstBar).isZero()
        assertThat(atTheBarLine).isEqualTo(8)
        assertThat(viewModel.uiState.value?.playedColumn).isEqualTo(9)
    }

    @Test
    fun `a frame without listening moves nothing`() {
        // When
        viewModel.onFrame(0)
        viewModel.onFrame(ticks(100))

        // Then
        assertThat(viewModel.uiState.value?.playedColumn).isNull()
    }

    /** The time of a frame [count] ticks after the frame at 0, in nanoseconds, rounded up: [count] ticks are due. */
    private fun ticks(count: Int): Long = (count * NANOS_PER_SECOND + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val TICKS_PER_SECOND = 60

        /** What the music player notes as the score of the garden is played. */
        val PLAY = "play $GARDEN_SCORE_ID"

        /** A T in the calm mood. */
        val A_GARDEN: Garden = Garden.parse(
            """
            ................
            ................
            ................
            ................
            ................
            ................
            ....TTT.........
            .....T..........
            ................
            ................
            ................
            ................
            ................
            ................
            """,
        )
    }
}
