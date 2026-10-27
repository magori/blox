package ch.magori.blox.feature.game.results

import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.RecordResult
import kotlinx.coroutines.flow.Flow

/**
 * Where the results of a game go, as its variant says (ADR 0026): the records of the Classic, or the results of the
 * daily game. The view model knows only this port; the factory chooses the adapter. Each write fails with an
 * [java.io.IOException] when the results cannot be written; any other failure is a bug.
 */
internal interface GameResults {
    /** The best the game has to beat, shown in the top bar, followed as it changes. */
    val best: Flow<Int>

    /** The game starts: once per game, never for a game restored after the process died. */
    suspend fun start()

    /** The game is over with [points]: whether they beat the best. */
    suspend fun end(points: Int): RecordResult

    /** The game is left before its end (Pause then Menu, Changer de jeu), with the [points] of the moment. */
    suspend fun leave(points: Int)
}

/**
 * The Classic: its score goes to the [records] at its end, and their record is the best to beat. Its start counts
 * nothing, and a game left before its end gives the records nothing.
 */
internal class ClassicGameResults(private val records: HighScoreRepository) : GameResults {
    override val best: Flow<Int> get() = records.highScore

    override suspend fun start() = Unit

    override suspend fun end(points: Int): RecordResult = records.submit(points)

    override suspend fun leave(points: Int) = Unit
}
