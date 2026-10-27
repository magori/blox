package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class SheetChoiceRowTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName(
        "a choice row is 56 dp high, read as a radio button with its label and its sub-line, the chosen one checked",
    )
    fun aChoiceRowIsReadAsARadioButtonTheChosenOneChecked() = extension.use {
        // Given Surprise chosen, Marche not
        val picks = mutableListOf<String>()
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Column(Modifier.selectableGroup()) {
                    SheetChoiceRow(label = CHOSEN, subLine = CHOSEN_SUB_LINE, selected = true, onClick = {
                        picks += CHOSEN
                    })
                    SheetChoiceRow(label = OTHER, subLine = OTHER_SUB_LINE, selected = false, onClick = {
                        picks += OTHER
                    })
                }
            }
        }

        // Then « Surprise, Un peu de tout, case d'option, sélectionnée », once a row, 56 dp high
        onAllNodes(isSelectable()).assertCountEquals(2)
        onNode(isSelectable() and hasText(CHOSEN) and hasText(CHOSEN_SUB_LINE))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
            .assertHeightIsEqualTo(ROW_HEIGHT)
        onNode(isSelectable() and hasText(OTHER) and hasText(OTHER_SUB_LINE))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsNotSelected()
            .assertHeightIsEqualTo(ROW_HEIGHT)

        // And the chosen row alone bears its check
        onAllNodesWithTag(SHEET_CHOICE_CHECK_TAG, useUnmergedTree = true).assertCountEquals(1)
        onNode(hasTestTag(SHEET_CHOICE_CHECK_TAG) and hasAnyAncestor(isSelected()), useUnmergedTree = true)
            .assertExists()

        // When the other one is touched
        onNode(isSelectable() and hasText(OTHER)).performClick()

        // Then it is asked for (the caller holds the choice)
        runOnIdle { assertThat(picks).containsExactly(OTHER) }
    }

    private companion object {
        const val CHOSEN = "Surprise"
        const val CHOSEN_SUB_LINE = "Un peu de tout"
        const val OTHER = "Marche"
        const val OTHER_SUB_LINE = "Pas réguliers, entraînants"
        val ROW_HEIGHT = 56.dp
    }
}
