package ch.magori.blox.feature.settings.navigation

import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SettingsRouteTest {
    @Test
    fun `a route restored after the death of the process keeps its section, or none`() {
        // Given: the routes as the navigation saves them
        val routes = listOf(SettingsRoute(SettingsSection.CLASSIC), SettingsRoute())

        // When
        val restored = routes.map { Json.decodeFromString(SettingsRoute.serializer(), Json.encodeToString(it)) }

        // Then
        assertThat(restored).isEqualTo(routes)
    }
}
