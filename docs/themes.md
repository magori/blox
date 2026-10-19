# Thèmes

Deux thèmes, issus de la [maquette](maquette.md). Les jetons d'interface sont codés dans
`core:designsystem` (`BloxPalettes`). Les styles de pièces seront codés dans `feature:game`,
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
`BloxTheme.shapes` (coins carrés en Original, arrondis de 10 dp en Couleurs ; `button` sert aux
commandes, aux boutons des écrans — Reprendre, Menu, Rejouer — et au bouton pause). Le code est dans
`feature:game` : paquet `board` (`CellStyles`, `CellPainter`, `BoardGeometry`,
`DisplayedCells`, `BoardCanvas`) et paquet `sidepanel` (colonne latérale, aperçu de la pièce
suivante dessiné avec le même `CellPainter`).

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
`GameScreenTest › the pause screen takes the place of the game while it is paused`,
`GameScreenTest › the game over screen takes the place of the game once it is over`.
Vues : V2, V3.

**Thème de l'application** (P3.5) : le thème choisi à l'accueil s'applique à tous les écrans
(accueil, jeu, pause, fin de partie) : `MainActivity` enveloppe toute la navigation dans
`BloxTheme(kind)`, et `kind` vient d'`AppTheme` (`app`), un `StateFlow` lu depuis les
préférences, connu tout de suite après la première lecture (pas de flash à la recréation de
l'activité). Avant cette lecture, rien n'est composé et la première image est retenue, pour que
le fond de fenêtre ne s'affiche pas dans le mauvais thème. Le fond de fenêtre
(`colors.xml`, `blox_window_background`) est celui du thème Original ; un test le compare à
`BloxPalettes.Original.page`.

**Logo de l'accueil** (P3.7) : quatre cellules en carré 2 × 2, couleurs du jeton
`BloxPalette.logoCells` (`LogoColors`, exactement quatre couleurs) : en Original, les
remplissages de I, O, J et T ; en Couleurs, ceux de I, O, Z et S. Ces couleurs répètent celles
des pièces (`feature:game`) : un test vérifie qu'elles restent égales.

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
`CellStylesTest › in the Colorful theme a piece has the fill and bevels of docs themes md`,
`CellStylesTest › in the Original theme an empty cell is the board background without border`,
`CellStylesTest › in the Colorful theme an empty cell has a 1 px border`,
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

Fond sombre, une couleur par pièce, effet biseauté (bordure claire en haut et à gauche,
foncée en bas et à droite).

| Jeton | Valeur |
|---|---|
| `page` | `#12141C` |
| `boardBackground` | `#0B0C12` |
| `panel` | `#1C1F2B` |
| `panelBorder`, `frame`, `button` | `#2A2E3E` |
| `ink` | `#F1F1F4` |
| `label` | `#9AA0B4` |
| `accent` | `#EF8D3B` |
| `accentInk` | `#12141C` |
| `buttonPressed`, `buttonOutline` | `#646B87` |
| `accentPressed` | `#C0651C` |

Police : *Chakra Petch* dans la maquette (licence OFL), coins arrondis (10 px).

| Pièce | Fond | Biseau clair | Biseau foncé |
|---|---|---|---|
| I | `#3FC5E8` | `#9BE6F6` | `#1D8FAE` |
| O | `#F2C641` | `#FAE08F` | `#B38A17` |
| T | `#A467E0` | `#CFA6F1` | `#6E36A8` |
| S | `#5BC46A` | `#A3E3AB` | `#2F8A3C` |
| Z | `#E5534B` | `#F29A94` | `#A32A24` |
| J | `#3F7EE8` | `#93B8F4` | `#1F4FA6` |
| L | `#EF8D3B` | `#F6C18F` | `#AD5A14` |

Cellule vide : `#0B0C12` avec une bordure de 1 px `#191C27` (style de cellule, dans
`feature:game` ; à monter dans la palette s'il sert ailleurs). Bordure biseautée de 3 px.
**Commandes** (P2.17) : une touche tenue prend la teinte « pressée » de sa couleur (`buttonPressed`,
`accentPressed`) et garde son encre ; elle se distingue de la page (au moins 3:1) et de la même
touche au repos (au moins 1,5:1). Les lettres A et B sont en gras à 20 sp (grand texte, seuil
3:1) ; les flèches sont des pictogrammes (3:1).

**Cadre des touches** (P2.22) : chaque touche au repos est entourée d'un cadre de 2 dp
(`BloxBorders.frame`) en `buttonOutline`, qui la détache de la page d'au moins 3:1 (WCAG 1.4.11,
composant non textuel). Dans le thème Couleurs, le fond `button` ne se détachait de la page qu'à
1,36:1 ; le cadre `#646B87` donne 3,50:1. Une touche tenue est remplie de sa teinte pressée
jusqu'au bord, cadre compris. La touche A n'a pas de cadre propre : l'accent se détache déjà de la
page. Dans le thème Original, le cadre a la couleur du bouton (3,31:1 contre la page) : rien ne
change à l'œil. Le cadre partage sa teinte avec `buttonPressed` dans Couleurs ; le test le garde à
3:1 si l'une des deux change. Le bouton pause de la barre du haut garde ce défaut (P2.23).

**Tests :** `KeyColorsTest › a held key stands out from the page, by a contrast of 3 to 1 at least`,
`KeyColorsTest › a held key looks other than a key not held`,
`KeyColorsTest › a held key stands out from the same key not held, by a contrast of 1,5 to 1 at least`,
`KeyColorsTest › the glyph of a key stands out from its face, by a contrast of 3 to 1 at least`,
`KeyColorsTest › the letters A and B, large text, stand out from their key by a contrast of 3 to 1 at least`,
`KeyColorsTest › the letters A and B are large text, bold and of 18,66 sp at least`,
`KeyColorsTest › a key at rest stands out from the page by its outline, by a contrast of 3 to 1 at least`,
`KeyColorsTest › a held key is filled with its pressed shade up to its edge`,
`CellStylesTest › in the Original theme a piece has the fill, border and centre of docs themes md, framed by a joint`,
`CellLayoutTest › two flat cells side by side in the Original theme are apart by their two joints, of 1 px each at least`.
Vues : V2, V3.

## Choix de l'accueil

Un choix sélectionné (mode, thème, niveau de départ, musique) est rempli en `accent`, son
libellé en `accentInk`. Il ne se distingue pas que par la teinte (WCAG 1.4.1) : les deux états
diffèrent d'au moins 3:1 en luminance, et le choix non sélectionné est cadré quand le
sélectionné est plein. Le soulignement ajouté en P5.26 est retiré à la demande du développeur
(P5.36). Un
choix non sélectionné est dessiné comme un encart du jeu : `panel`, cadre de 2 dp en
`panelBorder` (`BloxBorders.frame`, partagé avec les encarts et `SecondaryButton`), libellé en `ink`, forme
`button` (carrée en Original) ; le choix sélectionné porte un cadre de sa propre couleur, pour
que tous gardent la même taille. Les deux états se distinguent d'au moins 3:1 (WCAG 1.4.11), le
libellé de son fond d'au moins 4,5:1 (P5.26, constat C3 de la revue qualité). Aucun jeton de palette n'a changé. Le code est `choiceColors` (`feature:home`), sur le modèle de
`keyColors`.

| | Original | Couleurs |
|---|---|---|
| Sélectionné / non sélectionné (`accent` / `panel`) | 5,7:1 (avant 1,99:1) | 6,7:1 (avant 5,48:1) |
| Libellé sélectionné (`accentInk` sur `accent`) | 8,0:1 | 7,5:1 |
| Libellé non sélectionné (`ink` sur `panel`) | 5,7:1 (avant 4,0:1) | 14,6:1 |

**Tests :** `ChoiceColorsTest › in every theme a selected choice stands out from an unselected one by a contrast of 3 to 1 at least`,
`ChoiceColorsTest › the label of a choice stands out from its background by a contrast of 4,5 to 1 at least`,
`ChoiceColorsTest › an unselected choice is framed as a panel, a selected one in its own background`,
`ContrastTest › black on white has the greatest contrast, 21 to 1, whatever the order`,
`HomeScreenTest › TalkBack reads each selected choice as selected, and the others as not selected`.
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
« Jouer » passe ainsi de 40 à 48 dp de haut. Exception en attente de décision : la largeur des
niveaux de départ (P5.29) ; leur hauteur fait bien 48 dp.

**Tests :** `PaletteContrastTest › a text stands out from its background by a contrast of 4,5 to 1 at least`,
`HomeScreenTest › every button and choice is 48 dp wide and high at least, but the start levels`,
`HomeScreenTest › every start level is 48 dp high at least`,
`GameScreenTest › every button of the screen is 48 dp wide and high at least`. Vues : V1 à V4.
## Éditeur de musique (maquette)

Couleurs des pistes de l'éditeur de musique (P4.19), retenues dans la maquette (V7 à V18) et le
prototype de l'éditeur ; à confirmer quand l'éditeur sera codé. Le thème Brume n'existe pour
l'instant que dans la maquette (V5, V6, V13 à V18).

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

## Polices

Polices embarquées (ADR 0016), telles que publiées par `google/fonts` (SIL OFL 1.1), dans
`core:designsystem/src/main/res/font` ; leurs licences sont dans l'APK
(`src/main/assets/licenses/<famille>/OFL.txt`). Pas de sous-ensemble : ce serait une version
modifiée de la police, à renommer.

| Rôle | Original | Couleurs |
|---|---|---|
| Titres | Press Start 2P Regular | Chakra Petch Bold |
| Libellés | Press Start 2P Regular | Chakra Petch Regular |
| Valeurs (score, niveau, lignes, record) | Press Start 2P Regular | Chakra Petch Bold, chiffres complétés |

En Original, tout reste en Regular, seule graisse de la police pixel (pas de gras synthétisé).
`BloxTheme.typography` porte la police et la graisse de chaque rôle ; la typographie Material
en dérive (titres pour display, headline et title, libellés pour le reste). Les chiffres de
*Chakra Petch* sont proportionnels, sans `tnum` : `valueText()` complète chaque chiffre plus
étroit par un espacement en em jusqu'à la largeur du plus large (`DigitWidths`) et coupe le
crénage, pour qu'une valeur ne bouge pas en changeant. Un texte qui rétrécit pour tenir ne
descend pas sous 8 sp (`MinimumFontSize`). Avec la police du système agrandie, la police pixel
ne tient plus partout à 8 sp (P2.18).

**Tests :** `BloxTypographyTest › in the Original theme titles, labels and values are in the pixel font, without a synthesized bold`,
`BloxTypographyTest › in the Colorful theme titles and values are in Chakra Petch bold, labels in Chakra Petch regular`,
`BloxTypographyTest › the Material styles take the font of the titles for headings and of the labels for the rest`,
`BloxTypographyTest › a text that shrinks to fit its place stays at least 8 sp, to stay readable`,
`EmbeddedFontsTest › each embedded font has its OFL licence in the assets of the module, which go into the APK`,
`ValueDigitsTest › in each theme the ten digits of a value take the same width, so that a value does not move`,
`ValueDigitsTest › in each theme the values are laid out without kerning, which would bring two digits closer`.
Vues : V1, V2, V3, V4.
