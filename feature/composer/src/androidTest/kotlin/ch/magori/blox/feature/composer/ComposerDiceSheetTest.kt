package ch.magori.blox.feature.composer

import android.view.KeyEvent
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.designsystem.state.SwappableRegistry
import ch.magori.blox.core.designsystem.text.assertWordsWhole
import ch.magori.blox.core.designsystem.text.textLayout
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/** The dice of the bar of the editor and its sheet « Au hasard » (P5.101, V33). */
@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class ComposerDiceSheetTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val viewModel = composerViewModel(FakeMusicPlayer(), FakeMusicLibrary())

    @Test
    @DisplayName("the dice of the bar opens the sheet Au hasard, read as a dialog")
    fun theDiceOpensTheSheet() = extension.use {
        // Given
        showEditor()
        onNode(isDialog()).assertDoesNotExist()

        // When
        onNodeWithContentDescription("Au hasard").performClick()

        // Then a dialog, its title read first, as a heading
        onNode(isDialog()).assertExists()
        onNode(hasText("Au hasard") and isHeading() and hasAnyAncestor(isDialog())).assertExists()
    }

    @Test
    @DisplayName(
        "the sheet offers Surprise and the seven categories, each with what it sounds like, the chosen one checked",
    )
    fun theSheetOffersTheCategories() = extension.use {
        // Given
        showEditor()

        // When
        onNodeWithContentDescription("Au hasard").performClick()

        // Then eight radio buttons, each with its sub-line, Surprise chosen at first
        assertThat(
            onAllNodes(isSelectable() and hasAnyAncestor(isDialog())).fetchSemanticsNodes(),
        ).hasSize(CHOICES.size)
        CHOICES.forEach { (label, subLine) ->
            val row = choice(label)
                .assert(hasText(subLine))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            if (label == "Surprise") row.assertIsSelected() else row.assertIsNotSelected()
        }
    }

    @Test
    @DisplayName("choosing a category keeps the sheet open and the choice")
    fun choosingACategoryKeepsTheSheetOpen() = extension.use {
        // Given the sheet open
        showEditor()
        onNodeWithContentDescription("Au hasard").performClick()

        // When
        choice("Calme").performScrollTo().performClick()

        // Then
        onNode(isDialog()).assertExists()
        choice("Calme").assertIsSelected()
        choice("Surprise").assertIsNotSelected()

        // When the sheet is closed by the system back, then opened again
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        onNode(isDialog()).assertDoesNotExist()
        onNodeWithContentDescription("Au hasard").performClick()

        // Then the choice is kept
        choice("Calme").assertIsSelected()
    }

    @Test
    @DisplayName(
        "Tirer une musique rolls in the category chosen and closes the sheet, and a message tells Annuler",
    )
    fun tirerUneMusiqueRollsAndClosesTheSheet() = extension.use {
        // Given a note in the grid, and Calme chosen in the sheet
        showEditor()
        onNodeWithContentDescription("Mi bas, temps 1").performClick()
        onNodeWithContentDescription("Au hasard").performClick()
        choice("Calme").performScrollTo().performClick()
        waitForIdle()
        mainClock.autoAdvance = false

        // When
        rollButton().performClick()
        mainClock.advanceTimeBy(MESSAGE_SHOWN_MILLIS)

        // Then the sheet is closed, the line under the title tells the style, and the message tells Annuler
        onNode(isDialog()).assertDoesNotExist()
        onNodeWithText("Calme · ", substring = true).assertExists()
        onNodeWithText(DICE_MESSAGE).assertExists()

        // When
        onNodeWithText("Annuler").performClick()
        mainClock.advanceTimeBy(MESSAGE_SHOWN_MILLIS)

        // Then the note is back, and the message gone with the undo
        onNodeWithContentDescription("Mi bas, temps 1")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "note"))
        onNodeWithText(DICE_MESSAGE).assertDoesNotExist()
    }

    @Test
    @DisplayName("on a small phone with the largest font, each choice of the sheet shows its words whole")
    fun eachChoiceShowsWholeWithALargeFont() = extension.use {
        // Given a phone 360 dp wide, the font twice as large
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                BloxTheme(kind = BloxThemeKind.COLORFUL) {
                    ComposerScreen(
                        viewModel = viewModel,
                        onBack = {},
                        onDeleted = {},
                        modifier = Modifier.size(360.dp, 640.dp),
                    )
                }
            }
        }

        // When
        onNodeWithContentDescription("Au hasard").performClick()

        // Then no label nor sub-line is cut, « Mélancolique » neither
        CHOICES.forEach { (label, subLine) ->
            listOf(label, subLine).forEach { text ->
                val layout = textLayout(text)
                assertThat(layout.hasVisualOverflow).describedAs(text).isFalse()
                assertWordsWhole(layout)
            }
        }
    }

    @Test
    @DisplayName("the sheet open, a category chosen, comes back open with its choice when the screen is recreated")
    fun theSheetComesBackWhenTheScreenIsRecreated() = extension.use {
        // Given the sheet open, Calme chosen, the state of the screen saved as a rotation saves it
        val registry = SwappableRegistry()
        var shown by mutableStateOf(true)
        setContent {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                BloxTheme(kind = BloxThemeKind.COLORFUL) {
                    if (shown) ComposerScreen(viewModel = viewModel, onBack = {}, onDeleted = {})
                }
            }
        }
        onNodeWithContentDescription("Au hasard").performClick()
        choice("Calme").performScrollTo().performClick()
        val saved = runOnIdle { registry.performSave() }

        // When the screen is recreated, its state given back, on the same view model
        runOnIdle { shown = false }
        onNode(isDialog()).assertDoesNotExist()
        runOnIdle {
            registry.delegate = SaveableStateRegistry(saved) { true }
            shown = true
        }

        // Then the sheet is open again, Calme checked
        onNode(isDialog()).assertExists()
        choice("Calme").assertIsSelected()
    }

    @Test
    @DisplayName("two touches on Tirer une musique in one frame roll once, and Annuler brings back the music")
    fun twoTouchesOnTirerUneMusiqueRollOnce() = extension.use {
        // Given a note in the grid, and the sheet open
        showEditor()
        onNodeWithContentDescription("Mi bas, temps 1").performClick()
        onNodeWithContentDescription("Au hasard").performClick()
        val roll = checkNotNull(rollButton().fetchSemanticsNode().config[SemanticsActions.OnClick].action)
        mainClock.autoAdvance = false

        // When Tirer une musique is touched twice before the sheet goes, without a frame between
        runOnUiThread {
            roll()
            roll()
        }
        mainClock.advanceTimeBy(MESSAGE_SHOWN_MILLIS)

        // Then one roll only: Annuler brings back the note
        onNodeWithText("Annuler").performClick()
        mainClock.advanceTimeBy(MESSAGE_SHOWN_MILLIS)
        onNodeWithContentDescription("Mi bas, temps 1")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "note"))
    }

    private fun ComposeContext.showEditor() = showComposer(viewModel)

    /** *Tirer une musique*, in the sheet. */
    private fun ComposeContext.rollButton(): SemanticsNodeInteraction =
        onNode(hasText("Tirer une musique") and hasAnyAncestor(isDialog()))

    /** The choice [label] of the sheet. */
    private fun ComposeContext.choice(label: String): SemanticsNodeInteraction =
        onNode(isSelectable() and hasText(label) and hasAnyAncestor(isDialog()))

    private companion object {
        const val MESSAGE_SHOWN_MILLIS = 500L
        const val DICE_MESSAGE = "Musique tirée au hasard. « Annuler » ramène l'ancienne."

        /** The choices of the dice, in their order, each with its sub-line. */
        val CHOICES = listOf(
            "Surprise" to "L'une des sept, au hasard",
            "Joyeux" to "Vive et sautillante, en majeur",
            "Calme" to "Lente, avec beaucoup d'air",
            "Épique" to "Des notes fortes, au galop",
            "Mélancolique" to "De longues notes qui descendent",
            "Mystérieux" to "Des notes tenues, un peu étranges",
            "Rétro" to "Rapide, tout en arpèges",
            "Danse" to "Deux temps bien marqués",
        )
    }
}
