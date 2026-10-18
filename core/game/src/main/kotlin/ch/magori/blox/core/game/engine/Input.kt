package ch.magori.blox.core.game.engine

/** What the player does with the controller or the screen, given to [GameEngine.apply]. */
public sealed interface Input {
    /** [button] is pressed, and held until its [Release]. */
    public data class Press(public val button: Button) : Input

    /** [button] is released. */
    public data class Release(public val button: Button) : Input

    /** The game is paused: nothing changes until [Resume], and the held buttons are forgotten. */
    public data object Pause : Input

    /** The paused game goes on where it stood. */
    public data object Resume : Input
}
