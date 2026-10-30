# Pictogrammes, logo et mascotte de Couleurs v2

Relevés dans la maquette (P5.87, 2026-10-25) pour `core:designsystem` (`BloxIcon`). Chaque
pictogramme existe en deux versions, comme les 19 de P5.54 :

- **traits** (Couleurs, Brume) : un tracé SVG dans une grille de 24 × 24, bouts et angles
  arrondis, sans remplissage sauf mention, en `currentColor` ; épaisseur 2,25 sauf mention dans
  le tableau ;
- **pixels** (Original) : un bitmap de 8 × 8, `#` plein, `.` vide, dessiné comme les autres en
  carrés d'un nombre entier de pixels de l'écran (P5.54a).

Les tracés en traits sont recopiés des planches V26 à V34. Les bitmaps 8 × 8 sont proposés ici
et restent **à confirmer sur la maquette en thème Original** (P5.85).

## Pictogrammes nouveaux

| Nom (`BloxIcon`) | Usage | Tracé en traits (24 × 24) | Trait |
|---|---|---|---|
| `CARTRIDGE` | « Changer de jeu » (accueil) | `M6 3h12v13l-2 5H8l-2-5zM9 7h6v6H9zM10 18h4` | 2,25 |
| `MINUS` | Baisser le niveau, ralentir le tempo | `M5 12h14` | 2,5 |
| `STAR` | Puce Record | `M12 3.2l2.6 5.6 6.1.7-4.5 4.1 1.2 6.1L12 16.6l-5.4 3.1 1.2-6.1-4.5-4.1 6.1-.7z` | 2,25 |
| `NOTE_OFF` | Musique « Aucune » | `M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM20 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM3 3l18 18` | 2,25 |
| `COMPOSE` | Tuile « Composer une musique » (accueil) | `M9 18V5l11-2v6M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM14 21h2.5l5-5-2.5-2.5-5 5z` | 2,25 |
| `CHECK` | Choix retenu (Mode, Thème, feuille Musique, Utiliser, styles) | `M4 12.5l5 5L20 6.5` (remplace la coche à bouts carrés de `ChoiceCheck`) | 3 |
| `BACK` | Retour des barres (Paramètres, musique) | `M15 5l-7 7 7 7` | 2,25 |
| `INSERT` | Choix du jeu, console en attente | `M12 3v16M5 13l7 7 7-7` | 2,5 |
| `ALERT` | Message d'erreur (Enregistrer) | cercle de rayon 9 en (12, 12) et `M12 7v6M12 16.5v.5` | 2,5 |
| `EQUALIZER` | Aperçu en cours | trois barres de 3 × 14 en `accent`, coins de 2, écart de 2, animées en hauteur (35 % à 100 %, 0,5 s) ; pas de tracé | — |
| `REDUCED_EFFECTS` | Paramètres, Effets réduits | `M12 3v3M12 18v3M3 12h3M18 12h3M6.3 6.3l2 2M15.7 15.7l2 2M6.3 17.7l2-2M15.7 8.3l2-2M3 3l18 18` | 2,25 |
| `KEYS_SKETCH` | Paramètres, Gaucher | 40 × 24 : trois carrés de 8 (coins de 2) en `label` à x = 0, 9, 18, y = 10 ; disque B de rayon 5 en `label` à (31, 17) ; disque A de rayon 5 à (35, 7), en `accent-pressed` le jour (`accent` serait à 2,1:1 sur `panel`) et en `accent` la nuit. En Gaucher, miroir : carrés à x = 14, 23, 32, B à (9, 17), A à (5, 7) | — |
| `VIBRATE` | Paramètres, Vibrations | `M8 3h8a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zM3 8v8M21 8v8` | 2,25 |
| `TOUCH` | Paramètres, Vibration des touches | `M10 11V5.5a1.5 1.5 0 0 1 3 0V11M13 10.5a1.5 1.5 0 0 1 3 0V12M16 11.5a1.5 1.5 0 0 1 3 0V15a6 6 0 0 1-6 6h-1.5a5 5 0 0 1-4-2l-3-4a1.4 1.4 0 0 1 2.1-1.9L10 15` | 2,25 |
| `VOLUME` | Paramètres, volumes | `M4 9h4l5-4v14l-5-4H4zM16.5 9a4 4 0 0 1 0 6M19 6.5a7.5 7.5 0 0 1 0 11` | 2,25 |
| `TROPHY` | Paramètres, Record du Classique | `M8 4h8v5a4 4 0 0 1-8 0zM8 6H5a3 3 0 0 0 3 4M16 6h3a3 3 0 0 1-3 4M12 13v4M8.5 20h7M10 17h4` | 2,25 |
| `EJECT` | Choix du jeu, éjecter | plein : `M12 4L20 13H4z` et rectangle x 4, y 16, 16 × 3,5, coins de 1,5 | — |
| `CHEVRON_DOWN` | Rangée Musique (ouvre la feuille) | `M6 9l6 6 6-6` | 2,5 |
| `CLOSE` | Fermer une feuille ou un message | `M6 6l12 12M18 6L6 18` | 2,5 |

Le triangle de Jouer v2 (`M8 5.5 L19 12 L8 18.5 Z`, plein, trait de 2 aux angles arrondis) est un
dessin du bouton, pas un pictogramme : `PLAY` reste celui de P5.54 pour Écouter.

## Bitmaps 8 × 8 (Original, à confirmer en P5.85)

```text
CARTRIDGE    MINUS        STAR         NOTE_OFF     COMPOSE
.######.     ........     ...##...     #.######     ..######
.#....#.     ........     ...##...     .#.#####     ..######
.#....#.     ........     ########     ..#....#     ..#....#
.######.     ########     .######.     ..##...#     ..#..#.#
.######.     ########     ..####..     ..#.#..#     ..#.###.
.######.     ........     .##..##.     ###..#..     #####...
..####..     ........     .#....#.     ###...#.     ####....
..#..#..     ........     ........     .......#     ........

CHECK        BACK         INSERT       ALERT        EQUALIZER
........     ....#...     ...##...     ..####..     ........
.......#     ...#....     ...##...     .#....#.     ......#.
......##     ..#.....     ...##...     #..##..#     ....#.#.
#....##.     .#......     ...##...     #..##..#     .#..#.#.
##..##..     ..#.....     #..##..#     #......#     .#..#.#.
.####...     ...#....     .######.     #..##..#     .#..#.#.
..##....     ....#...     ..####..     .#....#.     .#..#.#.
........     ........     ...##...     ..####..     ........

REDUCED      VIBRATE      TOUCH        VOLUME       TROPHY
#..##...     ..####..     ...#....     ...#....     .######.
.#....#.     ..#..#..     ...#....     ..##..#.     #.####.#
..#.....     #.#..#.#     ...###..     ####.#.#     #.####.#
#..#...#     #.#..#.#     #..####.     ####.#.#     .######.
#...#..#     #.#..#.#     ##.####.     ####.#.#     ..####..
.....#..     ..#..#..     .######.     ..##..#.     ...##...
.#....#.     ..####..     ..####..     ...#....     ..####..
...##..#     ........     ..####..     ........     ........

EJECT        CHEVRON_DOWN CLOSE
...##...     ........     ##....##
..####..     ........     .##..##.
.######.     #......#     ..####..
########     .#....#.     ...##...
........     ..#..#..     ..####..
########     ...##...     .##..##.
########     ........     ##....##
........     ........     ........
```

`EQUALIZER` garde trois barres (colonnes 1, 4 et 6) ; en Original il ne s'anime pas (la police
pixel et les aplats restent fixes), le carré d'Arrêter suffit. `KEYS_SKETCH` n'a pas de bitmap :
en Original, la rangée Gaucher montre le même schéma en carrés pleins (`radius` 0), B en `label`
et A en `accent`.

## Logo BloX v2

Composant `LogoBloX` de la maquette (proposition 7, retenue). Pour une taille `s` (hauteur des
lettres : 56 dp à l'accueil sur téléphone debout, 44 dp couché, 96 dp sur tablette, 42 dp dans la
barre du choix du jeu) et `k = s / 80` :

- « Bl », puis la mascotte **Serrurier** (O, cases de `25 × k`) à la place du « o », puis « X »,
  alignés en bas, écart de `2 × k`, la mascotte décalée de `4 × k` sur les côtés et de `6 × k`
  vers le haut ;
- lettres en **Bagel Fat One** (`logo`), interligne 0,9 ;
- **jour** : « Bl » en dégradé 135° `iris-1` `#C9A7FF` → `iris-2` `#9FF0D0` (60 %) → `iris-3`
  `#FFC6A8`, « X » en `iris-3` → `iris-1` ; trois ombres portées (`drop-shadow`) : un liseré blanc
  décalé de `2 × k` vers le bas sans flou, une ombre `#B48CFF` décalée de `5 × k` vers le bas sans
  flou, puis `rgba(90,80,160,0.3)` décalée de `12 × k` vers le bas et floutée de `14 × k` ;
- **nuit** : « Bl » en `iris-1` `#C6FF3D` → `iris-2` `#3DFFD0` (60 %) → `iris-3` `#A77BFF` ; « X »
  en `#A77BFF` → `#FF6FB5` (exception : ce rose n'est pas un jeton, c'est celui de la planche
  Y2K de nuit ; à garder tel quel dans le logo) ; ombre `#2A2D3A` décalée de `4 × k` vers le bas,
  puis halo `rgba(198,255,61,0.45)` flouté de `18 × k` sans décalage (`rgba(167,123,255,0.5)`
  pour le X) ;
- TalkBack lit « BloX » ; c'est le titre de l'écran.

Original garde son logo de quatre cases (P3.7).

## Mascotte

Composant `Mascotte` de la maquette : un personnage par jeu, fait de la pièce qui le nomme. Seuls
deux servent au lot : **Classique** (T, couronne) sur la cartouche (cases de 38 dp), dans la
console du choix du jeu (18 dp) et dans la pause (16 dp sur téléphone, 22 sur tablette) ;
**Serrurier** (O, clé) dans le logo. Pour une case de `c` dp et un trait `t = max(2, 0,06 × c)` :

- **Corps** : les cases de la pièce (T : (0,0) (1,0) (2,0) (1,1) ; O : 2 × 2), comme une case de
  pièce v2 (dégradé du reflet à la couleur, coins de `0,3 × c` le jour, `0,26 × c` la nuit). La
  maquette colore encore le T en rouge Y2K (`#FF9EAD`) et les accessoires en jaune Y2K ; le code
  prendra les jetons `piece-t` et `piece-o` : c'est un **changement de dessin**, à valider sur la
  maquette en P5.85, pas une recopie.
- **Visage**, centré en (1,5 ; 0,55) cases pour le T, (1 ; 0,9) pour le O :
  - deux yeux blancs de `0,3 × c` écartés de `± 0,26 × c` du centre et remontés de `0,08 × c` ;
    pupilles de la moitié de l'œil en `ink`, décalées de `0,03 × c` vers le bas et de `0,02 × c`
    vers le centre ;
  - bouche en demi-cercle de `0,3 × c` × `0,14 × c`, `0,12 × c` sous le centre, trait `t` ;
  - joues `rgba(255,110,150,0.45)` de `0,18 × c` × `0,11 × c`, à `± 0,46 × c` du centre et
    `0,1 × c` dessous (aucune pour la pièce I ni en sommeil).
- **Endormie** (pause, jeux à venir) : yeux en barres de `0,3 × c` × `t`, remontés de `0,04 × c` ;
  bouche en barre de `0,16 × c` × `t` ; deux « z » en `ink`, de `0,42 × c` à (+0,4 c ; −0,95 c)
  à 75 % d'opacité et de `0,3 × c` à (+0,72 c ; −1,25 c) à 55 % ; corps en `cell-dead` ; sans
  accessoire.
- **Accessoire** (éveillée), dans une boîte carrée `a = 0,95 × c`, posée à
  `x = largeur × c − 0,55 × a` et `y = −0,6 × a`, tournée de 14°, trait de 1,8 sur la grille de
  24, contour en `ink` : couronne `M3 18 L4.5 7.5 L9 11.5 L12 5 L15 11.5 L19.5 7.5 L21 18 Z`
  remplie de `piece-o` ; clé `M3.5 8 a4.5 4.5 0 1 0 9 0 a4.5 4.5 0 1 0 -9 0 Z` remplie de
  `piece-o` avec le trait `M11.2 11.2 L20 20 M15.5 15.5 L18 13 M18 18 L20.5 15.5`.
- **Ombre** : le jour `rgba(90,80,160,0.3)` décalée de `0,12 × c` vers le bas et floutée de
  `0,16 × c` ; la nuit, un halo de la couleur du corps à 60 % flouté de `0,25 × c` (aucun endormie).
- TalkBack lit le nom du jeu, « endormi » en plus quand elle dort ; décorative à côté d'un libellé.
- **Codée** (P5.77, `core:board`, `Mascot`, `MascotLook`) : le visage garde l'encre du jour, même la nuit, sur
  le corps clair de la pièce ; endormie, elle est pâle la nuit (`cell-dead`). Dans la carte de la pause, elle est
  décorative (TalkBack ne la lit pas, « Pause » suit). **Tests :**
  `MascotLookTest › the Classic mascot is a T pointing down and the Locksmith mascot an O`,
  `MascotLookTest › an awake mascot is a bubble of its piece, its face in the ink of the day, its accessory in yellow`,
  `MascotLookTest › an asleep mascot is in cell-dead, flat, its face in ink, without accessory nor shadow`,
  `GameScreenPauseTest › the card shows the Classic mascot asleep above Pause, a decoration TalkBack does not read` ;
  captures `Mascots*`, `Pause*`.
- **Jardin musical** (P10.13) : un L debout sur son pied, pied à droite (l'apparition tournée d'un quart de tour
  anti-horaire : cases (0,0) (0,1) (0,2) (1,2)), visage en (1 ; 2,5), sur le pied ; feuille
  `M4 20 C4 10 10 4 20 4 C20 14 14 20 4 20 Z` remplie de `piece-s`, nervure `M4 20 L13 11`, posée comme les autres
  accessoires (dans le coin libre au-dessus de la case (1,0)) ; corps en `piece-l`. **Tests :**
  `MascotLookTest › the Jardin musical mascot is an L standing on its foot, the foot to the right`,
  `MascotLookTest › the Jardin musical mascot is a bubble of the L, its leaf in the mint of the S`.
