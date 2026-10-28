package ch.magori.blox.core.board.mascot

import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.game.model.Position
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MascotLookTest {
    @Test
    fun `the Classic mascot is a T pointing down and the Locksmith mascot an O`() {
        assertThat(MascotCharacter.CLASSIC.cells).containsExactlyInAnyOrder(
            Position(0, 0),
            Position(1, 0),
            Position(2, 0),
            Position(1, 1),
        )
        assertThat(MascotCharacter.LOCKSMITH.cells).containsExactlyInAnyOrder(
            Position(0, 0),
            Position(1, 0),
            Position(0, 1),
            Position(1, 1),
        )
    }

    @Test
    fun `the Graine du jour mascot is a Z, as it comes in`() {
        assertThat(MascotCharacter.DAILY.cells).containsExactlyInAnyOrder(
            Position(0, 0),
            Position(1, 0),
            Position(1, 1),
            Position(2, 1),
        )
    }

    @Test
    fun `the Jardin musical mascot is an L standing on its foot, the foot to the right`() {
        assertThat(MascotCharacter.GARDEN.cells).containsExactlyInAnyOrder(
            Position(0, 0),
            Position(0, 1),
            Position(0, 2),
            Position(1, 2),
        )
    }

    @Test
    fun `an awake mascot is a bubble of its piece, its face in the ink of the day, its accessory in yellow`() {
        // By day, the pastels of T and O; corners of 0,3 cell; a shadow under it
        assertThat(mascotLook(MascotCharacter.CLASSIC, night = false, asleep = false)).isEqualTo(
            MascotLook(
                fill = Color(0xFFC3A2FF),
                highlight = Color(0xFFEFE6FF),
                corner = 0.3f,
                accessory = Color(0xFFFFE680),
                face = Color(0xFF1C1B2E),
                shadow = MascotShadow(color = Color(90, 80, 160).copy(alpha = 0.3f), drop = 0.12f, blur = 0.16f),
            ),
        )
        // By night, their neons; corners of 0,26 cell; a halo of its colour around it
        assertThat(mascotLook(MascotCharacter.LOCKSMITH, night = true, asleep = false)).isEqualTo(
            MascotLook(
                fill = Color(0xFFFFE45E),
                highlight = Color(0xFFFFF5BF),
                corner = 0.26f,
                accessory = Color(0xFFFFE45E),
                face = Color(0xFF1C1B2E),
                shadow = MascotShadow(color = Color(0xFFFFE45E).copy(alpha = 0.6f), drop = 0f, blur = 0.25f),
            ),
        )
    }

    @Test
    fun `the Graine du jour mascot is a bubble of the Z, its sprout in the mint of the S`() {
        assertThat(mascotLook(MascotCharacter.DAILY, night = false, asleep = false)).isEqualTo(
            MascotLook(
                fill = Color(0xFFFF94A6),
                highlight = Color(0xFFFFE0E6),
                corner = 0.3f,
                accessory = Color(0xFF9DEDB9),
                face = Color(0xFF1C1B2E),
                shadow = MascotShadow(color = Color(90, 80, 160).copy(alpha = 0.3f), drop = 0.12f, blur = 0.16f),
            ),
        )
        assertThat(mascotLook(MascotCharacter.DAILY, night = true, asleep = false).accessory)
            .isEqualTo(Color(0xFF3DFF8F))
    }

    @Test
    fun `the Jardin musical mascot is a bubble of the L, its leaf in the mint of the S`() {
        assertThat(mascotLook(MascotCharacter.GARDEN, night = false, asleep = false)).isEqualTo(
            MascotLook(
                fill = Color(0xFFFFB896),
                highlight = Color(0xFFFFE9DE),
                corner = 0.3f,
                accessory = Color(0xFF9DEDB9),
                face = Color(0xFF1C1B2E),
                shadow = MascotShadow(color = Color(90, 80, 160).copy(alpha = 0.3f), drop = 0.12f, blur = 0.16f),
            ),
        )
        assertThat(mascotLook(MascotCharacter.GARDEN, night = true, asleep = false).accessory)
            .isEqualTo(Color(0xFF3DFF8F))
    }

    @Test
    fun `an asleep mascot is in cell-dead, flat, its face in ink, without accessory nor shadow`() {
        assertThat(mascotLook(MascotCharacter.CLASSIC, night = false, asleep = true)).isEqualTo(
            MascotLook(
                fill = Color(0xFFD9DBE6),
                highlight = Color(0xFFD9DBE6),
                corner = 0.3f,
                accessory = null,
                face = Color(0xFF1C1B2E),
                shadow = null,
            ),
        )
        assertThat(mascotLook(MascotCharacter.CLASSIC, night = true, asleep = true)).isEqualTo(
            MascotLook(
                fill = Color(0xFF2A2D3A),
                highlight = Color(0xFF2A2D3A),
                corner = 0.26f,
                accessory = null,
                face = Color(0xFFF2F4FA),
                shadow = null,
            ),
        )
    }
}
