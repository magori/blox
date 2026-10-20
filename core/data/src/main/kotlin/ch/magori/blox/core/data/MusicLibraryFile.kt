package ch.magori.blox.core.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
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
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import java.io.EOFException
import java.io.IOException
import java.time.Instant

/** The file of the saved musics, apart from the settings and the records. */
const val MUSICS_FILE: String = "musics.bin"

/**
 * A DataStore of saved musics living in [scope], on the file [produceFile] gives; one instance per file. A corrupted
 * file is replaced by an empty one, as the files of the settings and of the records are: the list starts again empty, the corrupted
 * file copied aside first ([keepAside]).
 */
internal fun musicLibraryDataStore(
    scope: CoroutineScope,
    produceFile: () -> Path,
): DataStore<MusicLibraryContent> = DataStoreFactory.create(
    storage = OkioStorage(FileSystem.SYSTEM, MusicLibrarySerializer, producePath = produceFile),
    corruptionHandler = ReplaceFileCorruptionHandler {
        keepAside(produceFile())
        MusicLibraryContent()
    },
    scope = scope,
)

/**
 * Copies the corrupted file at [path] to `<path>.corrupt`, replacing an older copy, before the DataStore replaces it: the
 * musics of the player may still be found in it. A copy that fails (a full disk) is given up, and the list still starts
 * again empty.
 */
private fun keepAside(path: Path) {
    try {
        FileSystem.SYSTEM.copy(path, checkNotNull(path.parent) / "${path.name}$CORRUPTED_SUFFIX")
    } catch (_: IOException) {
        // The copy is a help for later, not a condition of reading the musics again.
    }
}

/** What the copy of a corrupted musics file adds to its name. */
private const val CORRUPTED_SUFFIX = ".corrupt"

/**
 * The format of the musics file, a format of BloX on Okio, without a library of its own: [MAGIC], its [VERSION], the
 * identity of the music played during the games (an empty text when there is none), then the count of the musics and
 * each music in turn: its identity, its name, the time of its last change (since version 2: a byte, 0 for a time
 * unknown, or 1 then its milliseconds since 1970), its category (an empty text when there is none), its scale (the MIDI
 * number of its tonic, its type), its tempo, its beats per bar, then its voices, each one its waveform, its volume
 * and its notes, each one its MIDI number ([REST] for a rest) and its beats. The enums are written by their names,
 * which must not change; a text is its length in bytes, then its bytes in UTF-8; a decimal number is its 64 bits.
 */
internal object MusicLibrarySerializer : OkioSerializer<MusicLibraryContent> {
    /** The first four bytes of every musics file, « BLXM » in ASCII: a file without them is not one. */
    private const val MAGIC = 0x424C584D

    /** The version of the format, written after [MAGIC]: a change of the format gets a number of its own. */
    private const val VERSION = 2

    override val defaultValue: MusicLibraryContent = MusicLibraryContent()

    /**
     * The content of [source]. A file cut short or holding what no music can be (an unknown type of scale or waveform, a pitch
     * out of range, voices of different lengths, two musics of the same identity) is
     * corrupted: the DataStore replaces it with an empty one.
     * A file of a later version, which a backup of a later BloX may bring back, cannot be read: it is kept for that
     * version, and the musics cannot be written until it comes.
     */
    override suspend fun readFrom(source: BufferedSource): MusicLibraryContent = try {
        with(source) {
            if (readInt() != MAGIC) throw CorruptionException("The musics file is not one")
            val version = readInt()
            if (version < 1) throw CorruptionException("The musics file is of version $version, which no BloX writes")
            if (version > VERSION) throw IOException("The musics file is of version $version, later than $VERSION")
            val gameMusicId = readText().takeIf { it.isNotEmpty() }?.let(ScoreId::Personal)
            val musics = List(readCount()) { readMusic(version) }
            require(musics.distinctBy { it.id }.size == musics.size) { "Two musics of the file have the same identity" }
            // A music for the games that is none of the musics is forgotten: no music saved later takes its place.
            MusicLibraryContent(musics, gameMusicId?.takeIf { id -> musics.any { it.id == id } })
        }
    } catch (cutShort: EOFException) {
        throw CorruptionException("The musics file is cut short", cutShort)
    } catch (invalid: IllegalArgumentException) {
        throw CorruptionException("The musics file holds what no music can be", invalid)
    }

    override suspend fun writeTo(
        t: MusicLibraryContent,
        sink: BufferedSink,
    ) {
        requireFits(t)
        with(sink) {
            writeInt(MAGIC)
            writeInt(VERSION)
            writeText(t.gameMusicId?.key.orEmpty())
            writeInt(t.musics.size)
            t.musics.forEach { writeMusic(it) }
        }
    }

    /** A music as [version] of the format writes it: version 1 does not keep the time of its last change. */
    private fun BufferedSource.readMusic(version: Int): SavedMusic {
        val id = ScoreId.Personal(readText())
        val name = readText()
        val modifiedAt = if (version >= DATED_SINCE) readTime() else null
        // A category a later version removed is lost, not the music: it reads as a music written by hand.
        val category = readText().let { name -> Category.entries.firstOrNull { it.name == name } }
        val scale = Scale(Pitch(readInt()), ScaleType.valueOf(readText()))
        val bpm = readInt()
        val beatsPerBar = readInt()
        val voices = List(readCount()) { readVoice() }
        return SavedMusic(id, name, category, scale, Score(id, bpm, beatsPerBar, voices), modifiedAt)
    }

    private fun BufferedSink.writeMusic(music: SavedMusic) {
        writeText(music.id.key)
        writeText(music.name)
        writeTime(music.modifiedAt)
        writeText(music.category?.name.orEmpty())
        writeInt(music.scale.tonic.midi)
        writeText(music.scale.type.name)
        writeInt(music.score.bpm)
        writeInt(music.score.beatsPerBar)
        writeInt(music.score.voices.size)
        music.score.voices.forEach { writeVoice(it) }
    }

    private fun BufferedSource.readVoice(): Voice {
        val waveform = Waveform.valueOf(readText())
        val volume = readDouble()
        val notes = List(readCount()) {
            val midi = readInt()
            Note(pitch = if (midi == REST) null else Pitch(midi), beats = readDouble())
        }
        return Voice(waveform, volume, notes)
    }

    private fun BufferedSink.writeVoice(voice: Voice) {
        writeText(voice.waveform.name)
        writeDouble(voice.volume)
        writeInt(voice.notes.size)
        voice.notes.forEach { note ->
            writeInt(note.pitch?.midi ?: REST)
            writeDouble(note.beats)
        }
    }

    private fun BufferedSource.readText(): String = readUtf8(readCount().toLong())

    private fun BufferedSink.writeText(text: String) {
        val bytes = text.encodeToByteArray()
        writeInt(bytes.size)
        write(bytes)
    }

    /**
     * A time, or none: [UNKNOWN_TIME] for a time unknown, or [KNOWN_TIME] then its milliseconds since 1970 in
     * Greenwich. Any other mark is what no file holds.
     */
    private fun BufferedSource.readTime(): Instant? = when (val mark = readByte().toInt()) {
        UNKNOWN_TIME -> null
        KNOWN_TIME -> Instant.ofEpochMilli(readLong())
        else -> throw IllegalArgumentException("A time is marked $UNKNOWN_TIME or $KNOWN_TIME, was $mark")
    }

    private fun BufferedSink.writeTime(time: Instant?) {
        if (time == null) {
            writeByte(UNKNOWN_TIME)
        } else {
            writeByte(KNOWN_TIME)
            writeLong(time.toEpochMilli())
        }
    }

    private fun BufferedSource.readDouble(): Double = Double.fromBits(readLong())

    private fun BufferedSink.writeDouble(value: Double) {
        writeLong(value.toRawBits())
    }

    /**
     * Refuses with an [IllegalArgumentException], before a byte is written, a [content] with a list or a text beyond
     * [MOST_ELEMENTS]: [readCount] would take the file for a corrupted one, and the whole library would be lost.
     */
    private fun requireFits(content: MusicLibraryContent) {
        val counts = sequence {
            yield(content.gameMusicId?.key.orEmpty().utf8Size())
            yield(content.musics.size)
            content.musics.forEach { music ->
                yield(music.id.key.utf8Size())
                yield(music.name.utf8Size())
                yield(music.score.voices.size)
                music.score.voices.forEach { yield(it.notes.size) }
            }
        }
        counts.forEach { count ->
            require(count <= MOST_ELEMENTS) {
                "A list or a text of the file holds $MOST_ELEMENTS at the most, was $count"
            }
        }
    }

    private fun String.utf8Size(): Int = encodeToByteArray().size

    /**
     * A count of elements or of bytes, refused if it is negative or beyond [MOST_ELEMENTS]: a corrupted count must not
     * have a list of billions of elements allocated before the end of the file is found.
     */
    private fun BufferedSource.readCount(): Int =
        readInt().also { require(it in 0..MOST_ELEMENTS) { "A count goes from 0 to $MOST_ELEMENTS, was $it" } }

    /** The most elements of a list, or bytes of a text, the file may hold: far beyond what a music has. */
    private const val MOST_ELEMENTS = 1 shl 16

    /** The MIDI number written for a rest, which no pitch has. */
    private const val REST = -1

    /** The first version that keeps the time of the last change of each music. */
    private const val DATED_SINCE = 2

    /** The mark of a time unknown: a music saved in version 1, before the format kept it. */
    private const val UNKNOWN_TIME = 0

    /** The mark of a time known, its milliseconds after it. */
    private const val KNOWN_TIME = 1
}
