package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.cellsOf
import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

class ClassicRotationSystemTest {
    private val rotationSystem: RotationSystem = ClassicRotationSystem

    @ParameterizedTest(name = "{0}")
    @EnumSource(PieceType::class)
    fun `every orientation of every piece has 4 distinct cells`(type: PieceType) {
        // When
        val orientations = rotationSystem.orientations(type)

        // Then
        assertThat(orientations).isNotEmpty().allSatisfy { orientation ->
            assertThat(orientation.cells).hasSize(4)
        }
    }

    @ParameterizedTest(name = "{0} has {1} orientations")
    @CsvSource("O, 1", "I, 2", "S, 2", "Z, 2", "T, 4", "J, 4", "L, 4")
    fun `O has one orientation, I S and Z have two, T J and L have four`(
        type: PieceType,
        expectedCount: Int,
    ) {
        assertThat(rotationSystem.orientations(type)).hasSize(expectedCount)
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("spawnDrawings")
    fun `each piece spawns in its NES orientation around the pivot`(
        type: PieceType,
        drawing: String,
    ) {
        // When
        val spawn = rotationSystem.spawnOrientation(type)

        // Then
        assertThat(spawn.cells).isEqualTo(cellsOf(drawing))
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(PieceType::class)
    fun `no piece spawns with a cell above its pivot`(type: PieceType) {
        // When
        val spawn = rotationSystem.spawnOrientation(type)

        // Then
        assertThat(spawn.cells).allSatisfy { cell -> assertThat(cell.row).isGreaterThanOrEqualTo(0) }
    }

    @Test
    fun `T turns clockwise through up, right, down and left`() {
        assertOrientationsInClockwiseOrder(
            PieceType.T,
            """
            .T.
            T@T
            """,
            """
            T.
            @T
            T.
            """,
            T_DOWN,
            """
            .T
            T@
            .T
            """,
        )
    }

    @Test
    fun `J turns clockwise through left, up, right and down`() {
        assertOrientationsInClockwiseOrder(
            PieceType.J,
            """
            .J
            .@
            JJ
            """,
            """
            J..
            J@J
            """,
            """
            JJ
            @.
            J.
            """,
            J_DOWN,
        )
    }

    @Test
    fun `Z switches between horizontal and vertical`() {
        assertOrientationsInClockwiseOrder(
            PieceType.Z,
            Z_HORIZONTAL,
            """
            .Z
            @Z
            Z.
            """,
        )
    }

    @Test
    fun `O has a single orientation`() {
        assertOrientationsInClockwiseOrder(PieceType.O, O)
    }

    @Test
    fun `S switches between horizontal and vertical`() {
        assertOrientationsInClockwiseOrder(
            PieceType.S,
            S_HORIZONTAL,
            """
            S.
            @S
            .S
            """,
        )
    }

    @Test
    fun `L turns clockwise through right, down, left and up`() {
        assertOrientationsInClockwiseOrder(
            PieceType.L,
            """
            L.
            @.
            LL
            """,
            L_DOWN,
            """
            LL
            .@
            .L
            """,
            """
            ..L
            L@L
            """,
        )
    }

    @Test
    fun `I switches between vertical and horizontal`() {
        assertOrientationsInClockwiseOrder(
            PieceType.I,
            """
            I
            I
            @
            I
            """,
            I_HORIZONTAL,
        )
    }

    /** Then the orientations of [type], in clockwise order, are the shapes drawn in [drawings]. */
    private fun assertOrientationsInClockwiseOrder(
        type: PieceType,
        vararg drawings: String,
    ) {
        val shapes = rotationSystem.orientations(type).map { orientation -> orientation.cells }
        assertThat(shapes).containsExactlyElementsOf(drawings.map(::cellsOf))
    }

    private companion object {
        const val T_DOWN = """
            T@T
            .T.
            """
        const val J_DOWN = """
            J@J
            ..J
            """
        const val L_DOWN = """
            L@L
            L..
            """
        const val Z_HORIZONTAL = """
            Z@.
            .ZZ
            """
        const val S_HORIZONTAL = """
            .@S
            SS.
            """
        const val O = """
            O@
            OO
            """
        const val I_HORIZONTAL = """
            II@I
            """

        @JvmStatic
        fun spawnDrawings(): List<Arguments> = listOf(
            argumentSet("T points down", PieceType.T, T_DOWN),
            argumentSet("J points down", PieceType.J, J_DOWN),
            argumentSet("Z lies flat", PieceType.Z, Z_HORIZONTAL),
            argumentSet("O", PieceType.O, O),
            argumentSet("S lies flat", PieceType.S, S_HORIZONTAL),
            argumentSet("L points down", PieceType.L, L_DOWN),
            argumentSet("I lies flat", PieceType.I, I_HORIZONTAL),
        )
    }
}
