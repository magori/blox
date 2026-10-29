package ch.magori.blox.feature.locksmith

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.data.OpeningOutcome
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.PadlockResult
import ch.magori.blox.core.locksmith.Refusal

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

/** Padlock 3, of 5 pieces, the padlock of the previews. */
private val start: PadlockGame = PadlockGame.start(Padlock.ofLevel(3))

/** This game, its selected piece turned clockwise until it takes the orientation of [piece], put down on its pivot. */
private fun PadlockGame.placing(piece: Piece): PadlockGame {
    val rank = given.indices.first { it !in placed && given[it] == piece.type }
    val turned = generateSequence(select(rank)) { it.turn(Rotation.CLOCKWISE) }
        .first { it.orientationOf(rank) == piece.orientation }
    return turned.place(piece.pivot).game
}

/**
 * The first given piece placed where the solution has it, the next one aimed by its pivot on the first cell inside the
 * ring where it covers the ring, then placed there: refused, it bumps into the metal; its cells on the ring are drawn
 * over the rim.
 */
private fun refusedState(): LocksmithUiState {
    val placed = start.placing(start.padlock.solution.first { it.type == start.given.first() })
    val aim = INSIDE_CELLS.first { pivot -> placed.selectedAt(pivot)!!.cells.any { !it.isInside } }
    val refused = placed.place(aim)
    return LocksmithUiState(level = 3, game = refused.game, events = refused.events, move = 1, aim = aim)
}

/**
 * The first given piece, turned, aimed by its pivot on the first cell inside the ring where it covers the bottom of
 * the ring, then placed there: refused, it bumps into the metal; its cells on the ring are drawn under the rim, over
 * the room kept for them above the message.
 */
private fun refusedBottomState(): LocksmithUiState {
    val turns = generateSequence(start) { it.turn(Rotation.CLOCKWISE) }.take(ORIENTATIONS)
    val (turned, aim) = turns.flatMap { game -> INSIDE_CELLS.asSequence().map { game to it } }
        .first { (game, pivot) -> game.selectedAt(pivot)!!.cells.any { it.row == Padlock.SIZE - 1 } }
    val refused = turned.place(aim)
    return LocksmithUiState(level = 3, game = refused.game, events = refused.events, move = 1, aim = aim)
}

/** The first given piece placed where no way is left, and the hint asked: it names that piece, which blocks. */
private fun blockingState(): LocksmithUiState {
    val turns = generateSequence(start) { it.turn(Rotation.CLOCKWISE) }.take(ORIENTATIONS).toList()
    val (game, hint) = turns.flatMap { turned -> INSIDE_CELLS.map { turned.place(it) } }
        .filter { update -> update.events.any { it is PadlockEvent.PiecePlaced } }
        .map { it.game.hint() }
        .map { it.game to (it.events.single() as PadlockEvent.HintGiven).hint }
        .first { (_, hint) -> hint is Hint.Blocking }
    return LocksmithUiState(level = 3, game = game, hint = hint, move = 2)
}

/** A hint asked, a placement refused on the metal, then every piece placed: the padlock open, its card shown. */
private fun openState(): LocksmithUiState {
    val tried = start.hint().game.place(Position(0, 0)).game
    val open = start.padlock.solution.fold(tried) { game, piece -> game.placing(piece) }
    return LocksmithUiState(level = 3, game = open, events = listOf(PadlockEvent.PadlockOpened), move = 8)
}

/** Padlock 3 opened without taking back, refusal or hint, its first opening: PARFAITE. */
private fun perfectState(): LocksmithUiState {
    val open = start.padlock.solution.fold(start) { game, piece -> game.placing(piece) }
    return LocksmithUiState(
        level = 3,
        game = open,
        events = listOf(PadlockEvent.PadlockOpened),
        move = 5,
        outcome = OpeningOutcome.FIRST_OPENING,
        best = open.result,
        frontier = 4,
    )
}

/** The padlock open of [openState], better than its best before: NOUVEAU MEILLEUR. */
private fun newBestState(): LocksmithUiState = openState().let { open ->
    open.copy(outcome = OpeningOutcome.NEW_BEST, best = open.game.result, frontier = 4)
}

/**
 * The padlock open of [openState], replayed while padlocks 1 to 12 are open, no better than its best, opened without a
 * hint: the best shown, and Reprendre à la serrure 13.
 */
private fun replayState(): LocksmithUiState = openState().let { open ->
    val best = PadlockResult(pieces = open.game.given.size, moves = open.game.given.size + 1, refusals = 2, hints = 0)
    open.copy(outcome = OpeningOutcome.NOTHING_NEW, best = best, frontier = 13)
}

/** The padlock open of [openState], its opening not written: « Résultat non enregistré ». */
private fun unrecordedState(): LocksmithUiState = openState().copy(unrecorded = true, frontier = 3)

/**
 * The first given piece, turned, aimed by its pivot on the first cell inside the ring where it covers the ring at
 * [column], 0 or 9, and nowhere above or below the 8 × 8, then placed there: refused, its cells on the ring at the side,
 * cut at the edge of the screen.
 */
private fun refusedSideState(column: Int): LocksmithUiState {
    val turns = generateSequence(start) { it.turn(Rotation.CLOCKWISE) }.take(ORIENTATIONS)
    val (turned, aim) = turns.flatMap { game -> INSIDE_CELLS.asSequence().map { game to it } }
        .first { (game, pivot) ->
            val cells = game.selectedAt(pivot)!!.cells
            cells.any { it.column == column } && cells.all { it.row in INSIDE }
        }
    val refused = turned.place(aim)
    return LocksmithUiState(level = 3, game = refused.game, events = refused.events, move = 1, aim = aim)
}

/** The first piece of the solution placed where it lies: the last action, its feedback still to play. */
private fun placedState(): LocksmithUiState {
    val piece = start.padlock.solution.first()
    val rank = start.given.indices.first { start.given[it] == piece.type }
    val turned = generateSequence(start.select(rank)) { it.turn(Rotation.CLOCKWISE) }
        .first { it.orientationOf(rank) == piece.orientation }
    val placed = turned.place(piece.pivot)
    return LocksmithUiState(level = 3, game = placed.game, events = placed.events, move = 1)
}

/** A hint asked at the start: where a piece fits. */
private fun hintState(): LocksmithUiState {
    val update = start.hint()
    val hint = (update.events.single() as PadlockEvent.HintGiven).hint
    return LocksmithUiState(level = 3, game = update.game, events = update.events, move = 1, hint = hint)
}

/**
 * [state] under the Original theme of the application: the screen imposes the Colorful one, and keeps the [effects] of
 * the application. The feedback of its last action frozen at [feedbackTick], or over.
 */
@Composable
private fun LocksmithPreview(
    state: LocksmithUiState,
    feedbackTick: Int? = null,
    effects: BloxEffects = BloxEffects.Full,
) {
    BloxTheme(kind = BloxThemeKind.ORIGINAL, effects = effects) {
        LocksmithScreen(state = state, actions = LocksmithActions.None, feedbackTick = feedbackTick)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithRefusedDayPreview() {
    LocksmithPreview(refusedState())
}

@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
internal fun LocksmithRefusedNightPreview() {
    LocksmithPreview(refusedState())
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithBlockingDayPreview() {
    LocksmithPreview(blockingState())
}

@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
internal fun LocksmithBlockingNightPreview() {
    LocksmithPreview(blockingState())
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithOpenDayPreview() {
    LocksmithPreview(openState())
}

@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
internal fun LocksmithOpenNightPreview() {
    LocksmithPreview(openState())
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithRefusedBottomDayPreview() {
    LocksmithPreview(refusedBottomState())
}

@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
internal fun LocksmithRefusedBottomNightPreview() {
    LocksmithPreview(refusedBottomState())
}

/** The card on a small screen, in large text: it keeps to its share of the height, the bar and the padlock above it. */
@Preview(widthDp = 320, heightDp = 480, fontScale = 2f)
@Composable
internal fun LocksmithOpenSmallDayPreview() {
    LocksmithPreview(openState())
}

@Preview(widthDp = 320, heightDp = 480, fontScale = 2f, uiMode = UI_MODE_NIGHT)
@Composable
internal fun LocksmithOpenSmallNightPreview() {
    LocksmithPreview(openState())
}

/** A padlock opened perfectly, its sticker PARFAITE at rest. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithOpenPerfectDayPreview() {
    LocksmithPreview(perfectState())
}

/** A new best opening, its sticker NOUVEAU MEILLEUR at rest. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithOpenNewBestDayPreview() {
    LocksmithPreview(newBestState())
}

/** An old padlock replayed: its best opening, and three buttons, Reprendre à la serrure 13 among them. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithOpenReplayDayPreview() {
    LocksmithPreview(replayState())
}

@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
internal fun LocksmithOpenReplayNightPreview() {
    LocksmithPreview(replayState())
}

/** The same on a small screen, in large text: the card keeps to its share of the height, and scrolls. */
@Preview(widthDp = 320, heightDp = 480, fontScale = 2f)
@Composable
internal fun LocksmithOpenReplaySmallDayPreview() {
    LocksmithPreview(replayState())
}

/** An opening that could not be written: « Résultat non enregistré ». */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithOpenUnrecordedDayPreview() {
    LocksmithPreview(unrecordedState())
}

/** A preview refused at the left edge, on the ring in column 0: its cells there cut 4 dp from the edge of the screen. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithRefusedLeftDayPreview() {
    LocksmithPreview(refusedSideState(column = 0))
}

/** A preview refused at the right edge, on the ring in column 9. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithRefusedRightDayPreview() {
    LocksmithPreview(refusedSideState(column = Padlock.SIZE - 1))
}

/** A preview refused at the left edge, the padlock shaken to the left, 4 ticks in: its cut cell narrows in its margin. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithRefusedLeftShakeDayPreview() {
    LocksmithPreview(refusedSideState(column = 0), feedbackTick = 4)
}

/**
 * A preview refused at the right edge, the padlock at the height of its shake to the right, on its first tick: its cut
 * cell narrows rather than leave its margin, its cross whole within.
 */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithRefusedRightShakeDayPreview() {
    LocksmithPreview(refusedSideState(column = Padlock.SIZE - 1), feedbackTick = 0)
}

/** A preview refused at the left edge in reduced effects: its crossed cells lit, the cut one included, 2 ticks in. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithRefusedLeftReducedDayPreview() {
    LocksmithPreview(refusedSideState(column = 0), feedbackTick = 2, effects = REDUCED)
}

/** A piece just placed, its cells lit, 2 ticks in. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithPlacedFeedbackDayPreview() {
    LocksmithPreview(placedState(), feedbackTick = 2)
}

/** A refusal, the padlock shaken aside, 1 tick in. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithRefusedShakeDayPreview() {
    LocksmithPreview(refusedState(), feedbackTick = 1)
}

/** A refusal in reduced effects: no shake, the preview lit in danger, fading, 2 ticks in. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithRefusedReducedDayPreview() {
    LocksmithPreview(refusedState(), feedbackTick = 2, effects = REDUCED)
}

/** The padlock open, its hole lit from the bottom up, 6 ticks in. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithOpenWaveDayPreview() {
    LocksmithPreview(openState(), feedbackTick = 6)
}

/** The padlock open in reduced effects: its whole hole lit at once, fading, 2 ticks in. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithOpenReducedDayPreview() {
    LocksmithPreview(openState(), feedbackTick = 2, effects = REDUCED)
}

/** A hint, its cells at the height of their swell, 15 ticks in. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun LocksmithHintSwellDayPreview() {
    LocksmithPreview(hintState(), feedbackTick = 15)
}

/** A padlock of 6 pieces, the first one chosen, its tiles shrunk to 48 dp to hold on one row of a phone of 360 dp. */
@Preview(widthDp = 360, heightDp = 800)
@Composable
internal fun LocksmithSixTilesDayPreview() {
    LocksmithPreview(sixTilesState())
}

/** The same on a phone of 320 dp: under 48 dp on one row, the tiles go on two rows of 3, at 52 dp. */
@Preview(widthDp = 320, heightDp = 800)
@Composable
internal fun LocksmithSixTilesNarrowDayPreview() {
    LocksmithPreview(sixTilesState())
}

/** Padlock 3, of 5 pieces, on 300 dp: under 48 dp on one row, its tiles go on two rows, the last one centred. */
@Preview(widthDp = 300, heightDp = 800)
@Composable
internal fun LocksmithFiveTilesNarrowDayPreview() {
    LocksmithPreview(LocksmithUiState(level = 3, game = start.select(0)))
}

/** Padlock 4, of 6 pieces, the first one chosen. */
private fun sixTilesState(): LocksmithUiState =
    LocksmithUiState(level = 4, game = PadlockGame.start(Padlock.ofLevel(4)).select(0))

/** Reduced effects, as the player chooses them. */
private val REDUCED = BloxEffects(reduced = true, forcedBySystem = false)

/** The orientations of a piece, at most. */
private const val ORIENTATIONS = 4
