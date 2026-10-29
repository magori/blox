package ch.magori.blox.feature.musics

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.designsystem.component.GLYPH_BUTTON_ICON_TAG
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.ScoreId
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
internal class MyMusicsScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val evening = ScoreId.Personal("evening")

    private val march = ScoreId.Personal("march")

    private val uiState = MyMusicsUiState(
        musics = listOf(
            aCard(evening, "Thème du soir", Category.CALM, key = "fa majeur", inGame = true),
            aCard(
                march,
                "Ma marche",
                null,
                key = "la mineur",
                length = "0:15",
                spokenLength = "15 secondes",
            ),
        ),
        gameMusic = "Thème du soir",
    )

    /** What the screen asked for, in order. */
    private val calls = mutableListOf<String>()

    @Test
    @DisplayName("each card tells the name, style, key and length of its music, or the bar played while listened")
    fun eachCardTellsItsMusic() = extension.use {
        // Given « Ma marche » listened to, at its third bar of eight
        showMyMusics(
            uiState.copy(
                musics = listOf(uiState.musics[0], uiState.musics[1].copy(listening = Listening(bar = 3, bars = 8))),
            ),
        )

        // Then
        onNodeWithContentDescription("Lecture, mesure 3 sur 8", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertShows("Lecture · mesure 3 sur 8")
        onAllNodesWithContentDescription("la mineur", substring = true, useUnmergedTree = true).assertCountEquals(0)
        // The info lines as TalkBack says them, in words
        onNodeWithContentDescription("Calme, fa majeur, 24 secondes", substring = true).assertExists()
        onNodeWithContentDescription("Lecture, mesure 3 sur 8", substring = true).assertExists()
        onNodeWithContentDescription("la mineur", substring = true).assertDoesNotExist()

        // When
        onNodeWithContentDescription("Écouter Thème du soir").performClick()
        onNodeWithContentDescription("Arrêter Ma marche").performClick()

        // Then
        runOnIdle { assertThat(calls).containsExactly("listen evening", "listen march") }
    }

    @Test
    @DisplayName(
        "listening to a music shows the equalizer and the bar played, and a gauge of eight bars that TalkBack leaves out",
    )
    fun listeningShowsTheEqualizerAndTheGauge() = extension.use {
        // Given « Ma marche » listened to, at its third bar of eight
        val listened = uiState.copy(
            musics = listOf(uiState.musics[0], uiState.musics[1].copy(listening = Listening(bar = 3, bars = 8))),
        )
        var state by mutableStateOf(listened)
        setMyMusics { state }

        // Then its row alone bears the equalizer, beside the bar played, and the gauge of its eight bars
        val inMarch = hasAnyAncestor(holding("Ma marche"))
        onNode(hasTestTag(MUSICS_EQUALIZER_TAG) and inMarch, useUnmergedTree = true).assertIsDisplayed()
        onAllNodes(hasTestTag(MUSICS_EQUALIZER_TAG), useUnmergedTree = true).assertCountEquals(1)
        onNodeWithContentDescription("Lecture, mesure 3 sur 8", useUnmergedTree = true)
            .assertShows("Lecture · mesure 3 sur 8")
        onNode(hasTestTag(MUSICS_GAUGE_TAG), useUnmergedTree = true)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility))
        onAllNodes(hasTestTag(MUSICS_GAUGE_BAR_TAG), useUnmergedTree = true).assertCountEquals(8)

        // When the music stops
        state = uiState

        // Then
        onAllNodes(hasTestTag(MUSICS_EQUALIZER_TAG), useUnmergedTree = true).assertCountEquals(0)
        onAllNodes(hasTestTag(MUSICS_GAUGE_TAG), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    @DisplayName(
        "the card Pendant les parties names the music played during the games, or the music chosen on the home screen",
    )
    fun theCardPendantLesPartiesNamesTheGameMusic() = extension.use {
        // Given
        var state by mutableStateOf(uiState)
        setMyMusics { state }

        // Then the card shows its label in capitals and the name, read as one sentence
        onNodeWithContentDescription("Pendant les parties : Thème du soir").assertIsDisplayed()
        onNodeWithText("PENDANT LES PARTIES", useUnmergedTree = true).assertIsDisplayed()

        // When no music of the player is played during the games
        state = uiState.copy(gameMusic = null)

        // Then
        onNodeWithContentDescription("Pendant les parties : la musique choisie à l'accueil").assertIsDisplayed()
        onNodeWithText("La musique choisie à l'accueil", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    @DisplayName("a row is read as listen, the name that opens the editor, the switch and the menu, in this order")
    fun aRowIsReadInOrder() = extension.use {
        // Given
        showMyMusics()

        // Then the buttons of the row of Thème du soir, in the order TalkBack reads them, from left to right
        val buttons = onAllNodes(
            hasClickAction() and
                (hasText("Thème du soir") or hasContentDescription("Thème du soir", substring = true)),
        ).fetchSemanticsNodes()
        assertThat(buttons.map { it.spokenLabel() }).containsExactly(
            "Écouter Thème du soir",
            "Thème du soir",
            "Thème du soir pendant les parties",
            "Plus d'actions pour Thème du soir",
        )
        assertThat(buttons.map { it.boundsInRoot.left }).isSorted()
        assertThat(buttons.map { it.config.getOrNull(SemanticsProperties.Role) }).containsOnly(Role.Button)
        onNode(hasClickAction() and hasText("Thème du soir")).assertClickLabel("Modifier")
    }

    @Test
    @DisplayName("each card shows its style, its key and its length on one line, without its tempo nor its last change")
    fun eachCardShowsStyleKeyAndLengthOnOneLine() = extension.use {
        // Given
        showMyMusics()

        // Then the line shows them with « · » and « 0:24 »
        onNodeWithContentDescription("Calme, fa majeur, 24 secondes", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertShows("Calme · fa majeur · 0:24")
        onNodeWithContentDescription("Libre, la mineur, 15 secondes", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertShows("Libre · la mineur · 0:15")
        onAllNodesWithText("temps", substring = true, useUnmergedTree = true).assertCountEquals(0)
        onAllNodesWithText("Modifiée", substring = true, useUnmergedTree = true).assertCountEquals(0)
        // TalkBack, which reads the button that opens the editor as a whole, says them in words only
        onNodeWithText("Ma marche").assertContentDescriptionContains("Libre, la mineur, 15 secondes", substring = true)
        onAllNodesWithText("·", substring = true).assertCountEquals(0)
        onAllNodesWithText("0:24", substring = true).assertCountEquals(0)
        onAllNodesWithContentDescription("0:24", substring = true).assertCountEquals(0)
    }

    @Test
    @DisplayName("a row is as high as its buttons, a long name on one line, Utiliser a disc of 48 dp")
    fun aRowIsAsHighAsItsButtons() = extension.use {
        // Given
        showMyMusics(uiState.copy(musics = listOf(uiState.musics[0], uiState.musics[1].copy(name = LONG_NAME))))

        // Then the name and its info line, which open the editor, are as high as the buttons beside them
        onNodeWithText(LONG_NAME).assertHeightIsEqualTo(BloxSizes.minTouchTarget)
        onNodeWithText(LONG_NAME, useUnmergedTree = true).assertOnOneLineCutShort()
        onNodeWithContentDescription("$LONG_NAME pendant les parties").assertIsSquareOfTouchTarget()
        // A glyph, not a word
        onNodeWithText("Utiliser").assertDoesNotExist()
        onNodeWithText("En jeu").assertDoesNotExist()
    }

    @Test
    @DisplayName("with a font twice as large, the name and the info of a card stay on one line each, cut short")
    fun withALargeFontTheCardKeepsOneLineEach() = extension.use {
        // Given
        val long = uiState.musics[1].copy(name = LONG_NAME, key = "fa dièse mixolydien")
        showMyMusics(uiState.copy(musics = listOf(long)), fontScale = 2f)

        // Then
        onNodeWithText(LONG_NAME, useUnmergedTree = true).assertIsDisplayed().assertOnOneLineCutShort()
        onNodeWithContentDescription("Libre, fa dièse mixolydien, 15 secondes", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertShows("Libre · fa dièse mixolydien · 0:15")
            .assertOnOneLineCutShort()
        // The buttons beside them keep their size
        onNodeWithContentDescription("Écouter $LONG_NAME").assertIsSquareOfTouchTarget()
        onNodeWithContentDescription("$LONG_NAME pendant les parties").assertIsSquareOfTouchTarget()
        onNodeWithContentDescription("Plus d'actions pour $LONG_NAME").assertIsSquareOfTouchTarget()
    }

    @Test
    @DisplayName("each category is named on its card as the specification names it, a music by hand Libre")
    fun eachCategoryIsNamedAsTheSpecificationNamesIt() = extension.use {
        // Given a card per category, and one of a music written by hand
        val names = mapOf(
            Category.JOYFUL to "Joyeux",
            Category.CALM to "Calme",
            Category.EPIC to "Épique",
            Category.MELANCHOLIC to "Mélancolique",
            Category.MYSTERIOUS to "Mystérieux",
            Category.RETRO to "Rétro",
            Category.DANCE to "Danse",
            null to "Libre",
        )
        assertThat(names.keys).containsAll(Category.entries)
        showMyMusics(
            uiState.copy(
                musics = names.keys.mapIndexed { index, category ->
                    aCard(ScoreId.Personal("m$index"), "Musique $index", category, key = "do majeur")
                },
            ),
        )

        // Then
        names.values.forEach { name ->
            val info = "$name, do majeur, "
            onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription(info, substring = true))
            onNodeWithContentDescription(info, substring = true).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName(
        "the music played during the games bears EN JEU and its button is selected, each button saying what it does",
    )
    fun theMusicInGameBearsEnJeuAndItsButtonIsSelected() = extension.use {
        // Given
        showMyMusics()

        // Then its name bears EN JEU, which TalkBack leaves to the button beside it, read as selected
        onNode(hasTestTag(MUSICS_IN_GAME_TAG) and hasAnyAncestor(holding("Thème du soir")), useUnmergedTree = true)
            .assertIsDisplayed()
        onAllNodes(hasTestTag(MUSICS_IN_GAME_TAG), useUnmergedTree = true).assertCountEquals(1)
        onNode(hasClickAction() and hasText("Thème du soir"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString("Thème du soir"))))
        onNodeWithContentDescription("Thème du soir pendant les parties")
            .assertIsSelected()
            .assertClickLabel("Ne plus la jouer pendant les parties")
        onNodeWithContentDescription("Ma marche pendant les parties")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
            .assertClickLabel("La jouer pendant les parties")

        // When
        onNodeWithContentDescription("Ma marche pendant les parties").performClick()
        onNodeWithContentDescription("Thème du soir pendant les parties").performClick()

        // Then
        runOnIdle { assertThat(calls).containsExactly("use march", "use evening") }
    }

    @Test
    @DisplayName("the back button and the title stay in place when the list scrolls")
    fun theHeaderStaysInPlace() = extension.use {
        // Given more musics than the screen holds
        showMyMusics(
            uiState.copy(
                musics = List(12) { aCard(ScoreId.Personal("m$it"), "Musique ${it + 1}", category = null) },
            ),
        )

        // When
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Musique 12"))

        // Then
        onNodeWithContentDescription("Retour à l'accueil").assertIsDisplayed()
        onNodeWithText("Mes musiques").assertIsDisplayed()
    }

    @Test
    @DisplayName("the menu of a card renames the music, chooses it for the games, or deletes it")
    fun theMenuOfACardRenamesChoosesOrDeletes() = extension.use {
        // Given
        showMyMusics()

        // When
        onNodeWithContentDescription("Plus d'actions pour Ma marche").performClick()
        onNodeWithText("Renommer").performClick()
        onNodeWithContentDescription("Plus d'actions pour Ma marche").performClick()
        onNodeWithText("La jouer pendant les parties").performClick()
        onNodeWithContentDescription("Plus d'actions pour Thème du soir").performClick()
        onNodeWithText("Ne plus la jouer pendant les parties").assertExists()
        onNodeWithText("Supprimer").performClick()

        // Then
        runOnIdle { assertThat(calls).containsExactly("rename march", "use march", "delete evening") }
    }

    @Test
    @DisplayName("from the keyboard, Renommer closed gives the focus back to the menu « … » of its card")
    fun renommerClosedGivesTheFocusBackToTheMenu() = extension.use {
        // Given the keyboard in use, the list shown
        var state by mutableStateOf(uiState)
        setContent {
            val inputMode = LocalInputModeManager.current
            LaunchedEffect(Unit) { inputMode.requestInputMode(InputMode.Keyboard) }
            ThemedMyMusics(renameProblem = { null }, kind = BloxThemeKind.COLORFUL) { state }
        }
        onNodeWithContentDescription("Plus d'actions pour Ma marche").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Renommer").performSemanticsAction(SemanticsActions.OnClick)
        state = uiState.copy(dialog = MyMusicsDialog.Rename(march, "Ma marche"))
        onNode(hasSetTextAction()).assertExists()

        // When the window Renommer closes
        state = uiState

        // Then
        onNodeWithContentDescription("Plus d'actions pour Ma marche").assertIsFocused()
    }

    @Test
    @DisplayName("touching the name of a music, or Modifier in its menu, opens the music in the editor")
    fun theNameOrModifierOpensTheMusicInTheEditor() = extension.use {
        // Given
        showMyMusics()

        // When
        onNodeWithText("Ma marche").performClick()
        onNodeWithContentDescription("Plus d'actions pour Thème du soir").performClick()
        onNodeWithText("Modifier").performClick()

        // Then
        runOnIdle { assertThat(calls).containsExactly("edit march", "edit evening") }
    }

    @Test
    @DisplayName("the dialog Renommer refuses a name taken, saying why, and renames with a free one")
    fun theRenameDialogRefusesANameTaken() = extension.use {
        // Given
        showMyMusics(
            uiState.copy(dialog = MyMusicsDialog.Rename(march, "Ma marche")),
            renameProblem = { name ->
                if (name.trim().equals("Thème du soir", ignoreCase = true)) NameProblem.TAKEN else null
            },
        )

        // When
        onNode(hasSetTextAction()).performTextReplacement("thème du soir")

        // Then
        onNodeWithText("Une autre musique porte déjà ce nom.").assertExists()
        onNodeWithText("Renommer", useUnmergedTree = true).assertExists()
        onNode(hasClickAction() and hasText("Renommer")).assertIsNotEnabled()

        // When
        onNode(hasSetTextAction()).performTextReplacement("Ma grande marche")
        onNode(hasClickAction() and hasText("Renommer")).assertIsEnabled().performClick()
        onNodeWithText("Annuler").performClick()

        // Then
        runOnIdle { assertThat(calls).containsExactly("renamed Ma grande marche", "dismissed") }
    }

    @Test
    @DisplayName("the confirmation of a deletion warns that the music of the home screen replaces the one in game")
    fun theConfirmationOfADeletionWarnsOfTheMusicInGame() = extension.use {
        // Given
        showMyMusics(uiState.copy(dialog = MyMusicsDialog.ConfirmDelete(evening, "Thème du soir", inGame = true)))

        // Then
        onNodeWithText("Supprimer « Thème du soir » ?").assertExists()
        onNodeWithText(
            "Elle est jouée pendant les parties : la musique choisie à l'accueil la remplacera. " +
                "Tu pourras annuler juste après.",
        ).assertExists()
    }

    @Test
    @DisplayName("Supprimer, in the confirmation, deletes the music")
    fun supprimerDeletesTheMusic() = extension.use {
        // Given
        showMyMusics(uiState.copy(dialog = MyMusicsDialog.ConfirmDelete(march, "Ma marche", inGame = false)))

        // When
        onNodeWithText("Supprimer").performClick()

        // Then
        runOnIdle { assertThat(calls).containsExactly("delete confirmed") }
    }

    @Test
    @DisplayName("Garder la musique, in the confirmation, keeps it")
    fun garderLaMusiqueKeepsIt() = extension.use {
        // Given
        showMyMusics(uiState.copy(dialog = MyMusicsDialog.ConfirmDelete(march, "Ma marche", inGame = false)))

        // When
        onNodeWithText("Garder la musique").performClick()

        // Then
        runOnIdle { assertThat(calls).containsExactly("dismissed") }
    }

    @Test
    @DisplayName("a rename or a deletion that could not be written is told in its dialog, still open")
    fun aWriteThatFailedIsToldInItsDialog() = extension.use {
        // Given
        var state by mutableStateOf(uiState.copy(dialog = MyMusicsDialog.Rename(march, "Ma marche", failed = true)))
        setMyMusics { state }

        // Then
        onNodeWithText("L'opération a échoué.").assertExists()

        // When
        state = uiState.copy(dialog = MyMusicsDialog.ConfirmDelete(march, "Ma marche", inGame = false, failed = true))

        // Then
        onNodeWithText("L'opération a échoué.").assertExists()
    }

    @Test
    @DisplayName("a name too long is refused with the most characters a name may have")
    fun aNameTooLongIsRefused() = extension.use {
        // Given
        showMyMusics(
            uiState.copy(dialog = MyMusicsDialog.Rename(march, "Ma marche")),
            renameProblem = { NameProblem.TOO_LONG },
        )

        // Then
        onNodeWithText("40 caractères au plus.").assertExists()
    }

    @Test
    @DisplayName("a name pasted too long is cut at the most characters a name may have, 40 emoji kept whole")
    fun aNamePastedTooLongIsCut() = extension.use {
        // Given
        showMyMusics(uiState.copy(dialog = MyMusicsDialog.Rename(march, "Ma marche")))
        val longest = "x".repeat(SavedMusic.MAX_NAME_LENGTH)
        val notes = "\uD83C\uDFB5".repeat(SavedMusic.MAX_NAME_LENGTH)

        // When
        onNode(hasSetTextAction()).performTextReplacement(longest + "yz")
        onNode(hasClickAction() and hasText("Renommer")).performClick()
        onNode(hasSetTextAction()).performTextReplacement(notes)
        onNode(hasClickAction() and hasText("Renommer")).performClick()

        // Then
        runOnIdle { assertThat(calls).containsExactly("renamed $longest", "renamed $notes") }
    }

    @Test
    @DisplayName("after a deletion, Annuler puts the music back, and the cross closes the message")
    fun afterADeletionAnnulerPutsTheMusicBack() = extension.use {
        // Given
        showMyMusics(uiState.copy(deleted = "Ma marche"))

        // Then
        onNodeWithText("« Ma marche » est supprimée.").assertExists()

        // When
        onNodeWithText("Annuler").performClick()
        onNodeWithContentDescription("Fermer le message").performClick()

        // Then
        runOnIdle { assertThat(calls).containsExactly("undo", "undo dismissed") }
    }

    @Test
    @DisplayName("Au hasard and Nouvelle open the editor, and the back button goes back home")
    fun theButtonsOpenTheEditorOrGoBack() = extension.use {
        // Given
        showMyMusics()

        // When
        onNodeWithText("Au hasard").performClick()
        onNodeWithText("Nouvelle").performClick()
        onNodeWithContentDescription("Retour à l'accueil").performClick()

        // Then
        runOnIdle { assertThat(calls).containsExactly("random", "new", "back") }
    }

    @Test
    @DisplayName("without music the page says « Aucune musique pour l'instant. » and keeps Au hasard and Nouvelle")
    fun withoutMusicThePageSaysSoAndKeepsItsButtons() = extension.use {
        // Given
        showMyMusics(MyMusicsUiState(musics = emptyList(), gameMusic = null))

        // Then the page says it on its own line, a heading for TalkBack, and invites to compose one
        onNodeWithText("Aucune musique pour l'instant.")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithText("Compose la tienne, ou laisse le dé t'en proposer une.").assertIsDisplayed()
        onNode(hasClickAction() and hasText("Au hasard")).assertIsDisplayed()
        onNode(hasClickAction() and hasText("Nouvelle")).assertIsDisplayed()
    }

    @Test
    @DisplayName("every target of the page is 48 dp at least on 360 dp, in a font twice as large")
    fun everyTargetIsLargeEnough() = extension.use {
        // Given a music listened to, the message of a deletion, a font twice as large, on 360 dp
        val listened = uiState.musics[1].copy(listening = Listening(bar = 3, bars = 8))
        showMyMusics(
            uiState.copy(musics = listOf(uiState.musics[0], listened), deleted = "Ma marche"),
            fontScale = 2f,
            width = 360.dp,
        )

        // Then each button of the bar, of the rows, of the message and of the foot
        val targets = onAllNodes(hasClickAction())
        val count = targets.fetchSemanticsNodes().size
        assertThat(count).isEqualTo(BAR_TARGETS + 2 * ROW_TARGETS + MESSAGE_TARGETS + FOOT_TARGETS)
        repeat(count) { index ->
            targets[index].assertWidthIsAtLeast(
                BloxSizes.minTouchTarget,
            ).assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        }
    }

    @Test
    @DisplayName("a foot higher than its room stays under the card of the games, and scrolls to Nouvelle")
    fun aFootHigherThanItsRoomStaysUnderTheCard() = extension.use {
        // Given the message of a deletion, in a font twice as large, on a window too low for the whole foot
        showMyMusics(uiState.copy(deleted = "Ma marche"), fontScale = 2f, width = 360.dp, height = 420.dp)

        // Then the foot does not cover the card above it
        val card = onNodeWithContentDescription("Pendant les parties : Thème du soir").getUnclippedBoundsInRoot()
        val message = onNodeWithText("« Ma marche » est supprimée.", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertThat(message.top).isGreaterThanOrEqualTo(card.bottom)

        // And its last button can be reached
        onNode(hasClickAction() and hasText("Nouvelle")).performScrollTo().assertIsDisplayed()
    }

    @Test
    @DisplayName("Au hasard and Nouvelle bear an icon that TalkBack does not read, their label read alone")
    fun auHasardAndNouvelleBearAnIconThatTalkBackDoesNotRead() = extension.use {
        // Given
        showMyMusics()

        // Then
        listOf("Au hasard", "Nouvelle").forEach { label ->
            val button = hasClickAction() and hasText(label)
            iconOf(holding(label)).assertIsDecorative()
            onNode(button)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString(label))))
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        }
    }

    @Test
    @DisplayName("the listen button and Utiliser of a card are read once, their icons adding nothing")
    fun theListenButtonAndUtiliserAreReadOnce() = extension.use {
        // Given Ma marche, neither listened to nor played during the games
        showMyMusics()

        // Then each button is read by its description alone, once, and bears its icon, decorative
        listOf("Écouter Ma marche", "Ma marche pendant les parties").forEach { description ->
            onNodeWithContentDescription(description)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(description)))
            onAllNodes(hasContentDescription(description), useUnmergedTree = true).assertCountEquals(1)
            onNode(
                hasTestTag(GLYPH_BUTTON_ICON_TAG) and hasAnyAncestor(hasContentDescription(description)),
                useUnmergedTree = true,
            ).assertIsDecorative()
        }
    }

    @ParameterizedTest(name = "in {0}")
    @EnumSource(BloxThemeKind::class)
    @DisplayName("in a font twice as large, on 360 dp, Au hasard and Nouvelle are shown whole, after their icons")
    fun inALargeFontAuHasardAndNouvelleAreShownWhole(kind: BloxThemeKind) = extension.use {
        // Given a font twice as large, on 360 dp, the width of the mockup
        showMyMusics(fontScale = 2f, kind = kind, width = 360.dp)

        // Then each label is shown on one line, not cut, after its icon, within its button
        listOf("Au hasard", "Nouvelle").forEach { label ->
            val button = hasClickAction() and hasText(label)
            val bounds = onNode(button).getUnclippedBoundsInRoot()
            val icon = iconOf(holding(label)).getUnclippedBoundsInRoot()
            val text = onNode(hasText(label), useUnmergedTree = true)
            val layout = text.layout()
            assertThat(layout.lineCount).isEqualTo(1)
            assertThat(layout.hasVisualOverflow).isFalse()
            val shown = text.getUnclippedBoundsInRoot()
            assertThat(icon.left).isGreaterThanOrEqualTo(bounds.left)
            assertThat(icon.right).isLessThanOrEqualTo(shown.left)
            assertThat(shown.right).isLessThanOrEqualTo(bounds.right)
        }
    }

    /** The icon of the button [button] matches, alone in it. */
    private fun ComposeContext.iconOf(button: SemanticsMatcher): SemanticsNodeInteraction =
        onNode(hasTestTag(MUSICS_ICON_TAG) and hasAnyAncestor(button), useUnmergedTree = true)

    /** The button that holds the text [label], as the unmerged tree sees it: its text is one of its children. */
    private fun holding(label: String): SemanticsMatcher = hasClickAction() and hasAnyDescendant(hasText(label))

    /** The card of a music of eight bars. */
    private fun aCard(
        id: ScoreId.Personal,
        name: String,
        category: Category?,
        key: String = "do majeur",
        length: String = "0:24",
        spokenLength: String = "24 secondes",
        inGame: Boolean = false,
    ): MusicCard = MusicCard(
        id = id,
        name = name,
        category = category,
        key = key,
        length = length,
        spokenLength = spokenLength,
        inGame = inGame,
    )

    private fun ComposeContext.showMyMusics(
        state: MyMusicsUiState = uiState,
        renameProblem: (String) -> NameProblem? = { null },
        fontScale: Float = 1f,
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
        width: Dp = Dp.Unspecified,
        height: Dp = Dp.Unspecified,
    ) = setMyMusics(renameProblem, fontScale, kind, width, height) { state }

    /**
     * Shows the list in the state [state] gives, read again when it changes, its font [fontScale] times as large, in the
     * theme [kind], [width] wide and [height] high.
     */
    private fun ComposeContext.setMyMusics(
        renameProblem: (String) -> NameProblem? = { null },
        fontScale: Float = 1f,
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
        width: Dp = Dp.Unspecified,
        height: Dp = Dp.Unspecified,
        state: () -> MyMusicsUiState,
    ) {
        setContent {
            val density = Density(LocalDensity.current.density, fontScale)
            CompositionLocalProvider(LocalDensity provides density) {
                ThemedMyMusics(renameProblem, kind, Modifier.width(width).height(height), state)
            }
        }
    }

    /** The list in the theme [kind], its actions written down in [calls]. */
    @Composable
    private fun ThemedMyMusics(
        renameProblem: (String) -> NameProblem?,
        kind: BloxThemeKind,
        modifier: Modifier = Modifier,
        state: () -> MyMusicsUiState,
    ) {
        BloxTheme(kind = kind) {
            MyMusicsScreen(
                modifier = modifier,
                uiState = state(),
                actions = object : MyMusicsActions {
                    override fun onBack() {
                        calls += "back"
                    }

                    override fun onListen(id: ScoreId.Personal) {
                        calls += "listen ${id.key}"
                    }

                    override fun onUse(id: ScoreId.Personal) {
                        calls += "use ${id.key}"
                    }

                    override fun onEdit(id: ScoreId.Personal) {
                        calls += "edit ${id.key}"
                    }

                    override fun onRename(id: ScoreId.Personal) {
                        calls += "rename ${id.key}"
                    }

                    override fun renameProblem(name: String): NameProblem? = renameProblem(name)

                    override fun onRenamed(name: String) {
                        calls += "renamed $name"
                    }

                    override fun onDelete(id: ScoreId.Personal) {
                        calls += "delete ${id.key}"
                    }

                    override fun onDeleteConfirmed() {
                        calls += "delete confirmed"
                    }

                    override fun onDialogDismissed() {
                        calls += "dismissed"
                    }

                    override fun onUndo() {
                        calls += "undo"
                    }

                    override fun onUndoDismissed() {
                        calls += "undo dismissed"
                    }

                    override fun onRandom() {
                        calls += "random"
                    }

                    override fun onNew() {
                        calls += "new"
                    }
                },
            )
        }
    }
}

/** A name too long for a card at its width: « Ballades pour une longue soirée en hiver ». */
private const val LONG_NAME = "Ballades pour une longue soirée en hiver"

/** The text of this node as it is laid out: what it shows, whatever TalkBack reads. */
private fun SemanticsNodeInteraction.layout(): TextLayoutResult {
    val layouts = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
    return layouts.single()
}

/** Asserts that TalkBack says what touching this node does: [label]. */
private fun SemanticsNodeInteraction.assertClickLabel(label: String): SemanticsNodeInteraction {
    assertThat(fetchSemanticsNode().config[SemanticsActions.OnClick].label).isEqualTo(label)
    return this
}

/** What names this node first for TalkBack: its text, else its description. */
private fun SemanticsNode.spokenLabel(): String? = config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
    ?: config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()

/** Asserts that this node shows [text]. */
private fun SemanticsNodeInteraction.assertShows(text: String): SemanticsNodeInteraction {
    assertThat(layout().layoutInput.text.text).isEqualTo(text)
    return this
}

/** Asserts that the text of this node takes one line, cut short by an ellipsis. */
private fun SemanticsNodeInteraction.assertOnOneLineCutShort(): SemanticsNodeInteraction {
    val layout = layout()
    assertThat(layout.lineCount).isEqualTo(1)
    assertThat(layout.isLineEllipsized(0)).isTrue()
    return this
}

/** Asserts that this node is a square of the touch target, 48 dp. */
private fun SemanticsNodeInteraction.assertIsSquareOfTouchTarget(): SemanticsNodeInteraction =
    assertWidthIsEqualTo(BloxSizes.minTouchTarget).assertHeightIsEqualTo(BloxSizes.minTouchTarget)

/** Asserts that this icon is decorative: TalkBack has nothing to read on it, neither a description nor a role. */
private fun SemanticsNodeInteraction.assertIsDecorative(): SemanticsNodeInteraction =
    assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))

/**
 * The targets of the page: Retour; on a row, Écouter, the name, the switch and « … »; Annuler and the cross; Au hasard
 * and Nouvelle.
 */
private const val BAR_TARGETS = 1
private const val ROW_TARGETS = 4
private const val MESSAGE_TARGETS = 2
private const val FOOT_TARGETS = 2
