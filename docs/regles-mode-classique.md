# Règles du mode Classique

Le mode Classique reproduit les **règles de la version NES (1989)**. C'est la référence
choisie dans l'[ADR 0004](adr/0004-regles-nes-comme-reference.md) : ses règles sont
simples, bien documentées par la communauté, et définies en images (ticks), donc
directement testables.

> Statut des valeurs : les règles marquées **[à vérifier]** viennent de mémoire et doivent
> être confrontées à une source de référence (par exemple le wiki communautaire *TetrisWiki*,
> pages « Tetris (NES) ») avant d'écrire le test correspondant. Les autres sont des valeurs
> largement citées et stables.

> Traçabilité : quand une règle est implémentée, ajouter sous elle une ligne
> **Tests :** `ClasseDeTest › nom du test` (un ou plusieurs) et, si elle se voit à l'écran,
> **Vues :** `V2`, `V3`… (numéros de [maquette.md](maquette.md)). Une règle sans ligne
> « Tests » n'est pas encore implémentée.

## Ce que le mode Classique n'a pas

Pas de réserve (*hold*), pas de pièce fantôme, pas de chute instantanée (*hard drop*),
pas de rotation avec décalage contre les murs (*wall kicks*), pas de délai de verrouillage
réinitialisable, pas de T-spin. C'est ce qui le distingue du mode Moderne.

## Plateau

- Grille de **10 colonnes × 20 lignes**, toutes visibles. Origine en haut à gauche : la
  colonne va de 0 à 9 de gauche à droite, la ligne de 0 à 19 de haut en bas. Une cellule est
  vide ou occupée par un type de pièce ; lire ou remplir une cellule hors du plateau est refusé.

  **Tests :** `BoardTest › an empty board has 10 columns and 20 rows`,
  `BoardTest › every cell of an empty board is empty`,
  `BoardTest › a position outside columns 0 to 9 or rows 0 to 19 is outside the board`,
  `BoardTest › the four corners are inside the board`,
  `BoardTest › reading or filling a cell outside the board is rejected`,
  `BoardTest › filling a cell returns a new board and leaves the original unchanged`.
- **Lignes cachées** : deux lignes, −2 et −1, prolongent le plateau vers le haut. Elles sont
  invisibles et toujours vides ; la pièce en jeu peut s'y trouver (par exemple un I tourné à
  la verticale juste après son apparition). Une pièce tient si chacune de ses cellules est dans
  les colonnes 0 à 9, les lignes −2 à 19, et sur une case vide. Au verrouillage, les cellules
  restées dans les lignes cachées sont perdues et la partie continue. Source : ROM NES, d'après
  meatfighter.com, « Applying AI to Nintendo Tetris » (validité de −2 à 19 ; seules les
  lignes 0 et suivantes sont stockées au verrouillage).

  **Tests :** `PieceTest › the cells of a piece are its pivot plus the offsets of its orientation`,
  `BoardCollisionTest › a piece on an empty board touches nothing`,
  `BoardCollisionTest › a cell in column -1 or 10 is a collision, even in a hidden row`,
  `BoardCollisionTest › a cell on row 20, below the floor, is a collision`,
  `BoardCollisionTest › a piece touching an edge without crossing it fits`,
  `BoardCollisionTest › a cell on an occupied square is a collision, the same piece just above fits`,
  `BoardCollisionTest › the hidden rows -1 and -2 are free, row -3 is a collision`,
  `BoardLockTest › cells in the hidden rows are lost, the visible ones join the board`.
- Les pièces apparaissent centrées en haut du plateau.
- **Fin de partie** : la nouvelle pièce ne peut pas apparaître parce que sa position de départ
  est occupée.

## Pièces

Sept pièces de quatre cellules : `I`, `O`, `T`, `S`, `Z`, `J`, `L`.

**Tests :** `BoardTest › parse reads a dot as an empty cell and a piece letter as a cell filled
by that piece` (les sept types ; formes à venir en P1.2).

- **Orientation de départ** : pièces « à plat », aucune cellule au-dessus du pivot. T, J et L
  apparaissent pointe vers le bas (`TTT/.T.`, `JJJ/..J`, `LLL/L..`), S, Z et I à l'horizontale
  (`.SS/SS.`, `ZZ./.ZZ`, `IIII`), O en carré. Source : table des orientations de la ROM NES
  ($8A9C), d'après meatfighter.com, « Applying AI to Nintendo Tetris ».

  **Tests :** `ClassicRotationSystemTest › each piece spawns in its NES orientation around the pivot`,
  `ClassicRotationSystemTest › no piece spawns with a cell above its pivot`.
- La pièce suivante est affichée dans un aperçu (une seule).

## Tirage des pièces

Générateur de la NES :

1. Tirer un nombre de 0 à 7. Les nombres 0 à 6 désignent, dans l'ordre de la ROM, T, J, Z,
   O, S, L, I ; 7 signifie « aucune pièce ».
2. Si on obtient 7 ou la même pièce que la précédente, tirer à nouveau **une seule fois** un
   nombre de 0 à 6, et garder ce résultat quel qu'il soit. Pour la première pièce de la
   partie, seul le 7 provoque ce second tirage.

La « pièce précédente » est la dernière pièce tirée, celle de l'aperçu, et non celle qui
vient de se verrouiller.

Conséquence : les répétitions sont rares mais possibles, et il n'y a aucune garantie de
voir chaque pièce régulièrement (contrairement au « sac de 7 » du mode Moderne).

Source : TetrisWiki, *Tetris (NES, Nintendo)* ; ordre des pièces d'après la ROM
(meatfighter.com, « Applying AI to Nintendo Tetris »). **Écart assumé** : dans la ROM, le
second tirage ajoute l'identifiant d'orientation de la pièce précédente, d'où des
probabilités de 13,8 % à 14,7 % ; BloX tire uniformément (14,3 % pour chaque pièce).

Le générateur se crée avec une graine (`ClassicRandomizer(seed)`) ; les tests lui injectent
une source d'aléatoire scriptée. C'est le seul état modifiable du domaine
([ADR 0014](adr/0014-tirage-seul-etat-modifiable.md)).

**Tests :** `ClassicRandomizerTest › a valid first draw different from the previous piece is kept`,
`ClassicRandomizerTest › a first draw of 7 is followed by a second draw that is kept`,
`ClassicRandomizerTest › a first draw equal to the previous piece is followed by a second draw that is kept even if it repeats`,
`ClassicRandomizerTest › the first draw maps to the pieces in the ROM order T J Z O S L I`,
`ClassicRandomizerTest › for the first piece of the game a first draw of 7 is followed by a second draw`,
`ClassicRandomizerTest › the first draw is made among 8 values and the second among 7`,
`ClassicRandomizerTest › two randomizers created with the same seed draw the same sequence`,
`ClassicRandomizerTest › two randomizers created with different seeds draw different sequences`.

## Rotation

Système de rotation de la NES :

- Bouton **A** : rotation horaire. Bouton **B** : rotation anti-horaire.
- **Aucun décalage** : si la position après rotation est occupée ou hors du plateau, la
  rotation est simplement refusée.
- `O` ne tourne pas. `I`, `S` et `Z` n'ont que **deux états** (ils basculent entre les deux).
  `T`, `J`, `L` ont quatre états.
- Il n'y a pas de centre à calculer : chaque orientation est une liste fixe de 4 décalages
  autour d'un pivot, qui fait partie de la pièce et ne bouge pas pendant la rotation. Les 19
  orientations (O 1 ; I, S, Z 2 ; T, J, L 4), dans l'ordre horaire, viennent de la table de la
  ROM NES ($8A9C, meatfighter.com, même source).

  **Tests :** `ClassicRotationSystemTest › every orientation of every piece has 4 distinct cells`,
  `ClassicRotationSystemTest › O has one orientation, I S and Z have two, T J and L have four`,
  `ClassicRotationSystemTest › T turns clockwise through up, right, down and left` (et un test
  par pièce : J, Z, O, S, L, I), `ClassicRotationSystemTest › every orientation has its pivot as one of its cells`,
  `OrientationTest › an orientation needs exactly four distinct cells`.
- **Tourner** : A passe à l'orientation suivante dans l'ordre horaire, B à la précédente, en
  boucle ; le pivot ne bouge pas. Le système de rotation propose des candidats dans l'ordre
  d'essai et le moteur garde le premier qui tient ; le mode Classique n'en propose qu'un. Une
  orientation qui n'appartient pas au type de la pièce est refusée. Tourner le O ne change rien :
  faut-il alors émettre l'événement de rotation (son) ? **[à vérifier]**, à trancher en P1.12
  (par défaut : seulement si la pièce change).

  **Tests :** `ClassicRotationSystemTest › a clockwise rotation gives the next orientation, a counterclockwise one the previous`,
  `ClassicRotationSystemTest › four clockwise turns bring T J and L back, two are enough for I S and Z, O never changes`,
  `ClassicRotationSystemTest › a clockwise turn followed by a counterclockwise turn gives back the starting orientation`,
  `ClassicRotationSystemTest › the pivot does not move during a rotation`,
  `ClassicRotationSystemTest › an orientation that does not belong to the piece type is refused`,
  `RotationOnBoardTest › a rotation against a wall is refused, without any kick`,
  `RotationOnBoardTest › a rotation onto an occupied cell is refused`,
  `RotationOnBoardTest › a rotation into the hidden rows is accepted, the pivot stays put`.

## Gravité

La pièce descend d'une ligne toutes les *N* images, selon le niveau :

| Niveau | Images par ligne |
|---|---|
| 0 | 48 |
| 1 | 43 |
| 2 | 38 |
| 3 | 33 |
| 4 | 28 |
| 5 | 23 |
| 6 | 18 |
| 7 | 13 |
| 8 | 8 |
| 9 | 6 |
| 10 – 12 | 5 |
| 13 – 15 | 4 |
| 16 – 18 | 3 |
| 19 – 28 | 2 |
| 29 et plus | 1 |

1 image = 1 tick = 1/60 s (la NES tourne en réalité à environ 60,1 images/s, l'écart est
négligeable). Le niveau ne peut pas être négatif.

**Tests :** `ClassicGravityTableTest › at level 0 the piece falls one row every 48 ticks`,
`ClassicGravityTableTest › from level 1 to 9 each level has its own speed`,
`ClassicGravityTableTest › from level 10 to 28 levels share the same speed by tier`,
`ClassicGravityTableTest › from level 29 onwards the piece falls one row every tick`,
`LevelTest › a level cannot be negative`, `LevelTest › level 0 is the lowest valid level`.

## Verrouillage

Pas de délai de verrouillage : quand la gravité devrait faire descendre la pièce et que la
case en dessous est occupée (ou que c'est le fond), la pièce **se verrouille immédiatement**.

Verrouiller remplit les cellules de la pièce avec son type, sur un nouveau plateau ; ses
cellules restées dans les lignes cachées sont perdues. Une pièce qui ne tient pas (hors des
murs, sous le fond, au-dessus de la ligne −2 ou sur une case occupée) ne peut pas être
verrouillée. La décision de verrouiller (descente refusée) sera prouvée par le moteur (P1.12).

**Tests :** `BoardLockTest › locking a piece fills its four cells with its type on a new board`,
`BoardLockTest › cells in the hidden rows are lost, the visible ones join the board`,
`BoardLockTest › locking a piece that does not fit is refused, even by a cell in a hidden row`,
`BoardLockTest › the original board does not change`.

## Commandes

| Action | Effet |
|---|---|
| ◀ / ▶ | Déplacement d'une colonne. Maintenu : répétition automatique (DAS). |
| ▼ | Descente rapide : une première ligne **2 images** après l'appui, puis une ligne toutes les **2 images**. |
| A | Rotation horaire. |
| B | Rotation anti-horaire. |
| Pause | Met en pause et **masque le plateau** (sinon la pause sert à réfléchir). |

**Déplacement** : ◀, ▶ et ▼ décalent la pièce d'une case. Le déplacement est refusé si la pièce
ne tient plus à sa nouvelle place (mur, fond ou case occupée) ; il est accepté dans les lignes
cachées. Une descente refusée est le signal du verrouillage (P1.6).

**Tests :** `BoardMoveTest › a piece moves one column left, one column right or one row down, the original piece is unchanged`,
`BoardMoveTest › a move against the left or right wall is refused`,
`BoardMoveTest › a move down onto the floor is refused`,
`BoardMoveTest › a move onto an occupied cell is refused`,
`BoardMoveTest › a move within the hidden rows is accepted`.

**Répétition automatique (DAS)** : l'appui (image 0) déplace aussitôt ; maintenu, la pièce se
déplace de nouveau **16 images** après l'appui, puis toutes les **6 images**. Quand un mur ou
une case occupée bloque le déplacement, le DAS reste **chargé** : la pièce repart dès que la
place se libère, puis toutes les 6 images.

**Descente rapide** : l'appui de ▼ ne descend pas tout de suite ; la première ligne tombe
2 images après l'appui (la 3ᵉ image en comptant celle de l'appui), puis une ligne toutes les
2 images. Relâcher ▼ arrête aussitôt.

Les deux commandes suivent le même mécanisme (`AutoRepeat`), réglé par une politique
(`InputRepeat`, valeurs NES dans `ClassicInputRepeat`).

Sources : TetrisWiki, *Tetris (NES, Nintendo)* (« DAS initial delay is 16 frames, and then
every 6 frames », « Soft drop speed is 1/2G ») ; ROM d'après meatfighter.com, « Applying AI to
Nintendo Tetris » : `autorepeatY = 1` à l'appui de ▼ (`$8934`), descente quand il atteint 3
puis retour à 1 (`$894E`, `$8954`) ; `autorepeatX = 16` après un déplacement refusé (`$8A05`).

**Tests :** `AutoRepeatTest › on the first press the piece shifts at once`,
`AutoRepeatTest › while the shift is held nothing happens for 15 ticks then the piece shifts 16 ticks after the press`,
`AutoRepeatTest › then the held shift moves the piece every 6 ticks`,
`AutoRepeatTest › after a release a new press shifts at once and the count starts again`,
`AutoRepeatTest › a held soft drop moves the piece down a first row 2 ticks after the press then every 2 ticks`,
`AutoRepeatTest › releasing the soft drop stops it at once`,
`AutoRepeatTest › a shift charged against a wall moves at once when the way is free, then every 6 ticks`,
`RepeatTimingTest › a repeat interval of 0 is refused`, `RepeatTimingTest › a first repeat before the press is refused`.

Interactions entre commandes (ROM, même source) : ▼ maintenu bloque le déplacement latéral, ◀ ou
▶ maintenu empêche la descente rapide ; ▶ est testé avant ◀. Elles relèvent du moteur (P1.12).

## Délai d'apparition

Entre le verrouillage d'une pièce et l'apparition de la suivante, la NES attend de
**10 à 18 images** selon la hauteur à laquelle la pièce s'est verrouillée : 10 images dans
les deux lignes du bas, puis 2 de plus par groupe de 4 lignes au-dessus, au plus 18.

| Ligne du pivot | 19 – 18 | 17 – 14 | 13 – 10 | 9 – 6 | 5 et au-dessus (lignes cachées comprises) |
|---|---|---|---|---|---|
| Images | 10 | 12 | 14 | 16 | 18 |

Source : TetrisWiki, *Tetris (NES, Nintendo)* (« ARE is 10~18 frames depending on the height
at which the piece locked »). La hauteur se mesure sur la ligne du pivot : simplification,
probablement fidèle à la ROM, **[à vérifier]**. La règle est une politique interchangeable
(`EntryDelay`), dont `ClassicEntryDelay` porte les valeurs NES.

**Tests :** `ClassicEntryDelayTest › a piece locked in the two bottom rows waits 10 ticks before the next one appears`,
`ClassicEntryDelayTest › each group of 4 rows higher adds 2 ticks`,
`ClassicEntryDelayTest › the entry delay is at most 18 ticks`,
`ClassicEntryDelayTest › a row outside -2 to 19 is refused`.

Quand des lignes sont effacées, l'animation s'ajoute : 5 passes, une toutes les 4 images,
soit **17 à 20 images** selon l'image du verrouillage (TetrisWiki ; meatfighter.com, « The
clearing animation requires 5 passes », une toutes les quatre images). Le détail du calcul
est **[à vérifier]** ; il revient au moteur (P1.12).

## Lignes et score

Une ligne pleine (ses 10 cellules occupées) est effacée, les lignes au-dessus descendent
d'autant, et autant de lignes vides apparaissent en haut. Les lignes effacées peuvent ne pas
être contiguës. L'effacement rend aussi les indices des lignes effacées dans le plateau
d'avant, du haut vers le bas, pour l'animation.

**Tests :** `BoardClearTest › a full row is cleared and the rows above fall down one row`,
`BoardClearTest › two three and four contiguous full rows are cleared`,
`BoardClearTest › with full rows that are not contiguous everything falls to the right place`,
`BoardClearTest › without a full row the board does not change and no row is cleared`,
`BoardClearTest › empty rows appear at the top, as many as rows cleared`,
`BoardClearTest › a full top row is replaced by an empty row`.

| Lignes effacées d'un coup | Points |
|---|---|
| 1 | 40 × (niveau + 1) |
| 2 | 100 × (niveau + 1) |
| 3 | 300 × (niveau + 1) |
| 4 | 1200 × (niveau + 1) |

Aucune ligne effacée ne rapporte rien ; plus de 4 lignes ou un nombre négatif est refusé.

**Tests :** `ClassicScoringRuleTest › clearing no line scores no point`,
`ClassicScoringRuleTest › at level 0 each number of lines cleared has its base value`,
`ClassicScoringRuleTest › the base value is multiplied by the level plus one`,
`ClassicScoringRuleTest › a number of lines cleared outside 0 to 4 is refused`.

**Descente rapide** : 1 point par ligne descendue en maintenant ▼, compté une fois au
verrouillage pour les lignes descendues d'affilée par la pièce. C'est une
simplification assumée : le calcul réel de la NES est plus particulier **[à vérifier]**.

**Tests :** `ClassicScoringRuleTest › a soft drop scores one point per row dropped`,
`ClassicScoringRuleTest › a negative number of rows soft dropped is refused`.

## Niveaux

- Le joueur choisit un **niveau de départ** de 0 à 9.
- Premier passage de niveau quand le total de lignes atteint
  `min(départ × 10 + 10, max(100, départ × 10 − 50))`.
- Ensuite, un niveau de plus toutes les **10 lignes**.

Exemples : départ 0 → niveau 1 à 10 lignes ; départ 5 → niveau 6 à 60 lignes ;
départ 9 → niveau 10 à 100 lignes. Avant le premier passage, le niveau reste celui de départ
(départ 5 : niveau 5 de 0 à 59 lignes).

Pour un départ de 0 à 9, la formule vaut toujours `(départ + 1) × 10`, et le niveau est
`max(départ, lignes ÷ 10)`. La seconde branche (`départ × 10 − 50`) ne sert qu'aux départs 10
à 19 que la NES propose avec A + Start, absents de BloX. Un départ hors de 0 à 9 est refusé.

**Tests :** `ClassicLevelProgressionTest › starting at level 0 the first level up comes at 10 lines`,
`ClassicLevelProgressionTest › before the first level up the level stays at the start level`,
`ClassicLevelProgressionTest › the first level up comes at the start level plus one times 10 lines`,
`ClassicLevelProgressionTest › after the first level up the level rises every 10 lines`,
`ClassicLevelProgressionTest › a start level outside 0 to 9 is refused`,
`LinesTest › a total of lines cannot be negative`, `LinesTest › a total of 0 lines is the lowest valid total`.

**Ordre score / niveau** : quand un effacement fait monter de niveau, le total de lignes et le
niveau sont mis à jour d'abord, puis les points sont comptés **avec le nouveau niveau**.
Source : rétro-ingénierie de la ROM, meatfighter.com, « Applying AI to Nintendo Tetris »
(« After the total lines count and potentially the level are incremented, a value in that list
is multiplied by level number plus one »). À prouver par un test du moteur (P1.12).

## Statistiques de fin de partie

Score, lignes, niveau atteint, nombre de « 4 lignes » d'un coup, durée de la partie.
