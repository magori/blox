package ch.magori.blox.core.music

import kotlin.random.Random

/** The letter of a pair of bars in the plan `A A' B A''`. */
internal enum class Section {
    A,
    A_PRIME,
    B,
    A_DOUBLE_PRIME,
}

/** What the rhythm of a bar does in the plan of a [Form]. */
internal enum class BarRole {
    /** Takes up the rhythm of the motif. */
    MOTIF,

    /** Departs from the motif, and from the phrase end before it, on another cell of the category. */
    CONTRAST,

    /** Ends a phrase on a half cadence, breathing on a long note or a silence, on a cell other than the motif. */
    PHRASE_END,
}

/** One bar of a [Form]: its [section], its [role] in the plan, the [chords] it plays in turn, and its [rhythm]. */
internal data class Bar(val section: Section, val role: BarRole, val chords: List<Chord>, val rhythm: RhythmCell) {
    /** The chord in place at [onset] beats from the start of the bar: each chord plays an equal share of the bar. */
    fun chordAt(onset: Double): Chord = chords[(onset * chords.size / GENERATED_BEATS_PER_BAR).toInt()]
}

/**
 * The skeleton of a generated music of [category], before its pitches: its key and [scale], the [progression] it
 * plays at the pace of [harmonicRhythm], its [tempo] in beats per minute, the [motif] whose rhythm carries the music,
 * and [bars] of the plan `A A' B A''`.
 */
internal data class Form(
    val category: Category,
    val scale: Scale,
    val progression: Progression,
    val harmonicRhythm: HarmonicRhythm,
    val tempo: Int,
    val motif: RhythmCell,
    val bars: List<Bar>,
) {
    /** The chord in place under [note], struck in its bar. */
    fun chordUnder(note: StruckNote): Chord = bars[note.bar].chordAt(note.onset)

    companion object {
        /**
         * The two bars of each section: A states the motif, A' takes it up again and ends the question, B departs from
         * it then comes back to it, and A'' takes it up again and ends the answer.
         */
        private val PLAN: List<Pair<Section, BarRole>> = listOf(
            Section.A to BarRole.MOTIF,
            Section.A to BarRole.MOTIF,
            Section.A_PRIME to BarRole.MOTIF,
            Section.A_PRIME to BarRole.PHRASE_END,
            Section.B to BarRole.CONTRAST,
            Section.B to BarRole.MOTIF,
            Section.A_DOUBLE_PRIME to BarRole.MOTIF,
            Section.A_DOUBLE_PRIME to BarRole.PHRASE_END,
        )

        /** The bars of a form, counted from 0. */
        val BARS: IntRange = PLAN.indices

        /** Bars 4 and 8, counted from 0, which end each phrase on a half cadence. */
        val CADENCES: Set<Int> = BARS.filter { PLAN[it].second == BarRole.PHRASE_END }.toSet()

        /**
         * The form of a music of [choice], drawn with [random]: the same draw gives the same form. The draws come in
         * this order: the category, the harmony, the tonic, the progression, the tempo, the motif, then the rhythm of
         * each bar that does not take up the motif, from the first bar to the last. The draws of the melody and the bass
         * will come after those of the form, so that they leave it as it is. A [tonic] imposed takes the place of the
         * draw of the tonic, which it skips.
         */
        fun compose(
            choice: CategoryChoice,
            random: Random,
            tonic: Pitch? = null,
        ): Form {
            val category = choice.resolve(random)
            val harmony = category.harmonies.random(random)
            val scale = Scale(tonic ?: Category.TONICS.random(random), harmony.scaleType)
            val progression = harmony.progressions.random(random)
            val tempo = category.tempo.draw(random)
            val motif = category.rhythms.random(random)
            val halfCadence = scale.fifthTriad()
            val bars = buildList<Bar> {
                PLAN.forEachIndexed { bar, (section, role) ->
                    val chords = progression.chordsOfBar(bar, scale, harmony.harmonicRhythm)
                    val previous = lastOrNull()?.rhythm
                    val rhythm = when (role) {
                        BarRole.MOTIF -> motif
                        BarRole.CONTRAST -> (category.rhythms - setOfNotNull(motif, previous)).random(random)
                        BarRole.PHRASE_END -> (category.phraseEnds - motif).random(random)
                    }
                    val cadenced = if (role == BarRole.PHRASE_END) chords.dropLast(1) + halfCadence else chords
                    add(Bar(section, role, cadenced, rhythm))
                }
            }
            return Form(category, scale, progression, harmony.harmonicRhythm, tempo, motif, bars)
        }
    }
}
