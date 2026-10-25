package ch.magori.blox.feature.game.controls

import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KeyDropTest {
    @Test
    fun `a held key goes 2 dp down`() {
        // Then (docs/design-system: a pressed key takes its pressed fill and goes 2 px down, in every theme): a cue
        // other than colour, where the pressed fill stands out little from the face at rest
        assertThat(keyDrop(held = true)).isEqualTo(2.dp)
        assertThat(keyDrop(held = false)).isEqualTo(0.dp)
    }
}
