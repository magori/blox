package ch.magori.blox.core.audio

import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Scores
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The fake shared by the tests of the features keeps the contract of the real player; it plays music files too. */
class FakeMusicPlayerTest : MusicPlayerContract() {
    override fun playerUnderTest(scope: TestScope): PlayerUnderTest = heardThrough(FakeMusicPlayer())

    private val myFile = MusicFile("content://music/my-file")

    @Test
    fun `a music file plays, noted by its uri`() {
        // Given
        val fake = FakeMusicPlayer()

        // When
        fake.play(myFile).stop()
        fake.play(myFile).apply { stop() }.resume()

        // Then
        assertThat(fake.calls).containsExactly(
            "play file ${myFile.uri}",
            "stop",
            "play file ${myFile.uri}",
            "stop",
            "resume file ${myFile.uri}",
        )
        assertThat(fake.playingFile).isEqualTo(myFile)
    }

    @Test
    fun `a music file replaces the score playing, and a score the file`() = runTest {
        // Given a score playing
        val fake = FakeMusicPlayer()
        val score = fake.play(Scores.minuet)

        // When a file plays
        val file = fake.play(myFile)

        // Then
        assertThat(score.isPlaying.first()).isFalse()
        assertThat(fake.playing).isNull()
        assertThat(fake.playingFile).isEqualTo(myFile)

        // When a score plays again
        fake.play(Scores.korobeiniki)

        // Then
        assertThat(file.isPlaying.first()).isFalse()
        assertThat(fake.playingFile).isNull()
        assertThat(fake.playing).isEqualTo(Scores.korobeiniki.id)
    }
}
