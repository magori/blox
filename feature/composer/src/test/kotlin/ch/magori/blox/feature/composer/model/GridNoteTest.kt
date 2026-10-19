package ch.magori.blox.feature.composer.model

import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GridNoteTest {
    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `a note lasts at least a sixteenth note`(length: Int) {
        assertThatIllegalArgumentException().isThrownBy { GridNote(pitch("E4"), 0, length) }
    }
}
