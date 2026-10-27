package ch.magori.blox.feature.game.navigation

import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.random.Random

class GameRouteTest {
    @Test
    fun `a game route refuses a negative start level`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { GameRoute(startLevel = -1, seed = 1L) }
    }

    @Test
    fun `a daily game starts at level 5 whatever the level chosen`() {
        // Given the daily game of the 26th of October 2026
        val date = LocalDate.of(2026, 10, 26)

        // When / Then its route starts at level 5, and no other level is accepted for it
        assertThat(GameRoute.daily(date, seed = 20261026L, id = 1L).startLevel).isEqualTo(5)
        assertThatIllegalArgumentException().isThrownBy {
            GameRoute(startLevel = 9, seed = 20261026L, variant = GameVariant.Daily(date, id = 1L))
        }
    }

    @Test
    fun `a daily game is drawn in the colorful theme, a classic game in the theme chosen`() {
        // Given / When / Then
        assertThat(GameVariant.Daily(LocalDate.of(2026, 10, 26), id = 1L).colorfulOnly).isTrue()
        assertThat(GameVariant.Classic.colorfulOnly).isFalse()
    }

    @Test
    fun `the date of a daily route is saved as its ISO text`() {
        // Given
        val route = GameRoute.daily(LocalDate.of(2026, 10, 26), seed = 20261026L, id = 1L)

        // When
        val saved = Json.encodeToString(GameRoute.serializer(), route)

        // Then the date is written as its ISO text, and the route comes back as it was
        assertThat(saved).contains("\"date\":\"2026-10-26\"")
        assertThat(Json.decodeFromString(GameRoute.serializer(), saved)).isEqualTo(route)
    }

    @Test
    fun `replaying a classic game draws a new seed, at the same level`() {
        // Given
        val finished = GameRoute(startLevel = 7, seed = 1L)

        // When / Then
        assertThat(finished.replayed(Random(7))).isEqualTo(GameRoute(startLevel = 7, seed = Random(7).nextLong()))
    }

    @Test
    fun `replaying a daily game keeps its date and its seed, under a new id`() {
        // Given
        val finished = GameRoute.daily(LocalDate.of(2026, 10, 26), seed = 20261026L, id = 1L)

        // When / Then
        assertThat(finished.replayed(Random(7)))
            .isEqualTo(GameRoute.daily(LocalDate.of(2026, 10, 26), seed = 20261026L, id = Random(7).nextLong()))
    }
}
