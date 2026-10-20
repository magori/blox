package ch.magori.blox.core.data

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant

/** The contract of the saved musics, on a DataStore file, and what is proper to a file: it is kept, its errors. */
class DataStoreMusicLibraryTest : MusicLibraryContract() {
    @TempDir
    lateinit var directory: File

    @Test
    fun `the musics and the one played during the games survive a new library on the same file`() = runTest {
        // Given a music generated in E major, one written by hand in D dorian, the second one played during the games
        val generated = aMusic("a", name = "Joyeux 2")
        val written =
            aMusic("b", name = "À la main", category = null, scale = Scale(Pitch.parse("D4"), ScaleType.DORIAN))
        inScopeClosedAfter { scope ->
            libraryIn(scope).apply {
                save(generated)
                save(written)
                setGameMusic(ScoreId.Personal("b"))
            }
        }

        // When
        val reopened = libraryIn(backgroundScope)

        // Then
        assertThat(reopened.musics.first()).containsExactly(generated, written)
        assertThat(reopened.gameMusic.first()).isEqualTo(written)
    }

    @Test
    fun `a corrupted musics file gives an empty list, and a new music is saved`() = runTest {
        // Given a musics file that is not one
        musicsFile.writeBytes(byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42))
        val library = libraryIn(backgroundScope)

        // When / Then
        assertThat(library.musics.first()).isEmpty()
        library.save(aMusic("a", name = "Calme"))
        assertThat(library.musics.first()).containsExactly(aMusic("a", name = "Calme"))
    }

    @Test
    fun `a musics file of version 1 is read as it was written, a format that must not change`() = runTest {
        // Given a file of version 1, after its four bytes « BLXM »: no music played during the games, then a calm
        // music of one bar, a rest then a C5
        musicsFile.writeBytes(
            Buffer()
                .writeUtf8("BLXM")
                .writeInt(1)
                .text("")
                .writeInt(1)
                .text("a").text("Calme").text("CALM")
                .writeInt(60).text("MAJOR_PENTATONIC")
                .writeInt(80).writeInt(4)
                .writeInt(1)
                .text("SQUARE").writeLong(0.07.toRawBits())
                .writeInt(2)
                .writeInt(-1).writeLong(2.0.toRawBits())
                .writeInt(72).writeLong(2.0.toRawBits())
                .readByteArray(),
        )

        // When
        val musics = libraryIn(backgroundScope).musics.first()

        // Then
        val id = ScoreId.Personal("a")
        val voice = Voice(Waveform.SQUARE, 0.07, listOf(Note(null, 2.0), Note(Pitch.parse("C5"), 2.0)))
        assertThat(musics).containsExactly(
            SavedMusic(
                id = id,
                name = "Calme",
                category = Category.CALM,
                scale = Scale(Pitch.parse("C4"), ScaleType.MAJOR_PENTATONIC),
                score = Score(id, bpm = 80, beatsPerBar = 4, voices = listOf(voice)),
                // Version 1 did not keep the time of the changes: it is unknown.
                modifiedAt = null,
            ),
        )
    }

    @Test
    fun `a musics file is written in version 2, the time of the last change of each music after its name`() = runTest {
        // Given a library whose clock tells 20 October 2026 at ten, Greenwich time
        inScopeClosedAfter { scope ->
            // When a calm music of one bar, a rest then a C5, is saved
            val id = ScoreId.Personal("a")
            val voice = Voice(Waveform.SQUARE, 0.07, listOf(Note(null, 2.0), Note(Pitch.parse("C5"), 2.0)))
            libraryIn(scope).save(
                SavedMusic(
                    id = id,
                    name = "Calme",
                    category = Category.CALM,
                    scale = Scale(Pitch.parse("C4"), ScaleType.MAJOR_PENTATONIC),
                    score = Score(id, bpm = 80, beatsPerBar = 4, voices = listOf(voice)),
                ),
            )
        }

        // Then the file holds it in version 2: its time, known (1), in milliseconds since 1970, follows its name
        assertThat(musicsFile.readBytes()).isEqualTo(
            Buffer()
                .writeUtf8("BLXM")
                .writeInt(2)
                .text("")
                .writeInt(1)
                .text("a").text("Calme")
                .writeByte(1).writeLong(1_792_490_400_000)
                .text("CALM")
                .writeInt(60).text("MAJOR_PENTATONIC")
                .writeInt(80).writeInt(4)
                .writeInt(1)
                .text("SQUARE").writeLong(0.07.toRawBits())
                .writeInt(2)
                .writeInt(-1).writeLong(2.0.toRawBits())
                .writeInt(72).writeLong(2.0.toRawBits())
                .readByteArray(),
        )
    }

    @Test
    fun `a musics file of version 2 is read as it was written, a format that must not change`() = runTest {
        // Given a file of version 2: a music of a time known, 20 October 2026 at ten in Greenwich, then one of a time
        // unknown, which a music of version 1 written again keeps
        musicsFile.writeBytes(
            Buffer()
                .header(version = 2, musics = 2)
                .music("a", "Calme", time = { writeByte(1).writeLong(1_792_490_400_000) })
                .music("b", "Joyeux", time = { writeByte(0) })
                .readByteArray(),
        )

        // When
        val musics = libraryIn(backgroundScope).musics.first()

        // Then
        assertThat(musics.map { it.name to it.modifiedAt }).containsExactly(
            "Calme" to Instant.parse("2026-10-20T10:00:00Z"),
            "Joyeux" to null,
        )
    }

    @Test
    fun `the musics of a file of version 1 keep a time unknown once the file is written in version 2`() = runTest {
        // Given a file of version 1, which did not keep the time of the changes
        musicsFile.writeBytes(Buffer().header(musics = 1).music("a", "Calme").readByteArray())

        // When another music is saved, and the file opened again
        inScopeClosedAfter { scope -> libraryIn(scope).save(aMusic("b", name = "Joyeux")) }
        val musics = libraryIn(backgroundScope).musics.first()

        // Then the file is of version 2, the music of version 1 of a time unknown, not of the time of the migration
        assertThat(Buffer().write(musicsFile.readBytes()).apply { skip(4) }.readInt()).isEqualTo(2)
        assertThat(musics.map { it.name to it.modifiedAt }).containsExactly("Calme" to null, "Joyeux" to MODIFIED_AT)
    }

    @Test
    fun `a time marked neither known nor unknown makes the musics file corrupted`() = runTest {
        // Given a file of version 2 whose only music has a time marked 2
        musicsFile.writeBytes(
            Buffer().header(version = 2, musics = 1).music("a", "Calme", time = { writeByte(2) }).readByteArray(),
        )

        // When / Then
        assertThat(libraryIn(backgroundScope).musics.first()).isEmpty()
        assertThat(File(directory, "$MUSICS_FILE.corrupt")).exists()
    }

    @Test
    fun `a musics file of a later version gives an empty list, and is kept for that version`() = runTest {
        // Given a file of version 3, as a backup of a later BloX would bring back
        val later = Buffer().writeUtf8("BLXM").writeInt(3).text("").writeInt(0).readByteArray()
        musicsFile.writeBytes(later)
        val library = libraryIn(backgroundScope)

        // When
        val musics = library.musics.first()
        val failure = runCatching { library.save(aMusic("a", name = "Calme")) }.exceptionOrNull()

        // Then nothing is read, nothing is written: the musics of the later version are not lost
        assertThat(musics).isEmpty()
        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(musicsFile.readBytes()).isEqualTo(later)
    }

    @Test
    fun `a musics file that counts billions of musics gives an empty list, without allocating them`() = runTest {
        // Given a file of version 1 whose count of musics is corrupted
        musicsFile.writeBytes(Buffer().writeUtf8("BLXM").writeInt(1).text("").writeInt(Int.MAX_VALUE).readByteArray())

        // When / Then
        assertThat(libraryIn(backgroundScope).musics.first()).isEmpty()
    }

    @Test
    fun `a musics file that cannot be read gives an empty list and no music for the games`() = runTest {
        // Given a directory where the musics file should be: reading it fails with an IOException
        musicsFile.mkdirs()
        val library = libraryIn(backgroundScope)

        // When / Then
        assertThat(library.musics.first()).isEmpty()
        assertThat(library.gameMusic.first()).isNull()
    }

    @Test
    fun `a failure other than an IOException while the musics are read is not absorbed`() = runTest {
        // Given a DataStore whose reading fails with a bug
        val library = DataStoreMusicLibrary(FailingDataStore(IllegalStateException("a bug")), MutableClock())

        // When
        val failures = listOf(library.musics, library.gameMusic).map { runCatching { it.first() }.exceptionOrNull() }

        // Then
        assertThat(failures).allSatisfy {
            assertThat(it).isInstanceOf(IllegalStateException::class.java).hasMessage("a bug")
        }
    }

    @Test
    fun `a music whose name is too long is refused and the saved musics are kept`() = runTest {
        // Given a saved music
        val failure = inScopeClosedAfter { scope ->
            val library = libraryIn(scope)
            library.save(aMusic("a", name = "Calme"))

            // When a music of a name of 41 characters is saved
            runCatching { library.save(aMusic("b", name = "x".repeat(41))) }.exceptionOrNull()
        }

        // Then it is refused, and the file still holds the saved music
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(libraryIn(backgroundScope).musics.first()).containsExactly(aMusic("a", name = "Calme"))
    }

    @Test
    fun `a music the file cannot hold is refused before a byte is written, and the saved musics are kept`() = runTest {
        // Given a saved music, and a music of one voice of more notes than a list of the file may hold
        val id = ScoreId.Personal("b")
        val notes = List(65_537) { Note(Pitch.parse("C4"), 1.0) }
        val tooLong = SavedMusic(
            id = id,
            name = "Interminable",
            category = null,
            scale = Scale(Pitch.parse("C4"), ScaleType.MAJOR),
            score = Score(id, bpm = 120, beatsPerBar = 1, voices = listOf(Voice(Waveform.SQUARE, 0.07, notes))),
        )
        val failure = inScopeClosedAfter { scope ->
            val library = libraryIn(scope)
            library.save(aMusic("a", name = "Calme"))

            // When
            runCatching { library.save(tooLong) }.exceptionOrNull()
        }

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(libraryIn(backgroundScope).musics.first()).containsExactly(aMusic("a", name = "Calme"))
    }

    @Test
    fun `the names of the enums written in the musics file do not change`() {
        // The format of version 1 writes a category, a type of scale and a waveform by its name: renaming a constant
        // would lose the saved musics of every player, or change the format (a version 2, MusicLibrarySerializer).
        assertThat(Category.entries.map { it.name }).containsExactly(
            "JOYFUL",
            "CALM",
            "EPIC",
            "MELANCHOLIC",
            "MYSTERIOUS",
            "RETRO",
            "DANCE",
        )
        assertThat(ScaleType.entries.map { it.name }).containsExactly(
            "MAJOR",
            "NATURAL_MINOR",
            "HARMONIC_MINOR",
            "DORIAN",
            "MIXOLYDIAN",
            "MAJOR_PENTATONIC",
        )
        assertThat(Waveform.entries.map { it.name }).containsExactly("SQUARE", "TRIANGLE")
    }

    @Test
    fun `a music of a category the file does not know is read without a category`() = runTest {
        // Given a file of version 1 whose only music has a category that no longer exists
        musicsFile.writeBytes(Buffer().header(musics = 1).music("a", "Calme", category = "JAZZ").readByteArray())

        // When
        val musics = libraryIn(backgroundScope).musics.first()

        // Then the category is lost, not the music
        assertThat(musics.map { it.name to it.category }).containsExactly("Calme" to null)
    }

    @Test
    fun `a musics file cut short in the middle of a music gives an empty list, and a new music is saved`() = runTest {
        // Given a file of version 1 whose only music lacks its last bytes
        val whole = Buffer().header(musics = 1).music("a", "Calme").readByteArray()
        musicsFile.writeBytes(whole.copyOf(whole.size - 6))
        val library = libraryIn(backgroundScope)

        // When / Then
        assertThat(library.musics.first()).isEmpty()
        library.save(aMusic("b", name = "Joyeux"))
        assertThat(library.musics.first()).containsExactly(aMusic("b", name = "Joyeux"))
    }

    @Test
    fun `a musics file of a version below 1 is corrupted, and gives an empty list`() = runTest {
        // Given a file of version 0, which no BloX writes
        musicsFile.writeBytes(Buffer().header(version = 0, musics = 1).music("a", "Calme").readByteArray())
        val library = libraryIn(backgroundScope)

        // When / Then
        assertThat(library.musics.first()).isEmpty()
        library.save(aMusic("b", name = "Joyeux"))
        assertThat(library.musics.first()).containsExactly(aMusic("b", name = "Joyeux"))
    }

    @Test
    fun `a corrupted musics file is copied aside before the list starts again empty`() = runTest {
        // Given a musics file that is not one
        val corrupted = byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42)
        musicsFile.writeBytes(corrupted)

        // When
        val musics = libraryIn(backgroundScope).musics.first()

        // Then its bytes are kept in musics.bin.corrupt, for a later look
        assertThat(musics).isEmpty()
        assertThat(File(directory, "$MUSICS_FILE.corrupt").readBytes()).isEqualTo(corrupted)
    }

    @Test
    fun `a music for the games the file does not hold is forgotten, even when a music of its identity is saved`() =
        runTest {
            // Given a file whose music for the games is none of its musics
            musicsFile.writeBytes(Buffer().header(gameMusic = "z", musics = 1).music("a", "Calme").readByteArray())
            val library = libraryIn(backgroundScope)

            // When a music of that identity is saved
            library.save(aMusic("z", name = "Joyeux"))

            // Then it is not played during the games
            assertThat(library.gameMusic.first()).isNull()
        }

    @Test
    fun `a musics file holding two musics of the same identity is corrupted, and gives an empty list`() = runTest {
        // Given
        musicsFile.writeBytes(Buffer().header(musics = 2).music("a", "Calme").music("a", "Joyeux").readByteArray())

        // When / Then
        assertThat(libraryIn(backgroundScope).musics.first()).isEmpty()
    }

    @Test
    fun `a musics file of version 1 with a music for the games, written by hand, is read as it was written`() =
        runTest {
            // Given a file of version 1 whose only music, played during the games, has no category and a name of 9
            // bytes in UTF-8: « Nuit » and a space, then a moon of 4 bytes
            musicsFile.writeBytes(
                Buffer()
                    .writeUtf8("BLXM")
                    .writeInt(1)
                    .text("n")
                    .writeInt(1)
                    .text("n")
                    .writeInt(9).writeUtf8("Nuit 🌙")
                    .text("")
                    .writeInt(62).text("DORIAN")
                    .writeInt(90).writeInt(4)
                    .writeInt(1)
                    .text("TRIANGLE").writeLong(0.16.toRawBits())
                    .writeInt(1)
                    .writeInt(38).writeLong(4.0.toRawBits())
                    .readByteArray(),
            )
            val library = libraryIn(backgroundScope)

            // When
            val musics = library.musics.first()

            // Then
            val id = ScoreId.Personal("n")
            val voice = Voice(Waveform.TRIANGLE, 0.16, listOf(Note(Pitch.parse("D2"), 4.0)))
            val night = SavedMusic(
                id = id,
                name = "Nuit 🌙",
                category = null,
                scale = Scale(Pitch.parse("D4"), ScaleType.DORIAN),
                score = Score(id, bpm = 90, beatsPerBar = 4, voices = listOf(voice)),
            )
            assertThat(musics).containsExactly(night)
            assertThat(library.gameMusic.first()).isEqualTo(night)
        }

    override fun emptyLibrary(
        scope: CoroutineScope,
        clock: Clock,
    ): MusicLibrary = libraryIn(scope, clock)

    /** Runs [block] in a scope of its own, then closes it: the DataStores living in it release the file. */
    private suspend fun <T> TestScope.inScopeClosedAfter(block: suspend (CoroutineScope) -> T): T {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        val result = block(scope)
        scope.coroutineContext.job.cancelAndJoin()
        return result
    }

    /**
     * The start of a musics file: « BLXM », [version], the key of the music played during the games, the count of
     * musics.
     */
    private fun Buffer.header(
        version: Int = 1,
        gameMusic: String = "",
        musics: Int,
    ): Buffer = writeUtf8("BLXM").writeInt(version).text(gameMusic).writeInt(musics)

    /**
     * A music as the musics file writes it: one bar of 4 beats in C major, a melody of one C5. Its [time] follows its
     * name in version 2; version 1 has none, the default.
     */
    private fun Buffer.music(
        key: String,
        name: String,
        category: String = "CALM",
        time: Buffer.() -> Buffer = { this },
    ): Buffer = text(key).text(name).time().text(category)
        .writeInt(60).text("MAJOR")
        .writeInt(120).writeInt(4)
        .writeInt(1)
        .text("SQUARE").writeLong(0.07.toRawBits())
        .writeInt(1)
        .writeInt(72).writeLong(4.0.toRawBits())

    /** A text as the musics file writes it: its length in bytes, then its bytes in UTF-8. */
    private fun Buffer.text(text: String): Buffer = writeInt(text.encodeToByteArray().size).writeUtf8(text)

    /** The musics file of the test, in [directory]. */
    private val musicsFile: File get() = File(directory, MUSICS_FILE)

    /** A library on the musics file of [directory], its DataStore in [scope], dating by [clock], as in production. */
    private fun libraryIn(
        scope: CoroutineScope,
        clock: Clock = MutableClock(),
    ): MusicLibrary = DataStoreMusicLibrary(scope, clock) { musicsFile.absolutePath.toPath() }
}
