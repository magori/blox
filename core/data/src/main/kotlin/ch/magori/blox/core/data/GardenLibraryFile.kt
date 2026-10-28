package ch.magori.blox.core.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.coroutines.CoroutineScope
import okio.BufferedSink
import okio.BufferedSource
import okio.Path
import java.io.EOFException
import java.io.IOException
import java.time.Instant

/**
 * The file of the kept gardens, « Mes jardins », apart from the garden in progress ([GARDEN_FILE]). It follows the
 * backup of the phone (backup rules of `app`): the gardens are creations of the player, as the saved musics.
 */
const val GARDENS_FILE: String = "gardens.bin"

/**
 * A DataStore of kept gardens living in [scope], on the file [produceFile] gives; one instance per file. A corrupted
 * file is copied aside, to `gardens.bin.corrupt`, then replaced by an empty list, as the other files of BloX are.
 */
internal fun gardenLibraryDataStore(
    scope: CoroutineScope,
    produceFile: () -> Path,
): DataStore<GardenLibraryContent> =
    binaryDataStore(scope, GardenLibrarySerializer, produceFile) { GardenLibraryContent() }

/**
 * The format of the gardens file, a format of BloX on Okio, as the musics file: [MAGIC], its [VERSION], the count of the
 * gardens ([GardenLibrary.MAX_GARDENS] at the most), then each garden in turn: its identity, its name, the time of its
 * last change in milliseconds since 1970 in Greenwich, then the garden as a single garden is encoded ([writeGarden]).
 * A text is its length in bytes, then its bytes in UTF-8; a number is its 32 bits, a time its 64 bits.
 */
internal object GardenLibrarySerializer : OkioSerializer<GardenLibraryContent> {
    /** The first four bytes of every gardens file, « BLXJ » (jardins) in ASCII: a file without them is not one. */
    private const val MAGIC = 0x424C584A

    /** The version of the format, written after [MAGIC]: a change of the format gets a number of its own. */
    private const val VERSION = 1

    /** The most bytes of a name: [KeptGarden.MAX_NAME_LENGTH] characters of 4 bytes each in UTF-8. */
    private const val MOST_NAME_BYTES = KeptGarden.MAX_NAME_LENGTH * 4

    override val defaultValue: GardenLibraryContent = GardenLibraryContent()

    /**
     * The content of [source]. A file cut short or holding what no library can be (more than
     * [GardenLibrary.MAX_GARDENS] gardens, two of the same identity or of the same name, a name a garden cannot have, a
     * garden that cannot be, bytes after the last garden) is corrupted: the DataStore replaces it with an empty one.
     * A file of a later version, which a backup of a later BloX may bring back, cannot be read: it is kept for that
     * version, and the gardens cannot be written until it comes.
     */
    override suspend fun readFrom(source: BufferedSource): GardenLibraryContent = try {
        with(source) {
            if (readInt() != MAGIC) throw CorruptionException("The gardens file is not one")
            val version = readInt()
            if (version < 1) throw CorruptionException("The gardens file is of version $version, which no BloX writes")
            if (version > VERSION) throw IOException("The gardens file is of version $version, later than $VERSION")
            val gardens = List(readCount(GardenLibrary.MAX_GARDENS)) { readKeptGarden() }
            require(exhausted()) { "The gardens file goes on after its last garden" }
            requireDistinct(gardens)
            GardenLibraryContent(gardens)
        }
    } catch (cutShort: EOFException) {
        throw CorruptionException("The gardens file is cut short", cutShort)
    } catch (invalid: IllegalArgumentException) {
        throw CorruptionException("The gardens file holds what no library can be", invalid)
    }

    override suspend fun writeTo(
        t: GardenLibraryContent,
        sink: BufferedSink,
    ) {
        requireFits(t)
        with(sink) {
            writeInt(MAGIC)
            writeInt(VERSION)
            writeInt(t.gardens.size)
            t.gardens.forEach { writeKeptGarden(it) }
        }
    }

    /** Refuses two [gardens] of the same identity, or of the same name, the case and the spaces around it ignored. */
    private fun requireDistinct(gardens: List<KeptGarden>) {
        require(gardens.distinctBy { it.id }.size == gardens.size) { "Two gardens of the file have the same identity" }
        val names = gardens.takenNames()
        val sameName = gardens.any { names.isTaken(it.name, apartFrom = it.id) }
        require(!sameName) { "Two gardens of the file have the same name" }
    }

    /**
     * Refuses with an [IllegalArgumentException], before a byte is written, a [content] the file could not read back:
     * more gardens than [GardenLibrary.MAX_GARDENS], an identity or a name beyond the bytes the file reads. The whole
     * library would be lost, taken for a corrupted one.
     */
    private fun requireFits(content: GardenLibraryContent) {
        require(content.gardens.size <= GardenLibrary.MAX_GARDENS) {
            "A gardens file holds ${GardenLibrary.MAX_GARDENS} gardens at the most, was ${content.gardens.size}"
        }
        content.gardens.forEach { kept ->
            require(kept.id.key.utf8Size() <= GardenId.MOST_KEY_BYTES) {
                "An identity of the file holds ${GardenId.MOST_KEY_BYTES} bytes at the most"
            }
            require(kept.name.utf8Size() <= MOST_NAME_BYTES) {
                "A name of the file holds $MOST_NAME_BYTES bytes at the most"
            }
        }
    }

    private fun BufferedSource.readKeptGarden(): KeptGarden {
        val id = GardenId(readText(GardenId.MOST_KEY_BYTES))
        val name = readText(MOST_NAME_BYTES)
        val modifiedAt = Instant.ofEpochMilli(readLong())
        return KeptGarden(id, name, readGarden(), modifiedAt)
    }

    private fun BufferedSink.writeKeptGarden(kept: KeptGarden) {
        writeText(kept.id.key)
        writeText(kept.name)
        writeLong(kept.modifiedAt.toEpochMilli())
        writeGarden(kept.garden)
    }
}
