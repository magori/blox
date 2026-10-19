package ch.magori.blox.core.music

import kotlin.math.abs
import kotlin.random.Random

/**
 * The bass voice of a generated music: the notes of each of its [bars], which play the chords of a [Form] in the motif of
 * its category. The root of each chord opens it; every note is in the chord in place, but a passing note on the last
 * weak beat before a new chord, consonant with the melody, reached by a fourth at most and left by step. The bass keeps
 * within its range and under the melody, never leaps past an octave, and never moves in parallel fifths or octaves with
 * the melody, as [parallelsBetween] finds them, from bar 8 back to bar 1 too as the music loops.
 */
internal data class Bass(val bars: List<List<Note>>) {
    /** The notes struck, in the order they are played. */
    val struck: List<StruckNote> get() = struckIn(bars)

    companion object {
        /** The range of the bass voice, two octaves, so that every root may be played in two octaves. */
        val RANGE: PitchRange = BASS_RANGE

        /** The steps a search for the bass may take before giving up on the form and melody. */
        const val SEARCH_BUDGET: Int = 10_000

        /** How likely the bass tries a passing note on the last weak beat before a new chord. */
        const val PASSING_CHANCE: Double = 0.5

        /**
         * The bass of [form], under [melody], drawn with [random] after the melody, or `null` if no bass fits them
         * within [searchBudget] steps, shared out between the roots the first note may take. The draws come in this order: whether the bass tries a passing note, with
         * [passingChance], on each last weak beat before a new chord, from the first bar to the last; the search draws
         * nothing more.
         */
        fun compose(
            form: Form,
            melody: Melody,
            random: Random,
            passingChance: Double = PASSING_CHANCE,
            searchBudget: Int = SEARCH_BUDGET,
        ): Bass? = BassComposer(form, melody, random, passingChance, searchBudget).compose()
    }
}

/**
 * A bass note to strike in [bar], counted from 0, at [onset] beats, for [beats] beats, under [chord], the [index]-th
 * note of that chord, under [above], the notes of the melody sounding with it. A slot that [passes] tries first
 * a passing note, a note of the scale that leads by step to the root of [next], the chord after it.
 */
private data class BassSlot(
    val bar: Int,
    val onset: Double,
    val beats: Double,
    val chord: Chord,
    val index: Int,
    val above: List<Pitch>,
    val next: Chord? = null,
    val passes: Boolean = false,
) {
    val time: Double get() = timeOf(bar, onset)
}

/** Fills the chords of [form] with bass notes, note after note, going back on a choice that leads nowhere. */
private class BassComposer(
    private val form: Form,
    melody: Melody,
    random: Random,
    passingChance: Double,
    private val searchBudget: Int,
) {
    private val style = form.category.bass

    private val melody: List<StruckNote> = melody.struck

    private val slots: List<BassSlot> = run {
        val sounding = eachStruckIn(melody.bars) { bar, onset, note, pitch ->
            Sounding(bar, onset, onset + note.beats, pitch)
        }
        val plain = form.bars.flatMapIndexed { bar, it ->
            val span = GENERATED_BEATS_PER_BAR.toDouble() / it.chords.size
            it.chords.flatMapIndexed { at, chord ->
                val end = (at + 1) * span
                generateSequence(at * span) { onset -> onset + style.beats }.takeWhile { onset -> onset < end }
                    .mapIndexed { index, onset ->
                        val beats = minOf(style.beats, end - onset)
                        val above = sounding.filter { note -> note.bar == bar && note.overlaps(onset, onset + beats) }
                        BassSlot(bar, onset, beats, chord, index, above.map { it.pitch })
                    }.toList()
            }
        }
        plain.mapIndexed { at, slot -> drawPassing(slot, plain[(at + 1) % plain.size], random, passingChance) }
    }

    /**
     * [slot], drawn with [passingChance] to try a passing note if it is the last weak beat of its chord, before [next],
     * which opens a new chord.
     */
    private fun drawPassing(
        slot: BassSlot,
        next: BassSlot,
        random: Random,
        passingChance: Double,
    ): BassSlot {
        val beforeChange = next.index == 0 && next.chord != slot.chord
        if (slot.index == 0 || !beforeChange || slot.onset in RhythmCell.STRONG_BEATS) return slot
        return slot.copy(next = next.chord, passes = random.nextDouble() < passingChance)
    }

    /** The slots of the first two bars, whose pitches the verticals of the end of the music move to as it loops. */
    private val opening: Int = 2

    /** The steps the search may still take for the root of the first note being tried. */
    private var steps = 0

    /**
     * The states from which no bass follows by the rules, already searched: whether the next notes fit depends on
     * these pitches alone, so that the search never goes through the same dead end twice.
     */
    private val deadEnds = mutableSetOf<DeadEnd>()

    /**
     * Slot [index] to fill after [previous], the last pitch placed, with [waiting], the perfect fifths and octaves so far
     * where the voices have not both moved since, which the next notes may still move in parallel from, and the pitches of the
     * [opening] slots, which the last notes lead back to as the music loops.
     */
    private data class DeadEnd(
        val index: Int,
        val previous: Pitch?,
        val waiting: Set<Pair<Pitch, Pitch>>,
        val opening: List<Pitch>,
    )

    /**
     * The bass, or `null` if none fits. Each root the first note may take gets an equal share of the steps: a dead end
     * that only the loop back to the first note shows, on the last slot, would otherwise use them all up on the first
     * root tried.
     */
    fun compose(): Bass? {
        val openings = candidates(0, emptyList()).filter { allows(0, it, emptyList()) }
        val pitches = openings.firstNotNullOfOrNull { opening ->
            steps = searchBudget / openings.size
            place(listOf(opening))
        }?.iterator() ?: return null
        return Bass(
            form.bars.indices.map { bar ->
                slots.filter { it.bar == bar }.map { Note(pitches.next(), it.beats) }
            },
        )
    }

    /** The pitches of all the slots, starting with [placed], or `null` if none follows them by the rules. */
    private fun place(placed: List<Pitch>): List<Pitch>? {
        if (placed.size == slots.size) return placed
        if (--steps < 0) return null
        val index = placed.size
        val verticals = verticalsOf(melody, struck(placed), slots[index].time)
        val waiting = verticals.filterIndexed { at, vertical ->
            vertical.isPerfect && verticals.drop(at + 1).none(vertical::bothMovedIn)
        }
        val state =
            DeadEnd(index, placed.lastOrNull(), waiting.map { it.upper to it.lower }.toSet(), placed.take(opening))
        if (state in deadEnds) return null
        return candidates(index, placed).filter {
            allows(index, it, placed)
        }.firstNotNullOfOrNull { place(placed + it) }
            ?: run {
                // A search out of steps marks a state it has not been through as a dead end too: no harm, as every
                // later call gives up at once, until the next root of the first note gets its share of steps, and the
                // dead ends remember that root.
                deadEnds += state
                null
            }
    }

    /** The notes struck by the bass on the first slots, one for each pitch [placed]. */
    private fun struck(placed: List<Pitch>): List<StruckNote> =
        placed.mapIndexed { index, pitch -> StruckNote(slots[index].bar, slots[index].onset, pitch) }

    /**
     * The pitches slot [index] may take after [placed], the best first: a passing note if it [BassSlot.passes], then the
     * note of the motif, then the same note in another octave, then, after the root that opens each chord, the other
     * notes of the chord. The root after a passing note tries first the octave a step from it.
     */
    private fun candidates(
        index: Int,
        placed: List<Pitch>,
    ): List<Pitch> {
        val slot = slots[index]
        val motif = motifNote(index, placed)
        val previous = placed.lastOrNull()
        if (slot.index == 0) {
            val roots = inRange(motif).sortedBy { abs(it.midi - (previous ?: motif).midi) }
            val afterPassing = previous != null && slots[index - 1].passes
            val stepsFirst = if (afterPassing) {
                roots.sortedBy {
                    !form.scale.isStep(checkNotNull(previous), it)
                }
            } else {
                roots
            }
            return stepsFirst.sortedBy { leapsByTritone(previous, it) }
        }
        val same = inRange(motif).sortedBy { abs(it.midi - motif.midi) }
        val others = slot.chord.pitches.flatMap(::inRange).filterNot { it.semitonesAboveInOctave(motif) == 0 }
        val passing = if (slot.passes) passingNotes(index, placed) else emptyList()
        return (passing + same + others.sortedBy { abs(it.midi - motif.midi) }).distinct()
            .sortedBy { leapsByTritone(previous, it) }
    }

    /** Whether the bass would leap by a tritone from [previous] to [candidate], which it tries last. */
    private fun leapsByTritone(
        previous: Pitch?,
        candidate: Pitch,
    ): Boolean = previous != null && abs(candidate.midi - previous.midi) == TRITONE

    /**
     * The note the motif of the [style] plays on slot [index] after [placed]: the root of the chord, the lowest one
     * before any note is placed; then, from the root placed where the chord opens, that root again, or its fifth or its
     * octave above it, below it when above goes past the range.
     */
    private fun motifNote(
        index: Int,
        placed: List<Pitch>,
    ): Pitch {
        val slot = slots[index]
        val root = placed.getOrNull(index - slot.index) ?: inRange(slot.chord.root).first()
        val interval = when (style.motif) {
            BassMotif.REPEATED_ROOT -> return root
            BassMotif.ROOT_AND_FIFTH -> slot.chord.quality.offsets.last()
            BassMotif.OCTAVES -> SEMITONES_PER_OCTAVE
        }
        if (slot.index % 2 == 0) return root
        val above = root.midi + interval
        if (above <= Bass.RANGE.highest.midi) return Pitch(above)
        return Pitch(root.midi - (SEMITONES_PER_OCTAVE - interval % SEMITONES_PER_OCTAVE))
    }

    /**
     * The notes of the scale, in the chord or not, that slot [index] may pass through to lead by step to the root of the
     * next chord: any root in the range, or the first note placed, which the last slot leads back to as the music loops.
     * The nearest to the note before come first.
     */
    private fun passingNotes(
        index: Int,
        placed: List<Pitch>,
    ): List<Pitch> {
        val roots = if (index == slots.lastIndex) placed.take(1) else inRange(checkNotNull(slots[index].next).root)
        val previous = placed.last()
        return (Bass.RANGE.lowest.midi..Bass.RANGE.highest.midi).map(::Pitch)
            .filter { pitch -> pitch in form.scale && roots.any { form.scale.isStep(pitch, it) } }
            .sortedBy { abs(it.midi - previous.midi) }
    }

    /** The pitches of the range with the note of [pitch], in any octave, from low to high. */
    private fun inRange(pitch: Pitch): List<Pitch> {
        val range = Bass.RANGE
        val lowest = range.lowest.midi + pitch.semitonesAboveInOctave(range.lowest)
        return (lowest..range.highest.midi step SEMITONES_PER_OCTAVE).map(::Pitch)
    }

    /**
     * Whether [candidate] may be struck on slot [index] after [placed]: under the melody, a passing note going on by step
     * to the root of the next chord, and no parallel fifths or octaves with the melody, as [parallelsBetween] finds
     * them: among the verticals up to the next slot, then, on the last slot, over the whole music as it loops.
     */
    private fun allows(
        index: Int,
        candidate: Pitch,
        placed: List<Pitch>,
    ): Boolean {
        val slot = slots[index]
        if (slot.above.any { it.midi <= candidate.midi }) return false
        if (candidate !in slot.chord && !passesWell(candidate, slot, placed.last())) return false
        if (!resolves(index, candidate, placed) || !leapsWithinOctave(index, candidate, placed)) return false
        val bass = struck(placed + candidate)
        if (index == slots.lastIndex) return parallelsBetween(melody, bass).isEmpty()
        return parallelsAmong(verticalsOf(melody, bass, slots[index + 1].time), loops = false).isEmpty()
    }

    /**
     * Whether [candidate] on slot [index] goes on by step from a passing note before it, and, a passing note on the last
     * slot, leads by step to the first note placed, as the music loops.
     */
    private fun resolves(
        index: Int,
        candidate: Pitch,
        placed: List<Pitch>,
    ): Boolean {
        val previous = placed.lastOrNull()
        if (previous != null && previous !in slots[index - 1].chord && !form.scale.isStep(previous, candidate)) {
            return false
        }
        return index != slots.lastIndex || candidate in slots[index].chord ||
            form.scale.isStep(candidate, placed.first())
    }

    /**
     * Whether [candidate] on slot [index] lies an octave at most from the note before it, and, on the last slot, from the
     * first note, as the music loops.
     */
    private fun leapsWithinOctave(
        index: Int,
        candidate: Pitch,
        placed: List<Pitch>,
    ): Boolean {
        val neighbours = listOfNotNull(placed.lastOrNull(), placed.firstOrNull()?.takeIf { index == slots.lastIndex })
        return neighbours.all { abs(candidate.midi - it.midi) <= SEMITONES_PER_OCTAVE }
    }

    /**
     * Whether [candidate], out of the chord of [slot], passes well after [previous]: a fourth at most from it, and no
     * second, seventh or tritone with the notes of the melody sounding with it.
     */
    private fun passesWell(
        candidate: Pitch,
        slot: BassSlot,
        previous: Pitch,
    ): Boolean = abs(candidate.midi - previous.midi) <= PERFECT_FOURTH &&
        slot.above.none { it.semitonesAboveInOctave(candidate) in DISSONANCES }

    private companion object {
        /** The bars at the start of the music, counted from 0, that the dead ends remember the bass of. */
        const val OPENING_BARS = 2

        /** The seconds, sevenths and tritone, octaves left out, that a passing note never makes with the melody. */
        val DISSONANCES = setOf(1, 2, TRITONE, 10, 11)
    }
}

/** A note of [pitch] of the melody sounding in [bar], counted from 0, from [onset] to [end] beats. */
private data class Sounding(val bar: Int, val onset: Double, val end: Double, val pitch: Pitch) {
    /** Whether the note sounds at some time between [from] and [until]. */
    fun overlaps(
        from: Double,
        until: Double,
    ): Boolean = onset < until && from < end
}
