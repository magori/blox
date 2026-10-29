package ch.magori.blox.feature.locksmith.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.savedState
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * A key of the Locksmith through the saved state of Android, as `rememberNavBackStack` saves each key of the stack
 * before the process dies (`NavKeySerializer`: its class name, then its fields): the JVM tests have no `Bundle`.
 */
internal class LocksmithRouteSavingTest {
    @Test
    @DisplayName("a padlock route comes back from the saved state as it was")
    fun aPadlockRouteComesBackFromTheSavedStateAsItWas() {
        // Given
        val route: NavKey = LocksmithRoute(level = 4)

        // When
        val restored = decodeFromSavedState(SERIALIZER, encodeToSavedState(SERIALIZER, route))

        // Then
        assertThat(restored).isEqualTo(route)
    }

    @Test
    @DisplayName("a padlock route saved with its seed by an older version comes back as the padlock of its level")
    fun aPadlockRouteSavedWithItsSeedComesBackAsThePadlockOfItsLevel() {
        // Given padlock 2 as the versions before P7.30 saved it, with the seed it was drawn on
        val saved = savedState {
            putString("type", LocksmithRoute::class.java.name)
            putSavedState(
                "value",
                savedState {
                    putInt("level", 2)
                    putLong("seed", 7L)
                },
            )
        }

        // When
        val restored = decodeFromSavedState(SERIALIZER, saved)

        // Then: the seed is dropped, the padlock is that of its level
        assertThat(restored).isEqualTo(LocksmithRoute(level = 2))
    }

    private companion object {
        val SERIALIZER = NavKeySerializer<NavKey>()
    }
}
