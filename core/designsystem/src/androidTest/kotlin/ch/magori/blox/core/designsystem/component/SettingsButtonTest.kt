package ch.magori.blox.core.designsystem.component

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class SettingsButtonTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("the settings button is a button of 48 dp, read Paramètres once, that opens the settings")
    fun theSettingsButtonIsReadParametres(kind: BloxThemeKind) = extension.use {
        // Given
        var opened = 0
        setContent { BloxTheme(kind = kind) { SettingsButton(onClick = { opened++ }) } }

        // Then a button, read once, its icon adding nothing
        onNodeWithContentDescription("Paramètres")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Paramètres")))
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
        onAllNodesWithContentDescription("Paramètres", useUnmergedTree = true).assertCountEquals(1)
        onNode(hasTestTag(GLYPH_BUTTON_ICON_TAG) and hasAnyAncestor(hasContentDescription("Paramètres")), true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))

        // When
        onNodeWithContentDescription("Paramètres").performClick()

        // Then
        runOnIdle { assertThat(opened).isEqualTo(1) }
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("the settings button is drawn as the button of the gear")
    fun theSettingsButtonIsDrawnAsTheGear(kind: BloxThemeKind) = extension.use {
        // Given the settings button, beside the button of the gear and that of the cartridge
        setContent {
            BloxTheme(kind = kind) {
                Column(verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2)) {
                    SettingsButton(onClick = {}, modifier = Modifier.testTag(SETTINGS))
                    GlyphButton(BloxIcon.GEAR, "Roue", onClick = {}, modifier = Modifier.testTag(GEAR))
                    GlyphButton(BloxIcon.CARTRIDGE, "Cartouche", onClick = {}, modifier = Modifier.testTag(OTHER))
                }
            }
        }

        // Then
        val settings = drawn(SETTINGS)
        assertThat(settings.sameAs(drawn(GEAR))).describedAs("as the gear").isTrue()
        assertThat(settings.sameAs(drawn(OTHER))).describedAs("as another icon").isFalse()
    }

    private fun ComposeContext.drawn(tag: String): Bitmap = onNodeWithTag(tag).captureToImage().asAndroidBitmap()

    private companion object {
        const val SETTINGS = "settings"
        const val GEAR = "gear"
        const val OTHER = "other"
    }
}
