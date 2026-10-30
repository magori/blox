package ch.magori.blox.core.designsystem.text

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.component.nameTooLongText

/** What a window names: its texts say « musique » or « jardin ». */
enum class NamedThing { MUSIC, GARDEN }

/**
 * Why a name is refused, as the window tells it: the `NameProblem` of `core:data`, which this module does not see, the
 * same entries by the same names (`NameRefusal.valueOf(problem.name)`).
 */
enum class NameRefusal {
    /** Empty, or only spaces. */
    BLANK,

    /**
     * More than the most characters a name may have. Never shown today: the editor, Mes musiques and the garden cut the
     * name typed at that length (`cutName`).
     */
    TOO_LONG,

    /** Another of the list has it. */
    TAKEN,
}

/**
 * What the window that names a [thing] says, under the field, of a name refused for [refusal]: « Donne-lui un nom. »,
 * whatever is named, « Une autre musique porte déjà ce nom. », « 40 caractères au plus. » ([maxLength]).
 */
@Composable
@ReadOnlyComposable
fun nameRefusalText(
    refusal: NameRefusal,
    thing: NamedThing,
    maxLength: Int,
): String = when (refusal) {
    NameRefusal.BLANK -> stringResource(R.string.core_designsystem_name_blank)
    NameRefusal.TOO_LONG -> nameTooLongText(maxLength)
    NameRefusal.TAKEN -> stringResource(thing.takenText)
}

/** What the window says of a name another has: « Un autre jardin porte déjà ce nom. ». */
@get:StringRes
internal val NamedThing.takenText: Int
    get() = when (this) {
        NamedThing.MUSIC -> R.string.core_designsystem_name_taken_music
        NamedThing.GARDEN -> R.string.core_designsystem_name_taken_garden
    }
