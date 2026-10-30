package ch.magori.blox.feature.settings

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.text.assertWordsWhole
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxTypographies
import ch.magori.blox.feature.settings.navigation.SettingsSection
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class SettingsScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val uiState = SettingsUiState(
        reducedEffects = false,
        vibrations = false,
        soundEffects = false,
        keyVibration = false,
        effectsVolume = 100,
        musicVolume = 100,
        canVibrate = true,
    )

    /** What the page asked for, in order, each answer named by the action it calls. */
    private val asked = mutableListOf<String>()

    /** What the page shows; a test may change it once the page is shown. */
    private var shown by mutableStateOf<SettingsUiState?>(null)

    @Test
    @DisplayName("the back arrow, 48 dp, read Retour, goes back")
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
    @DisplayName("the page is drawn in Colorful whatever the theme chosen, with the effects of the player")
    fun thePageIsDrawnInColorfulWhateverTheThemeChosen() = extension.use {
        // Given the Original theme chosen for the Classic, Android imposing the reduced effects
        setContent {
            BloxTheme(kind = BloxThemeKind.ORIGINAL, effects = BloxEffects(reduced = true, forcedBySystem = true)) {
                SettingsScreen(uiState = uiState, section = SettingsSection.CLASSIC, actions = SettingsActions.None)
            }
        }

        // Then the title is in the font of the Colorful theme, and the effects are those of the player
        assertThat(textLayout("Paramètres").layoutInput.style.fontFamily)
            .isEqualTo(BloxTypographies.Colorful.screenTitle.fontFamily)
        onNodeWithText("Imposé par les réglages d'Android").assertExists()
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
    @DisplayName(
        "each setting says under its label what it does, and for which games when not all, read with it",
    )
    fun eachSettingSaysWhatItDoes() = extension.use {
        // Given
        showSettingsScreen()

        // Then
        mapOf(
            "Gaucher" to "$GAMES_WITH_KEYS : flèches à gauche, B et A à droite",
            "Effets sonores" to "Rotation, lignes, fin de partie",
            "Vibrations" to "$GAMES_WITH_KEYS : lignes effacées, fin de partie",
            "Vibration des touches" to "$GAMES_WITH_KEYS : un léger toucher à chaque touche",
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
        // Given the full effects, the vibrations and the key vibration off, the sound effects on
        showSettingsScreen(
            uiState.copy(reducedEffects = false, vibrations = false, soundEffects = true, keyVibration = false),
        )

        // When
        SWITCHES.forEach { onNodeWithText(it).performScrollTo().performClick() }

        // Then
        runOnIdle {
            assertThat(asked).containsExactly(
                "reduced effects true",
                "left handed true",
                "sound effects false",
                "vibrations true",
                "key vibration true",
            )
        }
    }

    @Test
    @DisplayName(
        "Affichage holds Effets réduits, which says what it does, then Gaucher, and no note, above Sons et vibrations",
    )
    fun theCardAffichageHoldsReducedEffectsThenGaucher() = extension.use {
        // Given
        showSettingsScreen()

        // Then the theme, chosen on the home of the Classic, is no longer told among the settings of all games
        val heading = onNodeWithText("Affichage")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .getUnclippedBoundsInRoot()
        val row = onNode(
            isToggleable() and hasText("Effets réduits") and hasText("Moins de mouvements et de particules"),
        )
            .assertIsOff()
            .assertIsEnabled()
            .getUnclippedBoundsInRoot()
        val gaucher = onNode(isToggleable() and hasText("Gaucher")).getUnclippedBoundsInRoot()
        val sounds = onNodeWithText("Sons et vibrations").getUnclippedBoundsInRoot()
        assertThat(row.top).isGreaterThanOrEqualTo(heading.bottom)
        assertThat(gaucher.top).isGreaterThanOrEqualTo(row.bottom)
        assertThat(sounds.top).isGreaterThanOrEqualTo(gaucher.bottom)

        // And no text between the card and the next heading: Affichage has no note
        val card = onNodeWithTag(DISPLAY_CARD_TAG).fetchSemanticsNode().boundsInRoot
        val nextHeading = onNodeWithText("Sons et vibrations").fetchSemanticsNode().boundsInRoot
        val between = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .filter { it.boundsInRoot.top >= card.bottom && it.boundsInRoot.bottom <= nextHeading.top }
            .map { it.config[SemanticsProperties.Text] }
        assertThat(between).isEmpty()
    }

    @ParameterizedTest(name = "left handed: {0}")
    @ValueSource(booleans = [false, true])
    @DisplayName(
        "Gaucher stands under Effets réduits, and says for which games and where the keys stand, for either hand",
    )
    fun gaucherSaysWhereTheKeysAre(leftHanded: Boolean) = extension.use {
        // Given a right handed player, or a left handed one
        showSettingsScreen(uiState.copy(leftHanded = leftHanded))

        // Then one switch, on for the left handed, whose sub-line, read with it, names the games that have keys and
        // says where their keys stand in the next game
        val subLine = if (leftHanded) {
            "$GAMES_WITH_KEYS : B et A à gauche, flèches à droite"
        } else {
            "$GAMES_WITH_KEYS : flèches à gauche, B et A à droite"
        }
        val reducedEffects = onNode(isToggleable() and hasText("Effets réduits")).getUnclippedBoundsInRoot()
        val gaucher = onNode(isToggleable() and hasText("Gaucher") and hasText(subLine))
            .assert(if (leftHanded) isOn() else isOff())
            .assertIsEnabled()
            .getUnclippedBoundsInRoot()
        assertThat(gaucher.top).isGreaterThanOrEqualTo(reducedEffects.bottom)
    }

    @Test
    @DisplayName("a forced switch reads « Imposé par les réglages d'Android » and cannot be turned off")
    fun aForcedSwitchSaysWhyAndCannotBeTurnedOff() = extension.use {
        // Given full effects chosen, but Android removing the animations
        showSettingsScreen(
            uiState.copy(reducedEffects = false),
            effects = BloxEffects(reduced = true, forcedBySystem = true),
        )

        // When
        val row = onNode(isToggleable() and hasText("Effets réduits") and hasText("Imposé par les réglages d'Android"))
        row.performScrollTo().performClick()

        // Then it shows on, as Android imposes it, and nothing is asked: the choice of the player stays as it was
        row.assertIsOn().assertIsNotEnabled()
        runOnIdle { assertThat(asked).isEmpty() }
    }

    @Test
    @DisplayName("Effets réduits shows the choice of the player while Android animates")
    fun reducedEffectsShowsTheChoiceWhileAndroidAnimates() = extension.use {
        // Given reduced effects chosen, Android animating
        showSettingsScreen(
            uiState.copy(reducedEffects = true),
            effects = BloxEffects(reduced = true, forcedBySystem = false),
        )

        // Then
        onNodeWithText("Effets réduits").assertIsOn().assertIsEnabled()
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

    @Test
    @DisplayName(
        "in a font twice as large, on 360 dp, a setting label keeps its words whole and 90 % of its size at least",
    )
    fun inALargeFontEachSettingLabelKeepsItsWordsWholeAndMostOfItsSize() = extension.use {
        // Given a font twice as large, on the narrowest phone
        showSettingsScreen(width = NARROW_PHONE, fontScale = 2f)

        // Then no word is cut from one line to the next (« Vibratio / ns »), and a label shrinks, if ever, only a
        // little: it is as large as the heading of the section, in the same style, which never shrinks
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

    @ParameterizedTest(name = "effects forced by Android {0}")
    @ValueSource(booleans = [false, true])
    @DisplayName("in a font twice as large, on 360 dp, every sub-line shows whole, its words whole")
    fun inALargeFontEverySubLineShowsWhole(forced: Boolean) = extension.use {
        // Given a font twice as large, on the narrowest phone, Android imposing the reduced effects or not
        showSettingsScreen(
            width = NARROW_PHONE,
            fontScale = 2f,
            effects = if (forced) BloxEffects(reduced = true, forcedBySystem = true) else BloxEffects.Full,
        )

        // Then nothing is cut, not even « Moins de mouvements et de particules »
        val reducedEffects = if (forced) "Imposé par les réglages d'Android" else "Moins de mouvements et de particules"
        (listOf(reducedEffects) + SUB_LINES).forEach { subLine ->
            val layout = textLayout(subLine)
            assertWordsWhole(layout)
            assertThat(layout.hasVisualOverflow).describedAs(subLine).isFalse()
        }
    }

    @Test
    @DisplayName(
        "Sons et vibrations holds its settings, then says the volumes hold at once, the rest from the next game",
    )
    fun theSettingsStandUnderTheirHeadingThenTheNoteOnWhenTheyHold() = extension.use {
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
        val note = onNodeWithText("Volumes : tout de suite. Le reste : dès le prochain jeu ouvert.")
            .getUnclippedBoundsInRoot()
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
    @DisplayName(
        "Effacer, beside Record du Classique, read once as « Effacer le record du Classique », asks to clear it",
    )
    fun effacerAsksToClearTheRecord() = extension.use {
        // Given a phone of 360 dp, in a normal font
        showSettingsScreen(width = NARROW_PHONE)

        // Then the label above, beside the button, which says it all: TalkBack reads the row once
        val heading = onNodeWithText("Record")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .getUnclippedBoundsInRoot()
        val labelNode = onNode(hasText("Record du Classique"), useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility))
        val label = labelNode.getUnclippedBoundsInRoot()
        val effacer = onNode(hasContentDescription("Effacer le record du Classique") and hasClickAction())
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        val effacerBounds = effacer.getUnclippedBoundsInRoot()
        assertThat(label.top).isGreaterThanOrEqualTo(heading.bottom)
        assertThat(effacerBounds.left).isGreaterThanOrEqualTo(label.right)
        assertThat(effacerBounds.top).isLessThan(label.bottom)

        // When
        effacer.performScrollTo().performClick()

        // Then nothing cleared yet: the page asks first
        runOnIdle { assertThat(asked).containsExactly("clear record") }
    }

    @Test
    @DisplayName(
        "the page opened from the choice of the game shows no record, nor where the theme of the Classic is chosen",
    )
    fun thePageOpenedFromTheChoiceOfTheGameShowsNoRecord() = extension.use {
        // Given the page opened without the section of a game
        showSettingsScreen(section = null)

        // Then the settings of all games, under their heading, and nothing of the Classic
        onNodeWithText("Tous les jeux").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithText("Sons et vibrations").assertExists()
        onNodeWithText("Réglages du Classique").assertDoesNotExist()
        onNodeWithText("Record").assertDoesNotExist()
        onNode(hasText("Record du Classique"), useUnmergedTree = true).assertDoesNotExist()
        onNode(hasContentDescription("Effacer le record du Classique")).assertDoesNotExist()
        onNodeWithText("Le thème du Classique se choisit dans son accueil.").assertDoesNotExist()
    }

    @Test
    @DisplayName("the title of a part, Tous les jeux, is larger than the title of a section, Affichage")
    fun theTitleOfAPartIsLargerThanTheTitleOfASection() = extension.use {
        // Given
        showSettingsScreen(section = null)

        // Then
        val part = textLayout("Tous les jeux").layoutInput.style.fontSize
        val section = textLayout("Affichage").layoutInput.style.fontSize
        assertThat(part.value).isGreaterThan(section.value)
    }

    @Test
    @DisplayName("the section of the Classic is read Réglages du Classique, a heading")
    fun theSectionOfTheClassicIsReadReglagesDuClassiqueAHeading() = extension.use {
        // Given the page opened from the Classic
        showSettingsScreen(section = SettingsSection.CLASSIC)

        // Then
        onNodeWithText("Réglages du Classique").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    @DisplayName("opened from the Classic, its section comes first, then Tous les jeux, a heading, above Affichage")
    fun openedFromTheClassicThePageShowsItsSectionFirstThenTousLesJeux() = extension.use {
        // Given the page opened from the Classic
        showSettingsScreen(section = SettingsSection.CLASSIC)

        // Then, from the top: the title of the page, the section of the Classic, then the settings of all games
        val tops = listOf(
            onNodeWithText("Paramètres"),
            onNodeWithText("Réglages du Classique"),
            onNode(hasContentDescription("Effacer le record du Classique") and hasClickAction()),
            onNodeWithText("Tous les jeux").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)),
            onNodeWithText("Affichage"),
            onNodeWithText("Sons et vibrations"),
        ).map { it.getUnclippedBoundsInRoot().top }
        assertThat(tops).isSorted()
    }

    @Test
    @DisplayName(
        "opened from the Classic, its section says under the record where its theme is chosen, above Tous les jeux",
    )
    fun openedFromTheClassicItsSectionSaysWhereItsThemeIsChosen() = extension.use {
        // Given the page opened from the Classic
        showSettingsScreen(section = SettingsSection.CLASSIC)

        // Then the note, last of the section of the Classic, as close under the card Record as the note of Sons et
        // vibrations under its card: it belongs to the Classic, not to Tous les jeux
        val record = onNodeWithTag(RECORD_CARD_TAG).getUnclippedBoundsInRoot()
        val note = onNodeWithText("Le thème du Classique se choisit dans son accueil.").getUnclippedBoundsInRoot()
        val allGames = onNodeWithText("Tous les jeux").getUnclippedBoundsInRoot()
        assertThat((note.top - record.bottom).value).isCloseTo(BloxSpaces.space2.value, within(HALF_A_DP))
        assertThat(allGames.top).isGreaterThanOrEqualTo(note.bottom)
    }

    @Test
    @DisplayName("in a font twice as large, on 360 dp, Effacer goes under Record du Classique, its words whole")
    fun inALargeFontEffacerGoesUnderItsLabel() = extension.use {
        // Given
        showSettingsScreen(width = NARROW_PHONE, fontScale = 2f)

        // Then
        val label = onNode(hasText("Record du Classique"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val effacer = onNode(hasContentDescription("Effacer le record du Classique") and hasClickAction())
            .getUnclippedBoundsInRoot()
        assertThat(effacer.top).isGreaterThanOrEqualTo(label.bottom)
        assertWordsWhole(textLayout("Record du Classique"))
    }

    @Test
    @DisplayName("while the clear is written, the window stays open and both its answers are disabled")
    fun whileTheClearIsWrittenTheWindowWaits() = extension.use {
        // Given
        showSettingsScreen(uiState.copy(recordClearing = RecordClearing.CLEARING))

        // Then
        onNodeWithText("Effacer le record ?").assertExists()
        onNode(hasText("Garder le record") and hasClickAction()).assertIsNotEnabled()
        onNode(hasText("Effacer le record") and hasClickAction()).assertIsNotEnabled()
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
    @DisplayName(
        "once cleared, the page says « Record effacé » in a live region there from the start, and the window is gone",
    )
    fun onceClearedThePageSaysRecordEfface() = extension.use {
        // Given the window that asks: the live region is there, silent
        showSettingsScreen(uiState.copy(recordClearing = RecordClearing.CLEARING))
        val live = onNode(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        live.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))

        // When the record is cleared
        shown = uiState.copy(recordClearing = RecordClearing.CLEARED)

        // Then the same region says it, so that TalkBack announces a change, not a node that the focus coming back
        // from the window would cover
        live.assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString("Record effacé"))))
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
        section: SettingsSection? = SettingsSection.CLASSIC,
        width: Dp = Dp.Unspecified,
        fontScale: Float = 1f,
        effects: BloxEffects = BloxEffects.Full,
    ) {
        val actions = SettingsActions(
            onBack = { asked += "back" },
            onReducedEffectsChanged = { asked += "reduced effects $it" },
            onLeftHandedChanged = { asked += "left handed $it" },
            onSoundEffectsChanged = { asked += "sound effects $it" },
            onEffectsVolumeChanged = { asked += "effects volume $it" },
            onMusicVolumeChanged = { asked += "music volume $it" },
            onVibrationsChanged = { asked += "vibrations $it" },
            onKeyVibrationChanged = { asked += "key vibration $it" },
            onClearRecord = { asked += "clear record" },
            onKeepRecord = { asked += "keep record" },
            onClearRecordConfirmed = { asked += "clear record confirmed" },
        )
        shown = state
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(effects = effects) {
                    SettingsScreen(
                        modifier = Modifier.width(width),
                        uiState = shown,
                        section = section,
                        actions = actions,
                    )
                }
            }
        }
    }
}

/** The switches of the page, in its order. */
private val SWITCHES = listOf("Effets réduits", "Gaucher", "Effets sonores", "Vibrations", "Vibration des touches")

/** A phone of 360 dp wide, as on the mockup: the narrowest of those BloX is drawn for. */
private val NARROW_PHONE = 360.dp

/** The games that have keys, which Gaucher and both vibrations name: not all games (ADR 0030). */
private const val GAMES_WITH_KEYS = "Classique, Graine du jour"

/** The least part of its size a label of a setting keeps in a large font, shrinking only so as not to cut a word. */
private const val LEAST_PART_OF_THE_SIZE = 0.9f

/** What a gap measured in the root may differ from its spacing, rounded to whole pixels. */
private const val HALF_A_DP = 0.5f

/** The sub-lines of the switches after Effets réduits, in the order of the page. */
private val SUB_LINES = listOf(
    "$GAMES_WITH_KEYS : flèches à gauche, B et A à droite",
    "Rotation, lignes, fin de partie",
    "$GAMES_WITH_KEYS : lignes effacées, fin de partie",
    "$GAMES_WITH_KEYS : un léger toucher à chaque touche",
)
