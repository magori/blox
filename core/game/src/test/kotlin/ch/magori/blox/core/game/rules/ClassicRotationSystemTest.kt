package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.cellsOf
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
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
    fun `the spawn orientation of a type is the same however often it is asked`(type: PieceType) {
        // Given the spawn orientation of the other types asked in between
        val first = rotationSystem.spawnOrientation(type)
        PieceType.entries.forEach(rotationSystem::spawnOrientation)

        // When
        val again = rotationSystem.spawnOrientation(type)

        // Then
        assertThat(again).isEqualTo(first)
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
            T_RIGHT,
            T_DOWN,
            T_LEFT,
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

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("everyOrientation")
    fun `every orientation has its pivot as one of its cells`(
        type: PieceType,
        index: Int,
    ) {
        assertThat(rotationSystem.orientations(type)[index].cells).contains(Position(0, 0))
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("tTurnsFromSpawn")
    fun `a clockwise rotation gives the next orientation, a counterclockwise one the previous`(
        rotation: Rotation,
        expectedDrawing: String,
    ) {
        // Given a T at spawn, pointing down
        val spawn = rotationSystem.spawnOrientation(PieceType.T)

        // When
        val rotated = rotationSystem.rotate(PieceType.T, spawn, rotation)

        // Then
        assertThat(rotated.cells).isEqualTo(cellsOf(expectedDrawing))
    }

    @ParameterizedTest(name = "{0} is back after {1} clockwise turns")
    @CsvSource("T, 4", "J, 4", "L, 4", "I, 2", "S, 2", "Z, 2", "O, 1")
    fun `four clockwise turns bring T J and L back, two are enough for I S and Z, O never changes`(
        type: PieceType,
        turns: Int,
    ) {
        // Given
        val spawn = rotationSystem.spawnOrientation(type)

        // When
        val afterEachTurn = generateSequence(spawn) { rotationSystem.rotate(type, it, Rotation.CLOCKWISE) }
            .drop(1)
            .take(turns)
            .toList()

        // Then
        assertThat(afterEachTurn.dropLast(1)).doesNotContain(spawn)
        assertThat(afterEachTurn.last()).isEqualTo(spawn)
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("everyOrientation")
    fun `a clockwise turn followed by a counterclockwise turn gives back the starting orientation`(
        type: PieceType,
        index: Int,
    ) {
        // Given
        val start = rotationSystem.orientations(type)[index]

        // When
        val clockwise = rotationSystem.rotate(type, start, Rotation.CLOCKWISE)
        val back = rotationSystem.rotate(type, clockwise, Rotation.COUNTERCLOCKWISE)

        // Then
        assertThat(back).isEqualTo(start)
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("everyOrientationAndButton")
    fun `the pivot does not move during a rotation`(
        type: PieceType,
        index: Int,
        rotation: Rotation,
    ) {
        // Given
        val piece = Piece(type, rotationSystem.orientations(type)[index], Position(5, 10))

        // When
        val candidates = rotationSystem.candidates(piece, rotation)

        // Then
        assertThat(candidates).isNotEmpty().allSatisfy { candidate ->
            assertThat(candidate.pivot).isEqualTo(Position(5, 10))
        }
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("buttons")
    fun `an orientation that does not belong to the piece type is refused`(rotation: Rotation) {
        // Given an O carrying the spawn orientation of T
        val piece = Piece(PieceType.O, rotationSystem.spawnOrientation(PieceType.T), Position(5, 10))

        // When / Then
        assertThatIllegalArgumentException()
            .isThrownBy { rotationSystem.candidates(piece, rotation) }
            .withMessageContaining("not an orientation of O")
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
        const val T_LEFT = """
            .T
            T@
            .T
            """
        const val T_RIGHT = """
            T.
            @T
            T.
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

        @JvmStatic
        fun tTurnsFromSpawn(): List<Arguments> = listOf(
            argumentSet("A (clockwise): T points left", Rotation.CLOCKWISE, T_LEFT),
            argumentSet("B (counterclockwise): T points right", Rotation.COUNTERCLOCKWISE, T_RIGHT),
        )

        @JvmStatic
        fun buttons(): List<Arguments> = listOf(
            argumentSet("A (clockwise)", Rotation.CLOCKWISE),
            argumentSet("B (counterclockwise)", Rotation.COUNTERCLOCKWISE),
        )

        @JvmStatic
        fun everyOrientation(): List<Arguments> = PieceType.entries.flatMap { type ->
            ClassicRotationSystem.orientations(type).indices.map { index ->
                argumentSet("$type, orientation $index", type, index)
            }
        }

        @JvmStatic
        fun everyOrientationAndButton(): List<Arguments> = PieceType.entries.flatMap { type ->
            ClassicRotationSystem.orientations(type).indices.flatMap { index ->
                listOf(
                    argumentSet("$type, orientation $index, A (clockwise)", type, index, Rotation.CLOCKWISE),
                    argumentSet(
                        "$type, orientation $index, B (counterclockwise)",
                        type,
                        index,
                        Rotation.COUNTERCLOCKWISE,
                    ),
                )
            }
        }
    }
}
