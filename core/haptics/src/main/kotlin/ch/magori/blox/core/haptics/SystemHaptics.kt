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
 * (most tablets) cannot vibrate and plays nothing. Kept thin on purpose, as `SystemAudioFocus`: a vibration cannot
 * be observed from a test; the patterns and their usages are tested on the JVM ([hapticEffectFor],
 * [hapticUsageFor]), when to vibrate is tested in the game, with a fake, and [canVibrate] on a device.
 *
 * What the game tells (lines cleared, game over) vibrates as a game: `VibrationAttributes.USAGE_MEDIA` from
 * Android 13, `AudioAttributes` `USAGE_GAME` before, which the system files under the same media vibrations. Not
 * `USAGE_TOUCH`, which the player's touch feedback setting turns off: a line cleared is not a touch. Nor
 * `USAGE_HARDWARE_FEEDBACK`, kept for the feedback of physical controls and of the system.
 *
 * A key pressed ([Haptic.KEY_PRESS]) is a touch on the interface. From Android 13, it vibrates as
 * `VibrationAttributes.USAGE_TOUCH`, which the touch feedback setting of Android turns off. Before, it vibrates
 * with the `AudioAttributes` `USAGE_ASSISTANCE_SONIFICATION`, which at best sets its intensity: there, only the key
 * vibration setting of BloX decides, in the game.
 *
 * What the system does with them: Do not disturb does not silence media vibrations, while the battery saver and
 * the general vibration switch of the system turn them off; from Android 13, the player's media and touch
 * vibration intensities apply too, besides the settings of BloX.
 */
public class SystemHaptics(context: Context) : Haptics {
    /** The vibrator of the device, or null if it has none. */
    private val vibrator: Vibrator? = context.applicationContext.defaultVibrator()?.takeIf { it.hasVibrator() }

    override val canVibrate: Boolean get() = vibrator != null

    override fun play(haptic: Haptic) {
        val vibrator = vibrator ?: return
        val effect = hapticEffectFor(haptic, Build.VERSION.SDK_INT).toVibrationEffect()
        val usage = hapticUsageFor(haptic)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(
                effect,
                checkNotNull(
                    when (usage) {
                        HapticUsage.TOUCH -> TOUCH_VIBRATION_ATTRIBUTES
                        HapticUsage.GAME -> MEDIA_VIBRATION_ATTRIBUTES
                    },
                ),
            )
        } else {
            vibrateWithAudioAttributes(vibrator, effect, usage)
        }
    }

    /**
     * Before Android 13, the usage of a vibration is given by audio attributes, those of [usage]. That call is
     * deprecated from Android 13, which this branch never reaches.
     */
    @Suppress("DEPRECATION")
    private fun vibrateWithAudioAttributes(
        vibrator: Vibrator,
        effect: VibrationEffect,
        usage: HapticUsage,
    ) {
        vibrator.vibrate(
            effect,
            when (usage) {
                HapticUsage.TOUCH -> TOUCH_AUDIO_ATTRIBUTES
                HapticUsage.GAME -> GAME_AUDIO_ATTRIBUTES
            },
        )
    }

    private companion object {
        /** The vibration attributes of a touch, from Android 13 where they exist; null before, never read there. */
        val TOUCH_VIBRATION_ATTRIBUTES: VibrationAttributes? = vibrationAttributesOf(HapticUsage.TOUCH)

        /** The vibration attributes of media, from Android 13 where they exist; null before, never read there. */
        val MEDIA_VIBRATION_ATTRIBUTES: VibrationAttributes? = vibrationAttributesOf(HapticUsage.GAME)

        /** The vibration attributes of [usage] from Android 13: touch or media; null before. */
        private fun vibrationAttributesOf(usage: HapticUsage): VibrationAttributes? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                VibrationAttributes.createForUsage(
                    when (usage) {
                        HapticUsage.TOUCH -> VibrationAttributes.USAGE_TOUCH
                        HapticUsage.GAME -> VibrationAttributes.USAGE_MEDIA
                    },
                )
            } else {
                null
            }

        val GAME_AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()

        val TOUCH_AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .build()
    }
}

/** The `VibrationEffect` of [this] effect; a [HapticEffect.Predefined] only from Android 10, where it exists. */
private fun HapticEffect.toVibrationEffect(): VibrationEffect = when (this) {
    is HapticEffect.Predefined -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.createPredefined(
            when (effect) {
                PredefinedEffect.TICK -> VibrationEffect.EFFECT_TICK
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
