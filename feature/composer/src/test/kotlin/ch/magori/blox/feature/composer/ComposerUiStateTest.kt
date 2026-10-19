package ch.magori.blox.feature.composer

import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.Tempo
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.model.pitch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ComposerUiStateTest {
    @Test
    fun `a tempo button is disabled at the end of the tempos, where it has no effect`() {
        // When
        val fastest = uiState(Tempo(Tempo.FASTEST))
        val slowest = uiState(Tempo(Tempo.SLOWEST))
        val between = uiState(Tempo(Tempo.FASTEST - Tempo.STEP))

        // Then
        assertThat(listOf(fastest.canSpeedUp, fastest.canSlowDown)).containsExactly(false, true)
        assertThat(listOf(slowest.canSpeedUp, slowest.canSlowDown)).containsExactly(true, false)
        assertThat(listOf(between.canSpeedUp, between.canSlowDown)).containsExactly(true, true)
    }

    @Test
    fun `the hint of an empty grid shows while the open track has no note, whatever the other track holds`() {
        // Given a note on the melody only
        val editor = EditorState.new().touch(pitch("E4"), eighth = 0)

        // When
        val melody = ComposerUiState(editor, showOtherTrack = true)
        val bass = ComposerUiState(editor.copy(track = Track.BASS), showOtherTrack = true)

        // Then
        assertThat(melody.isTrackEmpty).isFalse()
        assertThat(bass.isTrackEmpty).isTrue()
    }

    @Test
    fun `Listen is disabled while neither track has a note, as there is nothing to hear`() {
        // When
        val empty = ComposerUiState(EditorState.new(), showOtherTrack = true)
        val bassOnly = ComposerUiState(
            EditorState.new().copy(track = Track.BASS).touch(pitch("C3"), eighth = 0),
            showOtherTrack = true,
        )

        // Then
        assertThat(empty.canListen).isFalse()
        assertThat(bassOnly.canListen).isTrue()
    }

    private fun uiState(tempo: Tempo) =
        ComposerUiState(EditorState(ScoreDraft.new(tempo = tempo)), showOtherTrack = true)
}
