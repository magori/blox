package ch.magori.blox.core.audio

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.music.SoundEffects
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * `AudioTrack` in static mode really opens, plays and replays every sound effect of BloX on the device; what to play
 * and when, and the failures, are tested on the JVM (`SoundEffectTracksTest`). Whether it is heard is checked by ear.
 */
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
class StaticAudioTrackTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    /** At the rate of the output of the device, as the application renders them. */
    private val synthesizer = Synthesizer(effectSampleRate(context))

    @Test
    @DisplayName("every effect opens, plays, plays again over itself, and is released without failing")
    fun everyEffectOpensPlaysAndIsReleased() {
        SoundEffects.all.forEach { effect ->
            assertThatNoException().describedAs("$effect").isThrownBy {
                // Given
                val track = StaticAudioTrack(synthesizer.render(effect), synthesizer.sampleRate)

                // When it plays, then plays again before its end
                track.play()
                track.play()

                // Then nothing failed
                track.release()
            }
        }
    }
}
