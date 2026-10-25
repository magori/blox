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
internal enum class PredefinedEffect { TICK, CLICK, HEAVY_CLICK }

/**
 * The effect of [haptic] on the version [sdkInt] of Android. The patterns stay sober, as the feedback of a game
 * that lasts minutes:
 * - [Haptic.KEY_PRESS]: the predefined tick from Android 10, the lightest predefined effect, lighter than the
 *   click, felt under the finger many times a second without tiring; before, a pulse of 10 ms, half the one of
 *   the lines cleared;
 * - [Haptic.LINES_CLEARED]: the predefined click from Android 10; before, a pulse of 20 ms, about the length of
 *   that click;
 * - [Haptic.FOUR_LINES_CLEARED]: the predefined heavy click from Android 10; before, a pulse of 50 ms, felt as
 *   clearly stronger without lasting;
 * - [Haptic.GAME_OVER]: no predefined effect says « the end », so a waveform on every version: 80 ms, a pause
 *   of 100 ms, then 200 ms, under half a second, a fall felt once per game.
 */
internal fun hapticEffectFor(
    haptic: Haptic,
    sdkInt: Int,
): HapticEffect {
    val (predefined, pulseMillis) = when (haptic) {
        Haptic.KEY_PRESS -> PredefinedEffect.TICK to KEY_PRESS_MILLIS
        Haptic.LINES_CLEARED -> PredefinedEffect.CLICK to LINES_CLEARED_MILLIS
        Haptic.FOUR_LINES_CLEARED -> PredefinedEffect.HEAVY_CLICK to FOUR_LINES_CLEARED_MILLIS
        Haptic.GAME_OVER -> return HapticEffect.Waveform(GAME_OVER_TIMINGS)
    }
    return when {
        sdkInt >= PREDEFINED_EFFECTS_SDK -> HapticEffect.Predefined(predefined)
        else -> HapticEffect.OneShot(pulseMillis)
    }
}

/** What a vibration is for, which tells the system which of the player's settings apply to it. */
internal enum class HapticUsage {
    /**
     * The feedback of a touch on the interface. From Android 13, the touch feedback setting of Android turns it
     * off; before, the system at best sets its intensity, and only the settings of BloX decide.
     */
    TOUCH,

    /** The feedback of a game, filed by the system under the media vibrations. */
    GAME,
}

/**
 * The usage of [haptic]: a key pressed is a touch, so that from Android 13 the player who turned the touch
 * feedback of Android off does not feel it (before, only the key vibration setting of BloX decides, in the game);
 * what the game tells (lines cleared, game over) is not a touch.
 */
internal fun hapticUsageFor(haptic: Haptic): HapticUsage = when (haptic) {
    Haptic.KEY_PRESS -> HapticUsage.TOUCH
    Haptic.LINES_CLEARED, Haptic.FOUR_LINES_CLEARED, Haptic.GAME_OVER -> HapticUsage.GAME
}

/** Android 10 (API 29, `Build.VERSION_CODES.Q`): the first version with predefined effects. */
private const val PREDEFINED_EFFECTS_SDK = 29

/** The pulse of a key pressed before Android 10: the shortest one, half the one of [LINES_CLEARED_MILLIS]. */
private const val KEY_PRESS_MILLIS = 10L

/** The pulse of one to three lines before Android 10, about the length of the predefined click. */
private const val LINES_CLEARED_MILLIS = 20L

/** The pulse of four lines before Android 10: clearly stronger than [LINES_CLEARED_MILLIS], still short. */
private const val FOUR_LINES_CLEARED_MILLIS = 50L

/** The game over: off 0 ms, on 80 ms, off 100 ms, on 200 ms. */
private val GAME_OVER_TIMINGS = listOf(0L, 80L, 100L, 200L)
