package ch.magori.blox.core.haptics

/**
 * The vibration of a [Haptic] on a version of Android, described without Android so that it is tested on the
 * JVM; [SystemHaptics] turns it into a `VibrationEffect`.
 */
internal sealed interface HapticEffect {
    /** An effect of the system, tuned for the actuator of the device (from Android 10). */
    data class Predefined(val effect: PredefinedEffect) : HapticEffect

    /** A single pulse of [millis], at the default amplitude. */
    data class OneShot(val millis: Long) : HapticEffect

    /** Pulses at the default amplitude: [timings] alternate off and on, starting off, played once. */
    data class Waveform(val timings: List<Long>) : HapticEffect
}

/** The predefined effects used, from the lightest to the strongest. */
internal enum class PredefinedEffect { CLICK, HEAVY_CLICK }

/**
 * The effect of [haptic] on the version [sdkInt] of Android. The patterns stay sober, as the feedback of a game
 * that lasts minutes:
 * - [Haptic.LINES_CLEARED]: the predefined click from Android 10, the lightest effect tuned for the actuator;
 *   before, a pulse of 20 ms, about the length of that click;
 * - [Haptic.FOUR_LINES_CLEARED]: the predefined heavy click from Android 10; before, a pulse of 50 ms, felt as
 *   clearly stronger without lasting;
 * - [Haptic.GAME_OVER]: no predefined effect says « the end », so a waveform on every version: 80 ms, a pause
 *   of 100 ms, then 200 ms, under half a second, a fall felt once per game.
 */
internal fun hapticEffectFor(
    haptic: Haptic,
    sdkInt: Int,
): HapticEffect {
    val fourLines = haptic == Haptic.FOUR_LINES_CLEARED
    return when {
        haptic == Haptic.GAME_OVER -> HapticEffect.Waveform(GAME_OVER_TIMINGS)

        sdkInt >= PREDEFINED_EFFECTS_SDK -> HapticEffect.Predefined(
            if (fourLines) PredefinedEffect.HEAVY_CLICK else PredefinedEffect.CLICK,
        )

        else -> HapticEffect.OneShot(if (fourLines) FOUR_LINES_CLEARED_MILLIS else LINES_CLEARED_MILLIS)
    }
}

/** Android 10 (API 29, `Build.VERSION_CODES.Q`): the first version with predefined effects. */
private const val PREDEFINED_EFFECTS_SDK = 29

/** The pulse of one to three lines before Android 10, about the length of the predefined click. */
private const val LINES_CLEARED_MILLIS = 20L

/** The pulse of four lines before Android 10: clearly stronger than [LINES_CLEARED_MILLIS], still short. */
private const val FOUR_LINES_CLEARED_MILLIS = 50L

/** The game over: off 0 ms, on 80 ms, off 100 ms, on 200 ms. */
private val GAME_OVER_TIMINGS = listOf(0L, 80L, 100L, 200L)
