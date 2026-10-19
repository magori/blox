package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.BASS_RANGE
import ch.magori.blox.core.music.MELODY_RANGE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TrackTest {
    @Test
    fun `each track spans the range in which the generator writes its voice`() {
        assertThat(Track.MELODY.range).isEqualTo(MELODY_RANGE)
        assertThat(Track.BASS.range).isEqualTo(BASS_RANGE)
    }

    @Test
    fun `the other track of the melody is the bass, and of the bass the melody`() {
        assertThat(Track.MELODY.other).isEqualTo(Track.BASS)
        assertThat(Track.BASS.other).isEqualTo(Track.MELODY)
    }
}
