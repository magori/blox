package ch.magori.blox.core.board.tile

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.piece.pieceCells
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

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

    private companion object {
        const val TAG = "tile"
    }
}
