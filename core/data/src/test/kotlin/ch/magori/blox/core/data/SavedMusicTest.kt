package ch.magori.blox.core.data

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.core.music.ScoreId
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SavedMusicTest {
    private val scale = Scale(Pitch.parse("C4"), ScaleType.MAJOR)

    @Test
    fun `a saved music is known by the identity of its score`() {
        // Given a score known by another identity
        val score = aScore(ScoreId.Personal("other"))

        // When / Then
        assertThatIllegalArgumentException().isThrownBy {
            SavedMusic(ScoreId.Personal("mine"), "Mine", Category.CALM, scale, score)
        }
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["", "   "])
    fun `a saved music has a name that is not blank`(name: String) {
        // Given
        val id = ScoreId.Personal("mine")

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { SavedMusic(id, name, null, scale, aScore(id)) }
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = [" Calme", "Calme ", "\tCalme"])
    fun `a saved music has no spaces around its name`(name: String) {
        // Given
        val id = ScoreId.Personal("mine")

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { SavedMusic(id, name, null, scale, aScore(id)) }
    }
}
