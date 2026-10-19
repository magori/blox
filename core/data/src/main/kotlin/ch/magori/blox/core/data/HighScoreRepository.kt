package ch.magori.blox.core.data

import kotlinx.coroutines.flow.Flow

/** The best score of the player, kept on the device. */
interface HighScoreRepository {
    /**
     * The record, its real value (the screen caps what it shows); 0 until a game is played.
     *
     * Never fails with an [java.io.IOException]: records that cannot be read give 0, once, and the flow
     * then ends. Any other failure is a bug, and goes through to the collector: it is not to be caught.
     */
    val highScore: Flow<Int>

    /**
     * Submits the [score] of a game that has just ended: it becomes the record if it is higher.
     * To submit as soon as the game is over, so that a record is not lost if the process dies.
     * The result compares the score with the record from before this game.
     *
     * @throws java.io.IOException if the records cannot be written; any other failure is a bug.
     * @throws IllegalArgumentException if [score] is negative.
     */
    suspend fun submit(score: Int): RecordResult
}
