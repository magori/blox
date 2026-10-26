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
  **Tests :** `PadlockGenerationTest › padlock n holds min of 6 and 2 plus n pieces`,
  `PadlockGenerationTest › there is no padlock before padlock 1`.
- Chaque serrure a sa graine, tirée par l'application quand elle ouvre la serrure suivante (clé
  de navigation `LocksmithRoute(level, seed)`). La graine n'est pas affichée : la suite de
  `Random` n'est pas un contrat (ADR 0014), une graine partagée demanderait un générateur maison.
- Après la mort du processus, la clé restaurée régénère la même serrure depuis sa graine, **sans
  pièce posée**.
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
résolution ; son de refus. Ce sont des effets sonores superposés (`SoundEffect`), joués par
`SoundEffectPlayer` et soumis au réglage des effets sonores. Une reprise ne coupe aucun son : seule
la pilule de l'accord s'éteint à l'écran. Pas de vibration pour l'instant.

## Écran

Conception de `designer-jeu` (2026-10-26), en Couleurs v2, téléphone debout d'abord ; vue V35
de la [maquette](maquette.md) (à dessiner). De haut en bas : barre (retour, « SERRURE 04 »,
coups, cadenas d'état), bande des accords (une pilule par chiffre romain de `progression`,
remplie pour chaque accord de `chords` ; la cadence V–I est la paire des deux dernières),
serrure (8 × 8 cases de 42 dp au plus, selon la largeur, dans un rebord de métal), message,
Indice et Recommencer, tuiles des pièces données, puis tourner à gauche, **Poser**, tourner à
droite.

- **Pose en deux temps** : toucher une case présente la pièce choisie en **aperçu**, pivot sous
  le doigt ; on la tourne, on la déplace d'un autre toucher, puis **Poser** (ou un second
  toucher sur l'aperçu) la pose. Les cases fautives de l'aperçu sont barrées : l'aperçu est un
  état de l'écran, le domaine ne juge que la pose.
- Retours de la pose, du refus, de la serrure ouverte et de l'indice, chacun avec sa version
  « Effets réduits » ([ADR 0024](adr/0024-retours-de-jeu-et-effets-reduits.md)). L'indice reste
  marqué jusqu'à la pose suivante.
- Serrure ouverte : carte avec les compteurs, **Serrure suivante** et **Rejouer cette serrure**.
- Pas de bouton pour passer une serrure : l'indice débloque.
