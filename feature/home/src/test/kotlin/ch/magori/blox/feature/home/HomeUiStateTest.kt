package ch.magori.blox.feature.home

import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HomeUiStateTest {
    private val home = HomeUiState(
        theme = ThemeChoice.COLORFUL,
        music = MusicChoice.MY_FILE,
        startLevel = 5,
        startLevels = 0..9,
        record = 0,
        listenable = emptyList(),
        previewing = null,
    )

    @Test
    fun `with Mon fichier chosen but no music file kept, the reminder names Korobeiniki, played in its place`() {
        // Given Mon fichier chosen, without a file (the settings restored on another device)
        val withoutFile = home.copy(hasMusicFile = false)

        // When / Then
        assertThat(withoutFile.remindedMusic).isEqualTo(MusicChoice.KOROBEINIKI)
    }

    @Test
    fun `with Mon fichier chosen and a file kept, the reminder names Mon fichier`() {
        // Given Mon fichier chosen, its file kept
        val withFile = home.copy(hasMusicFile = true)

        // When / Then
        assertThat(withFile.remindedMusic).isEqualTo(MusicChoice.MY_FILE)
    }
}
