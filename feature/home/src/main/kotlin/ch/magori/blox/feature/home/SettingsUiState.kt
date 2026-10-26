package ch.magori.blox.feature.home

/**
 * What the settings page shows (V21, V31).
 *
 * @property vibrations whether the games vibrate: off unless the player turned them on.
 * @property soundEffects whether the games play their sound effects: on unless the player turned them off.
 * @property keyVibration whether each key of the games vibrates when pressed: off unless the player turned it on.
 * @property effectsVolume the volume of the sound effects, in percent, a multiple of 10 from 0 to 100.
 * @property musicVolume the volume of the music, in percent, a multiple of 10 from 0 to 100.
 * @property canVibrate whether the device has a vibrator: without one, both vibrations are disabled, and say why.
 * @property reducedEffects whether the player asked for reduced effects: off unless the player turned them on. Android
 * may impose them, which the theme says (`BloxTheme.effects`), not this choice, kept as it was.
 * @property recordClearing where the clearing of the record stands.
 */
internal data class SettingsUiState(
    val reducedEffects: Boolean,
    val vibrations: Boolean,
    val soundEffects: Boolean,
    val keyVibration: Boolean,
    val effectsVolume: Int,
    val musicVolume: Int,
    val canVibrate: Boolean,
    val recordClearing: RecordClearing = RecordClearing.NONE,
)

/** Where the clearing of the record stands (V31, card Record). */
internal enum class RecordClearing {
    /** Nothing asked: the button Effacer only. */
    NONE,

    /** The window asks whether to clear the record, « Garder le record » first. */
    ASKED,

    /** The clear is being written: the window stays open, its answers disabled, and nothing closes it. */
    CLEARING,

    /** The record could not be cleared: the window, still open, says it failed. */
    FAILED,

    /** The record is cleared: « Record effacé », read by TalkBack. */
    CLEARED,
}
