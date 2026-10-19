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
  quitte la partie une fois la fin affichée. Aucun tick n'est donné pendant la pause : la durée
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
  `BackActionTest › once the game is over the system back leaves the game, as Menu does`. Vues : V2, V3,
  pause (pas encore maquettée, P2.16).
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
`TicksTest › a duration of 1 tick is the shortest one accepted` (la gravité et le délai d'apparition rendent un `Ticks`, au moins 1).

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

**Tests :** `DataStoreHighScoreRepositoryTest › without a saved record the record is 0`,
`DataStoreHighScoreRepositoryTest › a higher score becomes the record, and submit tells it is a new record and the previous one`,
`DataStoreHighScoreRepositoryTest › a lower or equal score leaves the record and is not a new record`,
`DataStoreHighScoreRepositoryTest › a score of 0 in the first game is not a new record`,
`DataStoreHighScoreRepositoryTest › a first positive score is a new record against 0`,
`DataStoreHighScoreRepositoryTest › a negative score is refused and leaves the record at 0`,
`DataStoreHighScoreRepositoryTest › the record survives a new repository on the same file`,
`DataStoreHighScoreRepositoryTest › the record flow emits the new record after a submit`,
`DataStoreHighScoreRepositoryTest › two concurrent submits keep the highest score, the second one seeing the first`,
`DataStoreHighScoreRepositoryTest › an unreadable records file gives a record of 0, and the next score becomes the record`,
`DataStoreHighScoreRepositoryTest › a records file that cannot be read gives a record of 0`,
`GameViewModelTest › at the game over the score is submitted once and the screen gets the final values and the record result`,
`GameViewModelTest › once the game is over the frames change nothing and the score is not submitted again`,
`GameViewModelTest › while the score is being submitted the game over waits, then shows once with the answer`,
`GameViewModelTest › the score is saved even when the view model is cleared while it is being submitted`,
`GameViewModelTest › when the records cannot be written the game over still shows, without a new record`,
`GameViewModelTest › the record shown is the one of the records, its real value, updated when it changes`,
`GameViewModelTest › the record stays shown when the frames move the game`,
`GameViewModelTest › until the records give it the record is unknown, not 0`,
`GameViewModelTest › records that fail while they are read leave the game going, with the last record known`.
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
is multiplied by level number plus one »). Dans le moteur, cela se passe à la fin de
l'animation d'effacement, avec l'événement `LinesCleared` ; le score est un type `Points`, jamais
négatif.

**Tests :** `GameEngineClearTest › when the clear is over the cleared board is shown, lines and level go up, points use the new level`,
`PointsTest › points cannot be negative`, `PointsTest › points add up`,
`LinesTest › the lines cleared at once add to the total`.

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
`LineClearTest › a number of lines cleared at once outside 0 to 4 is refused`,
`LineClearTest › only 4 lines cleared at once make four lines`,
`BoardClearTest › the result gives the number of lines cleared at once`.
