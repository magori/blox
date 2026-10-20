package ch.magori.blox.feature.composer

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.CategoryChoice
import ch.magori.blox.core.music.Surprise
import ch.magori.blox.feature.composer.ui.ChoiceGroup
import ch.magori.blox.feature.composer.ui.ComposerSizes
import ch.magori.blox.feature.composer.ui.ToolButton

/**
 * *Au hasard*, then what the dice composes in: Surprise and the seven categories, read as radio buttons, the one chosen,
 * [choice], filled. The choices scroll sideways when they do not fit, as with a large font, the one chosen kept in
 * sight.
 */
@Composable
internal fun DicePanel(
    choice: CategoryChoice,
    onCategoryChosen: (CategoryChoice) -> Unit,
    onDice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val half = maxWidth / 2
        Row(
            horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(
                text = stringResource(R.string.feature_composer_dice),
                onClick = onDice,
                labelPadding = LabelPadding,
                wraps = true,
                // Half the line at most, its label on two lines with a large font: the choices keep room to show
                // the one chosen.
                modifier = Modifier.widthIn(max = half),
            )
            ChoiceGroup(
                choices = DiceChoices,
                selected = choice,
                onChosen = onCategoryChosen,
                label = { stringResource(it.choiceLabel) },
                scroll = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** What the dice may compose in, in the order of the specification: Surprise first, then the seven categories. */
private val DiceChoices: List<CategoryChoice> = listOf(Surprise) + Category.entries

/** The room on each side of the label of *Au hasard*. */
private val LabelPadding = 6.dp

/** The name of a choice of the dice. */
@get:StringRes
private val CategoryChoice.choiceLabel: Int
    get() = when (this) {
        Surprise -> R.string.feature_composer_category_surprise
        is Category -> label
    }
