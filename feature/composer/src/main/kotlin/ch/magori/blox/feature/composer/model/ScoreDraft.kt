package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A music being written in the editor: its [scale], which gives the lines of the grids, its [category], `null` for a
 * music written by hand, its [tempo], and the grids of its [melody] and [bass].
 */
internal data class ScoreDraft(
    val scale: Scale,
    val category: Category?,
    val tempo: Tempo,
    val melody: TrackGrid,
    val bass: TrackGrid,
) {
    /** Whether neither grid has a note. */
    val isEmpty: Boolean get() = melody.notes.isEmpty() && bass.notes.isEmpty()

    /** The grid of [track]. */
    operator fun get(track: Track): TrackGrid = when (track) {
        Track.MELODY -> melody
        Track.BASS -> bass
    }

    /** The draft with [grid] as the grid of [track]. */
    fun with(
        track: Track,
        grid: TrackGrid,
    ): ScoreDraft = when (track) {
        Track.MELODY -> copy(melody = grid)
        Track.BASS -> copy(bass = grid)
    }

    /**
     * The score that plays this draft, known by [id]: eight bars of four beats, a voice per track, its silences written
     * as [TrackGrid.toNotes] writes them.
     */
    fun toScore(id: ScoreId): Score = Score(
        id = id,
        bpm = tempo.bpm,
        beatsPerBar = BEATS_PER_BAR,
        voices = Track.entries.map { track -> track.voice(get(track).toNotes()) },
    )

    companion object {
        private const val BEATS_PER_BAR = 4

        /** A new music, empty, on [scale]: C major unless a category gives another one. */
        fun new(
            scale: Scale = Scale(Pitch.parse("C4"), ScaleType.MAJOR),
            category: Category? = null,
            tempo: Tempo = Tempo(120),
        ): ScoreDraft = ScoreDraft(
            scale = scale,
            category = category,
            tempo = tempo,
            melody = TrackGrid.empty(Track.MELODY, scale),
            bass = TrackGrid.empty(Track.BASS, scale),
        )

        /**
         * The draft that edits [score], written on [scale] in [category]: a score knows neither, the one who opens it
         * does (the generator, or the saved music). Its first voice is the melody, its second the bass.
         */
        fun fromScore(
            score: Score,
            scale: Scale,
            category: Category?,
        ): ScoreDraft? {
            if (!fitsTheGrid(score)) return null
            val empty = new(scale, category, Tempo(score.bpm))
            return empty.copy(
                melody = TrackGrid.playing(Track.MELODY, scale, score.voices[0].notes),
                bass = TrackGrid.playing(Track.BASS, scale, score.voices.getOrNull(1)?.notes.orEmpty()),
            )
        }

        /**
         * Whether [score] has a voice per track at most, a tempo of the editor, and lasts eight bars of four beats, every
         * note a whole number of sixteenth notes.
         */
        private fun fitsTheGrid(score: Score): Boolean = score.voices.size <= Track.entries.size &&
            score.bpm in Tempo.SLOWEST..Tempo.FASTEST &&
            score.beatsPerBar == BEATS_PER_BAR &&
            score.voices.first().beats == BARS * BEATS_PER_BAR.toDouble() &&
            score.voices.all { voice -> voice.notes.all { isWholeSixteenths(it.beats) } }

        private fun isWholeSixteenths(beats: Double): Boolean {
            val sixteenths = beats / BEATS_PER_SIXTEENTH
            return abs(sixteenths - sixteenths.roundToInt()) < TOLERANCE
        }

        /** Sixteenth notes a duration may be off the grid by: the rounding error of a duration in floating point. */
        private const val TOLERANCE = 1e-6
    }
}
