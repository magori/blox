package ch.magori.blox.core.audio

import ch.magori.blox.core.music.SoundEffects
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

/** Sound effects played one after the other, each after its wait (the Locksmith, the musical garden). */
class SoundCueTest {
    private val player = FakeSoundEffectPlayer()

    @Test
    fun `the cues play at once or once their wait is over, in the order of their waits`() = runTest {
        // Given
        val cues = listOf(
            SoundCue(SoundEffects.levelUp, afterMillis = 450),
            SoundCue(SoundEffects.rotation),
            SoundCue(SoundEffects.linesCleared, afterMillis = 80),
            SoundCue(SoundEffects.gameOver),
        )

        // When
        launch { player.playCues(cues) }
        runCurrent()
        val atOnce = player.played.toList()
        advanceTimeBy(80)
        runCurrent()
        val after80 = player.played.toList()
        advanceTimeBy(370)
        runCurrent()

        // Then
        assertThat(atOnce).containsExactly(SoundEffects.rotation, SoundEffects.gameOver)
        assertThat(after80).containsExactly(SoundEffects.rotation, SoundEffects.gameOver, SoundEffects.linesCleared)
        assertThat(player.played).containsExactly(
            SoundEffects.rotation,
            SoundEffects.gameOver,
            SoundEffects.linesCleared,
            SoundEffects.levelUp,
        )
    }

    @Test
    fun `cues cancelled before their wait is over play no more`() = runTest {
        // Given
        val playing = launch {
            player.playCues(listOf(SoundCue(SoundEffects.rotation), SoundCue(SoundEffects.levelUp, afterMillis = 80)))
        }
        runCurrent()

        // When
        playing.cancel()
        advanceTimeBy(100)
        runCurrent()

        // Then
        assertThat(player.played).containsExactly(SoundEffects.rotation)
    }

    @Test
    fun `a cue cannot wait a negative time`() {
        // When / Then
        assertThatIllegalArgumentException().isThrownBy { SoundCue(SoundEffects.rotation, afterMillis = -1) }
    }
}
