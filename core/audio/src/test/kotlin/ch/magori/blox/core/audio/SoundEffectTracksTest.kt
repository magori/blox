package ch.magori.blox.core.audio

import ch.magori.blox.core.music.SoundEffect
import ch.magori.blox.core.music.SoundEffects
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SoundEffectTracksTest {
    /** What the fake rendering gives: a short, recognisable buffer per effect. */
    private val buffers = mapOf(
        SoundEffects.rotation to floatArrayOf(1f),
        SoundEffects.linesCleared to floatArrayOf(2f, 2f),
    )

    private val rendered = mutableListOf<SoundEffect>()
    private val tracks = mutableListOf<FakeEffectTrack>()
    private val failures = mutableListOf<Throwable>()

    /** The openings of a track tried, whether they open or fail. */
    private var openings = 0

    @Test
    fun `an effect is rendered and its track opened at its first play, then each play replays the same track`() =
        runTest {
            // Given
            val player = player()

            // When the rotation plays three times
            repeat(3) { player.play(SoundEffects.rotation) }
            runCurrent()

            // Then
            assertThat(rendered).containsExactly(SoundEffects.rotation)
            assertThat(tracks).hasSize(1)
            assertThat(tracks.single().samples).containsExactly(1f)
            assertThat(tracks.single().plays).isEqualTo(3)
        }

    @Test
    fun `each effect has its own track, so that an effect playing over another does not cut it`() = runTest {
        // Given
        val player = player()

        // When
        player.play(SoundEffects.rotation)
        player.play(SoundEffects.linesCleared)
        runCurrent()

        // Then
        assertThat(tracks.map { it.samples.toList() }).containsExactly(listOf(1f), listOf(2f, 2f))
        assertThat(tracks.map { it.plays }).containsExactly(1, 1)
    }

    @Test
    fun `play returns at once, the effect is opened and played on the scope of the player`() = runTest {
        // Given
        val player = player()

        // When
        player.play(SoundEffects.rotation)

        // Then nothing ran yet on the caller
        assertThat(rendered).isEmpty()
        assertThat(tracks).isEmpty()
        runCurrent()
        assertThat(tracks.single().plays).isEqualTo(1)
    }

    @Test
    fun `several effects played before the scope runs open one track each`() = runTest {
        // Given
        val player = player()

        // When the rotation and the lines cleared are played in turn, three times, before the scope runs
        repeat(3) {
            player.play(SoundEffects.rotation)
            player.play(SoundEffects.linesCleared)
        }
        runCurrent()

        // Then
        assertThat(rendered).containsExactly(SoundEffects.rotation, SoundEffects.linesCleared)
        assertThat(tracks.map { it.samples.toList() }).containsExactly(listOf(1f), listOf(2f, 2f))
        assertThat(tracks.map { it.plays }).containsExactly(3, 3)
    }

    @Test
    fun `warming up renders and opens the effects on the scope without playing them, their first play only replays`() =
        runTest {
            // Given
            val player = player()

            // When the effects are warmed up
            player.warmUp(listOf(SoundEffects.rotation, SoundEffects.linesCleared))
            assertThat(tracks).isEmpty()
            runCurrent()

            // Then both are opened, silent
            assertThat(tracks.map { it.plays }).containsExactly(0, 0)

            // When the rotation then plays
            player.play(SoundEffects.rotation)
            runCurrent()

            // Then it replays the track warmed up, without rendering nor opening anything
            assertThat(rendered).containsExactly(SoundEffects.rotation, SoundEffects.linesCleared)
            assertThat(tracks.map { it.plays }).containsExactly(1, 0)
        }

    @Test
    fun `after an output that cannot open, the effect is not tried again for 5 s, its rendering kept`() = runTest {
        // Given an output that fails to open once
        openFailures += UnsupportedOperationException("No audio output")
        val player = player()
        player.play(SoundEffects.rotation)
        runCurrent()

        // When the rotation plays again and again within the 5 s that follow
        repeat(10) {
            player.play(SoundEffects.rotation)
            advanceTimeBy(400.milliseconds)
        }
        runCurrent()

        // Then nothing is tried, nor reported, again
        assertThat(openings).isEqualTo(1)
        assertThat(failures).hasSize(1)

        // When it plays once the 5 s have passed
        advanceTimeBy(1.seconds)
        player.play(SoundEffects.rotation)
        runCurrent()

        // Then a track opens and plays, from the rendering made the first time
        assertThat(openings).isEqualTo(2)
        assertThat(tracks.single().plays).isEqualTo(1)
        assertThat(rendered).containsExactly(SoundEffects.rotation)
    }

    @Test
    fun `an output that cannot open plays nothing, reports its failure, and opens at a play 5 s later`() = runTest {
        // Given an output that fails to open once
        val refusal = UnsupportedOperationException("No audio output")
        openFailures += refusal
        val player = player()

        // When the rotation plays, then again 5 s later
        player.play(SoundEffects.rotation)
        runCurrent()
        advanceTimeBy(5.seconds)
        player.play(SoundEffects.rotation)
        runCurrent()

        // Then the failure reached the player, not the caller, and the second play opened a track
        assertThat(failures).containsExactly(refusal)
        assertThat(tracks.single().plays).isEqualTo(1)
    }

    @Test
    fun `a track that fails to play reports its failure, is released, and another one opens at a play 5 s later`() =
        runTest {
            // Given the first track opened fails when it plays
            val player = player()
            failingPlays = 1

            // When the rotation plays, then again within 5 s, then once they have passed
            player.play(SoundEffects.rotation)
            runCurrent()
            player.play(SoundEffects.rotation)
            runCurrent()
            advanceTimeBy(5.seconds)
            player.play(SoundEffects.rotation)
            runCurrent()

            // Then
            assertThat(failures).hasSize(1).first().isInstanceOf(IllegalStateException::class.java)
            assertThat(tracks).hasSize(2)
            assertThat(tracks.map { it.released }).containsExactly(true, false)
            assertThat(tracks.map { it.plays }).containsExactly(0, 1)
        }

    @Test
    fun `a track that fails to play, then to be released, reports both failures and never reaches the caller`() =
        runTest {
            // Given the first track opened fails when it plays, and when it is released
            val player = player()
            failingPlays = 1
            failingReleases = true

            // When
            player.play(SoundEffects.rotation)
            runCurrent()

            // Then runTest would fail with an uncaught failure of the scope
            assertThat(failures).hasSize(2)
        }

    @Test
    fun `a rendering that fails plays nothing and reports its failure`() = runTest {
        // Given an effect the rendering does not know: it fails
        val player = player()

        // When
        player.play(SoundEffects.gameOver)
        runCurrent()

        // Then
        assertThat(failures).hasSize(1)
        assertThat(tracks).isEmpty()
    }

    /** The failures the next openings of a track throw, in order; empty, they open. */
    private val openFailures = ArrayDeque<RuntimeException>()

    /** How many of the next tracks opened fail at their first play. */
    private var failingPlays = 0

    /** Whether the tracks opened fail when they are released. */
    private var failingReleases = false

    private fun TestScope.player(): SoundEffectTracks = SoundEffectTracks(
        render = { effect ->
            rendered += effect
            buffers[effect] ?: throw IllegalArgumentException("No rendering for $effect")
        },
        openTrack = { samples ->
            openings++
            openFailures.removeFirstOrNull()?.let { throw it }
            FakeEffectTrack(samples, failsToPlay = failingPlays-- > 0, failingReleases).also { tracks += it }
        },
        scope = backgroundScope,
        timeSource = testScheduler.timeSource,
        onFailure = { failures += it },
    )

    /** A track that plays nothing and counts its plays; one that [failsToPlay] throws as a dead track would. */
    private class FakeEffectTrack(
        val samples: FloatArray,
        private val failsToPlay: Boolean,
        private val failsToRelease: Boolean,
    ) : EffectTrack {
        var plays = 0
            private set
        var released = false
            private set

        override fun play() {
            check(!failsToPlay) { "The track died" }
            plays++
        }

        override fun release() {
            released = true
            check(!failsToRelease) { "The track could not be released" }
        }
    }
}
