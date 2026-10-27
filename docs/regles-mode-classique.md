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
- **Apparition** : la pièce apparaît avec son pivot en colonne 5, ligne 0, dans son
  orientation de départ (source ROM, meatfighter.com : les pièces apparaissent aux
  coordonnées (5, 0)). La position fait partie des règles du mode, pas du moteur.
- **Fin de partie** : la nouvelle pièce ne peut pas apparaître parce que sa position de départ
  est occupée ; elle reste affichée. Le moteur émet alors `GameOver`, une seule fois, seul
  événement de ce tick (le verrouillage et l'effacement ont eu lieu aux ticks d'avant, le
  délai d'apparition durant au moins un tick) ; ensuite, ni tick ni entrée ne changent plus
  l'état ni n'émettent rien. La fin n'est jamais précédée de `StackDanger(off)` : une apparition
  bloquée laisse une pile d'au moins 19 lignes, et la pile ne sort du danger qu'à 12 ou moins
  (garantie liée aux seuils de `ClassicStackDangerRule`). Le score, les lignes, le niveau, les
  statistiques et la durée se lisent dans l'état final.

  **Tests :** `GameEngineGameOverTest › GameOver is sent once, on the tick where the next piece cannot appear`,
  `GameEngineGameOverTest › the last events are PieceLocked, then LinesCleared if any, then GameOver, and never StackDanger(off)`,
  `GameEngineTickTest › if the next piece does not fit when it appears, the game is over and the piece stays shown`,
  `GameEngineTickTest › a piece that appears gives no event`,
  `GameEngineTickTest › a tick after the game is over changes nothing, the tick count included`,
  `GameEngineControlsTest › once the game is over, an input changes nothing`. Vue : V4.
- **Début de partie** : le niveau de départ doit appartenir à la plage du mode (0 à 9) ; la
  première pièce est tirée sans pièce précédente, la suivante avec la première comme
  précédente. Une même graine redonne la même partie (un moteur par partie, ADR 0014).

  **Tests :** `GameEngineStartTest › a game started at level 0 has level 0, no line, no point, an empty board and a falling piece`,
  `GameEngineStartTest › starting a game gives no event`,
  `GameEngineStartTest › the last starting level of the range is accepted`,
  `GameEngineStartTest › a starting level outside the range of the level progression is refused`,
  `GameEngineStartTest › the first piece is drawn with no previous piece, the next one with the first as previous`,
  `GameEngineStartTest › the piece in play appears with its pivot at (5, 0), in the spawn orientation of the rotation system`,
  `GameEngineFactoryTest › two engines created with the same seed deal the same pieces`,
  `ClassicLevelProgressionTest › the Classic mode accepts starting levels 0 to 9`.

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
  L'orientation de départ ne dépend que du type, quel que soit le moment ou le nombre de
  demandes : l'écran s'en sert aussi pour dessiner la pièce suivante.
  `ClassicRotationSystemTest › the spawn orientation of a type is the same however often it is asked`.
- La pièce suivante est affichée dans un aperçu (une seule), dans son orientation de départ
  selon les règles du moteur. Ses cellules sont calculées une fois par type, pas à chaque
  image, et la colonne latérale ne se redessine pas quand seule la pièce bouge.
  `GameViewModelTest › the screen gets the cells of the next piece in its spawn orientation`,
  `GameViewModelTest › the next piece shown is the same instance from one frame to the next while it does not change`,
  `GameScreenTest › the side column does not recompose while the piece moves` (V2).


**Lecture par TalkBack** (P5.3) : le plateau dit la hauteur de la pile, trous compris (« Plateau :
vide », « Plateau : pile de 4 rangées sur 20 »). Elle ne change qu'au verrouillage ou à
l'effacement et dit le danger ; la pièce en jeu n'y est pas, car elle bouge plusieurs fois par
seconde et relancerait TalkBack (et le dessin, P5.11). Pas de région annoncée d'office : une
annonce à chaque verrouillage couperait TalkBack en pleine partie. L'écran se lit toujours dans
le même ordre, en portrait comme en paysage : plateau, scores, pièce suivante, flèches, puis B
et A, chaque partie d'un bloc.
`BoardSpeechTest › TalkBack says the height of the stack, holes included, out of the rows of the board`,
`BoardSpeechTest › TalkBack says an empty board is empty, rather than a stack of no row`,
`BoardCanvasTest › TalkBack reads the height of the stack on the board, in rows`,
`BoardCanvasTest › TalkBack reads a stack of one row in the singular`,
`BoardCanvasTest › TalkBack reads an empty board as empty`,
`BoardCanvasTest › TalkBack reads the new height of the stack once a piece locks on top of it`,
`BoardCanvasTest › the painters are not built again when a piece locks and the stack grows`,
`GameScreenTest › TalkBack reads the board, the next piece, the score, the arrows, then B and A, each part as a whole`.
Vues : V2, V3. À essayer avec TalkBack sur un téléphone.
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
  orientation qui n'appartient pas au type de la pièce est refusée. Tourner le O ne change rien.
  Une rotation réussie émet `PieceRotated` (la pièce tournée, avant la chute du même tick), en
  tête des événements du tick ; une rotation refusée et le O n'émettent rien (P4.8, tranché :
  seulement si la pièce change). Il sert à l'effet sonore de rotation ; la rotation ne vibre pas.

  **Tests (événement) :** `GameEngineControlsTest › a piece that turns sends PieceRotated, with the piece as it turned`,
  `GameEngineControlsTest › a rotation refused, or one that leaves the O as it was, sends nothing`,
  `GameEngineControlsTest › a piece that turns then locks on the same tick sends PieceRotated before PieceLocked`,
  `GameEngineControlsTest › the rotation comes before the drop in a tick`,
  `GameEngineStackDangerTest › a piece that turns then locks into the danger sends PieceRotated, then PieceLocked, then StackDanger`,
  `GameEngineGameOverTest › a piece that turns then locks for the last time leaves GameOver alone on the tick it ends`,
  `HapticOfEventTest › a rotation gives nothing`.

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

**Dans le moteur** : à l'apparition, le compteur de gravité part de 0 ; il augmente d'un à
chaque tick, et la pièce descend quand il atteint la valeur de la table, puis il repart de 0.
Le tick d'apparition ne compte pas. Au niveau 0, la première descente a donc lieu 48 ticks
après l'apparition. Source : ROM d'après meatfighter.com (`$98BA` remet `fallTimer` à 0 à
l'apparition ; descente quand `fallTimer >= dropSpeed`).

**Tests :** `GameEngineTickTest › at level 0 the piece drops one row every 48 ticks, not before`,
`GameEngineTickTest › at level 29 the piece drops one row on every tick`,
`PhaseTest › a falling piece cannot have a negative gravity count`.

## Verrouillage

Pas de délai de verrouillage : quand la gravité devrait faire descendre la pièce et que la
case en dessous est occupée (ou que c'est le fond), la pièce **se verrouille immédiatement**.

Verrouiller remplit les cellules de la pièce avec son type, sur un nouveau plateau ; ses
cellules restées dans les lignes cachées sont perdues. Une pièce qui ne tient pas (hors des
murs, sous le fond, au-dessus de la ligne −2 ou sur une case occupée) ne peut pas être
verrouillée. La pièce se verrouille sur le tick même où la gravité tente la descente et
échoue : le « délai de verrouillage » de la NES égale donc le délai de chute (source ROM,
meatfighter.com). Le moteur émet alors `PieceLocked`.

**Tests :** `BoardLockTest › locking a piece fills its four cells with its type on a new board`,
`BoardLockTest › cells in the hidden rows are lost, the visible ones join the board`,
`BoardLockTest › locking a piece that does not fit is refused, even by a cell in a hidden row`,
`BoardLockTest › the original board does not change`,
`GameEngineTickTest › when the drop is refused the piece locks, PieceLocked is sent and the entry delay begins`,
`GameEngineTickTest › at level 0 a piece on the floor locks on the 48th tick, not before`.

## Commandes

| Action | Effet |
|---|---|
| ◀ / ▶ | Déplacement d'une colonne. Maintenu : répétition automatique (DAS). |
| ▼ | Descente rapide : une première ligne **2 images** après l'appui, puis une ligne toutes les **2 images**. |
| A | Rotation horaire. |
| B | Rotation anti-horaire. |
| Pause | Met en pause et **masque le plateau** (sinon la pause sert à réfléchir). Bouton dans la barre du haut ; l'écran de pause (Reprendre, Menu) remplace tout l’écran de jeu. |

**Commandes tactiles** : les cinq boutons de l'écran signalent l'appui et le relâchement, pour
l'appui maintenu. Plusieurs doigts peuvent tenir plusieurs boutons (◀ tenu pendant un appui sur
A) ; deux doigts sur le même bouton ne donnent qu'un appui, relâché au départ du dernier ; quand
les commandes quittent l'écran, tout est relâché ; un doigt qui se pose de nouveau relâche
d'abord son ancien bouton. L'écran transmet tout fidèlement : les exclusions de la NES (▼ contre
le latéral, ▶ prioritaire) sont appliquées par le moteur (P1.12d). Un doigt reste attaché au
bouton qu'il a touché en premier (pas de glissement d'un bouton à l'autre, à revoir en P2.8).

**Tests :** `HeldButtonsTest › a finger put on a button sends a single press`,
`HeldButtonsTest › lifting that finger sends a release`,
`HeldButtonsTest › a second finger on another button does not release the first, so left can be held while pressing A`,
`HeldButtonsTest › leaving the screen releases every held button`,
`HeldButtonsTest › two fingers on the same button give a single press, and the button is released when the last one lifts`,
`HeldButtonsTest › leaving the screen releases a button held by two fingers only once`,
`HeldButtonsTest › the held buttons are the buttons under at least one finger`,
`HeldButtonsTest › pressing again with a known finger releases its previous button`,
`HeldButtonsTest › a tap from accessibility services sends a press then a release, and leaves nothing held`.
Vues : V2, V3.

**Clavier et manette** (P5.27) : une touche tient son bouton comme un doigt, de l'appui au
relâchement ; un doigt et une touche sur le même bouton ne donnent qu'un appui. Les répétitions
du clavier sont ignorées (le moteur répète lui-même, DAS). Seul l'écran de jeu en cours écoute
les touches, et un toucher sur une commande ne lui retire pas le focus.

| Bouton | Touches |
|---|---|
| ◀ | flèche gauche (= croix de la manette), Q (ZQSD), A (WASD) |
| ▼ | flèche bas, S |
| ▶ | flèche droite, D |
| A, horaire | flèche haut du **clavier**, X, K, bouton A de la manette |
| B, anti-horaire | Z, W, Y (QWERTZ), J, Ctrl, boutons B et X de la manette (X est à gauche de A, comme B sur la NES) |
| Pause | Échap, P, Start |

Le haut de la croix d'une manette ne fait rien, comme sur la NES. Les touches du jeu sont
consommées : Android ne les replie pas en « retour ». En pause, Start, Échap et P reprennent la
partie ; les autres touches servent à passer d'un bouton à l'autre. En fin de partie, B, Échap
et les touches de jeu sont consommées sans effet (un joueur qui martèle la rotation ne quitte
pas l'écran de fin) ; les flèches, la croix et le bouton A restent au système pour atteindre
Menu et Rejouer. Les diagonales de croix (télécommandes) ne sont pas liées.

**Tests :** `KeyActionTest › the arrows and the cross of a pad, ZQSD and WASD hold left, down and right`,
`KeyActionTest › the up arrow, X, K and the A button of a pad hold A, the clockwise rotation`,
`KeyActionTest › Z, W and Y, J, Ctrl and the B and X buttons of a pad hold B, the counterclockwise rotation`,
`KeyActionTest › Escape, P and the Start button of a pad pause the game as they go down`,
`KeyActionTest › a pause key coming back up or repeated is kept from the system and does nothing`,
`KeyActionTest › a key event neither down nor up does nothing`,
`KeyActionTest › the repeats of a held key are kept from the system and do nothing, the engine repeats a held button (DAS)`,
`KeyActionTest › another key does nothing in the game, and is left to the system`,
`KeyActionTest › the up of the cross of a pad does nothing, as on the NES, and is kept from the system`,
`KeyActionTest › the other directions and buttons of a pad play as on a keyboard`,
`KeySourceTest › a keyboard, with or without arrows, is a keyboard`,
`KeySourceTest › a gamepad or a joystick is a pad, even when it also says it is a keyboard`,
`HeldControlsTest › a key of the game is kept from the system, another key is left to it`,
`HeldControlsTest › the pause is asked once, as Escape goes down, never as it comes back up`,
`HeldControlsTest › a key going down then up presses then releases its button`,
`HeldControlsTest › the up of the cross of a pad is kept from the system and holds nothing`,
`HeldButtonsTest › a key holds its button as a finger does, pressed going down and released coming back up`,
`HeldButtonsTest › a key and a finger are never taken for each other, even when the key code is the pointer id`,
`HeldButtonsTest › a finger and a key on the same button press it once, and release it when the last one lets go`,
`MenuKeyActionTest › in pause, Start, Escape and P resume the game as they go down`,
`MenuKeyActionTest › in pause, a resume key coming back up or repeated is kept from the system and does nothing`,
`MenuKeyActionTest › in pause, any other key is left to the system, to move between the buttons and press them`,
`MenuKeyActionTest › once the game is over, B, Escape and the other keys of the game are kept from the system and do nothing`,
`MenuKeyActionTest › once the game is over, the arrows, the A button of a pad and the keys of the menus are left to the system`,
`MenuKeyActionTest › a key of a menu asks the resume once and is kept from the system, another key is left to it`,
`MenuKeyActionTest › while the menu itself holds the focus, an arrow going down moves the focus into its buttons`,
`MenuKeyActionTest › an arrow on a menu holding the focus itself moves the focus into the buttons, and is kept from the system`,
`GameScreenKeysTest › a key held down holds its button, once despite the repeats of the keyboard, until it comes back up`,
`GameScreenKeysTest › Escape pauses the game, and the keys still held are released with the controls`,
`GameScreenKeysTest › a key held across the pause does nothing once the game resumes`,
`GameScreenKeysTest › once the game resumes, the screen takes the focus again and the keys play`,
`GameScreenKeysTest › after a touch on a control, the keys still play`,
`GameScreenKeysTest › in pause, Start resumes the game`,
`GameScreenKeysTest › in pause, the cross sent by the system still moves the focus to a button`,
`GameScreenKeysTest › once the game is over, B and Escape hammered through the system neither go back nor leave the screen`.
Vues : V2, V3 (jeu), pause et fin de partie. Rendre un bouton tenable via TalkBack ou Switch
Access reste à faire (P5.34) ; à vérifier sur une vraie manette et un vrai clavier (P2.8).

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
`RepeatTimingTest › a repeat interval of 0 is refused`, `RepeatTimingTest › a first repeat before the press is refused`,
`RepeatTimingTest › without a step on the press, no step is due on the tick of the press`,
`RepeatTimingTest › a first repeat and an interval of 1 tick give a step on every tick after the press`,
`RepeatTimingTest › no step is due on the tick just before the first repeat`.

**Commandes dans le moteur** (P1.12d, ROM au tick près, même source) :

- **Pause** (P1.15) : `Input.Pause` met la partie en pause (`GameState.paused`) et vide les
  commandes (touches tenues, appuis non encore traités, répétition latérale) ; en pause, `tick`
  ne change rien, `tickCount` compris (la durée de jeu n'avance pas), et seul `Input.Resume`
  compte : les appuis et relâchements sont ignorés. `Resume` reprend exactement où la partie
  s'était arrêtée (gravité, délai, animation). Une touche tenue à travers la pause doit être
  pressée de nouveau ; la descente rapide en cours est coupée et ses pas ne rapportent rien
  (son compteur reste visible pendant la pause, il est remis à zéro au premier tick). `Pause`
  en pause, `Resume` hors pause et `Pause` après la fin sont sans effet. Le masquage du plateau
  est l'affaire de l'écran (P2.6).

  **Tests :** `GameEnginePauseTest › Pause pauses the game, without changing the phase or sending an event`,
  `GameEnginePauseTest › while paused a tick changes nothing, the tick count included`,
  `GameEnginePauseTest › Resume goes on exactly where the game stood`,
  `GameEnginePauseTest › a press while paused is ignored, so A pressed during the pause does not turn the piece`,
  `GameEnginePauseTest › the pause forgets the held buttons and the shift repeat, a release during the pause does nothing`,
  `GameEnginePauseTest › the pause stops the soft drop, which needs a new press of down after the resume`,
  `GameEnginePauseTest › a press just before the pause is forgotten`,
  `GameEnginePauseTest › Pause while paused, Resume while playing and Pause after the end change nothing`.

  **À l'écran** (P2.2, P2.6) : le bouton pause de la barre du haut met en pause ; l'écran de
  pause remplace tout le jeu (plateau masqué) ; Reprendre reprend sans rattraper le temps passé
  en pause ; Menu quitte la partie. Le retour système met en pause en jeu, reprend en pause, et
  quitte la partie une fois la fin affichée, ou tant que la fenêtre est trop petite pour jouer
  (P2.24, `docs/maquette.md`, V2 et V3). Aucun tick n'est donné pendant la pause : la durée
  de jeu n'avance pas. Quand l'application passe en arrière-plan (`ON_PAUSE`), la partie se met
  en pause d'elle-même et y reste au retour, jusqu'à Reprendre (P5.4) ; une recréation de
  l'activité (thème sombre, langue, taille de fenêtre) fait de même. Pendant la pause, la boucle
  d'images est suspendue.

  **Tests :** `GameViewModelTest › on pause the game is paused`,
  `GameViewModelTest › while paused the frames change nothing, not even the duration of the game`,
  `GameViewModelTest › the controls received while paused are ignored, also after the game resumes`,
  `GameViewModelTest › on resume the game goes on exactly where it stood, without catching up the pause`,
  `GameViewModelTest › once the game is over a pause does nothing`,
  `GameViewModelTest › pausing a game already paused does nothing`,
  `GameViewModelTest › after a pause without frames, the frames start again without catching up the time paused`,
  `GameViewModelTest › frames started again while paused change nothing, the game stays paused until it resumes`,
  `BackActionTest › during the game the system back pauses it`,
  `BackActionTest › while paused the system back resumes the game`,
  `BackActionTest › once the game is over the system back leaves the game, as Menu does`,
  `GameScreenLifecycleTest › a screen no longer in front (ON_PAUSE) pauses the game`,
  `GameScreenLifecycleTest › a paused game asks for no frame: Compose becomes idle`,
  `GameScreenLifecycleTest › a game resumed goes on from where it stood, without catching up the time spent paused`,
  `GameScreenLifecycleTest › once the game is over, Rejouer and Menu do nothing until the screen is in front`,
  `BloxNavDisplayTest › in a game under way, the system back pauses the game, which stays on screen`,
  `BloxNavDisplayTest › in a paused game, the system back resumes it`,
  `BloxNavDisplayTest › once the game is over, the system back goes back to the home screen`,
  `BloxNavDisplayTest › once the game is over, Rejouer starts a new game at its level, in place of the finished one`,
  `BloxNavDisplayTest › once the game is over, Menu goes back to the home screen`. Vues : V2, V3,
  pause (pas encore maquettée, P2.16).

  **Après la mort du processus** (P5.13) : quand le système tue le processus d'une partie (en
  jeu, en pause ou sur l'écran de fin), la pile de navigation restaure la partie, qui revient
  **en pause et sans musique**, rejouée depuis sa route (même graine, même niveau de départ) :
  l'état du moteur n'est jamais sauvé (ADR 0014). Reprendre la lance et joue sa musique depuis
  le début ; Menu revient à l'accueil. L'écran de fin n'est pas restauré, mais le score avait
  déjà été enregistré et ne l'est pas une seconde fois. Seul un marqueur « partie commencée »
  passe par le `SavedStateHandle` de l'entrée de navigation (ADR 0013).

  **Tests :** `GameViewModelTest › a game recreated after the process died starts paused, with no music`,
  `GameViewModelTest › a first game, with nothing saved, is not paused and plays its music`,
  `GameViewModelTest › a game recreated after the process died plays its music from the start when the player resumes it`,
  `GameViewModelTest › a game over whose process died comes back as a new game, paused, its score not submitted again`,
  `GameViewModelFactoryTest › a new entry gives the game of its route, not paused`,
  `GameViewModelFactoryTest › an entry restored after the process died gives the game of its route, paused`,
  `GameEntryRestorationTest › a game entry restored after the process died shows its game paused`.
  Vue : pause.
- Hors pause, `apply(state, input)` n'enregistre que les touches (`Press`, `Release` de ◀ ▶ ▼ A B) ; tout
  se passe dans `tick`, dans l'ordre de la ROM : **déplacement, rotation, descente**. Un
  verrouillage, même par descente rapide, a donc lieu dans `tick`, avec le même délai, la même
  animation et les mêmes événements que par la gravité.
- **Appuis** : un appui compte sur front montant ; un appui de moins d'une image (pressé puis
  relâché entre deux ticks) compte quand même au tick suivant (écart assumé : sur NES, un appui
  dure au moins une image) ; presser une touche déjà tenue n'est pas un nouvel appui. Un appui
  fait pendant l'effacement ou le délai d'apparition est perdu, comme dans la ROM (le
  déplacement, la rotation et la descente n'y sont pas appelés) : chaque tick efface les appuis
  qu'il n'a pas lus, quelle que soit la phase. Un bouton pressé à ce moment et encore tenu est
  seulement tenu pour la pièce suivante : A ou B ne la tourne pas, ▼ ne la fait pas descendre,
  et ◀ ou ▶ ne relance pas le DAS : le déplacement reprend son compteur où il en était (un DAS
  chargé déplace la pièce dès son premier tick de chute), ou part de 0 s'il n'y en a pas encore.
- **Déplacement** : ▶ l'emporte sur ◀ ; ▼ tenu bloque le déplacement et fige le DAS, qui reprend
  où il en était (`$89B2`). Seul un **nouvel appui** de ◀ ou ▶ relance le DAS, quelle que soit
  la direction (`$89B8`) ; un changement de direction sans nouvel appui (◀ et ▶ tenus puis ▶
  relâché) continue le compteur, sans pas immédiat. Le DAS survit au verrouillage et reste figé
  pendant l'effacement et le délai d'apparition. Sans déplacement plus tôt dans la partie, un ◀
  ou ▶ tenu sans nouvel appui part d'un compteur à 0 (`autorepeatX`, `$0046`) : premier
  déplacement au 16ᵉ tick de chute, puis tous les 6 ticks (`$89C4` à `$89CE`, meatfighter.com).
  **[à vérifier]** : la valeur 0 suppose une RAM effacée en début de partie ; la source ne dit
  pas ce que contient `autorepeatX` avant le premier déplacement.
- **Rotation** : A (horaire) l'emporte sur B s'ils sont pressés ensemble (`$88B4`) ; la pièce
  prend le premier candidat du système de rotation qui tient, sinon elle ne bouge pas.
- **Descente rapide** : elle démarre sur un **nouvel appui de ▼**, sans appui de ◀ ou ▶ dans le
  même tick (`$892A` à `$892E`) ; A ou B pressé avec ▼ ne l'empêche pas. Elle continue tant que
  ▼ est tenu seul ; ◀ ou ▶ tenu, ou ▼ relâché, l'arrête et remet ses pas à zéro, et il faut un
  nouvel appui de ▼ (`$8939` à `$8945`). Un ▼ tenu à travers le verrouillage ne fait donc pas
  descendre la pièce suivante. Un pas de descente rapide remet la gravité à zéro (`$8958`) ; au
  plus une descente par tick.
- **Points de descente rapide** : les pas faits d'affilée, comptés au verrouillage. Le pas
  refusé qui verrouille ne compte pas : la ROM l'ajoute à `holdDownPoints` puis retire 1 au
  score (`holdDownPoints − 1` si au moins 2, d'après le désassemblage de `$9BFE`, cité de
  mémoire) ; le résultat net est le même. Deux nuances non reproduites, **[à vérifier]** sur
  le désassemblage : un verrouillage par la gravité pendant une descente rapide (niveaux 19 et
  plus) donnerait un point de moins dans la ROM, et l'addition BCD peut être fausse pour les
  grandes valeurs.

**Tests :** `GameEngineControlsTest › pressing left moves nothing at once, the piece shifts one column on the next tick`,
`GameEngineControlsTest › a held left shifts on the first tick, then 16 ticks after the press, then every 6 ticks`,
`GameEngineControlsTest › when left and right are held together, right wins`,
`GameEngineControlsTest › a new press of left or right starts the shift repeat again, so the piece shifts at once`,
`GameEngineControlsTest › a change of direction without a new press goes on with the shift repeat, with no shift at once`,
`GameEngineControlsTest › a shift refused by the stack charges the repeat, so the piece shifts as soon as the way is free`,
`GameEngineControlsTest › A turns the piece clockwise and B counterclockwise, on the next tick`,
`GameEngineControlsTest › a held A turns the piece once, a short tap between two ticks turns it too`,
`GameEngineControlsTest › a held down drops the piece a first row 2 ticks after the press, then every 2 ticks`,
`GameEngineControlsTest › a soft drop step starts the gravity count again`,
`GameEngineControlsTest › the piece drops at most one row per tick, even when the soft drop and the gravity both fall due`,
`GameEngineControlsTest › a held down blocks the shift`,
`GameEngineControlsTest › a held shift stops the soft drop and its points, and down must be pressed again to soft drop`,
`GameEngineControlsTest › down released and pressed again between two ticks starts the soft drop again`,
`GameEngineControlsTest › the rows soft dropped score when the piece locks, the refused step that locks it does not count`,
`GameEngineControlsTest › only the rows soft dropped in a row score, a release of down starts them again`,
`GameEngineControlsTest › a lock by soft drop goes the way of a lock by gravity, line clear, entry delay and events included`,
`GameEngineControlsTest › the held shift outlives the lock and stands still during the line clear and the entry delay`,
`GameEngineControlsTest › a down held through the lock does not drop the next piece until it is pressed again`,
`GameEngineControlsTest › a press of a button already held is not a new press`,
`GameEngineControlsTest › a press of down starts the soft drop unless left or right is pressed on the same tick`,
`GameEngineControlsTest › the shift comes before the rotation in a tick`,
`GameEngineControlsTest › the rotation comes before the drop in a tick`,
`GameEngineControlsTest › a rotation against the wall is refused, and a new press of B on the next tick turns the piece back`,
`GameEngineControlsTest › A and B pressed together turn the piece once, clockwise`,
`GameEngineControlsTest › a held down freezes the shift repeat, which goes on where it stood when down is released`,
`GameEngineControlsTest › once the game is over, an input changes nothing`,
`GameEngineControlsTest › A or B pressed before the next piece is lost, it appears unturned`,
`GameEngineControlsTest › left or right pressed before the next piece, with no shift repeat yet, shifts it on its 16th tick of fall`,
`GameEngineControlsTest › left or right pressed before the next piece, with a charged shift repeat, shifts it on its first tick of fall`,
`GameEngineControlsTest › down pressed before the next piece does not soft drop it until it is pressed again`,
`GameEngineControlsTest › A or left pressed on the last tick of the entry delay is lost, the next piece neither turns nor shifts`. Vues : V2, V3.

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

**Dans le moteur** : le délai se compte à partir du tick qui suit le verrouillage (le tick du
verrouillage n'en fait pas partie, **[à vérifier]** contre la ROM) ; la pièce suivante apparaît
au dernier tick du délai, avec son compteur de gravité à 0, et un nouveau tirage prend la
pièce qui apparaît comme précédente. Un I vertical verrouillé pivot en ligne 18 (cellules 16 à
19) attend 10 ticks : la hauteur se mesure bien sur le pivot.

**Tests :** `GameEngineTickTest › the next piece appears when the entry delay of the locked piece is over, and a new draw follows it`,
`GameEngineTickTest › a vertical I locked with its pivot on row 18, cells on rows 16 to 19, waits 10 ticks`,
`GameEngineTickTest › if the next piece does not fit when it appears, the game is over and the piece stays shown`,
`PhaseTest › an entry delay lasts at least one tick`.

Quand des lignes sont effacées, l'animation s'ajoute : 5 passes, une toutes les 4 images,
soit **17 à 20 images** selon l'image du verrouillage (TetrisWiki ; meatfighter.com, « The
clearing animation requires 5 passes », une toutes les quatre images).

**Dans le moteur** : un verrouillage qui complète des lignes ouvre l'animation ; le plateau
affiché reste celui d'avant l'effacement. Si le verrouillage a lieu au tick N de la partie,
l'animation dure `16 + (4 − N mod 4)` ticks (17 à 20) ; elle finit donc toujours sur un
multiple de 4, à la 5ᵉ passe. Le délai d'apparition, calculé au verrouillage, ne commence
qu'ensuite. Aucun événement n'est émis pendant l'animation. **Écart assumé** : la NES aligne
l'animation sur un compteur d'images global, qui tourne depuis l'allumage ; BloX utilise le
compteur de la partie (`GameState.tickCount`, arrêté en pause et en fin de partie), pour que
tout se déduise de la graine. La différence (1 à 3 images) n'est pas perceptible.

**Tests :** `GameEngineClearTest › a lock that fills rows starts the clear, the board shown still holds the full rows`,
`GameEngineClearTest › the clear lasts 16 ticks plus the ticks up to the next multiple of 4 of the game tick count`,
`GameEngineClearTest › after the clear comes the entry delay computed when the piece locked, then the next piece`,
`GameEngineClearTest › the ticks between the lock and the end of the clear give no event`,
`PhaseTest › a clear lasts at least one tick`, `TicksTest › a duration of 0 ticks or less is refused`,
`TicksTest › a duration of 1 tick is the shortest one accepted`,
`TicksTest › a duration of 1 tick is over after one tick`,
`TicksTest › a longer duration has one tick less left after one tick` (la gravité et le délai
d'apparition rendent un `Ticks`, au moins 1 ; les décomptes de l'effacement et du délai
d'apparition, et les délais de répétition des commandes, sont aussi des `Ticks`, et
l'effacement comme le délai d'apparition se décomptent de la même façon, P5.21).

**Durée de l'effacement, politique des règles** (P6.2) : la durée de l'effacement n'est plus
écrite dans le moteur. C'est une politique, `LineClearDelay`, que le mode Classique fournit avec
`ClassicLineClearDelay` (16 ticks, plus les ticks jusqu'au multiple de 4 suivant du tick du
verrouillage). Un effacement dure au moins 1 tick, dans les deux modes.

**Tests :** `ClassicLineClearDelayTest › the clear lasts 16 ticks plus the ticks up to the next multiple of 4 of the lock tick`,
`ClassicLineClearDelayTest › a negative lock tick is refused`,
`RulesTest › the Classic rules clear lines with the line clear delay of the NES`,
`GameEngineClearTest › the clear lasts as long as the line clear delay of the rules says`.

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

Aucune ligne effacée ne rapporte rien. Le nombre de lignes effacées d'un coup est un `LineClear`, qui
porte la borne 0 à 4 : plus de 4 lignes ou un nombre négatif est refusé dès sa construction, avant
le calcul des points (voir « Statistiques de fin de partie » plus bas).

**Affichage du score** : sur 6 chiffres, plafonné à 999 999 comme sur la NES ; le domaine
garde la vraie valeur, seul l'affichage plafonne, partout où le score est montré (jeu, fin
de partie, record) **[à vérifier]**. Le niveau et les lignes ne sont pas plafonnés (ce que fait
la NES au-delà de 999 lignes est **[à vérifier]**).

**Record** : le meilleur score, gardé avec sa vraie valeur (sans plafond), vaut 0 avant la
première partie. Il est enregistré dès la fin de partie. Un score devient un **nouveau record**
s'il dépasse strictement le record d'avant la partie (égaler ne suffit pas, 0 à la première
partie non plus). Un fichier illisible ou corrompu compte comme « pas de record ».

**Ancien record en fin de partie** (P9.14, V30, décision du développeur du 2026-10-27) : sous le
score final, une ligne rappelle le record d'avant la partie, tel que le dépôt l'a rendu à la fin
(`RecordResult.previous`) : « Ancien record 098760 » si la partie l'a battu, « Record 098760 »
sinon. Valeur sur 6 chiffres comme le score (plafond 999 999) ; TalkBack la lit avec le score, à sa
vraie valeur : « Score : 123450, Ancien record : 98760 ». Pas de ligne sans record d'avant (0,
première partie) ni quand les records n'ont pu être écrits.
**Tests :** `GameOverTextsTest › the best before the game is shown as a score, capped at 999 999, and read at its real value`,
`GameOverTextsTest › without a best before the game no best is recalled`,
`GameResultsTest › a classic game over tells the record before it, beaten or not`,
`GameViewModelTest › a classic game over gets the record it had to beat`,
`GameLabelsTest › the game over recalls the record as Ancien record once beaten, as Record otherwise`,
`GameScreenTest › the game over recalls the best before the game under the score, read with it; nothing without one` ;
captures `GameOverColorful`, `GameOverColorfulNight`, `GameOverColorfulNewRecord`, `GameOverColorfulNewRecordNight`.
Vues : V30.

**Effacer le record** (P5.66, pour les Paramètres v2) : le record revient à 0, et le score suivant
qui dépasse 0 devient un nouveau record. Effacer sans record laisse 0. Une écriture impossible
(disque plein) échoue avec une `IOException` et garde le record tel qu'il était ; le flux du record
émet 0 après un effacement réussi.

**Tests :** `HighScoreRepositoryContract › a cleared record is 0`,
`HighScoreRepositoryContract › a score after a clear is a new record`,
`HighScoreRepositoryContract › clearing without a record leaves 0`,
`HighScoreRepositoryContract › the record flow emits 0 after a clear`,
`HighScoreRepositoryContract › a submit that cannot be written fails with an IOException`,
`HighScoreRepositoryContract › a clear that cannot be written fails with an IOException`,
`HighScoreRepositoryContract › a clear that cannot be written keeps the record`,
`FakeHighScoreRepositoryTest › a clear waits for the gate before it clears`.

**Tests :** `HighScoreRepositoryContract › without a saved record the record is 0`,
`HighScoreRepositoryContract › a higher score becomes the record, and submit tells it is a new record and the previous one`,
`HighScoreRepositoryContract › a lower or equal score leaves the record and is not a new record`,
`HighScoreRepositoryContract › a score of 0 in the first game is not a new record`,
`HighScoreRepositoryContract › a first positive score is a new record against 0`,
`HighScoreRepositoryContract › a negative score is refused and leaves the record at 0`,
`DataStoreHighScoreRepositoryTest › the record survives a new repository on the same file`,
`HighScoreRepositoryContract › the record flow emits the new record after a submit`,
`HighScoreRepositoryContract › a lower score emits nothing on the record flow`,
`HighScoreRepositoryContract › two concurrent submits keep the highest score, the second one seeing the first`,
`DataStoreFileContract › an unreadable file reads as an empty one, and what is written next is kept`,
`DataStoreFileContract › a file that cannot be read reads as an empty one`,
`GameViewModelTest › at the game over the score is submitted once and the screen gets the final values and the record result`,
`GameViewModelTest › once the game is over the frames change nothing and the score is not submitted again`,
`GameViewModelTest › while the score is being submitted the game over waits, then shows once with the answer`,
`GameViewModelTest › the score is saved even when the view model is cleared while it is being submitted`,
`GameViewModelTest › when the records cannot be written the game over still shows, without a new record`,
`GameViewModelTest › a game over below the record shows no new record`,
`GameViewModelTest › the record shown is the one of the records, its real value, updated when it changes`,
`GameViewModelTest › the record stays shown when the frames move the game`,
`GameViewModelTest › until the records give it the record is unknown, not 0`,
`GameViewModelTest › unreadable records show 0 and the game goes on`.
Vues : V1, V2, V3, V4.

**Tests :** `SidePanelTextTest › the score is shown on 6 digits padded with zeros`,
`SidePanelTextTest › beyond 999 999 the score shown stays at 999 999`,
`SidePanelTextTest › the level is shown on at least 2 digits, without a cap`,
`SidePanelTextTest › the lines are shown on at least 3 digits, without a cap`,
`TopBarTextsTest › the record is shown as the score in the game, capped at 999 999`,
`TopBarTextsTest › TalkBack reads the real record, without padding zeros nor cap`. Vues : V2, V3.

**Tests :** `ClassicScoringRuleTest › clearing no line scores no point`,
`ClassicScoringRuleTest › at level 0 each number of lines cleared has its base value`,
`ClassicScoringRuleTest › the base value is multiplied by the level plus one`,
`LineClearTest › a number of lines cleared at once outside 0 to 4 is refused`.

**Descente rapide** : 1 point par ligne descendue en maintenant ▼, compté une fois au
verrouillage pour les lignes descendues d'affilée par la pièce. C'est une
simplification assumée : le calcul réel de la NES est plus particulier **[à vérifier]**.

**Tests :** `ClassicScoringRuleTest › a soft drop scores one point per row dropped`,
`ClassicScoringRuleTest › a negative number of rows soft dropped is refused`.

## Vibrations

Optionnelles (P5.2), **désactivées par défaut** (décision du développeur ; réglage `vibrations` des
préférences, interrupteur de l'accueil V1, P5.37).
Une ligne effacée (1 à 3) donne un clic bref, quatre lignes un clic fort (effets prédéfinis
d'Android à partir d'Android 10, impulsions de 20 et 50 ms avant), la fin de partie 80 ms,
une pause de 100 ms puis 200 ms. La pose d'une pièce et le danger ne vibrent pas (trop
fréquents ; la musique marque déjà le danger). Rien ne vibre en pause, après la fin de partie,
avant que les réglages répondent, ni sur un appareil sans vibreur. Les vibrations sont celles
d'un jeu (`USAGE_MEDIA`) : Ne pas déranger ne les coupe pas, l'économiseur de batterie et
l'interrupteur général des vibrations les coupent. Le réglage est lu une fois par partie.

**Tests :** `HapticOfEventTest › one to three lines cleared give the short pulse`,
`HapticOfEventTest › four lines cleared at once give the stronger pulse`,
`HapticOfEventTest › the game over gives its own pattern`,
`HapticOfEventTest › a clear of no line, a lock and the danger give nothing`,
`HapticEffectsTest › from Android 10 the lines cleared play the predefined click`,
`HapticEffectsTest › from Android 10 four lines cleared play the predefined heavy click`,
`HapticEffectsTest › before Android 10 the lines cleared play a pulse of 20 ms, four lines one of 50 ms`,
`HapticEffectsTest › the game over plays 80 ms, a pause of 100 ms, then 200 ms, on every version`,
`GameViewModelTest › clearing one line vibrates the short pulse`,
`GameViewModelTest › clearing four lines at once vibrates the stronger pulse`,
`GameViewModelTest › the game over vibrates its pattern once, and the frames after it nothing more`,
`GameViewModelTest › with the vibrations turned off in the settings nothing vibrates`,
`GameViewModelTest › until the settings answer nothing vibrates`,
`GameViewModelTest › a game recreated paused after the process died vibrates nothing while it stays paused`,
`UserPreferencesRepositoryContract › without saved preferences the defaults are the Original theme, Korobeiniki, level 0 and vibrations off`,
`UserPreferencesRepositoryContract › a saved vibrations choice is read back`,
`DataStoreUserPreferencesRepositoryTest › settings saved before the vibrations existed keep their choices, with vibrations off`,
`DataStoreUserPreferencesRepositoryTest › the vibrations are stored as a boolean under a key that must not change`.
Vue : V1 (interrupteur, `docs/maquette.md`). À essayer sur un téléphone.

**Vibration des touches** (P5.67, réglage `keyVibration` des Préférences v2, désactivé par
défaut) : l'appui d'une touche de jeu donne la vibration la plus légère, le tick prédéfini
d'Android (`EFFECT_TICK`, « less strong compared to EFFECT_CLICK », doc Android) à partir
d'Android 10, une impulsion de 10 ms avant **[à vérifier]** sur un téléphone Android 8 ou 9 à
moteur lent. Seul l'appui vibre, jamais le relâchement ni la répétition du DAS (elle est dans le
moteur, pas dans les appuis). Le réglage est indépendant de l'interrupteur `vibrations` (lignes et
fin de partie) et lu une fois par partie ; rien ne vibre en pause, après la fin de partie ni avant
que les réglages répondent. C'est un toucher : à partir d'Android 13 (`USAGE_TOUCH`), le réglage
« vibration au toucher » d'Android la coupe ; avant, seul le réglage de BloX décide. Un nouvel appui
remplace une vibration en cours (un clic de quatre lignes peut être écourté). `Haptics.canVibrate`
dit si l'appareil a un vibreur, pour griser les réglages de vibration (P5.70).

**Tests :** `HapticEffectsTest › the key press is the predefined tick from Android 10 and a pulse of 10 ms before`,
`HapticEffectsTest › the key press vibrates as a touch, the others as a game`,
`SystemHapticsTest › the haptics can vibrate exactly when the device has a vibrator`,
`SystemHapticsTest › every haptic is accepted by the system, with its effect and its usage`,
`GameViewModelTest › a pressed key vibrates once when key vibration is on`,
`GameViewModelTest › a released key does not vibrate`,
`GameViewModelTest › with key vibration off no key vibrates`,
`GameViewModelTest › until the settings answer no key vibrates`,
`GameViewModelTest › a key pressed while the game is paused does not vibrate`,
`GameViewModelTest › a key pressed after the game over does not vibrate`,
`GameViewModelTest › the key vibration is read once per game, at its start`.

## Effets sonores

Optionnels (P4.8), **activés par défaut** depuis le 2026-10-25 (décision du développeur, P5.65 : un
fichier sans la clé `sound_effects` les active, un « désactivé » enregistré par le joueur est gardé ;
avant, désactivés par défaut ; réglage `soundEffects` des
préférences, interrupteur de l'accueil V1, P5.37 ; un changement vaut pour la partie suivante). Une rotation réussie donne un
bip, une à trois lignes un arpège montant, quatre lignes un arpège sur deux octaves, la fin de
partie un arpège descendant ; la pose d'une pièce et le danger ne sonnent pas (trop fréquents).
Les effets se jouent par-dessus la musique sans la couper. Rien ne sonne en pause, après la fin
de partie ni avant que les réglages répondent ; le réglage est lu une fois par partie, et les
effets sont préparés dès qu'il répond, pour que le premier ne soit pas en retard. Détail des
sons : `docs/musique.md`, Effets sonores.

**Tests :** `SoundEffectOfEventTest › a rotation gives the rotation effect`,
`SoundEffectOfEventTest › one to three lines cleared give the lines cleared effect`,
`SoundEffectOfEventTest › four lines cleared at once give the four lines effect`,
`SoundEffectOfEventTest › the game over gives its own effect`,
`SoundEffectOfEventTest › a clear of no line, a lock and the danger give nothing`,
`GameViewModelTest › clearing one line plays the lines cleared effect`,
`GameViewModelTest › clearing four lines at once plays the four lines effect`,
`GameViewModelTest › a piece turned plays the rotation effect on the frame of its tick`,
`GameViewModelTest › the game over plays its effect once, and the frames after it nothing more`,
`GameViewModelTest › with the sound effects turned off in the settings no effect plays, the vibrations still do`,
`GameViewModelTest › with the vibrations turned off in the settings the sound effects still play`,
`GameViewModelTest › until the settings answer no effect plays`,
`GameViewModelTest › a game recreated paused after the process died plays no effect while it stays paused`,
`GameViewModelTest › with the sound effects on, the game warms them all up as soon as the settings answer, before any plays`,
`GameViewModelTest › with the sound effects off, the game warms nothing up`,
`UserPreferencesRepositoryContract › without saved preferences the sound effects are on`,
`UserPreferencesRepositoryContract › a saved sound effects choice is read back`,
`DataStoreUserPreferencesRepositoryTest › settings saved before the sound effects existed keep their choices, with sound effects on`,
`DataStoreUserPreferencesRepositoryTest › the sound effects are stored as a boolean under a key that must not change`.
Vue : V1 (interrupteur, `docs/maquette.md`). À essayer sur un téléphone.

## Préférences v2

Réglages ajoutés pour Couleurs v2 (P5.65), utilisés par les tâches qui suivent : **Effets
réduits** (P5.71), **Gaucher** (P5.79), **Vibration des touches** (P5.67), **Volume de la
musique** et **Volume des effets** (P5.68). Sans fichier, effets non réduits, droitier, touches
sans vibration, deux volumes à 100 %. Un volume est un `Volume` (`core:music`), un pourcentage de
0 à 100 par pas de 10 (`Volume.FULL` vaut 100, `Volume.SILENT` 0) ; toute autre valeur est
refusée, et une valeur illisible du fichier retombe sur la valeur par défaut. Clés figées :
`reduced_effects`, `left_handed`, `key_vibration` (booléens), `music_volume`, `effects_volume`
(entiers en pourcentage). Un fichier d'avant Couleurs v2 garde tous ses choix.

**Volumes appliqués** (P5.68) : le volume de la musique vaut pour toute musique (partitions,
fichier personnel, aperçus de l'accueil et de l'éditeur, parties), celui des effets pour les effets
sonores. Un volume devient une amplitude par `Volume.gain` = (pourcentage ÷ 100)², une courbe qui
suit l'oreille (50 % donne un quart de l'amplitude, environ −12 dB ; 10 %, le plus bas, −40 dB).
Un changement dans les Paramètres s'entend tout de suite, même pendant une musique ; un volume de 0
joue en silence sans arrêter la musique. Le volume reste d'une lecture à la suivante. Au démarrage,
tout joue à 100 % jusqu'à la première lecture des réglages (seule une partie restaurée après la
mort du processus peut en jouer quelques tranches).

**Tests :** `VolumeTest › a volume of 0 gives a gain of 0 and 100 a gain of 1`,
`VolumeTest › the gain grows with the volume`,
`VolumeTest › half the volume gives a quarter of the gain, as the ear hears loudness on a curve`,
`MusicPlayerContract › a volume set before a play applies to it`,
`MusicPlayerContract › a volume set while a score plays applies to it, down to silence, without stopping it`,
`ReopeningSinkTest › an output reopened plays at the gain set`,
`AudioFilePlayerTest › a volume set applies to the file playing and to the files played after it`,
`MusicSwitchTest › the volume goes to the scores and to the music files alike`,
`SoundEffectTracksTest › the volume of the effects applies to each track`,
`SoundEffectTracksTest › a track that cannot take the volume reports its failure, is released, and another one opens 5 s later`,
`SoundEffectTracksTest › a track that cannot take the volume as it opens is released and plays nothing`,
`SavedVolumesTest › a volume changed in the settings applies to the music playing`,
`SavedVolumesTest › the volume of the effects saved in the settings applies to the sound effects, apart from the music`,
`SavedVolumesTest › another setting changed applies no volume again`,
`StaticAudioTrackTest › an effect turned down, then to silence, plays without failing`.

**Tests :** `VolumeTest › a volume is a multiple of 10 from 0 to 100`,
`VolumeTest › a volume below 0, above 100 or between two steps of 10 is refused`,
`VolumeTest › a percentage that is no volume gives no volume, without failing`,
`VolumeTest › a percentage that is a volume gives that volume`,
`VolumeTest › the full volume is 100 percent`, `VolumeTest › the silent volume is 0 percent`,
`UserPreferencesRepositoryContract › without saved preferences effects are not reduced, right handed, keys do not vibrate, volumes at 100`,
`UserPreferencesRepositoryContract › each new setting saved is read back`,
`DataStoreUserPreferencesRepositoryTest › settings saved before Couleurs v2 keep their choices`,
`DataStoreUserPreferencesRepositoryTest › each new setting is stored under a key that must not change`.

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

**Niveau dérivé** (P5.20) : le niveau d'une partie n'est jamais fourni, il est toujours celui
que la progression du mode donne pour le niveau de départ et les lignes (`GameProgress`, qui
porte le niveau de départ). Il ne passe jamais sous le niveau de départ (refusé à la
construction) et ne baisse jamais au fil des effacements : un effacement qui le ferait baisser
est refusé, contre une progression fautive d'un autre mode. Les tests du moteur partent donc
d'états atteignables : « niveau 29 » veut dire départ 0 et 290 lignes.

**Tests :** `GameProgressTest › the level is the one the level progression gives for the start level and the lines`,
`GameProgressTest › a progression that gives a level below the start level is refused`,
`GameProgressTest › a clear with a progression whose level goes down is refused`,
`ClassicLevelProgressionTest › the level never goes below the start level, and never goes down as lines are cleared`,
`GameEngineClearTest › the level never goes down after a clear`.

**Ordre score / niveau** : quand un effacement fait monter de niveau, le total de lignes et le
niveau sont mis à jour d'abord, puis les points sont comptés **avec le nouveau niveau**.
Source : rétro-ingénierie de la ROM, meatfighter.com, « Applying AI to Nintendo Tetris »
(« After the total lines count and potentially the level are incremented, a value in that list
is multiplied by level number plus one »). Dans le moteur, cela se passe à la fin de
l'animation d'effacement, avec l'événement `LinesCleared` ; le score est un type `Points`, jamais
négatif.

**Tests :** `GameEngineClearTest › when the clear is over the cleared board is shown, lines and level go up, points use the new level`,
`PointsTest › points cannot be negative`, `PointsTest › points add up`,
`LinesTest › the lines cleared at once add to the total`.

**Événement de niveau** (P5.64) : au tick où un effacement fait monter de niveau (la fin de son
animation), le moteur émet `LevelUp(niveau atteint)`, après `LinesCleared` et avant un éventuel
`StackDanger` de ce tick. Un effacement fait au plus 4 lignes et les paliers sont espacés de 10 :
il ne franchit jamais qu'un niveau. Aucun `LevelUp` au début d'une partie ni quand le niveau ne
change pas ; un départ à 5 en reçoit un premier à 60 lignes. Il précède aussi la fin de partie
quand le dernier effacement fait monter de niveau. Il joue le son « niveau » (P5.69, quatre sauts
d'octave sur do, `docs/musique.md`), qui remplace sur ce tick l'arpège d'une à trois lignes ; quatre
lignes gardent leur effet seul, et le passage ne se voit alors qu'à l'écran. La rotation et la fin
de partie du même tick jouent en plus. Il ne vibre pas : l'effacement du même tick vibre déjà.

**Tests :** `GameEngineClearTest › a line clear that reaches the next level sends a level up with the new level`,
`GameEngineClearTest › a line clear that stays at the same level sends no level up`,
`GameEngineClearTest › the level up comes after the lines cleared and before the stack danger of that tick`,
`GameEngineClearTest › starting at level 5 the first level up comes at 60 lines, not at 10`,
`GameEngineStartTest › a game that starts sends no level up`,
`GameEngineGameOverTest › a last line clear that reaches the next level sends PieceLocked, LinesCleared, LevelUp, then GameOver`,
`HapticOfEventTest › a level up does not vibrate`,
`SoundEffectOfEventTest › a level up gives the level up sound`,
`SoundEffectOfEventTest › on the tick of a level up the level up sound replaces the one to three lines effect`,
`SoundEffectOfEventTest › four lines that reach the next level play the four lines effect alone`,
`SoundEffectOfEventTest › the other effects of a tick all play, in order, as the game over after a level up`,
`SoundEffectOfEventTest › a rotation on the tick of a level up still plays, before the level up sound`,
`GameViewModelTest › a level up plays the level up sound when the sound effects are on`.

## Statistiques de fin de partie

Score, lignes, niveau atteint, nombre de « 4 lignes » d'un coup, durée de la partie. Score,
lignes et niveau viennent de l'état de la partie ; les statistiques ajoutent le nombre de
« 4 lignes » d'un coup, compté à la fin de l'animation d'effacement. La durée est le compteur
de ticks de la partie (`GameState.tickCount`) : chaque tick qui fait avancer la partie, délais
et animations compris ; il ne bouge plus après la fin (et pas en pause, P1.15). La conversion
en minutes et secondes appartient à l'écran : le domaine ne connaît que les ticks.

Un effacement compte de 0 à 4 lignes d'un coup ; toute autre valeur est refusée.

**Tests :** `GameStatisticsTest › a game starts with no four-line clear`,
`GameStatisticsTest › only a clear of 4 lines at once counts as a four-line clear`,
`GameStatisticsTest › four-line clears add up over the game`,
`GameEngineClearTest › a four-line clear counts in the statistics of the game`,
`GameEngineTickTest › a tick after the game is over changes nothing, the tick count included`,
`GameStateTest › a negative tick count is refused`,
`LineClearTest › a number of lines cleared at once outside 0 to 4 is refused`,
`LineClearTest › only 4 lines cleared at once make four lines`,
`BoardClearTest › the result gives the number of lines cleared at once`.
