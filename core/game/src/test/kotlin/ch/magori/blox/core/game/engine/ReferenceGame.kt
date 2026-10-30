package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRandomizer

/**
 * A reference game: a game of the Classic mode recorded once, tick by tick, before the engine was changed (P11.4,
 * before the extraction of `DropTiming`), and played again by the tests to show that the engine still plays it the
 * same way.
 *
 * Its text, one file per game in `src/test/resources/reference-games`, is made of:
 * - comment lines, starting with `#`, ignored;
 * - the header: `seed <n>` (the pieces are dealt by [ClassicRandomizer.stable] from that seed), `start-level <n>`, and
 *   `lines <n>`, the lines the game is given at the start, so that it can begin at a level no start level reaches;
 * - one line per call to [GameEngine.tick]: the inputs given just before it, then `>`, then the state after it, then,
 *   after `|`, its events if it has any (see [traceLine]);
 * - after a tick that changed the board, the board, one row per line, indented by two spaces.
 *
 * The inputs are read from the text; the states, events and boards are what the engine gives, rendered by [trace].
 */
internal data class ReferenceGame(
    val seed: Long,
    val startLevel: Level,
    val lines: Lines,
    val inputs: List<List<Input>>,
) {
    /** The engine of this game, with [rules]. */
    fun engine(rules: Rules): GameEngine = GameEngine(rules, ClassicRandomizer.stable(seed))

    /** The state this game starts from, given its [lines]. */
    fun start(engine: GameEngine): GameState {
        val started = engine.start(startLevel).state
        return started.copy(progress = GameProgress.of(startLevel, lines, engine.rules.levelProgression))
    }

    /** The text of this game played with [rules], comments left out: the one to compare with the recorded text. */
    fun trace(rules: Rules): List<String> {
        val engine = engine(rules)
        var state = start(engine)
        val text = mutableListOf("seed $seed", "start-level ${startLevel.value}", "lines ${lines.value}")
        inputs.forEach { tickInputs ->
            val before = tickInputs.fold(state) { current, input -> engine.apply(current, input).state }
            val update = engine.tick(before)
            text += traceLine(tickInputs, update)
            if (update.state.board != state.board) text += update.state.board.toString().lines().map { "  $it" }
            state = update.state
        }
        return text
    }

    companion object {
        /** The game a recorded [text] describes: its header and its inputs. */
        fun parse(text: String): ReferenceGame {
            val lines = text.lines().filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("  ") }
            val header = lines
                .takeWhile { " > " !in it }
                .associate { it.substringBefore(' ') to it.substringAfter(' ') }
            return ReferenceGame(
                seed = header.getValue("seed").toLong(),
                startLevel = Level(header.getValue("start-level").toInt()),
                lines = Lines(header.getValue("lines").toInt()),
                inputs = lines.filter { " > " in it }.map { parseInputs(it.substringBefore(" > ")) },
            )
        }

        /** The recorded text of a game, comments and blank lines left out. */
        fun recordedTrace(text: String): List<String> = text.lines().filter { it.isNotBlank() && !it.startsWith("#") }

        /**
         * One line of the trace: `<inputs> > t<tickCount> <phase> next <type> level <n> lines <n> points <n>`, then
         * `danger` and `paused` when they hold, then `| <events>` if there are any.
         *
         * Inputs: `-` for none, else `+BUTTON` for a press, `-BUTTON` for a release, `pause`, `resume`.
         * Phase: `falling <type> <cells> after <ticks since the last drop>`, then `soft <rows>` if the piece soft
         * dropped; `clearing rows <rows> left <ticks> then <entry delay>`, then `danger` if the stack will be in danger
         * once cleared; `entry <ticks left>`; `over <type> <cells>`. Cells are `column,row`, in reading order.
         * Events: `rotated <type> <cells> <rotation>`, `locked <type> <cells>`, `cleared <count>`, `level-up <level>`,
         * `danger on|off`, `game-over`, separated by `;`.
         */
        fun traceLine(
            inputs: List<Input>,
            update: GameUpdate,
        ): String {
            val state = update.state
            val progress = state.progress
            val line = buildString {
                append(if (inputs.isEmpty()) "-" else inputs.joinToString(" ", transform = ::inputText))
                append(" > t${state.tickCount} ${phaseText(state.phase)} next ${state.next}")
                append(" level ${progress.level.value} lines ${progress.lines.value} points ${progress.score.value}")
                if (state.stackInDanger) append(" danger")
                if (state.paused) append(" paused")
            }
            if (update.events.isEmpty()) return line
            return "$line | ${update.events.joinToString("; ", transform = ::eventText)}"
        }

        private fun inputText(input: Input): String = when (input) {
            is Input.Press -> "+${input.button}"
            is Input.Release -> "-${input.button}"
            Input.Pause -> "pause"
            Input.Resume -> "resume"
        }

        private fun parseInputs(text: String): List<Input> = if (text == "-") {
            emptyList()
        } else {
            text.split(' ').map {
                when {
                    it == "pause" -> Input.Pause
                    it == "resume" -> Input.Resume
                    it.startsWith("+") -> Input.Press(Button.valueOf(it.drop(1)))
                    it.startsWith("-") -> Input.Release(Button.valueOf(it.drop(1)))
                    else -> error("Unknown input: $it")
                }
            }
        }

        private fun phaseText(phase: Phase): String = when (phase) {
            is Phase.Falling -> buildString {
                append("falling ${pieceText(phase.piece)} after ${phase.ticksSinceDrop}")
                if (phase.softDropRows > 0) append(" soft ${phase.softDropRows}")
            }

            is Phase.Clearing -> buildString {
                append("clearing rows ${phase.rowsCleared.clearedRows.joinToString(",")}")
                append(" left ${phase.ticksLeft.value} then ${phase.entryDelayAfter.value}")
                if (phase.stackInDangerAfter) append(" danger")
            }

            is Phase.EntryDelay -> "entry ${phase.ticksLeft.value}"

            is Phase.GameOver -> "over ${pieceText(phase.piece)}"
        }

        private fun eventText(event: GameEvent): String = when (event) {
            is GameEvent.PieceRotated -> "rotated ${pieceText(event.piece)} ${event.rotation}"
            is GameEvent.PieceLocked -> "locked ${pieceText(event.piece)}"
            is GameEvent.LinesCleared -> "cleared ${event.lineClear.count}"
            is GameEvent.LevelUp -> "level-up ${event.level.value}"
            is GameEvent.StackDanger -> "danger ${if (event.on) "on" else "off"}"
            GameEvent.GameOver -> "game-over"
        }

        private fun pieceText(piece: Piece): String {
            val cells = piece.cells.sortedWith(compareBy(Position::row, Position::column))
            return "${piece.type} ${cells.joinToString(" ") { "${it.column},${it.row}" }}"
        }
    }
}
