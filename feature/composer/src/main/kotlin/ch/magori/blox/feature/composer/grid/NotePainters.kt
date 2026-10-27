package ch.magori.blox.feature.composer.grid

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import ch.magori.blox.core.designsystem.theme.BubbleLook
import ch.magori.blox.core.designsystem.theme.BubblePainter
import ch.magori.blox.core.designsystem.theme.bubbleLook

/**
 * The look of this bubble on a note of [size] pixels at [density], from its smaller side, as a cell of a piece
 * ([bubbleLook]): the corners of `radius-cell` and the gloss from 16 dp, corners of 2 dp and no gloss below.
 */
internal fun NoteBubble.lookAt(
    size: Size,
    density: Density,
): BubbleLook = bubbleLook(with(density) { size.minDimension.toDp() }, gloss)

/**
 * The painters of the notes, each one built by [build] once for its look, its size and the density, then kept: a touch
 * draws the line again, never builds a painter again; a change of density builds them again. A grid has few sizes of
 * note (the cells, a beat apart or not, their halves), and a theme two looks, one per track: the painters are looked up one by one, without allocating anything.
 * Should a window resized many times leave more than [MAX_PAINTERS], they are built anew.
 */
@Stable
internal class NotePainters<T : Any>(private val build: (look: NoteLook, size: Size, density: Density) -> T) {
    private val built = ArrayList<Built<T>>()

    /** How many painters are kept. */
    val kept: Int get() = built.size

    /** The painter of a note of [look] and [size] at [density], built on the first call. */
    fun get(
        look: NoteLook,
        size: Size,
        density: Density,
    ): T {
        for (index in built.indices) {
            val kept = built[index]
            if (kept.matches(look, size, density)) return kept.painter
        }
        if (built.size >= MAX_PAINTERS) built.clear()
        return build(look, size, density).also { built += Built(look, size.width, size.height, density.density, it) }
    }

    private class Built<T>(
        val look: NoteLook,
        val width: Float,
        val height: Float,
        val density: Float,
        val painter: T,
    ) {
        fun matches(
            look: NoteLook,
            size: Size,
            density: Density,
        ) = width == size.width && height == size.height && this.density == density.density && this.look == look
    }
}

/** Far more painters than a grid needs: a few sizes, two looks. */
private const val MAX_PAINTERS = 64

/**
 * A note of [look] and [size] pixels at [density], drawn at the origin: a bubble built once, with its path, its
 * gradient and its gloss, by the [BubblePainter] of the design system, as a cell of a piece (core:board); or a block of
 * its colour, flat.
 */
internal class NotePainter(look: NoteLook, private val size: Size, density: Density) {
    private val color = look.color
    private val bubble: BubblePainter? = look.bubble?.let { bubble ->
        val shaped = bubble.lookAt(size, density)
        BubblePainter(look.color, bubble.highlight, shaped.shape, shaped.gloss, size, density)
    }

    fun draw(scope: DrawScope) {
        if (bubble == null) scope.drawRect(color, size = size) else bubble.draw(scope)
    }
}
