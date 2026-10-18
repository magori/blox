package ch.magori.blox.core.game.engine

/**
 * What the player does with the controller, given to [GameEngine.apply]. The pause will join as two more
 * inputs (`Pause`, `Resume`).
 */
public sealed interface Input {
    /** [button] is pressed, and held until its [Release]. */
    public data class Press(public val button: Button) : Input

    /** [button] is released. */
    public data class Release(public val button: Button) : Input
}
