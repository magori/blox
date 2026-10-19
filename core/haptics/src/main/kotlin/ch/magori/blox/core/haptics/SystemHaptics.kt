package ch.magori.blox.core.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * The [Haptics] of the device's vibrator, with the patterns of [hapticEffectFor]. A device without a vibrator
 * (most tablets, the emulator) plays nothing. Kept thin on purpose, as `SystemAudioFocus`: it has no test, a
 * vibration cannot be observed from one and the emulator has no vibrator; the patterns are tested on the JVM
 * ([hapticEffectFor]), and when to vibrate is tested in the game, with a fake.
 *
 * The vibrations are those of a game: `VibrationAttributes.USAGE_MEDIA` from Android 13, `AudioAttributes`
 * `USAGE_GAME` before, which the system files under the same media vibrations. Not `USAGE_TOUCH`, which is the
 * feedback of a touch on the interface, and which the player's touch feedback setting turns off: a line cleared
 * is not a touch. Nor `USAGE_HARDWARE_FEEDBACK`, kept for the feedback of physical controls and of the system.
 *
 * What the system does with them: Do not disturb does not silence media vibrations, while the battery saver and
 * the general vibration switch of the system turn them off; from Android 13, the player's media vibration
 * intensity applies too, besides the setting of BloX.
 */
public class SystemHaptics(context: Context) : Haptics {
    /** The vibrator of the device, or null if it has none. */
    private val vibrator: Vibrator? = context.applicationContext.defaultVibrator()?.takeIf { it.hasVibrator() }

    override fun play(haptic: Haptic) {
        val vibrator = vibrator ?: return
        val effect = hapticEffectFor(haptic, Build.VERSION.SDK_INT).toVibrationEffect()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
        } else {
            vibrateAsGame(vibrator, effect)
        }
    }

    /**
     * Before Android 13, the usage of a vibration is given by audio attributes: those of a game, as the media
     * usage given from Android 13. That call is deprecated from Android 13, which this branch never reaches.
     */
    @Suppress("DEPRECATION")
    private fun vibrateAsGame(
        vibrator: Vibrator,
        effect: VibrationEffect,
    ) {
        vibrator.vibrate(effect, GAME_AUDIO_ATTRIBUTES)
    }

    private companion object {
        val GAME_AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()
    }
}

/** The `VibrationEffect` of [this] effect; a [HapticEffect.Predefined] only from Android 10, where it exists. */
private fun HapticEffect.toVibrationEffect(): VibrationEffect = when (this) {
    is HapticEffect.Predefined -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.createPredefined(
            when (effect) {
                PredefinedEffect.CLICK -> VibrationEffect.EFFECT_CLICK
                PredefinedEffect.HEAVY_CLICK -> VibrationEffect.EFFECT_HEAVY_CLICK
            },
        )
    } else {
        error("No predefined effect before Android 10: $this")
    }

    is HapticEffect.OneShot -> VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)

    is HapticEffect.Waveform -> VibrationEffect.createWaveform(timings.toLongArray(), NO_REPEAT)
}

/** The index to repeat a waveform from: none, it plays once. */
private const val NO_REPEAT = -1

/** The default vibrator of the device, through `VibratorManager` from Android 12; null if the system has none. */
private fun Context.defaultVibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    getSystemService(VibratorManager::class.java)?.defaultVibrator
} else {
    getSystemService(Vibrator::class.java)
}
