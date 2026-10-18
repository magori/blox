package ch.magori.blox.music

import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Scores

/**
 * The BloX score a music [choice] plays, during a game as in the preview of the home screen; null when
 * the choice plays none of them: no music, or the file of the player, which comes with its own player
 * (P4.6). The application, which knows both the settings and the scores, gives it to the two features.
 */
internal fun scoreOf(choice: MusicChoice): Score? = when (choice) {
    MusicChoice.KOROBEINIKI -> Scores.korobeiniki
    MusicChoice.MINUET -> Scores.minuet
    MusicChoice.NONE -> null
    MusicChoice.MY_FILE -> null
}
