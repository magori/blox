package ch.magori.blox.core.haptics

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
/**
 * The vibrator of the device, as [SystemHaptics] sees it. A vibration cannot be observed from a test: what it plays
 * is tested on the JVM ([hapticEffectFor], [hapticUsageFor]), and when the game asks for it, with a fake.
 */
class SystemHapticsTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    @DisplayName("the haptics can vibrate exactly when the device has a vibrator")
    fun canVibrateWhenTheDeviceHasAVibrator() {
        // Given the vibrator of the device, as the system describes it
        val hasVibrator = context.systemVibrator()?.hasVibrator() ?: false

        // When
        val canVibrate = SystemHaptics(context).canVibrate

        // Then
        assertThat(canVibrate).isEqualTo(hasVibrator)
    }

    @Test
    @DisplayName("every haptic is accepted by the system, with its effect and its usage")
    fun everyHapticIsAcceptedBySystem() {
        // Given a device that vibrates: without a vibrator, nothing would be given to the system, the test is skipped
        val haptics = SystemHaptics(context)
        assumeTrue(haptics.canVibrate, "the device has no vibrator")

        // When / Then: the system accepts the effect and the attributes of each one, without failing
        Haptic.entries.forEach(haptics::play)
    }

    /**
     * The default vibrator of the device, looked up again by the test: an oracle independent of [SystemHaptics],
     * written apart on purpose, not shared with the code under test.
     */
    private fun Context.systemVibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        getSystemService(Vibrator::class.java)
    }
}
