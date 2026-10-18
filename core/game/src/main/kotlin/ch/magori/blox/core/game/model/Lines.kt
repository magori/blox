package ch.magori.blox.core.game.model

/** Total number of lines cleared since the start of the game. */
@JvmInline
public value class Lines(public val value: Int) {
    init {
        require(value >= 0) { "A total of lines cannot be negative: $value" }
    }

    /** The total once [lineClear] more lines are cleared. */
    public operator fun plus(lineClear: LineClear): Lines = Lines(value + lineClear.count)
}
