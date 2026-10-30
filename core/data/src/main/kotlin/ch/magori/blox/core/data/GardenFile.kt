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

/**
 * The file of the garden kept, apart from the settings and the musics. It follows the backup of the phone (backup rules
 * of `app`): the garden is a creation of the player, as the saved musics.
 */
const val GARDEN_FILE: String = "garden.bin"

/**
 * A DataStore of the garden kept living in [scope], on the file [produceFile] gives; one instance per file. A corrupted
 * file is copied aside, to `garden.bin.corrupt`, then replaced by the empty garden, calm: a bug of a later version of the
 * garden must not lose the garden of the player without a trace.
 */
internal fun gardenDataStore(
    scope: CoroutineScope,
    produceFile: () -> Path,
): DataStore<CurrentGarden> = binaryDataStore(scope, GardenSerializer, produceFile) { CurrentGarden.EMPTY }

/**
 * The format of the garden file, a format of BloX on Okio, as the musics file: [MAGIC], its [VERSION], then, since
 * version 2, the key of the kept garden the garden comes from as a text, empty if it comes from none; then the garden
 * as a single garden is encoded ([writeGarden]): the name of its mood, the count of its pieces, then each piece.
 *
 * The key comes before the garden, right after the version, as a header: the garden is the end of the file, which
 * tells where it ends, in every version. An empty text for « none » follows the game music of the musics file, and
 * needs no flag: a key is never blank ([GardenId]).
 */
internal object GardenSerializer : OkioSerializer<CurrentGarden> {
    /** The first four bytes of every garden file, « BLXG » in ASCII: a file without them is not one. */
    private const val MAGIC = 0x424C5847

    /**
     * The version of the format, written after [MAGIC]: a change of the format gets a number of its own. Version 1
     * holds the garden alone, read as coming from no kept garden; version 2 adds the kept garden it comes from.
     */
    private const val VERSION = 2

    /** The first version holding the kept garden the garden comes from. */
    private const val VERSION_WITH_KEPT_FROM = 2

    override val defaultValue: CurrentGarden = CurrentGarden.EMPTY

    /**
     * The garden of [source] ([readGarden]) and the kept garden it comes from. A file cut short or holding what no
     * garden can be (an unknown mood or type, a piece off the grid, over another one, or of a shape not of its type, a
     * blank key, bytes after the last piece) is corrupted: the DataStore replaces it with the empty garden. A file of a
     * later version, which a backup of a later BloX may bring back, cannot be read: it is kept for that version, and the
     * garden cannot be written until it comes.
     */
    override suspend fun readFrom(source: BufferedSource): CurrentGarden = try {
        with(source) {
            if (readInt() != MAGIC) throw CorruptionException("The garden file is not one")
            val version = readInt()
            if (version < 1) throw CorruptionException("The garden file is of version $version, which no BloX writes")
            if (version > VERSION) throw IOException("The garden file is of version $version, later than $VERSION")
            val keptFrom = if (version >= VERSION_WITH_KEPT_FROM) readKeptFrom() else null
            val garden = readGarden().also { require(exhausted()) { "The garden file goes on after its last piece" } }
            CurrentGarden(garden, keptFrom)
        }
    } catch (cutShort: EOFException) {
        throw CorruptionException("The garden file is cut short", cutShort)
    } catch (invalid: IllegalArgumentException) {
        throw CorruptionException("The garden file holds what no garden can be", invalid)
    }

    /**
     * Writes [t]; refuses with an [IllegalArgumentException], before a byte is written, a key of a kept garden the file
     * could not read back ([GardenId.MOST_KEY_BYTES]): the garden would be lost, taken for a corrupted one.
     */
    override suspend fun writeTo(
        t: CurrentGarden,
        sink: BufferedSink,
    ) {
        val key = t.keptFrom?.key.orEmpty()
        require(key.utf8Size() <= GardenId.MOST_KEY_BYTES) {
            "A key of a kept garden holds ${GardenId.MOST_KEY_BYTES} bytes at the most in the garden file"
        }
        with(sink) {
            writeInt(MAGIC)
            writeInt(VERSION)
            writeText(key)
            writeGarden(t.garden)
        }
    }

    /** The kept garden the garden comes from, null if its key is empty. */
    private fun BufferedSource.readKeptFrom(): GardenId? =
        readText(GardenId.MOST_KEY_BYTES).takeIf { it.isNotEmpty() }?.let(::GardenId)
}
