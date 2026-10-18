package ch.magori.blox.core.game.engine

/** What the engine gives back for each call: the new [state], and the [events] of that step, in order. */
public data class GameUpdate(public val state: GameState, public val events: List<GameEvent>)
