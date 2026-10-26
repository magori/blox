package ch.magori.blox.feature.cartridges

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CartridgeTest {
    @Test
    fun `the tag of the Classic cartridge stays cartridge_classic, which the journey of benchmark copies as it is`() {
        // Given / When / Then: `:benchmark` does not see `feature:cartridges`, and finds the cartridge by this text
        // (`By.res`)
        assertThat(Cartridge.CLASSIC.tag).isEqualTo("cartridge_classic")
    }
}
