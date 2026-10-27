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
    fun `the cartridges are the Classic, the Locksmith and the Graine du jour, in this order`() {
        // Given / When / Then: the grid lays them out in the order of the entries, two by row
        assertThat(Cartridge.entries).containsExactly(Cartridge.CLASSIC, Cartridge.LOCKSMITH, Cartridge.DAILY)
    }

    @Test
    fun `the tag of the Graine du jour cartridge is cartridge_daily, which the journey of benchmark copies as it is`() {
        // Given / When / Then
        assertThat(Cartridge.DAILY.tag).isEqualTo("cartridge_daily")
    }

    @Test
    fun `each cartridge bears the mascot of its game, the T, the O or the Z`() {
        // Given / When / Then
        assertThat(Cartridge.entries.associateWith { it.mascot }).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                Cartridge.CLASSIC to MascotCharacter.CLASSIC,
                Cartridge.LOCKSMITH to MascotCharacter.LOCKSMITH,
                Cartridge.DAILY to MascotCharacter.DAILY,
            ),
        )
    }
}
