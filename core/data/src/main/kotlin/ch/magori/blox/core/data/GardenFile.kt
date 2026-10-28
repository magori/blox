package ch.magori.blox.core.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import ch.magori.blox.core.garden.Garden
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
): DataStore<Garden> = binaryDataStore(scope, GardenSerializer, produceFile) { Garden.EMPTY }

/**
 * The format of the garden file, a format of BloX on Okio, as the musics file: [MAGIC], its [VERSION], then the garden
 * as a single garden is encoded ([writeGarden]): the name of its mood, the count of its pieces, then each piece.
 */
internal object GardenSerializer : OkioSerializer<Garden> {
    /** The first four bytes of every garden file, « BLXG » in ASCII: a file without them is not one. */
    private const val MAGIC = 0x424C5847

    /** The version of the format, written after [MAGIC]: a change of the format gets a number of its own. */
    private const val VERSION = 1

    override val defaultValue: Garden = Garden.EMPTY

    /**
     * The garden of [source] ([readGarden]). A file cut short or holding what no garden can be (an unknown mood or
     * type, a piece off the grid, over another one, or of a shape not of its type, bytes after the last piece) is
     * corrupted: the DataStore replaces it with the empty garden. A file of a later version, which a backup of a later
     * BloX may bring back, cannot be read: it is kept for that version, and the garden cannot be written until it comes.
     */
    override suspend fun readFrom(source: BufferedSource): Garden = try {
        with(source) {
            if (readInt() != MAGIC) throw CorruptionException("The garden file is not one")
            val version = readInt()
            if (version < 1) throw CorruptionException("The garden file is of version $version, which no BloX writes")
            if (version > VERSION) throw IOException("The garden file is of version $version, later than $VERSION")
            readGarden().also { require(exhausted()) { "The garden file goes on after its last piece" } }
        }
    } catch (cutShort: EOFException) {
        throw CorruptionException("The garden file is cut short", cutShort)
    } catch (invalid: IllegalArgumentException) {
        throw CorruptionException("The garden file holds what no garden can be", invalid)
    }

    override suspend fun writeTo(
        t: Garden,
        sink: BufferedSink,
    ) {
        with(sink) {
            writeInt(MAGIC)
            writeInt(VERSION)
            writeGarden(t)
        }
    }
}
