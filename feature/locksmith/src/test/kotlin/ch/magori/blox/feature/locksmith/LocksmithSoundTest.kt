package ch.magori.blox.feature.locksmith

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.MutableCreationExtras
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.data.FakePadlockProgressRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.music.LocksmithSounds
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Progression
import ch.magori.blox.core.music.RomanNumeral
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import ch.magori.blox.feature.locksmith.sound.warmUpEffectsOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
class LocksmithSoundTest {
    private val start = PadlockGame.start(Padlock.ofLevel(2))
    private val soundEffects = FakeSoundEffectPlayer()
    private val preferences = FakeUserPreferencesRepository()

    /** The first given piece, where the solution has it. */
    private val firstPiece = start.padlock.solution.first { it.type == start.given.first() }

    private fun viewModel() = locksmithViewModel(LocksmithRoute(level = 2), soundEffects, preferences)

    @Test
    fun `a placement plays the arpeggio and the bass of its chord`() {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.turnTo(firstPiece.orientation)
        viewModel.place(firstPiece.pivot)

        // Then
        val chord = viewModel.uiState.value.game.chords.last()
        assertThat(soundEffects.played).containsExactlyElementsOf(LocksmithSounds.effectsOf(chord))
    }

    @Test
    fun `the second placement plays the chord of IV`() {
        // Given: a padlock of 4 pieces, I-IV-V-I, its first piece placed
        val viewModel = viewModel()
        viewModel.placing(firstPiece)
        soundEffects.played.clear()

        // When
        viewModel.placing(start.padlock.solution.first { it != firstPiece })

        // Then
        val iv = Scale(Pitch.parse("C4"), ScaleType.MAJOR).chord(RomanNumeral.parse("IV"))
        assertThat(start.progression).isEqualTo(Progression.parse("I-IV-V-I"))
        assertThat(soundEffects.played).containsExactly(LocksmithSounds.arpeggio(iv), LocksmithSounds.bass(iv))
    }

    @Test
    fun `the last placement adds the opening flourish after the final chord`() = runTest {
        // Given: every piece but the last one placed
        val viewModel = viewModel()
        val solution = start.padlock.solution
        solution.dropLast(1).forEach { viewModel.placing(it) }
        soundEffects.played.clear()

        // When
        viewModel.placing(solution.last())
        val atOnce = soundEffects.played.toList()
        advanceTimeBy(449)
        val justBefore = soundEffects.played.toList()
        advanceTimeBy(1)
        runCurrent()

        // Then
        val finalChord = viewModel.uiState.value.game.chords.last()
        assertThat(atOnce).containsExactlyElementsOf(LocksmithSounds.effectsOf(finalChord))
        assertThat(justBefore).isEqualTo(atOnce)
        assertThat(soundEffects.played).isEqualTo(atOnce + LocksmithSounds.openingFlourish)
    }

    @Test
    fun `the opening flourish still waiting dies with the screen left`() = runTest {
        // Given: the padlock opened in a view model kept as the navigation entry keeps it
        val store = ViewModelStore()
        val extras = MutableCreationExtras().apply { set(LocksmithViewModel.RouteKey, LocksmithRoute(2)) }
        val factory = LocksmithViewModel.Factory(
            soundEffects,
            preferences,
            FakePadlockProgressRepository(),
            MainScope(),
            ClearedProgressRelay(),
        )
        val viewModel = ViewModelProvider.create(store, factory, extras)[LocksmithViewModel::class]
        start.padlock.solution.forEach { viewModel.placing(it) }

        // When: the entry leaves the stack before the flourish (back, or the next padlock)
        store.clear()
        advanceUntilIdle()

        // Then
        assertThat(soundEffects.played).isNotEmpty().doesNotContain(LocksmithSounds.openingFlourish)
    }

    @Test
    fun `a refusal plays the refusal`() {
        // Given
        val viewModel = viewModel()

        // When: the pivot in the corner, on the metal
        viewModel.place(Position(0, 0))

        // Then
        assertThat(soundEffects.played).containsExactly(LocksmithSounds.refusal)
    }

    @Test
    fun `taking back, a hint and choosing play nothing`() {
        // Given
        val viewModel = viewModel()
        viewModel.placing(firstPiece)
        soundEffects.played.clear()

        // When
        viewModel.takeBack(0)
        viewModel.hint()
        viewModel.select(1)
        viewModel.turn(Rotation.CLOCKWISE)

        // Then
        assertThat(soundEffects.played).isEmpty()
    }

    @Test
    fun `no sound when the sound effects are off`() = runTest {
        // Given
        preferences.setSoundEffects(false)
        val viewModel = viewModel()

        // When: a refusal, then the padlock opened
        viewModel.place(Position(0, 0))
        start.padlock.solution.forEach { viewModel.placing(it) }
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value.game.isOpen).isTrue()
        assertThat(soundEffects.played).isEmpty()
    }

    @Test
    fun `the effects of the padlock are warmed up when its screen opens`() {
        // When
        viewModel()

        // Then
        assertThat(soundEffects.warmedUp).containsExactlyElementsOf(warmUpEffectsOf(start))
    }

    @Test
    fun `nothing is warmed up when the sound effects are off`() = runTest {
        // Given
        preferences.setSoundEffects(false)

        // When
        viewModel()

        // Then
        assertThat(soundEffects.warmedUp).isEmpty()
    }
}
