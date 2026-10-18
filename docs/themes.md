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
est exposé par `BloxTheme.kind` (`core:designsystem`). Le code est dans `feature:game`,
paquet `board` (`CellStyles`, `BoardGeometry`, `DisplayedCells`, `BoardCanvas`).

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
vérifier sur la maquette Claude Design).

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

## Polices

Les deux polices sont sous licence OFL. Deux options, à trancher (voir SUIVI.md) :
fichiers `.ttf` embarqués dans `core:designsystem/src/main/res/font`, ou polices
téléchargeables via `ui-text-google-fonts`. En attendant, le squelette utilise la police
système.
