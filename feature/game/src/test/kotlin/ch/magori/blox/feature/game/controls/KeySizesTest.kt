package ch.magori.blox.feature.game.controls

import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class KeySizesTest {
    @Test
    fun `a phone wide enough has arrows of 64 dp and B and A of 68 dp`() {
        // Given a phone of 412 dp: 380 dp between its margins of 16 dp
        val width = 380.dp

        // When
        val keys = keySizes(width)

        // Then (key-arrow, key-rotate)
        assertThat(keys.arrow).isEqualTo(64.dp)
        assertThat(keys.rotation).isEqualTo(68.dp)
    }

    @Test
    fun `a phone of 390 dp, too narrow for them, has keys halfway to the compact ones`() {
        // Given a phone of 390 dp: 358 dp between its margins, where the keys of a phone take 3 × 64 + 2 × 8, 18,
        // 2 × 68 + 16, 378 dp
        val width = 358.dp

        // When
        val keys = keySizes(width)

        // Then arrows of 58 dp and B and A of 62 dp, halfway between 64 and 52, 68 and 56: 348 dp
        assertThat(keys).isEqualTo(KeySizes(arrow = 58.dp, rotation = 62.dp))
        assertThat(keys.width).isLessThanOrEqualTo(width)
    }

    @Test
    fun `the keys fit a 360 dp phone with 16 dp margins`() {
        // Given a phone of 360 dp: 328 dp between its margins
        val width = 328.dp

        // When
        val keys = keySizes(width)

        // Then the compact keys (key-arrow-compact, key-rotate-compact), the two groups side by side, 18 dp apart at
        // least: 3 × 52 + 2 × 8, 18, 2 × 56 + 16, 318 dp
        assertThat(keys).isEqualTo(KeySizes(arrow = 52.dp, rotation = 56.dp))
        assertThat(keys.directionsWidth + 18.dp + keys.rotationsWidth).isLessThanOrEqualTo(width)
    }

    @Test
    fun `the down arrow stands half a key below the left and right arrows`() {
        // When
        val phone = KeySizes.Phone
        val compact = KeySizes.Compact

        // Then (asked by the developer, 2026-10-28): 32 dp for arrows of 64 dp, as much in proportion when smaller; the
        // group as high as an arrow and that half
        assertThat(phone.arrowRaise).isEqualTo(32.dp)
        assertThat(phone.directionsHeight).isEqualTo(96.dp)
        assertThat(compact.arrowRaise).isEqualTo(26.dp)
        assertThat(compact.directionsHeight).isEqualTo(78.dp)
    }

    @Test
    fun `B stands 24 dp lower than A`() {
        // When
        val keys = KeySizes.all

        // Then, as on a handheld console: the group as high as a key and that drop
        keys.forEach { assertThat(it.rotationsHeight).isEqualTo(it.rotation + 24.dp) }
    }

    @Test
    fun `B and A stand 16 dp apart`() {
        // When
        val keys = KeySizes.Phone

        // Then (asked by the developer, 2026-10-26), the arrows 8 dp apart
        assertThat(ControlSizes.rotationSpacing).isEqualTo(16.dp)
        assertThat(keys.rotationsWidth).isEqualTo(68.dp + 16.dp + 68.dp)
        assertThat(keys.directionsWidth).isEqualTo(64.dp + 8.dp + 64.dp + 8.dp + 64.dp)
    }
}
