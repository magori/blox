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

**Tests :** `CellStylesTest › in the Original theme a piece has the fill, border and centre of docs themes md`,
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
| `buttonPressed` | `#646B87` |
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

**Tests :** `KeyColorsTest › a held key stands out from the page, by a contrast of 3 to 1 at least`,
`KeyColorsTest › a held key looks other than a key not held`,
`KeyColorsTest › a held key stands out from the same key not held, by a contrast of 1,5 to 1 at least`,
`KeyColorsTest › the glyph of a key stands out from its face, by a contrast of 3 to 1 at least`,
`KeyColorsTest › the letters A and B, large text, stand out from their key by a contrast of 3 to 1 at least`,
`KeyColorsTest › the letters A and B are large text, bold and of 18,66 sp at least`,
`CellStylesTest › in the Original theme a piece has the fill, border and centre of docs themes md, framed by a joint`,
`CellLayoutTest › two flat cells side by side in the Original theme are apart by their two joints, of 1 px each at least`.
Vues : V2, V3.

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
