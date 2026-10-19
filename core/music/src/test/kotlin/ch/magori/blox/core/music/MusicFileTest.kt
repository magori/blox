package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MusicFileTest {
    @Test
    fun `a music file is known by the uri of the document the player chose`() {
        // Given
        val uri = "content://com.android.providers.media.documents/document/audio%3A42"

        // When
        val file = MusicFile(uri)

        // Then
        assertThat(file.uri).isEqualTo(uri)
        assertThat(file).isEqualTo(MusicFile(uri))
    }

    @ParameterizedTest(name = "\"{0}\" is refused")
    @ValueSource(strings = ["", " "])
    fun `a music file without a uri is refused`(uri: String) {
        // When / Then
        assertThatIllegalArgumentException().isThrownBy { MusicFile(uri) }
    }

    @Test
    fun `a score and a music file are both a music to play`() {
        // Given
        val musics: List<Music> = listOf(Scores.korobeiniki, MusicFile("content://music/1"))

        // When
        val kinds = musics.map { music ->
            when (music) {
                is Score -> "score"
                is MusicFile -> "file"
            }
        }

        // Then
        assertThat(kinds).containsExactly("score", "file")
    }
}
