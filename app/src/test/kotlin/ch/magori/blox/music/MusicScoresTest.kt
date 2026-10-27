package ch.magori.blox.music

import ch.magori.blox.core.data.FakeMusicFileRepository
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Scores
import ch.magori.blox.feature.game.music.GameMusic
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

class MusicScoresTest {
    private val myFile = MusicFile("content://com.android.providers.media.documents/document/audio%3A42")

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("choices")
    fun `each music choice plays its score, or none`(
        choice: MusicChoice,
        expected: Score?,
    ) {
        // Given / When / Then
        assertThat(scoreOf(choice)).isEqualTo(expected)
    }

    @Test
    fun `my file plays the music file chosen`() {
        // When / Then
        assertThat(musicOf(MusicChoice.MY_FILE, myFile)).isEqualTo(GameMusic.Fixed(myFile))
    }

    @Test
    fun `my file without a file chosen plays Korobeiniki, as on another device the settings were restored on`() {
        // When / Then
        assertThat(musicOf(MusicChoice.MY_FILE, file = null)).isEqualTo(GameMusic.Fixed(Scores.korobeiniki))
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(names = ["KOROBEINIKI", "MINUET", "NONE"])
    fun `the other choices play their score, or none, whatever the file chosen`(choice: MusicChoice) {
        // When / Then
        assertThat(musicOf(choice, myFile)).isEqualTo(scoreOf(choice)?.let(GameMusic::Fixed))
    }

    @Test
    fun `the live choice asks for a live music rather than a score`() {
        // When / Then: the two moods are composed for each game, from its seed, and not by the choice
        assertThat(musicOf(MusicChoice.LIVE, myFile)).isEqualTo(GameMusic.Live)
        assertThat(musicOf(MusicChoice.LIVE, file = null)).isEqualTo(GameMusic.Live)
    }

    @Test
    fun `a music file that cannot be played falls back on the score of my file without a file`() {
        // When / Then: the fallback of the player (MusicSwitch) is the one of the settings restored without their file
        assertThat(GameMusic.Fixed(FALLBACK_SCORE)).isEqualTo(musicOf(MusicChoice.MY_FILE, file = null))
    }

    @Test
    fun `the music of a choice is read with the music file kept`() = runTest {
        // Given a music file kept
        val files = FakeMusicFileRepository(initial = myFile)
        val noFile = FakeMusicFileRepository()

        // When / Then
        assertThat(files.musicOf(MusicChoice.MY_FILE)).isEqualTo(GameMusic.Fixed(myFile))
        assertThat(noFile.musicOf(MusicChoice.MY_FILE)).isEqualTo(GameMusic.Fixed(Scores.korobeiniki))
        assertThat(files.musicOf(MusicChoice.MINUET)).isEqualTo(GameMusic.Fixed(Scores.minuet))
        assertThat(files.musicOf(MusicChoice.LIVE)).isEqualTo(GameMusic.Live)
    }

    private companion object {
        @JvmStatic
        fun choices(): List<Arguments> = listOf(
            argumentSet("Korobeiniki", MusicChoice.KOROBEINIKI, Scores.korobeiniki),
            argumentSet("the minuet", MusicChoice.MINUET, Scores.minuet),
            argumentSet("no music", MusicChoice.NONE, null),
            argumentSet("my file, a file and not a score", MusicChoice.MY_FILE, null),
            argumentSet("the live music, composed for each game and not a fixed score", MusicChoice.LIVE, null),
        )
    }
}
