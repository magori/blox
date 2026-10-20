package ch.magori.blox.feature.composer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ToolbarLinesTest {
    @Test
    fun `labels that hold in their share of the line at their smallest size keep the toolbar on one line`() {
        // Given the labels of the pixel font at 8 px a character, on 336 px
        val lines = linesOf(modes = labels("Poser", "Allonger") { it * 8 }, tools = tools { it * 8 })

        // Then
        assertThat(lines).isEqualTo(ToolbarLines.ONE)
    }

    @Test
    fun `a label wider than its share of the line at its smallest size puts the modes above the tools`() {
        // Given the labels of the pixel font at 12 px a character, the font twice as large: « Poser » takes 60 px
        val lines = linesOf(modes = labels("Poser", "Allonger") { it * 12 }, tools = tools { it * 12 })

        // Then
        assertThat(lines).isEqualTo(ToolbarLines.TWO)
    }

    @Test
    fun `a label as wide as its share of the line, to the pixel, keeps the toolbar on one line`() {
        // Given « Allonger » 68 px wide: its share is 74 px, less 3 px on each side
        val lines = linesOf(modes = modes(allonger = 68), tools = tools { 1 })

        // Then
        assertThat(lines).isEqualTo(ToolbarLines.ONE)
    }

    @Test
    fun `the gap between the two modes comes out of their share of the line`() {
        // Given « Allonger » 69 px wide: it would hold in 9/38 of the line less its four gaps (75.8 px), not in its
        // share of the modes less the gap between them (74 px), less 3 px on each side
        val lines = linesOf(modes = modes(allonger = 69), tools = tools { 1 })

        // Then
        assertThat(lines).isNotEqualTo(ToolbarLines.ONE)
    }

    @Test
    fun `a share is counted in the whole pixels the line gives it, not in fractions`() {
        // Given a line of 80 px, without gaps nor padding: « Allonger » has 18.95 px by the fractions, the 19 px of its
        // share once the line has rounded the group of the modes, then each mode
        val lines = toolbarLines(
            modes = modes(allonger = 19),
            tools = tools { 1 },
            width = 80,
            gap = 0,
            padding = 0,
            stackedPadding = 0,
        )

        // Then
        assertThat(lines).isEqualTo(ToolbarLines.ONE)
    }

    @Test
    fun `tools that do not hold side by side on the whole width at their smallest size stand one above the other`() {
        // Given the labels of the pixel font at 13 px a character on 296 px, a phone of 320 dp: « Annuler » takes
        // 91 px, its share of the line 100 px less 7 px on each side
        val lines = linesOf(modes = labels("Poser", "Allonger") { it * 13 }, tools = tools { it * 13 }, width = 296)

        // Then
        assertThat(lines).isEqualTo(ToolbarLines.TOOLS_STACKED)
    }

    @Test
    fun `the margin of the labels counts once the modes stand above the tools`() {
        // Given « Annuler » 94 px wide on 296 px: in its share of 100 px with 3 px on each side, not with 7 px
        val modes = labels("Poser", "Allonger") { it * 13 }
        val tools = listOf(MeasuredLabel("Annuler", 94), MeasuredLabel("Copier", 1), MeasuredLabel("Effacer", 1))

        // Then
        assertThat(linesOf(modes, tools, width = 296)).isEqualTo(ToolbarLines.TOOLS_STACKED)
    }

    private fun linesOf(
        modes: List<MeasuredLabel>,
        tools: List<MeasuredLabel>,
        width: Int = WIDTH,
    ) = toolbarLines(modes, tools, width = width, gap = GAP, padding = PADDING, stackedPadding = STACKED_PADDING)

    /** « Poser », 1 px wide, and « Allonger », [allonger] px wide. */
    private fun modes(allonger: Int) = listOf(MeasuredLabel("Poser", 1), MeasuredLabel("Allonger", allonger))

    /** « Annuler », « Copier » and « Effacer », each as wide as [width] gives for its count of characters. */
    private fun tools(width: (Int) -> Int) = labels("Annuler", "Copier", "Effacer", width = width)

    /** The [texts], each as wide as [width] gives for its count of characters. */
    private fun labels(
        vararg texts: String,
        width: (Int) -> Int,
    ) = texts.map { MeasuredLabel(it, width(it.length)) }

    private companion object {
        const val WIDTH = 336
        const val GAP = 4
        const val PADDING = 3
        const val STACKED_PADDING = 7
    }
}
