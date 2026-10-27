package ch.magori.blox.feature.game.pause

import ch.magori.blox.core.designsystem.theme.BloxEffects
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PauseCurtainTest {
    @Test
    fun `the curtain shuts in 12 ticks, the duration of motion-curtain, before the card shows`() {
        // Given a curtain that starts to close as the game pauses
        val closing = PauseCurtain.closing(BloxEffects.Full)

        // When 11 ticks, then 12 ticks pass
        val almost = closing.advance(ticks = 11)
        val shut = closing.advance(ticks = 12)

        // Then the card shows only once it is shut
        assertThat(almost.phase).isEqualTo(CurtainPhase.CLOSING)
        assertThat(shut.phase).isEqualTo(CurtainPhase.CLOSED)
    }

    @Test
    fun `the curtain comes down row by row from the top, over the board and the next piece alike`() {
        // Given a curtain that starts to close
        val closing = PauseCurtain.closing(BloxEffects.Full)

        // When / Then: no row at first, half the rows halfway, every row once shut
        assertThat(closing.coveredRows(rows = 20)).isZero()
        assertThat(closing.advance(ticks = 1).coveredRows(rows = 20)).isEqualTo(2)
        assertThat(closing.advance(ticks = 6).coveredRows(rows = 20)).isEqualTo(10)
        assertThat(closing.advance(ticks = 6).coveredRows(rows = 2)).isEqualTo(1)
        assertThat(closing.advance(ticks = 12).coveredRows(rows = 20)).isEqualTo(20)
        assertThat(closing.advance(ticks = 12).coveredRows(rows = 2)).isEqualTo(2)
    }

    @Test
    fun `a shut curtain opens again in 12 ticks, from the bottom, and only then lets the game go on`() {
        // Given a shut curtain, which Reprendre opens
        val opening = PauseCurtain.closing(BloxEffects.Full).advance(ticks = 12).opening()

        // When 11 ticks, then 12 ticks pass
        val almost = opening.advance(ticks = 11)
        val open = opening.advance(ticks = 12)

        // Then the card goes at once, the rows come back from the bottom, and the game may go on only once it is open
        assertThat(opening.phase).isEqualTo(CurtainPhase.OPENING)
        assertThat(opening.advance(ticks = 6).coveredRows(rows = 20)).isEqualTo(10)
        assertThat(almost.phase).isEqualTo(CurtainPhase.OPENING)
        assertThat(almost.coveredRows(rows = 20)).isEqualTo(2)
        assertThat(open.phase).isEqualTo(CurtainPhase.OPEN)
        assertThat(open.coveredRows(rows = 20)).isZero()
    }

    @Test
    fun `in reduced effects the curtain fades over every row, shut in 8 ticks, the duration of motion-reduced-fade`() {
        // Given a curtain that starts to close, in reduced effects
        val closing = PauseCurtain.closing(Reduced)

        // When / Then: nothing comes down, the whole curtain fades in, and is shut after 8 ticks
        assertThat(closing.opacity).isZero()
        assertThat(closing.advance(ticks = 1).coveredRows(rows = 20)).isEqualTo(20)
        assertThat(closing.advance(ticks = 4).opacity).isEqualTo(0.5f)
        assertThat(closing.advance(ticks = 7).phase).isEqualTo(CurtainPhase.CLOSING)
        assertThat(closing.advance(ticks = 8).phase).isEqualTo(CurtainPhase.CLOSED)
        assertThat(closing.advance(ticks = 8).opacity).isEqualTo(1f)
    }

    @Test
    fun `in full effects the curtain is opaque, the rows it covers hide the board`() {
        val closing = PauseCurtain.closing(BloxEffects.Full)

        assertThat(closing.advance(ticks = 1).opacity).isEqualTo(1f)
    }

    @Test
    fun `a game shown already paused, as after a recreation, has its curtain shut at once`() {
        val shut = PauseCurtain.closed(BloxEffects.Full)

        assertThat(shut.phase).isEqualTo(CurtainPhase.CLOSED)
        assertThat(shut.coveredRows(rows = 20)).isEqualTo(20)
    }

    @Test
    fun `a curtain opened while it comes down goes back up from where it stood`() {
        // Given a curtain halfway down
        val halfway = PauseCurtain.closing(BloxEffects.Full).advance(ticks = 6)

        // When it opens, then 6 ticks pass
        val open = halfway.opening().advance(ticks = 6)

        // Then it is open
        assertThat(open.phase).isEqualTo(CurtainPhase.OPEN)
    }

    @Test
    fun `an open curtain lets the game go on, and covers nothing`() {
        val open = PauseCurtain.open()

        assertThat(open.phase).isEqualTo(CurtainPhase.OPEN)
        assertThat(open.coveredRows(rows = 20)).isZero()
    }

    private companion object {
        val Reduced = BloxEffects(reduced = true, forcedBySystem = false)
    }
}
