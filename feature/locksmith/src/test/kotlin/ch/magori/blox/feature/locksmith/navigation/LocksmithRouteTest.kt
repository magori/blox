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
        assertThatIllegalArgumentException().isThrownBy { LocksmithRoute(level) }
    }

    @Test
    fun `a route restored after the death of the process gives the same padlock, nothing placed`() {
        // Given: the route as the navigation saves it
        val route = LocksmithRoute(level = 4)
        val saved = Json.encodeToString(LocksmithRoute.serializer(), route)

        // When
        val restored = Json.decodeFromString(LocksmithRoute.serializer(), saved)

        // Then
        assertThat(locksmithViewModel(restored).uiState.value.game).isEqualTo(PadlockGame.start(Padlock.ofLevel(4)))
    }

    @Test
    fun `the next padlock is one level up`() {
        assertThat(LocksmithRoute(level = 3).next()).isEqualTo(LocksmithRoute(level = 4))
    }
}
