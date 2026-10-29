package ch.magori.blox.feature.locksmith.navigation

import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PadlocksRouteTest {
    @ParameterizedTest(name = "from padlock {0}")
    @ValueSource(ints = [0, -1])
    fun `the padlock the screen comes from is numbered from 1`(current: Int) {
        assertThatIllegalArgumentException().isThrownBy { PadlocksRoute(current) }
    }

    @Test
    fun `a route restored after the death of the process comes from the same padlock, or from none`() {
        // Given: the routes as the navigation saves them
        val routes = listOf(PadlocksRoute(current = 5), PadlocksRoute())

        // When
        val restored = routes.map { Json.decodeFromString(PadlocksRoute.serializer(), Json.encodeToString(it)) }

        // Then
        assertThat(restored).isEqualTo(routes)
    }
}
