package ch.magori.blox.feature.musics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
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
            MusicCard(evening, "Thème du soir", Category.CALM, bpm = 80, inGame = true),
            MusicCard(march, "Ma marche", category = null, bpm = 132, inGame = false),
        ),
        gameMusic = "Thème du soir",
    )

    /** What the screen asked for, in order. */
    private val calls = mutableListOf<String>()

    @Test
    @DisplayName("each card tells the name, the category and the tempo of its music, or the bar played while listened")
    fun eachCardTellsItsMusic() = extension.use {
        // Given « Ma marche » listened to, at its third bar of eight
        showMyMusics(
            uiState.copy(
                musics = listOf(uiState.musics[0], uiState.musics[1].copy(listening = Listening(bar = 3, bars = 8))),
            ),
        )

        // Then
        onNodeWithText("Pendant les parties : Thème du soir").assertExists()
        onNodeWithText("Calme · 80 temps/min").assertExists()
        onNodeWithText("Lecture · mesure 3 sur 8").assertExists()
        onNodeWithText("Composée à la main · 132 temps/min").assertDoesNotExist()

        // When
        onNodeWithContentDescription("Écouter Thème du soir").performClick()
        onNodeWithContentDescription("Arrêter Ma marche").performClick()

        // Then
        runOnIdle { assertThat(calls).containsExactly("listen evening", "listen march") }
    }

    @Test
    @DisplayName("each category is named on its card as the specification names it, a music by hand Composée à la main")
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
            null to "Composée à la main",
        )
        assertThat(names.keys).containsAll(Category.entries)
        showMyMusics(
            uiState.copy(
                musics = names.keys.mapIndexed { index, category ->
                    MusicCard(
                        ScoreId.Personal("m$index"),
                        "Musique $index",
                        category,
                        bpm = 100 + index,
                        inGame = false,
                    )
                },
            ),
        )

        // Then
        names.values.forEachIndexed { index, name ->
            val info = "$name · ${100 + index} temps/min"
            onNode(hasScrollToNodeAction()).performScrollToNode(hasText(info))
            onNodeWithText(info).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("Utiliser and En jeu are read as a switch, on for the music played during the games, and choose it")
    fun useAndInGameAreASwitch() = extension.use {
        // Given
        showMyMusics()

        // Then
        onNodeWithContentDescription("Thème du soir pendant les parties")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOn()
        onNodeWithContentDescription("Ma marche pendant les parties").assertIsOff()

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
                musics = List(12) { MusicCard(ScoreId.Personal("m$it"), "Musique ${it + 1}", null, 100, false) },
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
        onNodeWithText("L'enregistrement a échoué.").assertExists()

        // When
        state = uiState.copy(dialog = MyMusicsDialog.ConfirmDelete(march, "Ma marche", inGame = false, failed = true))

        // Then
        onNodeWithText("L'enregistrement a échoué.").assertExists()
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
    @DisplayName("without a music, the list invites to compose one, and the games play the music of the home screen")
    fun withoutAMusicTheListInvitesToComposeOne() = extension.use {
        // Given
        showMyMusics(MyMusicsUiState(musics = emptyList(), gameMusic = null))

        // Then
        onNodeWithText("Aucune musique pour l'instant. Compose la tienne, ou laisse le dé t'en proposer une.")
            .assertExists()
        onNodeWithText("Pendant les parties : la musique choisie à l'accueil").assertExists()
    }

    @Test
    @DisplayName("every button is 48 dp wide and high at least")
    fun everyButtonIsLargeEnough() = extension.use {
        // Given
        showMyMusics(uiState.copy(deleted = "Ma marche"))

        // Then
        val targets = onAllNodes(hasClickAction())
        val count = targets.fetchSemanticsNodes().size
        assertThat(count).isGreaterThan(1)
        repeat(count) { index ->
            targets[index].assertWidthIsAtLeast(
                BloxSizes.minTouchTarget,
            ).assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        }
    }

    private fun ComposeContext.showMyMusics(
        state: MyMusicsUiState = uiState,
        renameProblem: (String) -> NameProblem? = { null },
    ) = setMyMusics(renameProblem) { state }

    /** Shows the list in the state [state] gives, read again when it changes. */
    private fun ComposeContext.setMyMusics(
        renameProblem: (String) -> NameProblem? = { null },
        state: () -> MyMusicsUiState,
    ) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                MyMusicsScreen(
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
}
