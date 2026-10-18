package ch.magori.blox.feature.game.navigation

import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class GameRouteTest {
    @Test
    fun `a game route refuses a negative start level`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { GameRoute(startLevel = -1, seed = 1L) }
    }
}
