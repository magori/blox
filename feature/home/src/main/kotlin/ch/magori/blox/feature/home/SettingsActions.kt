package ch.magori.blox.feature.home

import androidx.compose.runtime.Immutable

/** What the settings page asks for, each to its callback; no logic here (the view model decides). */
@Immutable
internal class SettingsActions(
    val onBack: () -> Unit,
    val onSoundEffectsChanged: (Boolean) -> Unit,
    val onEffectsVolumeChanged: (Int) -> Unit,
    val onMusicVolumeChanged: (Int) -> Unit,
    val onVibrationsChanged: (Boolean) -> Unit,
    val onKeyVibrationChanged: (Boolean) -> Unit,
    val onClearRecord: () -> Unit,
    val onKeepRecord: () -> Unit,
    val onClearRecordConfirmed: () -> Unit,
) {
    companion object {
        /** A page that does nothing: previews and screenshots. */
        val None = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
