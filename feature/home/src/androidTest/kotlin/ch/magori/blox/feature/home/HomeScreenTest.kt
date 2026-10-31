package ch.magori.blox.feature.home

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.designsystem.component.GLYPH_BUTTON_ICON_TAG
import ch.magori.blox.core.designsystem.component.PLAY_BUTTON_ICON_TAG
import ch.magori.blox.core.designsystem.component.SettledSheet
import ch.magori.blox.core.designsystem.component.isChoiceCheck
import ch.magori.blox.core.designsystem.component.tapScrim
import ch.magori.blox.core.designsystem.test.RecompositionRecorder
import ch.magori.blox.core.designsystem.text.assertWordsWhole
import ch.magori.blox.core.designsystem.theme.BloxEffects
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class HomeScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @JvmField
    @RegisterExtension
    val recompositions = RecompositionRecorder()

    private val uiState = HomeUiState(
        theme = ThemeChoice.COLORFUL,
        music = MusicChoice.MINUET,
        startLevel = 5,
        startLevels = 0..9,
        record = 0,
        listenable = emptyList(),
        previewing = null,
    )

    /** The gauge of the start level, one slider for TalkBack. */
    private val gauge = hasContentDescription("Niveau de départ")

    @Test
    @DisplayName("choosing a music saves it and keeps the sheet open")
    fun choosingAMusicSavesItAndKeepsTheSheetOpen() = extension.use {
        // Given the sheet Musique open, the minuet chosen
        val chosen = mutableListOf<MusicChoice>()
        showHomeScreen(music = MusicActions.None.copy(onMusicChosen = { chosen += it }))
        openMusicSheet("Menuet")

        // When
        onNode(hasText("Korobeïniki") and hasAnyAncestor(isDialog())).performClick()

        // Then Korobeïniki is handed over to be saved (HomeViewModel saves it), and the sheet stays
        runOnIdle { assertThat(chosen).containsExactly(MusicChoice.KOROBEINIKI) }
        onNode(isDialog()).assertExists()
    }

    /** Opens the sheet Musique from the row Musique of the card, [music] being the music it shows. */
    private fun ComposeContext.openMusicSheet(music: String) {
        onNodeWithContentDescription("Musique : $music. Choisir une autre musique").performScrollTo().performClick()
    }

    /** What matches [matcher] in the sheet open. */
    private fun inSheet(matcher: SemanticsMatcher): SemanticsMatcher = matcher and hasAnyAncestor(isDialog())

    @Test
    @DisplayName("listening to a music stops the one previewed")
    fun listeningToAMusicStopsTheOnePreviewed() = extension.use {
        // Given the minuet listened to, in the sheet, where Korobeïniki may be listened to too
        val listened = mutableListOf<MusicChoice>()
        showHomeScreen(
            uiState.copy(
                listenable = listOf(MusicChoice.KOROBEINIKI, MusicChoice.MINUET),
                previewing = MusicChoice.MINUET,
            ),
            music = MusicActions.None.copy(onListen = { listened += it }),
        )
        openMusicSheet("Menuet")
        // Each music bears its own listen button, the one listened to only offering to stop
        onAllNodes(inSheet(hasContentDescription("Arrêter", substring = true))).assertCountEquals(1)
        onNode(inSheet(hasContentDescription("Arrêter Menuet"))).assertExists()

        // When
        onNode(inSheet(hasContentDescription("Écouter Korobeïniki"))).performClick()

        // Then Korobeïniki is asked for (HomeViewModel stops the minuet before playing it), in the sheet still open
        runOnIdle { assertThat(listened).containsExactly(MusicChoice.KOROBEINIKI) }
        onNode(isDialog()).assertExists()
    }

    @Test
    @DisplayName("closing the sheet stops the preview")
    fun closingTheSheetStopsThePreview() = extension.use {
        // Given the sheet open, the minuet listened to
        var closed = 0
        showHomeScreen(
            uiState.copy(listenable = listOf(MusicChoice.MINUET), previewing = MusicChoice.MINUET),
            music = MusicActions.None.copy(onSheetClosed = { closed++ }),
        )
        openMusicSheet("Menuet")

        // When the scrim is touched, above the sheet
        onNode(isDialog()).tapScrim()

        // Then the sheet is gone, and says so once (HomeViewModel stops the preview)
        onNode(isDialog()).assertDoesNotExist()
        runOnIdle { assertThat(closed).isEqualTo(1) }
    }

    @Test
    @DisplayName("with Mon fichier chosen without a file, the row Musique keeps its name, after an empty slot of 48 dp")
    fun withMyFileWithoutAFileTheRowKeepsItsNameAfterAnEmptySlot() = extension.use {
        // Given Mon fichier chosen, without a file: nothing to listen to, Korobeïniki playing in its place
        showHomeScreen(uiState.copy(music = MusicChoice.MY_FILE, hasMusicFile = false, listenable = emptyList()))

        // Then the row names the choice made, after the room of a listen button, left empty
        val opener = onNodeWithContentDescription("Musique : Mon fichier. Choisir une autre musique")
            .getUnclippedBoundsInRoot()
        val slot = onNode(hasTestTag(MUSIC_SLOT_TAG), useUnmergedTree = true)
            .assertWidthIsEqualTo(BloxSizes.minTouchTarget)
            .assertHeightIsEqualTo(BloxSizes.minTouchTarget)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .getUnclippedBoundsInRoot()
        assertThat(opener.left).isGreaterThanOrEqualTo(slot.right)
        assertThat(slot.top).isLessThan(opener.bottom)
        assertThat(slot.bottom).isGreaterThan(opener.top)
        onAllNodes(hasContentDescription("Écouter", substring = true)).assertCountEquals(0)
    }

    @Test
    @DisplayName("Aucune shows the note barred")
    fun noneShowsTheNoteBarred() = extension.use {
        // Given Aucune chosen
        showHomeScreen(uiState.copy(music = MusicChoice.NONE, listenable = listOf(MusicChoice.KOROBEINIKI)))

        // Then the row Musique bears the note struck through, which TalkBack does not read, and nothing to listen to
        onNode(hasTestTag(MUSIC_NONE_TAG), useUnmergedTree = true)
            .assertExists()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        onAllNodes(hasContentDescription("Écouter", substring = true)).assertCountEquals(0)
    }

    @Test
    @DisplayName("the sheet is read as a dialog by TalkBack")
    fun theSheetIsReadAsADialogByTalkBack() = extension.use {
        // Given
        showHomeScreen(uiState.copy(listenable = listOf(MusicChoice.KOROBEINIKI, MusicChoice.MINUET)))

        // When the row Musique, a button, is touched
        onNodeWithContentDescription("Musique : Menuet. Choisir une autre musique")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        openMusicSheet("Menuet")

        // Then a dialog, its title first, then its musics in their order, each a radio button, the minuet chosen
        val read = readingOrder(onNode(isDialog()).fetchSemanticsNode()).map { it.first }
        assertThat(read.first()).isEqualTo("Musique")
        val musics = listOf("Korobeïniki", "Menuet", "Vivante", "Mon fichier", "Aucune")
        assertThat(musics.map { music -> read.indexOfFirst { it.startsWith(music) } }).isSorted().doesNotContain(-1)
        musics.forEach { music ->
            onNode(inSheet(hasText(music)))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                .assertHeightIsAtLeast(BloxSizes.settingRow)
        }
        onNode(inSheet(hasText("Menuet"))).assertIsSelected()
        onNode(inSheet(hasText("Korobeïniki"))).assertIsNotSelected()
    }

    @Test
    @DisplayName(
        "with Mon fichier chosen without a file, the reminder under Jouer names Korobeïniki, played in its place",
    )
    fun withMyFileWithoutAFileTheReminderNamesKorobeiniki() = extension.use {
        // Given Mon fichier chosen, without a file (the settings restored on another device)
        showHomeScreen(uiState.copy(music = MusicChoice.MY_FILE, hasMusicFile = false))

        // Then
        onNode(hasText("Classique · Niveau 05 · Korobeïniki"), useUnmergedTree = true).assertExists()
        onNode(hasTestTag(PLAY_TAG)).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("Jouer, Classique, niveau 5, Korobeïniki"),
            ),
        )
    }

    @Test
    @DisplayName(
        "in the sheet, Mon fichier with a file offers Changer, which opens the picker, before its listen button",
    )
    fun inTheSheetMyFileOffersChange() = extension.use {
        // Given a music file kept
        var changed = 0
        showHomeScreen(
            uiState.copy(listenable = listOf(MusicChoice.MY_FILE), hasMusicFile = true),
            music = MusicActions.None.copy(onChangeMusicFile = { changed++ }),
        )
        openMusicSheet("Menuet")

        // Then Changer stands on the line of Mon fichier, before Écouter
        val change = onNode(inSheet(hasContentDescription("Changer de fichier")))
        val listen = onNode(inSheet(hasContentDescription("Écouter Mon fichier"))).getUnclippedBoundsInRoot()
        val bounds = change.getUnclippedBoundsInRoot()
        assertThat(bounds.right).isLessThanOrEqualTo(listen.left)
        assertThat(bounds.top).isLessThan(listen.bottom)
        assertThat(bounds.bottom).isGreaterThan(listen.top)

        // When
        change.performClick()

        // Then
        runOnIdle { assertThat(changed).isEqualTo(1) }
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("in a font twice as large, the row Mon fichier keeps its words whole, Changer beside or under Écouter")
    fun inALargeFontTheRowMyFileKeepsChangeAndListen(kind: BloxThemeKind) = extension.use {
        // Given the sheet, a music file kept, in a font twice as large, on 360 dp
        showSettledMusicSheet(NARROW_PHONE, fontScale = 2f, kind = kind)

        // Then the choice keeps its words whole, and Changer holds one line
        assertWordsWhole(textLayout("Un fichier audio de l'appareil"))
        val layout = textLayout("Changer")
        assertWordsWhole(layout)
        assertThat(layout.lineCount).isEqualTo(1)
        // And Changer stands beside Écouter, before it, or under it when there is no room for the words of the choice
        val (change, listen, name) = myFileRow()
        assertThat(change.right <= listen.left || change.top >= listen.bottom).describedAs("beside or under").isTrue()
        assertApart(change, listen, name)
        assertWithinSheet(change, listen)
    }

    @Test
    @DisplayName(
        "in a font twice as large, in a narrow window, Changer goes under Écouter, at the end of the row, apart",
    )
    fun inALargeFontInANarrowWindowChangeGoesUnderListen() = extension.use {
        // Given the sheet, a music file kept, in a font twice as large, in a window of 280 dp, narrower than any phone:
        // beside Écouter, Changer would leave the choice less room than its longest word
        showSettledMusicSheet(280.dp, fontScale = 2f, kind = BloxThemeKind.COLORFUL)

        // Then
        val (change, listen, name) = myFileRow()
        assertThat(change.top).isGreaterThanOrEqualTo(listen.bottom)
        assertThat(change.right.value).isCloseTo(listen.right.value, within(PIXEL_ROUNDING))
        assertApart(change, listen, name)
        assertWithinSheet(change, listen)
    }

    @Test
    @DisplayName("in a normal font, Changer stands beside Écouter, before it, apart from the name Mon fichier")
    fun inANormalFontChangeStandsBesideListen() = extension.use {
        // Given the sheet, a music file kept, on 360 dp
        showSettledMusicSheet(NARROW_PHONE, fontScale = 1f, kind = BloxThemeKind.COLORFUL)

        // Then
        val (change, listen, name) = myFileRow()
        assertThat(listen.left).isGreaterThanOrEqualTo(change.right)
        assertApart(change, listen, name)
        assertWithinSheet(change, listen)
    }

    @Test
    @DisplayName("in the sheet, Mon fichier with a file shows its name under it, read by TalkBack with the choice")
    fun inTheSheetMyFileShowsTheNameOfItsFile() = extension.use {
        // Given a music file kept, with its name
        showHomeScreen(
            uiState.copy(listenable = listOf(MusicChoice.MY_FILE), hasMusicFile = true, musicFileName = FILE_NAME),
        )
        openMusicSheet("Menuet")

        // Then the name stands in place of « Un fichier audio de l'appareil », read with Mon fichier
        onNode(inSheet(isSelectable() and hasText("Mon fichier") and hasText(FILE_NAME))).assertExists()
        onAllNodes(inSheet(hasText("Un fichier audio de l'appareil"))).assertCountEquals(0)
    }

    @Test
    @DisplayName("in the sheet, Vivante is read by TalkBack with its sub-line, and may be listened to")
    fun inTheSheetVivanteIsReadWithItsSubLineAndMayBeListenedTo() = extension.use {
        // Given the sheet open, the live music listenable
        val listened = mutableListOf<MusicChoice>()
        showHomeScreen(
            uiState.copy(listenable = listOf(MusicChoice.LIVE)),
            music = MusicActions.None.copy(onListen = { listened += it }),
        )
        openMusicSheet("Menuet")
        // ...its row read by TalkBack with its sub-line, not chosen
        onNode(inSheet(isSelectable() and hasText("Vivante") and hasText(LIVE_SUB_LINE))).assertIsNotSelected()

        // When
        onNode(inSheet(hasContentDescription("Écouter Vivante"))).performClick()

        // Then the live music is asked for (HomeViewModel has a music composed for the listening)
        runOnIdle { assertThat(listened).containsExactly(MusicChoice.LIVE) }
    }

    @Test
    @DisplayName("with Vivante chosen, the reminder under Jouer names it, and the row Musique offers to listen to it")
    fun withVivanteChosenTheReminderNamesIt() = extension.use {
        // Given Vivante chosen, at level 5
        showHomeScreen(uiState.copy(music = MusicChoice.LIVE, listenable = listOf(MusicChoice.LIVE)))

        // Then
        onNode(hasText("Classique · Niveau 05 · Vivante"), useUnmergedTree = true).assertExists()
        onNode(hasTestTag(PLAY_TAG)).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("Jouer, Classique, niveau 5, Vivante"),
            ),
        )
        onNodeWithContentDescription("Musique : Vivante. Choisir une autre musique").assertExists()
        onNode(hasContentDescription("Écouter Vivante")).assertExists()
    }

    @Test
    @DisplayName("in the sheet, Mon fichier with a file of unknown name says it is an audio file of the device")
    fun inTheSheetMyFileOfUnknownNameSaysItIsAFileOfTheDevice() = extension.use {
        // Given a music file kept without a name (kept before the names were, or whose name could not be read)
        showHomeScreen(uiState.copy(listenable = listOf(MusicChoice.MY_FILE), hasMusicFile = true))
        openMusicSheet("Menuet")

        // Then
        onNode(inSheet(isSelectable() and hasText("Mon fichier") and hasText("Un fichier audio de l'appareil")))
            .assertExists()
    }

    @Test
    @DisplayName(
        "in a normal font, on 360 dp, a name of a file stands whole under Mon fichier, Changer under the row",
    )
    fun inANormalFontANameOfAFileStandsWhole() = extension.use {
        // Given the sheet, a music file of a usual name kept, on 360 dp
        showSettledMusicSheet(NARROW_PHONE, fontScale = 1f, kind = BloxThemeKind.COLORFUL, fileName = FILE_NAME)

        // Then the name is not cut: Changer leaves it room, under Écouter, at the end of the row
        val layout = textLayout(FILE_NAME)
        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.isLineEllipsized(0)).isFalse()
        val (change, listen, _) = myFileRow()
        assertThat(change.top).isGreaterThanOrEqualTo(listen.bottom)
        assertThat(change.right.value).isCloseTo(listen.right.value, within(PIXEL_ROUNDING))
        val name = onNode(hasText(FILE_NAME), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertApart(change, listen, name)
        assertWithinSheet(change, listen, name)
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName(
        "in a font twice as large, a long name of a file holds one line under Mon fichier, cut in its middle, apart",
    )
    fun inALargeFontALongNameOfAFileHoldsOneLine(kind: BloxThemeKind) = extension.use {
        // Given the sheet, a music file of a long name kept, in a font twice as large, on 360 dp
        showSettledMusicSheet(NARROW_PHONE, fontScale = 2f, kind = kind, fileName = LONG_FILE_NAME)

        // Then the name holds one line, its end kept, read whole
        val layout = textLayout(LONG_FILE_NAME)
        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.getLineEnd(0, visibleEnd = true)).isEqualTo(LONG_FILE_NAME.length)
        onNode(isSelectable() and hasText(LONG_FILE_NAME)).assertExists()
        // And it stands apart from Changer and Écouter, within the sheet
        val (change, listen, _) = myFileRow()
        val name = onNode(hasText(LONG_FILE_NAME), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertApart(change, listen, name)
        assertWithinSheet(change, listen, name)
    }

    /**
     * The sheet Musique come in, without its window, [width] wide, in a font [fontScale] times as large, in [kind]: its
     * window would take the density of the device, not the one given to the test. A music file is kept, of [fileName].
     */
    private fun ComposeContext.showSettledMusicSheet(
        width: Dp,
        fontScale: Float,
        kind: BloxThemeKind,
        fileName: String? = null,
    ) {
        val state = uiState.copy(
            listenable = listOf(MusicChoice.MY_FILE),
            hasMusicFile = true,
            musicFileName = fileName,
        )
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = kind, effects = STILL) {
                    Box(Modifier.width(width).testTag(SETTLED_SHEET)) {
                        SettledSheet(title = "Musique") { MusicSheetChoices(state, MusicActions.None) }
                    }
                }
            }
        }
    }

    /** Where Changer, Écouter and the name Mon fichier stand in its row of the sheet. */
    private fun ComposeContext.myFileRow(): Triple<DpRect, DpRect, DpRect> = Triple(
        onNodeWithContentDescription("Changer de fichier").getUnclippedBoundsInRoot(),
        onNodeWithContentDescription("Écouter Mon fichier").getUnclippedBoundsInRoot(),
        onNode(hasText("Mon fichier"), useUnmergedTree = true).getUnclippedBoundsInRoot(),
    )

    /** No two of [parts] overlap. */
    private fun assertApart(vararg parts: DpRect) {
        parts.toList().forEachIndexed { index, part ->
            parts.drop(index + 1).forEach { other ->
                val overlap = part.left < other.right && other.left < part.right &&
                    part.top < other.bottom && other.top < part.bottom
                assertThat(overlap).describedAs("$part over $other").isFalse()
            }
        }
    }

    /** Each of [parts] stands within the sheet, from side to side. */
    private fun ComposeContext.assertWithinSheet(vararg parts: DpRect) {
        val sheet = onNode(hasTestTag(SETTLED_SHEET)).getUnclippedBoundsInRoot()
        parts.forEach { part ->
            assertThat(part.left).isGreaterThanOrEqualTo(sheet.left)
            assertThat(part.right).isLessThanOrEqualTo(sheet.right)
        }
    }

    @Test
    @DisplayName("in the sheet, a music of the player for the games is the only music selected, read with its sub-line")
    fun inTheSheetTheMusicOfThePlayerIsTheOnlyMusicSelected() = extension.use {
        // Given
        showHomeScreen(uiState.copy(gameMusic = GameMusicUi(name = "Thème du soir", previewed = false)))
        openMusicSheet("Thème du soir")

        // Then it stands before Aucune, chosen, and TalkBack says it is the music of the games
        onNode(inSheet(hasText("Thème du soir")))
            .assertIsSelected()
            .assert(hasText("Ma musique · en jeu"))
        onAllNodes(inSheet(isSelected())).assertCountEquals(1)
        val theme = onNode(inSheet(hasText("Thème du soir"))).getUnclippedBoundsInRoot()
        assertThat(theme.bottom).isLessThanOrEqualTo(onNode(inSheet(hasText("Aucune"))).getUnclippedBoundsInRoot().top)
    }

    @Test
    @DisplayName("TalkBack reads each selected choice as selected, and the others as not selected")
    fun talkBackReadsEachSelectedChoiceAsSelected() = extension.use {
        // Given
        showHomeScreen()

        // Then
        onNodeWithText("Classique").assertIsSelected()
        onNodeWithText("Couleurs").assertIsSelected()
        onNodeWithText("Original").assertIsNotSelected()
    }

    @Test
    @DisplayName("the Modern mode is disabled, as TalkBack reads it, beside the Classic mode, selected")
    fun theModernModeIsDisabled() = extension.use {
        // Given
        showHomeScreen()

        // Then
        onNodeWithContentDescription("Moderne, bientôt").assertIsNotEnabled().assertIsNotSelected()
        onNodeWithText("Classique").assertIsEnabled().assertIsSelected()
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("on 360 dp, the two modes stand on one line")
    fun theTwoModesStandOnOneLine(kind: BloxThemeKind) = extension.use {
        // Given
        showHomeScreen(width = NARROW_PHONE, kind = kind)

        // Then
        val classic = onNodeWithText("Classique").getUnclippedBoundsInRoot()
        val modern = onNodeWithContentDescription("Moderne, bientôt").getUnclippedBoundsInRoot()
        assertThat(modern.top).isEqualTo(classic.top)
        assertThat(modern.left).isGreaterThanOrEqualTo(classic.right)
    }

    @Test
    @DisplayName("the home leaves 16 dp above its bar and on its sides, and 16 dp between two rows of its settings")
    fun theHomeLeaves16DpAroundItsBarAndBetweenRows() = extension.use {
        // Given a phone of 360 dp, without the bars of the system, which push the screen down
        showHomeScreen(width = NARROW_PHONE, withoutInsets = true)

        // Then the bar is 16 dp under the top, 16 dp off each side
        val gear = onNodeWithContentDescription("Paramètres").getUnclippedBoundsInRoot()
        val disc = onNodeWithContentDescription("Changer de jeu").getUnclippedBoundsInRoot()
        assertThat(gear.top.value).isCloseTo(16f, within(PIXEL_ROUNDING))
        assertThat(disc.left.value).isCloseTo(16f, within(PIXEL_ROUNDING))
        assertThat(gear.right.value).isCloseTo((NARROW_PHONE - 16.dp).value, within(PIXEL_ROUNDING))
        // And a row starts with its label, above its group, or with its group, beside its label and taller
        val modes = onNodeWithText("Classique").getUnclippedBoundsInRoot()
        val theme = minOf(
            onNodeWithText("Thème").getUnclippedBoundsInRoot().top,
            onNodeWithText("Original").getUnclippedBoundsInRoot().top,
        )
        assertThat((theme - modes.bottom).value).isCloseTo(16f, within(PIXEL_ROUNDING))
    }

    @Test
    @DisplayName("the music selected being listened to offers to stop it, under a glyph that TalkBack does not read")
    fun theMusicSelectedBeingListenedToOffersToStopIt() = extension.use {
        // Given
        showHomeScreen(uiState.copy(listenable = listOf(MusicChoice.MINUET), previewing = MusicChoice.MINUET))

        // Then
        onNodeWithContentDescription("Écouter Menuet").assertDoesNotExist()
        onNodeWithContentDescription("Arrêter Menuet")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Arrêter Menuet")))
        listenIconOf(hasContentDescription("Arrêter Menuet"))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
    }

    @Test
    @DisplayName("each selected choice bears a check, the others none, and TalkBack does not read it")
    fun eachSelectedChoiceBearsACheckThatTalkBackDoesNotRead() = extension.use {
        // Given « Classique » and « Couleurs » (the musics are chosen in their sheet, which bears its own checks)
        showHomeScreen()

        // Then a cue other than colour (WCAG 2.1, 1.4.1), in the selected choices only
        val checks = onAllNodes(isChoiceCheck(), useUnmergedTree = true)
        assertThat(checks.fetchSemanticsNodes()).hasSize(2)
        onAllNodes(isChoiceCheck() and hasParent(isSelected()), useUnmergedTree = true)
            .assertCountEquals(2)
        // And decorative: TalkBack already says « sélectionné », it reads nothing more
        checks.assertAll(
            SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription) and
                SemanticsMatcher.keyNotDefined(SemanticsProperties.Text),
        )
    }

    @Test
    @DisplayName("every target of the home is 48 dp at least on 360 dp")
    fun everyTargetIsLargeEnough() = extension.use {
        // Given a music that may be listened to, with its button, on a phone of 360 dp, the width of the mockup
        showHomeScreen(uiState.copy(listenable = listOf(MusicChoice.MINUET)), width = NARROW_PHONE)

        // Then: 48 dp visible, a rule of BloX, stricter than WCAG 2.2 (2.5.8, 24 px) and than Android, whose
        // 48 dp is a touch area that may pass beyond what is drawn; − and + of the gauge included
        val targets = onAllNodes(hasClickAction())
        val count = targets.fetchSemanticsNodes().size
        assertThat(count).isGreaterThanOrEqualTo(HOME_TARGETS)
        repeat(count) { index ->
            targets[index].assertWidthIsAtLeast(BloxSizes.minTouchTarget)
                .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        }
        // And the gauge, touched or dragged
        onNode(gauge).assertWidthIsAtLeast(BloxSizes.minTouchTarget).assertHeightIsAtLeast(BloxSizes.minTouchTarget)
    }

    @Test
    @DisplayName("the gauge chooses the start level and saves it")
    fun theGaugeChoosesTheStartLevel() = extension.use {
        // Given level 5 chosen, of 0 to 9
        val chosen = mutableListOf<Int>()
        showHomeScreen(onStartLevelChosen = { chosen += it })
        onNode(gauge).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(current = 5f, range = 0f..9f, steps = 8),
            ),
        )

        // When TalkBack, or a finger, moves it to 7
        onNode(gauge).performSemanticsAction(SemanticsActions.SetProgress) { it(7f) }

        // Then 7 is handed over to be saved (HomeViewModel saves it)
        runOnIdle { assertThat(chosen).containsExactly(7) }
    }

    @Test
    @DisplayName("the reminder under Play says the mode, the level on two digits and the music, read whole by TalkBack")
    fun theReminderUnderPlaySaysTheModeTheLevelAndTheMusic() = extension.use {
        // Given level 5 and the minuet
        showHomeScreen(width = NARROW_PHONE)

        // Then the reminder stands under Jouer
        val play = onNode(hasText("Jouer") and hasAnyAncestor(hasTestTag(PLAY_TAG)), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val reminder = onNode(hasText(REMINDER), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertThat(reminder.top).isGreaterThanOrEqualTo(play.bottom)
        // And TalkBack reads Jouer, then the reminder in words, without its dots nor the zero of the level
        onNode(hasTestTag(PLAY_TAG)).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("Jouer, Classique, niveau 5, Menuet"),
            ),
        )
    }

    @Test
    @DisplayName("Jouer starts a game at the level chosen")
    fun playStartsAGameAtTheLevelChosen() = extension.use {
        // Given level 5 chosen
        val played = mutableListOf<Int>()
        showHomeScreen(onPlay = { played += it })

        // When
        onNode(hasTestTag(PLAY_TAG)).performScrollTo().performClick()

        // Then
        runOnIdle { assertThat(played).containsExactly(5) }
    }

    @Test
    @DisplayName("TalkBack reads the parts in the order of the screen")
    fun talkBackReadsThePartsInTheOrderOfTheScreen() = extension.use {
        // Given the minuet selected, which may be listened to, on 360 dp
        showHomeScreen(uiState.copy(listenable = listOf(MusicChoice.MINUET)), width = NARROW_PHONE)

        // Then the bar, the title, Jouer, the settings, row by row, and the tiles, in the order of the tree
        val expected = listOf(
            "Changer de jeu", "Record 0", "Paramètres", "BloX", "Jouer, Classique, niveau 5, Menuet",
            "Mode", "Classique", "Moderne, bientôt", "Thème", "Original", "Couleurs", "Niveau", "Niveau de départ",
            "Musique", "Écouter Menuet", "Musique : Menuet. Choisir une autre musique", "Mes musiques",
            "Composer une musique",
        )
        val read = readingOrder(onRoot().fetchSemanticsNode()).filter { (name, _) -> name in expected }
        assertThat(read.map { it.first }).containsExactlyElementsOf(expected)
        // And each part stands beside the one before on its line, after it, whatever their heights (a label is centred
        // beside a taller group), or on a row under it
        read.zipWithNext().forEach { (before, after) ->
            val (a, b) = before.second to after.second
            val sameLine = b.top < a.bottom && b.bottom > a.top
            if (sameLine) {
                assertThat(b.left).describedAs("${after.first} beside ${before.first}").isGreaterThanOrEqualTo(a.left)
            } else {
                assertThat(b.top).describedAs("${after.first} under ${before.first}").isGreaterThanOrEqualTo(a.top)
            }
        }
    }

    /** What TalkBack reads of [node] and of each node under it, in the order of the tree, and where each stands. */
    private fun ComposeContext.readingOrder(node: SemanticsNode): List<Pair<String, DpRect>> {
        val name = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
            ?: node.config.getOrNull(SemanticsProperties.Text)?.joinToString()
        val bounds = with(density) {
            node.boundsInRoot.let { DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp()) }
        }
        val own = if (name != null) listOf(name to bounds) else emptyList()
        return own + node.children.flatMap { readingOrder(it) }
    }

    @Test
    @DisplayName("listening to the music of the player for the games calls for it")
    fun listeningToTheMusicOfThePlayerCallsForIt() = extension.use {
        // Given
        var listened = 0
        showHomeScreen(
            uiState.copy(gameMusic = GameMusicUi(name = "Thème du soir", previewed = false)),
            music = MusicActions.None.copy(onListenGameMusic = { listened++ }),
        )

        // When
        onNodeWithContentDescription("Écouter Thème du soir").performScrollTo().performClick()

        // Then
        assertThat(listened).isEqualTo(1)
    }

    @Test
    @DisplayName("the music of the player being listened to offers to stop it")
    fun theMusicOfThePlayerBeingListenedToOffersToStopIt() = extension.use {
        // Given
        showHomeScreen(uiState.copy(gameMusic = GameMusicUi(name = "Thème du soir", previewed = true)))

        // Then
        onNodeWithContentDescription("Arrêter Thème du soir").assertHasClickAction()
        onNodeWithContentDescription("Écouter Thème du soir").assertDoesNotExist()
    }

    @Test
    @DisplayName("the tile Composer une musique, read as written, opens the music editor")
    fun composeTileOpensTheEditor() = extension.use {
        // Given
        var composing = 0
        showHomeScreen(onCompose = { composing++ })

        // When
        onNode(hasText("Composer une musique") and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performScrollTo()
            .performClick()

        // Then
        runOnIdle { assertThat(composing).isEqualTo(1) }
    }

    @Test
    @DisplayName("the tile Mes musiques, read as written, opens my musics")
    fun myMusicsTileOpensMyMusics() = extension.use {
        // Given
        var opened = 0
        showHomeScreen(onMyMusics = { opened++ })

        // When
        onNode(hasText("Mes musiques") and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performScrollTo()
            .performClick()

        // Then
        runOnIdle { assertThat(opened).isEqualTo(1) }
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("in a font twice as large, on 360 dp, the labels of the tiles are shown whole, their words whole")
    fun inALargeFontTheLabelsOfTheTilesAreShownWhole(kind: BloxThemeKind) = extension.use {
        // Given a font twice as large, on 360 dp
        showHomeScreen(width = NARROW_PHONE, fontScale = 2f, kind = kind)

        // Then each label, as laid out, holds whole words within its tile and the screen
        listOf("Mes musiques", "Composer une musique").forEach { label ->
            val layout = textLayout(label)
            assertWordsWhole(layout)
            assertThat(layout.hasVisualOverflow).describedAs(label).isFalse()
            val tile = onNode(hasText(label) and hasClickAction()).getUnclippedBoundsInRoot()
            val shown = onNode(hasText(label), useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertThat(shown.left).isGreaterThanOrEqualTo(tile.left)
            assertThat(shown.right).isLessThanOrEqualTo(tile.right)
            assertThat(tile.right).isLessThanOrEqualTo(NARROW_PHONE)
        }
    }

    @Test
    @DisplayName("in a normal font, on 360 dp, the tiles stand side by side, as high as each other")
    fun inANormalFontTheTilesStandSideBySide() = extension.use {
        // Given the normal font, on 360 dp, in Couleurs (the pixel font of Original, wider, stacks them already: it
        // stays readable, the tiles of Original are not drawn otherwise)
        showHomeScreen(width = NARROW_PHONE, kind = BloxThemeKind.COLORFUL)

        // Then
        val (myMusics, compose) = tiles()
        assertThat(compose.top).isEqualTo(myMusics.top)
        assertThat(compose.left).isGreaterThanOrEqualTo(myMusics.right)
        assertThat(compose.bottom).isEqualTo(myMusics.bottom)
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("in a font twice as large, on 360 dp, the tiles stand one above the other, each as wide as the line")
    fun inALargeFontTheTilesStandOneAboveTheOther(kind: BloxThemeKind) = extension.use {
        // Given a font twice as large, on 360 dp, where « Composer » no longer holds in half the line
        showHomeScreen(width = NARROW_PHONE, fontScale = 2f, kind = kind)

        // Then
        val (myMusics, compose) = tiles()
        assertThat(compose.top).isGreaterThanOrEqualTo(myMusics.bottom)
        listOf(myMusics, compose).forEach { tile ->
            assertThat(tile.left.value).isCloseTo(16f, within(PIXEL_ROUNDING))
            assertThat(tile.right.value).isCloseTo((NARROW_PHONE - 16.dp).value, within(PIXEL_ROUNDING))
        }
    }

    /** Where the tiles Mes musiques and Composer une musique stand, once the home scrolled down to the last. */
    private fun ComposeContext.tiles(): Pair<DpRect, DpRect> {
        val myMusics = onNode(hasText("Mes musiques") and hasClickAction())
        val compose = onNode(hasText("Composer une musique") and hasClickAction()).performScrollTo()
        return myMusics.getUnclippedBoundsInRoot() to compose.getUnclippedBoundsInRoot()
    }

    /** The text shown as [text], as laid out. */
    private fun ComposeContext.textLayout(text: String): TextLayoutResult {
        val label = onNode(hasText(text), useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        label.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single()
    }

    @Test
    @DisplayName("the gear button, 48 dp, read Paramètres, opens the settings")
    fun theGearButtonOpensTheSettings() = extension.use {
        // Given
        var opened = 0
        showHomeScreen(onSettings = { opened++ })

        // When
        onNodeWithContentDescription("Paramètres")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(BloxSizes.minTouchTarget)
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
            .performClick()

        // Then
        runOnIdle { assertThat(opened).isEqualTo(1) }
    }

    @Test
    @DisplayName("the disc Changer de jeu, 48 dp, at the start of the bar and opposite the gear, changes the game")
    fun theChangeGameDiscChangesTheGame() = extension.use {
        // Given a phone of 360 dp
        var changed = 0
        showHomeScreen(width = NARROW_PHONE, onChangeGame = { changed++ })
        val disc = onNodeWithContentDescription("Changer de jeu")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(BloxSizes.minTouchTarget)
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        val bounds = disc.getUnclippedBoundsInRoot()
        val gear = onNodeWithContentDescription("Paramètres").getUnclippedBoundsInRoot()
        // The mirror of the gear: as far from the left edge as the gear from the right one, as high, as wide
        assertThat(bounds.left.value).isCloseTo((NARROW_PHONE - gear.right).value, within(PIXEL_ROUNDING))
        assertThat(bounds.top.value).isCloseTo(gear.top.value, within(PIXEL_ROUNDING))
        assertThat((bounds.right - bounds.left).value).isCloseTo((gear.right - gear.left).value, within(PIXEL_ROUNDING))

        // When
        disc.performClick()

        // Then
        runOnIdle { assertThat(changed).isEqualTo(1) }
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("the record chip stands in the bar between Changer de jeu and the gear, 48 dp high")
    fun theRecordChipStandsInTheBar(kind: BloxThemeKind) = extension.use {
        // Given a phone of 360 dp, in a font twice as large, a record of six digits
        showHomeScreen(uiState.copy(record = 999_999), width = NARROW_PHONE, fontScale = 2f, kind = kind)

        // Then
        val chip = onNodeWithContentDescription("Record 999999")
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
            .getUnclippedBoundsInRoot()
        val disc = onNodeWithContentDescription("Changer de jeu").getUnclippedBoundsInRoot()
        val gear = onNodeWithContentDescription("Paramètres").getUnclippedBoundsInRoot()
        assertThat(chip.left).isGreaterThanOrEqualTo(disc.right)
        assertThat(chip.right).isLessThanOrEqualTo(gear.left)
        assertThat(chip.top).isLessThan(gear.bottom)
        assertThat(chip.bottom).isGreaterThan(gear.top)
    }

    @Test
    @DisplayName("the logo BloX, read as the heading of the screen, stands centred under the bar, the preview under it")
    fun theLogoStandsCentredUnderTheBar() = extension.use {
        // Given
        showHomeScreen(width = NARROW_PHONE)

        // Then
        val logo = onNode(hasContentDescription("BloX") and isHeading()).getUnclippedBoundsInRoot()
        val preview = onNode(hasTestTag(HOME_PREVIEW_TAG), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val gear = onNodeWithContentDescription("Paramètres").getUnclippedBoundsInRoot()
        assertThat(logo.top).isGreaterThanOrEqualTo(gear.bottom)
        assertThat((logo.left + logo.right) / 2 - NARROW_PHONE / 2).isBetween((-1).dp, 1.dp)
        assertThat(preview.top).isGreaterThanOrEqualTo(logo.bottom)
        assertThat((preview.left + preview.right) / 2 - NARROW_PHONE / 2).isBetween((-1).dp, 1.dp)
    }

    @Test
    @DisplayName("nothing on the home recomposes while the preview plays")
    fun nothingOnTheHomeRecomposesWhileThePreviewPlays() = extension.use {
        // Given the home at level 9, its frames handed by hand
        mainClock.autoAdvance = false
        showHomeScreen(uiState.copy(startLevel = 9), effects = BloxEffects.Full)
        mainClock.advanceTimeByFrame()
        val before = onRoot().captureToImage().asAndroidBitmap()

        // When 4 seconds go by: the T falls and clears its lines, the halo of Jouer pulses
        recompositions.start()
        mainClock.advanceTimeBy(FOUR_SECONDS)

        // Then the home is drawn again, and only drawn
        assertThat(onRoot().captureToImage().asAndroidBitmap().sameAs(before)).isFalse()
        assertThat(recompositions.recomposed).isEmpty()
    }

    @Test
    @DisplayName("a touch on the home puts the halo of Jouer off for 3 seconds again")
    fun aTouchOnTheHomePutsTheHaloOffAgain() = extension.use {
        // Given the home in full effects, its frames handed by hand, nothing yet around Jouer
        mainClock.autoAdvance = false
        showHomeScreen(effects = BloxEffects.Full)
        mainClock.advanceTimeByFrame()
        val off = aboveJouer()

        // When the halo pulses, three seconds and a half later, and the player touches the home (the logo)
        mainClock.advanceTimeBy(HALF_A_PULSE_IN)
        val pulsing = aboveJouer()
        onNode(hasContentDescription("BloX") and isHeading()).performTouchInput {
            down(center)
            up()
        }
        mainClock.advanceTimeBy(ONE_SECOND)

        // Then the halo showed above Jouer, and a second after the touch, it is off again
        assertThat(pulsing.sameAs(off)).describedAs("the halo pulsing").isFalse()
        assertThat(aboveJouer().sameAs(off)).describedAs("the halo off again").isTrue()
    }

    /** The strip of the screen just above Jouer, where its halo shows, under the preview. */
    private fun ComposeContext.aboveJouer(): Bitmap {
        val image = onRoot().captureToImage().asAndroidBitmap()
        val top = onNode(hasTestTag(PLAY_TAG)).getUnclippedBoundsInRoot().top
        val (from, height) = with(density) { (top - ABOVE_JOUER).roundToPx() to STRIP.roundToPx() }
        return Bitmap.createBitmap(image, 0, from, image.width, height)
    }

    @Test
    @DisplayName("in reduced effects the preview and the halo stand still")
    fun inReducedEffectsThePreviewAndTheHaloStandStill() = extension.use {
        // Given the home in reduced effects, its frames handed by hand
        mainClock.autoAdvance = false
        showHomeScreen(effects = STILL)
        mainClock.advanceTimeByFrame()
        val before = onRoot().captureToImage().asAndroidBitmap()

        // When 4 seconds go by, those of a T falling and of the first pulses of the halo
        mainClock.advanceTimeBy(FOUR_SECONDS)

        // Then nothing has moved
        assertThat(onRoot().captureToImage().asAndroidBitmap().sameAs(before)).isTrue()
    }

    @Test
    @DisplayName("the home screen no longer shows the settings, gone to their own page")
    fun theHomeScreenNoLongerShowsTheSettings() = extension.use {
        // Given
        showHomeScreen()

        // Then
        onNodeWithText("Réglages").assertDoesNotExist()
        onNodeWithText("Vibrations").assertDoesNotExist()
        onNodeWithText("Effets sonores").assertDoesNotExist()
    }

    @Test
    @DisplayName("Jouer, Écouter and the tiles bear an icon that TalkBack does not read")
    fun theButtonsWithALabelBearAnIconThatTalkBackDoesNotRead() = extension.use {
        // Given the minuet selected, which may be listened to
        showHomeScreen(uiState.copy(listenable = listOf(MusicChoice.MINUET)))

        // Then each bears one icon, decorative
        listOf(
            playIconOf(hasTestTag(PLAY_TAG)),
            listenIconOf(hasContentDescription("Écouter Menuet")),
            iconOf(tileIn("Mes musiques")),
            iconOf(tileIn("Composer une musique")),
        ).forEach { icon ->
            icon
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
        }
        // And TalkBack reads each button its label once
        onNodeWithContentDescription("Écouter Menuet")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Écouter Menuet")))
        listOf("Mes musiques", "Composer une musique").forEach { label ->
            onAllNodes(hasText(label), useUnmergedTree = true).assertCountEquals(1)
        }
    }

    @Test
    @DisplayName("the gear button is read Paramètres once, its icon adding nothing")
    fun theGearButtonIsReadOnce() = extension.use {
        // Given
        showHomeScreen()

        // Then the button alone is described, once
        onNodeWithContentDescription("Paramètres")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Paramètres")))
        onAllNodes(hasContentDescription("Paramètres"), useUnmergedTree = true).assertCountEquals(1)
        // And its icon is there, with nothing for TalkBack: neither a description nor a role
        onNode(
            hasTestTag(GLYPH_BUTTON_ICON_TAG) and hasAnyAncestor(hasContentDescription("Paramètres")),
            useUnmergedTree = true,
        )
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName(
        "in a font twice as large, on 360 dp, Jouer is shown whole beside its icon, and Écouter within the screen",
    )
    fun inALargeFontJouerIsShownWholeAndListenWithinTheScreen(kind: BloxThemeKind) = extension.use {
        // Given a font twice as large, on 360 dp, the minuet selected, which may be listened to
        showHomeScreen(
            uiState.copy(listenable = listOf(MusicChoice.MINUET)),
            width = NARROW_PHONE,
            fontScale = 2f,
            kind = kind,
        )

        // Then the label of Jouer is shown whole, on one line, beside its icon, within its button and the screen
        val play = hasTestTag(PLAY_TAG)
        val bounds = onNode(play).getUnclippedBoundsInRoot()
        val icon = playIconOf(play).getUnclippedBoundsInRoot()
        val layout = textLayout("Jouer")
        assertThat(layout.lineCount).isEqualTo(1)
        // Drawn within its box: its paragraph may be laid out wider than the text, so not `hasVisualOverflow`
        assertThat(layout.getLineRight(0)).isLessThanOrEqualTo(layout.size.width.toFloat())
        val shown = onNode(hasText("Jouer"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertThat(icon.right <= shown.left || icon.left >= shown.right).isTrue()
        assertThat(minOf(icon.left, shown.left)).isGreaterThanOrEqualTo(bounds.left)
        assertThat(maxOf(icon.right, shown.right)).isLessThanOrEqualTo(bounds.right)
        assertThat(bounds.right).isLessThanOrEqualTo(NARROW_PHONE)
        // And Écouter, a disc of 48 dp whatever the font, within the screen
        onNodeWithContentDescription("Écouter Menuet")
            .assertWidthIsEqualTo(BloxSizes.minTouchTarget)
            .assertHeightIsEqualTo(BloxSizes.minTouchTarget)
        assertThat(onNodeWithContentDescription("Écouter Menuet").getUnclippedBoundsInRoot().right)
            .isLessThanOrEqualTo(NARROW_PHONE)
    }

    /** A tile in the tree of its parts, unmerged: the button, its label under it. */
    private fun tileIn(label: String): SemanticsMatcher = hasClickAction() and hasAnyDescendant(hasText(label))

    /** The icon of the button [button] matches, alone in it. */
    private fun ComposeContext.iconOf(button: SemanticsMatcher): SemanticsNodeInteraction =
        onNode(hasTestTag(HOME_ICON_TAG) and hasAnyAncestor(button), useUnmergedTree = true)

    /** The triangle of Jouer, the button [button] matches (PlayButton of the design system). */
    private fun ComposeContext.playIconOf(button: SemanticsMatcher): SemanticsNodeInteraction =
        onNode(hasTestTag(PLAY_BUTTON_ICON_TAG) and hasAnyAncestor(button), useUnmergedTree = true)

    /** The icon of the listen button [button] matches, a disc of its icon alone. */
    private fun ComposeContext.listenIconOf(button: SemanticsMatcher): SemanticsNodeInteraction =
        onNode(hasTestTag(GLYPH_BUTTON_ICON_TAG) and hasAnyAncestor(button), useUnmergedTree = true)

    private fun ComposeContext.showHomeScreen(
        state: HomeUiState = uiState,
        width: Dp = Dp.Unspecified,
        fontScale: Float = 1f,
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
        effects: BloxEffects = STILL,
        music: MusicActions = MusicActions.None,
        onStartLevelChosen: (Int) -> Unit = {},
        onPlay: (Int) -> Unit = {},
        onCompose: () -> Unit = {},
        onMyMusics: () -> Unit = {},
        onSettings: () -> Unit = {},
        onChangeGame: () -> Unit = {},
        withoutInsets: Boolean = false,
    ) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = kind, effects = effects) {
                    HomeScreen(
                        modifier = Modifier
                            .width(width)
                            .then(
                                if (withoutInsets) Modifier.consumeWindowInsets(WindowInsets.safeDrawing) else Modifier,
                            ),
                        uiState = state,
                        onThemeChosen = {},
                        music = music,
                        onStartLevelChosen = onStartLevelChosen,
                        onPlay = onPlay,
                        onCompose = onCompose,
                        onMyMusics = onMyMusics,
                        onSettings = onSettings,
                        onChangeGame = onChangeGame,
                    )
                }
            }
        }
    }
}

/** A phone of 360 dp wide, as on the mockup: the narrowest of those BloX is drawn for. */
private val NARROW_PHONE = 360.dp

/** The name of a music file, as the picker shows it. */
private const val FILE_NAME = "orage-d-ete.ogg"

/** A name of a music file far too long for a line of the sheet. */
private const val LONG_FILE_NAME = "enregistrement-de-l-orage-du-soir-sur-le-lac-en-aout-version-longue.ogg"

/** The rounding of an edge to the pixel, in dp: far below a pixel. */
private const val PIXEL_ROUNDING = 0.01f

/** What « Vivante » is, under its name in the sheet. */
private const val LIVE_SUB_LINE = "Calme, puis tendue quand la pile monte"

/** The reminder under Jouer, at level 5 with the minuet. */
private const val REMINDER = "Classique · Niveau 05 · Menuet"

/**
 * The targets of the home, at least: Changer de jeu, Paramètres, Jouer, two modes, two themes, − and +, the row Musique,
 * Écouter, two tiles.
 */
private const val HOME_TARGETS = 13

/** Four seconds of the clock of the test, in milliseconds. */
private const val FOUR_SECONDS = 4_000L

/**
 * The reduced effects, those of the home in most of these tests: in full effects, the preview and the halo ask for a
 * frame at each frame, and would keep Compose from ever being idle while its clock runs by itself.
 */
private val STILL = BloxEffects(reduced = true, forcedBySystem = false)

/** Three seconds and a half: the first pulse of the halo half gone. */
private const val HALF_A_PULSE_IN = 3_500L

/** A second of the clock of the test, in milliseconds. */
private const val ONE_SECOND = 1_000L

/** The strip above Jouer where its halo shows: from 10 dp above it, 8 dp high, in the room between it and the hero. */
private val ABOVE_JOUER = 10.dp
private val STRIP = 8.dp

/** The tag of the box the sheet come in is drawn in, without its window. */
private const val SETTLED_SHEET = "settled_sheet"
