package ch.magori.blox.core.designsystem.component

import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.designsystem.state.SwappableRegistry
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class BloxSheetTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("a sheet is read as a dialog by TalkBack, its title first")
    fun aSheetIsReadAsADialogItsTitleFirst() = extension.use {
        // Given
        showSheet()

        // Then a dialog, whose first words are its title, a heading, before what it holds
        val dialog = onNode(isDialog()).fetchSemanticsNode()
        assertThat(readOrder(dialog)).startsWith(TITLE, FIRST_CHOICE)
        onNodeWithText(TITLE).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    @DisplayName("the system back and a touch on the scrim close the sheet")
    fun theSystemBackAndATouchOnTheScrimCloseTheSheet() = extension.use {
        // Given a sheet that its screen closes when asked
        var closings = 0
        var open by mutableStateOf(true)
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                if (open) {
                    BloxSheet(title = TITLE, onDismissRequest = {
                        closings++
                        open = false
                    }) { WindowText(FIRST_CHOICE) }
                }
            }
        }

        // When the system goes back
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

        // Then
        runOnIdle { assertThat(closings).isEqualTo(1) }
        onNode(isDialog()).assertDoesNotExist()

        // When it opens again, and the scrim is touched, above the sheet
        runOnIdle { open = true }
        onNode(isDialog()).performTouchInput { click(Offset(centerX, SCRIM_TAP.toPx())) }

        // Then
        runOnIdle { assertThat(closings).isEqualTo(2) }
        onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    @DisplayName("a sheet asks once to close, sent back then touched on its scrim twice")
    fun aSheetAsksOnceToClose() = extension.use {
        // Given a sheet whose screen takes its time to close it
        var closings = 0
        showSheet(onDismissRequest = { closings++ })

        // When the system goes back, then the scrim is touched twice
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        repeat(2) { onNode(isDialog()).performTouchInput { click(Offset(centerX, SCRIM_TAP.toPx())) } }

        // Then
        runOnIdle { assertThat(closings).isEqualTo(1) }
    }

    @Test
    @DisplayName("a sheet lifted by a bottom inset keeps its last row of actions in sight, its content scrolling")
    fun aSheetLiftedByABottomInsetKeepsItsActionsInSight() = extension.use {
        // Given a sheet of many lines and its action, over a keyboard of 320 dp
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                BloxSheet(
                    title = TITLE,
                    onDismissRequest = {},
                    insets = WindowInsets(bottom = KEYBOARD),
                    actions = { WindowButton(text = ACTION, onClick = {}) },
                ) {
                    repeat(LINES) { WindowText("Ligne $it") }
                }
            }
        }

        // Then the action stands whole above the keyboard, and the lines scroll
        val screen = onNode(isDialog()).getUnclippedBoundsInRoot()
        val action = onNode(hasText(ACTION) and hasClickAction()).getUnclippedBoundsInRoot()
        assertThat(action.bottom).isLessThanOrEqualTo(screen.bottom - KEYBOARD)
        assertThat(action.top).isGreaterThanOrEqualTo(screen.top)
        onNode(hasScrollAction() and hasAnyDescendant(hasText("Ligne 0"))).assertExists()
        onNodeWithText("Ligne ${LINES - 1}").assertIsNotDisplayed()
    }

    @Test
    @DisplayName(
        "on 360 by 640 dp, in a font twice as large, over a keyboard of 300 dp, a sheet keeps its last answer whole " +
            "above the keyboard, its title scrolling with its content",
    )
    fun inALargeFontTheLastAnswerStaysAboveTheKeyboard() = extension.use {
        // Given the sheet Enregistrer, its title on two lines, its answers one above the other, without its window
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                BloxTheme(kind = BloxThemeKind.COLORFUL) {
                    Box(Modifier.size(PHONE_WIDTH, PHONE_HEIGHT).testTag(PHONE)) {
                        SheetFrame(
                            title = SAVE_TITLE,
                            onDismissRequest = {},
                            insets = WindowInsets(bottom = SMALL_KEYBOARD),
                            entrance = { SheetEntrance.Settled },
                            actions = {
                                WindowButton(text = CANCEL, onClick = {}, modifier = Modifier.fillMaxWidth())
                                WindowButton(
                                    text = SAVE,
                                    onClick = {},
                                    kind = WindowButtonKind.PRIMARY,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                        ) {
                            repeat(LINES) { WindowText("Ligne $it") }
                        }
                    }
                }
            }
        }

        // Then the last answer stands whole above the keyboard, and the title scrolls with the lines
        val phone = onNode(hasTestTag(PHONE)).getUnclippedBoundsInRoot()
        val save = onNode(hasText(SAVE) and hasClickAction()).getUnclippedBoundsInRoot()
        assertThat(save.bottom).isLessThanOrEqualTo(phone.bottom - SMALL_KEYBOARD)
        assertThat(save.top).isGreaterThanOrEqualTo(phone.top)
        onNode(hasScrollAction() and hasAnyDescendant(hasText(SAVE_TITLE))).assertExists()
    }

    @Test
    @DisplayName(
        "the window of a sheet neither dims the screen nor plays an animation of the platform, from its first frame",
    )
    fun theWindowOfASheetNeitherDimsNorAnimates() = extension.use {
        // Given the attributes of the window of a sheet, read as its content is first composed, once it is shown
        var attributes: WindowManager.LayoutParams? = null
        showSheet {
            val window = (LocalView.current.parent as DialogWindowProvider).window
            if (attributes == null) attributes = WindowManager.LayoutParams().apply { copyFrom(window.attributes) }
            WindowText(FIRST_CHOICE)
        }

        // Then
        runOnIdle {
            val shown = checkNotNull(attributes)
            assertThat(shown.windowAnimations).isZero()
            assertThat(shown.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND).isZero()
        }
    }

    @Test
    @DisplayName("a sheet that had come in stands in place at once when its screen is recreated")
    fun aSheetThatHadComeInStandsInPlaceWhenRecreated() = extension.use {
        // Given a sheet come in, on a clock that only moves when told, its state saved as a rotation saves it
        val registry = SwappableRegistry()
        var open by mutableStateOf(true)
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                BloxTheme(kind = BloxThemeKind.COLORFUL) {
                    if (open) BloxSheet(title = TITLE, onDismissRequest = {}) { WindowText(FIRST_CHOICE) }
                }
            }
        }
        mainClock.advanceTimeBy(SETTLE_MS)
        val settled = onNodeWithText(TITLE).getUnclippedBoundsInRoot().top
        val saved = runOnIdle { registry.performSave() }

        // When the screen is recreated, its state given back
        runOnIdle { open = false }
        mainClock.advanceTimeByFrame()
        runOnIdle {
            registry.delegate = SaveableStateRegistry(saved) { true }
            open = true
        }
        mainClock.advanceTimeByFrame()

        // Then, before another frame, the sheet stands where it stood
        val restored = onNodeWithText(TITLE).getUnclippedBoundsInRoot().top
        assertThat(restored.value).isCloseTo(settled.value, within(0.5f))
    }

    /** The texts and descriptions under [node], in the order of its tree, the one TalkBack reads them in. */
    private fun readOrder(node: SemanticsNode): List<String> {
        val own = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        return own + node.children.flatMap { readOrder(it) }
    }

    private fun ComposeContext.showSheet(
        onDismissRequest: () -> Unit = {},
        content: @Composable ColumnScope.() -> Unit = { WindowText(FIRST_CHOICE) },
    ) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                BloxSheet(title = TITLE, onDismissRequest = onDismissRequest, content = content)
            }
        }
    }

    /** The saved state of a screen, which a recreation swaps for one given back from what was saved. */
    private companion object {
        const val TITLE = "Au hasard"
        const val FIRST_CHOICE = "Surprise"

        /** How far from the top of the screen the scrim is touched: far above the sheet. */
        val SCRIM_TAP = 120.dp

        /** A keyboard, high enough to leave little room to the sheet. */
        val KEYBOARD = 320.dp
        const val ACTION = "Tirer une musique"
        const val LINES = 30

        /** A phone of 360 by 640 dp, under a keyboard of 300 dp, and the sheet Enregistrer. */
        const val PHONE = "phone"
        val PHONE_WIDTH = 360.dp
        val PHONE_HEIGHT = 640.dp
        val SMALL_KEYBOARD = 300.dp
        const val SAVE_TITLE = "Enregistrer la musique"
        const val CANCEL = "Annuler"
        const val SAVE = "Enregistrer"

        /** Longer than motion card, 300 ms. */
        const val SETTLE_MS = 1_000L
    }
}
