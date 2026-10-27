package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.createComposeExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class ScoreChipTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("TalkBack reads a score chip by its description alone, not the capped value shown")
    fun talkBackReadsAScoreChipByItsDescriptionAlone(kind: BloxThemeKind) = extension.use {
        // Given a record beyond the six digits shown (moved from HomeScreenTest, the record of the home)
        setContent {
            BloxTheme(kind = kind) { ScoreChip(label = "Record", value = "999999", description = "Record 1234567") }
        }

        // Then TalkBack reads its value alone, not the text shown, capped (« RECORD 999999 »)
        onNodeWithContentDescription("Record 1234567")
            .assertExists()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
        onNodeWithText("Record", substring = true, ignoreCase = true).assertDoesNotExist()
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("a score chip is 48 dp high at least, as the discs beside it, even in a font twice as large")
    fun aScoreChipIs48DpHighAtLeast(kind: BloxThemeKind) = extension.use {
        // Given
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                BloxTheme(kind = kind) {
                    ScoreChip(label = "Meilleur du jour", value = "—", description = "Meilleur du jour : aucun essai")
                }
            }
        }

        // Then
        onNodeWithContentDescription("Meilleur du jour : aucun essai").assertHeightIsAtLeast(BloxSizes.minTouchTarget)
    }
}
