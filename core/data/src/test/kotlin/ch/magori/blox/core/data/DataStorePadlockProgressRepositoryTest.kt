package ch.magori.blox.core.data

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
 * The contract of the progress of the padlocks, on a DataStore file, and what is proper to a file: it is kept, its
 * format, its errors.
 */
class DataStorePadlockProgressRepositoryTest :
    PadlockProgressRepositoryContract(),
    DataStoreFileContract<PadlockProgressRepository> {
    @TempDir
    lateinit var directory: File

    /** The padlocks file of the test, in [directory]. */
    override val file: File get() = File(directory, PADLOCKS_FILE)

    @Test
    fun `a padlocks file is written in version 1, generation 1, each opening as pieces, moves, refusals, hints`() =
        runTest {
            // Given
            inScopeClosedAfter { scope ->
                val repository = repositoryIn(scope)

                // When padlocks 1 and 2 open
                repository.record(1, FIRST)
                repository.record(2, SECOND)
            }

            // Then « BLXS », the version, the generation of the padlocks, the count, then the openings in order
            assertThat(file.readBytes()).isEqualTo(
                Buffer()
                    .writeUtf8("BLXS")
                    .writeInt(1)
                    .writeInt(1)
                    .writeInt(2)
                    .writeInt(3).writeInt(3).writeInt(1).writeInt(0)
                    .writeInt(4).writeInt(5).writeInt(0).writeInt(2)
                    .readByteArray(),
            )
        }

    @Test
    fun `the progress survives a new repository on the same file`() = runTest {
        // Given padlocks 1 and 2 open, padlock 1 again better
        val kept = inScopeClosedAfter { scope ->
            repositoryIn(scope).run {
                record(1, FIRST)
                record(2, SECOND)
                record(1, FIRST.copy(refusals = 0))
                progress.first()
            }
        }

        // When
        val reopened = repositoryIn(backgroundScope).progress.first()

        // Then
        assertThat(reopened).isEqualTo(kept)
        assertThat(reopened.frontier).isEqualTo(3)
    }

    @Test
    fun `a padlocks file of version 1 is read as it was written, a format that must not change`() = runTest {
        // Given a file of version 1, generation 1, holding two openings
        file.writeBytes(Buffer().header(padlocks = 2).opening(3, 3, 1, 0).opening(4, 5, 0, 2).readByteArray())

        // When
        val progress = repositoryIn(backgroundScope).progress.first()

        // Then
        assertThat(progress.openings).containsExactly(FIRST, SECOND)
        assertThat(progress.frontier).isEqualTo(3)
    }

    @Test
    fun `a corrupted padlocks file is copied aside before the progress starts again empty`() = runTest {
        // Given a padlocks file that is not one
        val corrupted = byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42)
        file.writeBytes(corrupted)

        // When
        val progress = repositoryIn(backgroundScope).progress.first()

        // Then its bytes are kept in padlocks.bin.corrupt, for a later look
        assertThat(progress).isEqualTo(PadlockProgress())
        assertThat(File(directory, "$PADLOCKS_FILE.corrupt").readBytes()).isEqualTo(corrupted)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("progressesThatCannotBe")
    fun `a padlocks file holding what no progress can be is corrupted, gives an empty progress, and is written again`(
        case: String,
        bytes: ByteArray,
    ) = runTest {
        // Given
        file.writeBytes(bytes)
        val repository = repositoryIn(backgroundScope)

        // When / Then the progress starts again from padlock 1, and its next opening is kept
        assertThat(repository.progress.first()).isEqualTo(PadlockProgress())
        assertThat(repository.record(1, FIRST)).isEqualTo(OpeningOutcome.FIRST_OPENING)
        assertThat(repository.progress.first().openings).containsExactly(FIRST)
    }

    @Test
    fun `a padlocks file of a later version gives an empty progress, and is kept for that version`() = runTest {
        // Given a file of version 2, as a backup of a later BloX would bring back
        val later = Buffer().header(version = 2, padlocks = 1).opening(3, 3, 1, 0).readByteArray()
        file.writeBytes(later)
        val repository = repositoryIn(backgroundScope)

        // When
        val progress = repository.progress.first()
        val failures = listOf<suspend () -> Unit>({ repository.record(1, FIRST) }, { repository.clear() })
            .map { change -> runCatching { change() }.exceptionOrNull() }

        // Then nothing is read, nothing is written: the progress of the later version is not lost
        assertThat(progress).isEqualTo(PadlockProgress())
        assertThat(failures).allSatisfy { assertThat(it).isInstanceOf(IOException::class.java) }
        assertThat(file.readBytes()).isEqualTo(later)
    }

    @Test
    fun `openings of a later generation of padlocks give an empty progress, and are kept for that generation`() =
        runTest {
            // Given a file of generation 2: openings of padlocks that this BloX does not make
            val later = Buffer().header(generation = 2, padlocks = 1).opening(3, 3, 1, 0).readByteArray()
            file.writeBytes(later)
            val repository = repositoryIn(backgroundScope)

            // When
            val progress = repository.progress.first()
            val failure = runCatching { repository.record(1, FIRST) }.exceptionOrNull()

            // Then they are not taken for openings of the padlocks of generation 1, nor lost
            assertThat(progress).isEqualTo(PadlockProgress())
            assertThat(failure).isInstanceOf(IOException::class.java)
            assertThat(file.readBytes()).isEqualTo(later)
        }

    @Test
    fun `a padlocks file that counts billions of padlocks gives an empty progress, the file being found cut short`() =
        runTest {
            // Given a file whose count of padlocks is corrupted
            file.writeBytes(Buffer().header(padlocks = Int.MAX_VALUE).opening(3, 3, 1, 0).readByteArray())

            // When / Then the file is found cut short, before a list of billions of openings is made
            assertThat(repositoryIn(backgroundScope).progress.first()).isEqualTo(PadlockProgress())
        }

    override fun emptyRepository(scope: CoroutineScope): PadlockProgressRepository = repositoryIn(scope)

    override fun repositoryIn(scope: CoroutineScope): PadlockProgressRepository =
        DataStorePadlockProgressRepository(scope) { file.absolutePath.toPath() }

    override fun repositoryFailingWith(failure: Exception): PadlockProgressRepository =
        DataStorePadlockProgressRepository(FailingDataStore(failure))

    /** The progress. */
    override fun readsOf(repository: PadlockProgressRepository): List<Flow<Any?>> = listOf(repository.progress)

    /** No padlock open. */
    override val emptyReads: List<Any?> = listOf(PadlockProgress())

    /** Padlock 1 opened, a first opening. */
    override suspend fun write(repository: PadlockProgressRepository) {
        assertThat(repository.record(1, FIRST)).isEqualTo(OpeningOutcome.FIRST_OPENING)
    }

    /** Padlock 1 open. */
    override val readsAfterWrite: List<Any?> = listOf(PadlockProgress(listOf(FIRST)))

    private companion object {
        /** Files of version 1 that no progress can be, each named by what is wrong with it. */
        @JvmStatic
        fun progressesThatCannotBe(): List<Arguments> {
            val whole = Buffer().header(padlocks = 1).opening(3, 3, 1, 0).readByteArray()
            return listOf(
                "not a padlocks file" to byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42),
                "a version below 1, which no BloX writes" to
                    Buffer().header(version = 0, padlocks = 1).opening(3, 3, 1, 0).readByteArray(),
                "a generation below 1, which no BloX writes" to
                    Buffer().header(generation = 0, padlocks = 1).opening(3, 3, 1, 0).readByteArray(),
                "cut short in the middle of an opening" to whole.copyOf(whole.size - 2),
                "a negative count of padlocks" to Buffer().header(padlocks = -1).readByteArray(),
                "an opening without pieces" to Buffer().header(padlocks = 1).opening(0, 0, 0, 0).readByteArray(),
                "an opening in fewer moves than pieces" to
                    Buffer().header(padlocks = 1).opening(3, 2, 0, 0).readByteArray(),
                "a negative count of refusals" to Buffer().header(padlocks = 1).opening(3, 3, -1, 0).readByteArray(),
                "a negative count of hints" to Buffer().header(padlocks = 1).opening(3, 3, 0, -1).readByteArray(),
                "bytes after the last opening" to
                    Buffer().header(padlocks = 1).opening(3, 3, 1, 0).writeInt(0).readByteArray(),
            ).map { (case, bytes) -> Arguments.of(case, bytes) }
        }

        /** An opening of padlock 1, of 3 pieces, with a refusal. */
        val FIRST = PadlockOpening(pieces = 3, moves = 3, refusals = 1, hints = 0)

        /** An opening of padlock 2, of 4 pieces, with a piece taken back and two hints. */
        val SECOND = PadlockOpening(pieces = 4, moves = 5, refusals = 0, hints = 2)

        /** The start of a padlocks file: « BLXS », [version], [generation], the count of the open [padlocks]. */
        fun Buffer.header(
            version: Int = 1,
            generation: Int = 1,
            padlocks: Int,
        ): Buffer = writeUtf8("BLXS").writeInt(version).writeInt(generation).writeInt(padlocks)

        /** An opening as the padlocks file writes it: its [pieces], [moves], [refusals] and [hints]. */
        fun Buffer.opening(
            pieces: Int,
            moves: Int,
            refusals: Int,
            hints: Int,
        ): Buffer = writeInt(pieces).writeInt(moves).writeInt(refusals).writeInt(hints)
    }
}
