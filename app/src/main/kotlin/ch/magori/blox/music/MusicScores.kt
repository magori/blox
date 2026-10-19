package ch.magori.blox.music

import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.MusicFileRepository
import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.music.Music
import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Scores
import kotlinx.coroutines.flow.first

/**
 * What plays in place of « Mon fichier » when it has no file to play: no file chosen (as on another device, the
 * settings restored without the file, which never leaves this one), or a file that can no longer be read.
 */
internal val FALLBACK_SCORE: Score = Scores.korobeiniki

/**
 * The BloX score a music [choice] plays; null when the choice plays none of them: no music, or the file of the
 * player, which comes with its own player. The home screen offers to listen to the choices with a score.
 */
internal fun scoreOf(choice: MusicChoice): Score? = when (choice) {
    MusicChoice.KOROBEINIKI -> Scores.korobeiniki
    MusicChoice.MINUET -> Scores.minuet
    MusicChoice.NONE -> null
    MusicChoice.MY_FILE -> null
}

/**
 * The music a [choice] plays during a game, [file] being the music file the player chose, if any: « Mon fichier »
 * plays it, or [FALLBACK_SCORE] without one; the other choices their score, or nothing. A file that can no longer be
 * read falls back on [FALLBACK_SCORE] too, once played (`MusicSwitch`).
 */
internal fun musicOf(
    choice: MusicChoice,
    file: MusicFile?,
): Music? = when (choice) {
    MusicChoice.MY_FILE -> file ?: FALLBACK_SCORE
    else -> scoreOf(choice)
}

/** The music a [choice] plays, with the music file kept in [this] repository. Read at the start of each game. */
internal suspend fun MusicFileRepository.musicOf(choice: MusicChoice): Music? = musicOf(choice, musicFile.first())

/**
 * The score of the music of the player chosen for the games in « Mes musiques », which a game plays instead of the
 * music chosen on the home screen; null when there is none. Read once, at the start of each game.
 */
internal suspend fun MusicLibrary.gameMusicScore(): Score? = gameMusic.first()?.score
