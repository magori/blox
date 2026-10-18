package ch.magori.blox.feature.game.topbar

import androidx.compose.runtime.Immutable
import ch.magori.blox.feature.game.sidepanel.scoreText

/** The texts of the top bar: the [record] shown, and the [spokenRecord] read by TalkBack. */
@Immutable
internal data class TopBarTexts(val record: String, val spokenRecord: String)

/**
 * The texts of the top bar for [record]: shown as the score in the game, on 6 digits and capped at
 * 999 999; read by TalkBack as the real value, without padding zeros nor cap.
 */
internal fun topBarTexts(record: Int): TopBarTexts = TopBarTexts(
    record = scoreText(record),
    spokenRecord = record.toString(),
)
