package ch.magori.blox.feature.musics

import androidx.annotation.StringRes
import ch.magori.blox.core.music.Category

/** The name of [category] on the card of a music, « Libre » for a music written by hand (null). */
@StringRes
internal fun categoryLabel(category: Category?): Int = when (category) {
    null -> R.string.feature_musics_category_free
    Category.JOYFUL -> R.string.feature_musics_category_joyful
    Category.CALM -> R.string.feature_musics_category_calm
    Category.EPIC -> R.string.feature_musics_category_epic
    Category.MELANCHOLIC -> R.string.feature_musics_category_melancholic
    Category.MYSTERIOUS -> R.string.feature_musics_category_mysterious
    Category.RETRO -> R.string.feature_musics_category_retro
    Category.DANCE -> R.string.feature_musics_category_dance
}
