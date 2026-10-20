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
import java.time.Instant

/** A score of one bar known by [id]: a melody of two notes and a rest, over a bass of one long note. */
internal fun aScore(
    id: ScoreId,
    bpm: Int = 120,
): Score = Score(
    id = id,
    bpm = bpm,
    beatsPerBar = 4,
    voices = listOf(
        Voice(
            Waveform.SQUARE,
            0.07,
            listOf(Note(Pitch.parse("E4"), 1.5), Note(null, 0.5), Note(Pitch.parse("G#4"), 2.0)),
        ),
        Voice(Waveform.TRIANGLE, 0.16, listOf(Note(Pitch.parse("E2"), 4.0))),
    ),
)

/**
 * A saved music called [name], known by the key [key], a joyful one in E major unless said otherwise, last changed at
 * [modifiedAt]: the time the clock of the tests tells, [MODIFIED_AT], unless said otherwise.
 */
internal fun aMusic(
    key: String,
    name: String = key,
    category: Category? = Category.JOYFUL,
    scale: Scale = Scale(Pitch.parse("E4"), ScaleType.MAJOR),
    bpm: Int = 120,
    modifiedAt: Instant? = MODIFIED_AT,
): SavedMusic {
    val id = ScoreId.Personal(key)
    return SavedMusic(
        id = id,
        name = name,
        category = category,
        scale = scale,
        score = aScore(id, bpm),
        modifiedAt = modifiedAt,
    )
}
