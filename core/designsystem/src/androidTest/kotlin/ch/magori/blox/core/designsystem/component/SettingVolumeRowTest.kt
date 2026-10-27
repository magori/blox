package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performSemanticsAction
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class SettingVolumeRowTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("TalkBack reads one slider named by the label of its row, the label shown once and not read again")
    fun talkBackReadsOneSliderNamedByItsRow() = extension.use {
        // Given
        val asked = mutableListOf<Int>()
        setRow(onPercentChange = { asked += it })

        // Then the label is shown, left out by TalkBack, which reads it once, as the name of the slider
        onNode(hasText(LABEL), useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility))
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(LABEL)))
            .assertIsEnabled()

        // When TalkBack moves it one step down
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(70f) }

        // Then
        assertThat(asked).containsExactly(70)
    }

    @Test
    @DisplayName("a disabled row is read as unavailable and cannot be moved")
    fun aDisabledRowCannotBeMoved() = extension.use {
        // Given
        setRow(enabled = false)

        // Then
        onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertIsNotEnabled()
    }

    private fun ComposeContext.setRow(
        enabled: Boolean = true,
        onPercentChange: (Int) -> Unit = {},
    ) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                SettingVolumeRow(
                    icon = BloxIcon.VOLUME,
                    label = LABEL,
                    percent = 80,
                    onPercentChange = onPercentChange,
                    enabled = enabled,
                )
            }
        }
    }

    private companion object {
        const val LABEL = "Volume des effets"
    }
}
