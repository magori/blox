# Règles du Serrurier

Le Serrurier est le second jeu de BloX, une énigme sans chronomètre : poser des pièces dans le
trou d'une serrure jusqu'à la remplir. Chaque pièce posée joue l'accord suivant d'une phrase qui
finit sur la cadence parfaite V–I, et la serrure s'ouvre sur l'accord de tonique. Il mélange les
deux domaines du projet : les pièces et leurs rotations de `core:game`, le principe « composer,
puis évaluer » et les accords de `core:music` ([générateur de musique](generateur-musique.md)).

Origine : prototype « Idée · Le Serrurier » de la maquette (2026-10-25), repris et corrigé par
la conception de l'agent `designer-jeu` et le découpage de l'agent `architecte` (2026-10-26),
décisions du développeur le 2026-10-26. Architecture : [ADR 0025](adr/0025-le-serrurier.md).

> Traçabilité : comme pour le Classique, une règle implémentée reçoit une ligne
> **Tests :** `ClasseDeTest › nom du test` et, si elle se voit à l'écran, **Vues :** `V…`.
> Le domaine est implémenté (P7.3 à P7.8) : grille, génération, partie, indice et accords. Le son et l'écran suivent la phase P7 de [SUIVI.md](../SUIVI.md).

> Coordonnées : comme le plateau du Classique, la grille a son origine en haut à gauche,
> `Position(column, row)`, les lignes croissant vers le bas. **Ordre de lecture** : ligne par
> ligne de haut en bas, et dans une ligne de gauche à droite.

## Vocabulaire

| Terme | Code | Sens |
|---|---|---|
| Serrure | `Padlock` | Une grille, son trou et une solution. Le mot *lock* désigne déjà le verrouillage d'une pièce du Classique (`PieceLocked`, `LockDelay`). |
| Pièce posée | `Piece` (`core:game`) | Un type, une orientation et la position de son pivot : le type existant sert tel quel. |
| Pièces données | — | Les pièces de la solution remises au joueur, identifiées par leur **rang** dans cette liste (deux T sont deux pièces distinctes). |
| Trou | — | Les cases à remplir. |
| Métal | — | Les cases de la grille hors du trou. |

## Grille

- **10 × 10 cases.** L'anneau extérieur (colonnes et lignes 0 et 9) est toujours du métal : le
  trou tient dans les **8 × 8 cases intérieures**. L'écran peut donc ne montrer que ces 8 × 8
  cases, dans un rebord de métal.
  **Tests :** `PadlockTest › the hole stays inside the outer ring`.
- Le trou est **l'union des cases d'une solution** : 3 à 6 pièces posées sans chevauchement,
  chacune touchant une pièce précédente par un bord (pas seulement par un coin). Toute serrure a
  donc au moins une solution ; elle n'est pas forcément unique.
  **Tests :** `PadlockTest › the hole is the union of the cells of the solution`,
  `PadlockTest › two pieces of a solution cannot overlap`,
  `PadlockTest › a padlock holds 3 to 6 pieces`,
  `PadlockTest › a padlock of 3 or 6 pieces is accepted`,
  `PadlockTest › every piece but the first touches an earlier one edge to edge`,
  `PadlockTest › a piece may touch any earlier piece, not only the one before it`.
- Chaque pièce de la solution prend une orientation du Classique pour son type.
  **Tests :** `PadlockTest › a piece of a solution takes a Classic orientation of its type`.
- Pour les tests, une serrure s'écrit en ASCII : `#` pour le métal, le rang de chaque pièce de la
  solution (`1` à `6`, sans trou dans la suite) sur ses cases ; chaque pièce prend l'orientation
  du Classique et le pivot qui couvrent ses cases (les 19 formes sont toutes différentes). Tout
  autre caractère, une grille qui n'est pas de 10 × 10 ou un rang qui n'est pas une pièce est
  refusé.
  **Tests :** `PadlockTest › parse reads the hole and the pieces of the solution`,
  `PadlockTest › parse finds the orientation and the pivot of every Classic piece`,
  `PadlockTest › parse rejects a grid not 10 by 10, an unknown cell, a wrong or missing rank, a rank not a piece`.

## Pièces

- Les sept pièces I, O, T, S, Z, J, L, avec les **orientations du Classique**
  (`ClassicRotationSystem`, 19 formes en tout) et leur **pivot**.
- Une pièce donnée commence dans son orientation d'apparition (`spawnOrientation`) et garde son
  orientation quand on en choisit une autre puis qu'on y revient.
  **Tests :** `PadlockGameTest › each piece keeps its orientation when another one is selected`.
- **Tourner** : un quart de tour dans un sens ou dans l'autre, autour du pivot
  (`RotationSystem.rotate`). O ne change pas.
  **Tests :** `PadlockGameTest › turning turns the selected piece a quarter turn either way`.
- **Poser sur une case** : la pièce devient une `Piece` dont le pivot est la case visée ; ses
  cases (`Piece.cells`) peuvent sortir de la grille, c'est la serrure qui juge. Tourner une pièce
  présentée ne la déplace donc pas sous le doigt (le prototype ancrait la case la plus à gauche
  de la rangée du haut, qui change à chaque rotation).
  **Tests :** `PadlockGameTest › a piece inside the hole is placed, its pivot on the cell aimed at`.

## Générer une serrure : composer, puis évaluer

Même principe que la musique : on compose des candidats au hasard, on élimine, on note, on
garde le meilleur. La génération est une **fonction pure de la graine**
(`PadlockGenerator.generate(pieceCount, seed)`, qui construit son `Random(seed)` elle-même) :
aucun état modifiable de plus dans le domaine ([ADR 0014](adr/0014-tirage-seul-etat-modifiable.md)),
et une même graine donne toujours la même serrure. Comme pour la musique, ce `Random(seed)` est un
maître : chaque candidat est composé avec son propre `Random`, tiré du maître, si bien qu'un
candidat ne dépend pas du nombre de tirages qu'ont consommés les précédents.
**Tests :** `PadlockGenerationTest › the same seed gives the same padlock`,
`PadlockGenerationTest › different seeds give different padlocks`,
`PadlockGenerationTest › a candidate does not depend on the draws of the previous ones`.

1. **Un candidat** : pour chaque pièce, jusqu'à **200 essais** d'un type, d'une orientation (parmi
   toutes celles du type) et d'un pivot tiré dans les 8 × 8 intérieures. Un essai est retenu si
   toutes ses cases sont dans les 8 × 8 intérieures, libres, et (sauf pour la première pièce) si
   l'une touche par un bord une case déjà prise. Si une pièce n'est pas placée en 200 essais, le
   candidat est abandonné.
   **Tests :** `PadlockGeneratorTest › a candidate has the number of pieces asked`,
   `PadlockGeneratorTest › a candidate is a valid padlock whatever the random`,
   `PadlockGeneratorTest › a candidate that cannot place a piece is ruled out`,
   `PadlockGeneratorTest › the same random gives the same candidate`,
   `PadlockGeneratorTest › different randoms give different candidates`,
   `PadlockGeneratorTest › each piece gets its own attempts`,
   `PadlockGeneratorTest › almost every random gives a candidate of 6 pieces`,
   `PadlockGeneratorTest › the first piece of a candidate takes every placement inside the ring`,
   `PadlockGeneratorTest › over many randoms the candidates use every orientation of every type and every inner cell`.
2. **20 candidats** par graine, abandonnés compris ; chacun est noté, le vingtième aussi.
   **Tests :** `PadlockGenerationTest › when no seed gives a padlock within the bound the generator fails, 20 candidates per seed`,
   `PadlockGenerationTest › the twentieth candidate is rated too`.
3. **Élimination** : un trou rectangulaire (il remplit sa boîte englobante) n'est pas une
   serrure.
   **Tests :** `PadlockRatingTest › a rectangular hole is ruled out`.
4. **Note**, entière : `10 × contour + 30 × variété − 3 × boîte`, où le contour compte les côtés
   de case du trou qui donnent sur le métal, la variété le nombre de types différents, la boîte
   l'aire de la boîte englobante du trou (le prototype notait `contour + 3 × variété − 0,3 × boîte`
   plus un bruit aléatoire : même ordre, sans arrondi ni bruit). À égalité, le premier candidat
   composé l'emporte, comme dans le générateur de musique.
   **Tests :** `PadlockRatingTest › the rating is 10 per edge of the outline, 30 per kind of piece, minus 3 per cell of the box`,
   `PadlockGenerationTest › the best candidate is kept, the first one among equals`.
5. Si aucun candidat ne reste, on recommence avec la **graine suivante** (`seed + 1` ;
   après la plus grande graine vient la plus petite), au plus **100 graines** ; au-delà, c'est une erreur de programmation (`IllegalStateException`), pas un
   état de l'écran. Le nombre de graines et la composition d'un candidat sont des paramètres du
   générateur, pour que les tests puissent provoquer cet échec ; les essais sont un paramètre du
   candidat.
   **Tests :** `PadlockGenerationTest › when every candidate is given up the next seed is tried`,
   `PadlockGenerationTest › when every candidate is ruled out the next seed is tried`,
   `PadlockGenerationTest › negative seeds give padlocks and the seed after the largest one is the smallest one`,
   `PadlockGenerationTest › a padlock of fewer than 3 or more than 6 pieces is refused before any candidate is composed`,
   `PadlockGenerationTest › a generation tries one seed at least`,
   `PadlockGenerationTest › when no seed gives a padlock within the bound the generator fails, 20 candidates per seed`,
   `PadlockGenerationTest › every padlock of 500 seeds has the pieces asked and a hole that is no rectangle`.
6. **Pièces données** au joueur : celles de la solution, rangées dans l'ordre de `PieceType`
   (I O T S Z J L), qui ne trahit pas la solution.
   **Tests :** `PadlockTest › the pieces given to the player follow the order I O T S Z J L, not the solution`.

## Niveaux

- Serrure *n* (à partir de 1) : **min(6, 2 + n)** pièces, donc 3, 4, puis 5, puis 6 dès la
  serrure 4.
  La serrure *n* d'une graine est `Padlock.generate(level, seed)`, seule entrée publique du
  générateur.
  **Tests :** `PadlockGenerationTest › padlock n holds min of 6 and 2 plus n pieces`,
  `PadlockGenerationTest › there is no padlock before padlock 1`,
  `PadlockGenerationTest › padlock n of a seed holds min of 6 and 2 plus n pieces`,
  `PadlockGenerationTest › the same level and seed give the same padlock, another seed another one`,
  `PadlockGenerationTest › there is no padlock 0`.
- Chaque serrure a sa graine, tirée par l'application quand elle ouvre la serrure suivante (clé
  de navigation `LocksmithRoute(level, seed)`). La clé numérote les serrures dès 1, et la suivante
  est `route.next(seed)`. La graine n'est pas affichée : la suite de `Random` n'est pas un contrat
  (ADR 0014), une graine partagée demanderait un générateur maison.
  **Tests :** `LocksmithRouteTest › padlocks are numbered from 1`,
  `LocksmithRouteTest › the next padlock is one level up, on the seed it is given`,
  `LocksmithViewModelTest › the screen opens on padlock n of the seed of its route, nothing placed`,
  `LocksmithViewModelTest › the factory builds the padlock and the level of the route the entry hands it`,
  `LocksmithViewModelTest › the factory fails clearly without the route of the entry`.
- Après la mort du processus, la clé restaurée régénère la même serrure depuis sa graine, **sans
  pièce posée**.
  **Tests :** `LocksmithRouteTest › a route restored after the death of the process gives the same padlock, nothing placed`.
- La cartouche « Serrurier » ouvre la serrure 1 sur une graine tirée au hasard ; « Serrure
  suivante » met à sa place la serrure d'un niveau de plus, sur une nouvelle graine, et un second
  toucher ne saute pas deux serrures.
  **Tests :** `OpenCartridgeTest › the Locksmith cartridge opens padlock 1 over the cartridges, on a seed drawn at random`,
  `OpenCartridgeTest › a second tap on the Locksmith cartridge does not stack a second padlock`,
  `NextPadlockTest › the next padlock takes the place of the open one, one level up, on a new seed`,
  `NextPadlockTest › a second tap on next padlock does not move on twice`,
  `BloxNavDisplayTest › the Locksmith cartridge opens padlock 1`,
  `BloxNavDisplayTest › on the Locksmith, the system back goes back to the cartridges, and then leaves`,
  `BloxNavDisplayTest › a restored stack with a padlock on top shows that padlock again, nothing placed`.
  **Vues :** V26, V35.
- Les barres système suivent la variante Couleurs du Serrurier (jour ou nuit selon le téléphone),
  quel que soit le thème choisi pour le Classique ; ailleurs, le thème choisi.
  **Tests :** `SystemBarsTest › the bars of the locksmith follow its colorful variant, whatever the theme chosen`,
  `SystemBarsTest › the bars of every other screen follow the theme chosen`,
  `SystemBarsAppearanceTest › at night in the Original theme, the bars turn light under the Locksmith, and dark again once it is left`.
  **Vues :** V26, V35.
- Le niveau n'est pas enregistré : la cartouche ouvre toujours la serrure 1. Reprendre à la
  dernière serrure demandera une préférence (point ouvert).

## Partie

Une partie est un `PadlockGame` immuable (`start`, `select`, `turn`, `place`, `takeBack`,
`restart`) ; chaque action rend la partie suivante et ses événements (`PadlockUpdate`).

- **Choisir** une pièce donnée non posée, par son rang ; à l'ouverture et après chaque pose
  acceptée, la première pièce non posée est choisie d'office.
  **Tests :** `PadlockGameTest › a new game selects the first given piece in its spawn orientation, nothing placed`,
  `PadlockGameTest › the first free piece is selected after a placement`,
  `PadlockGameTest › selecting a placed piece or taking back a free one changes nothing`,
  `PadlockGameTest › a rank that is not given is a programming error`.
- **Poser** la pièce choisie, dans son orientation, avec son pivot sur une case :
  - **acceptée** si toutes ses cases sont dans le trou et libres : événement `PiecePlaced` ;
  - **refusée** sinon, avec la raison : une case sur le métal ou hors de la grille
    (`Refusal.OUT_OF_HOLE`), une case sur une pièce posée (`Refusal.OVERLAP`). Quand les deux s'appliquent,
    `OUT_OF_HOLE` l'emporte. Événement `PlacementRefused` ; la partie ne change pas, sauf le
    compteur des refus.
  **Tests :** `PadlockGameTest › a piece inside the hole is placed, its pivot on the cell aimed at`,
  `PadlockGameTest › a piece off the grid is refused as out of the hole`,
  `PadlockGameTest › a piece over the metal is refused as out of the hole and only the refusals count it`,
  `PadlockGameTest › a piece over a placed one is refused as an overlap`,
  `PadlockGameTest › a piece both over the metal and over a placed one is refused as out of the hole`.
- **Reprendre** une pièce posée : elle quitte la grille et redevient la pièce choisie, avec son
  orientation (`PieceTakenBack`).
  **Tests :** `PadlockGameTest › a placed piece is taken back, selected in its orientation, and the moves are kept`.
- **Serrure ouverte** quand toutes les pièces sont posées : la dernière pose émet `PiecePlaced`
  puis `PadlockOpened`. Toute pose acceptée étant juste, il n'y a rien d'autre à vérifier. Ensuite,
  choisir, tourner, poser, reprendre et demander un indice sont ignorés, sans événement ; seul
  Recommencer reste possible.
  **Tests :** `PadlockGameTest › placing every piece opens the padlock, the last placement first`,
  `PadlockGameTest › once the padlock is open nothing but restart is taken`,
  `PadlockHintTest › once the padlock is open no hint is given`.
- **Recommencer** : même serrure, aucune pièce posée, compteurs à zéro.
  **Tests :** `PadlockGameTest › restarting keeps the padlock, clears the pieces placed and the counters`.
- La partie garde l'**ordre des poses** (pour l'indice).
  **Tests :** `PadlockGameTest › the order of the placements is kept, a piece taken back leaves it`,
  `PadlockGameTest › a piece taken back and placed again is the last placed`,
  `PadlockGameTest › two games whose pieces were placed in a different order are not equal`.
- La partie dit quelle pièce posée couvre une case (`rankAt`) et où irait la pièce choisie, pivot sur  une case (`selectedAt`), comme `place` la poserait ; l'écran en tire son aperçu.  **Tests :** `PadlockGameTest › a cell tells the rank of the placed piece that covers it, if any`,  `PadlockGameTest › the selected piece put down on a cell is its type, in its orientation, its pivot there`,  `PadlockGameTest › an open padlock has no selected piece to put down`.
- **Compteurs**, séparés :
  - **coups** : les poses acceptées, y compris celles reprises ensuite ;
  - **refus** : les poses refusées ;
  - **indices** : les indices demandés.
  **Tests :** `PadlockGameTest › a piece over the metal is refused as out of the hole and only the refusals count it`,
  `PadlockGameTest › a placed piece is taken back, selected in its orientation, and the moves are kept`,
  `PadlockHintTest › the hint shows where a free piece fits and counts a hint`.

## Indice

- Un solveur de couverture exacte cherche comment remplir le trou restant avec les pièces non
  posées (au plus 6 pièces et 24 cases : un retour arrière suffit). Ordre de recherche, pour que
  l'indice soit toujours le même : la première case libre du trou dans l'ordre de lecture ; les
  pièces non posées par rang (de plusieurs pièces d'un même type, seule la première est essayée,
  les autres donneraient les mêmes couvertures) ; pour chacune, ses orientations dans l'ordre de
  `orientations(type)`, chacune avec le pivot qui fait de cette case la première de la pièce dans
  l'ordre de lecture (aucun autre ne peut convenir : c'est la première case libre).
  **Tests :** `PadlockHintTest › the hint shows where a free piece fits and counts a hint`,
  `PadlockHintTest › following the hints opens every padlock of 200 seeds`,
  `PadlockHintTest › the hint tries the orientations of a piece in their order`,
  `PadlockHintTest › the hint covers the first free cell in reading order`,
  `PadlockHintTest › a hint is quick once a piece is placed anywhere in a padlock of 6 pieces`.
- S'il trouve : l'indice désigne la pièce qui couvre la première case libre dans la première
  couverture trouvée : **son rang, son orientation et son pivot**. Il ne propose jamais une pièce
  déjà posée (défaut du prototype), et trouve un chemin même quand le joueur s'est écarté de la
  solution d'origine. C'est un `Hint.Fit`.
  **Tests :** `PadlockHintTest › the hint never names a piece already placed`,
  `PadlockHintTest › the hint finds a way even when the player left the solution`.
- Sinon : l'indice désigne **une pièce posée qui bloque**, la plus récemment posée dont le retrait
  seul rend la serrure de nouveau soluble ; si aucun retrait seul ne suffit, la dernière pièce
  posée. C'est un `Hint.Blocking`.
  **Tests :** `PadlockHintTest › the hint names the piece that blocks when no way is left`,
  `PadlockHintTest › the hint names the latest piece whose removal alone leaves a way`,
  `PadlockHintTest › the hint names the latest of several pieces whose removal alone leaves a way`,
  `PadlockHintTest › when no single piece unblocks the padlock the hint names the last piece placed`.
- Chaque indice compte dans le compteur des indices (`PadlockGame.hints`) et émet `HintGiven` ; une
  fois la serrure ouverte, aucun indice n'est donné.
  **Tests :** `PadlockHintTest › the hint shows where a free piece fits and counts a hint`,
  `PadlockHintTest › once the padlock is open no hint is given`.

## Accords

Do majeur, tonique C4, un accord par pièce : après une pose acceptée, on joue l'accord de rang *n*, *n* étant
le nombre de pièces sur la grille. La suite d'accords dépend du nombre de pièces de la serrure et
finit toujours par la **cadence parfaite** V–I : la dernière pose résout sur I. Les accords
entendus sont `PadlockGame.chords`, dans la tonalité `PadlockChords.KEY`, sur la suite
`PadlockGame.progression` ; une reprise retire le
dernier.
**Tests :** `PadlockGameTest › the chords heard are one per piece on the grid, from C4 major, the last placement resolving on I`,
`PadlockGameTest › a game plays the chords of its number of pieces`,
`PadlockGameTest › a piece taken back takes its chord back`.
Tous les accords de la serrure, entendus ou non, sont `PadlockGame.progressionChords` ; `chords` en est le début.
**Tests :** `PadlockGameTest › the chords of a padlock are those of its progression, in C4 major, all heard once it is open`.

| Pièces | Accords |
|---|---|
| 3 | IV – V – I |
| 4 | I – IV – V – I |
| 5 | I – vi – IV – V – I |
| 6 | I – vi – ii – IV – V – I |

Ce sont des `Progression` de `core:music` (`PadlockChords.progression(pieceCount)`), sans type à part.
**Tests :** `PadlockChordsTest › a padlock plays one chord per piece and ends on the perfect cadence V then I`,
`PadlockChordsTest › the chords of a padlock follow the table of the rules`,
`PadlockChordsTest › there are no chords for a padlock of fewer than 3 or more than 6 pieces`.

**Son** : arpège de l'accord et basse deux octaves plus bas, joués ensemble ; fanfare de
serrure ouverte ; son de refus (`LocksmithSounds`, valeurs dans [musique.md](musique.md#effets-sonores)). Ce
sont des effets sonores superposés (`SoundEffect`), joués par
`SoundEffectPlayer` et soumis au réglage des effets sonores. Une reprise ne coupe aucun son : seule
la pilule de l'accord s'éteint à l'écran. Pas de vibration pour l'instant.

- Chaque événement de la dernière action donne ses sons (`soundCuesFor`, fonction pure de `feature:locksmith`) :
  une pose acceptée, l'arpège et la basse de son accord (celui de rang *n*) ; la serrure ouverte, en plus, la
  fanfare, **après l'arpège final** (sa durée plus 10 ms, soit 450 ms aujourd'hui) ; un refus, le son de refus ;
  une reprise et un indice, rien. Choisir, tourner, viser et recommencer ne jouent rien.
  **Tests :** `PadlockSoundCuesTest › a placement plays the arpeggio and the bass of its chord`,
  `PadlockSoundCuesTest › the second placement plays the chord of IV`,
  `PadlockSoundCuesTest › the last placement adds the opening flourish after the final chord`,
  `PadlockSoundCuesTest › the flourish waits for the final chord to end`,
  `PadlockSoundCuesTest › a refusal plays the refusal`,
  `PadlockSoundCuesTest › taking back and a hint play nothing`,
  `LocksmithSoundTest › a placement plays the arpeggio and the bass of its chord`,
  `LocksmithSoundTest › the second placement plays the chord of IV`,
  `LocksmithSoundTest › the last placement adds the opening flourish after the final chord`,
  `LocksmithSoundTest › a refusal plays the refusal`,
  `LocksmithSoundTest › taking back, a hint and choosing play nothing`.
- Le réglage des effets sonores est lu une fois, à l'ouverture de l'écran, comme au Classique : effets coupés,
  ni son ni préchauffage.
  **Tests :** `LocksmithSoundTest › no sound when the sound effects are off`,
  `LocksmithSoundTest › nothing is warmed up when the sound effects are off`.
- À l'ouverture de l'écran, les effets de la serrure sont préchauffés, chacun une fois : les accords de sa
  suite, la fanfare et le refus.
  **Tests :** `PadlockSoundCuesTest › the effects warmed up are those of every chord of the padlock, the flourish and the refusal, once each`,
  `LocksmithSoundTest › the effects of the padlock are warmed up when its screen opens`.
- Une fanfare encore en attente meurt avec l'écran quitté (retour, ou « Serrure suivante » qui remplace
  l'entrée) : elle ne sonne pas sur la serrure suivante. Une action à l'écran, Recommencer compris, ne coupe
  aucun son.
  **Tests :** `LocksmithSoundTest › the opening flourish still waiting dies with the screen left`.
- Point ouvert : les pistes d'effets préchauffées ne sont jamais libérées (jusqu'à 12 de plus que le Classique,
  environ 1 Mo) ; à mesurer sur un téléphone (`dumpsys media.audio_flinger`, P7.20).

## Écran

**Thème : Couleurs v2 seulement** (décision du développeur, 2026-10-26). L'écran du Serrurier est
toujours dessiné en Couleurs, de jour ou de nuit selon le téléphone, quel que soit le thème choisi
pour le Classique ; il n'a pas d'aspect Original.

Conception de `designer-jeu` (2026-10-26), en Couleurs v2, téléphone debout d'abord ; vue V35
de la [maquette](maquette.md) (à dessiner). De haut en bas : barre (retour, « SERRURE 04 »,
coups, cadenas d'état), bande des accords (une pilule par chiffre romain de `progression`,
remplie pour chaque accord de `chords` ; la cadence V–I est la paire des deux dernières),
serrure (8 × 8 cases de 42 dp au plus, selon la largeur, dans un rebord de métal), message,
Indice et Recommencer, tuiles des pièces données, puis tourner à gauche, **Poser**, tourner à
droite.

- **Cases de la serrure** (`core:board`, `LockCell`, `LockCellCanvas`, en Couleurs seulement) : le
  métal (`lock-metal`, à 3:1 au moins du trou et de la page, un rivet `lock-rivet` plus foncé au
  centre) ; le trou (case vide du plateau, enfoncée par `lock-hole-inset`) ; la pièce posée (sa
  bulle du plateau) ; l'aperçu qui tient (plat, cerné d'un contour `ink`) ; l'aperçu refusé (une
  croix sur chaque case fautive) ; l'indice (teinte `lock-hint` et un point `ink`, sur une case
  libre ou sur la pièce posée qui bloque). Aucun état ne repose sur la seule couleur.
  **Tests :** `LockCellStylesTest › a metal cell is a square of lock metal with a rivet in its centre`,
  `LockCellStylesTest › a rivet is darker than the metal, so that it does not read as a small hole`,
  `LockCellStylesTest › the metal stands out from the hole and from the page by 3 to 1 at least`,
  `LockCellStylesTest › a hole is an empty cell of the board sunk by the hole inset`,
  `LockCellStylesTest › a placed piece has the bubble it has on the board`,
  `LockCellStylesTest › a preview that fits differs from its placed piece by more than its colour`,
  `LockCellStylesTest › a preview that does not fit is crossed out, so that the refusal does not rest on colour`,
  `LockCellStylesTest › the cross of a refused preview reads on every piece, by its ink or by its halo`,
  `LockCellStylesTest › a hint reads apart from the hole and from every preview without colour`,
  `LockCellStylesTest › a placed piece named by the hint bears the hint mark`,
  `LockCellStylesTest › the padlock has no style in the Original theme, the Locksmith is drawn in the Colorful theme only`,
  `LockTokensTest › each colour token of the padlock is its colour in the code, day and night`,
  `LockTokensTest › the hole inset of tokens json is the inner shadow of a hole, day and night`,
  `LockTokensTest › every lock token of tokens json is coded in core board`.
  Point ouvert : les pièces posées se détachent du métal à moins de 3:1 de jour (de 1,7:1 à
  2,8:1), par leur forme et leur relief plus que par la couleur ; à juger par `designer-jeu`.
- Les tuiles des pièces données dessinent la pièce ramenée en haut à gauche
  (`Orientation.broughtToOrigin()`, comme la pièce suivante du Classique).
  **Tests :** `OrientationTest › an orientation brought to the origin touches row 0 and column 0`,
  `OrientationTest › bringing an orientation to the origin keeps its shape`.
- **Pose en deux temps** : toucher une case présente la pièce choisie en **aperçu**, pivot sous
  le doigt ; on la tourne, on la déplace d'un autre toucher, puis **Poser** (ou un second
  toucher sur l'aperçu) la pose. Les cases fautives de l'aperçu sont barrées : l'aperçu est un
  état de l'écran, le domaine ne juge que la pose. Toucher une pièce posée, ou sa tuile, la
  reprend : elle redevient l'aperçu, là où elle était. Un refus garde l'aperçu en place ; tourner,
  choisir ou déplacer l'aperçu oublie la raison du refus. Un toucher sur l'aperçu le pose, même au-dessus
  d'une pièce posée (refusé : chevauchement) ; une fois la serrure ouverte, le toucher ne fait rien.
  Indice est grisé tant qu'un indice est affiché. L'anneau de métal ne se touche pas : le pivot
  reste toujours dans les 8 × 8.
  **Tests :** `LocksmithTapTest › touching a cell aims the selected piece there, its pivot on that cell, nothing placed`,
  `LocksmithTapTest › touching a cell outside the preview moves it there`,
  `LocksmithTapTest › a second touch on the preview places it and the preview is gone`,
  `LocksmithTapTest › a refused second touch keeps the preview where it is and tells why`,
  `LocksmithTapTest › a refusal keeps the preview, the place button tells the reason`,
  `LocksmithTapTest › the place button places the preview`,
  `LocksmithTapTest › the place button does nothing without a preview`,
  `LocksmithTapTest › touching a placed piece takes it back as a preview where it lay`,
  `LocksmithTapTest › taking a placed piece back by its tile shows it as a preview where it lay`,
  `LocksmithTapTest › touching the preview over a placed piece is refused as an overlap, nothing taken back`,
  `LocksmithTapTest › moving or turning the preview after a refusal forgets the refusal, without counting a move`,
  `LocksmithTapTest › once the padlock is open a touch changes nothing`,
  `LocksmithTapTest › touching the ring of metal aims nothing, the pivot stays inside the 8 by 8`,
  `LocksmithTapTest › restart clears the preview`.
- Ce que montre la grille : les 8 × 8 cases intérieures (le trou, le métal, les pièces posées,
  l'indice sur ses cases libres ou sur la pièce qui bloque, puis l'aperçu par-dessus, chaque case
  fautive barrée), dans un rebord de métal d'un quart de case (`lockMetal`, le métal des cases). Une
  case de l'aperçu sur l'anneau est dessinée barrée, en case entière qui déborde le rebord ; celles
  hors de la grille 10 × 10 ne sont pas dessinées.
  **Tests :** `LockCellsTest › the screen draws the 8 by 8 cells inside the ring, the hole as holes and the rest as metal`,
  `LockCellStylesTest › the rim of the padlock, drawn by the screen, is the metal of its cells`,
  `LockCellsTest › a preview cell on the ring of metal is drawn crossed out`,
  `LockCellsTest › a placed piece shows its type on its cells`,
  `LockCellsTest › the preview shows the selected piece, each cell off the hole or over a placed piece crossed out`,
  `LockCellsTest › a preview over a placed piece crosses out the cells they share`,
  `LockCellsTest › a hint marks the free cells where its piece fits`,
  `LockCellsTest › a hint naming a placed piece marks that piece`,
  `LockCellsTest › the preview is drawn over the hint`.
- Le message sous la serrure dit, dans cet ordre : la serrure ouverte et la cadence qui a fermé la
  phrase (« La cadence V–I a fermé la phrase. », lue « cinq majeur, un majeur »), la raison d'un refus
  sous son numéro (« Refus 2 : … », pour que TalkBack redise un deuxième refus de suite), l'orientation
  de la pièce qu'on vient de tourner (« Pièce T, un quart de tour à droite. », comptée en quarts de tour
  depuis sa forme de départ, jusqu'à l'action suivante), ce que marque l'indice en nommant sa pièce
  (« L'indice marque où tient la pièce L. », ou la pièce posée qui bloque), puis comment jouer, avec ou
  sans aperçu. Le message est la seule zone annoncée (polie) tant que la serrure est fermée, et ne
  l'est plus une fois ouverte : le titre de la carte prend alors le focus. Chaque case se lit
  « Colonne c, ligne l : trou, métal, pièce… », une case de l'indice « indice : la pièce L tient ici » ;
  la grille se lit comme une collection de 8 rangées et 8 colonnes, l'anneau laissé de côté. Le bandeau
  des accords se lit par degré et qualité (« cinq majeur »), jamais par son chiffre romain, que TalkBack
  lirait comme un mot : « Accords entendus : … À venir : … ».
  **Tests :** `LocksmithMessageTest › without a preview the message says to touch the hole`,
  `LocksmithMessageTest › with a preview the message says to touch it again or to place it`,
  `LocksmithMessageTest › a refusal tells its reason`,
  `LocksmithMessageTest › a refusal is told with its number, so that TalkBack tells a second refusal in a row again`,
  `LocksmithMessageTest › a refusal is told before the hint and the preview`,
  `LocksmithMessageTest › a turn tells the orientation of the chosen piece, before the hint and the preview`,
  `LocksmithMessageTest › a hint is told before the preview`,
  `LocksmithMessageTest › a hint tells where a piece fits, or that a placed piece blocks`,
  `LocksmithMessageTest › a hint names its piece, the one that fits or the placed one that blocks`,
  `LocksmithMessageTest › an open padlock tells the cadence that closed the phrase, whatever came before`,
  `LocksmithViewModelTest › a turn is told by the screen until the next action`,
  `LocksmithReadingTest › the orientation of a piece is read in quarter turns from where it started, clockwise`,
  `LocksmithReadingTest › a piece of two orientations is upright or turned a quarter, either way, and an O always upright`,
  `LocksmithReadingTest › the band of the chords reads those heard, then those to come`,
  `LocksmithReadingTest › a chord is read by its degree and its quality, not by its numeral, which TalkBack would read as a word`,
  `LocksmithReadingTest › the band says when no chord is heard yet, and when they all are`,
  `LocksmithReadingTest › the cadence that closed the phrase is read by its two chords, V then I`,
  `LocksmithScreenTest › the grid is read as a collection of 8 rows and 8 columns, each cell at its place, the ring left out`,
  `LocksmithScreenTest › turning the chosen piece tells its new orientation by the message, its tile keeping it to be read`,
  `LocksmithScreenTest › after a placement, and after a choice, the message is the one region TalkBack announces`,
  `LocksmithScreenTest › once open, the message reads the cadence by its chords, as the band does`,
  `LocksmithScreenTest › the band of the chords reads the chords heard and those to come, by their degree`,
  `LocksmithScreenTest › a second refusal in a row is told again, under its number`,
  `LocksmithScreenTest › the piece named by the hint is read in the message and on the cells it marks`,
  `LocksmithScreenTest › the message is a polite live region while the padlock is closed, and no longer once it is open`.
  **Vues :** V35 (captures `LocksmithRefused`, `LocksmithRefusedBottom`, `LocksmithBlocking`, `LocksmithOpen`
  et `LocksmithOpenSmall`, chacune `Day` et `Night`, de `LocksmithScreenshots.kt`).
- Tailles (`padlockSizes`, fonction pure) : case de 42 dp au plus, sinon la largeur divisée par 8,5, en pixels
  entiers ; rebord d'un quart de case arrondi ; huit cases et deux rebords tiennent toujours dans la largeur.
  Tant que la serrure est fermée, la place du débord d'une case de l'aperçu (case moins rebord, moins l'écart
  avec les voisins) est gardée au-dessus et au-dessous de la plaque, pour que la croix ne couvre ni la
  cadence ni le message et que la grille ne saute pas ; une fois ouverte, plus d'aperçu, plus de place gardée.
  **Tests :** `PadlockSizesTest › the cells take their largest size when the width allows it`,
  `PadlockSizesTest › on a narrow width, eight cells and the rim on both sides fit, in a whole number of pixels`,
  `PadlockSizesTest › when the rim rounds up, eight cells and the rim still fit the width`,
  `PadlockSizesTest › the rim is a quarter of a cell, rounded to a whole number of pixels`,
  `PadlockSizesTest › a cell of the preview on the ring overflows the rim by a cell less the rim, the room kept above and below`,
  `PadlockSizesTest › while the padlock is closed, the room kept is the overflow less the gap to the neighbours`,
  `PadlockSizesTest › once the padlock is open, no preview is left and no room is kept`.
- Mise en page : une colonne qui défile (barre, accords, grille, message, Indice et Recommencer, tuiles), sous
  elle les commandes ancrées en bas (tourner, **Poser**, tourner), zone plafonnée à la moitié de la hauteur.
  La barre a un bouton retour et un cadenas d'état (24 dp, anse levée une fois ouverte), décoratif pour
  TalkBack : c'est le titre de la barre qui dit l'état, « Serrure 03, fermée » ou « Serrure 03, ouverte ». Les
  deux dernières pilules des accords portent une accolade et le mot « cadence ». La tuile choisie et son anneau
  descendent ensemble quand on appuie (ce qu'un écran dessine autour d'un bouton suit le bouton).
  **Tests :** `LocksmithMessageTest › the title of the bar tells the padlock closed until it opens`,
  `LocksmithScreenTest › the padlock of the bar has no node, the title of the bar tells the padlock closed, then open`,
  `LocksmithScreenTest › the back button of the bar goes back`,
  `BloxNavDisplayTest › on the Locksmith, the back button of the bar goes back to the cartridges`,
  `BubbleButtonTest › what a screen draws around a button, as the ring of a chosen tile, goes down with it`.
- Aperçu sur l'anneau en colonne 0 ou 9 : quand la place manque au-delà du rebord, la case qui déborde est
  rognée à 4 dp du bord de l'écran, sa croix entière dedans ; avec assez de place, elle est entière. La case
  rognée garde sa hauteur, son joint et ses coins. Exception à la marge des écrans du design system, notée
  dans son README.
  **Tests :** `PadlockSizesTest › at 390 dp, a cell of the preview on the ring at the side is cut 4 dp from the edge, its cross whole within`,
  `PadlockSizesTest › with room enough beyond the rim, a cell of the preview on the ring at the side is whole`,
  `PadlockSizesTest › a cell of the preview on the ring at the side never passes the edge of the screen less the margin`,
  `CellLayoutTest › a cell cut to a narrower width keeps its height, its joint and its corners, its fill as narrow`.
- Retours animés ([ADR 0024](adr/0024-retours-de-jeu-et-effets-reduits.md)), en ticks (`LocksmithFeedback`,
  pur), dessinés sans recomposer la grille :
  - **pose** : les cases de la pièce s'allument 8 ticks, de moins en moins ; en Effets réduits, rien, comme le
    verrouillage du Classique : la pilule de l'accord suffit ;
  - **refus** : la serrure tremble de côté 18 ticks, de moins en moins, dès sa deuxième image, puis revient en
    place ; seules les cases barrées (anneau compris) sont concernées ; en Effets réduits, pas de tremblement,
    l'aperçu s'allume et s'éteint en 8 ticks. Deux refus de suite sont deux retours, chacun rejoué depuis le
    début ; un écran affiché sur un refus déjà fait (retour à l'écran) ne tremble pas ;
  - **serrure ouverte** : le trou s'allume rangée par rangée depuis le bas, 2 ticks entre deux rangées ; en
    Effets réduits, tout le trou d'un coup, éteint en 8 ticks ;
  - **indice** : ses cases (ou la pièce posée qui bloque) gonflent d'une seule lueur de 30 ticks, une fois,
    pour que rien ne clignote ; en Effets réduits, allumées d'un coup, éteintes en 8 ticks. L'indice reste
    marqué jusqu'à la pose suivante ;
  - reprendre, choisir, tourner ou viser ne donnent aucun retour.
  L'écran garde les Effets réduits de l'application (le `BloxTheme` Couleurs les perdait, corrigé).
  **Tests :** `LocksmithFeedbackTest › a placement lights the cells of its piece for 8 ticks, dimmer at each tick`,
  `LocksmithFeedbackTest › in reduced effects a placement lights nothing, as a lock of the Classic, the pill of its chord telling it`,
  `LocksmithFeedbackTest › a refusal shakes the padlock side to side for 18 ticks from its first, less and less, then puts it back`,
  `LocksmithFeedbackTest › a refusal concerns the cells it crosses out alone, those on the ring included`,
  `LocksmithFeedbackTest › in reduced effects a refusal does not shake, its preview lit and fading in 8 ticks`,
  `LocksmithFeedbackTest › an open padlock lights its hole row by row from the bottom up, each row 2 ticks after the one below`,
  `LocksmithFeedbackTest › in reduced effects an open padlock lights its whole hole at once, fading in 8 ticks`,
  `LocksmithFeedbackTest › a hint lights the cells it marks in one swell of 30 ticks, once, so that nothing flashes`,
  `LocksmithFeedbackTest › in reduced effects a hint lights its cells at once, fading in 8 ticks`,
  `LocksmithFeedbackTest › a hint naming a placed piece that blocks lights that piece`,
  `LocksmithFeedbackTest › taking a piece back, choosing, turning or aiming shows no feedback`,
  `LocksmithFeedbackTest › two refusals in a row are two feedbacks, each one played from its start`,
  `LocksmithScreenTest › the screen keeps the reduced effects of the application: a button pressed stays in place`,
  `LocksmithScreenTest › after a refusal, the padlock is shaken from its second frame, and back in place 18 ticks later`,
  `LocksmithScreenTest › in reduced effects, a refusal never shakes the padlock`,
  `LocksmithScreenTest › a screen shown with a refusal already made is not shaken`,
  `LocksmithScreenTest › the grid does not recompose while the padlock shakes`,
  `LocksmithScreenTest › with its view model: a placement refused, then accepted, then taken back`.
  **Vues :** V35 (captures `LocksmithPlacedFeedback`, `LocksmithRefusedShake`, `LocksmithRefusedReduced`,
  `LocksmithRefusedLeft`, `LocksmithRefusedLeftShake`, `LocksmithRefusedLeftReduced`, `LocksmithRefusedRight`,
  `LocksmithHintSwell`, `LocksmithOpenWave` et `LocksmithOpenReduced`, en `Day`).
- Les types de `core:locksmith` que reçoit l'écran sont immuables et déclarés stables dans
  `compose-stability.conf`.
  **Tests :** `LocksmithImmutabilityTest › every instance field of the classes the screen receives is final`.
- **Serrure ouverte** : une carte remplace les commandes du bas ; Indice, Recommencer et les tuiles
  disparaissent, le message reste. Elle montre trois compteurs (coups, refus, indices, lus « Coups : n »), une
  phrase sur la cadence, **Serrure suivante** (bouton principal) et **Rejouer cette serrure** (secondaire), et
  défile si la place manque.
  **Tests :** `OpenCardTest › there is no card while the padlock is closed`,
  `OpenCardTest › an open padlock shows its moves, its refusals and its hints, each one its own counter`,
  `LocksmithScreenTest › on the card of an open padlock, Serrure suivante goes to the next padlock`,
  `LocksmithScreenTest › on the card of an open padlock, Rejouer cette serrure restarts the padlock`,
  `LocksmithScreenTest › once the padlock is open, neither Place, nor Hint, nor the tiles are left`,
  `LocksmithScreenTest › while the padlock is closed, the card is not shown`.
  TalkBack n'entend « Serrure ouverte ! » qu'une fois : le titre de la carte est un en-tête, seul nœud à le
  dire, et prend le focus à l'ouverture ; après **Rejouer cette serrure**, le focus revient au message. Rien ne
  prend le focus tant que l'état ne change pas (retour à l'écran compris).
  **Tests :** `LocksmithFocusTest › the title of the card takes the focus when the padlock opens`,
  `LocksmithFocusTest › replaying an open padlock gives the focus to the message`,
  `LocksmithFocusTest › nothing takes the focus while the padlock stays closed or stays open, as on coming back to the screen`,
  `LocksmithScreenTest › on opening, « Serrure ouverte ! » is a heading and takes the focus`,
  `LocksmithScreenTest › once open, a single node says « Serrure ouverte ! »`,
  `LocksmithScreenTest › after Rejouer cette serrure, the message takes the focus`.
- Pas de bouton pour passer une serrure : l'indice débloque.
- L'état de l'écran garde les événements de la dernière action (retours et sons), le numéro de
  l'action (deux refus à la suite restent deux retours) et l'indice affiché, jusqu'à la pose
  acceptée suivante, une reprise ou Recommencer.
  **Tests :** `LocksmithViewModelTest › a placement gives the next game and its events`,
  `LocksmithViewModelTest › a refused placement tells its reason`,
  `LocksmithViewModelTest › two refusals in a row are two moves the screen can tell apart`,
  `LocksmithViewModelTest › a hint shows where a piece fits until the next placement`,
  `LocksmithViewModelTest › taking a piece back gives the next game and its event`,
  `LocksmithViewModelTest › restart keeps the padlock and starts it over, hint and events cleared`.
