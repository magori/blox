package ch.magori.blox.core.data

import kotlinx.coroutines.flow.Flow

/** The best score of the player, kept on the device. */
interface HighScoreRepository {
    /** The record, its real value (the screen caps what it shows); 0 until a game is played. */
    val highScore: Flow<Int>

    /**
     * Submits the [score] of a game that has just ended: it becomes the record if it is higher.
     * To submit as soon as the game is over, so that a record is not lost if the process dies.
     * The result compares the score with the record from before this game.
     *
     * @throws IllegalArgumentException if [score] is negative.
     */
    suspend fun submit(score: Int): RecordResult
}
