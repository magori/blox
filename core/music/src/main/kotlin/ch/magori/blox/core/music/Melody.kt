package ch.magori.blox.core.music

import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

/**
 * The melody voice of a generated music: the notes and rests of each of its [bars], which fill the rhythm of a [Form].
 * Every note is in the scale, but a chromatic passing note where the category allows it, and none stands a semitone
 * from a note a chord borrows from out of the scale. A note of the chord in place is struck on beats 1 and 3 and ends
 * each phrase. No leap goes past an octave or by a tritone, a leap of a fourth or more turns back by step, and no note
 * is struck more than three times in a row, from bar 8 back to bar 1 too as the music loops. The melody spans a major
 * tenth at most, within the range of its voice, and no bar copies the one before it.
 */
internal data class Melody(val bars: List<List<Note>>) {
    /** The notes struck, rests left out, in the order they are played. */
    val struck: List<StruckNote> get() = struckIn(bars)

    companion object {
        /** The range of the melody voice. */
        val RANGE: PitchRange = PitchRange(Pitch.parse("C4"), Pitch.parse("C6"))

        /** The steps a search for an arched melody may take before giving up for a melody without an arch. */
        const val ARCHED_SEARCH_BUDGET: Int = 5_000

        /** The steps a search for a melody without an arch may take before refusing the form, which no melody fits. */
        const val SEARCH_BUDGET: Int = 50_000

        /**
         * The melody of [form], leaning as [traits] say, within [range], drawn with [random] after the form: the same
         * form and the same draw give the same melody. The melody arches up to its highest note, struck once on a strong
         * beat of bar 5; a search that gives up past [archedSearchBudget] steps leaves a melody without the arch, and a
         * form that no melody fits within [SEARCH_BUDGET] more steps is refused with an `IllegalStateException`.
         *
         * The draws come in this order: the order in which the strong beats of bar 5 are tried for the peak; then, for
         * each search, the notes of the bars that do not take up the motif, note after note as it goes. A search starts
         * again from scratch after a thousand steps, so that the draws of the searches given up, with an arch or not,
         * come before those of the search that succeeds. Last come the draws of the chromatic passing notes.
         */
        fun compose(
            form: Form,
            random: Random,
            traits: MelodyTraits = form.category.melody,
            range: PitchRange = RANGE,
            archedSearchBudget: Int = ARCHED_SEARCH_BUDGET,
        ): Melody = MelodyComposer(form, random, traits, range, archedSearchBudget).compose()
    }
}

/** A note to strike in [bar], counted from 0, at [onset] beats from its start, under [chord]. */
private data class Slot(val bar: Int, val onset: Double, val chord: Chord) {
    val isOnStrongBeat: Boolean get() = onset in RhythmCell.STRONG_BEATS
}

/** The highest note of an arched melody, [pitch], struck once on slot [slot]. */
private data class Peak(val slot: Int, val pitch: Pitch) {
    /** Whether [candidate] may be struck on slot [index]: the peak on its slot, and only notes under it elsewhere. */
    fun keepsUnder(
        index: Int,
        candidate: Pitch,
    ): Boolean = if (index == slot) candidate == pitch else candidate.midi < pitch.midi
}

/**
 * Fills the rhythm of [form] with pitches, note after note, going back on a choice that leads nowhere. The bars that
 * take up the motif try first the notes that keep its contour; the others draw their notes with [random], by the
 * weights of the moves and of the arch, leaning as [traits] say.
 */
private class MelodyComposer(
    private val form: Form,
    private val random: Random,
    private val traits: MelodyTraits,
    private val range: PitchRange,
    private val archedSearchBudget: Int,
) {
    /**
     * The notes of the scale the melody plays: a major tenth centred on the middle of the [range], cut to the range when
     * it is narrower. The window does not follow the tonic: every key finds its tonic in a tenth.
     */
    private val window: List<Pitch> = run {
        val middle = (range.lowest.midi + range.highest.midi) / 2
        val lowest = maxOf(range.lowest.midi, middle - MAJOR_TENTH / 2)
        val highest = minOf(range.highest.midi, middle + MAJOR_TENTH / 2)
        (lowest..highest).map(::Pitch).filter { it in form.scale }
    }

    private val slots: List<Slot> = form.bars.flatMapIndexed { index, bar ->
        val onsets = bar.rhythm.steps.runningFold(0.0) { onset, step -> onset + step.beats }
        bar.rhythm.steps.zip(onsets).filterNot { (step, _) -> step.isRest }.map { (_, onset) ->
            Slot(index, onset, bar.chordAt(onset))
        }
    }

    /** The slots of each bar, as indices in [slots]. */
    private val slotsOfBar: List<IntRange> = form.bars.indices.map { bar ->
        slots.indexOfFirst { it.bar == bar }..slots.indexOfLast { it.bar == bar }
    }

    /**
     * The places the highest note of the melody may take, in the order drawn: each strong beat struck in bar 5, the
     * contrast, two thirds into the music, with the top note of its chord in the window. Bar 6 takes up the motif, which
     * the peak would bend.
     */
    private val peaks: List<Peak> = slotsOfBar[PEAK_BAR].filter {
        slots[it].isOnStrongBeat
    }.shuffled(random).map { slot ->
        Peak(slot, window.last { it in slots[slot].chord })
    }

    fun compose(): Melody {
        val arched = peaks.asSequence().flatMap { attempts(it, archedSearchBudget / peaks.size) }
        val searches = arched + attempts(peak = null, Melody.SEARCH_BUDGET)
        val pitches = (searches.firstNotNullOfOrNull { it.run() } ?: error("No melody fits $form")).iterator()
        return Melody(
            form.bars.map { bar ->
                bar.rhythm.steps.map { step -> Note(if (step.isRest) null else pitches.next(), step.beats) }
            },
        )
    }

    /**
     * Searches under [peak], or without an arch if it is `null`, that share [budget] steps, each starting again from
     * scratch, with new draws, after [RESTART_STEPS]: a search stuck on a choice made early gives way to another rather
     * than going through every later choice.
     */
    private fun attempts(
        peak: Peak?,
        budget: Int,
    ): Sequence<Search> = (0 until budget step RESTART_STEPS).asSequence().map { spent ->
        Search(peak, minOf(RESTART_STEPS, budget - spent))
    }

    /** A search for the pitches of all the slots, arched up to [peak] unless it is `null`, giving up past [budget] steps. */
    private inner class Search(private val peak: Peak?, private val budget: Int) {
        private var steps = 0

/** The note the arch rises to, or the top of the first peak drawn for a melody without an arch. */
        private val top: Pitch = (
            peak
                ?: peaks.first()
            ).pitch

        fun run(): List<Pitch>? = place(emptyList())?.let(::passChromatically)

        /** The pitches of all the slots, starting with [placed], or `null` if none follows them by the rules. */
        private fun place(placed: List<Pitch>): List<Pitch>? {
            if (placed.size == slots.size) return placed
            if (++steps > budget) return null
            val index = placed.size
            val candidates = window.filter { allows(slots[index], it, placed) }
            val ordered = when (val target = motifTarget(index, placed)) {
                null -> drawn(candidates) { freeWeight(slots[index], it, placed.lastOrNull(), top) }
                else -> motifOrder(index, candidates, target, placed).asSequence()
            }
            return ordered.firstNotNullOfOrNull { place(placed + it) }
        }

        private fun allows(
            slot: Slot,
            candidate: Pitch,
            placed: List<Pitch>,
        ): Boolean {
            if (peak != null && !peak.keepsUnder(placed.size, candidate)) return false
            if ((slot.isOnStrongBeat || endsPhrase(placed.size)) && candidate !in slot.chord) return false
            if (relatesFalsely(slot, candidate)) return false
            if (closesBar(slot.bar, placed.size) && copiesBarBefore(slot.bar, placed + candidate)) return false
            if (!movesWell(placed, candidate)) return false
            val count = placed.size + 1
            if (count < LOOPED_NOTES && !canBeReachedBack(placed + candidate)) return false
            if (count < slots.size) return true
            val loop = placed + candidate
            return (0 until LOOPED_NOTES).all { movesWell(loop + loop.take(it), loop[it]) }
        }

        /**
         * Whether a last note of the melody, a note of the chord that ends it, can still lead back to the first notes of
         * [opening] as the music loops, by the rules on moves.
         */
        private fun canBeReachedBack(opening: List<Pitch>): Boolean =
            window.filter { it in slots.last().chord }.any { last ->
                opening.indices.all { movesWell(listOf(last) + opening.take(it), opening[it]) }
            }

        /**
         * Whether [candidate] moves well after [placed]: no leap past an octave or by a tritone, no fourth note alike in a
         * row, and a step back the other way after a leap of a fourth or more.
         */
        private fun movesWell(
            placed: List<Pitch>,
            candidate: Pitch,
        ): Boolean {
            val previous = placed.lastOrNull() ?: return true
            val leap = abs(candidate.midi - previous.midi)
            if (leap > SEMITONES_PER_OCTAVE || leap == TRITONE) return false
            val repeats = placed.takeLast(MOST_REPEATED_NOTES)
            if (repeats.size == MOST_REPEATED_NOTES && repeats.all { it == candidate }) return false
            val beforeLeap = placed.getOrNull(placed.size - 2) ?: return true
            val lastMove = previous.midi - beforeLeap.midi
            return abs(lastMove) < PERFECT_FOURTH ||
                (form.scale.isStep(previous, candidate) && (candidate.midi - previous.midi).sign == -lastMove.sign)
        }

        /**
         * [pitches] with, if the category allows them, some notes on a weak beat between two notes of the scale a whole
         * tone apart turned into the chromatic note between them, as long as the melody still follows the rules.
         */
        private fun passChromatically(pitches: List<Pitch>): List<Pitch> {
            if (!form.category.allowsChromaticPassingNotes) return pitches
            return (1 until pitches.lastIndex).fold(pitches) { melody, index ->
                val (before, after) = melody[index - 1] to melody[index + 1]
                val passing = Pitch((before.midi + after.midi) / 2)
                val passes = !slots[index].isOnStrongBeat && abs(after.midi - before.midi) == WHOLE_TONE &&
                    before in form.scale && after in form.scale && passing !in form.scale
                val changed = melody.mapIndexed { at, pitch -> if (at == index) passing else pitch }
                if (passes && random.nextDouble() < CHROMATIC_CHANCE && accepts(changed)) changed else melody
            }
        }

        /** Whether every note of [pitches] follows the rules after the notes before it. */
        private fun accepts(pitches: List<Pitch>): Boolean =
            pitches.indices.all { allows(slots[it], pitches[it], pitches.subList(0, it)) }
    }

    /** The seven-note scale of the harmony, whose chords borrow a note when it is not in it. */
    private val harmony: Scale = Scale(form.scale.tonic, form.scale.type.harmony)

    /**
     * Whether [candidate] stands a semitone from a note that the chord of [slot] borrows from out of the scale: the
     * G sharp of A harmonic minor under the G of its flat VII, a false relation.
     */
    private fun relatesFalsely(
        slot: Slot,
        candidate: Pitch,
    ): Boolean = slot.chord.pitches.filterNot { it in harmony }.any { borrowed ->
        candidate.semitonesAboveInOctave(borrowed) in listOf(1, SEMITONES_PER_OCTAVE - 1)
    }

    /** Whether slot [index] is the last note of a bar that ends a phrase, which closes on a note of its chord. */
    private fun endsPhrase(index: Int): Boolean =
        form.bars[slots[index].bar].role == BarRole.PHRASE_END && index == slotsOfBar[slots[index].bar].last

    /** Whether the note of slot [index] is the last one of [bar], which may then copy the bar before. */
    private fun closesBar(
        bar: Int,
        index: Int,
    ): Boolean = bar > 0 && index == slotsOfBar[bar].last

    /** Whether [placed], ending on the last note of [bar], plays that bar as a copy of the one before. */
    private fun copiesBarBefore(
        bar: Int,
        placed: List<Pitch>,
    ): Boolean = form.bars[bar].rhythm == form.bars[bar - 1].rhythm &&
        slotsOfBar[bar].map(placed::get) == slotsOfBar[bar - 1].map(placed::get)

    /**
     * How likely [candidate] is drawn after [previous] in [slot]: steps first, then thirds, and leaps rarely, towards the
     * height the arch reaches in the bar of [slot], leaning as [traits] say.
     */
    private fun freeWeight(
        slot: Slot,
        candidate: Pitch,
        previous: Pitch?,
        top: Pitch,
    ): Double {
        val index = window.indexOf(candidate)
        val away = (index - ARCH[slot.bar] * window.indexOf(top)) / REGISTER_SPREAD
        val register = 1 / (1 + away * away)
        if (previous == null) return register
        val move = MOVE_WEIGHTS.getOrElse(abs(index - window.indexOf(previous))) { LEAST_MOVE_WEIGHT }
        val fifth = if (abs(candidate.midi - previous.midi) == PERFECT_FIFTH) traits.fifths else 1.0
        val descent = when {
            !form.scale.isStep(previous, candidate) -> 1.0
            candidate.midi < previous.midi -> traits.descents
            else -> 1 / traits.descents
        }
        val arpeggio = if (candidate != previous && candidate in slot.chord && previous in slot.chord) {
            traits.arpeggios
        } else {
            1.0
        }
        return move * fifth * descent * arpeggio * register
    }

    /** [candidates] in the order of draws made by [weight], each draw among the candidates not drawn yet. */
    private fun drawn(
        candidates: List<Pitch>,
        weight: (Pitch) -> Double,
    ): Sequence<Pitch> = sequence {
        val left = candidates.toMutableList()
        while (left.isNotEmpty()) {
            var draw = random.nextDouble(left.sumOf(weight))
            val pick = left.firstOrNull { candidate ->
                draw -= weight(candidate)
                draw < 0
            } ?: left.last()
            left -= pick
            yield(pick)
        }
    }

    /** The notes of the motif stated in bar 1, as indices in the [window]. */
    private fun motifOf(placed: List<Pitch>): List<Int> = slotsOfBar.first().map { window.indexOf(placed[it]) }

    /**
     * Where the note of slot [index] would stand in the [window] to take up the contour of the motif stated in bar 1, or
     * `null` outside the bars that take it up again. The first note of the bar plays the same part of the chord in place
     * as the first note of the motif in the chord of bar 1, root, third or fifth, in the octave nearest the note before
     * it; the next ones move from the note before them by the moves of the motif, in degrees of the scale.
     */
    private fun motifTarget(
        index: Int,
        placed: List<Pitch>,
    ): Int? {
        val slot = slots[index]
        if (slot.bar == 0 || form.bars[slot.bar].role != BarRole.MOTIF) return null
        val motif = motifOf(placed)
        val position = index - slotsOfBar[slot.bar].first
        if (position > 0) return window.indexOf(placed.last()) + motif[position] - motif[position - 1]
        val first = slots.first().chord
        val part = first.quality.offsets.indexOf(window[motif.first()].semitonesAboveInOctave(first.root))
        val transposed = slot.chord.root.midi + slot.chord.quality.offsets[part]
        val octaves = -OCTAVES_AROUND..OCTAVES_AROUND
        val nearest = octaves.map { transposed + it * SEMITONES_PER_OCTAVE }.minBy { abs(it - placed.last().midi) }
        return window.indices.minBy { abs(window[it].midi - nearest) }
    }

    /**
     * [candidates] for slot [index] of a bar taking up the motif, the best first: on the first note of the bar, the
     * notes that do not leap a fourth or more from the bar before, then those from which the motif keeps most of its
     * contour; then the notes nearest the [target] of the contour.
     */
    private fun motifOrder(
        index: Int,
        candidates: List<Pitch>,
        target: Int,
        placed: List<Pitch>,
    ): List<Pitch> {
        val opensBar = index == slotsOfBar[slots[index].bar].first
        return candidates.sortedWith(
            compareBy<Pitch> { opensBar && abs(it.midi - placed.last().midi) >= PERFECT_FOURTH }
                .thenByDescending { if (opensBar) motifFit(index, it, placed) else 0 }
                .thenBy { abs(window.indexOf(it) - target) },
        )
    }

    /**
     * How many notes of the bar opening on slot [first] would strike a chord note on the strong beats, in the window, if
     * they took up the moves of the motif exactly from [start]: the transposition that keeps most of the motif.
     */
    private fun motifFit(
        first: Int,
        start: Pitch,
        placed: List<Pitch>,
    ): Int {
        val motif = motifOf(placed)
        return motif.indices.count { position ->
            val pitch = window.getOrNull(window.indexOf(start) + motif[position] - motif.first())
            val slot = slots[first + position]
            pitch != null && (!slot.isOnStrongBeat || pitch in slot.chord)
        }
    }

    private companion object {
        const val MOST_REPEATED_NOTES = 3

        /** The first notes that follow the last one again as the music loops, as far as the rules on moves look back. */
        const val LOOPED_NOTES = 3

        /** How likely a note that may turn into a chromatic passing note does. */
        const val CHROMATIC_CHANCE = 0.5

        /** The steps a search may take before starting again from scratch. */
        const val RESTART_STEPS = 1_000

        /** Bar 5, counted from 0, where the melody reaches its highest note. */
        const val PEAK_BAR = 4

        /** The height of the arch in each bar, from the lowest note of the window (0) to the peak (1). */
        val ARCH = listOf(0.4, 0.5, 0.6, 0.5, 1.0, 0.8, 0.5, 0.3)

        /** How far from the height of the arch, in degrees of the scale, a note is half as likely. */
        const val REGISTER_SPREAD = 2.0

        /** The weight of a move by 0 to 7 degrees of the scale: a repeat, a step, a third... up to an octave. */
        val MOVE_WEIGHTS = listOf(1.0, 10.0, 3.0, 1.5, 1.0, 0.3, 0.2, 0.2)
        const val LEAST_MOVE_WEIGHT = 0.1

        /** The octaves above and below the chord where the first note of a bar taking up the motif is looked for. */
        const val OCTAVES_AROUND = 3
    }
}
