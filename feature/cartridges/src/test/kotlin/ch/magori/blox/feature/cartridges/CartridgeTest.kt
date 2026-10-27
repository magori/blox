package ch.magori.blox.feature.cartridges

import ch.magori.blox.core.board.mascot.MascotCharacter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CartridgeTest {
    @Test
    fun `the tag of the Classic cartridge stays cartridge_classic, which the journey of benchmark copies as it is`() {
        // Given / When / Then: `:benchmark` does not see `feature:cartridges`, and finds the cartridge by this text
        // (`By.res`)
        assertThat(Cartridge.CLASSIC.tag).isEqualTo("cartridge_classic")
    }

    @Test
    fun `each cartridge bears the mascot of its game, the T of the Classic and the O of the Locksmith`() {
        // Given / When / Then
        assertThat(Cartridge.entries.associateWith { it.mascot }).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                Cartridge.CLASSIC to MascotCharacter.CLASSIC,
                Cartridge.LOCKSMITH to MascotCharacter.LOCKSMITH,
            ),
        )
    }
}
