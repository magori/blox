package ch.magori.blox.feature.musics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.Glyphs
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.ScoreId

/** What the player does on « Mes musiques »: the screen hands each action over, and decides nothing. */
@Stable
internal interface MyMusicsActions {
    /** The back button: home. */
    fun onBack()

    /** *Écouter*, or *Arrêter*, the music of [id]. */
    fun onListen(id: ScoreId.Personal)

    /** *Utiliser*, or *En jeu*, on the music of [id]; or its item in the menu. */
    fun onUse(id: ScoreId.Personal)

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
 * « Mes musiques » (V7, V10): the back button and, at the top, the music played during the games; a card per saved
 * music, to listen to it, choose it for the games, rename or delete it from its menu « … »; the message that undoes a
 * deletion; *Au hasard* and *Nouvelle*, which open the editor.
 */
@Composable
internal fun MyMusicsScreen(
    uiState: MyMusicsUiState,
    actions: MyMusicsActions,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.page)
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The header stays in place, as in the mockup: the back button is always at hand.
        Header(gameMusic = uiState.gameMusic, onBack = actions::onBack)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (uiState.musics.isEmpty()) {
                item(key = EMPTY_KEY) {
                    Text(
                        text = stringResource(R.string.feature_musics_empty),
                        color = palette.label,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
                    )
                }
            }
            items(uiState.musics, key = {
                "$MUSIC_KEY${it.id.key}"
            }) { card -> MusicCardRow(card = card, actions = actions) }
        }
        uiState.deleted?.let { name ->
            UndoMessage(name = name, onUndo = actions::onUndo, onClose = actions::onUndoDismissed)
        }
        BottomButtons(onRandom = actions::onRandom, onNew = actions::onNew)
    }
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

/** The back button, the title, and the music played during the games. */
@Composable
private fun Header(
    gameMusic: String?,
    onBack: () -> Unit,
) {
    val palette = BloxTheme.palette
    Row(
        modifier = Modifier.padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlyphButton(
            description = stringResource(R.string.feature_musics_back),
            onClick = onBack,
            glyph = { Glyphs.back(this, it) },
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // On one line, shrunk if needed: the pixel font would cut « musiques » otherwise.
            SingleLineText(
                text = stringResource(R.string.feature_musics_title),
                color = palette.ink,
                style = BloxTheme.typography.title.merge(MaterialTheme.typography.titleLarge),
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(
                    R.string.feature_musics_game_music,
                    gameMusic ?: stringResource(R.string.feature_musics_game_music_home),
                ),
                color = palette.label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The card of a music: listen, its name and its info line, *Utiliser* or *En jeu* under them, and its menu « … ». The
 * button stands under the name, not beside it as in the mockup: the pixel font of Original and a font twice as large
 * would leave the name a few letters.
 */
@Composable
private fun MusicCardRow(
    card: MusicCard,
    actions: MyMusicsActions,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.panel
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val listening = card.listening
        val listenDescription =
            if (listening !=
                null
            ) {
                R.string.feature_musics_stop_description
            } else {
                R.string.feature_musics_listen_description
            }
        GlyphButton(
            description = stringResource(listenDescription, card.name),
            onClick = { actions.onListen(card.id) },
            background = palette.page,
            glyph = { ink -> if (listening != null) Glyphs.stop(this, ink) else Glyphs.play(this, ink) },
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = card.name,
                color = palette.ink,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (listening != null) {
                    stringResource(R.string.feature_musics_listening, listening.bar, listening.bars)
                } else {
                    stringResource(R.string.feature_musics_info, stringResource(categoryLabel(card.category)), card.bpm)
                },
                color = palette.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            UseButton(name = card.name, inGame = card.inGame, onClick = { actions.onUse(card.id) })
        }
        CardMenu(card = card, actions = actions)
    }
}

/** *Utiliser*, framed; or *En jeu*, filled in the accent, as a selected choice of the home screen. */
@Composable
private fun UseButton(
    name: String,
    inGame: Boolean,
    onClick: () -> Unit,
) {
    val palette = BloxTheme.palette
    // A switch for TalkBack: « Thème du soir pendant les parties », on or off. The label shows the state too.
    TextButtonBox(
        text = stringResource(if (inGame) R.string.feature_musics_in_game else R.string.feature_musics_use),
        description = stringResource(R.string.feature_musics_use_description, name),
        background = if (inGame) palette.accent else palette.panel,
        ink = if (inGame) palette.accentInk else palette.ink,
        frame = if (inGame) palette.accent else palette.panelBorder,
        onClick = onClick,
        switchedOn = inGame,
    )
}

/** The menu « … » of a card: *Renommer*, *La jouer* or *Ne plus la jouer pendant les parties*, *Supprimer*. */
@Composable
private fun CardMenu(
    card: MusicCard,
    actions: MyMusicsActions,
) {
    val palette = BloxTheme.palette
    Box {
        var expanded by remember { mutableStateOf(false) }
        GlyphButton(
            description = stringResource(R.string.feature_musics_menu_description, card.name),
            onClick = { expanded = true },
            glyph = { Glyphs.more(this, it) },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = palette.panel,
        ) {
            MenuItem(R.string.feature_musics_menu_rename, palette.ink) {
                expanded = false
                actions.onRename(card.id)
            }
            MenuItem(
                if (card.inGame) R.string.feature_musics_menu_stop_using else R.string.feature_musics_menu_use,
                palette.ink,
            ) {
                expanded = false
                actions.onUse(card.id)
            }
            MenuItem(R.string.feature_musics_menu_delete, palette.danger) {
                expanded = false
                actions.onDelete(card.id)
            }
        }
    }
}

@Composable
private fun MenuItem(
    text: Int,
    color: Color,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = stringResource(text), color = color, style = MaterialTheme.typography.labelLarge) },
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

/** « « X » est supprimée. », with « Annuler » and its cross: it stays until the next action. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UndoMessage(
    name: String,
    onUndo: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = BloxTheme.palette
    // The actions beside the message, or under it when they do not fit: the message is never cut.
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.ink, BloxTheme.shapes.panel)
            .padding(start = 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.feature_musics_deleted, name),
            color = palette.page,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            TextButtonBox(
                text = stringResource(R.string.feature_musics_undo),
                background = palette.ink,
                ink = palette.page,
                frame = palette.ink,
                onClick = onUndo,
            )
            GlyphButton(
                description = stringResource(R.string.feature_musics_close_message),
                onClick = onClose,
                ink = palette.page,
                glyph = { Glyphs.close(this, it) },
            )
        }
    }
}

/** *Au hasard*, framed as a panel, and *Nouvelle*, in the accent, side by side. */
@Composable
private fun BottomButtons(
    onRandom: () -> Unit,
    onNew: () -> Unit,
) {
    val palette = BloxTheme.palette
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButtonBox(
            text = stringResource(R.string.feature_musics_random),
            background = palette.panel,
            ink = palette.ink,
            frame = palette.panelBorder,
            onClick = onRandom,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        )
        TextButtonBox(
            text = stringResource(R.string.feature_musics_new),
            background = palette.accent,
            ink = palette.accentInk,
            frame = palette.accent,
            onClick = onNew,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        )
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

/** Three musics, the first played during the games, the second listened to. */
private val previewState = MyMusicsUiState(
    musics = listOf(
        MusicCard(ScoreId.Personal("1"), "Thème du soir", Category.CALM, bpm = 80, inGame = true),
        MusicCard(
            ScoreId.Personal("2"),
            "Ballades pour une longue soirée en hiver",
            Category.RETRO,
            bpm = 156,
            inGame = false,
            listening = Listening(bar = 3, bars = 8),
        ),
        MusicCard(ScoreId.Personal("3"), "Ma marche", category = null, bpm = 132, inGame = false),
    ),
    gameMusic = "Thème du soir",
    deleted = "Joyeux 2",
)

/** The actions of the previews: none. */
private object NoActions : MyMusicsActions {
    override fun onBack() = Unit

    override fun onListen(id: ScoreId.Personal) = Unit

    override fun onUse(id: ScoreId.Personal) = Unit

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

/** The keys of the items of the list: a key of music never takes the one of the empty list. */
private const val EMPTY_KEY = "empty"
private const val MUSIC_KEY = "music:"
