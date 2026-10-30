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
 * The file of the progress of the Serrurier. It follows the backup of the phone (backup rules of `app`): it is what
 * the player earned, and padlock n is the same on any phone (ADR 0025, amendment P7.28).
 */
const val PADLOCKS_FILE: String = "padlocks.bin"

/**
 * A DataStore of the progress of the padlocks living in [scope], on the file [produceFile] gives; one instance per
 * file. A corrupted file is copied aside, to `padlocks.bin.corrupt`, then replaced by an empty progress, as the other
 * files of BloX are.
 */
internal fun padlockProgressDataStore(
    scope: CoroutineScope,
    produceFile: () -> Path,
): DataStore<PadlockProgress> = binaryDataStore(scope, PadlockProgressSerializer, produceFile) { PadlockProgress() }

/**
 * The format of the padlocks file, a format of BloX on Okio, as the gardens file: [MAGIC], its [VERSION], the
 * [GENERATION] of the padlocks its openings are of, the count N of the open padlocks, then the best opening of padlocks
 * 1 to N in turn, each one as 4 numbers: its pieces, moves, refusals and hints. A number is its 32 bits.
 */
internal object PadlockProgressSerializer : OkioSerializer<PadlockProgress> {
    /** The first four bytes of every padlocks file, « BLXS » (serrures) in ASCII: a file without them is not one. */
    private const val MAGIC = 0x424C5853

    /** The version of the format, written after [MAGIC]: a change of the format gets a number of its own. */
    private const val VERSION = 1

    /**
     * The generation of the padlocks, the contract that makes padlock n (ADR 0014, amendment P7.28): should it change
     * one day, the openings of the padlocks of an older generation stay attributed to theirs.
     */
    private const val GENERATION = 1

    override val defaultValue: PadlockProgress = PadlockProgress()

    /**
     * The content of [source]. A file cut short or holding what no progress can be (a version or a generation below 1,
     * a negative count, an opening that cannot be, bytes after the last opening) is corrupted: the DataStore replaces
     * it with an empty one. A file of a later version, or of a later generation of padlocks, which a backup of a later
     * BloX may bring back, cannot be read: it is kept for them, and the progress cannot be written until they come.
     */
    override suspend fun readFrom(source: BufferedSource): PadlockProgress = try {
        with(source) {
            if (readInt() != MAGIC) throw CorruptionException("The padlocks file is not one")
            val version = readInt()
            require(version >= 1) { "The padlocks file is of version $version, which no BloX writes" }
            if (version > VERSION) throw IOException("The padlocks file is of version $version, later than $VERSION")
            val generation = readInt()
            require(generation >= 1) { "The padlocks are of generation $generation, which no BloX makes" }
            if (generation > GENERATION) {
                throw IOException("The padlocks of the file are of generation $generation, later than $GENERATION")
            }
            val openings = readOpenings(readCount(Int.MAX_VALUE))
            require(exhausted()) { "The padlocks file goes on after its last opening" }
            PadlockProgress(openings)
        }
    } catch (cutShort: EOFException) {
        throw CorruptionException("The padlocks file is cut short", cutShort)
    } catch (invalid: IllegalArgumentException) {
        throw CorruptionException("The padlocks file holds what no progress can be", invalid)
    }

    override suspend fun writeTo(
        t: PadlockProgress,
        sink: BufferedSink,
    ) {
        with(sink) {
            writeInt(MAGIC)
            writeInt(VERSION)
            writeInt(GENERATION)
            writeInt(t.openings.size)
            t.openings.forEach { opening ->
                writeInt(opening.pieces)
                writeInt(opening.moves)
                writeInt(opening.refusals)
                writeInt(opening.hints)
            }
        }
    }

    /**
     * The [count] openings that follow, the list growing as they are read: a corrupted count of billions finds the end
     * of the file before a list of billions is allocated.
     */
    private fun BufferedSource.readOpenings(count: Int): List<PadlockOpening> = buildList {
        repeat(count) {
            add(PadlockOpening(pieces = readInt(), moves = readInt(), refusals = readInt(), hints = readInt()))
        }
    }
}
