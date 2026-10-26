package ch.magori.blox.feature.home

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
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

    private val uiState = SettingsUiState(
        vibrations = false,
        soundEffects = false,
        keyVibration = false,
        effectsVolume = 100,
        musicVolume = 100,
        canVibrate = true,
    )

    /** What the page asked for, in order, each answer named by the action it calls. */
    private val asked = mutableListOf<String>()

    @Test
    @DisplayName("the back arrow, 48 dp, read Retour, goes back to the home screen")
    fun theBackArrowGoesBack() = extension.use {
        // Given
        showSettingsScreen()

        // When
        onNodeWithContentDescription("Retour")
            .assertWidthIsAtLeast(BloxSizes.minTouchTarget)
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
            .performClick()

        // Then
        runOnIdle { assertThat(asked).containsExactly("back") }
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
        SWITCHES.forEach { onNodeWithText(it).assertDoesNotExist() }
        onNodeWithText("Effacer").assertDoesNotExist()
    }

    @Test
    @DisplayName("TalkBack reads each setting as a switch, read off when off")
    fun talkBackReadsEachSettingAsASwitchReadOffWhenOff() = extension.use {
        // Given every switch off (the vibrations are off by default, the sound effects on)
        showSettingsScreen()

        // Then « Vibrations, interrupteur, éteint »
        SWITCHES.forEach { setting ->
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
            "Effets sonores" to "Rotation, lignes, fin de partie",
            "Vibrations" to "Lignes effacées, fin de partie",
            "Vibration des touches" to "Un léger toucher à chaque touche",
        ).forEach { (setting, subLine) ->
            onNode(isToggleable() and hasText(setting) and hasText(subLine)).assertExists()
        }
    }

    @Test
    @DisplayName("TalkBack reads a setting turned on as on, the other staying off")
    fun talkBackReadsASettingTurnedOnAsOn() = extension.use {
        // Given
        showSettingsScreen(uiState.copy(vibrations = false, soundEffects = true, keyVibration = true))

        // Then
        onNodeWithText("Vibrations").assertIsOff()
        onNodeWithText("Effets sonores").assertIsOn()
        onNodeWithText("Vibration des touches").assertIsOn()
    }

    @Test
    @DisplayName("tapping a setting off turns it on, tapping a setting on turns it off")
    fun tappingASettingSwitchesIt() = extension.use {
        // Given the vibrations and the key vibration off, the sound effects on
        showSettingsScreen(uiState.copy(vibrations = false, soundEffects = true, keyVibration = false))

        // When
        SWITCHES.forEach { onNodeWithText(it).performScrollTo().performClick() }

        // Then
        runOnIdle { assertThat(asked).containsExactly("sound effects false", "vibrations true", "key vibration true") }
    }

    @Test
    @DisplayName("each setting is a row as wide as the screen within its margins, 48 dp high at least")
    fun eachSettingIsAFullWidthRow() = extension.use {
        // Given a phone of 360 dp, with margins of 16 dp, and the 4 dp of the card around its rows
        showSettingsScreen(width = NARROW_PHONE)

        // Then
        SWITCHES.forEach { setting ->
            onNodeWithText(setting)
                .assertWidthIsEqualTo(NARROW_PHONE - 40.dp)
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
        // the pixel font of Original, 28 dp a letter, has 252 dp for its 280: it shrinks that little, never more;
        // elsewhere the label is as large as the heading of the section, in the same style, which never shrinks
        val normalSize = textLayout("Sons et vibrations").layoutInput.style.fontSize
        SWITCHES.forEach { setting ->
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

        // Then the sound effects and their volume, the music, then the vibrations, in the order of the design system
        val heading = onNodeWithText("Sons et vibrations")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .getUnclippedBoundsInRoot()
        val rows = listOf(
            onNodeWithText("Effets sonores"),
            volume("Volume des effets"),
            volume("Volume de la musique"),
            onNodeWithText("Vibrations"),
            onNodeWithText("Vibration des touches"),
        ).map { it.getUnclippedBoundsInRoot() }
        val note = onNodeWithText("Valable dès la partie suivante.").getUnclippedBoundsInRoot()
        assertThat(rows.first().top).isGreaterThanOrEqualTo(heading.bottom)
        rows.zipWithNext { above, below -> assertThat(below.top).isGreaterThanOrEqualTo(above.bottom) }
        assertThat(note.top).isGreaterThanOrEqualTo(rows.last().bottom)
    }

    @Test
    @DisplayName("without a vibrator both vibrations are disabled and say « Cet appareil ne vibre pas »")
    fun withoutAVibratorBothVibrationsAreDisabled() = extension.use {
        // Given a device without a vibrator, the vibrations saved on
        showSettingsScreen(uiState.copy(canVibrate = false, vibrations = true))

        // Then each says why it cannot be touched, instead of what it does
        listOf("Vibrations", "Vibration des touches").forEach { setting ->
            onNode(isToggleable() and hasText(setting) and hasText("Cet appareil ne vibre pas"))
                .assertIsNotEnabled()
        }
        onNodeWithText("Effets sonores").assertIsEnabled()
    }

    @Test
    @DisplayName("the volume of the effects is disabled while the sound effects are off")
    fun theVolumeOfTheEffectsIsDisabledWhileTheSoundEffectsAreOff() = extension.use {
        // Given the sound effects off
        showSettingsScreen(uiState.copy(soundEffects = false))

        // Then
        volume("Volume des effets").assertIsNotEnabled()
        volume("Volume de la musique").assertIsEnabled()
    }

    @Test
    @DisplayName("each volume is a slider of its name, whose steps hand the new percent over")
    fun eachVolumeIsASliderOfItsName() = extension.use {
        // Given the sound effects on, both volumes full
        showSettingsScreen(uiState.copy(soundEffects = true))

        // When TalkBack moves each one step down
        volume("Volume des effets").performSemanticsAction(SemanticsActions.SetProgress) { it(90f) }
        volume("Volume de la musique").performSemanticsAction(SemanticsActions.SetProgress) { it(90f) }

        // Then
        runOnIdle { assertThat(asked).containsExactly("effects volume 90", "music volume 90") }
    }

    @Test
    @DisplayName("Effacer, under its heading Record, beside Record du Classique, asks to clear the record")
    fun effacerAsksToClearTheRecord() = extension.use {
        // Given
        showSettingsScreen()

        // Then
        val heading = onNodeWithText("Record")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .getUnclippedBoundsInRoot()
        val label = onNodeWithText("Record du Classique").getUnclippedBoundsInRoot()
        assertThat(label.top).isGreaterThanOrEqualTo(heading.bottom)

        // When
        onNode(hasText("Effacer") and hasClickAction())
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
            .performScrollTo()
            .performClick()

        // Then nothing cleared yet: the page asks first
        runOnIdle { assertThat(asked).containsExactly("clear record") }
    }

    @Test
    @DisplayName("the window asks « Effacer le record ? », « Garder le record » first, each answer to its own action")
    fun theWindowAsksWhetherToClearTheRecord() = extension.use {
        // Given the page that asks
        showSettingsScreen(uiState.copy(recordClearing = RecordClearing.ASKED))

        // Then the safe answer first
        onNodeWithText("Effacer le record ?").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val keep = onNode(hasText("Garder le record") and hasClickAction())
        val clear = onNode(hasText("Effacer le record") and hasClickAction())
        val keepBounds = keep.getUnclippedBoundsInRoot()
        val clearBounds = clear.getUnclippedBoundsInRoot()
        assertThat(keepBounds.left < clearBounds.left || keepBounds.bottom <= clearBounds.top).isTrue()
        onNodeWithText("L'opération a échoué.").assertDoesNotExist()

        // When
        keep.performClick()
        clear.performClick()

        // Then
        runOnIdle { assertThat(asked).containsExactly("keep record", "clear record confirmed") }
    }

    @Test
    @DisplayName("once cleared, the page says « Record effacé », read by TalkBack as it shows, and the window is gone")
    fun onceClearedThePageSaysRecordEfface() = extension.use {
        // Given
        showSettingsScreen(uiState.copy(recordClearing = RecordClearing.CLEARED))

        // Then
        onNodeWithText("Record effacé")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        onNodeWithText("Effacer le record ?").assertDoesNotExist()
    }

    @Test
    @DisplayName("a clear that failed keeps the window open and says « L'opération a échoué. »")
    fun aClearThatFailedKeepsTheWindowOpen() = extension.use {
        // Given
        showSettingsScreen(uiState.copy(recordClearing = RecordClearing.FAILED))

        // Then
        onNodeWithText("Effacer le record ?").assertExists()
        onNodeWithText("L'opération a échoué.").assertExists()
        onNode(hasText("Effacer le record") and hasClickAction()).assertExists()
        onNodeWithText("Record effacé").assertDoesNotExist()
    }

    /** The slider of the volume [name]. */
    private fun ComposeContext.volume(name: String): SemanticsNodeInteraction = onNode(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo) and hasContentDescription(name),
    )

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
    ) {
        val actions = SettingsActions(
            onBack = { asked += "back" },
            onSoundEffectsChanged = { asked += "sound effects $it" },
            onEffectsVolumeChanged = { asked += "effects volume $it" },
            onMusicVolumeChanged = { asked += "music volume $it" },
            onVibrationsChanged = { asked += "vibrations $it" },
            onKeyVibrationChanged = { asked += "key vibration $it" },
            onClearRecord = { asked += "clear record" },
            onKeepRecord = { asked += "keep record" },
            onClearRecordConfirmed = { asked += "clear record confirmed" },
        )
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = kind) {
                    SettingsScreen(modifier = Modifier.width(width), uiState = state, actions = actions)
                }
            }
        }
    }
}

/** The switches of the page, in its order. */
private val SWITCHES = listOf("Effets sonores", "Vibrations", "Vibration des touches")

/** A phone of 360 dp wide, as on the mockup: the narrowest of those BloX is drawn for. */
private val NARROW_PHONE = 360.dp

/** The least part of its size a label of a setting keeps in a large font, shrinking only so as not to cut a word. */
private const val LEAST_PART_OF_THE_SIZE = 0.9f
