package ch.magori.blox.feature.settings.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * The key of the settings through the saved state of Android, as `rememberNavBackStack` saves each key of the stack
 * before the process dies (`NavKeySerializer`: its class name, then its fields): the JVM tests have no `Bundle`.
 */
internal class SettingsRouteSavingTest {
    @Test
    @DisplayName("the key of the page opened from the Classic is saved and restored with its section")
    fun theKeyOfThePageOpenedFromTheClassicIsSavedAndRestoredWithItsSection() {
        // Given the key opened from the Classic, and the one opened without a section
        val routes: List<NavKey> = listOf(SettingsRoute(SettingsSection.CLASSIC), SettingsRoute())

        // When
        val restored = routes.map { decodeFromSavedState(SERIALIZER, encodeToSavedState(SERIALIZER, it)) }

        // Then
        assertThat(restored).isEqualTo(routes)
    }

    private companion object {
        val SERIALIZER = NavKeySerializer<NavKey>()
    }
}
