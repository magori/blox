package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag

/** The check of a selected choice of a [ChoiceGroup], decorative, found in the tree not merged into its segment. */
fun isChoiceCheck(): SemanticsMatcher = hasTestTag(CHOICE_CHECK_TAG)
