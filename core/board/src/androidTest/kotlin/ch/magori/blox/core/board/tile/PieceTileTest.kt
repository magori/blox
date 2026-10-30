package ch.magori.blox.core.board.tile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.piece.pieceCells
import ch.magori.blox.core.board.pieceColor
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.math.abs

// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
@OptIn(ExperimentalTestApi::class)
internal class PieceTileTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val right = ClassicRotationSystem.orientations(PieceType.L).first()
    private val piece = pieceCells(PieceType.L, right)

    @Test
    @DisplayName(
        "a tile is a button read by the name and the state it is given, chosen, its piece without a node of its own",
    )
    fun aTileIsAButtonReadByTheNameAndTheStateItIsGiven() = extension.use {
        // Given the chosen tile of a piece, named and read in its orientation by its screen
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                PieceTile(
                    piece = piece,
                    description = "Pièce L",
                    onClick = {},
                    modifier = Modifier.testTag(TAG),
                    chosen = true,
                    stateDescription = "un quart de tour à droite",
                )
            }
        }

        // Then TalkBack reads a button, by that name alone, its state when explored, never announced, and that it is
        // chosen; the piece drawn in it has no node of its own
        onNodeWithTag(TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Pièce L")))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "un quart de tour à droite"))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
            .assertIsSelected()
        onNodeWithTag(TAG, useUnmergedTree = true).onChildren()
            .filter(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .assertCountEquals(0)
    }

    @Test
    @DisplayName("without a piece, a tile is an empty slot, not chosen, which a touch still reaches")
    fun anEmptySlotIsStillTouched() = extension.use {
        // Given the tile of a piece placed, an empty slot
        val name = "Pièce L, posée : la reprendre"
        var touches = 0
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                PieceTile(
                    piece = null,
                    description = name,
                    onClick = { touches++ },
                    modifier = Modifier.testTag(TAG),
                )
            }
        }

        // When it is touched
        onNodeWithTag(TAG).performClick()

        // Then the screen hears it; the slot reads its name alone, without a state, not chosen
        assertThat(touches).isEqualTo(1)
        onNodeWithTag(TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(name)))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
            .assertIsNotSelected()
    }

    @Test
    @DisplayName("the size the screen gives a tile wins over that of a touch target, as the wide tiles of the garden")
    fun theSizeTheScreenGivesATileWins() = extension.use {
        // Given a tile 84 dp wide and 52 dp high, as in the tray of the musical garden
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                PieceTile(
                    piece = piece,
                    description = "Pièce L",
                    onClick = {},
                    modifier = Modifier.testTag(TAG).size(84.dp, 52.dp),
                )
            }
        }

        // Then it takes that size
        onNodeWithTag(TAG)
            .assertWidthIsEqualTo(84.dp)
            .assertHeightIsEqualTo(52.dp)
    }

    @Test
    @DisplayName("a tile of 48 dp, the least a screen gives it, draws its piece in its colour, never in black")
    fun aTileOf48DpDrawsItsPieceInItsColour() = extension.use {
        // Given a tile shrunk to a touch target, as in the tray of the Locksmith on a phone of 360 dp
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL, darkSystem = false) {
                PieceTile(
                    piece = piece,
                    description = "Pièce L",
                    onClick = {},
                    modifier = Modifier.size(48.dp).testTag(TAG),
                )
            }
        }

        // Then its pixels hold the colour of the L, and none is black
        val pixels = onNodeWithTag(TAG).captureToImage().toPixelMap()
        val colours = (0 until pixels.width).flatMap { x -> (0 until pixels.height).map { y -> pixels[x, y] } }
        val fill = pieceColor(BloxThemeVariant.COLORFUL_LIGHT, PieceType.L)
        assertThat(colours).noneMatch { it.red < DARK && it.green < DARK && it.blue < DARK }
        assertThat(colours).anyMatch { it.isCloseTo(fill) }
    }

    @Test
    @DisplayName("in a tile of 48 dp, the piece stays centred as in a tile of 52 dp, to a pixel")
    fun inATileOf48DpThePieceStaysCentred() = extension.use {
        // Given a tile shrunk to a touch target, beside a tile of its own size
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL, darkSystem = false) {
                Row {
                    PieceTile(piece, "Pièce L", onClick = {}, modifier = Modifier.size(48.dp).testTag(TAG))
                    PieceTile(piece, "Pièce L", onClick = {}, modifier = Modifier.testTag(WHOLE_TAG))
                }
            }
        }

        // Then the box around the coloured pixels of the L is as far from the centre in both, to a pixel: the relief of
        // the cells, lighter on top, makes that box sit a little below the centre of either
        val shrunk = offCentre(onNodeWithTag(TAG).captureToImage().toPixelMap())
        val whole = offCentre(onNodeWithTag(WHOLE_TAG).captureToImage().toPixelMap())
        assertThat(shrunk.first).isCloseTo(whole.first, within(1f))
        assertThat(shrunk.second).isCloseTo(whole.second, within(1f))
        assertThat(whole.first).isCloseTo(0f, within(1f))
    }

    /** How far the centre of the box around the coloured pixels of [pixels] is from their centre, across and down. */
    private fun offCentre(pixels: PixelMap): Pair<Float, Float> {
        val coloured = (0 until pixels.width).flatMap { x ->
            (0 until pixels.height).filter { y -> pixels[x, y].isVivid() }.map { y -> x to y }
        }
        val xs = coloured.map { it.first }
        val ys = coloured.map { it.second }
        return (xs.min() + xs.max() + 1 - pixels.width) / 2f to (ys.min() + ys.max() + 1 - pixels.height) / 2f
    }

    /** Whether this pixel is of the colour of a piece, rather than of the greys and whites of the tile. */
    private fun Color.isVivid(): Boolean = maxOf(red, green, blue) - minOf(red, green, blue) > VIVID

    private fun Color.isCloseTo(other: Color): Boolean =
        abs(red - other.red) < NEAR && abs(green - other.green) < NEAR && abs(blue - other.blue) < NEAR

    private companion object {
        const val TAG = "tile"
        const val WHOLE_TAG = "whole tile"

        /** Under this in each channel, a pixel is black. */
        const val DARK = 0.15f

        /** Within this in each channel, a pixel is of a colour. */
        const val NEAR = 0.1f

        /** Over this between its strongest and its weakest channel, a pixel is of a colour. */
        const val VIVID = 0.2f
    }
}
