package ch.magori.blox.feature.locksmith.navigation

import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.feature.locksmith.locksmithViewModel
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExtendWith(MainDispatcherExtension::class)
class LocksmithRouteTest {
    @ParameterizedTest(name = "level {0}")
    @ValueSource(ints = [0, -1])
    fun `padlocks are numbered from 1`(level: Int) {
        assertThatIllegalArgumentException().isThrownBy { LocksmithRoute(level, seed = 1) }
    }

    @Test
    fun `a route restored after the death of the process gives the same padlock, nothing placed`() {
        // Given: the route as the navigation saves it
        val route = LocksmithRoute(level = 4, seed = 2026)
        val saved = Json.encodeToString(LocksmithRoute.serializer(), route)

        // When
        val restored = Json.decodeFromString(LocksmithRoute.serializer(), saved)

        // Then
        assertThat(locksmithViewModel(restored).uiState.value.game)
            .isEqualTo(PadlockGame.start(Padlock.generate(level = 4, seed = 2026)))
    }

    @Test
    fun `the next padlock is one level up, on the seed it is given`() {
        assertThat(LocksmithRoute(level = 3, seed = 1).next(seed = 42)).isEqualTo(LocksmithRoute(level = 4, seed = 42))
    }
}
