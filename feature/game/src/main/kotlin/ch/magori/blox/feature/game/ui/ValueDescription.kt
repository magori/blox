package ch.magori.blox.feature.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import ch.magori.blox.feature.game.R

/** What TalkBack reads of a value of the game screens: its [label], then its [value] as it is said. */
@Composable
@ReadOnlyComposable
internal fun valueDescription(
    label: String,
    value: String,
): String = stringResource(R.string.feature_game_value_description, label, value)
