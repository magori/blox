package ch.magori.blox.feature.home

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.text.assertWordsWhole
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class SettingsScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val uiState = SettingsUiState(vibrations = false, soundEffects = false)

    @Test
    @DisplayName("the back arrow, 48 dp, read Retour, goes back to the home screen")
    fun theBackArrowGoesBack() = extension.use {
        // Given
        var back = 0
        showSettingsScreen(onBack = { back++ })

        // When
        onNodeWithContentDescription("Retour")
            .assertWidthIsAtLeast(BloxSizes.minTouchTarget)
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
            .performClick()

        // Then
        runOnIdle { assertThat(back).isEqualTo(1) }
    }

    @Test
    @DisplayName("the title Paramètres is a heading, beside the back arrow")
    fun theTitleIsAHeadingBesideTheBackArrow() = extension.use {
        // Given
        showSettingsScreen()

        // Then
        val title = onNodeWithText("Paramètres")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .getUnclippedBoundsInRoot()
        val back = onNodeWithContentDescription("Retour").getUnclippedBoundsInRoot()
        assertThat(title.left).isGreaterThanOrEqualTo(back.right)
        assertThat(title.top).isLessThan(back.bottom)
    }

    @Test
    @DisplayName("while the saved settings are not read, the page shows its back arrow and its title, and no switch")
    fun whileTheSettingsAreNotReadThePageShowsItsHeaderAndNoSwitch() = extension.use {
        // Given settings not read yet
        showSettingsScreen(state = null)

        // Then
        onNodeWithContentDescription("Retour").assertHasClickAction()
        onNodeWithText("Paramètres").assertExists()
        onNodeWithText("Vibrations").assertDoesNotExist()
        onNodeWithText("Effets sonores").assertDoesNotExist()
    }

    @Test
    @DisplayName("TalkBack reads Vibrations and Effets sonores as switches, read off when off")
    fun talkBackReadsTheSettingsAsSwitchesReadOffWhenOff() = extension.use {
        // Given both switches off (the vibrations are off by default, the sound effects on)
        showSettingsScreen()

        // Then « Vibrations, interrupteur, désactivé »
        listOf("Vibrations", "Effets sonores").forEach { setting ->
            onNodeWithText(setting)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
                .assertIsToggleable()
                .assertIsOff()
                .assertHasClickAction()
        }
    }

    @Test
    @DisplayName("each setting says what it does under its label, read with it by TalkBack")
    fun eachSettingSaysWhatItDoes() = extension.use {
        // Given
        showSettingsScreen()

        // Then
        mapOf(
            "Vibrations" to "Lignes effacées, fin de partie",
            "Effets sonores" to "Rotation, lignes, fin de partie",
        ).forEach { (setting, subLine) ->
            onNode(isToggleable() and hasText(setting) and hasText(subLine)).assertExists()
        }
    }

    @Test
    @DisplayName("TalkBack reads a setting turned on as on, the other staying off")
    fun talkBackReadsASettingTurnedOnAsOn() = extension.use {
        // Given
        showSettingsScreen(uiState.copy(vibrations = false, soundEffects = true))

        // Then
        onNodeWithText("Vibrations").assertIsOff()
        onNodeWithText("Effets sonores").assertIsOn()
    }

    @Test
    @DisplayName("tapping a setting off turns it on, tapping a setting on turns it off")
    fun tappingASettingSwitchesIt() = extension.use {
        // Given the vibrations off, the sound effects on
        val vibrations = mutableListOf<Boolean>()
        val soundEffects = mutableListOf<Boolean>()
        showSettingsScreen(
            uiState.copy(vibrations = false, soundEffects = true),
            onVibrationsChanged = { vibrations += it },
            onSoundEffectsChanged = { soundEffects += it },
        )

        // When
        onNodeWithText("Vibrations").performClick()
        onNodeWithText("Effets sonores").performClick()

        // Then
        runOnIdle {
            assertThat(vibrations).containsExactly(true)
            assertThat(soundEffects).containsExactly(false)
        }
    }

    @Test
    @DisplayName("each setting is a row as wide as the screen within its margins, 48 dp high at least")
    fun eachSettingIsAFullWidthRow() = extension.use {
        // Given a phone of 360 dp, with margins of 16 dp, as « Mes musiques »
        showSettingsScreen(width = NARROW_PHONE)

        // Then
        listOf("Vibrations", "Effets sonores").forEach { setting ->
            onNodeWithText(setting)
                .assertWidthIsEqualTo(NARROW_PHONE - 32.dp)
                .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        }
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName(
        "in a font twice as large, on 360 dp, a setting label keeps its words whole and 90 % of its size at least",
    )
    fun inALargeFontEachSettingLabelKeepsItsWordsWholeAndMostOfItsSize(kind: BloxThemeKind) = extension.use {
        // Given a font twice as large, on the narrowest phone
        showSettingsScreen(width = NARROW_PHONE, fontScale = 2f, kind = kind)

        // Then no word is cut from one line to the next (« Vibratio / ns »). Beside its pictogram, « Vibrations » in
        // the pixel font of Original, 28 dp a letter, has 260 dp for its 280: it shrinks that little, never more;
        // elsewhere the label is as large as the heading of the section, in the same style, which never shrinks
        val normalSize = textLayout("Sons et vibrations").layoutInput.style.fontSize
        listOf("Vibrations", "Effets sonores").forEach { setting ->
            val label = textLayout(setting)
            assertWordsWhole(label)
            assertThat(label.hasVisualOverflow).isFalse()
            assertThat(label.layoutInput.style.fontSize.value)
                .isGreaterThanOrEqualTo(normalSize.value * LEAST_PART_OF_THE_SIZE)
            onNodeWithText(setting).assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        }
    }

    @Test
    @DisplayName(
        "the settings stand under their heading Sons et vibrations, and Valable dès la partie suivante under them",
    )
    fun theSettingsStandUnderTheirHeadingAndTheNoteUnderThem() = extension.use {
        // Given
        showSettingsScreen()

        // Then
        val heading = onNodeWithText("Sons et vibrations")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .getUnclippedBoundsInRoot()
        val vibrations = onNodeWithText("Vibrations").getUnclippedBoundsInRoot()
        val soundEffects = onNodeWithText("Effets sonores").getUnclippedBoundsInRoot()
        val note = onNodeWithText("Valable dès la partie suivante.").getUnclippedBoundsInRoot()
        assertThat(vibrations.top).isGreaterThanOrEqualTo(heading.bottom)
        assertThat(soundEffects.top).isGreaterThanOrEqualTo(vibrations.bottom)
        assertThat(note.top).isGreaterThanOrEqualTo(soundEffects.bottom)
    }

    /** The text shown as [text], as laid out. */
    private fun ComposeContext.textLayout(text: String): TextLayoutResult {
        val label = onNode(hasText(text), useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        label.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single()
    }

    private fun ComposeContext.showSettingsScreen(
        state: SettingsUiState? = uiState,
        width: Dp = Dp.Unspecified,
        fontScale: Float = 1f,
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
        onBack: () -> Unit = {},
        onVibrationsChanged: (Boolean) -> Unit = {},
        onSoundEffectsChanged: (Boolean) -> Unit = {},
    ) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = kind) {
                    SettingsScreen(
                        modifier = Modifier.width(width),
                        uiState = state,
                        onBack = onBack,
                        onVibrationsChanged = onVibrationsChanged,
                        onSoundEffectsChanged = onSoundEffectsChanged,
                    )
                }
            }
        }
    }
}

/** A phone of 360 dp wide, as on the mockup: the narrowest of those BloX is drawn for. */
private val NARROW_PHONE = 360.dp

/** The least part of its size a label of a setting keeps in a large font, shrinking only so as not to cut a word. */
private const val LEAST_PART_OF_THE_SIZE = 0.9f
