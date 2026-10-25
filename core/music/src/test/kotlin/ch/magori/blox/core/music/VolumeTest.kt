package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class VolumeTest {
    @ParameterizedTest(name = "{0} %")
    @ValueSource(ints = [0, 10, 50, 90, 100])
    fun `a volume is a multiple of 10 from 0 to 100`(percent: Int) {
        // When
        val volume = Volume(percent)

        // Then
        assertThat(volume.percent).isEqualTo(percent)
    }

    @ParameterizedTest(name = "{0} %")
    @ValueSource(ints = [Int.MIN_VALUE, -10, -1, 1, 5, 55, 99, 101, 110, Int.MAX_VALUE])
    fun `a volume below 0, above 100 or between two steps of 10 is refused`(percent: Int) {
        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Volume(percent) }
    }

    @ParameterizedTest(name = "{0} %")
    @ValueSource(ints = [-10, 5, 55, 110])
    fun `a percentage that is no volume gives no volume, without failing`(percent: Int) {
        // When / Then
        assertThat(Volume.ofPercentOrNull(percent)).isNull()
    }

    @ParameterizedTest(name = "{0} %")
    @ValueSource(ints = [0, 40, 100])
    fun `a percentage that is a volume gives that volume`(percent: Int) {
        // When / Then
        assertThat(Volume.ofPercentOrNull(percent)).isEqualTo(Volume(percent))
    }

    @Test
    fun `the full volume is 100 percent`() {
        // When / Then
        assertThat(Volume.FULL).isEqualTo(Volume(100))
    }

    @Test
    fun `the silent volume is 0 percent`() {
        // When / Then
        assertThat(Volume.SILENT).isEqualTo(Volume(0))
    }
}
