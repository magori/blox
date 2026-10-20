package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.text.TextLayoutResult

/**
 * Whether [text], going to the line at each of [lineEnds], cuts a word: a line goes on to the next only after a space.
 * After a slash or a hyphen, it is refused on purpose: « temps/ » / « min » reads as badly as a word cut. The words
 * are told apart by their spaces only: a language written without them (Chinese, Japanese, Thai) would have each line
 * break taken for a cut, and the text shrunk to its floor.
 */
internal fun cutsAWord(
    text: CharSequence,
    lineEnds: List<Int>,
): Boolean = lineEnds.any { end ->
    end in 1 until text.length && !text[end - 1].isWhitespace() && !text[end].isWhitespace()
}

/** Whether this text, as laid out, cuts a word from one line to the next. */
internal val TextLayoutResult.cutsAWord: Boolean
    get() = cutsAWord(layoutInput.text, List((lineCount - 1).coerceAtLeast(0)) { getLineEnd(it) })
