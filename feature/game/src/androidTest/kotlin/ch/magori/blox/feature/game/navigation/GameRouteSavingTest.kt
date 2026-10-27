package ch.magori.blox.feature.game.navigation

import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * A game route through the saved state of Android, as the navigation saves its stack before the process dies: the
 * JVM tests have no `Bundle`.
 */
internal class GameRouteSavingTest {
    @Test
    @DisplayName("a daily route comes back from the saved state with its date, its seed and its id")
    fun aDailyRouteComesBackFromTheSavedStateWithItsDateItsSeedAndItsId() {
        // Given
        val route = GameRoute.daily(LocalDate.of(2026, 10, 26), seed = 20261026L, id = 42L)

        // When
        val restored = decodeFromSavedState(GameRoute.serializer(), encodeToSavedState(GameRoute.serializer(), route))

        // Then
        assertThat(restored).isEqualTo(route)
    }

    @Test
    @DisplayName("a classic route comes back from the saved state as it was")
    fun aClassicRouteComesBackFromTheSavedStateAsItWas() {
        // Given
        val route = GameRoute(startLevel = 7, seed = 42L)

        // When
        val restored = decodeFromSavedState(GameRoute.serializer(), encodeToSavedState(GameRoute.serializer(), route))

        // Then
        assertThat(restored).isEqualTo(route)
    }
}
