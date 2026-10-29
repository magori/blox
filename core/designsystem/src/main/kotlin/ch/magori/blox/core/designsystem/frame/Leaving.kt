package ch.magori.blox.core.designsystem.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

/**
 * Something shown while it is [open], which leaves with an effect (ADR 0024): composed by [content] while [open] is not
 * null; once it is null, on the last value open for [length] ticks more, counted on the frames of the screen from 0
 * ([FrameTicks]), with those ticks to read where it is drawn ([leaving], null while open); then not at all. Its way out
 * composes nothing again but its end. A [length] of 0, as in reduced effects, makes it go at once; what was not open
 * since this is composed, as on coming back to a screen, does not leave again.
 */
@Composable
fun <T : Any> LeavingContent(
    open: T?,
    length: Int,
    content: @Composable (value: T, leaving: (() -> Int)?) -> Unit,
) {
    val last = remember { LastOpen<T>() }
    if (open != null) last.value = open
    val closed = open == null
    val ticks = remember(closed) { mutableIntStateOf(0) }
    var gone by remember(closed) { mutableStateOf(false) }
    LaunchedEffect(closed, length) {
        if (!closed) return@LaunchedEffect
        val frames = FrameTicks()
        while (ticks.intValue < length) {
            withFrameNanos { ticks.intValue += frames.advance(it) }
        }
        gone = true
        last.value = null
    }
    val shown = open ?: last.value?.takeUnless { gone || length == 0 } ?: return
    val leaving = remember(closed) { if (closed) ({ ticks.intValue }) else null }
    content(shown, leaving)
}

/** The last value a [LeavingContent] was open on, which it leaves with: no state, as nothing is drawn from it. */
private class LastOpen<T : Any> {
    var value: T? = null
}
