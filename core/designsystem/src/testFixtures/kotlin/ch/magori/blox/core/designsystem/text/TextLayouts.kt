package ch.magori.blox.core.designsystem.text

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.TextLayoutResult
import org.assertj.core.api.Assertions.assertThat

/** The text [text], as laid out: the layout of its own node, in the tree not merged into its button. */
fun SemanticsNodeInteractionsProvider.textLayout(text: String): TextLayoutResult {
    val node = onNode(hasText(text), useUnmergedTree = true).fetchSemanticsNode()
    val layouts = mutableListOf<TextLayoutResult>()
    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
    return layouts.single()
}

/** Each line of [layout] holds whole words only: none is cut from one line to the next, at a space or not. */
fun assertWordsWhole(layout: TextLayoutResult) {
    val text = layout.layoutInput.text.text
    val words = List(layout.lineCount) { line ->
        text.substring(layout.getLineStart(line), layout.getLineEnd(line)).split(' ').filter { it.isNotEmpty() }
    }
    assertThat(words.flatten()).describedAs(text).containsExactlyElementsOf(text.split(' ').filter { it.isNotEmpty() })
}
