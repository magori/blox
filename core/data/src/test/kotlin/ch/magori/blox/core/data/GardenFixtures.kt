package ch.magori.blox.core.data

import okio.Buffer

/**
 * [ANOTHER_GARDEN] as a garden is encoded, without the header of a file: the epic mood, one piece, an O whose pivot is
 * on column 8, row 6, then its four cells from its pivot, row by row from the top. The body of `garden.bin` in
 * version 1, a format that must not change.
 */
internal val O_IN_THE_MIDDLE_EPIC: ByteArray = Buffer().text("EPIC").writeInt(1).o(column = 8, row = 6).readByteArray()

/** A text as a garden is encoded: its length in bytes, then its bytes in UTF-8. */
internal fun Buffer.text(text: String): Buffer = writeInt(text.encodeToByteArray().size).writeUtf8(text)

/**
 * A piece as a garden is encoded: its [type], its pivot on [column] and [row], then its [cells] from its pivot; an O by
 * default.
 */
internal fun Buffer.o(
    column: Int,
    row: Int,
    type: String = "O",
    cells: List<Pair<Int, Int>> = listOf(-1 to 0, 0 to 0, -1 to 1, 0 to 1),
): Buffer = text(type).writeInt(column).writeInt(row).apply {
    cells.forEach { (cellColumn, cellRow) -> writeInt(cellColumn).writeInt(cellRow) }
}
