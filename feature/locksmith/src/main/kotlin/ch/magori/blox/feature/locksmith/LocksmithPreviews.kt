package ch.magori.blox.feature.locksmith

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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
import ch.magori.blox.core.locksmith.Refusal

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

/** Padlock 3 of seed 7, the padlock of the previews. */
private val start: PadlockGame = PadlockGame.start(Padlock.generate(level = 3, seed = 7))

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

/** [state] under the Original theme of the application: the screen imposes the Colorful one. */
@Composable
private fun LocksmithPreview(state: LocksmithUiState) {
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        LocksmithScreen(state = state, actions = LocksmithActions.None)
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

/** The orientations of a piece, at most. */
private const val ORIENTATIONS = 4
