package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class SettingActionRowTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("an action wider than its row goes under the label and stays within the row")
    fun anActionWiderThanItsRowStaysWithinIt() = extension.use {
        // Given an action that asks 400 dp, in a row of 300 dp
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                SettingActionRow(
                    icon = BloxIcon.TROPHY,
                    label = "Record du Classique",
                    modifier = Modifier.testTag(ROW).width(300.dp),
                ) {
                    Box(Modifier.testTag(ACTION).width(400.dp).height(40.dp))
                }
            }
        }

        // Then
        val row = onNodeWithTag(ROW).getUnclippedBoundsInRoot()
        val action = onNodeWithTag(ACTION).getUnclippedBoundsInRoot()
        assertThat(action.left).isGreaterThanOrEqualTo(row.left)
        assertThat(action.right).isLessThanOrEqualTo(row.right)
    }

    private companion object {
        const val ROW = "row"
        const val ACTION = "action"
    }
}
