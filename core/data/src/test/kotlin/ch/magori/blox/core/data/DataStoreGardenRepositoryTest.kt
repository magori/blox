package ch.magori.blox.core.data

import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.garden.GardenPlacement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.io.IOException

/**
 * The contract of the garden kept, on a DataStore file, and what is proper to a file: it is kept, its format, its
 * errors.
 */
class DataStoreGardenRepositoryTest :
    GardenRepositoryContract(),
    DataStoreFileContract<GardenRepository> {
    @TempDir
    lateinit var directory: File

    @Test
    fun `the garden kept is read back, its mood too`() = runTest {
        // Given a garden of two pieces in the joyful mood, kept
        inScopeClosedAfter { scope -> repositoryIn(scope).keep(A_GARDEN) }

        // When the garden opens again, on a new repository
        val garden = repositoryIn(backgroundScope).garden.first()

        // Then
        assertThat(garden).isEqualTo(A_GARDEN)
        assertThat(garden.mood).isEqualTo(GardenMood.JOYFUL)
    }

    @Test
    fun `a garden is written in version 1, its mood then each piece by its type, its pivot and its cells`() = runTest {
        // Given
        inScopeClosedAfter { scope ->
            // When a single O in the middle, in the epic mood, is kept
            repositoryIn(scope).keep(ANOTHER_GARDEN)
        }

        // Then
        assertThat(file.readBytes()).isEqualTo(O_IN_THE_MIDDLE_EPIC_FILE)
    }

    @Test
    fun `the cells of a piece are written row by row from the top, whatever the order of its orientation`() = runTest {
        // Given a T pointing up, its pivot on column 1, row 1, whose orientation lists its top cell last
        val up = Orientation(setOf(Position(-1, 0), Position(0, 0), Position(1, 0), Position(0, -1)))
        val garden = (Garden.EMPTY.place(Piece(PieceType.T, up, Position(1, 1))) as GardenPlacement.Placed).garden

        // When
        inScopeClosedAfter { scope -> repositoryIn(scope).keep(garden) }

        // Then its top cell comes first, then the row of its pivot from the left
        assertThat(file.readBytes()).isEqualTo(
            Buffer()
                .writeUtf8("BLXG")
                .writeInt(1)
                .text("CALM")
                .writeInt(1)
                .text("T").writeInt(1).writeInt(1)
                .writeInt(0).writeInt(-1)
                .writeInt(-1).writeInt(0)
                .writeInt(0).writeInt(0)
                .writeInt(1).writeInt(0)
                .readByteArray(),
        )
    }

    @Test
    fun `a garden file of version 1 is read as it was written, a format that must not change`() = runTest {
        // Given
        file.writeBytes(O_IN_THE_MIDDLE_EPIC_FILE)

        // When
        val garden = repositoryIn(backgroundScope).garden.first()

        // Then
        val o = Orientation(setOf(Position(-1, 0), Position(0, 0), Position(-1, 1), Position(0, 1)))
        assertThat(garden.mood).isEqualTo(GardenMood.EPIC)
        assertThat(garden.pieces).containsExactly(Piece(PieceType.O, o, Position(8, 6)))
    }

    @Test
    fun `an unreadable garden gives an empty one`() = runTest {
        // Given a garden file that is not one
        file.writeBytes(byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42))

        // When
        val garden = repositoryIn(backgroundScope).garden.first()

        // Then empty and calm, without an error
        assertThat(garden).isEqualTo(Garden.EMPTY)
    }

    @Test
    fun `a garden file of a later version gives an empty garden, and is kept for that version`() = runTest {
        // Given a file of version 2, as a backup of a later BloX would bring back
        val later = Buffer().writeUtf8("BLXG").writeInt(2).text("EPIC").writeInt(0).readByteArray()
        file.writeBytes(later)
        val repository = repositoryIn(backgroundScope)

        // When
        val garden = repository.garden.first()
        val failure = runCatching { repository.keep(A_GARDEN) }.exceptionOrNull()

        // Then nothing is read, nothing is written: the garden of the later version is not lost
        assertThat(garden).isEqualTo(Garden.EMPTY)
        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(file.readBytes()).isEqualTo(later)
    }

    @Test
    fun `a garden file of a version below 1 is corrupted, and gives an empty garden`() = runTest {
        // Given a file of version 0, which no BloX writes, holding an O in the middle
        file.writeBytes(Buffer().header(version = 0, pieces = 1).o(column = 8, row = 6).readByteArray())
        val repository = repositoryIn(backgroundScope)

        // When / Then
        assertThat(repository.garden.first()).isEqualTo(Garden.EMPTY)
        repository.keep(A_GARDEN)
        assertThat(repository.garden.first()).isEqualTo(A_GARDEN)
    }

    @Test
    fun `a garden file holding two pieces on the same cells is corrupted, and gives an empty garden`() = runTest {
        // Given
        file.writeBytes(Buffer().header(pieces = 2).o(column = 8, row = 6).o(column = 8, row = 6).readByteArray())

        // When / Then
        assertThat(repositoryIn(backgroundScope).garden.first()).isEqualTo(Garden.EMPTY)
    }

    @Test
    fun `a garden file that counts billions of pieces gives an empty garden, without allocating them`() = runTest {
        // Given a file whose count of pieces is corrupted
        file.writeBytes(Buffer().header(pieces = Int.MAX_VALUE).o(column = 8, row = 6).readByteArray())

        // When / Then
        assertThat(repositoryIn(backgroundScope).garden.first()).isEqualTo(Garden.EMPTY)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("gardensThatCannotBe")
    fun `a garden file holding what no garden can be is corrupted, and gives an empty garden`(
        case: String,
        bytes: ByteArray,
    ) = runTest {
        // Given
        file.writeBytes(bytes)
        val repository = repositoryIn(backgroundScope)

        // When / Then the garden starts again empty, and the next one is kept
        assertThat(repository.garden.first()).isEqualTo(Garden.EMPTY)
        repository.keep(A_GARDEN)
        assertThat(repository.garden.first()).isEqualTo(A_GARDEN)
    }

    @Test
    fun `a corrupted garden file is copied aside before it is replaced`() = runTest {
        // Given a garden file that is not one
        val corrupted = byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42)
        file.writeBytes(corrupted)

        // When
        val garden = repositoryIn(backgroundScope).garden.first()

        // Then its bytes are kept in garden.bin.corrupt, for a later look
        assertThat(garden).isEqualTo(Garden.EMPTY)
        assertThat(File(directory, "$GARDEN_FILE.corrupt").readBytes()).isEqualTo(corrupted)
    }

    @Test
    fun `the names of the enums written in the garden file do not change`() {
        // The format of version 1 writes a mood and a type of piece by its name: renaming a constant would lose the
        // garden of every player, or change the format (a version 2, GardenSerializer).
        assertThat(GardenMood.entries.map { it.name }).containsExactly("JOYFUL", "CALM", "MYSTERIOUS", "EPIC")
        assertThat(PieceType.entries.map { it.name }).containsExactly("I", "O", "T", "S", "Z", "J", "L")
    }

    override fun emptyRepository(scope: CoroutineScope): GardenRepository = repositoryIn(scope)

    override suspend fun unwritableRepository(scope: CoroutineScope): GardenRepository {
        val dataStore = gardenDataStore(scope) { file.absolutePath.toPath() }
        DataStoreGardenRepository(dataStore).keep(A_GARDEN)
        return DataStoreGardenRepository(UnwritableDataStore(dataStore))
    }

    /** The garden file of the test, in [directory]. */
    override val file: File get() = File(directory, GARDEN_FILE)

    override fun repositoryFailingWith(failure: Exception): GardenRepository =
        DataStoreGardenRepository(FailingDataStore(failure))

    /** The garden kept. */
    override fun readsOf(repository: GardenRepository): List<Flow<Any?>> = listOf(repository.garden)

    /** The empty garden, calm. */
    override val emptyReads: List<Any?> = listOf(Garden.EMPTY)

    /** A garden of two pieces, in the joyful mood, kept. */
    override suspend fun write(repository: GardenRepository) = repository.keep(A_GARDEN)

    override val readsAfterWrite: List<Any?> = listOf(A_GARDEN)

    /** A repository on the garden file of [directory], its DataStore in [scope], as in production. */
    override fun repositoryIn(scope: CoroutineScope): GardenRepository =
        DataStoreGardenRepository(scope) { file.absolutePath.toPath() }

    private companion object {
        /** [ANOTHER_GARDEN] as the file of version 1 holds it: « BLXG », the version, then [O_IN_THE_MIDDLE_EPIC]. */
        val O_IN_THE_MIDDLE_EPIC_FILE: ByteArray =
            Buffer().writeUtf8("BLXG").writeInt(1).write(O_IN_THE_MIDDLE_EPIC).readByteArray()

        /** Files of version 1 that no garden can be, each named by what is wrong with it. */
        @JvmStatic
        fun gardensThatCannotBe(): List<Arguments> {
            val whole = Buffer().header(pieces = 1).o(column = 8, row = 6).readByteArray()
            return listOf(
                "cut short in the middle of a piece" to whole.copyOf(whole.size - 6),
                "a mood no version knows" to Buffer().header(mood = "JAZZ", pieces = 0).readByteArray(),
                "a type of piece no version knows" to
                    Buffer().header(pieces = 1).o(column = 8, row = 6, type = "X").readByteArray(),
                "a piece off the grid" to Buffer().header(pieces = 1).o(column = 0, row = 6).readByteArray(),
                "a piece whose shape is not one of its type" to
                    Buffer().header(pieces = 1)
                        .o(column = 8, row = 6, cells = listOf(-1 to 0, 0 to 0, 1 to 0, 2 to 0))
                        .readByteArray(),
                "a piece whose cells are the same" to
                    Buffer().header(pieces = 1)
                        .o(column = 8, row = 6, cells = List(4) { 0 to 0 })
                        .readByteArray(),
                "a text of a negative length" to
                    Buffer().writeUtf8("BLXG").writeInt(1).writeInt(-1).readByteArray(),
                "a negative count of pieces" to Buffer().header(pieces = -1).readByteArray(),
                "bytes after the last piece" to
                    Buffer().header(pieces = 1).o(column = 8, row = 6).writeInt(0).readByteArray(),
                "a name longer than 64 bytes" to
                    Buffer().writeUtf8("BLXG").writeInt(1).writeInt(Int.MAX_VALUE).writeUtf8("EPIC").readByteArray(),
            ).map { (case, bytes) -> Arguments.of(case, bytes) }
        }

        /** The start of a garden file:« BLXG », [version], the name of its [mood], the count of its [pieces]. */
        fun Buffer.header(
            version: Int = 1,
            mood: String = "EPIC",
            pieces: Int,
        ): Buffer = writeUtf8("BLXG").writeInt(version).text(mood).writeInt(pieces)
    }
}
