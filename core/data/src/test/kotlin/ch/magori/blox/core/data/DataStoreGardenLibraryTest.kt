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
import java.time.Clock
import java.time.Instant

/**
 * The contract of the kept gardens, on a DataStore file, and what is proper to a file: it is kept, its format, its
 * errors.
 */
class DataStoreGardenLibraryTest :
    GardenLibraryContract(),
    DataStoreFileContract<GardenLibrary> {
    @TempDir
    lateinit var directory: File

    @Test
    fun `the kept gardens survive a new library on the same file`() = runTest {
        // Given two gardens kept, the first one renamed
        val kept = inScopeClosedAfter { scope ->
            libraryIn(scope).run {
                val first = keep("Jardin calme", A_GARDEN)
                keep("Pluie douce", ANOTHER_GARDEN)
                rename(first.id, "Berceuse")
                gardens.first()
            }
        }

        // When
        val reopened = libraryIn(backgroundScope).gardens.first()

        // Then
        assertThat(reopened).isEqualTo(kept)
        assertThat(reopened.map { it.name }).containsExactly("Berceuse", "Pluie douce")
    }

    @Test
    fun `a gardens file is written in version 1, each garden by its identity, its name, its time and its body`() =
        runTest {
            // Given a library whose clock tells 20 October 2026 at ten, Greenwich time
            val kept = inScopeClosedAfter { scope ->
                // When a single O in the middle, in the epic mood, is kept
                libraryIn(scope).keep("Orage", ANOTHER_GARDEN)
            }

            // Then « BLXJ », the version, the count, then the garden: the time in milliseconds since 1970, then the
            // garden as a single garden is encoded
            assertThat(file.readBytes()).isEqualTo(
                Buffer()
                    .writeUtf8("BLXJ")
                    .writeInt(1)
                    .writeInt(1)
                    .text(kept.id.key).text("Orage").writeLong(1_792_490_400_000)
                    .write(O_IN_THE_MIDDLE_EPIC)
                    .readByteArray(),
            )
        }

    @Test
    fun `a gardens file of version 1 is read as it was written, a format that must not change`() = runTest {
        // Given a file of version 1 holding two gardens: an O in the middle, epic, then the same O, named with a moon
        file.writeBytes(
            Buffer()
                .header(gardens = 2)
                .keptGarden("a", "Orage")
                .keptGarden("b", "Nuit 🌙", millis = 1_792_494_000_000)
                .readByteArray(),
        )

        // When
        val gardens = libraryIn(backgroundScope).gardens.first()

        // Then
        assertThat(gardens).containsExactly(
            KeptGarden(GardenId("a"), "Orage", ANOTHER_GARDEN, Instant.parse("2026-10-20T10:00:00Z")),
            KeptGarden(GardenId("b"), "Nuit 🌙", ANOTHER_GARDEN, Instant.parse("2026-10-20T11:00:00Z")),
        )
    }

    @Test
    fun `a gardens file of a later version gives an empty list, and is kept for that version`() = runTest {
        // Given a file of version 2, as a backup of a later BloX would bring back
        val later = Buffer().header(version = 2, gardens = 1).keptGarden("a", "Orage").readByteArray()
        file.writeBytes(later)
        val library = libraryIn(backgroundScope)

        // When
        val gardens = library.gardens.first()
        val failure = runCatching { library.keep("Jardin calme", A_GARDEN) }.exceptionOrNull()

        // Then nothing is read, nothing is written: the gardens of the later version are not lost
        assertThat(gardens).isEmpty()
        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(file.readBytes()).isEqualTo(later)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("librariesThatCannotBe")
    fun `a gardens file holding what no library can be is corrupted, gives an empty list, and is written again`(
        case: String,
        bytes: ByteArray,
    ) = runTest {
        // Given
        file.writeBytes(bytes)
        val library = libraryIn(backgroundScope)

        // When / Then the list starts again empty, and the next garden is kept
        assertThat(library.gardens.first()).isEmpty()
        val kept = library.keep("Jardin calme", A_GARDEN)
        assertThat(library.gardens.first()).containsExactly(kept)
    }

    @Test
    fun `a gardens file that counts billions of gardens gives an empty list, without allocating them`() = runTest {
        // Given a file whose count of gardens is corrupted
        file.writeBytes(Buffer().header(gardens = Int.MAX_VALUE).keptGarden("a", "Orage").readByteArray())

        // When / Then
        assertThat(libraryIn(backgroundScope).gardens.first()).isEmpty()
    }

    @Test
    fun `a corrupted gardens file is copied aside before the list starts again empty`() = runTest {
        // Given a gardens file that is not one
        val corrupted = byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42)
        file.writeBytes(corrupted)

        // When
        val gardens = libraryIn(backgroundScope).gardens.first()

        // Then its bytes are kept in gardens.bin.corrupt, for a later look
        assertThat(gardens).isEmpty()
        assertThat(File(directory, "$GARDENS_FILE.corrupt").readBytes()).isEqualTo(corrupted)
    }

    @Test
    fun `a garden the file cannot hold is refused before a byte is written, and the kept gardens are kept`() = runTest {
        // Given a kept garden, and a deleted garden of an identity longer than the file may hold, made up by hand
        val tooLong = DeletedGarden(KeptGarden(GardenId("x".repeat(65)), "Orage", A_GARDEN, MODIFIED_AT), place = 0)
        val (kept, failure) = inScopeClosedAfter { scope ->
            val library = libraryIn(scope)
            val kept = library.keep("Jardin calme", A_GARDEN)

            // When
            kept to runCatching { library.restore(tooLong) }.exceptionOrNull()
        }

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(libraryIn(backgroundScope).gardens.first()).containsExactly(kept)
    }

    override fun emptyLibrary(
        scope: CoroutineScope,
        clock: Clock,
    ): GardenLibrary = libraryIn(scope, clock)

    /** The gardens file of the test, in [directory]. */
    override val file: File get() = File(directory, GARDENS_FILE)

    override fun repositoryIn(scope: CoroutineScope): GardenLibrary = libraryIn(scope)

    override fun repositoryFailingWith(failure: Exception): GardenLibrary =
        DataStoreGardenLibrary(FailingDataStore(failure), MutableClock())

    /** The kept gardens. */
    override fun readsOf(repository: GardenLibrary): List<Flow<Any?>> = listOf(repository.gardens)

    /** No garden. */
    override val emptyReads: List<Any?> = listOf(emptyList<KeptGarden>())

    /** A garden kept, under the identity the library gives it. */
    override suspend fun write(repository: GardenLibrary) {
        written = repository.keep("Jardin calme", A_GARDEN)
    }

    /** The garden of [write]. */
    private var written: KeptGarden? = null

    override val readsAfterWrite: List<Any?> get() = listOf(listOfNotNull(written))

    /** A library on the gardens file of [directory], its DataStore in [scope], dating by [clock], as in production. */
    private fun libraryIn(
        scope: CoroutineScope,
        clock: Clock = MutableClock(),
    ): GardenLibrary = DataStoreGardenLibrary(scope, clock) { file.absolutePath.toPath() }

    private companion object {
        /** Files of version 1 that no library can be, each named by what is wrong with it. */
        @JvmStatic
        fun librariesThatCannotBe(): List<Arguments> {
            val whole = Buffer().header(gardens = 1).keptGarden("a", "Orage").readByteArray()
            val tooMany = Buffer().header(gardens = 31).apply {
                repeat(31) { keptGarden("g$it", "Jardin $it") }
            }.readByteArray()
            return listOf(
                "not a gardens file" to byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42),
                "a version below 1, which no BloX writes" to
                    Buffer().header(version = 0, gardens = 1).keptGarden("a", "Orage").readByteArray(),
                "cut short in the middle of a garden" to whole.copyOf(whole.size - 6),
                "a negative count of gardens" to Buffer().header(gardens = -1).readByteArray(),
                "31 gardens, beyond the most a library holds" to tooMany,
                "two gardens of the same identity" to
                    Buffer().header(gardens = 2).keptGarden("a", "Orage").keptGarden("a", "Pluie").readByteArray(),
                "two gardens of the same name, the case ignored" to
                    Buffer().header(gardens = 2).keptGarden("a", "Orage").keptGarden("b", "ORAGE").readByteArray(),
                "a blank identity" to Buffer().header(gardens = 1).keptGarden("", "Orage").readByteArray(),
                "a blank name" to Buffer().header(gardens = 1).keptGarden("a", "").readByteArray(),
                "a name of 41 characters" to
                    Buffer().header(gardens = 1).keptGarden("a", "x".repeat(41)).readByteArray(),
                "an identity of a length beyond any" to
                    Buffer().header(gardens = 1).writeInt(Int.MAX_VALUE).writeUtf8("a").readByteArray(),
                "a garden that cannot be" to
                    Buffer().header(gardens = 1)
                        .keptGarden("a", "Orage", body = Buffer().text("JAZZ").writeInt(0).readByteArray())
                        .readByteArray(),
                "bytes after the last garden" to
                    Buffer().header(gardens = 1).keptGarden("a", "Orage").writeInt(0).readByteArray(),
            ).map { (case, bytes) -> Arguments.of(case, bytes) }
        }

        /** The start of a gardens file: « BLXJ », [version], the count of its [gardens]. */
        fun Buffer.header(
            version: Int = 1,
            gardens: Int,
        ): Buffer = writeUtf8("BLXJ").writeInt(version).writeInt(gardens)

        /**
         * A garden as the gardens file writes it: its identity [key], its [name], its time in [millis] since 1970 (by
         * default 20 October 2026 at ten, Greenwich time), then its [body], by default [O_IN_THE_MIDDLE_EPIC].
         */
        fun Buffer.keptGarden(
            key: String,
            name: String,
            millis: Long = 1_792_490_400_000,
            body: ByteArray = O_IN_THE_MIDDLE_EPIC,
        ): Buffer = text(key).text(name).writeLong(millis).write(body)
    }
}
