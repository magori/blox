package ch.magori.blox.feature.musics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.designsystem.component.ButtonEmphasis
import ch.magori.blox.core.designsystem.component.Equalizer
import ch.magori.blox.core.designsystem.component.FocusAfterDeletion
import ch.magori.blox.core.designsystem.component.FocusableBlock
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.Glyphs
import ch.magori.blox.core.designsystem.component.ListenButton
import ch.magori.blox.core.designsystem.component.PillBadge
import ch.magori.blox.core.designsystem.component.RowMenu
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.component.rowMenuDescription
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.ScoreId
import kotlin.math.min

/** The tag of the icons of « Mes musiques », decorative, read by the tests only. */
internal const val MUSICS_ICON_TAG = "musics_icon"

/** The tags of the equalizer, of the gauge of a music listened to and of each of its bars, read by the tests only. */
internal const val MUSICS_EQUALIZER_TAG = "musics_equalizer"
internal const val MUSICS_GAUGE_TAG = "musics_gauge"
internal const val MUSICS_GAUGE_BAR_TAG = "musics_gauge_bar"

/** The tag of the pill « EN JEU », read by the tests only: TalkBack leaves it out. */
internal const val MUSICS_IN_GAME_TAG = "musics_in_game"

/** What the player does on « Mes musiques »: the screen hands each action over, and decides nothing. */
@Stable
internal interface MyMusicsActions {
    /** The back button: home. */
    fun onBack()

    /** *Écouter*, or *Arrêter*, the music of [id]. */
    fun onListen(id: ScoreId.Personal)

    /** *Utiliser*, or *En jeu*, on the music of [id]; or its item in the menu. */
    fun onUse(id: ScoreId.Personal)

    /** The name of the music of [id] touched, or *Modifier* in its menu: the music opens in the editor. */
    fun onEdit(id: ScoreId.Personal)

    /** *Renommer*, in the menu of the music of [id]. */
    fun onRename(id: ScoreId.Personal)

    /** Why the music being renamed cannot be called [name]; null when it can. */
    fun renameProblem(name: String): NameProblem?

    /** The dialog *Renommer* confirmed with [name]. */
    fun onRenamed(name: String)

    /** *Supprimer*, in the menu of the music of [id]. */
    fun onDelete(id: ScoreId.Personal)

    /** *Supprimer*, in the confirmation. */
    fun onDeleteConfirmed()

    /** The dialog open cancelled. */
    fun onDialogDismissed()

    /** « Annuler », after a deletion. */
    fun onUndo()

    /** *Fermer le message* of the deletion. */
    fun onUndoDismissed()

    /** *Au hasard*. */
    fun onRandom()

    /** *Nouvelle*. */
    fun onNew()
}

/**
 * « Mes musiques » (V32): the bar, Retour and the title, and the card of the music played during the games, which stay
 * in place; a row per saved music, to listen to it, choose it for the games, rename or delete it from its menu « … »;
 * or, without music, a word to start; and, stuck at the bottom on a fade, the message that undoes a deletion, *Au
 * hasard* and *Nouvelle*, which open the editor. Once a music is deleted, the keyboard focus goes to the menu « … » of
 * the card under it, else above it, else to the page without music ([FocusAfterDeletion]).
 */
@Composable
internal fun MyMusicsScreen(
    uiState: MyMusicsUiState,
    actions: MyMusicsActions,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val empty = remember { FocusRequester() }
    // The menu « … » of each music, which a card composed later finds again. The table is written during the
    // composition of the cards (`getOrPut`): harmless, as it is remembered and a key always gives the same requester,
    // whichever composition asks first; it is purged of the musics gone after each composition (`SideEffect`).
    val menus = remember { HashMap<ScoreId.Personal, FocusRequester>() }
    val menuOf: (ScoreId.Personal) -> FocusRequester = remember { { id -> menus.getOrPut(id) { FocusRequester() } } }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BloxTheme.palette.page)
            .safeDrawingPadding()
            .padding(start = BloxSpaces.space4, top = BloxSpaces.space2, end = BloxSpaces.space4),
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
    ) {
        Header(onBack = actions::onBack)
        GameMusicCard(gameMusic = uiState.gameMusic)
        AboveBottomActions(
            modifier = Modifier.weight(1f),
            bottom = {
                BottomActions(
                    deleted = uiState.deleted,
                    actions = actions,
                )
            },
        ) { bottomRoom ->
            if (uiState.musics.isEmpty()) {
                EmptyList(focus = empty, modifier = Modifier.padding(bottom = bottomRoom))
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = bottomRoom),
                    verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
                ) {
                    items(uiState.musics, key = { it.id.key }) { card ->
                        MusicRow(
                            card = card,
                            actions = actions,
                            windowOpen = uiState.dialog != null,
                            menuFocus = menuOf(card.id),
                        )
                    }
                }
            }
        }
    }
    val listed = uiState.musics.map { it.id }
    // Its menus kept for the musics listed alone, those of the cards composed or not.
    SideEffect { menus.keys.retainAll(listed.toSet()) }
    FocusAfterDeletion(
        listed = listed,
        deleting = (uiState.dialog as? MyMusicsDialog.ConfirmDelete)?.id,
        windowOpen = uiState.dialog != null,
        focusOf = menuOf,
        empty = empty,
        // A card out of sight is not composed, its menu out of reach: the list jumps to it first.
        reveal = { index ->
            if (listState.layoutInfo.visibleItemsInfo.none { it.index == index }) listState.scrollToItem(index)
        },
    )
    when (val dialog = uiState.dialog) {
        is MyMusicsDialog.Rename -> RenameDialog(
            dialog = dialog,
            problemOf = actions::renameProblem,
            onRenamed = actions::onRenamed,
            onDismiss = actions::onDialogDismissed,
        )

        is MyMusicsDialog.ConfirmDelete -> DeleteDialog(
            dialog = dialog,
            onConfirm = actions::onDeleteConfirmed,
            onDismiss = actions::onDialogDismissed,
        )

        null -> Unit
    }
}

/** The bar of the page: the disc Retour, and the title, a heading for TalkBack. */
@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
    ) {
        GlyphButton(
            description = stringResource(R.string.feature_musics_back),
            onClick = onBack,
            glyph = { Glyphs.back(this, it) },
        )
        // On one line, shrunk if needed: the pixel font would cut « musiques » otherwise.
        SingleLineText(
            text = stringResource(R.string.feature_musics_title),
            color = BloxTheme.palette.ink,
            style = MaterialTheme.typography.titleLarge.merge(BloxTheme.typography.screenTitle),
            modifier = Modifier.weight(1f).semantics { heading() },
        )
    }
}

/**
 * The card « PENDANT LES PARTIES », on `panel-inset`: a note, decorative, then the label in `stat-label` above the
 * name of the music played during the games, [gameMusic], or « La musique choisie à l'accueil » when there is none.
 * TalkBack reads it as one sentence: « Pendant les parties : Thème du soir ».
 */
@Composable
private fun GameMusicCard(gameMusic: String?) {
    val palette = BloxTheme.palette
    val name = gameMusic ?: stringResource(R.string.feature_musics_game_music_home)
    val description = stringResource(
        R.string.feature_musics_game_music,
        gameMusic ?: stringResource(R.string.feature_musics_game_music_home_spoken),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GAME_MUSIC_CARD_HEIGHT)
            .background(palette.panelInset, BloxTheme.shapes.md)
            .padding(horizontal = BloxSpaces.space3, vertical = BloxSpaces.space2)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
    ) {
        BloxIcon(BloxIcon.NOTE, Modifier.testTag(MUSICS_ICON_TAG), size = IconSizes.alone, tint = palette.label)
        Column(verticalArrangement = Arrangement.spacedBy(TEXT_GAP)) {
            Text(
                text = stringResource(R.string.feature_musics_game_music_label),
                color = palette.label,
                style = MaterialTheme.typography.labelSmall.merge(BloxTheme.typography.statLabel),
            )
            Text(
                text = name,
                color = palette.ink,
                style = nameStyle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The style of the name of a music, on the card of the games and on its row: `button-label`. */
@Composable
@ReadOnlyComposable
private fun nameStyle(): TextStyle = MaterialTheme.typography.labelLarge.merge(BloxTheme.typography.buttonLabel)

/** The least height of the card « PENDANT LES PARTIES »: `min-height: 64px` in V32, no token. */
private val GAME_MUSIC_CARD_HEIGHT = 64.dp

/** Between a name and the line under it, on the card and on a row: `gap: 2px` in V32, under the smallest token. */
private val TEXT_GAP = 2.dp

/**
 * The page without music, in the middle of its room: « Aucune musique pour l'instant. », a heading for TalkBack, in
 * `stat-value` (Manrope bold at 17, as V32 draws it), then the invitation to compose one, or to let the die offer one,
 * in `label`; read in one, which the keyboard may focus ([focus]), as the last music deleted leaves it.
 */
@Composable
private fun EmptyList(
    focus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = BloxSpaces.space4),
        contentAlignment = Alignment.Center,
    ) {
        FocusableBlock(
            modifier = Modifier.focusRequester(focus),
            verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.feature_musics_empty_title),
                color = palette.ink,
                style = MaterialTheme.typography.titleMedium.merge(BloxTheme.typography.statValue),
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.feature_musics_empty_hint),
                color = palette.label,
                style = MaterialTheme.typography.bodyMedium.merge(BloxTheme.typography.small),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The row of a music, in a panel (V32): Écouter; its name and its info line, which open the music in the editor;
 * *Utiliser*, or *En jeu*, the choice made, each saying what a touch does; and its menu « … », a bare disc, whose focus
 * comes back once the window it opened closes ([windowOpen]). While it is listened to, the gauge of its bars stands
 * under its name, as wide as it; the buttons stay level with the name. Its menu is focused by [menuFocus].
 */
@Composable
private fun MusicRow(
    card: MusicCard,
    actions: MyMusicsActions,
    windowOpen: Boolean,
    menuFocus: FocusRequester,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.md
    val listening = card.listening
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .padding(BloxSpaces.space1),
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
    ) {
        val levelWithName = Modifier.alignBy { it.measuredHeight / 2 }
        ListenButton(
            playing = listening != null,
            musicName = card.name,
            onClick = { actions.onListen(card.id) },
            modifier = levelWithName,
        )
        Column(modifier = Modifier.weight(1f).alignBy(NameMiddle)) {
            NameButton(card = card, onEdit = { actions.onEdit(card.id) })
            if (listening != null) {
                BarGauge(
                    listening = listening,
                    modifier = Modifier
                        .padding(horizontal = NAME_INSET)
                        .padding(top = TEXT_GAP, bottom = BloxSpaces.space1),
                )
            }
        }
        // Utiliser, a note; En jeu, a check on the accent, the choice made, read as selected.
        GlyphButton(
            icon = if (card.inGame) BloxIcon.CHECK else BloxIcon.NOTE,
            description = stringResource(R.string.feature_musics_use_description, card.name),
            onClick = { actions.onUse(card.id) },
            modifier = levelWithName,
            emphasis = if (card.inGame) ButtonEmphasis.SELECTED else ButtonEmphasis.PLAIN,
            onClickLabel = stringResource(
                if (card.inGame) R.string.feature_musics_menu_stop_using else R.string.feature_musics_menu_use,
            ),
        )
        CardMenu(
            card = card,
            actions = actions,
            windowOpen = windowOpen,
            modifier = levelWithName.focusRequester(menuFocus),
        )
    }
}

/**
 * The name of a row and its info line, 48 dp high at least: touching them opens the music in the editor, which
 * TalkBack says. Its middle is [NameMiddle], which the buttons of the row stand level with.
 */
@Composable
private fun NameButton(
    card: MusicCard,
    onEdit: () -> Unit,
) {
    val listening = card.listening
    Column(
        verticalArrangement = Arrangement.spacedBy(TEXT_GAP, Alignment.CenterVertically),
        modifier = Modifier
            .markingItsMiddle()
            .fillMaxWidth()
            .heightIn(min = BloxSizes.minTouchTarget)
            .clickable(
                onClickLabel = stringResource(R.string.feature_musics_menu_edit),
                role = Role.Button,
                onClick = onEdit,
            )
            .padding(horizontal = NAME_INSET),
    ) {
        Text(
            text = card.name,
            color = BloxTheme.palette.ink,
            style = nameStyle(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(INFO_GAP),
        ) {
            if (listening != null) Equalizer(Modifier.testTag(MUSICS_EQUALIZER_TAG))
            CardInfo(card = card, listening = listening, modifier = Modifier.weight(1f, fill = false))
            if (card.inGame) InGameBadge()
        }
    }
}

/** The middle of the name of a row, which its buttons stand level with, whether the gauge is under it or not. */
private val NameMiddle = HorizontalAlignmentLine(::min)

/** Gives this element's middle as [NameMiddle]. */
private fun Modifier.markingItsMiddle(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height, mapOf(NameMiddle to placeable.height / 2)) { placeable.place(0, 0) }
}

/**
 * « EN JEU », a pill in `accent`, after the info line of the music played during the games. TalkBack leaves it out,
 * even within the name it stands in: the button *En jeu* beside it is read as selected.
 */
@Composable
private fun InGameBadge() {
    PillBadge(
        text = stringResource(R.string.feature_musics_in_game),
        style = MaterialTheme.typography.labelSmall.merge(BloxTheme.typography.statLabel),
        padding = PaddingValues(horizontal = BloxSpaces.space2, vertical = TEXT_GAP),
        modifier = Modifier
            .testTag(MUSICS_IN_GAME_TAG)
            .clearAndSetSemantics {},
    )
}

/** Between the edges of a name and its texts: `padding: 0 4px` in V32. */
private val NAME_INSET = BloxSpaces.space1

/** Between the equalizer, the info line and the pill under a name: `gap: 6px` in V32, between two tokens. */
private val INFO_GAP = 6.dp

/**
 * The line under the name of a row, on one line, cut short if needed: its style, its key and its length, « Joyeux ·
 * sol majeur · 0:16 »; or, while it is [listening] to, the bar played, in the ink as the mockup shows it. TalkBack
 * says it in words instead ([cardInfoDescription]): « Joyeux, sol majeur, 16 secondes », not « 0:16 » nor « · ».
 */
@Composable
private fun CardInfo(
    card: MusicCard,
    listening: Listening?,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val style = stringResource(categoryLabel(card.category))
    val description = cardInfoDescription(
        style = style,
        key = card.key,
        spokenLength = card.spokenLength,
        listening = listening,
    )
    // Its text left to the description alone: in the button that opens the editor, TalkBack would read « · » and
    // « 0:24 » too. Its layout stays exposed, as the one of a text, for the services that look for it.
    val laidOut = remember { LaidOutText() }
    Text(
        text = if (listening != null) {
            stringResource(R.string.feature_musics_listening, listening.bar, listening.bars)
        } else {
            stringResource(R.string.feature_musics_info, style, card.key, card.length)
        },
        color = if (listening != null) palette.ink else palette.label,
        style = MaterialTheme.typography.labelSmall.merge(BloxTheme.typography.small),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { laidOut.result = it },
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
            getTextLayoutResult { results -> laidOut.result?.let(results::add) ?: false }
        },
    )
}

/** The last layout of a text, kept without recomposing anything. */
private class LaidOutText {
    var result: TextLayoutResult? = null
}

/**
 * The menu « … » of a card ([RowMenu]): *Modifier*, *Renommer*, *La jouer* or *Ne plus la jouer pendant les parties*,
 * *Supprimer*. Its focus comes back once the window it opened closes ([windowOpen]).
 */
@Composable
private fun CardMenu(
    card: MusicCard,
    actions: MyMusicsActions,
    windowOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    RowMenu(description = rowMenuDescription(card.name), modifier = modifier, windowOpen = windowOpen) {
        Item(stringResource(R.string.feature_musics_menu_edit), BloxIcon.EDIT, onClick = { actions.onEdit(card.id) })
        RenameItem(onRename = { actions.onRename(card.id) })
        Item(
            text = stringResource(
                if (card.inGame) R.string.feature_musics_menu_stop_using else R.string.feature_musics_menu_use,
            ),
            icon = BloxIcon.NOTE,
            onClick = { actions.onUse(card.id) },
        )
        DeleteItem(onDelete = { actions.onDelete(card.id) })
    }
}

@Composable
private fun MyMusicsPreview(
    kind: BloxThemeKind,
    uiState: MyMusicsUiState = previewState,
) {
    BloxTheme(kind = kind) {
        MyMusicsScreen(uiState = uiState, actions = NoActions)
    }
}

/**
 * Three musics, the first played during the games, the second listened to, its name too long for its card, the third
 * written by hand.
 */
private val previewState = MyMusicsUiState(
    musics = listOf(
        MusicCard(
            ScoreId.Personal("1"),
            "Thème du soir",
            Category.CALM,
            key = "fa majeur",
            length = "0:24",
            spokenLength = "24 secondes",
            inGame = true,
        ),
        MusicCard(
            ScoreId.Personal("2"),
            "Ballades pour une longue soirée en hiver",
            Category.RETRO,
            key = "fa dièse mixolydien",
            length = "0:12",
            spokenLength = "12 secondes",
            inGame = false,
            listening = Listening(bar = 3, bars = 8),
        ),
        MusicCard(
            ScoreId.Personal("3"),
            "Ma marche",
            category = null,
            key = "la mineur",
            length = "0:15",
            spokenLength = "15 secondes",
            inGame = false,
        ),
    ),
    gameMusic = "Thème du soir",
    deleted = "Joyeux 2",
)

/** The actions of the previews: none. */
private object NoActions : MyMusicsActions {
    override fun onBack() = Unit

    override fun onListen(id: ScoreId.Personal) = Unit

    override fun onUse(id: ScoreId.Personal) = Unit

    override fun onEdit(id: ScoreId.Personal) = Unit

    override fun onRename(id: ScoreId.Personal) = Unit

    override fun renameProblem(name: String): NameProblem? = null

    override fun onRenamed(name: String) = Unit

    override fun onDelete(id: ScoreId.Personal) = Unit

    override fun onDeleteConfirmed() = Unit

    override fun onDialogDismissed() = Unit

    override fun onUndo() = Unit

    override fun onUndoDismissed() = Unit

    override fun onRandom() = Unit

    override fun onNew() = Unit
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun MyMusicsOriginalPreview() {
    MyMusicsPreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun MyMusicsColorfulPreview() {
    MyMusicsPreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun MyMusicsOriginalLargeFontPreview() {
    MyMusicsPreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun MyMusicsColorfulLargeFontPreview() {
    MyMusicsPreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun MyMusicsEmptyPreview() {
    MyMusicsPreview(BloxThemeKind.COLORFUL, MyMusicsUiState(musics = emptyList(), gameMusic = null))
}
