# Thèmes

Le thème Couleurs est refait dans la piste C de la maquette (« Y2K irisé », jour et nuit) : jetons,
typographie et composants cibles dans le [design system](design-system/README.md). Les couleurs de
jour et de nuit sont codées depuis P5.57 (section « Thème Couleurs ») ; polices, formes, composants
et pièces suivent (P5.58 à P5.74), et la suite de ce document décrit ces parties telles qu'elles
sont codées aujourd'hui. Le thème Original prend les
mêmes mises en page v2 avec ses propres jetons (ADR 0020). Valeurs Original retenues pour les
rôles nouveaux (P5.56, à confirmer sur la maquette en P5.85) : `emptyCell` `#DCEBD5` (le fond du
plateau), `panelInset` `#DCEBD5`, `scrim` `rgba(207,227,200,0.85)`, `clearGlow` `#EEF5EA`,
`dangerSoft` `rgba(52,80,58,0.14)` (Original n'a pas de rouge ; le danger se lit au cadre de
4 dp), `cellDead` `#BFD6B6`, `iris` remplacé par l'aplat `#34503A`, `accentRing` sans halo
(`OriginalPaletteTest › the roles new in v2 have in the Original theme the values of docs themes md`).
Points tranchés en P5.56 : pendant la partie (P5.78), NOUVEAU RECORD n'apparaît que si un record supérieur à 0 existait (sinon il s'afficherait dès le premier point) ; en fin de partie, la règle reste celle d'aujourd'hui (un premier score positif est un nouveau record, `HighScoreRepositoryContract › a first positive score is a new record against 0`) ;
un volume à 0 joue en silence (la musique ne s'arrête pas) ; pas de fondu au changement de thème.

Deux thèmes, issus de la [maquette](maquette.md). Les jetons d'interface sont codés dans
`core:designsystem` (`BloxPalettes`). Les styles de pièces sont codés dans `core:board` (P5.73, ADR 0023),
car ils dépendent des types de pièces du domaine.

## Principe

Le domaine ne connaît que `PieceType`. Un thème associe à chaque type un **style de cellule** :
couleur de fond, couleur de bordure, et un éventuel carré central. Ajouter un thème ne touche
jamais au moteur. Voir l'[ADR 0005](adr/0005-themes-hors-du-domaine.md).

Les tailles de la maquette sont données pour une cellule de 24 px ; le code les garde en
**fraction de la cellule** (2/24, 3/24, 1/24, carré central 8/24), pour que les proportions
suivent l'écran. Au dessin, la taille de cellule, l'origine du plateau et chaque épaisseur
sont arrondies au pixel entier, au moins 1 px, pour éviter les joints flous. Le thème courant
est exposé par `BloxTheme.kind` (`core:designsystem`), la forme des encarts par
`BloxTheme.shapes` (coins carrés en Original). Le code du plateau est dans `core:board` (P5.73) :
`CellStyles`, `CellPainter`, `BoardGeometry`, `DisplayedCells`, `BoardCanvas`, et l'aperçu de la
pièce suivante dessiné avec le même `CellPainter` ; la colonne latérale reste dans `feature:game`.

**Formes, reliefs, tailles et durées v2** (P5.59, `core:designsystem`), aux valeurs de
`tokens.json` comparées au code comme les couleurs (ADR 0019) :
- **Rayons** (`BloxShapes`) : `cell` 6, `md` 16 (encarts, choix, rangées, touches), `snack` 20 (messages de l'éditeur et de « Mes
  musiques »), `window` 24, `card` 28 (fenêtres de confirmation), `pill` en demi-hauteur (étiquette
  NOUVEAU RECORD) ; tout est carré en Original.
- **Reliefs** (`BloxTheme.glosses`, `Modifier.gloss`) : `button-gloss`, `button-gloss-pressed` et
  `cell-gloss` dessinés en ombres intérieures (un trait clair en haut, un sombre en bas, comme
  `inset 0 y 0` en CSS) et, pour `cell-gloss` de jour, un liseré autour de la forme ; jour et nuit
  ont les leurs, Original n'en a aucun. Comme en CSS, la bande intérieure suit l'arrondi intérieur du cadre (rayon moins le
  cadre) et le liseré grandit ses coins de sa largeur ; les bandes sont construites une fois par
  taille, densité, sens de lecture et relief, pas à chaque recomposition. Ils seront posés sur les boutons en P5.60 et sur les cases
  en P5.74.
- **Tailles** (`BloxSizes`) : cible de 48 dp, rangée de réglage, interrupteur, touches (`keyArrow`,
  `keyRotate` et leurs variantes compactes et tablette). `ControlSizes` (`feature:game`) y renvoie :
  les touches gardent aujourd'hui la taille compacte (52 et 56 dp), les tailles par largeur
  arrivent en P5.79. `sheetRise` (`sheet-rise`, 40 dp, P10.37) : la distance que parcourt une feuille du bas en
  montant à sa place ou en redescendant, distance de mouvement qu'aucun espacement ne donne. `messageRise`
  (`message-rise`, 16 dp, P10.38) : la montée du message « Gardé » du Jardin, qui empruntait `space4`.
- **Espacements** (`BloxSpaces`, `space1` à `space6`) et **durées** (`BloxMotion`) en ticks, au plus
  près des millisecondes du design system (1 tick = 1/60 s, ADR 0006) : `press` 6, `reducedFade`
  8, `curtain` 12, `row` 12 (rangée supprimée qui s'estompe puis se replie, P10.37), `card` 18, `sticker` 30, `pop` 45 ;
  pour le Jardin, `gardenBounce` 6, `gardenGlow` 8, `gardenFall` 8 (chute d'une pièce d'Au hasard, P10.38),
  `gardenFallStep` 2.

**Tests :** `ShapeTokensTest › each radius, size and space of tokens json is the one of the code`,
`ShapeTokensTest › every radius, size and space token of tokens json is in the code or listed as not coded, with its reason`,
`OriginalShapesTest › the Original theme has square corners and no gloss`,
`GlossTokensTest › each shadow of tokens json is the gloss of the code, day and night`,
`GlossTokensTest › every shadow token of tokens json is a gloss or a drop shadow of the code`,
`GlossTokensTest › a shadow written in tokens json is read as its inner shadows and its ring`,
`MotionTokensTest › each duration of tokens json is a whole number of ticks, the nearest to its milliseconds`,
`MotionTokensTest › every duration token of tokens json is in the code or listed as not coded, with its reason`,
`GlossTokensTest › a shadow blurred or moved sideways is refused, the code could not draw it`,
`GlossTokensTest › a ring of a negative width is refused`,
`GlossDrawingTest › two calls with the same gloss, shape and frame give the same modifier, so a recomposition keeps its bands`,
`GlossDrawingTest › the bands are built once while their inputs stay the same`,
`GlossDrawingTest › the bands are built again when the size, the density, the direction or a value of the gloss changes`,
`GlossDrawingTest › the inner bands of a rounded shape follow the inside of its frame, their corners narrower by the frame`,
`GlossDrawingTest › the ring of a rounded shape grows its corners by its width`,
`GlossDrawingTest › a square shape stays square, inside its frame and around it` ; captures `GlossSheetOriginal`,
`GlossSheetColorful`, `GlossSheetColorfulNight`.

**Curseur à pas et jauge du niveau** (P5.62, `core:designsystem`, composants Choice et Switch) : une logique
pure, `SteppedValue` (interne), donne le pas le plus proche d'une position ou d'une valeur, sans sortir des
bornes ; un nombre invalide (NaN) ne change rien. Le **curseur de volume** (`VolumeSlider`) va de 0 à 100 % par
pas de 10 (la règle de `Volume`, `core:music`, que les features convertissent), valeur affichée en % ; la
**jauge du niveau** (`LevelGauge`) a 10 barres de 8 dp, de 8 à 26 dp de haut, pleines en `accent` jusqu'au
niveau et creuses au-delà (le niveau ne se lit pas qu'à la couleur : atteint et au-delà ne font que 1,6:1 de
jour), et des boutons − et +, − désactivé au plus bas, + au plus haut. TalkBack ne lit qu'un curseur, qui
avance d'un pas à la fois (− et + exclus). En Original, dont le `panel-border` est l'accent, les barres
au-delà, la piste et le curseur désactivé sont en `button`. Désactivé : piste au-delà en `panel-inset` cadrée
de `panel-border`. Les deux suivent le sens de lecture. Ils seront branchés sur les Paramètres (P5.70) et
l'accueil (P5.75) ; pas de flèches au clavier pour l'instant (P5.90).

**Tests :** `SteppedValueTest`, `GaugeBarsTest`, `StepColorsTest`, `VolumeSliderTest`, `LevelGaugeTest`, dont
`GaugeBarsTest › the bars up to the value are filled and those beyond it hollow, so the value does not read by colour alone`,
`StepColorsTest › a disabled slider in Original, whose panel border is its accent, is drawn in button, never in its accent`,
`VolumeSliderTest › TalkBack reads one slider with its value and moves it one step at a time`,
`VolumeSliderTest › a touch on the track gives the nearest step`,
`VolumeSliderTest › a drag never takes the value out of its range`,
`VolumeSliderTest › in a language read from right to left, the track starts on its right`,
`LevelGaugeTest › minus is disabled at the lowest step and plus at the highest`,
`LevelGaugeTest › TalkBack reads one slider with its value and moves it one step at a time`,
`LevelGaugeTest › in a language read from right to left, the leftmost bar is the highest level` ;
captures `Stepper*`. Vues : V27, V31.

**Autocollant** (P7.45, `Sticker` dans `core:designsystem`) : un mot en capitales, `accent-ink` sur `accent`, dans
une pilule (`radius-pill`), en `points-pop` par défaut, penché de −4° (`STICKER_TILT`) comme collé à la main, cadré
de `frame` (2 dp) dans la couleur de la page qui le découpe de ce qu'il couvre, 10 × 5 dp autour du texte par
défaut. Partant de la taille de son style, il rapetisse plutôt que de déborder : sur une ligne (`shrinkToFit`), ou,
avec `maxLines` au-delà d'une, sur le moins de lignes possible (`shrinkToFewestLines`) ; sans taille dans son style
(`points-pop` en Original), il part de celle du texte autour, et, sans taille là non plus, garde celle du texte sans
rapetisser. En Original, il ne penche jamais : ses pixels seraient flous. Ce que lit TalkBack et son mouvement
appartiennent à l'écran, par le `modifier`, posé avant l'inclinaison. Il remplace trois copies, au rendu inchangé
(captures identiques) : NOUVEAU RECORD ou NOUVEAU MEILLEUR de la barre du jeu (non cadré, 4 × 2 dp, sur deux lignes
alignées à droite, à la taille de l'étiquette qu'il remplace), NOUVELLE des cartouches (11 sp espacé de 0,04 em, −6°
sur le coin de la cartouche, 140 dp au plus) et NOUVEAU MEILLEUR ou PARFAITE de la carte du Serrurier (en rebond, par
un `graphicsLayer` d'échelle dans le `modifier`). L'autocollant « +points » du plateau, dessiné au canevas, en reprend
l'inclinaison. Le badge EN JEU de Mes musiques, droit, en `stat-label`, sans cadre, est un badge et non un
autocollant : il passe par le badge commun, ci-dessous.
**Tests :** `StickerLookTest › a sticker is accent-ink on accent, framed in the colour of the page`,
`StickerLookTest › a sticker not framed has no frame`,
`StickerLookTest › in Couleurs a sticker leans by its tilt, 4 degrees anticlockwise unless told otherwise`,
`StickerLookTest › in Original a sticker never leans, whose pixels would blur`,
`StickerTest › a sticker shows its text in capitals`,
`StickerTest › in a font twice as large and a narrow place, a sticker of one line shrinks to fit its width`,
`StickerTest › a sticker in a style without a size, among texts without one, still shows its text`,
`StickerTest › in a font twice as large and a narrow place, a sticker of two lines breaks between its words` ;
captures `TopBarNewRecordLargeFont`, `GameScreenNewRecord*`, `Cartridges*`, `LocksmithOpenPerfectDay`,
`LocksmithOpenNewBestDay`. Vues : V2, V36, V35.

**Badge** (P7.51, `PillBadge` dans `core:designsystem`, nommé ainsi pour ne pas se confondre avec le `Badge` de
Material) : un libellé court, sur une ligne (`maxLines`, 1 par défaut), `accent-ink` sur `accent` dans tous les
thèmes, Original compris (`badgeLook`, dont l'autocollant reprend les couleurs), dans une pilule droite
(`radius-pill`), sans inclinaison ni cadre, à la différence de l'autocollant. Son texte est pris tel quel (les
capitales sont à l'écran), en `small` sur `labelMedium` par défaut, 8 × 4 dp autour (`space2` × `space1`) par défaut ;
`withCheck` le fait précéder d'une coche de 12 dp, à `space1` du mot. Ce que lit TalkBack appartient à l'écran, par le
`modifier`, posé à la racine. Il remplace trois copies, au rendu inchangé (captures identiques) : EN JEU de Mes
musiques (`stat-label` sur `labelSmall`, 8 × 2 dp, effacé pour TalkBack), NOUVEAU RECORD ou NOUVEAU MEILLEUR de la fin
de partie (`title` à 11 sp, mis en capitales par l'écran, 16 × 8 dp, sur autant de lignes qu'il faut : sur un
téléphone étroit en police agrandie, RECORD passe à la ligne au lieu de disparaître) et « ouvert » de Mes jardins
(style et retrait par défaut, avec sa coche).
**Tests :** `BadgeLookTest › a badge is accent-ink on accent, in every theme`,
`PillBadgeTest › a badge shows its text as given, not in capitals`,
`PillBadgeTest › in a font twice as large and a narrow place, a badge stays on one line unless told otherwise`,
`PillBadgeTest › a badge bears a check before its text only with withCheck`,
`PillBadgeTest › the modifier of a badge applies to its root, its pill around its text`,
`GameOverScreenTest › in Original, on a phone of 320 dp in a font twice as large, NOUVEAU RECORD shows whole, RECORD included` ;
captures `MyMusics*`, `GameOverColorfulNewRecord*`, `GameOverOriginalNewRecord`, `GameOverDailyNewBest*`,
`MyGardensList*`. Vues : V4, V32, V39.

**Boutons bulles** (P5.60, `core:designsystem`, composant Button) : quatre familles, `ButtonFamily` —
Principal XL (72 dp de haut), Principal et Secondaire (56 dp), Icône (disque de 48 dp) — par
`BubbleButton`, et ses raccourcis `PrimaryButton`, `SecondaryButton` et `GlyphButton` (famille Icône :
retour, réglages, écoute, menu, fermeture). Ils remplacent les boutons de `feature:game`
(Reprendre, Menu, Rejouer) et l'ancien `SettingsButton` de `feature:home` (le `SettingsButton` d'aujourd'hui,
partagé, est décrit plus bas : **Paramètres**, P12.11). Couleurs par variante (`BloxTheme.buttons`) ; en
Original, carrés et sans relief par le thème. Le Principal est en `accent`, le Secondaire et l'Icône
en `button` cadré de `buttonOutline` ; en Original, Secondaire et Icône sont au repos sur la page,
encre `ink` et cadre `buttonOutline` (l'encre du bouton sur son fond n'atteint que 4,0:1), et
prennent `buttonPressed` pressés. États : **pressé**, 2 dp plus bas et remplissage pressé tout de
suite, retour en `motion-press` (6 ticks, `pressDepth`, compté par l'écran avec `FrameTicks`) ; seule la descente revient
progressivement, fond, encre et relief restent ceux du bouton pressé jusqu'au repos (sinon, en Original,
fond et libellé se croisaient) ; un tapotement bref dans une liste qui défile s'affiche aussi ;
**désactivé**, sans relief, fond `panelInset`, libellé en `label`, cadre `panelBorder` ; **focus**,
contour de 3 dp en `ink` à 3 dp du bouton. Libellés à 4,5:1 au moins sur leur fond, au repos et
pressés ; cadres du Secondaire et de l'Icône à 3:1 sur la page ; 48 dp au moins de haut et de large.
Effets réduits (P5.71) : retour immédiat par `returnTicks = 0`.

**Tests :** `ButtonLookTest › a pressed button goes 2 dp down and takes its pressed fill at once`,
`ButtonLookTest › a released button comes back in 6 ticks`,
`ButtonLookTest › halfway back a button is 1 dp down, still in its pressed fill and ink`,
`ButtonLookTest › a disabled button has no gloss, its label in label and its frame in panel border`,
`ButtonLookTest › the label of each family stands out from its fill by 4,5 to 1 at least, at rest, pressed and coming back`,
`ButtonLookTest › a focused button bears a 3 dp outline in ink`,
`ButtonLookTest › every family is 48 dp high at least, Play 72 and a main or secondary button 56`,
`ButtonLookTest › each variant draws its buttons in its own colours`,
`ButtonLookTest › the frame of a secondary or an icon button stands out from the page by 3 to 1 at least`,
`BubbleButtonTest › a pressed button goes 2 dp down at once, stays down while motion press lasts, then is back at rest`,
`BubbleButtonTest › a button tapped briefly in a scrolling column, pressed and released in one frame, still goes down`,
`BubbleButtonTest › a disabled button is read as disabled and does nothing when touched`,
`BubbleButtonTest › a labelled button is read once, as a button of its label, its icon adding nothing`,
`BubbleButtonTest › a button of each family is 48 dp high and wide at least, and as high as its family` ;
captures `AccentButtons*` et `PlainButtons*` (Original, jour, nuit). Vues : V19, V21, V27 à V34.

**Feuille du bas** (P5.96, ADR 0029) : `BloxSheet` monte dans une fenêtre plein écran, sans animation ni
assombrissement de la plateforme : voile `scrim`, panneau `panel` cadré de `panel-border` ouvert en bas, coins
`radius-card` en haut, poignée, titre `screen-title` qui défile avec le contenu ; les actions restent fixes et
visibles au-dessus du clavier et des barres système. Elle monte en 18 ticks (`motion-card`), en fondu de 8 ticks
en Effets réduits, et réapparaît en place après une rotation. Le retour ou le voile la ferment, une seule demande
étant transmise ; pas de glissement. Fermée par son écran avec `leaving` (P10.32, `LeavingContent`, `ClosingSheet`),
elle descend en 18 ticks (`motion-card`) en accélérant, son voile en fondu, et disparaît d'un coup en Effets réduits ;
pendant sa descente, elle est inerte : aucun toucher ne passe, TalkBack ne la lit plus, et elle ne demande pas à se
fermer. Rouverte pendant sa descente, elle reprend sa place. Seule la feuille de Mes jardins descend pour l'instant.
`SheetChoices` groupe des `SheetChoiceRow` (56 dp, rond puis coche, libellé et
sous-ligne) lues comme des boutons radio. `PanelTitle` est partagé avec les fenêtres. `SettledSheet` dessine une feuille
posée, sans sa fenêtre, pour les aperçus et les captures (P5.101) ; `rememberOnce`, public, garde une action
d'un second appel (fermeture, « Tirer une musique »).
**Tests :** `SheetEntranceTest › a sheet rises in 18 ticks, and in reduced effects fades in within 8 ticks`,
`BloxSheetTest › a sheet is read as a dialog by TalkBack, its title first`,
`BloxSheetTest › the system back and a touch on the scrim close the sheet`,
`BloxSheetTest › the window of a sheet neither dims the screen nor plays an animation of the platform, from its first frame`,
`BloxSheetTest › a sheet asks once to close, sent back then touched on its scrim twice`,
`SheetEntranceTest › a sheet closed goes down in 18 ticks, fading with its scrim, and in reduced effects it goes at once`,
`BloxSheetTest › a sheet closed by its screen goes down before it is gone, and goes at once in reduced effects`,
`BloxSheetTest › a sheet going down lets no touch through to what it holds, and TalkBack no longer reads it`,
`BloxSheetTest › a sheet going down does not ask to close, sent back or touched on its scrim`,
`BloxSheetTest › a sheet sent back, then opened again as it goes down, asks to close once more`,
`LeavingContentTest › closed, it leaves on its last value for its length, then is gone`,
`LeavingContentTest › closed, then open again as it leaves, it shows the value open, no longer leaving, and stays`,
`SheetChoiceRowTest › a choice row is 56 dp high, read as a radio button with its label and its sub-line, the chosen one checked`,
`SheetChoiceRowTest › the choices of a sheet are one group for TalkBack, each read as one of them` ; captures `Sheet*`.

**Fenêtres v2 et champ du nom** (P5.98, `core:designsystem`) : le titre de `WindowPanel` est en `screen-title`
(fondu dans `titleSmall`), lu comme un titre par TalkBack ; un panneau qui n'est pas une fenêtre (carte « Serrure
ouverte » du Serrurier) passe son propre style de titre. Les réponses (`WindowButton`, genre `WindowButtonKind` :
Secondaire, Principal, Danger) sont des boutons bulles, toujours empilées sur toute la largeur, à 8 dp l'une de
l'autre, la sûre d'abord, dans l'ordre où TalkBack les lit (décision du développeur, P5.86). Supprimer est rempli en
`danger` avec la corbeille (`ButtonFamily.DANGER_FILLED`), libellé en `danger-ink` à 4,5:1 au moins ; pressé, il
garde son remplissage. Le champ du nom (`NameField`, extrait de `NamingWindow`, longueur maximale en paramètre) :
fond `panel-inset`, 56 dp de haut au moins, coins `radius-md`, le nom sur autant de lignes qu'il faut ; cadre de
2 dp en `button-outline` au repos (3:1 au moins sur le panneau, WCAG 1.4.11), en `ink` au focus, en `danger` quand
le nom est refusé ; compteur « n/40 » compté en caractères, lu « n caractères sur 40 » ; motif du refus précédé du
pictogramme `ALERT` (16 dp, même à ×2), annoncé poliment par TalkBack. Renommer reste une fenêtre.
**Tests :** `WindowPanelTest › the title of a window is in screen title, read as a heading`,
`WindowAnswersTest › the answers of a window stand one above the other, the safe one first, as TalkBack reads them`,
`WindowAnswersTest › in a font twice as large, on 360 dp, the answers stand one above the other, each word whole`,
`WindowButtonLookTest › a deleting answer is filled in danger with its trash icon, its label standing out by 4,5 to 1`,
`NameFieldTest › the name field counts its characters out of 40, as TalkBack reads it`,
`NameFieldTest › a refused name frames the field in danger and says why, read by TalkBack as it shows`,
`NameFieldTest › the name field is framed in ink while it has the focus`,
`NameFieldFrameTest › the name field is framed in button outline at rest, in ink while it has the focus, in danger when refused`,
`NameFieldFrameTest › the frame of the name field at rest stands out from the panel by 3 to 1 at least` ;
captures `*Window*` de `core:designsystem` et de `feature:composer`. Vues : V25, V32, V34.

**États des boutons pour la musique** (P5.93) : `ButtonEmphasis` (`PLAIN`, `SELECTED`, `BARE`), `ButtonSize`
(`REGULAR`, `COMPACT`) et l'état `active`. Un bouton **retenu** (mesures, Utiliser / En jeu) est rempli en
`accent`, cadré en `accentPressed` (3:1 sur la page de jour, comme la touche A), encre `accentInk`, et annoncé
« sélectionné » par TalkBack ; désactivé, il reste en `accent`, à plat. Un bouton **en cours** (Écouter pendant
l'écoute) garde l'aspect pressé sans descendre. Un **disque nu** (« … », − et + du tempo, croix) n'a ni fond, ni
cadre, ni relief au repos ; pressé, il prend `buttonPressed` et un cadre `buttonOutline` ; désactivé, il reste nu,
pictogramme en `label`. La taille **compacte** (48 dp, décision du développeur) ne vaut que pour Principal et
Secondaire. Pictogrammes `CHECK` et `ALERT`, chacun avec son épaisseur de trait.
**Tests :** `ButtonLookTest › a selected button is filled in accent, framed and pressed in accent pressed, its ink in accent ink`,
`ButtonLookTest › the frame of a selected button stands out from the page by 3 to 1 at least`,
`ButtonLookTest › a disabled selected button stays in accent, flat and in place`,
`ButtonLookTest › a pressed bare button is framed in button outline, which stands out from the page by 3 to 1 at least`,
`ButtonLookTest › a disabled bare icon button stays bare, its glyph in label`,
`ButtonLookTest › only a main or a secondary button may be compact, 48 dp high`,
`BubbleButtonTest › a selected button is read as selected by TalkBack, a plain one is not` ; captures `ButtonVariants*`.

**Écouter et égaliseur** (P5.94, `core:designsystem`) : `ListenButton` remplace les boutons Écouter de l'accueil et
de l'éditeur. C'est un `GlyphButton` de la famille Icône (48 dp, ou la taille donnée), triangle `PLAY` au repos,
carré `STOP` pendant l'écoute, état **en cours** (aspect pressé) ; TalkBack lit « Écouter X » ou « Arrêter X », le
verbe seul sans nom de musique, le pictogramme ne disant rien. À l'accueil, il n'a plus de libellé visible.
`Equalizer` : trois barres de 3 × 14 dp, 2 dp entre elles, en `accent`, coins de la moitié de leur largeur (1,5 dp ;
les 2 dp de la maquette ne se dessinent pas sur 3 dp). Chaque barre monte de 35 % à 100 % de sa hauteur en 30 ticks et
redescend en 30, 10 ticks d'avance sur la précédente (choix du développeur) ; au repos, un escalier montant (35, 57,
78 %). Le tick n'est lu qu'au dessin, sur l'horloge de Compose (`FrameTicks`) : rien ne recompose. Immobile en Effets
réduits et en Original, ignoré par TalkBack. Il est posé sur la liste des musiques (P5.99). Sa boucle (30 ticks,
10 d'avance) reste sans jeton (P10.38) : elle appartient au dessin du pictogramme `EQUALIZER`, comme ses barres de
3 × 14 dp (docs/design-system/pictogrammes.md), et n'est ni une transition ni partagée avec un autre mouvement.
**Tests :** `ListenFaceTest › the listen button shows a triangle, and a square while playing, read Écouter or Arrêter with the name of the music`,
`ListenFaceTest › a music playing keeps its listen button in the pressed fill`,
`ListenButtonTest › a listen button is a button of 48 dp or the size given, read with the name of its music, or its verb alone`,
`ListenButtonTest › a listen button playing is drawn as the button of its square, active, and not as at rest`,
`EqualizerMotionTest › the equalizer moves its three bars between 35 and 100 percent of their height in 30 ticks`,
`EqualizerTest › in reduced effects or in Original the equalizer stands still`,
`EqualizerTest › the equalizer is left out by TalkBack`,
`EqualizerTest › nothing recomposes while the equalizer moves` ; captures `Listen*`.

**Paramètres** (P12.11, `core:designsystem`) : `SettingsButton` est la roue dentée au bout de la barre d’un accueil,
à l’accueil du Classique (`HomeBar`), à celui de la Graine du jour (`DailyScreen`) et au choix du jeu (`CartridgesScreen`,
P12.5, V36).
C’est un `GlyphButton` de la famille Icône, pictogramme `GEAR`, disque de 48 dp, que TalkBack lit « Paramètres » une
seule fois (chaîne `core_designsystem_settings`, qui remplace `feature_home_settings` et `feature_daily_settings`) ;
un toucher ouvre la page Paramètres. Il se dessine comme le bouton de la roue qu’il remplace, dans les deux thèmes :
l’accueil du Classique en Original ne change pas.
**Tests :** `SettingsButtonTest › the settings button is a button of 48 dp, read Paramètres once, that opens the settings`,
`SettingsButtonTest › the settings button is drawn as the button of the gear`.

**Coût du dessin** (P5.11, constats A2 et A7 de la revue qualité) : la géométrie et les
peintres, avec leurs `Path`, sont construits une fois par taille, thème et fond ; le plateau et
la pièce sont lus dans la phase de dessin, si bien qu'un mouvement de la pièce ne fait que
redessiner le plateau, sans recomposer ni lui ni l'écran de jeu. L'écran reçoit l'état comme un
fournisseur (`() -> GameUiState`) et chaque partie lit seulement ce qu'elle affiche, par un
état dérivé (`rememberPart`). Les recompositions se mesurent sur appareil avec un traceur de
composition (`RecompositionRecorder`, API interne de Compose, à surveiller aux montées de
version).
**Tests :** `BoardCanvasTest › the painters are built once while the piece moves`,
`BoardCanvasTest › nothing recomposes while the piece moves`,
`BoardCanvasTest › a moved piece is drawn as on a board drawn with it from the start`,
`BoardCanvasTest › the painters are built again when the size changes`,
`BoardCanvasTest › the painters are built again when the theme changes`,
`BoardCanvasTest › the painters are built again when the background changes`,
`GameScreenTest › nothing on the screen recomposes when only the piece moves`,
`GameScreenTest › a moved piece is drawn on the screen as on a screen drawn with it from the start`,
`GameScreenTest › the screen shows the new value of each field beside the board once it changes`,
`GameScreenTest › the card of the pause shows over the game while it is paused`,
`GameScreenTest › the game over screen takes the place of the game once it is over`.
Vues : V2, V3.

**Menu « … » et message temporaire** (P5.97, `core:designsystem`) : `BloxMenu` et `BloxMenuItem` remplacent le
`DropdownMenu` de Material : panneau `panel` cadré de `panel-border`, `radius-md`, ombre `menu-shadow` (jeton
ajouté à `tokens.json`, `BloxGlosses.menu`, aucune ombre en Original), posé sous l'extrémité de son bouton « … »
ou au-dessus s'il manque de place, toujours dans la fenêtre ; rangées de 48 dp, pictogramme décoratif, variante
`danger`, variante cochée lue comme une case cochée ; un appui hors du panneau, même dans la marge de son ombre,
ou le retour le ferme ; TalkBack annonce son ouverture (« Menu ») ; au clavier, le focus va sur la première
rangée, qui porte un anneau, et l'écran le rend au bouton « … » à la fermeture. `SnackMessage` remplace les
Snackbar : fond `ink`, texte `page`, `radius-snack`, action en texte et croix en disques nus sur `ink` (anneau de
focus en `page`), lu poliment par TalkBack ; l'action passe sous le texte plutôt que de le couper. Sa durée
d'affichage vient de `messageTimeoutMillis` : 4 s, 10 s avec une action, ou ce que recommande Android
(WCAG 2.2.1).
**Tests :** `BloxMenuTest › a menu item is 48 dp high, read as its label, its icon adding nothing`,
`BloxMenuTest › a checked item is read as a checked check box and bears a check`,
`BloxMenuTest › a touch in the margin around the panel of a menu closes it`,
`BloxMenuTest › TalkBack announces the menu as it opens, by its title`,
`MenuItemLookTest › a deleting item is in danger, icon and label`,
`SnackMessageTest › a message is read politely by TalkBack as it shows`,
`SnackMessageTest › in a font twice as large the action of a message goes under its text, no word cut`,
`SnackMessageLookTest › the text of a message stands out from ink by 4,5 to 1 at least, day and night`,
`MessageTimeoutTest › without accessibility settings a message stays 4 seconds, and 10 with an action` ;
captures `Menu*`, `SnackMessage*`.

**Menu « … » de rangée et fenêtre Renommer** (P10.36, `core:designsystem`) : `RowMenu`, commun à Mes musiques et
à Mes jardins, est le disque nu « … » de 48 dp en bout de rangée, lu « Plus d'actions pour X », qui ouvre un
`BloxMenu` de ses rangées, dans l'ordre donné : des rangées propres à la liste (`Item` : Modifier, La jouer…) et les
deux communes, Renommer (`RenameItem`) et Supprimer en `danger` (`DeleteItem`). Toucher une rangée ferme le menu,
puis fait ce qu'elle dit. Le focus revient au « … » quand le menu se ferme sans choix, ou, après une rangée qui
ouvre une fenêtre (Renommer, Supprimer), quand cette fenêtre, ouverte même quelques images plus tard, se ferme.
`RenameWindow` est la fenêtre « Renommer « X » », une `NamingWindow` répondue « Renommer » ; Supprimer reste
`DeleteWindow`. L'état des fenêtres reste dans chaque feature : il porte ce qu'on renomme ou supprime (une musique,
un jardin), que `core:designsystem` ne voit pas.
**Tests :** `RowMenuTest › « … » is read « Plus d'actions pour X » and offers its items in order, Renommer and Supprimer`,
`RowMenuTest › from the keyboard, « … » waits for the window an item opened to close, then takes the focus back`,
`RowMenuTest › a window that opens frames after its item still gives the focus back to « … » as it closes`,
`RowMenuTest › from the keyboard, the menu closed by Back gives the focus back to « … »`,
`NamingWindowTest › Renommer is titled after the name it renames and answered Renommer, which renames`.
Quand la rangée part après une suppression confirmée, `RowMenu` ne choisit pas où va le focus : son « … » peut déjà
être retiré (en Effets réduits, la rangée part d'un coup). C'est la liste qui le donne, une fois la fenêtre fermée
**et** la rangée retirée de la liste, dans n'importe quel ordre (Mes jardins, P10.39 ; Mes musiques, P7.52) ; elle
pose pour cela un `focusRequester` sur le `modifier` de chaque `RowMenu`.

**Focus après une suppression** (P7.52, `core:designsystem`) : `FocusAfterDeletion`, commun à Mes jardins et à Mes
musiques (règle de 2), donne ce focus. Il ne connaît que des clés : les clés listées, dans leur ordre (`listed`),
celle dont la fenêtre de suppression est ouverte (`deleting`), si une fenêtre est ouverte (`windowOpen`), le
`FocusRequester` de chaque clé (`focusOf`) et celui de la liste vide (`empty`). Il retient l'ordre de la liste à
l'ouverture de la fenêtre, ou à sa réouverture après une suppression non confirmée ; une liste relue sans l'élément,
la fenêtre encore ouverte, ne le remplace pas (sans quoi l'élément du milieu enverrait le focus au précédent, et
l'élément seul ferait planter l'application) ; une clé absente de cet ordre, par défense, envoie le focus au premier
élément listé. Une fois la fenêtre fermée **et** la clé absente de la liste, dans n'importe quel ordre, il donne le
focus à l'élément suivant, sinon au précédent, sinon à la liste vide ; au clavier seulement (`LocalInputModeManager`),
une image plus tard (`withFrameNanos`), après les effets de l'élément qui part. La suppression reste attendue jusqu'à
ce que le focus soit demandé : un effet annulé pendant cette image (la liste ou la fenêtre change) le redemande en
reprenant. Une suppression non confirmée attend que l'élément parte jusqu'à l'ouverture d'une autre fenêtre.
`reveal` reçoit d'abord l'indice, dans la liste, de l'élément qui prend le focus, même s'il est déjà en vue : à la
liste de décider s'il faut l'y amener. Une liste paresseuse, dont l'élément voisin peut n'être pas composé, s'en sert
(Mes musiques : `LazyListState.scrollToItem` s'il n'est pas visible) ; Mes jardins, en `Column`, n'en a pas besoin.
**Tests :** `PendingDeletionTest › the element after the one deleted takes the focus`,
`PendingDeletionTest › the last element deleted gives the focus to the one before it`,
`PendingDeletionTest › the only element deleted gives the focus to the empty list`,
`PendingDeletionTest › while the element awaited is still listed, nothing takes the focus, and it is still awaited`,
`PendingDeletionTest › an element gone gives the focus once only, once it is given`,
`PendingDeletionTest › an element gone still gives the focus while it has not been given`,
`PendingDeletionTest › the window of the same element opened again after a deletion not confirmed takes the order anew`,
`PendingDeletionTest › the element after the one deleted is the next one still listed, in the order as the window was open`,
`PendingDeletionTest › a list read without the element while its window is open keeps the order as the window opened`,
`PendingDeletionTest › the only element read out of the list while its window is open gives the focus to the empty list`,
`PendingDeletionTest › an element awaited that was never in the order gives the focus to the first element listed`,
`PendingDeletionTest › nothing awaited gives the focus to nothing`,
`FocusAfterDeletionTest › from the keyboard, the element after the one deleted takes the focus, else before it, else the list`,
`FocusAfterDeletionTest › from the keyboard, the element in the middle out of the list, then its window closed, gives the focus on`,
`FocusAfterDeletionTest › from the keyboard, the window of the element in the middle closed, then it out of the list, gives the focus on`,
`FocusAfterDeletionTest › from the keyboard, a deletion not confirmed moves the focus nowhere, until the element leaves after all`,
`FocusAfterDeletionTest › from the keyboard, after a deletion not confirmed, the focus follows the next deletion confirmed`,
`FocusAfterDeletionTest › by touch, an element deleted gives the focus to nothing`,
`FocusAfterDeletionTest › in a lazy list, the element that takes the focus is revealed first, at its index in the list`.

**Bloc focalisable** (P10.39, `core:designsystem`) : `FocusableBlock` est un bloc de texte qui ne fait rien mais que
le clavier peut focaliser, où va le focus quand ce qui le tenait part (une liste laissée vide : « Aucun jardin
gardé. », « Aucune musique pour l'instant. »). TalkBack le lit d'un trait ; focalisé, il porte l'anneau des boutons
(WCAG 2.4.7). Son contenu s'aligne comme celui d'une `Column` (`verticalArrangement`, `horizontalAlignment`, P7.52 :
la page sans musique est centrée).
**Test :** `FocusableBlockTest › from the keyboard, a focusable block takes the focus, read in one, and bears the ring of the buttons`.

**Thème de l'application** (P3.5) : le thème choisi à l'accueil s'applique à tous les écrans
(accueil, jeu, pause, fin de partie) : `MainActivity` enveloppe toute la navigation dans
`BloxTheme(kind)`, et `kind` vient d'`AppTheme` (`app`), un `StateFlow` lu depuis les
préférences, connu tout de suite après la première lecture (pas de flash à la recréation de
l'activité). Avant cette lecture, rien n'est composé et la première image est retenue, pour que
le fond de fenêtre ne s'affiche pas dans le mauvais thème. Le fond de fenêtre
(`colors.xml`, `blox_window_background`) est celui du thème Original ; un test le compare à
`BloxPalettes.Original.page`.

**Logo de l'accueil** (P3.7) : quatre cellules en carré 2 × 2, couleurs du jeton
`logoColors(variant)` (`LogoColors`, exactement quatre couleurs, `core:board` depuis P5.74) : en
Original, les remplissages de I, O, J et T ; en Couleurs, ceux de I, O, Z et S, jour et nuit. Elles
sont lues dans les styles des pièces, sans être recopiées ; `feature:home` dépend de `core:board`
pour cela (ADR 0023). En Couleurs, le logo v2 le remplace depuis P5.77 (« Héros de l'accueil »).

**Tests :** `AppThemeTest › each theme chosen on the home screen has its theme in the design system`,
`AppThemeTest › the theme of the application follows the saved theme, and shows none before it is read`,
`AppThemeTest › once read, the theme is known at once to a new collector`,
`AppThemeTest › changing the music or the start level does not emit the theme again`,
`WindowBackgroundTest › the window background is the page colour of the Original theme`,
`HomeLogoTest › in the Original theme the logo is a square of four cells in the four greens of the pieces`,
`HomeLogoTest › in the Colorful theme the logo is a square of four cells in the colours of I, O, Z and S`,
`LogoColorsTest › the logo cells of each theme are fills of its pieces`. Vues : V1, V2, V3, V4.

**Icône et écran de démarrage** (P5.1) : icône adaptative (`mipmap-anydpi`, minSdk 26, sans
`roundIcon`) : fond `blox_window_background` (page Original), premier plan des quatre cellules du
logo aux couleurs `BloxPalettes.Original.logoCells` (cellules de 22 dp, joint de 2 dp, carré de
46 dp centré sur 108, dans la zone sûre de 66 dp quel que soit le masque), couche monochrome de
même forme pour les icônes thématiques. Écran de démarrage d'Android 12 et plus par le thème
(`values-v31`, `Theme.Blox` héritant de `Base.Theme.Blox`, sans `core-splashscreen`) : fond de
page Original et icône ; il reste affiché jusqu'à la lecture du thème (première image
retenue, P3.5). La cellule pâle se distingue peu du fond (contraste d'environ 1,2:1), comme sur
le logo de l'accueil.

**Tests :** `LauncherIconTest › the four cells of the icon are the logo colours of the Original theme, in reading order`,
`LauncherIconTest › the adaptive icon has the page of the Original theme behind, the cells in front, and a monochrome layer`,
`LauncherIconTest › the application has no round icon, as every icon is adaptive from Android 8`,
`LauncherIconTest › the splash screen of Android 12 shows the page of the Original theme and the icon`,
`LauncherIconTest › the theme of Android 12 inherits the base theme and only adds the splash screen`,
`LauncherIconTest › every corner of the cells is in the safe zone of the adaptive icon, whatever the mask`,
`LauncherIconTest › the monochrome layer has the very shape of the cells in front`.

**Tests :** `CellStylesTest › in the Original theme a piece has the fill, border and centre of docs themes md, framed by a joint`,
`CellStylesTest › in the Colorful theme a piece is a bubble with the cell gloss, framed by a joint of 1 px, without border`,
`CellStylesTest › in the Original theme an empty cell is the board background without border`,
`CellStylesTest › in the Colorful theme an empty cell is framed by a joint of 1 px, as a piece is`,
`BoardGeometryTest › a cell is the smaller of the width over 10 and the height over 20, in whole pixels`,
`BoardGeometryTest › a cell and the board origin are whole numbers of pixels`,
`BoardGeometryTest › the board is centred in the available space`,
`CellPixelsTest › a fraction of the cell is drawn as a whole number of pixels, at least one`,
`DisplayedCellsTest › without a piece in play the displayed cells are the 200 cells of rows 0 to 19, row by row`,
`DisplayedCellsTest › the piece in play is drawn over the board, without its cells in the hidden rows`.
Vues : V2, V3.

## Thème « Original » — vert sauge pastel

Inspiré des écrans monochromes, adouci : pas de vert fluo. Les pièces se distinguent par leur
**motif** autant que par leur nuance, sinon S et Z seraient indiscernables.

| Jeton | Valeur | Usage |
|---|---|---|
| `page` | `#CFE3C8` | Fond de l'écran |
| `boardBackground` | `#DCEBD5` | Fond du plateau |
| `panel` | `#BFD6B6` | Encarts (score, niveau…) |
| `ink` | `#34503A` | Texte, contours, cadre |
| `button` | `#5F7F5C` | Boutons de commande |
| `buttonInk` / `accentInk` | `#EEF5EA` | Pictogrammes sur boutons |
| `accent` | `#34503A` | Bouton principal (A, Jouer) |
| `buttonPressed` | `#34503A` | Bouton de commande tenu |
| `buttonOutline` | `#5F7F5C` | Cadre des boutons de commande (égal à `button`, donc invisible) |
| `accentPressed` | `#1F3324` | Bouton principal tenu |

Police : pixel (*Press Start 2P* dans la maquette, licence OFL), coins carrés.

| Pièce | Fond | Bordure | Carré central |
|---|---|---|---|
| I | `#34503A` | `#34503A` | `#34503A` (plein foncé) |
| O | `#87A67F` | `#34503A` | `#34503A` |
| T | `#EEF5EA` | `#34503A` | `#34503A` (contour + point) |
| S | `#87A67F` | `#87A67F` | `#87A67F` (plein moyen) |
| Z | `#34503A` | `#34503A` | `#BFD6B6` (foncé, point clair) |
| J | `#BFD6B6` | `#34503A` | `#87A67F` |
| L | `#87A67F` | `#34503A` | `#EEF5EA` |

Cellule vide : fond du plateau, sans bordure. Bordure de 2 px sur une cellule de 24 px. Carré
central de 8 px sur 24, centré (valeur retenue en P2.3, la maquette ne la donnait pas : à
vérifier sur la maquette Claude Design). Chaque cellule de pièce est entourée d'un joint de
1/24 de cellule (au moins 1 px) de la couleur du fond sur lequel elle est dessinée (plateau ou
encart), soit 2/24 entre deux cellules : deux cellules en aplat voisines restent distinctes
(P2.17). Le thème Couleurs n'a pas de joint.

## Thème « Couleurs »

**Couleurs v2** (P5.57, piste C « Y2K irisé ») : le thème suit le mode sombre du téléphone. Le
choix du joueur (`BloxThemeKind`) et le mode du système donnent la variante dessinée
(`BloxThemeVariant` : `ORIGINAL`, `COLORFUL_LIGHT`, `COLORFUL_DARK`), exposée par
`BloxTheme.variant` ; Original n'a qu'une variante, claire. Changer de mode recrée l'activité, ce
qui met en pause une partie en cours (ADR 0020). Les palettes de jour et de nuit
(`BloxPalettes.ColorfulLight`, `ColorfulDark`) prennent les valeurs de
[`tokens.json`](design-system/tokens.json), seule source (ADR 0019) : le plugin de convention
`blox.design.tokens` passe ce fichier aux tests de `core:designsystem`, et `PaletteTokensTest`
compare chaque jeton à sa propriété ; une liste des jetons non codés, avec leur raison, est prévue mais vide
(depuis P5.88, `cell-ground` et `grid-line` font la grille de l'éditeur). Les jetons des pièces sont comparés
dans `core:board` (`PieceTokensTest`), avec le même lecteur, partagé par les doublures de test de
`core:designsystem`.
Le schéma Material part de la base claire ou sombre selon la variante, les icônes des barres système aussi
(P5.89, `BloxSystemBars` dans `app`) : sombres sur Original et Couleurs jour, claires sur Couleurs nuit, quel que
soit le mode du téléphone, réappliquées quand le thème ou le mode change ; avant la lecture du thème, celles
d'Original. Tests : `SystemBarsTest › the icons of the system bars are dark on a light variant and light on a dark one`,
`SystemBarsAppearanceTest › the system bars take the appearance of the variant drawn, not that of the mode of the system`,
`SystemBarsAppearanceTest › the system bars change their appearance as soon as the variant drawn changes`. Sous le Serrurier,
toujours dessiné en Couleurs (P7.18), les barres prennent sa variante Couleurs du mode du téléphone, quel que soit le
thème choisi : `SystemBarsTest › the bars of a screen drawn in colorful only follow its colorful variant, whatever the theme chosen`,
`SystemBarsTest › the bars of every other screen follow the theme chosen`,
`SystemBarsAppearanceTest › at night in the Original theme, the bars turn light under the Locksmith, and dark again once it is left`.
Les formes, polices et
pictogrammes ne dépendent que du thème (`BloxTheme.kind`), pas du mode. Les styles de pièces, le
dessin du plateau et de la pièce suivante prennent la variante ; leurs pinceaux sont construits une
fois par variante, taille et fond. La cellule vide prend `emptyCell`, séparée du plateau par une
marge de 1 px en `boardBackground`.

L'ancienne palette sombre (`page` `#12141C`, accent orange `#EF8D3B`) et les biseaux des pièces
v1 sont retirés ; leurs valeurs restent dans l'historique de ce fichier.

**Repères autres que la couleur** (P5.57, décision « Mixte » du développeur) : là où un aplat
n'atteint pas le contraste voulu, surtout dans les pastels du jour, l'état se lit autrement. La
touche tenue garde son cadre et descend de 2 dp, le choix retenu se lit à sa coche,
l'interrupteur à son cadre `buttonOutline` (l'encre en Original), la partie tenue d'une note à
ses bords de 2 dp dans la couleur de sa piste.

**Tests :** `BloxThemeVariantTest › the Colorful theme is the day variant in light mode and the night variant in dark mode`,
`BloxThemeVariantTest › the Original theme has a single variant, whatever the mode of the system`,
`BloxThemeVariantTest › each variant has its palette and its Material colour scheme, light or dark`,
`PaletteTokensTest › each colour token of tokens json has its value in the palette of the day and in the palette of the night`,
`PaletteTokensTest › every colour token of tokens json is in the palette, in the pieces or listed as not coded, with its reason`,
`PaletteTokensTest › a colour written rgba in tokens json is read with its alpha`,
`CellStylesTest › in the Colorful theme an empty cell is framed by a joint of 1 px, as a piece is`.
Vues : V22 à V34 (jour), nuit dans le design system.

**Cases « bulles »** (P5.74, `core:board`, composant Piece) : une couleur par pièce, jour et nuit,
aux valeurs de `tokens.json` (`piece-<x>` et son reflet `piece-<x>-hi`). Une case est un dégradé
vertical du reflet vers la teinte (à 55 % de la hauteur), arrondie, entourée d'une marge de 1 dp (arrondie au pixel supérieur, pour que le liseré du relief y
tienne à toute densité) dans la couleur du fond ; Original garde son joint de 1/24 de case. À 16 dp et plus (case entière, joint compris),
coins `radius-cell` (6 dp) et relief `cell-gloss` ; en dessous, coins de 2 dp et aucun relief. De
jour, le liseré du relief remplit cette marge : entre deux pièces, le joint en prend la couleur,
comme dans le design system. La case vide a les coins et la marge d'une pièce, en
`emptyCell` sur `boardBackground`, sans relief. Les chemins, dégradés et reliefs sont construits
avec les peintres, une fois par variante, taille, densité et fond, jamais pendant le déplacement
de la pièce. Original ne change pas.

**Tests :** `PieceTokensTest › each piece token of tokens json is the fill and the highlight of its piece, day and night`,
`CellLayoutTest › a cell under 16 dp has 2 dp corners and no gloss`,
`CellLayoutTest › a cell of 16 dp and more has the corners of radius cell and the gloss of cell gloss`,
`CellLayoutTest › an empty cell has the corners and the margin of a piece, without gloss`,
`CellLayoutTest › a bubble has a margin of 1 dp whatever its size`,
`CellLayoutTest › the ring of the gloss fits in the margin`,
`BoardCanvasTest › the painters are built again when the density changes`,
`CellStylesTest › in the Colorful theme a piece is a bubble with the cell gloss, framed by a joint of 1 px, without border`,
`BoardCanvasTest › the painters are built once while the piece moves` ; captures `BoardColorful`,
`BoardColorfulNight`, `BoardColorfulSmall`. Vues : V3, V22.

Polices : Manrope, Unbounded et Bagel Fat One (licence OFL, section « Polices ») ; coins selon `BloxShapes` (P5.59).
**Commandes** (P2.17, P5.57) : une touche tenue prend la teinte « pressée » de sa couleur
(`buttonPressed`, `accentPressed`), garde son encre et son cadre, et descend de 2 dp
(`BloxSizes.pressDrop`, partagé avec les boutons bulles). Elle se distingue de la page par son cadre (au moins 3:1) et de la même
touche au repos d'au moins 1,5:1 ; de jour, A tenue ne se distingue de A au repos que par ce
décalage (une teinte ne peut être à la fois à 1,5:1 de l'accent et à 4,5:1 sous l'encre). Les
pictogrammes de rotation de B et A (28 dp, sans lettre depuis P5.79) et les flèches sont des pictogrammes
(3:1).

**Cadre des touches** (P2.22, P5.57) : chaque touche est entourée d'un cadre de 2 dp
(`BloxBorders.frame`) en `buttonOutline`, qui la détache de la page d'au moins 3:1 (WCAG 1.4.11,
composant non textuel), tenue ou non. La touche A est cadrée en `accentPressed` : l'accent seul ne
se détache de la page du jour qu'à 2,3:1. Dans le thème Original, le cadre a la couleur du bouton
(3,31:1 contre la page) : rien ne change à l'œil. Le bouton pause de la barre du haut porte les
disque de la famille Icône des boutons bulles (P5.78 ; avant, les couleurs d'une touche au repos, P2.23).

**Tests :** `KeyColorsTest › a held key stands out from the page by its outline, by a contrast of 3 to 1 at least`,
`KeyColorsTest › a held key looks other than a key not held`,
`KeyColorsTest › a held key stands out from the key at rest by a contrast of 1,5 to 1 at least`,
`KeyColorsTest › for A in the day the held key differs from it at rest by less than 1,5 to 1, so its 2 dp drop tells it`,
`KeyColorsTest › the glyph of a key stands out from its face, by a contrast of 3 to 1 at least`,
`KeyColorsTest › a key at rest stands out from the page by its outline, by a contrast of 3 to 1 at least`,
`KeyColorsTest › a held key keeps the outline it has at rest`,
`KeyColorsTest › the key A is framed in the pressed shade of the accent, held or not`,
`KeyDropTest › a held key goes 2 dp down`,
`CellStylesTest › in the Original theme a piece has the fill, border and centre of docs themes md, framed by a joint`,
`CellLayoutTest › two flat cells side by side in the Original theme are apart by their two joints, of 1 px each at least`.
Vues : V2, V3.

**Retours de jeu, première tranche** (P5.80, `feature:game`, ADR 0024) : `GameFeedback`, pur et compté en ticks, vit
dans `GameUiState` ; l'écran ne lit ses valeurs qu'au dessin, par un calque posé sur le plateau (`BoardEffects`,
`core:board`). La pause fige les effets. Une pièce posée éclaire ses quatre cases de `clear-glow` à 0,6, qui s'éteint
en 8 ticks. Une rotation fait tourner de ±90° le pictogramme de **sa** touche, qui revient à 0 en 6 ticks ; chaque
touche tourne pour elle-même (`PieceRotated` porte le sens : A horaire, B anti-horaire). L'effacement est calé sur
`Phase.Clearing`, donc sur le délai NES : les rangées s'éclairent de `clear-glow`, leurs cases éclatent par paires
depuis le centre (5 paires en durée − 1 ticks ; la case gonfle à 1,2 puis s'efface, dessinée en dernier dans sa
rangée), puis les rangées du dessus glissent à leur place en 8 ticks (sortie douce). L'autocollant « +points »
monte d'une case et demie en 45 ticks, penché de −4°, placé par la géométrie du plateau (`boardPoint`) ; TalkBack
ne le lit pas. Le score compte jusqu'à sa nouvelle valeur en 30 ticks, seul son texte recompose. **Effets réduits** :
rien ne bouge, les rangées effacées s'affichent en `clear-glow` sans leurs cases, le score s'affiche d'un coup et
l'autocollant s'efface en 8 ticks au plus.

**Tests :** `GameFeedbackTest › a lock lights its four cells for 8 ticks`,
`GameFeedbackTest › a rotation turns the icon of its key by 90 degrees in 6 ticks`,
`GameFeedbackTest › a turn of B leaves the turn of A going on`,
`GameFeedbackTest › the clear pops the cells by pairs from the centre within the clear delay`,
`GameFeedbackTest › once the rows are cleared the rows above them slide down to their place`,
`GameFeedbackTest › the points sticker rises and fades in 45 ticks`,
`GameFeedbackTest › the score counts up to its new value once the rows are cleared`,
`GameFeedbackTest › in reduced effects nothing moves and the score shows at once`,
`GameFeedbackTest › in reduced effects the sticker fades out in 8 ticks at most`,
`GameViewModelTest › the clear the screen shows starts with the lock and lasts as long as the clear of the engine`,
`GameViewModelTest › a rotation reaches the screen as the turn of its key, and a lock as its cells`,
`GameViewModelTest › a paused game freezes its effects`,
`BoardPointTest › a point of the board is where the canvas draws it, in a height that is no multiple of 20`,
`BoardCanvasTest › the painters are built once while effects play`,
`BoardCanvasTest › a board whose effects are at rest is drawn as a board without effects`,
`GameScreenTest › nothing recomposes while an effect plays`,
`GameScreenTest › while the score counts, only its text recomposes`,
`GameScreenTest › the icon that turns is the one of A, the key that turned the piece, and B stays still`,
`GameScreenTest › the points sticker is not read by TalkBack`,
`GameScreenTest › the effects are drawn over the game, and gone once over` ;
captures `GameScreenClearing*` et `GameScreenCleared*` (jour, nuit, grande police, Effets réduits). Vue : V28.

## Choix de l'accueil

**Groupe de choix v2** (P5.63, `ChoiceGroup` dans `core:designsystem`, composant Choice) : il remplace les
choix de l'accueil décrits ci-dessous (`Choice`, `ChoiceRow`, `Section`, l'ancienne `ChoiceCheck`, devenue
`CheckMark`, partagée). Un groupe est cadré comme un bouton secondaire (`button`, cadre `buttonOutline` ; en
Original, la page, cadrée de la face des boutons) ; le segment retenu est en `accent`, en pilule, avec sa coche
en `accentInk` (la coche porte l'état : de jour, l'accent ne fait que 2,6:1 contre le groupe) ; un segment
indisponible est en `label`, marqué « BIENTÔT », et ne se choisit pas (TalkBack : « Moderne, bientôt », puis
l'état désactivé). Le libellé du groupe est un titre pour TalkBack, les segments un groupe de boutons radio.
Un groupe qui ne tient pas sur la ligne de son libellé passe dessous, sur toute la largeur ; si un libellé
devait encore rétrécir sous 75 % de sa taille (marges et coche gardées), les segments s'empilent. Segments de
48 dp de haut, cadre compris ; focus clavier par l'anneau des boutons ; ordre inversé de droite à gauche.
Avant la jauge de l'accueil (P5.75) et la feuille Musique (P5.76), le niveau de départ
(5 colonnes) et la musique (2 colonnes) étaient des groupes sur plusieurs rangées : fond retenu en `md`, coche
dans le coin. Libellés du groupe alignés au début de la ligne. `ChoiceColors.kt`, qui servait encore à `Setting`,
est retiré avec lui (P5.61) ; le type `ChoiceColors` de P5.95 (couleurs retenues d'un segment) n'a rien à voir avec lui.

**Tests :** `ChoiceLookTest › a selected segment is filled in accent and checked in the ink of its label`,
`ChoiceLookTest › an unavailable segment is clear, its label in label, unchecked`,
`ChoiceLookTest › a group is filled and framed as a secondary button, its frame standing out from the page by 3 to 1 at least`,
`ChoiceLookTest › the label of a segment stands out from what is behind it by 4,5 to 1 at least, its check by 3 to 1`,
`ChoiceLookTest › a segment focused by the keyboard bears a 3 dp ring in ink, as a button`,
`ChoiceLayoutTest › cells stay side by side while their labels need not shrink below three quarters of their size`,
`ChoiceLayoutTest › cells stack when their labels would shrink below three quarters of their size, margins and check kept`,
`ChoiceLayoutTest › the check in the corner of a cell stays within its fill, whose corners are those of radius-md`,
`ChoiceLayoutTest › a group asks for a selected choice that is available, and one column at least`,
`ChoiceGroupTest › exactly one segment is selected and bears the check, the others neither`,
`ChoiceGroupTest › an unavailable segment reads « Moderne, bientôt », disabled, and cannot be chosen`,
`ChoiceGroupTest › a group too wide for its line puts its label above and takes the whole width`,
`ChoiceGroupTest › each segment is 48 dp high as a target`,
`ChoiceGroupTest › TalkBack reads the label as a heading, then the segments as one group of radio buttons`,
`ChoiceGroupTest › in a group of several rows, an unavailable choice bears BIENTÔT too` ;
captures `ChoiceGroups*`. Vues : V27.

**Segments sans titre** (P5.95, `ChoiceSegments`) : les segments d'un groupe de choix sans libellé visible, nommés
pour TalkBack par la description du groupe (« Piste », « Outil »), pour les pistes et les outils de l'éditeur
(P5.102). Un segment peut porter un pictogramme décoratif (`ChoiceItem.icon`) que la coche remplace quand il est
retenu ; sa place est réservée, le libellé ne bouge pas. Les couleurs du segment retenu peuvent être données
(`ChoiceItem.selectedColors`, `ChoiceColors(fill, ink)`, ici les couleurs de la piste) ; l'appelant garantit alors
4,5:1 entre l'encre et le fond. Les pictogrammes ne vont que dans un groupe d'une rangée. TalkBack lit le nom du
groupe comme un arrêt à part : à vérifier sur appareil avec P5.102.
**Tests :** `ChoiceSegmentsTest › segments without a visible label are one group of radio buttons, read under the name given to TalkBack`,
`ChoiceSegmentsTest › a segment bears its icon before its label, and the selected one its check in its place`,
`ChoiceSegmentsTest › a selected segment is filled in the colours given to its choice`,
`ChoiceSegmentsTest › each segment is 48 dp high as a target`,
`ChoiceLookTest › a selected segment takes the colours given to it, its label standing out by 4,5 to 1 at least, day and night`,
`ChoiceLayoutTest › only a group of one row bears icons, where its check takes the place of an icon` ; captures
`ChoiceSegments*`.

Un choix sélectionné (mode, thème, niveau de départ, musique) est rempli en `accent`, son
libellé en `accentInk`. Il ne se distingue pas que par la teinte (WCAG 1.4.1) : les deux états
diffèrent d'au moins 3:1 en luminance, et le choix non sélectionné est cadré quand le
sélectionné est plein. Depuis Couleurs v2 (P5.57), l'accent pastel du jour ne se détache du
`panel` blanc qu'à 2,6:1 : c'est la coche, à 3:1 au moins de son fond, qui porte l'état dans tous
les thèmes. Le soulignement ajouté en P5.26 est retiré à la demande du développeur
(P5.36). Un
choix non sélectionné est dessiné comme un encart du jeu : `panel`, cadre de 2 dp en
`panelBorder` (`BloxBorders.frame`, partagé avec les encarts et `SecondaryButton`), libellé en `ink`, forme
`button` (carrée en Original) ; le choix sélectionné porte un cadre de sa propre couleur, pour
que tous gardent la même taille. Les deux états se distinguent d'au moins 3:1 (WCAG 1.4.11), le
libellé de son fond d'au moins 4,5:1 (P5.26, constat C3 de la revue qualité). Aucun jeton de palette n'a changé. Le code était `choiceColors` (`feature:home`, retiré en P5.61 ; les segments sont dessinés par `segmentLook` depuis P5.63), sur le modèle de
`keyColors`.

**Réglages** (P5.37, P5.61) : depuis P5.61, une rangée de réglage est `SettingSwitchRow` (`core:designsystem`,
composant Switch) : pictogramme, libellé, sous-ligne et piste, toute la rangée étant l'interrupteur ; une
rangée ne se désactive qu'avec sa raison, montrée à la place de la sous-ligne. Allumé, le pouce est à droite,
plus grand et coché, la piste en `accent` avec le relief d'un bouton ; éteint, à gauche, sur une piste `button`
(la page en Original). Pressée, la rangée se remplit en `buttonPressed`, le pouce grandit à 28 dp et revient en
`motion-press` comme un bouton ; la piste est alors cadrée dans l'encre de la rangée pressée, à 3:1 au moins.
Au repos, elle est cadrée en `buttonOutline` (l'encre en Original), à 3:1 sur la page. Désactivée : plate, piste
`panelInset` cadrée de `panelBorder`, pouce et libellé en `label` ; en Original, où ces couleurs valent l'encre,
le pouce est creux. Focus clavier : l'anneau des boutons (`FocusRing`). TalkBack lit une rangée comme un
interrupteur, son libellé, sa sous-ligne et son état « Allumé » ou « Éteint ». En police agrandie, seule la piste
passe à la ligne, à droite ; le libellé rétrécit au plus de 10 % pour garder ses mots entiers (Original ×2).
Remplace `Setting` et `settingColors` ; le pictogramme `VIBRATE` est ajouté.
**Tests :** `SwitchLookTest › a switch on has its thumb on the right, larger, checked, a switch off on the left`,
`SwitchLookTest › a pressed row fills with button pressed and its thumb grows to 28 dp`,
`SwitchLookTest › the track is framed in button outline at rest and in the ink of its row pressed, by 3 to 1, on and off`,
`SwitchLookTest › a focused row bears a 3 dp ring in ink, 3 dp off its edge`,
`SwitchLookTest › in Original the thumb of a disabled row is hollow, its outline alone drawn`,
`SwitchLookTest › the label and the sub-line stand out from their row by 4,5 to 1, at rest on the page and pressed`,
`SettingSwitchRowTest › TalkBack reads one switch per row, its label, its sub-line and its state`,
`SettingSwitchRowTest › a disabled row reads as unavailable and its sub-line says why`,
`SettingSwitchRowTest › TalkBack reads Allumé or Éteint as the state of a row, apart from its unavailability`,
`SettingSwitchRowTest › the whole row is the switch: a tap on its pictogram, its label or its track turns it over` ;
captures `Switches*`. Vues : V21, V31.

**Héros de l'accueil** (P5.77, `feature:home`, `core:board`, V27) : en Couleurs, le logo BloX v2 (`BloxLogo`, Bagel
Fat One en dégradé `iris`, la mascotte Serrurier à la place du « o », `docs/design-system/pictogrammes.md`), lu
comme le titre de l'écran, centré sous la barre ; une ombre floue ne dessine que son flou, les lettres restant
transparentes (`SOFT_SHADOW`, partagée dans `core:board`). Original garde son logo de quatre cases. Dessous, un
aperçu du plateau (`BoardRowsCanvas`, les 6 rangées du bas en cases de 14 dp, `boardRowsSize`) rejoue un T qui
tombe à la gravité du niveau choisi et efface deux lignes, montrées effacées 2 s ; son score est réel (100 × (niveau
+ 1) par ligne, `000600` au niveau 5) ; un nouveau niveau relance l'aperçu. Halo de Jouer (`PlayHalo`) : rien
pendant 3 s, puis trois impulsions (à 3, 4 et 5 s, d'1 s chacune, pilule qui s'élargit de 12 dp en s'effaçant, en
`accent-ring`), puis une toutes les 10 s après la troisième ; un toucher sur l'accueil le remet à 3 s. L'aperçu, le
score et le halo ont chacun leur calque, invalidé seulement quand leur valeur change : rien ne recompose.
**Effets réduits** : l'aperçu montre son état final (lignes effacées et points), aucun halo. Bagel Fat One reste
embarquée. Point ouvert : le flou de l'ombre (`BlurMaskFilter`) est net sous l'API 28, à juger sur un appareil.
**Tests :** `PreviewReplayTest › the preview drops a row every ticks per row of the chosen level`,
`PreviewReplayTest › the preview scores 100 times the level plus one for its two lines`,
`PreviewReplayTest › a preview given a new level starts again with a new T, at the gravity of that level`,
`PreviewReplayTest › the preview goes on where it is when the level stays the same`,
`PreviewReplayTest › the preview plays again once its two lines have shown cleared for 2 seconds`,
`PreviewReplayTest › the still preview, for reduced effects, shows its two lines cleared and their points`,
`PreviewReplayTest › the preview scores 000000, then 000600 once its lines clear at level 5, and 000600 at once still`,
`HomeMotionTest › the ticks of the screen play the preview at the level chosen and bring the halo of Jouer`,
`HomeMotionTest › the preview starts again with the new level when the level changes`,
`HomeMotionTest › a touch on the home puts the halo off for 3 seconds again`,
`HomeMotionTest › the preview notifies a change only when the T drops a row or locks`,
`HomeMotionTest › the halo notifies no change while it is off`,
`PlayHaloTest › the halo of Jouer stays off for 3 seconds`,
`PlayHaloTest › the halo of Jouer pulses 3 times after 3 seconds, then once every 10 seconds after the third`,
`PlayHaloTest › the halo is a pill around Jouer, which grows to its full spread as it fades`,
`PlayHaloTest › without a pulse, there is no halo to draw`,
`LogoLookTest › by day, Bl goes from iris-1 through iris-2 to iris-3 and X back, over a rim, a lilac and a shadow`,
`LogoLookTest › by night, Bl goes from iris-1 to iris-3 and X to the pink of the night, in a halo over a dark shadow`,
`LogoLookTest › a blurred shadow is drawn as its blur alone, the letters it is cast by left transparent`,
`LogoLookTest › a shadow without blur is drawn as the letters in its colour, lower`,
`DisplayedCellsTest › the cells of the bottom rows are the last rows of the board, the piece drawn over them`,
`DisplayedCellsTest › asking more rows than the board has, or none, is refused`,
`BoardRowsSizeTest › the bottom rows of the board take ten cells across and one cell per row`,
`BoardCanvasTest › the bottom rows of a board are drawn again as it changes, without recomposing, TalkBack reading nothing`,
`HomeScreenTest › the logo BloX, read as the heading of the screen, stands centred under the bar, the preview under it`,
`HomeScreenTest › nothing on the home recomposes while the preview plays`,
`HomeScreenTest › a touch on the home puts the halo of Jouer off for 3 seconds again`,
`HomeScreenTest › in reduced effects the preview and the halo stand still` ;
captures `Logo*`, `HomeColorful*`. Vue : V27.

**Paramètres v2** (P5.70, `feature:home`, déplacée dans `feature:settings` en P12.2, V31) : barre avec Retour et « Paramètres » ; titres « Sons et vibrations »
et « Record » au-dessus de leurs cartes (`panel`, rayon `window`, cadre `panelBorder`). Effets sonores, Volume des
effets (grisé tant que les effets sont coupés), Volume de la musique (`SettingVolumeRow`, `VolumeSlider` en %
converti en `Volume`), Vibrations, Vibration des touches (« Un léger toucher à chaque touche ») ; sans vibreur, les
deux vibrations sont grisées et disent « Cet appareil ne vibre pas » ; « Valable dès la partie suivante. ». Un
réglage allumé se fait sentir une fois, après un enregistrement réussi : la rotation pour les effets, la
vibration des lignes ou d'une touche pour les vibrations. Record : `SettingActionRow`, Effacer en bout de ligne
(dessous en grande police), lu « Effacer le record du Classique » ; il ouvre une `ConfirmWindow` (générique,
`DeleteWindow` réécrite dessus) qui propose d'abord « Garder le record » ; pendant l'écriture, la fenêtre reste
ouverte, réponses désactivées ; un échec la garde ouverte et le dit ; un succès la ferme et « Record effacé »
s'affiche dans une région vive présente dès l'ouverture de la page. Effacer est un bouton `DANGER` (cadre et
libellé `danger`, rempli pressé). Les curseurs gardent le pas sous le doigt pendant un glissé et ne demandent
chaque pas qu'une fois.
**Tests :** `SettingsViewModelTest › turning the sound effects on plays the rotation once`,
`SettingsViewModelTest › turning a vibration on vibrates once`,
`SettingsViewModelTest › clearing the record asks first, and keeping it changes nothing`,
`SettingsViewModelTest › a confirmed clear clears the record and says « Record effacé »`,
`SettingsViewModelTest › a clear that cannot be written keeps the window open and says it failed`,
`SettingsViewModelTest › while the record is being cleared, the window stays open and answers nothing more`,
`SettingsScreenTest › without a vibrator both vibrations are disabled and say « Cet appareil ne vibre pas »`,
`SettingsScreenTest › the volume of the effects is disabled while the sound effects are off`,
`SettingsScreenTest › once cleared, the page says « Record effacé » in a live region there from the start, and the window is gone`,
`ConfirmWindowTest › the window asks its question as a heading, then offers the safe answer first, each calling its own`,
`VolumeSliderTest › a drag asks for each step once, and the knob follows the finger while the value has not come back`,
`ButtonLookTest › a danger button is framed and labelled in danger at rest, filled in danger pressed` ;
captures `Settings*`, `DangerButtons*`. Vues : V21, V31.

**Portée de la page** (P12.3, [ADR 0030](adr/0030-reglages-generaux-et-par-jeu.md)) : la clé
`SettingsRoute(section: SettingsSection? = null)`, sérialisable, porte la section du jeu qui ouvre la page
(`SettingsSection.CLASSIC`, seule valeur) et la garde après la mort du processus. Ouverte depuis l'accueil du
Classique, la page montre sous sa barre le titre « Réglages du Classique » et la carte Record, puis le titre « Tous
les jeux » au-dessus d'Affichage et de Sons et vibrations ; ces deux titres de partie sont des titres pour TalkBack,
en `ink` et dans le style `title`, plus grands que les titres de section. Ouverte sans section, elle ne montre que
« Tous les jeux » : ni « Réglages du Classique », ni Record. La Graine du jour ouvre encore la section du Classique
jusqu'à P12.6 ; le choix du jeu l'ouvre sans section (P12.5, parcours
`BloxNavDisplayTest › the gear of the choice of the game opens the settings with no record, and Retour goes back to it`).
**Tests :** `SettingsRouteTest › a route restored after the death of the process keeps its section, or none`,
`SettingsRouteSavingTest › the key of the page opened from the Classic is saved and restored with its section`,
`SettingsScreenTest › the page opened from the choice of the game shows no record`,
`SettingsScreenTest › the section of the Classic is read Réglages du Classique, a heading`,
`SettingsScreenTest › the title of a part, Tous les jeux, is larger than the title of a section, Affichage`,
`SettingsScreenTest › opened from the Classic, its section comes first, then Tous les jeux, a heading, above Affichage`,
`BloxNavDisplayTest › the gear of the home screen opens the settings on the section of the Classic, its record within` ;
captures `Settings*`, dont `SettingsAllGames` (sans section). Vues : V21, V31.

**Effets réduits** (P5.71, ADR 0024) : `BloxEffects(reduced, forcedBySystem)`, porté par `BloxTheme` et lu par
`BloxTheme.effects`, résolu par une fonction pure (`BloxEffects.resolve`) depuis le choix du joueur et les
animations d'Android, observées en continu (`SystemAnimations`, réglage `ANIMATOR_DURATION_SCALE`, écran partagé
compris) ; le thème et le choix d'effets sont lus ensemble avant la première image (`AppTheme.appearance`). Carte
Affichage en tête des Paramètres : Effets réduits (« Moins de mouvements et de particules ») ; quand Android
retire les animations, l'interrupteur est allumé, bloqué et dit « Imposé par les réglages d'Android », sans
écrire le choix enregistré ; sous la carte, « Le thème se choisit à l'accueil. ». En Effets réduits, un bouton
pressé ne change que de couleur et revient aussitôt ; une touche de jeu tenue garde sa descente de 2 dp (seul
repère de A tenue de jour). Un sous-titre de rangée de réglage peut prendre trois lignes.
**Tests :** `BloxEffectsTest › effects are reduced when the player asks for it`,
`BloxEffectsTest › effects are reduced and forced when Android removes the animations, whatever the choice`,
`SystemAnimationsTest › removing the animations while the application runs is seen at once, and so is giving them back`,
`AppThemeTest › the theme and the choice of effects are known together, and neither before the settings are read`,
`ButtonLookTest › in reduced effects a pressed button changes its fill only`,
`HeldKeyTest › a held key goes 2 dp down with its tag and what TalkBack reads, reduced effects or not`,
`SettingsScreenTest › a forced switch reads « Imposé par les réglages d'Android » and cannot be turned off`,
`SettingsScreenTest › in a font twice as large, on 360 dp, every sub-line shows whole, its words whole`.

**Coche** (P5.42, essai demandé par le développeur) : le choix sélectionné porte une coche
dessinée (un trait qui descend puis remonte, bouts carrés) en `accentInk`, repère qui ne tient
pas à la couleur (WCAG 1.4.1). Elle se place avant le libellé, aussi haute que lui (1 em, 24 dp
au plus) et à 0,25 em de lui (4 dp au moins) ; pour les niveaux de départ, trop étroits, dans le
coin en haut à droite (10 dp, à 4 dp des bords), et chaque niveau garde 14 dp de chaque côté du
chiffre pour que la coche ne le touche pas : le chiffre reste centré et la cellule a la hauteur
des autres choix, même en police agrandie (P5.29). Seul le choix sélectionné réserve la place de sa coche : une rangée ne s'élargit
que d'une coche et passe à la ligne au même endroit ; elle se décale de quelques dp quand le choix
change. La coche est décorative : TalkBack annonce déjà « sélectionné ». En police ×2, le nom de
40 caractères d'une musique personnelle est coupé un mot plus tôt qu'avant (« une longue
soir… ») ; TalkBack le lit en entier. Code : `ChoiceCheck.kt` (`checkStroke`, `choiceInsets`).

Contrastes mesurés en P5.26, avant Couleurs v2 (les valeurs v2 sont vérifiées par les tests) :

| | Original | Couleurs v1 |
|---|---|---|
| Sélectionné / non sélectionné (`accent` / `panel`) | 5,7:1 (avant 1,99:1) | 6,7:1 (avant 5,48:1) |
| Libellé sélectionné (`accentInk` sur `accent`) | 8,0:1 | 7,5:1 |
| Libellé non sélectionné (`ink` sur `panel`) | 5,7:1 (avant 4,0:1) | 14,6:1 |

**Tests :** `ChoiceLookTest › the label of a segment stands out from what is behind it by 4,5 to 1 at least, its check by 3 to 1`,
`ChoiceLookTest › a group is filled and framed as a secondary button, its frame standing out from the page by 3 to 1 at least`,
`ContrastTest › black on white has the greatest contrast, 21 to 1, whatever the order`,
`HomeScreenTest › TalkBack reads each selected choice as selected, and the others as not selected`,
`ChoiceLookTest › a selected segment is filled in accent and checked in the ink of its label`,
`ChoiceCheckTest › the check is a stroke down then up, drawn wholly within its square`,
`ChoiceCheckTest › only a selected choice makes room for its check and its gap before its label`,
`ChoiceCheckTest › the check is as large as its label, 24 dp at most, a quarter of the label away from it, 4 dp at least`,
`HomeScreenTest › each selected choice bears a check, the others none, and TalkBack does not read it`.
Vues : V1.


## Accessibilité : contrastes et tailles de cible (P5.3)

Tout texte se détache de son fond d'au moins 4,5:1 dans chaque thème codé (Original,
Couleurs) : `ink` et `label` sur `page` et sur `panel`, `accentInk` sur `accent` (badge de
record, `PrimaryButton`, choix sélectionné, bouton « Jouer », qui reçoit ces couleurs
explicitement). Aucune couleur n'a changé. Brume n'existe que dans la maquette : il entrera dans
ces tests quand il sera codé.

Toute cible (bouton, choix, touche) fait au moins **48 dp visibles** de large et de haut
(`BloxSizes.minTouchTarget`, une seule constante) : c'est une règle de BloX, plus stricte que
WCAG 2.2 (2.5.8, 24 px) et qu'Android (qui accepte une zone tactile plus grande que le dessin).
« Jouer » passe ainsi de 40 à 48 dp de haut. Les niveaux de départ, en deux lignes de 5 depuis
P5.29 (décision du développeur), font 59 dp de large à 360 dp : il n'y a plus d'exception.

**Tests :** `PaletteContrastTest › a text stands out from its background by a contrast of 4,5 to 1 at least`,
`HomeScreenTest › every target of the home is 48 dp at least on 360 dp`,
`GameScreenTest › every button of the screen is 48 dp wide and high at least`. Vues : V1 à V4.
## Éditeur de musique (maquette)

Couleurs des pistes de l'éditeur de musique (P4.19), retenues dans la maquette (V7 à V18) et le
prototype de l'éditeur. Le thème Brume n'existe pour l'instant que dans la maquette (V5, V6,
V13 à V18).

**Valeurs codées** (P4.19b, `BloxPalette.melody`, `bass`, `outOfScale`, `TrackColors`) : Couleurs
reprend la maquette ; Original fonce deux teintes, trop pâles pour leur contraste : note tenue
`#6A8A66` (mélodie et basse, 3,1:1 contre la case) et libellé hors gamme `#486848` (4,6:1 sur la
page, toujours plus estompé que les autres libellés ; il est aussi en italique, pour ne pas
dépendre de la couleur). L'onglet de piste choisi est rempli en `accent` en Original et dans la
couleur de sa piste en Couleurs (mélodie cyan, basse orange), son libellé à 4,5:1 au moins.
`TrackColorsTest` (`feature:composer`) vérifie ces contrastes dans chaque variante codée. Les
valeurs Couleurs ci-dessous sont celles de la v1 : depuis P5.57, celles du jour et de la nuit
viennent de `tokens.json` (`melody`, `bass`, `danger`…). De jour, la partie tenue d'une note,
pastel, ne se détache de la case qu'à 2,1 à 2,3:1 : elle est bordée en haut et en bas, sur 2 dp,
dans la couleur de sa piste, qui s'en détache d'au moins 3:1. Depuis P5.88, les cases de la grille sont en `cellGround`
et séparées par des lignes `gridLine` (jetons `cell-ground` et `grid-line`), et ces contrastes se mesurent sur
la case (`TrackColorsTest › a cell of the grid is drawn on the cell ground, between lines of the grid, the notes of its tracks on it`,
`TrackColorsTest › the lines of the grid stand out from its cells by night by a contrast of 1,3 to 1 at least` (de nuit,
`grid-line` `#2A2D44`, P5.88 ; de jour, les lignes blanches se lisent contre les cases pâles),
`TrackColorsTest › the attack of a note and the edges of its held part stand out from the cell by a contrast of 3 to 1 at least`).

**Notes en bulles** (P5.103, V33, Couleurs seulement) : une note est dessinée comme une cellule du plateau,
par le même peintre (`BubblePainter` et `bubbleLook`, `core:designsystem`, partagés avec `core:board`) :
dégradé du reflet de sa piste (`melody-hi`, `bass-hi`) en haut à sa couleur à 55 % de sa hauteur, coins
`radius-cell` et relief `cell-gloss` dès 16 dp sur son plus petit côté, coins de 2 dp sans relief en dessous (une
double croche, environ 14 dp de large, reste plate). De nuit, `melody` et `bass` valent `piece-t` et `piece-l` :
leurs reflets sont ceux de ces pièces (`#DCCBFF`, `#FFD7B0`). Les peintres sont construits une fois par aspect,
taille et densité, gardés au plus à 64, sans allocation au dessin. La grille est posée dans un bloc `board` cadré
de 2 dp de `panel-border` (`radius-md`, marge de 8 dp), la ligne « TEMPS 1 2 3 4 » en `stat-label` capitales, et un
fondu de 24 dp vers `board` marque le bas tant que des lignes restent dessous. Original garde sa grille
(`BubbleDrawingTest › a bubble is lit from its highlight at the top and is in its colour from 55 percent of its height down`,
`NotePaintersTest › a note under 16 dp has 2 dp corners and no gloss`,
`NotePaintersTest › a note of 16 dp and more has the corners of radius cell and the gloss of cell gloss`,
`NotePaintersTest › the painters of the notes are built once per size and variant, not at each touch`,
`NotePaintersTest › the painters of the notes are built anew once more than 64 sizes are kept`,
`NotePaintersTest › the painters of the notes are built again when the density changes`,
`TrackColorsTest › the highlight and the colour of each track are those of tokens json, day and night`,
`PaletteTokensTest`). Vue : V33.

| Jeton | Original | Couleurs | Brume clair | Brume sombre |
|---|---|---|---|---|
| Mélodie | `#34503A` | `#3FC5E8` | `#6366F1` | `#818CF8` |
| Mélodie tenue | `#6E8F6A` | `#2A8BA6` | `#8A8DF4` | `#5F67C8` |
| Basse | `#5F7F5C` | `#EF8D3B` | `#C95F0C` | `#FB923C` |
| Basse tenue | `#87A67F` | `#A8642A` | `#E89A5C` | `#B0642C` |
| Libellé hors gamme | `#5F7F5C` | `#7C8193` | `#8B8799` | `#7E7B92` |

- La basse de Brume clair est `#C95F0C` (3,6:1 contre la case) : `#EA7A1E`, la première
  valeur, restait sous le minimum de 3:1 d'un élément graphique.
- En Original, les plus petits textes de l'éditeur sont en 8 et 9 px (police pixel).
- Supprimer prend la couleur `danger` : `#34503A` (Original, faute de rouge), `#E5534B`
  (Couleurs), `#C2264B` (Brume clair), `#FB7185` (Brume sombre).
- **Valeurs codées (P4.22c, « Mes musiques »)** : jetons `danger` et `dangerInk` de `BloxPalette`.
  Original : `#34503A` sur `#EEF5EA` (le danger vaut l'encre : « Supprimer » ne se distingue que
  par son libellé, comme dans la maquette). Couleurs : `#EB6A62` sur `#12141C`, au lieu du
  `#E5534B` de la maquette, qui ne donnait que 4,43:1 sur `panel` ; `#EB6A62` en donne 5,29.
  **Tests :** `PaletteContrastTest › a text stands out from its background by a contrast of 4,5 to 1 at least`
  (couples `DANGER_INK_ON_DANGER` et `DANGER_ON_PANEL`).

## Polices

Polices embarquées (ADR 0016, 0021), telles que publiées par `google/fonts` (SIL OFL 1.1), dans
`core:designsystem/src/main/res/font` ; leurs licences sont dans l'APK
(`src/main/assets/licenses/<famille>/OFL.txt`). Pas de sous-ensemble : ce serait une version
modifiée de la police, à renommer. `google/fonts` ne publie Manrope et Unbounded qu'en police
variable (axe `wght`, `Manrope[wght].ttf`, `Unbounded[wght].ttf`) : les fichiers sont seulement
renommés (`manrope_variable.ttf`, `unbounded_variable.ttf`, `bagel_fat_one_regular.ttf`). Chakra
Petch est retirée avec Couleurs v1 (P5.58).

| Rôle | Original | Couleurs |
|---|---|---|
| Titres | Press Start 2P Regular | Unbounded Bold |
| Libellés | Press Start 2P Regular | Manrope Medium |
| Valeurs (score, niveau, lignes, record) | Press Start 2P Regular | Manrope Bold, chiffres tabulaires (`tnum`) |
| Styles du design system (`screen-title` à `logo`) | Press Start 2P Regular, sans taille | ceux de `tokens.json` |

En Original, tout reste en Regular, seule graisse de la police pixel (pas de gras synthétisé).
`BloxTheme.typography` porte la police et la graisse de chaque rôle ; la typographie Material
en dérive (titres pour display, headline et title, libellés pour le reste). Elle porte aussi les
quinze styles de texte du design system (`screenTitle`, `finalScore`, `record`, `statValue`,
`gameTitle`, `statLabel`, `buttonLabel`, `choice`, `section`, `small`, `playLabel`,
`fourLinesPop`, `pointsPop`, `buttonLabelStrong`, `logo`) : en Couleurs, police, taille (1 px vaut
1 sp), interligne en em et graisse de `tokens.json` ; Unbounded pour `screen-title`, Jouer et les
autocollants, Bagel Fat One pour le seul logo, Manrope pour le reste. En Original, ces styles n'ont
que la police pixel : les écrans d'Original gardent leurs tailles, jusqu'à ce qu'ils prennent les
mises en page v2. Chaque graisse d'une police variable pose l'axe `wght` à sa valeur
(`variableFont`) : sans ce réglage, Android dessinerait l'instance par défaut du fichier
(ExtraLight pour Manrope, Regular pour Unbounded), quelle que soit la graisse demandée. Les tailles de
tablette que cite l'usage de certains styles (28 px pour `screen-title`) ne sont pas des jetons :
les mises en page de la tablette les prendront.

Chiffres à chasse fixe : Manrope a la fonction `tnum` (GSUB, substitution simple des dix chiffres
vers dix glyphes de 1240 unités sur 2000, soit 0,62 em, aux mêmes variations dans HVAR, donc à
chaque graisse). Les valeurs, le score final et le record la prennent (`"tnum"` dans
`fontFeatureSettings`) et coupent le crénage, pour qu'une valeur ne bouge pas en changeant ;
`DigitWidths` et `valueText()`, qui complétaient les chiffres proportionnels de Chakra Petch, sont
retirés. La police pixel est à chasse fixe. Un texte qui rétrécit pour tenir ne descend pas sous
8 sp (`MinimumFontSize`).

Police du système agrandie (P2.18, écran de jeu) : à ×2, 8 sp font 16 dp par lettre et la police
pixel ne tenait plus (« SUIVANTE » rogné, titre « CLASSI… »). Le plancher d'un texte qui rétrécit
(`shrinkFloor`, `shrinkToFit`, dans `core:designsystem` depuis P5.43) est donc le plus petit de 8 sp et de la taille
du texte en police normale, jamais au-dessus de sa taille maximale : un joueur qui agrandit la
police ne voit jamais un texte plus petit qu'en police normale (à ×2, un libellé de 11 sp peut
revenir à 11 dp), et la règle des 8 sp reste entière à ×1. Le calcul passe par `toSp()`, donc par
la mise à l'échelle non linéaire d'Android 14 et plus, que les tests JVM n'exercent pas (ils sont
linéaires). Il sert aux libellés et valeurs des encarts et au titre de la barre du haut (deux
lignes au plus). L'aperçu de la pièce suivante n'était pas écrasé : la barre des captures est la
pièce I. Captures : `GameScreenLargeFont`, `TopBarLargeFont`, `SidePanelLargeFont`,
`GameScreenLandscapeLargeFont`.

Un texte peut aussi ne jamais dépasser sa taille en police normale (`notEnlarged`,
`shrinkToFitNotEnlarged`, P2.26), son plancher restant celui de `shrinkFloor` : c'est le record
sous le titre de la barre du haut, dans une fenêtre très étroite, pour rester à la hauteur du
bouton pause. **Tests :** `ShrinkFloorTest › with a font enlarged twice a text not enlarged keeps its 18 dp with the normal font`,
`ShrinkFloorTest › with a font made smaller a text not enlarged keeps its size`,
`ShrinkFloorTest › the floor is never above the size of the text not enlarged`.

Accueil (P5.43) : les choix et `SingleLineText` (dont le lien « MES MUSIQUES », et les textes de
l'éditeur et de la liste, inchangés à l'œil) suivent la même règle. Un texte qui peut tenir sur
plusieurs lignes (`shrinkToFewestLines`) préfère une ligne, même plus petite, et ne passe à la
ligne que si une ligne demanderait une taille sous le plancher ; il ne coupe jamais un mot. C'est
« COMPOSER UNE MUSIQUE » : une ligne à ×1, deux lignes (« COMPOSER UNE » / « MUSIQUE ») à ×2 en
Original, une ligne un peu réduite à ×2 en Couleurs. À ×1, son libellé en Original grandit d'un pas
de 0,25 sp. Restent à ×2 : l'icône du bouton, de 18 dp, paraît petite ; le nom de 40 caractères
d'une musique en jeu reste coupé par « … » (TalkBack le lit en entier). Captures : `HomeLargeFont`,
`HomeColorfulLargeFont`, `HomeGameMusicLongNameLargeFont`.

Mots entiers (P5.47, P5.48) : un texte ne passe à la ligne qu'après une espace (`cutsAWord`) ;
après une barre oblique ou un trait d'union, c'est une coupure, refusée exprès (« 120 temps/min »,
« peut-être » réduisent plutôt). Limite : une langue sans espace (chinois, japonais, thaï)
réduirait toujours au plancher. `shrinkToFewestLines` vérifie désormais aussi les mots entiers
quand il passe sur plusieurs lignes. `shrinkToWholeWords` permet plusieurs lignes et ne réduit que
si le texte déborde ou coupe un mot ; il ne cherche pas le moins de lignes (message du dé, trois
lignes au plus ; libellés des réponses des fenêtres). `shrinkingLineHeight` garde l'interligne d'un
texte qui réduit en proportion de sa taille. Les réponses d'une fenêtre (`WindowAnswers`) sont
toujours empilées depuis P5.98 (section « Fenêtres v2 »), chacune sur toute la largeur, en grande
police plutôt que réduite ; un mot trop long pour la largeur entière réduit. `ButtonRow.kt` (côte à
côte si chaque mot tient dans sa moitié, en pixels entiers) reste pour la barre d'outils de l'éditeur
(P5.49) et les tuiles de l'accueil. Le texte d'une fenêtre (`WindowText`) garde ses mots
entiers (`shrinkToWholeWords`) : quand un mot et son point final ne tiennent pas sur la ligne, le
moteur coupait dans le mot et laissait le point seul ; la phrase réduit un peu à la place (ce n'est
pas la césure, désactivée pour le corps du texte). **Tests :**
`WindowTextTest › in a font twice as large, in Original, on 360 dp, a final point never stands alone on its line, words whole`,
`WindowTextTest › with the normal font, in Original, on 360 dp, a sentence keeps its size, its words whole`.
Capture : `LeaveWindowOriginalLargeFont`.

**Tests :** `BloxTypographyTest › in the Original theme titles, labels and values are in the pixel font, without a synthesized bold`,
`BloxTypographyTest › in the Colorful theme titles are in Unbounded bold, labels in Manrope medium and values in Manrope bold`,
`BloxTypographyTest › in the Original theme each style of the design system is in the pixel font, without a synthesized bold`,
`BloxTypographyTest › the logo style is in Bagel Fat One and is used for the logo only`,
`BloxTypographyTest › each text takes its weight on the weight axis of its font, not the default instance of a variable font`,
`TypographyTokensTest › each text style of tokens json has its family, size, line height and weight in the Colorful typography`,
`TypographyTokensTest › every text style of tokens json is in the Colorful typography, and nothing more`,
`BloxTypographyTest › the Material styles take the font of the titles for headings and of the labels for the rest`,
`BloxTypographyTest › a text that shrinks to fit its place stays at least 8 sp, to stay readable`,
`ShrinkFloorTest › the floor is never above the largest size of the text`,
`ShrinkFloorTest › the floor is never smaller on screen than 8 sp or than the text with the normal font, whichever is smaller`,
`ShrinkFloorTest › with a font enlarged twice a label of 11 sp may shrink back to its 11 dp with the normal font`,
`ShrinkFloorTest › with a font made smaller a text smaller than 8 sp never gets a floor above its largest size`,
`FewestLinesSizeTest › a text that fits on one line keeps one line even at a smaller size than on several`,
`FewestLinesSizeTest › a text that does not fit on one line even at the smallest size wraps on several lines`,
`FewestLinesSizeTest › a text that fits nowhere takes the smallest size`,
`FewestLinesSizeTest › the lines are never tried when one line is enough`,
`LargestSizeThatFitsTest › a text that fits at its largest size keeps it`,
`LargestSizeThatFitsTest › a text too large shrinks to the largest step from the smallest size that fits`,
`LargestSizeThatFitsTest › a text that does not fit even at the smallest size has no size that fits`,
`LargestSizeThatFitsTest › a largest size off the steps is kept when the text fits at it`,
`LargestSizeThatFitsTest › below a largest size off the steps a text takes the largest step from the smallest size that fits`,
`LargestSizeThatFitsTest › with the smallest size equal to the largest a text that fits takes it`,
`LargestSizeThatFitsTest › with the smallest size equal to the largest a text that does not fit has no size that fits`,
`LargestSizeThatFitsTest › a text that fits only at the smallest size takes exactly the smallest size`,
`LargestSizeThatFitsTest › the sizes tried grow with the logarithm of the number of steps`,
`HomeScreenTest › in a font twice as large, on 360 dp, the labels of the tiles are shown whole, their words whole`,
`LargestSizeThatFitsTest › when fitting is not monotonic, as a text that wraps may be, the size given was tried and fits`,
`WordCutTest › a text on one line cuts no word`,
`WordCutTest › a text that goes to the line between its words cuts no word`,
`WordCutTest › a text that goes to the line inside a word cuts it`,
`WordCutTest › a word cut on any of the lines is enough`,
`WordCutTest › a text that goes to the line after a slash or a hyphen cuts its word, a line goes on only after a space`,
`WordCutTest › a number cut between two digits is cut`,
`ShrinkingLineHeightTest › a text that shrinks keeps the line height of its style in proportion to its size`,
`ShrinkingLineHeightTest › a style without a line height in sp keeps its own`,
`WindowAnswersTest › the answers of a window stand one above the other, the safe one first, as TalkBack reads them`,
`WindowAnswersTest › in a font twice as large, on 360 dp, the answers stand one above the other, each word whole`,
`WindowAnswersTest › in a font twice as large, a word too long for the whole width of an answer shrinks rather than be cut`,
`EmbeddedFontsTest › each embedded font has its OFL licence in the assets of the module, which go into the APK`,
`EmbeddedFontsTest › the embedded families are Press Start 2P, Manrope, Unbounded and Bagel Fat One, without Chakra Petch`,
`ValueDigitsTest › in each theme the ten digits of a value take the same width, so that a value does not move`,
`ValueDigitsTest › in the Colorful theme the values take the tabular digits of Manrope, its tnum feature`,
`OpenTypeFontTest › a character that the font does not draw takes the missing glyph 0`,
`OpenTypeFontTest › the advance of a glyph varies by region of the axes, a region without delta not counted`,
`ValueDigitsTest › in each theme the values are laid out without kerning, which would bring two digits closer`.
Vues : V1, V2, V3, V4.
