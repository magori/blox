package ch.magori.blox.core.board.garden

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.drawnContent
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.garden.Garden
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
@OptIn(ExperimentalTestApi::class)
internal class GardenThumbnailDrawingTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("a thumbnail is decorative, 48 by 42 dp: TalkBack does not read it, its row says what it shows")
    fun aThumbnailIsDecorative() = extension.use {
        // Given the thumbnail of a garden
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                GardenThumbnail(garden = Garden.EMPTY, modifier = Modifier.testTag(TAG))
            }
        }

        // Then it takes its size, hidden from TalkBack, without a word of its own
        onNodeWithTag(TAG, useUnmergedTree = true)
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(42.dp)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    @DisplayName("at a density that cuts a cell in a fraction of a pixel, the cells meet without a seam")
    fun theCellsMeetWithoutASeam() = extension.use {
        // Given the thumbnail of a garden of an O, by day, at 2.625 pixels a dp: a cell of 7.875 px
        val garden = Garden.parse(
            """
            ................
            OO..............
            OO..............
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            """,
        )
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(DENSITY)) {
                BloxTheme(kind = BloxThemeKind.COLORFUL, darkSystem = false) {
                    GardenThumbnail(garden = garden, modifier = Modifier.testTag(TAG))
                }
            }
        }

        // When it is drawn
        drawnContent()
        val bitmap = onNodeWithTag(TAG).captureToImage().asAndroidBitmap()

        // Then each pixel is a free cell, a free cell of a band or the O, never a blend of two at an edge
        val variant = BloxThemeVariant.COLORFUL_LIGHT
        val expected = setOf(
            gardenCellStyle(variant, GardenCell.Free, inBand = false).fill,
            gardenCellStyle(variant, GardenCell.Free, inBand = true).fill,
            gardenCellStyle(variant, GardenCell.Note(PieceType.O), inBand = false).fill,
        ).map { it.toArgb() }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertThat(pixels.toSet()).containsExactlyInAnyOrderElementsOf(expected)
    }

    private companion object {
        const val TAG = "thumbnail"
        const val DENSITY = 2.625f
    }
}
