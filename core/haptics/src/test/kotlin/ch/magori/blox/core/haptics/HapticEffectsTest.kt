package ch.magori.blox.core.haptics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class HapticEffectsTest {
    @Test
    fun `from Android 10 the lines cleared play the predefined click`() {
        // When
        val effect = hapticEffectFor(Haptic.LINES_CLEARED, sdkInt = ANDROID_10)

        // Then
        assertThat(effect).isEqualTo(HapticEffect.Predefined(PredefinedEffect.CLICK))
    }

    @Test
    fun `from Android 10 four lines cleared play the predefined heavy click`() {
        // When
        val effect = hapticEffectFor(Haptic.FOUR_LINES_CLEARED, sdkInt = ANDROID_10)

        // Then
        assertThat(effect).isEqualTo(HapticEffect.Predefined(PredefinedEffect.HEAVY_CLICK))
    }

    @Test
    fun `before Android 10 the lines cleared play a pulse of 20 ms, four lines one of 50 ms`() {
        // When
        val lines = hapticEffectFor(Haptic.LINES_CLEARED, sdkInt = ANDROID_9)
        val fourLines = hapticEffectFor(Haptic.FOUR_LINES_CLEARED, sdkInt = ANDROID_9)

        // Then
        assertThat(lines).isEqualTo(HapticEffect.OneShot(millis = 20L))
        assertThat(fourLines).isEqualTo(HapticEffect.OneShot(millis = 50L))
    }

    @ParameterizedTest(name = "API {0}")
    @ValueSource(ints = [26, 28, 29, 33, 36])
    fun `the game over plays 80 ms, a pause of 100 ms, then 200 ms, on every version`(sdkInt: Int) {
        // When
        val effect = hapticEffectFor(Haptic.GAME_OVER, sdkInt)

        // Then
        assertThat(effect).isEqualTo(HapticEffect.Waveform(timings = listOf(0L, 80L, 100L, 200L)))
    }

    @Test
    fun `the key press is the predefined tick from Android 10 and a pulse of 10 ms before`() {
        // When
        val fromAndroid10 = hapticEffectFor(Haptic.KEY_PRESS, sdkInt = ANDROID_10)
        val beforeAndroid10 = hapticEffectFor(Haptic.KEY_PRESS, sdkInt = ANDROID_9)

        // Then
        assertThat(fromAndroid10).isEqualTo(HapticEffect.Predefined(PredefinedEffect.TICK))
        assertThat(beforeAndroid10).isEqualTo(HapticEffect.OneShot(millis = 10L))
    }

    @Test
    fun `the key press vibrates as a touch, the others as a game`() {
        // When
        val usages = Haptic.entries.associateWith(::hapticUsageFor)

        // Then
        assertThat(usages).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                Haptic.KEY_PRESS to HapticUsage.TOUCH,
                Haptic.LINES_CLEARED to HapticUsage.GAME,
                Haptic.FOUR_LINES_CLEARED to HapticUsage.GAME,
                Haptic.GAME_OVER to HapticUsage.GAME,
            ),
        )
    }

    private companion object {
        /** Android 9, API 28: the last version without predefined effects. */
        const val ANDROID_9 = 28

        /** Android 10, API 29: the first version with predefined effects. */
        const val ANDROID_10 = 29
    }
}
