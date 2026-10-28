package ch.magori.blox.core.data

import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class DeletedGardenTest {
    @Test
    fun `a deleted garden has a place that is not negative`() {
        // Given
        val kept = KeptGarden(GardenId("a"), "Orage", A_GARDEN, MODIFIED_AT)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { DeletedGarden(kept, place = -1) }
    }
}
