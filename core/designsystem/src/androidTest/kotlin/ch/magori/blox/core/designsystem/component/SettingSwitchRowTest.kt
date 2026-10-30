package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxSizes
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
// In Colorful alone: a setting row is drawn only on the settings page, in Colorful whatever the theme chosen (P12.13).
internal class SettingSwitchRowTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("TalkBack reads one switch per row, its label, its sub-line and its state")
    fun talkBackReadsOneSwitchPerRow() = extension.use {
        // Given one row on, one off
        showRows()

        // Then « Effets sonores, Rotation, lignes, fin de partie, interrupteur, activé »
        onAllNodes(isToggleable()).assertCountEquals(2)
        onNode(isToggleable() and hasText(SOUNDS) and hasText(SOUNDS_SUB_LINE))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOn()
        onNode(isToggleable() and hasText(VIBRATIONS) and hasText(VIBRATIONS_SUB_LINE))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOff()
        // The pictogram adds nothing to read
        onNodeWithText(SOUNDS).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    @DisplayName("a disabled row reads as unavailable and its sub-line says why")
    fun aDisabledRowReadsAsUnavailableAndSaysWhy() = extension.use {
        // Given a device without a vibrator
        val changes = mutableListOf<Boolean>()
        showRows(vibrationsDisabledReason = NO_VIBRATOR, onVibrationsChange = { changes += it })

        // When
        onNodeWithText(VIBRATIONS).performClick()

        // Then « Vibrations, Cet appareil ne vibre pas, interrupteur, désactivé, indisponible »
        onNode(isToggleable() and hasText(VIBRATIONS) and hasText(NO_VIBRATOR))
            .assertIsNotEnabled()
            .assertIsOff()
        onNodeWithText(VIBRATIONS_SUB_LINE).assertDoesNotExist()
        runOnIdle { assertThat(changes).isEmpty() }
    }

    @Test
    @DisplayName("TalkBack reads Allumé or Éteint as the state of a row, apart from its unavailability")
    fun talkBackReadsAlluméOrÉteintApartFromUnavailability() = extension.use {
        // Given the sound effects on, the vibrations off and disabled: in French, TalkBack would read « désactivé »
        // both for a switch off and for a row that cannot be touched
        showRows(vibrationsDisabledReason = NO_VIBRATOR)

        // Then « Effets sonores, …, interrupteur, allumé » and « Vibrations, …, interrupteur, éteint, désactivé »
        onNode(isToggleable() and hasText(SOUNDS))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Allumé"))
        onNode(isToggleable() and hasText(VIBRATIONS))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Éteint"))
            .assertIsNotEnabled()
    }

    @Test
    @DisplayName(
        "in a font twice as large, a label too long beside the track keeps its pictogram beside it, " +
            "and the track goes under it, on the right",
    )
    fun inALargeFontTheTrackGoesUnderTheLabelOnTheRight() = extension.use {
        // Given « Personnalisations » twice as large, on a phone of 360 dp: a word too long to stand beside the track
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                BloxTheme(kind = BloxThemeKind.COLORFUL) {
                    SettingSwitchRow(
                        icon = BloxIcon.VIBRATE,
                        label = LONG_WORD,
                        subLine = VIBRATIONS_SUB_LINE,
                        checked = false,
                        onCheckedChange = {},
                        modifier = Modifier.width(360.dp),
                    )
                }
            }
        }

        // Then the label stands on the first line, beside the slot of the pictogram, and the track under the
        // sub-line, at the right end of the row
        val row = onNode(isToggleable()).getUnclippedBoundsInRoot()
        val label = onNodeWithText(LONG_WORD, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val subLine = onNodeWithText(VIBRATIONS_SUB_LINE, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val track = onNode(hasTestTag(SWITCH_TRACK_TAG), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertThat(label.left - row.left).isGreaterThanOrEqualTo(48.dp)
        assertThat(label.top - row.top).isLessThan(16.dp)
        assertThat(track.top).isGreaterThanOrEqualTo(subLine.bottom)
        assertThat((row.right - track.right).value).isCloseTo(8f, within(1f))
    }

    @Test
    @DisplayName("the whole row is the switch: a tap on its pictogram, its label or its track turns it over")
    fun theWholeRowIsTheSwitch() = extension.use {
        // Given the sound effects on
        val changes = mutableListOf<Boolean>()
        showRows(onSoundsChange = { changes += it })
        val row = onNode(isToggleable() and hasText(SOUNDS))

        // When it is tapped at its left end, on its pictogram, on its label, and at its right end, on its track
        row.performTouchInput { click(Offset(END_TAP.toPx(), centerY)) }
        onNodeWithText(SOUNDS).performClick()
        row.performTouchInput { click(Offset(width - END_TAP.toPx(), centerY)) }

        // Then each tap asks to turn it off (it stays on: the caller holds its state)
        runOnIdle { assertThat(changes).containsExactly(false, false, false) }
    }

    @Test
    @DisplayName("the thumb stands on the right of the track when on, 24 dp, and on its left when off, 20 dp")
    fun theThumbStandsOnTheRightWhenOnAndOnTheLeftWhenOff() = extension.use {
        // Given the sound effects on, the vibrations off
        showRows()

        // Then
        val (on, off) = listOf(SOUNDS, VIBRATIONS).map { thumbAndTrack(it) }
        assertThat(on.thumb.width.value).isCloseTo(24f, within(0.5f))
        assertThat(on.track.right - on.thumb.right).isLessThan(on.thumb.left - on.track.left)
        assertThat(off.thumb.width.value).isCloseTo(20f, within(0.5f))
        assertThat(off.thumb.left - off.track.left).isLessThan(off.track.right - off.thumb.right)
    }

    @Test
    @DisplayName("a row is 56 dp high at least, and its track 52 by 32 dp")
    fun aRowIs56DpHighAndItsTrack52By32() = extension.use {
        // Given
        showRows()

        // Then
        onNode(isToggleable() and hasText(SOUNDS)).assertHeightIsAtLeast(BloxSizes.settingRow)
        onAllNodesWithTag(SWITCH_TRACK_TAG, useUnmergedTree = true)[0]
            .assertWidthIsEqualTo(BloxSizes.switchTrack)
            .assertHeightIsEqualTo(32.dp)
    }

    @Test
    @DisplayName("a held row draws its thumb 28 dp long, and its thumb comes back to 24 dp once released")
    fun aHeldRowDrawsItsThumb28DpLong() = extension.use {
        // Given the sound effects on, on a clock that only moves when told
        mainClock.autoAdvance = false
        showRows()
        mainClock.advanceTimeByFrame()

        // When it is held
        onNode(isToggleable() and hasText(SOUNDS)).performTouchInput { down(center) }
        mainClock.advanceTimeByFrame()

        // Then
        assertThat(thumbAndTrack(SOUNDS).thumb.width.value).isCloseTo(28f, within(0.5f))

        // When it is let go, and motion press is over
        onNode(isToggleable() and hasText(SOUNDS)).performTouchInput { cancel() }
        mainClock.advanceTimeBy(RETURN_MS)

        // Then
        assertThat(thumbAndTrack(SOUNDS).thumb.width.value).isCloseTo(24f, within(0.5f))
    }

    /**
     * The bounds of the thumb and of the track of the row of [label], in the order [showRows] draws them: the unmerged
     * nodes of a thumb and of a track do not bear the text of their row.
     */
    private fun ComposeContext.thumbAndTrack(label: String): ThumbAndTrack {
        val row = listOf(SOUNDS, VIBRATIONS).indexOf(label)
        val thumb = onAllNodesWithTag(SWITCH_THUMB_TAG, useUnmergedTree = true)[row]
        val track = onAllNodesWithTag(SWITCH_TRACK_TAG, useUnmergedTree = true)[row]
        return ThumbAndTrack(thumb.getUnclippedBoundsInRoot(), track.getUnclippedBoundsInRoot())
    }

    private data class ThumbAndTrack(val thumb: DpRect, val track: DpRect)

    private fun ComposeContext.showRows(
        vibrationsDisabledReason: String? = null,
        onSoundsChange: (Boolean) -> Unit = {},
        onVibrationsChange: (Boolean) -> Unit = {},
    ) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Column {
                    SettingSwitchRow(
                        icon = BloxIcon.NOTE,
                        label = SOUNDS,
                        subLine = SOUNDS_SUB_LINE,
                        checked = true,
                        onCheckedChange = onSoundsChange,
                    )
                    SettingSwitchRow(
                        icon = BloxIcon.VIBRATE,
                        label = VIBRATIONS,
                        subLine = VIBRATIONS_SUB_LINE,
                        checked = false,
                        onCheckedChange = onVibrationsChange,
                        disabledReason = vibrationsDisabledReason,
                    )
                }
            }
        }
    }

    private companion object {
        const val SOUNDS = "Effets sonores"
        const val SOUNDS_SUB_LINE = "Rotation, lignes, fin de partie"
        const val VIBRATIONS = "Vibrations"
        const val VIBRATIONS_SUB_LINE = "Lignes effacées, fin de partie"
        const val NO_VIBRATOR = "Cet appareil ne vibre pas"

        /**
         * A label whose word, in a font twice as large, no longer fits beside the track on 360 dp. A word made up for
         * the test: no label of today goes under the track in Colorful ×2, but the branch stays alive for a larger
         * font or a narrower screen.
         */
        const val LONG_WORD = "Personnalisations"

        /** How far from an end of a row a tap falls: on its pictogram, or on its track. */
        val END_TAP = 20.dp

        /** More than motion press, 100 ms, and the frames it takes to start counting. */
        const val RETURN_MS = 200L
    }
}
