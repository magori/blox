package ch.magori.blox.feature.composer

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.designsystem.component.BloxSheet
import ch.magori.blox.core.designsystem.component.PrimaryButton
import ch.magori.blox.core.designsystem.component.SheetChoiceRow
import ch.magori.blox.core.designsystem.component.SheetChoices
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.CategoryChoice
import ch.magori.blox.core.music.Surprise

/**
 * The sheet « Au hasard » of the dice of the bar (V33): Surprise and the seven categories, each with what it sounds
 * like, the one chosen, [choice], checked; touching another chooses it through [onCategoryChosen], the sheet staying
 * open. *Tirer une musique* calls [onRoll], which rolls and closes it; the system back and the scrim call
 * [onDismissRequest].
 */
@Composable
internal fun DiceSheet(
    choice: CategoryChoice,
    onCategoryChosen: (CategoryChoice) -> Unit,
    onRoll: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BloxSheet(
        title = stringResource(R.string.feature_composer_dice),
        onDismissRequest = onDismissRequest,
        actions = { RollButton(onRoll) },
    ) {
        DiceChoices(choice, onCategoryChosen)
    }
}

/** The choices of the dice, in the order of the specification, [choice] checked. */
@Composable
internal fun DiceChoices(
    choice: CategoryChoice,
    onCategoryChosen: (CategoryChoice) -> Unit,
) {
    SheetChoices {
        DiceChoiceList.forEach {
            SheetChoiceRow(
                label = stringResource(it.choiceLabel),
                subLine = stringResource(it.subLine),
                selected = it == choice,
                onClick = { onCategoryChosen(it) },
            )
        }
    }
}

/** *Tirer une musique*, the action of the sheet, as wide as it. */
@Composable
internal fun ColumnScope.RollButton(onRoll: () -> Unit) {
    PrimaryButton(
        text = stringResource(R.string.feature_composer_dice_roll),
        icon = BloxIcon.DICE,
        onClick = onRoll,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** What the dice may compose in, in the order of the specification: Surprise first, then the seven categories. */
private val DiceChoiceList: List<CategoryChoice> = listOf(Surprise) + Category.entries

/** The name of a choice of the dice. */
@get:StringRes
private val CategoryChoice.choiceLabel: Int
    get() = when (this) {
        Surprise -> R.string.feature_composer_category_surprise
        is Category -> label
    }

/** What a choice of the dice sounds like, under its name. */
@get:StringRes
private val CategoryChoice.subLine: Int
    get() = when (this) {
        Surprise -> R.string.feature_composer_dice_surprise_line
        Category.JOYFUL -> R.string.feature_composer_dice_joyful_line
        Category.CALM -> R.string.feature_composer_dice_calm_line
        Category.EPIC -> R.string.feature_composer_dice_epic_line
        Category.MELANCHOLIC -> R.string.feature_composer_dice_melancholic_line
        Category.MYSTERIOUS -> R.string.feature_composer_dice_mysterious_line
        Category.RETRO -> R.string.feature_composer_dice_retro_line
        Category.DANCE -> R.string.feature_composer_dice_dance_line
    }
