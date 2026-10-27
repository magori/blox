package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Plays a score in a loop. Each score is rendered once, away from the audio writing, and kept in a
 * cache ([TempoBuffers]), one mixed buffer per tempo factor, about 2.3 MB for 8 bars at 44 100 Hz. The
 * cache only holds the score played last and the scores it was switched to ([switchTo]), with their
 * renderings under way, which a stop does not cancel: playing another score abandons them. The loop
 * replays that buffer, so it never drifts. The samples go to a sink in slices, the sink paces the loop.
 * Playing another score replaces the one playing: there is never more than one loop, nor more than one
 * open sink. A sink that dies ([DeadSinkException]) is released and another one opened ([ReopeningSink]):
 * the music goes on where it stood. Any other failure (no audio output, a write refused, a sink that keeps
 * dying) stops the music, never the game: its [Playback.isPlaying] tells it.
 *
 * As soon as a score plays, its normal tempo and the tempos prepared for it ([prepareTempo], the danger
 * tempo of a game) are rendered in the background, if not cached yet, so that a change of tempo later
 * finds its buffer ready. A new tempo factor ([setTempoFactor]) is otherwise rendered once, in the
 * background, and cached too; the music keeps its sink and switches once that buffer is ready, at the
 * start of the next beat where a note starts in every voice ([switchBeats]), so that no note is cut.
 * The samples are never resampled, which would change the pitch. A tempo rendering that fails leaves
 * the music at its tempo; a rendering under way never holds back a stop, nor the next play.
 *
 * Another score switched to ([switchTo]) is rendered in the background at the tempo asked for, then goes on
 * from the next bar line, on the first beat of its bar of the same rank, on the same sink. Once it plays, its
 * normal tempo and the tempos prepared are rendered too. The score it replaces stays in the cache: switching
 * back to it renders nothing. A score switched to while stopped resumes on the first beat of its bar of the same rank
 * as the bar the music stopped in.
 */
public class ChiptuneSequencer internal constructor(
    private val render: (Score, Double) -> FloatArray,
    private val sampleRate: Int,
    private val openSink: (sampleRate: Int) -> AudioSink,
    private val scope: CoroutineScope,
    private val renderDispatcher: CoroutineDispatcher,
    private val writeDispatcher: CoroutineDispatcher,
    private val sliceSize: Int,
    private val onFailure: (Exception) -> Unit = {},
) : Player<Score> {
    /**
     * A sequencer that renders with [synthesizer] on [Dispatchers.Default] and writes to the sinks
     * [openSink] gives on [Dispatchers.IO], for as long as [scope] lives. A failure that stops the music
     * goes to [onFailure], to be logged; it is called on a thread of [Dispatchers.IO], the one the music
     * is written from, not on the main thread.
     */
    public constructor(
        synthesizer: Synthesizer,
        openSink: (sampleRate: Int) -> AudioSink,
        scope: CoroutineScope,
        onFailure: (Exception) -> Unit = {},
    ) : this(
        render = synthesizer::render,
        sampleRate = synthesizer.sampleRate,
        openSink = openSink,
        scope = scope,
        renderDispatcher = Dispatchers.Default,
        writeDispatcher = Dispatchers.IO,
        sliceSize = SLICE_SIZE,
        onFailure = onFailure,
    )

    /** Held by the loop playing: never two loops, nor two open sinks, at a time. */
    private val sinkLock = Mutex()

    /**
     * The buffers and renderings of the score played last and of the scores it was switched to; another score
     * played abandons them. Only used by the loop, under [sinkLock].
     */
    private val cache = mutableMapOf<Score, TempoBuffers>()

    /** The playback of the score played last, stopped or not. Only used on the main thread. */
    private var current: SequencerPlayback? = null

    /** The tempo factor asked for, read by the loop between two slices. */
    @Volatile
    private var requestedTempo = NORMAL_TEMPO

    /** The tempo factors to render ahead for the score played last, read by the loop between two slices. */
    @Volatile
    private var preparedTempos: Set<Double> = emptySet()

    /** The gain of the volume set, read by the loop between two slices; kept by a play, unlike the tempo. */
    @Volatile
    private var gain = Volume.FULL.gain

    /**
     * Plays [music] in a loop at the normal tempo, in place of the score playing, if any: the tempo asked
     * for before is forgotten, and so are the tempos prepared. Call it from the main thread.
     */
    override fun play(music: Score): Playback {
        requestedTempo = NORMAL_TEMPO
        preparedTempos = emptySet()
        return start(music, from = null)
    }

    /**
     * Goes on with [score] from the next bar line once it is rendered at the tempo asked for, in place of the score
     * played last; kept for the resume of its playback if it is stopped. Call it from the main thread.
     */
    override fun switchTo(score: Score) {
        current?.let {
            // Its buffers kept first: a loop starting meanwhile that sees the new score never abandons them.
            it.scores += score
            it.score = score
        }
    }

    /**
     * Renders the score played last at [factor] in the background, as soon as it plays, so that switching
     * to that tempo later renders nothing. Kept for a resume, forgotten by the next [play]. Call it from
     * the main thread.
     */
    override fun prepareTempo(factor: Double) {
        requireValidTempo(factor)
        preparedTempos = preparedTempos + factor
    }

    /**
     * Plays the scores at [volume], the one playing from its next slice: its sink plays at the gain of the volume,
     * so that a change reaches at once the samples it holds. A volume of 0 goes on playing, in silence. It may be
     * called from any thread: the gain is a `@Volatile` field, read by the loop between two slices.
     */
    override fun setVolume(volume: Volume) {
        gain = volume.gain
    }

    /**
     * Plays [score] in a loop from the start, or from where [from] stood, in place of the score playing; the
     * buffers of the other [scores] its playback played or was switched to stay ready.
     */
    private fun start(
        score: Score,
        from: Place?,
        scores: Set<Score> = setOf(score),
    ): Playback {
        val playback = SequencerPlayback(score, from, scores)
        // Stopped at once: the loop playing writes no more slice, nor sees the tempos of the next score.
        current?.stop()
        val job = scope.launch(writeDispatcher) {
            // One loop at a time: this one opens its sink once the one before has released its own, in
            // the order of the plays; a loop stopped while it waits never gets it. The cache is only used
            // under this lock.
            sinkLock.withLock {
                try {
                    loop(playback, from)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Exception) {
                    // No audio output, a rendering or a write that fails: the music stops, the game goes on.
                    onFailure(failure)
                }
            }
        }
        // However the loop ends (a stop, another score, a failure), its playback no longer plays.
        job.invokeOnCompletion { playback.stopped() }
        playback.job = job
        current = playback
        return playback
    }

    /** Where a loop stood: a [position] in a buffer of [length] samples of [score]. */
    private class Place(val position: Int, val length: Int, val score: Score)

    /** A score played by this sequencer, which keeps where its loop stood, to [resume] from there. */
    private inner class SequencerPlayback(score: Score, from: Place?, scores: Set<Score>) : Playback {
        lateinit var job: Job

        /** The score it plays, or goes on with once switched to ([switchTo]): written on the main thread. */
        @Volatile
        var score: Score = score

        /** The scores it played or was switched to, whose buffers stay ready: written on the main thread. */
        @Volatile
        var scores: Set<Score> = scores

        /** Whether this playback plays: false for good once stopped, replaced, or once its loop failed. */
        private val playing = MutableStateFlow(true)

        override val isPlaying: Flow<Boolean> = playing.asStateFlow()

        /**
         * Where the loop stood after its last slice written, or, before it wrote anything, where it
         * started from: a playback resumed then stopped at once keeps its place. Read on the main thread.
         */
        @Volatile
        var place: Place? = from

        // Stops this loop only: a play or a resume cancels the loop it replaces, so the job of a playback
        // replaced is cancelled already, and a game left after the next one started stops nothing.
        override fun stop() {
            job.cancel()
            stopped()
        }

        /** The loop ended, or is about to: called from the main thread, or from the thread of the loop. */
        fun stopped() {
            playing.value = false
        }

        override fun resume(): Playback = start(score, from = place, scores = scores)
    }

    /**
     * Renders the score of [playback] at the tempo asked for, or takes it from the cache, then writes it in a loop
     * until cancelled. Its normal tempo and the tempos prepared are rendered meanwhile, and so is a new tempo asked
     * for, which plays from the next beat once ready, and a score switched to, which plays from the next bar line.
     */
    private suspend fun loop(
        playback: SequencerPlayback,
        from: Place?,
    ) {
        var score = playback.score
        var beats = score.beatCount
        // The beats where no note is cut, then the end of the buffer, where the loop starts again.
        var switches = switchBeats(score) + beats
        keepOnly(playback.scores)
        var buffers = buffersFor(score)
        var tempo = requestedTempo
        var buffer = buffers.bufferOf(tempo)
        // An output that dies (a restart of the audio server, an output gone) is reopened.
        val sink = ReopeningSink { openSink(sampleRate) }
        // The tempo asked for last, rendered in the background while it is not the one playing.
        var asked = tempo
        // The score switched to, and the tempo asked for, rendered in the background until it plays.
        var switchAsked: Pair<TempoBuffers, Double>? = null
        // The score last read as switched to, and its buffers, null when it is the score playing.
        var switchTarget: Score = score
        var switchBuffers: TempoBuffers? = null
        // The gain the sink plays at: that of a new sink, until another volume is set.
        var sinkGain = Volume.FULL.gain
        try {
            var cursor = LoopCursor(
                buffer,
                from?.let { resumeAt(it, score, buffer.size, switches) } ?: 0,
            )
            val slice = FloatArray(sliceSize)
            while (true) {
                // A real sink blocks instead of suspending: check for a stop between slices.
                currentCoroutineContext().ensureActive()
                buffers.prepare(NORMAL_TEMPO)
                preparedTempos.forEach(buffers::prepare)
                buffers.harvest()
                val wantedGain = gain
                if (wantedGain != sinkGain) {
                    sink.setGain(wantedGain)
                    sinkGain = wantedGain
                }
                val wanted = requestedTempo
                if (wanted != asked) {
                    // Of quick changes, only the last one is rendered.
                    if (asked != tempo) buffers.giveUp(asked)
                    asked = wanted
                    if (asked != tempo) buffers.request(asked)
                }
                var written = 0
                // The score switched to, if any, at the tempo asked for: of quick switches, only the last one
                // is rendered.
                val target = playback.score
                // Looked up in the cache once per score switched to: hashing a whole score at each slice would cost
                // on the audio thread.
                if (target !== switchTarget) {
                    switchTarget = target
                    switchBuffers = if (target != score) buffersFor(target) else null
                }
                val targetBuffers = switchBuffers
                val wantedSwitch = targetBuffers?.let { it to asked }
                if (wantedSwitch != switchAsked) {
                    switchAsked?.let { (pending, factor) -> pending.giveUp(factor) }
                    switchAsked = wantedSwitch
                    wantedSwitch?.let { (pending, factor) -> pending.request(factor) }
                }
                targetBuffers?.harvest()
                val targetReady = targetBuffers?.ready(asked)
                if (targetBuffers != null && targetReady != null) {
                    // Switch at the start of the next bar, if it comes within this slice.
                    val grid = BeatGrid(buffer.size, beats)
                    val bar = score.beatsPerBar
                    val barLine = (grid.nextBeat(cursor.position) + bar - 1) / bar * bar
                    val beforeBar = grid.startOf(barLine) - cursor.position
                    if (beforeBar < sliceSize) {
                        cursor.fill(slice, until = beforeBar)
                        if (asked != tempo) buffers.giveUp(asked)
                        val rank = barLine % beats / bar
                        score = target
                        buffers = targetBuffers
                        switchAsked = null
                        switchBuffers = null
                        beats = score.beatCount
                        switches = switchBeats(score) + beats
                        buffer = targetReady
                        tempo = asked
                        cursor = LoopCursor(buffer, BeatGrid(buffer.size, beats).startOf(firstBeatOfBar(rank, score)))
                        written = beforeBar
                    }
                }
                // A rendering that failed gives no buffer: the old tempo goes on.
                val ready = if (asked != tempo) buffers.ready(asked) else null
                if (ready != null) {
                    // Switch at the start of the next beat where no note is cut, if it comes within this slice.
                    val grid = BeatGrid(buffer.size, beats)
                    val beat = switches.first { it >= grid.nextBeat(cursor.position) }
                    val beforeBeat = grid.startOf(beat) - cursor.position
                    if (beforeBeat < sliceSize) {
                        cursor.fill(slice, until = beforeBeat)
                        buffer = ready
                        tempo = asked
                        cursor = LoopCursor(buffer, BeatGrid(buffer.size, beats).startOf(beat % beats))
                        written = beforeBeat
                    }
                }
                cursor = cursor.fill(slice, from = written)
                playback.place = Place(cursor.position, buffer.size, score)
                sink.write(slice)
            }
        } finally {
            // A tempo asked for, not prepared, is not rendered for nothing; a tempo prepared goes on rendering.
            if (asked != tempo) buffers.giveUp(asked)
            switchAsked?.let { (pending, factor) -> pending.giveUp(factor) }
            sink.release()
        }
    }

    /**
     * Where [score] resumes from [place], in its buffer of [length] samples: the same place if [place] stood in it,
     * at the start of the beat of [switches] it is in; else, a score switched to while stopped, the first beat of its
     * bar of the same rank as the bar [place] stood in.
     */
    private fun resumeAt(
        place: Place,
        score: Score,
        length: Int,
        switches: List<Int>,
    ): Int {
        val beats = score.beatCount
        if (place.score == score) return resumePosition(place.position, place.length, length, switches, beats)
        val beat = BeatGrid(place.length, place.score.beatCount).nextBeat(place.position + 1) - 1
        return BeatGrid(length, beats).startOf(firstBeatOfBar(beat / place.score.beatsPerBar, score))
    }

    /** The buffers of [score], from the cache, or new ones. */
    private fun buffersFor(score: Score): TempoBuffers =
        cache.getOrPut(score) { TempoBuffers(score, render, renderDispatcher, parent = scope.coroutineContext[Job]) }

    /** Abandons the buffers of the scores other than [scores]. */
    private fun keepOnly(scores: Set<Score>) {
        val others = cache.filterKeys { it !in scores }
        others.values.forEach(TempoBuffers::abandon)
        cache -= others.keys
    }

    /**
     * Stops the score playing and releases its sink; does nothing if none is playing. Call it from
     * the main thread.
     */
    public fun stop() {
        current?.stop()
    }

    /**
     * Sets the tempo factor of the music: 1.0 is the tempo of the score, the caller chooses another
     * one (the stack danger). Kept for the next resume when nothing is playing; the next [play] starts
     * at 1.0 again. Call it from the main thread.
     */
    override fun setTempoFactor(factor: Double) {
        requireValidTempo(factor)
        requestedTempo = factor
    }

    private fun requireValidTempo(factor: Double) {
        require(factor > 0 && factor.isFinite()) { "The tempo factor is positive and finite, was $factor" }
    }

    private companion object {
        const val NORMAL_TEMPO = 1.0
    }
}

/** Samples per slice: about 23 ms at 44 100 Hz, short enough to stop quickly, long enough to keep the sink fed. */
internal const val SLICE_SIZE: Int = 1_024

/**
 * Where a loop stopped at [position] in a buffer of [length] samples resumes in a buffer of [newLength]
 * samples of [beats] beats: the same place in the score, `position × newLength / length` rounded and
 * brought back into the buffer, then back to the start of the beat of [switchBeats] it is in, so that
 * no note is cut. Computed on [Long].
 */
internal fun resumePosition(
    position: Int,
    length: Int,
    newLength: Int,
    switchBeats: List<Int>,
    beats: Int,
): Int {
    val scaled = ((2L * position * newLength + length) / (2L * length) % newLength).toInt()
    val grid = BeatGrid(newLength, beats)
    return grid.startOf(switchBeats.last { grid.startOf(it) <= scaled })
}

/** The first beat of the bar of [score] of rank [rank], modulo its number of bars. */
internal fun firstBeatOfBar(
    rank: Int,
    score: Score,
): Int = rank % (score.beatCount / score.beatsPerBar) * score.beatsPerBar
