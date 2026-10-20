package ch.magori.blox.feature.musics

import ch.magori.blox.core.music.Category
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CategoryLabelTest {
    @Test
    fun `each category has a label of its own, and a music written by hand another one`() {
        // When
        val labels = (Category.entries + null).map(::categoryLabel)

        // Then
        assertThat(labels).doesNotHaveDuplicates()
        assertThat(categoryLabel(null)).isEqualTo(R.string.feature_musics_category_free)
    }
}
