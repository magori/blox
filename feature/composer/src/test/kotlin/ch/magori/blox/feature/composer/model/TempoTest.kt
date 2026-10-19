package ch.magori.blox.feature.composer.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TempoTest {
    @ParameterizedTest
    @ValueSource(ints = [59, 201])
    fun `a tempo is refused outside 60 to 200 beats per minute`(bpm: Int) {
        assertThatIllegalArgumentException().isThrownBy { Tempo(bpm) }
    }

    @ParameterizedTest
    @ValueSource(ints = [60, 200])
    fun `a tempo of 60 or 200 beats per minute is accepted`(bpm: Int) {
        assertThat(Tempo(bpm).bpm).isEqualTo(bpm)
    }

    @Test
    fun `speeding up adds 10 beats per minute, up to 200`() {
        assertThat(Tempo(137).faster()).isEqualTo(Tempo(147))
        assertThat(Tempo(195).faster()).isEqualTo(Tempo(200))
    }

    @Test
    fun `slowing down takes 10 beats per minute away, down to 60`() {
        assertThat(Tempo(137).slower()).isEqualTo(Tempo(127))
        assertThat(Tempo(65).slower()).isEqualTo(Tempo(60))
    }
}
