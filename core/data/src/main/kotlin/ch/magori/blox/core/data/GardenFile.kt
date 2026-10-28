package ch.magori.blox.core.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.garden.GardenPlacement
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
 * The format of the garden file, a format of BloX on Okio, as the musics file: [MAGIC], its [VERSION], the name of the
 * mood, the count of the pieces, then each piece in turn: the name of its type, the column then the row of its pivot,
 * then its [CELLS_PER_PIECE] cells as offsets from its pivot (column, row), row by row from the top, then from the left.
 * The enums are written by their names, which must not change; a text is its length in bytes, then its bytes in UTF-8;
 * a number is its 32 bits. The cells are written whole, not as the number of an orientation: the file does not depend
 * on the order in which the rotation system lists them.
 */
internal object GardenSerializer : OkioSerializer<Garden> {
    /** The first four bytes of every garden file, « BLXG » in ASCII: a file without them is not one. */
    private const val MAGIC = 0x424C5847

    /** The version of the format, written after [MAGIC]: a change of the format gets a number of its own. */
    private const val VERSION = 1

    /** The cells of a piece. */
    private const val CELLS_PER_PIECE = 4

    /** The most pieces a garden holds, every cell of its grid covered. */
    private const val MOST_PIECES = Garden.COLUMNS * Garden.ROWS / CELLS_PER_PIECE

    /** The most bytes of a name of the file, far beyond the longest name of a mood or of a type. */
    private const val MOST_NAME_BYTES = 64

    /** Row by row from the top, then from the left: the order the cells of a piece are written in. */
    private val READING_ORDER: Comparator<Position> = compareBy(Position::row, Position::column)

    override val defaultValue: Garden = Garden.EMPTY

    /**
     * The garden of [source], each piece placed in turn as the player would. A file cut short or holding what no garden
     * can be (an unknown mood or type, a piece off the grid, over another one, or of a shape not of its type, bytes
     * after the last piece) is
     * corrupted: the DataStore replaces it with the empty garden. A file of a later version, which a backup of a later
     * BloX may bring back, cannot be read: it is kept for that version, and the garden cannot be written until it comes.
     */
    override suspend fun readFrom(source: BufferedSource): Garden = try {
        with(source) {
            if (readInt() != MAGIC) throw CorruptionException("The garden file is not one")
            val version = readInt()
            if (version < 1) throw CorruptionException("The garden file is of version $version, which no BloX writes")
            if (version > VERSION) throw IOException("The garden file is of version $version, later than $VERSION")
            val mood = GardenMood.valueOf(readText(MOST_NAME_BYTES))
            List(readCount(MOST_PIECES)) { readPiece() }.fold(Garden.EMPTY.withMood(mood), ::placing)
                .also { require(exhausted()) { "The garden file goes on after its last piece" } }
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
            writeText(t.mood.name)
            writeInt(t.pieces.size)
            t.pieces.forEach { writePiece(it) }
        }
    }

    /** [garden] with [piece] placed; an [IllegalArgumentException] if the garden refuses it. */
    private fun placing(
        garden: Garden,
        piece: Piece,
    ): Garden = when (val placement = garden.place(piece)) {
        is GardenPlacement.Placed -> placement.garden
        is GardenPlacement.Refused -> throw IllegalArgumentException("A piece of the file is ${placement.reason}")
    }

    private fun BufferedSource.readPiece(): Piece {
        val type = PieceType.valueOf(readText(MOST_NAME_BYTES))
        val pivot = readPosition()
        val orientation = Orientation(List(CELLS_PER_PIECE) { readPosition() }.toSet())
        return Piece(type, orientation, pivot)
    }

    private fun BufferedSink.writePiece(piece: Piece) {
        writeText(piece.type.name)
        writePosition(piece.pivot)
        piece.orientation.cells.sortedWith(READING_ORDER).forEach { writePosition(it) }
    }

    private fun BufferedSource.readPosition(): Position = Position(readInt(), readInt())

    private fun BufferedSink.writePosition(position: Position) {
        writeInt(position.column)
        writeInt(position.row)
    }
}
