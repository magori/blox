package ch.magori.blox.feature.home

/**
 * What the settings page shows (V21).
 *
 * @property vibrations whether the games vibrate: off unless the player turned them on.
 * @property soundEffects whether the games play their sound effects: on unless the player turned them off.
 */
internal data class SettingsUiState(val vibrations: Boolean, val soundEffects: Boolean)
