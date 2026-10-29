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
- Le trou est **l'union des cases d'une solution** : 3 à 8 pièces posées sans chevauchement,
  chacune touchant une pièce précédente par un bord (pas seulement par un coin). Toute serrure a
  donc au moins une solution ; elle n'est pas forcément unique.
  **Tests :** `PadlockTest › the hole is the union of the cells of the solution`,
  `PadlockTest › two pieces of a solution cannot overlap`,
  `PadlockTest › a padlock holds 3 to 8 pieces`,
  `PadlockTest › a padlock of 3 or 8 pieces is accepted`,
  `PadlockTest › every piece but the first touches an earlier one edge to edge`,
  `PadlockTest › a piece may touch any earlier piece, not only the one before it`.
- Chaque pièce de la solution prend une orientation du Classique pour son type.
  **Tests :** `PadlockTest › a piece of a solution takes a Classic orientation of its type`.
- Pour les tests, une serrure s'écrit en ASCII : `#` pour le métal, le rang de chaque pièce de la
  solution (`1` à `8`, sans trou dans la suite) sur ses cases ; chaque pièce prend l'orientation
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
garde le meilleur. La serrure *n* (à partir de 1) est `Padlock.ofLevel(n)`, une **fonction pure du
niveau** : aucun état modifiable de plus dans le domaine
([ADR 0014](adr/0014-tirage-seul-etat-modifiable.md)), et la serrure *n* est la même pour tous et
pour toujours. Le hasard vient de `StableRandom` (`core:game`), dont la suite est un contrat, semé
par *n* ; comme pour la musique, c'est un maître : chaque candidat est composé avec son propre
`StableRandom`, semé par le nombre suivant du maître, si bien qu'un candidat ne dépend pas du nombre
de tirages qu'ont consommés les précédents. Le nombre de pièces, leurs types, la forme du trou et
la préférence de solutions viennent du **profil** de la serrure (`PadlockProfile`, section
[Paliers de difficulté](#paliers-de-difficulté)).
**Tests :** `PadlockLevelTest › the candidates of padlock n draw on StableRandom, seeded by the numbers of StableRandom of n`.

1. **Un candidat** : pour chaque pièce, jusqu'à **200 essais** d'un type (parmi ceux du profil),
   d'une orientation (parmi toutes celles du type) et d'un pivot tiré dans les 8 × 8 intérieures.
   Un essai est retenu si toutes ses cases sont dans les 8 × 8 intérieures, libres, et (sauf pour
   la première pièce) si l'une touche par un bord une case déjà prise. Si une pièce n'est pas
   placée en 200 essais, le candidat est abandonné.
   **Tests :** `PadlockGeneratorTest › a candidate has the number of pieces asked`,
   `PadlockGeneratorTest › a candidate is a valid padlock whatever the random`,
   `PadlockGeneratorTest › a candidate that cannot place a piece is ruled out`,
   `PadlockGeneratorTest › the same random gives the same candidate`,
   `PadlockGeneratorTest › different randoms give different candidates`,
   `PadlockGeneratorTest › each piece gets its own attempts`,
   `PadlockGeneratorTest › almost every random gives a candidate of 6 pieces`,
   `PadlockGeneratorTest › the first piece of a candidate takes every placement inside the ring`,
   `PadlockGeneratorTest › over many randoms the candidates use every orientation of every type and every inner cell`,
   `PadlockGeneratorTest › a candidate draws its pieces from its set`.
2. **20 candidats** par essai, abandonnés compris ; chacun est noté, le vingtième aussi.
   **Tests :** `PadlockLevelTest › when no try gives a padlock within the bound the generator fails, 20 candidates per try`,
   `PadlockLevelTest › the twentieth candidate is rated too`.
3. **Élimination** : un trou rectangulaire (il remplit sa boîte englobante) n'est pas une
   serrure.
   **Tests :** `PadlockRatingTest › a rectangular hole is ruled out`.
4. **Note**, entière : `poids de la forme × contour + 30 × variété − 3 × boîte − poids de la
   préférence × solutions`, où le contour compte les côtés de case du trou qui donnent sur le
   métal, la variété le nombre de types différents, la boîte l'aire de la boîte englobante du
   trou, les solutions leur nombre plafonné à 5 (poids dans la section
   [Progression](#progression) ; pour un trou sinueux aux solutions libres,
   `10 × contour + 30 × variété − 3 × boîte`). Le prototype notait
   `contour + 3 × variété − 0,3 × boîte` plus un bruit aléatoire : même ordre, sans arrondi ni
   bruit. À égalité, le premier candidat composé l'emporte, comme dans le générateur de musique.
   **Tests :** `PadlockRatingTest › the rating is 10 per edge of the outline, 30 per kind of piece, minus 3 per cell of the box`,
   `PadlockRatingTest › each edge of the outline weighs as its shape asks, each solution takes off the weight of its preference`,
   `PadlockLevelTest › a padlock is rated for the shape and the preference of its profile`,
   `PadlockLevelTest › the best candidate is kept, the first one among equals`.
5. Si aucun candidat ne reste, on passe à l'**essai suivant** : l'essai *k* ≥ 1 part du premier
   nombre de `StableRandom((n shl 32) or k)`, jamais de *n* + *k*, qui est la graine d'une autre
   serrure. Au plus **100 essais** ; au-delà, c'est une erreur de programmation
   (`IllegalStateException`), pas un état de l'écran. Le nombre d'essais et la composition d'un
   candidat sont des paramètres du générateur, pour que les tests puissent provoquer cet échec ;
   les tirages par pièce sont un paramètre du candidat. Les serrures 1 à 200 ont toutes les
   pièces de leur profil (une de chaque type quand le profil le demande) autour d'un trou qui
   n'est pas un rectangle.
   **Tests :** `PadlockLevelTest › when every candidate is given up, try k draws on the pair n, k mixed by SplitMix, not on n + k`,
   `PadlockLevelTest › when every candidate is ruled out the next try is taken`,
   `PadlockLevelTest › the seeds of the tries of neighbouring padlocks never meet`,
   `PadlockLevelTest › a padlock takes one try at least`,
   `PadlockLevelTest › when no try gives a padlock within the bound the generator fails, 20 candidates per try`,
   `PadlockLevelTest › padlocks 1 to 200 hold the pieces of their profile, one of each when asked, around a hole no rectangle`.
6. **Pièces données** au joueur : celles de la solution, rangées dans l'ordre de `PieceType`
   (I O T S Z J L), qui ne trahit pas la solution.
   **Tests :** `PadlockTest › the pieces given to the player follow the order I O T S Z J L, not the solution`.

## Niveaux

- Serrure *n* (à partir de 1) : `Padlock.ofLevel(n)`, avec les pièces de son palier (table des
  [paliers](#paliers-de-difficulté)) : 3, 4, puis 5, puis 6 dès la serrure 4, 7 dès la 13, 8 dès
  la 21, puis le cycle de 5 au-delà de 30.
  **Tests :** `PadlockProfileTest › the padlocks 1 to 30 follow the table of the tiers`,
  `PadlockProfileTest › beyond padlock 30 the padlocks turn on a cycle of 5`,
  `PadlockProfileTest › there is no padlock before padlock 1`,
  `PadlockLevelTest › padlock n holds the pieces of its profile`,
  `PadlockLevelTest › padlocks 1 to 200 hold the pieces of their profile, one of each when asked, around a hole no rectangle`.
- La clé de navigation `LocksmithRoute(level)` ne porte que le niveau : pas de graine, la serrure
  *n* étant la même pour tous. La clé numérote les serrures dès 1, et la suivante est
  `route.next()`, d'un niveau de plus.
  **Tests :** `LocksmithRouteTest › padlocks are numbered from 1`,
  `LocksmithRouteTest › the next padlock is one level up`,
  `LocksmithViewModelTest › the screen opens on padlock n of its route, the same for everyone, nothing placed`,
  `LocksmithViewModelTest › the factory builds the padlock and the level of the route the entry hands it`,
  `LocksmithViewModelTest › the factory fails clearly without the route of the entry`.
- Après la mort du processus, la clé restaurée redonne la même serrure, **sans pièce posée**. Une
  clé sauvegardée par une version antérieure à P7.30, qui portait encore une graine, redonne la
  serrure de son niveau (la graine est ignorée).
  **Tests :** `LocksmithRouteTest › a route restored after the death of the process gives the same padlock, nothing placed`,
  `LocksmithRouteSavingTest › a padlock route comes back from the saved state as it was`,
  `LocksmithRouteSavingTest › a padlock route saved with its seed by an older version comes back as the padlock of its level`.
- La cartouche « Serrurier » ouvre la frontière, la première serrure non ouverte (la serrure 1 sans avancement ;
  P7.34, section [Avancement et anciennes serrures](#avancement-et-anciennes-serrures)) ; « Serrure suivante » met
  à sa place la serrure d'un niveau de plus, et un second toucher ne saute pas deux serrures.
  **Tests :** `OpenCartridgeTest › the Locksmith cartridge opens the frontier, the first padlock not open, over the cartridges`,
  `OpenCartridgeTest › a second tap on the Locksmith cartridge does not stack a second padlock`,
  `NextPadlockTest › the next padlock takes the place of the open one, one level up`,
  `NextPadlockTest › a second tap on next padlock does not move on twice`,
  `BloxNavDisplayTest › with no padlock open, the Locksmith cartridge names padlock 1 and opens it`,
  `BloxNavDisplayTest › on the Locksmith, the system back goes back to the cartridges, and then leaves`,
  `BloxNavDisplayTest › a restored stack with a padlock on top shows that padlock again, nothing placed`.
  **Vues :** V26, V35.
- Les barres système suivent la variante Couleurs du Serrurier (jour ou nuit selon le téléphone),
  quel que soit le thème choisi pour le Classique ; ailleurs, le thème choisi.
  **Tests :** `SystemBarsTest › the bars of a screen drawn in colorful only follow its colorful variant, whatever the theme chosen`,
  `SystemBarsTest › the bars of every other screen follow the theme chosen`,
  `SystemBarsAppearanceTest › at night in the Original theme, the bars turn light under the Locksmith, and dark again once it is left`.
  **Vues :** V26, V35.
- L'avancement est enregistré (P7.32, P7.33) et la cartouche ouvre la frontière (P7.34) : voir la section
  [Progression](#progression) ci-dessous.

## Progression

Décidée le 2026-10-28. Demande du développeur : garder l'avancement, poursuivre sur de nouvelles
serrures, rouvrir les anciennes.
Propositions de `designer-jeu` retenues par le développeur (P7.27, P7.28). Une serrure par niveau et ses paliers sont
faits (P7.29, P7.30) : c'est le jeu décrit plus haut. L'avancement est gardé et écrit à chaque ouverture, la carte
le montre (P7.31 à P7.33), la cartouche ouvre la frontière (P7.34), l'écran « Serrures » liste les serrures et
s'ouvre depuis le disque de la barre de V35 (P7.35, P7.36). Tant que la tâche P7.37 n'est pas faite (recommencer
depuis le début), le jeu suit les sections précédentes ; chaque règle recevra ses tests en son temps.

### Une serrure par niveau, la même pour tous

- La serrure *n* est **la même pour tous et pour toujours** : sa graine est *n*, et le générateur tire sur
  `StableRandom` (`core:game`, dont la suite est un contrat), plus sur `kotlin.random.Random`, dont la suite n'est pas
  garantie d'une version de Kotlin à l'autre (ADR 0014). Une ancienne serrure se rouvre donc à l'identique, et
  « serrure 27, parfaite » a le même sens pour deux joueurs.
- **Repli sans collision** : quand la graine *n* ne donne pas de serrure, le repli tire sur un flux dérivé du
  couple (*n*, essai *k*) (mélange SplitMix), jamais sur *n* + *k*, qui est la graine d'une autre serrure. Aucune des
  200 premières serrures n'a le même trou qu'une autre, à translation près.
- **Contrat de génération** (figé par un test, changé seulement par un ADR) : la suite de `StableRandom`, la graine
  et son repli, l'ordre des tirages (type, orientation, colonne, ligne), l'ordre de `PieceType.entries` et des
  orientations, 200 tirages par pièce, 20 candidats, la table des paliers ci-dessous, les poids de la note,
  l'élimination des trous rectangulaires et le départage au premier candidat. Forme du test : l'ASCII de la
  première serrure de chaque palier et de chaque famille, et une empreinte des serrures 1 à 100. Ne sont pas
  figés : l'ordre de recherche de l'indice, les suites d'accords, les noms et l'affichage des paliers.
- **Ordre de travail** (P7.29) : un script mesure d'abord les serrures 1 à 100 (forme du trou, nombre de
  solutions plafonné à 5, effort du solveur) ; les poids de la note sont calés pour que la médiane monte de palier
  en palier ; alors seulement le contrat est figé.
- **Fait** (P7.29) : `Padlock.ofLevel(level)` tire ses candidats sur `StableRandom` semé par *n* ; le repli de
  l'essai *k* ≥ 1 part du premier nombre de `StableRandom((n shl 32) or k)`. Mesure des serrures 1 à 100
  (`PadlockMeasure`, désactivé par défaut : `BLOX_PADLOCK_MEASURE=true`) : la médiane de l'effort du solveur (nœuds
  visités) monte de palier en palier, 7 (Apprenti), 10 (Compagnon sinueux), 14 (Compagnon mixte), 30,5 (Artisan),
  85 (Maître) ; toutes les serrures mesurées ont une solution unique. Génération des 100 serrures : 140 à 340 ms.
  Poids retenus : contour par arête 10 (sinueuse), 5 (mixte), 2 (compacte) ; retrait par solution (compte plafonné à
  5) 0 (libre), 15 (peu), 45 (unique) ; inchangés : +30 par type, −3 par case de la boîte. Le contrat fige l'ASCII
  des serrures 1, 2, 3, 4, 7, 13, 21 et 31 à 35, et une empreinte SHA-256 des serrures 1 à 100.
  **Tests :** `PadlockProfileTest › the padlocks 1 to 30 follow the table of the tiers`,
  `PadlockProfileTest › beyond padlock 30 the padlocks turn on a cycle of 5`,
  `PieceSetTest › a set draws its types in the order of the piece types, whatever was placed`,
  `PieceSetTest › one of each draws the types not placed yet`,
  `PadlockGeneratorTest › a candidate draws its pieces from its set`,
  `PadlockGeneratorTest › a candidate of one of each holds the seven types once`,
  `PadlockSolverTest › a hole that only its solution fills has one solution`,
  `PadlockSolverTest › every cover is counted once, pieces of a type being alike`,
  `PadlockSolverTest › the count stops at the cap`,
  `PadlockRatingTest › each edge of the outline weighs as its shape asks, each solution takes off the weight of its preference`,
  `PadlockRatingTest › the weights are those calibrated by the measure of padlocks 1 to 100`,
  `PadlockLevelTest › the candidates of padlock n draw on StableRandom, seeded by the numbers of StableRandom of n`,
  `PadlockLevelTest › when every candidate is given up, try k draws on the pair n, k mixed by SplitMix, not on n + k`,
  `PadlockLevelTest › when no try gives a padlock within the bound the generator fails, 20 candidates per try`,
  `PadlockLevelTest › padlock n holds the pieces of its profile`,
  `PadlockContractTest › the first padlock of each tier and of each family of the cycle never changes`,
  `PadlockContractTest › the padlocks 1 to 100 never change`,
  `PadlockContractTest › no two of the first 200 padlocks have the same hole, translation aside`,
  `PadlockContractTest › the draws per piece, the candidates and the solutions counted never change`,
  `PadlockLevelTest › the seeds of the tries of neighbouring padlocks never meet`,
  `PadlockLevelTest › a padlock takes one try at least`,
  `PadlockProfileTest › a profile of one of each holds 7 pieces at most`,
  `PadlockProfileTest › there is no padlock before padlock 1`.
- **Fait** (P7.30) : l'écran joue `Padlock.ofLevel(n)` ; la clé `LocksmithRoute(level)` n'a plus de graine, la
  suivante est `route.next()` (section [Niveaux](#niveaux)). L'ancien générateur sur `kotlin.random.Random`
  (`Padlock.generate`) est supprimé. Une pile sauvegardée par une version antérieure (clé avec graine) redonne la
  serrure de son niveau. La cartouche ouvrait alors toujours la serrure 1 ; elle ouvre la frontière depuis P7.34.
  **Tests :** `LocksmithRouteTest › the next padlock is one level up`,
  `LocksmithViewModelTest › the screen opens on padlock n of its route, the same for everyone, nothing placed`,
  `LocksmithRouteSavingTest › a padlock route saved with its seed by an older version comes back as the padlock of its level`,
  `NextPadlockTest › the next padlock takes the place of the open one, one level up`,
  `PadlockLevelTest › padlocks 1 to 200 hold the pieces of their profile, one of each when asked, around a hole no rectangle`.

### Paliers de difficulté

La difficulté monte par paliers aux noms de compagnonnage, puis tourne sur un cycle au lieu de durcir sans fin.
**Plafond : 8 pièces** (32 cases, la moitié du trou de 8 × 8) ; au-delà, le métal se morcelle.

| Serrures | Palier | Pièces | Types | Forme du trou | Solutions |
|---|---|---|---|---|---|
| 1 | Apprenti | 3 | sans S ni Z | sinueuse (note actuelle) | libre |
| 2 | Apprenti | 4 | sans S ni Z | sinueuse | libre |
| 3 | Apprenti | 5 | tous | sinueuse | libre |
| 4 à 6 | Compagnon | 6 | tous | sinueuse | libre |
| 7 à 12 | Compagnon | 6 | tous | mixte | peu de solutions préférées |
| 13 à 20 | Artisan | 7 | tous | mixte | peu de solutions préférées |
| 21 à 30 | Maître | 8 | tous | compacte | peu de solutions préférées |
| 31 et plus | Maître | 7 ou 8 | cycle de 5, selon *n* modulo 5 | voir le cycle | voir le cycle |

Cycle au-delà de 30 : 0 « un de chaque » (7 pièces, les 7 types une fois, forme mixte, peu de solutions) ; 1 compacte de 8, solution
unique préférée ; 2 « respiration » (8 pièces, forme sinueuse) ; 3 famille S, Z et T seulement (8 pièces, mixte, peu de
solutions) ;
4 compacte de 8, peu de solutions préférées.

- Un trou **sinueux** est facile (ses couloirs imposent les coups) ; un trou **compact** laisse plus de poses
  plausibles et de fausses pistes. La forme se règle par le poids du contour dans la note : +10 pour sinueuse (comme
  aujourd'hui), plus faible pour mixte et compacte (valeurs calées par la mesure).
- « Peu de solutions » est un **terme de la note** (compte plafonné à 5), jamais un filtre : une serrure se trouve
  toujours. Toute serrure garde au moins une solution (le trou est construit à partir d'elle) ; l'indice reste
  suffisant.
- Les suites d'accords s'étendent à 7 et 8 pièces : I–V–vi–iii–IV–V–I et I–V–vi–iii–IV–ii–V–I (les cinq premiers
  accords du canon de Pachelbel, domaine public, puis la cadence ; P7.29). Elles finissent sur
  V–I), à confirmer dans `docs/musique.md`.
- Paliers lisibles sans bruit : une ligne sur la carte de la dernière serrure d'un palier (« Palier suivant :
  Artisan · 7 pièces ») et un en-tête par palier dans l'écran « Serrures » ; pas de fenêtre ni de bannière.

### Avancement et anciennes serrures

- On retient, pour chaque serrure ouverte, sa **meilleure ouverture** : le triplet (coups, refus, indices) d'une
  même partie, comparé d'abord par les indices, puis les coups, puis les refus. Une serrure est **parfaite** quand
  elle est ouverte en autant de coups que de pièces, sans refus ni indice. Les serrures s'ouvrent dans l'ordre :
  les serrures ouvertes sont 1 à *N*, la suivante à ouvrir est *N* + 1 (la « frontière »).
  Une ouverture est un `PadlockResult` (P7.31) : le nombre de pièces de la serrure, qui permet de dire « parfaite »
  sans la régénérer, puis les coups, refus et indices ; `PadlockGame.result` le donne une fois la serrure ouverte
  (aucun avant). Les coups sont les poses acceptées, reprises comprises, donc jamais moins que les pièces ; deux
  ouvertures ne se comparent que pour un même nombre de pièces.
  **Tests :** `PadlockResultTest › fewer hints is better, even with more moves and refusals`,
  `PadlockResultTest › at equal hints, fewer moves is better, even with more refusals`,
  `PadlockResultTest › at equal hints and moves, fewer refusals is better`,
  `PadlockResultTest › an equal result is not better`,
  `PadlockResultTest › a padlock opened in as many moves as pieces, without refusal or hint, is perfect`,
  `PadlockResultTest › one more move, a refusal or a hint is not perfect`,
  `PadlockResultTest › a padlock without pieces or a negative counter is a programming error`,
  `PadlockResultTest › a padlock is not opened in fewer moves than pieces`,
  `PadlockResultTest › openings of padlocks of different numbers of pieces are not compared`,
  `PadlockGameTest › an open padlock gives the result of its opening, a padlock not open none`,
  `PadlockGameTest › a padlock opened without taking back, refusal or hint is perfect`.
- La **cartouche** ouvre la frontière, et porte la sous-ligne « Serrure *N* + 1 ».
  **Fait** (P7.34, `feature:cartridges`, `app`) :
  - La cartouche « Serrurier » ouvre la **frontière**, la première serrure non ouverte (la serrure 1 sans
    avancement), par-dessus les cartouches ; un second toucher n'empile pas une seconde serrure.
  - Elle porte la **sous-ligne « Serrure *N* »** (*N* la frontière), que TalkBack lit après le nom ; en police ×2 à
    360 dp, « Serrure 1000 » tient sur une ligne, sans être coupée. La sous-ligne n'apparaît qu'**une fois la
    frontière lue** : avant, la cartouche n'en porte aucune, plutôt qu'une ligne qui pourrait changer. Sa **place est
    réservée** sur toutes les cartouches, lue ou non, pour que la grille ne bouge pas : le Serrurier est aussi haut
    que le Classique à côté de lui.
  - `PadlockFrontier` (`app`) lit l'avancement dès le démarrage de l'application et le suit : `current`
    (`StateFlow<Int?>`, `null` tant que l'avancement n'est pas lu) et `await()`. Si la lecture échoue, le dépôt rend
    un avancement vide, et la frontière vaut 1.
  - Un **toucher fait avant la lecture l'attend**, au lieu d'ouvrir la serrure 1, qui n'est peut-être pas la
    frontière ; deux touchers pendant la lecture n'empilent qu'une serrure.
  - `CartridgeOpener` (`app`, remplace `openCartridge`) : seule la cartouche Serrurier attend, les autres empilent
    leur écran tout de suite. Un toucher sur une autre cartouche **annule** une attente du Serrurier encore en
    cours : aucune serrure ne s'empile par surprise sur le jeu ouvert, ni après qu'on l'a quitté.
  - `feature:cartridges` ne reçoit que la frontière (`padlockFrontier: StateFlow<Int?>`), sans dépendre d'une autre
    feature. Le parcours Macrobenchmark reste valide : l'application mesurée, sans avancement, ouvre la serrure 1.
  **Tests :** `PadlockFrontierTest › with no padlock open, the frontier is padlock 1`,
  `PadlockFrontierTest › once the progress is read, the frontier is known at once, without waiting for a collection`,
  `PadlockFrontierTest › the frontier moves on as a padlock opens, at each reading of the progress`,
  `PadlockFrontierTest › before the progress is read, no frontier is known, so that no line shows that may have to change`,
  `PadlockFrontierTest › a tap before the progress is read waits for it, rather than open padlock 1 when it is not the frontier`,
  `OpenCartridgeTest › the Locksmith cartridge opens the frontier, the first padlock not open, over the cartridges`,
  `OpenCartridgeTest › a second tap on the Locksmith cartridge does not stack a second padlock`,
  `OpenCartridgeTest › two taps on the Locksmith cartridge while the frontier is read stack a single padlock`,
  `OpenCartridgeTest › the Classic cartridge opens the Classic home over the cartridges, at once`,
  `OpenCartridgeTest › a game opened while the frontier of the Locksmith is read is not covered by a padlock`,
  `OpenCartridgeTest › a tap on the Locksmith left waiting while a game was opened and left opens no padlock once read`,
  `CartridgesScreenTest › the Locksmith cartridge bears the line « Serrure N », its frontier, which TalkBack reads after its name`,
  `CartridgesScreenTest › before the frontier is read, the Locksmith cartridge bears no line, rather than one that may change`,
  `CartridgesScreenTest › on 360 dp, the Locksmith with its line stands as high as the Classic beside it, read or not`,
  `CartridgesScreenTest › in a font twice as large, on 360 dp, the line « Serrure 1000 » holds on one line, unclipped`,
  `BloxNavDisplayTest › with no padlock open, the Locksmith cartridge names padlock 1 and opens it`,
  `BloxNavDisplayTest › the Locksmith cartridge names the frontier and opens it: padlock 3, once padlocks 1 and 2 are open` ;
  captures `CartridgesDay`, `CartridgesNight` et `CartridgesLargeFont`.
  **Vues :** V36.
- Un écran **« Serrures »** (grille, depuis le cadenas de la barre de V35, qui devient un disque de 48 dp) : un
  résumé (« 12 ouvertes · 3 parfaites »), « Reprendre · Serrure 13 », puis la grille, défilée sur la frontière ;
  chaque case dit son état par le texte, la couleur ne fait que le doubler : ouverte, parfaite (étoile), à ouvrir,
  en cours (celle d'où l'on vient), verrouillée (seulement pour compléter la rangée de la frontière, non touchable).
  Toucher une case ouverte la rejoue ; le retour rend la serrure en cours, pièces posées intactes.
  **Fait** (P7.35) : l'écran, clé `PadlocksRoute(current)` de `feature:locksmith` (`current`, la serrure d'où l'on
  vient, numérotée depuis 1, ou aucune ; gardée après la mort du processus), `PadlocksViewModel` et sa fabrique,
  construite par `app`. Toujours en thème Couleurs, barres système comprises, comme V35. La navigation (le disque
  de la barre de V35, le choix d'une serrure) est venue avec P7.36, décrite plus bas.
  - **Barre** : seule la barre (retour, « SERRURES ») est fixe. Le résumé « *n* ouvertes · *m* parfaites », lu
    comme un en-tête, et « Reprendre · Serrure *N* » (la frontière) défilent avec la grille. Rien ne s'affiche avant
    la lecture de l'avancement (pas d'écran vide qui clignote) ; l'écran suit l'avancement ensuite.
  - **Grille** : les serrures ouvertes puis la frontière, chacune sous l'en-tête de son palier (`PadlockTier`,
    `core:locksmith`, hors du contrat de génération) : « Apprenti · 3 à 5 pièces », « Compagnon · 6 pièces »,
    « Artisan · 7 pièces », « Maître · 7 ou 8 pièces » (Maître court de la serrure 21 à l'infini, cycle compris),
    lus comme des en-têtes. La grille s'ouvre défilée sur la frontière, et y revient quand sa hauteur change, tant
    que le joueur n'a pas défilé.
  - **Cases** : ouverte, parfaite (étoile ; ouverte en autant de coups que de pièces, sans refus ni indice), à
    ouvrir (la frontière), en cours (bord d'encre : la serrure d'où l'on vient, ouverte ou frontière). Les cases
    **verrouillées** ne font que compléter la rangée de la frontière, sans dépasser la fin de son palier (au-delà de
    30 aussi) ; elles ne sont jamais en cours, ne se touchent pas et se lisent « verrouillée ». TalkBack lit une case
    d'un trait : « Serrure 5, ouverte, parfaite, en cours, meilleure : 4 coups, 0 refus, 0 indice », et offre
    l'action « Ouvrir » sur une case touchable.
  - **État vide** (aucune ouverture) : « Ouvre la serrure 1 pour commencer. », sans Reprendre ; la serrure 1 est la
    frontière, sa rangée complétée.
  - **Police ×2** : moins de colonnes (cases de 80 dp au moins, jamais aucune colonne) ; sur un écran bas, résumé,
    Reprendre et grille défilent ensemble, une rangée de cases visible en entier.
  Pas encore de numéro de vue : la maquette de l'écran viendra en P7.38. Captures `PadlocksDay`, `PadlocksNight`,
  `PadlocksEmptyDay`, `PadlocksSmallDay`, `PadlocksLowDay`, `PadlocksLargeFontDay`.
  **Tests :** `PadlockTierTest › the padlocks follow the tiers of the table, the cycle being of masters`,
  `PadlockTierTest › the pieces of a tier are those of its padlocks, from the fewest to the most`,
  `PadlockTierTest › there is no tier before padlock 1`,
  `PadlocksGridTest › the open padlocks then the frontier, each under the header of its tier`,
  `PadlocksGridTest › a padlock opened in as many moves as pieces, without refusal or hint, is perfect`,
  `PadlocksGridTest › locked padlocks complete the row of the frontier, and no more`,
  `PadlocksGridTest › the locked padlocks stop at the end of the tier of the frontier`,
  `PadlocksGridTest › the locked padlocks complete the row of the frontier within its tier, also on the cycle beyond 30`,
  `PadlocksGridTest › the padlock the player comes from is in progress, open or the frontier`,
  `PadlocksGridTest › a locked padlock is never in progress, even when the player comes from it`,
  `PadlocksGridTest › without an opening, padlock 1 is the frontier, its row completed`,
  `PadlockColumnsTest › the larger the font, the fewer the columns, never none`,
  `TierHeaderTest › the pieces of a tier read as one count, two counts apart by « ou », or a range by « à »`,
  `PadlockTileTest › the words of a tile tell its state, and whether the player comes from it`,
  `PadlockTileTest › a tile is read with its state in full, a perfect padlock being open`,
  `PadlocksUiStateTest › the summary counts the open padlocks and the perfect ones, and Reprendre opens the next`,
  `PadlocksUiStateTest › without an opening the screen is empty, and padlock 1 is to open`,
  `PadlocksViewModelTest › the screen shows the best opening of each open padlock, padlock 1 first`,
  `PadlocksViewModelTest › nothing is shown until the progress is read, so that no empty screen flashes`,
  `PadlocksViewModelTest › the screen follows the progress, a padlock opened meanwhile shown`,
  `PadlocksRouteTest › the padlock the screen comes from is numbered from 1`,
  `PadlocksRouteTest › a route restored after the death of the process comes from the same padlock, or from none`,
  `PadlocksScreenTest › the summary tells the open padlocks and the perfect ones, read as a heading`,
  `PadlocksScreenTest › Reprendre opens the frontier`,
  `PadlocksScreenTest › a padlock is read in one go: its number, its state, whether the player comes from it, its best`,
  `PadlocksScreenTest › touching an open padlock or the frontier asks to open it, by its number`,
  `PadlocksScreenTest › a locked padlock, which completes the row of the frontier, is read locked and cannot be touched`,
  `PadlocksScreenTest › each tier has its header, its name and the pieces of its padlocks, read as a heading`,
  `PadlocksScreenTest › without an opening, the screen asks to open padlock 1, and offers no Reprendre`,
  `PadlocksScreenTest › the grid, a collection, opens scrolled on the frontier`,
  `PadlocksScreenTest › in a font twice as large the grid has fewer columns, its tiles 80 dp at least`,
  `PadlocksScreenTest › in a large font on a low screen, the summary and Reprendre scroll with the grid, a row of tiles shown whole`,
  `PadlocksScreenTest › a tile that can be touched offers TalkBack to open it, « Ouvrir »`,
  `SystemBarsTest › the padlocks of the locksmith`,
  `BloxNavDisplayTest › the screen of the padlocks over a padlock shows the progress, and back goes back to the padlock`.
  **Fait** (P7.36, `feature:locksmith`, `app`) : le disque et la navigation.
  - **Le disque « Serrures »** : le cadenas de la barre de V35 devient un disque de 48 dp (`GlyphButton`), qui dessine
    le cadenas, anse levée une fois la serrure ouverte. TalkBack le lit « Toutes les serrures », pour ce qu'il ouvre ;
    le titre de la barre garde son rôle de titre et dit l'état, « fermée » ou « ouverte ». Le trou de serrure du
    pictogramme est désormais percé dans le dessin ; les cases de l'écran « Serrures » partagent ce pictogramme.
  - **Ouvrir « Serrures »** (`openPadlocks`, `app`) : le disque empile l'écran par-dessus la serrure, qui y est « en
    cours ». La serrure reste dessous avec son ViewModel : le retour la rend, ses pièces posées. Un double toucher
    n'empile pas un second écran.
  - **Choisir une serrure** (`choosePadlock`, `app`) : une case ou « Reprendre » retire la liste et met la serrure
    choisie à la place de celle d'en dessous. Choisir celle d'où l'on vient ne fait que revenir : la même clé est
    gardée, et avec elle le ViewModel et les pièces posées. Sans serrure en dessous, la serrure choisie prend la place
    de la liste. Un second toucher ne change plus rien.
  - **Police ×2 à 360 dp** : la barre tient, titre aux mots entiers, coups sur une ligne, disque de 48 dp entier, sans
    chevauchement (capture `LocksmithBarLargeFontDay`). Les captures `Locksmith*` et `Padlocks*` ont été régénérées
    (le disque, le trou percé).
  **Tests :** `NextPadlockTest › the disc of a padlock opens the screen of the padlocks over it, that padlock current`,
  `NextPadlockTest › a second tap on the disc does not open the screen of the padlocks twice`,
  `NextPadlockTest › a padlock chosen on the screen of the padlocks takes the place of the padlock under it, and the list goes`,
  `NextPadlockTest › choosing the padlock one comes from only goes back to it, the same key kept`,
  `NextPadlockTest › a second tap on the screen of the padlocks moves nothing more`,
  `NextPadlockTest › with no padlock under the screen of the padlocks, the padlock chosen takes its place`,
  `BloxNavDisplayTest › the disc of a padlock opens the screen of the padlocks over it, and back finds its piece placed`,
  `BloxNavDisplayTest › a padlock chosen on the screen of the padlocks takes the place of the padlock, back then leaves it`,
  `BloxNavDisplayTest › choosing on the screen of the padlocks the padlock one comes from goes back to it, its piece placed`,
  `LocksmithScreenTest › the disc of the bar, read « Toutes les serrures », opens the padlocks`,
  `LocksmithScreenTest › the title of the bar tells the padlock closed, then open, the disc being read for what it opens`,
  `LocksmithScreenTest › in a font twice as large, on 360 dp, the bar holds its title, its moves and a disc of 48 dp`.
  **Vues :** V35 (la maquette montre encore le cadenas de 24 dp ; elle sera reprise en P7.38).
- Après avoir rejoué une ancienne serrure, « Serrure suivante » mène à la serrure d'après ; si elle est déjà
  ouverte, la carte ajoute « Reprendre à la serrure *N* ». Une nouvelle meilleure ouverture ou une serrure parfaite
  s'annonce par un autocollant (NOUVEAU MEILLEUR, PARFAITE).
  **Fait** (P7.33) : la carte, décrite dans la section [Écran](#écran) (« Serrure ouverte »). « Reprendre à la
  serrure *N* » met la frontière à la place de la serrure rejouée, en haut de la pile (`resumePadlocks`, `app`),
  comme « Serrure suivante » y met la suivante ; un second toucher ne fait rien de plus.
  **Tests :** `NextPadlockTest › resuming puts the frontier in the place of the padlock replayed`,
  `NextPadlockTest › a second tap on resume does not move on twice`,
  `LocksmithScreenTest › on the card of an old padlock replayed, Reprendre à la serrure N resumes at the frontier`.
- **Écriture à l'ouverture** (fait, P7.33) : quand la serrure s'ouvre, le ViewModel écrit son ouverture dans
  l'avancement, dans la portée de l'application (`mainScope` de l'`AppContainer`), qui survit à l'écran : quitter
  la serrure aussitôt ne perd pas l'ouverture. L'ouverture passe de `PadlockResult` (`core:locksmith`) à
  `PadlockOpening` (`core:data`, qui ne voit pas `core:locksmith`) par un adaptateur (`PadlockOpenings.kt`,
  `feature:locksmith`) ; un test garde les deux types en accord : mêmes compteurs dans les deux sens, mêmes
  rejets, même ordre du meilleur, même refus de comparer des serrures de nombres de pièces différents.
  - La **frontière est lue au moment d'écrire**, pas celle que l'écran suivait, qui peut retarder sur une
    écriture en cours ou devancer un avancement effacé depuis. Une serrure au-delà de la frontière (après une
    ouverture qui n'a pas pu s'écrire, par exemple) n'est pas enregistrée.
  - Une écriture impossible (`IOException` : disque plein, avancement d'une version ou d'une génération plus
    récente) laisse jouer ; la serrure n'est pas enregistrée. Toute autre erreur est un bug et remonte à la portée
    de l'application. Un avancement plus récent n'est lu qu'une fois par l'écran, puis une fois par ouverture,
    jamais en boucle.
  - Dans ces deux cas, la carte dit « Résultat non enregistré », tant que la serrure n'est pas recommencée.
  - Ce que l'ouverture a été pour l'avancement (première, nouvelle meilleure, rien de nouveau) est gardé pour la
    carte jusqu'à « Rejouer cette serrure ». Une écriture qui finit après « Rejouer » ne marque pas la partie
    recommencée.
  - L'écran suit, tant qu'il vit, la meilleure ouverture gardée de sa serrure (aucune pour une serrure pas encore
    ouverte) et la frontière, pour la carte.
  **Tests :** `PadlockOpeningsTest › a result becomes the opening of the same counters, and back, without loss`,
  `PadlockOpeningsTest › a result and an opening refuse the same counters, with the same failure`,
  `PadlockOpeningsTest › a result and an opening tell the better of two the same way, each criterion alone and against the others`,
  `PadlockOpeningsTest › a result and an opening both refuse to compare openings of different numbers of pieces`,
  `LocksmithProgressTest › opening the padlock records its opening, for its level`,
  `LocksmithProgressTest › a padlock beyond the frontier is not recorded, and the screen tells it`,
  `LocksmithProgressTest › the opening is recorded even when the screen is left at once`,
  `LocksmithProgressTest › an opening that cannot be written leaves the game playable, and the screen tells it`,
  `LocksmithProgressTest › what the opening was for the progress is kept for the card, until the padlock starts over`,
  `LocksmithProgressTest › the screen follows the best opening kept of its padlock, and the frontier`,
  `LocksmithProgressTest › a padlock not open has no best opening`,
  `LocksmithProgressTest › a progress of a later version is read once by the screen and once per opening, the game going on`,
  `LocksmithProgressTest › the frontier is read when the opening is written, not when the screen opened`,
  `LocksmithProgressTest › a write that ends after the padlock started over tells nothing of the game started over`,
  `LocksmithProgressTest › the frontier is followed after a first opening, until the card offers to resume`.
- **Recommencer depuis le début**, en bas de l'écran « Serrures », avec confirmation dans la page : les résultats
  sont effacés, les serrures restent les mêmes.
- L'avancement est un fichier versionné de `core:data` (qui ne stocke que des entiers et un numéro de génération)
  et **entre dans la sauvegarde Android** : c'est l'acquis du joueur, et les serrures sont les mêmes sur un autre
  appareil.
  Fait côté données (P7.32) : `PadlockProgressRepository` donne `progress` (les meilleures ouvertures des serrures
  1 à *N*, `PadlockOpening` de 4 entiers, mêmes règles que `PadlockResult`, et la frontière *N* + 1) ;
  `record(padlock, opening)` rend une première ouverture, une nouvelle meilleure ou rien de nouveau ; une serrure
  hors de 1 à la frontière, ou d'un autre nombre de pièces que sa meilleure, est une erreur de programmation ;
  `clear()` oublie tout. Fichier `padlocks.bin` v1 : « BLXS », la version, la génération (1), *N*, puis *N*
  ouvertures (pièces, coups, refus, indices). Corrompu (signature, version ou génération sous 1, compte négatif,
  ouverture impossible, tronqué, octets en trop) : copié à part, avancement vide. Version **ou génération** plus
  récente : avancement vide, fichier gardé sans écriture (les résultats d'une autre génération de serrures ne se
  mêlent pas aux siens).
  **Tests :** `PadlockOpeningTest › fewer hints is better, even with more moves and refusals`,
  `PadlockOpeningTest › a padlock is not opened in fewer moves than pieces`,
  `PadlockProgressRepositoryContract › without an opening no padlock is open and the frontier is padlock 1`,
  `PadlockProgressRepositoryContract › opening the frontier is a first opening, kept after the others, and the frontier moves on`,
  `PadlockProgressRepositoryContract › a better opening of an open padlock is a new best, and takes its place`,
  `PadlockProgressRepositoryContract › an opening no better than the best of its padlock is nothing new, and the best is kept`,
  `PadlockProgressRepositoryContract › a padlock beyond the frontier or before padlock 1 is a programming error, and nothing is kept`,
  `PadlockProgressRepositoryContract › an opening of another number of pieces than the best of its padlock is a programming error`,
  `PadlockProgressRepositoryContract › clearing the progress forgets every opening, and the frontier is padlock 1 again`,
  `DataStorePadlockProgressRepositoryTest › a padlocks file of version 1 is read as it was written, a format that must not change`,
  `DataStorePadlockProgressRepositoryTest › a padlocks file holding what no progress can be is corrupted, gives an empty progress, and is written again`,
  `DataStorePadlockProgressRepositoryTest › openings of a later generation of padlocks give an empty progress, and are kept for that generation`,
  `DataStorePadlockProgressRepositoryTest › a padlocks file that counts billions of padlocks gives an empty progress, the file being found cut short`.

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
  posées (au plus 8 pièces et 32 cases : un retour arrière suffit). Ordre de recherche, pour que
  l'indice soit toujours le même : la première case libre du trou dans l'ordre de lecture ; les
  pièces non posées par rang (de plusieurs pièces d'un même type, seule la première est essayée,
  les autres donneraient les mêmes couvertures) ; pour chacune, ses orientations dans l'ordre de
  `orientations(type)`, chacune avec le pivot qui fait de cette case la première de la pièce dans
  l'ordre de lecture (aucun autre ne peut convenir : c'est la première case libre).
  **Tests :** `PadlockHintTest › the hint shows where a free piece fits and counts a hint`,
  `PadlockHintTest › following the hints opens padlocks 1 to 200`,
  `PadlockHintTest › the hint tries the orientations of a piece in their order`,
  `PadlockHintTest › the hint covers the first free cell in reading order`,
  `PadlockHintTest › a hint is quick once a piece is placed anywhere in padlocks 1 to 100`.
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
`PadlockChordsTest › there are no chords for a padlock of fewer than 3 or more than 8 pieces`.

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
coups, cadenas d'état, devenu en P7.36 le disque « Serrures »), bande des accords (une pilule par chiffre romain de `progression`,
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
  `LockCellStylesTest › a hole is sunk, not raised, a shadow along its top and no light, no thickness at its bottom`,
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
- La tuile est `PieceTile` (`core:board`, partagée avec le Jardin musical, P10.2) : bouton
  secondaire de 52 dp (une taille donnée par l'écran l'emporte, la pièce restant centrée, 10 dp par
  case), anneau `accent` de 4 dp sur la tuile choisie, case vide pour une pièce posée, toujours
  touchable ; TalkBack la lit comme un bouton par son nom seul, son orientation en état, jamais
  annoncée ; la pièce dessinée n'a pas de nœud à elle. Ses cases viennent de
  `pieceCells(type, orientation)` (`core:board/piece`).
  **Tests :** `PieceCellsTest › a piece in any orientation, as a tile shows it, is its cells brought to the origin, with its size`,
  `PieceTileTest › a tile is a button read by the name and the state it is given, chosen, its piece without a node of its own`,
  `PieceTileTest › without a piece, a tile is an empty slot, not chosen, which a touch still reaches`,
  `PieceTileTest › the size the screen gives a tile wins over that of a touch target, as the wide tiles of the garden`,
  `PieceTileTest › a tile of 48 dp, the least a screen gives it, draws its piece in its colour, never in black`,
  `PieceTileTest › in a tile of 48 dp, the piece stays centred as in a tile of 52 dp, to a pixel` ;
  captures `PieceTiles`, `PieceTilesNight`, `PieceTilesShrunk`. Vues : V35.
- **Bac à la largeur** (P7.41) : sur une rangée, les tuiles font 52 dp au plus et rétrécissent jusqu'à 48 dp pour
  tenir entre les marges (6 tuiles de 48 dp à 360 dp) ; en dessous de 48 dp, elles passent sur deux rangées, de
  4 au plus, la première prenant la tuile en trop, la seconde centrée sous elle (`trayLayout`). Le calcul se fait
  en pixels entiers, comme Compose pose les tuiles et leurs écarts : une rangée ne dépasse jamais la largeur.
  Une tuile plus petite que 52 dp garde le canevas de sa pièce à 52 dp, centré : la pièce garde sa taille.
  **Tests :** `TrayLayoutTest › 6 tiles of 52 dp hold on one row of a phone of 390 dp`,
  `TrayLayoutTest › on a phone of 360 dp, 6 tiles shrink to 48 dp to hold on one row`,
  `TrayLayoutTest › rather than shrink under 48 dp, 7 tiles go on two rows, of 4 then 3, at 52 dp`,
  `TrayLayoutTest › the tiles of a row, laid out in whole pixels, never pass its width`,
  `TrayLayoutTest › where they hold at 52 dp, the tiles are as large as a tile lays itself out, 52 dp rounded to whole pixels`,
  `TrayLayoutTest › from 1 to 8 tiles, on a phone of 320 dp to 412 dp, each is 48 to 52 dp, on two rows of 4 at most`,
  `LocksmithScreenTest › on a phone of 360 dp, each of 6 tiles is a whole touch target, within the width`,
  `LocksmithScreenTest › on a phone of 360 dp, the 6 tiles stand on one row, none over the next`,
  `LocksmithScreenTest › on a phone of 320 dp, the 6 tiles go on two rows of 3, the second under the first`,
  `LocksmithScreenTest › on 300 dp, a padlock of 5 pieces centres its last 2 tiles under the first 3` ;
  captures `LocksmithSixTilesDay`, `LocksmithSixTilesNarrowDay`, `LocksmithFiveTilesNarrowDay`. Vues : V35.
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
  `ChordTellingsTest › a chord is read by its degree and its quality, not by its numeral, which TalkBack would read as a word`,
  `LocksmithReadingTest › a flat seventh is told 7 flat major on the Locksmith too`,
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
  La barre a un bouton retour, son titre en capitales, « SERRURE 03 » (numéro sur deux chiffres ; en
  grande police sur un écran étroit, il rapetisse en mots entiers plutôt que de couper un mot ; P7.25),
  et le disque « Serrures » (48 dp, P7.36), qui dessine le cadenas, anse levée une fois ouverte, et ouvre
  l'écran « Serrures ». TalkBack lit le disque « Toutes les serrures », pour ce qu'il ouvre, non pour l'état :
  c'est le titre de la barre, qui garde son rôle de titre, qui dit l'état, lu en casse normale,
  « Serrure 03, fermée » ou « Serrure 03, ouverte ». En police ×2, la barre tient à 360 dp (titre aux mots
  entiers, coups sur une ligne, disque entier, sans chevauchement ; capture `LocksmithBarLargeFontDay`). Les
  deux dernières pilules des accords portent une accolade et le mot « cadence ». La tuile choisie et son anneau
  descendent ensemble quand on appuie (ce qu'un écran dessine autour d'un bouton suit le bouton).
  **Tests :** `LocksmithMessageTest › the title of the bar tells the padlock closed until it opens`,
  `LocksmithScreenTest › the title of the bar tells the padlock closed, then open, the disc being read for what it opens`,
  `LocksmithScreenTest › the disc of the bar, read « Toutes les serrures », opens the padlocks`,
  `LocksmithScreenTest › the back button of the bar goes back`,
  `LocksmithScreenTest › the title of the bar is in capitals, its number on two digits, TalkBack reads it in words`,
  `LocksmithScreenTest › in a font twice as large, on 320 dp, the title of the bar keeps its words whole`,
  `LocksmithScreenTest › in a font twice as large, on 360 dp, the bar holds its title, its moves and a disc of 48 dp`,
  `BloxNavDisplayTest › on the Locksmith, the back button of the bar goes back to the cartridges`,
  `BubbleButtonTest › what a screen draws around a button, as the ring of a chosen tile, goes down with it`.
- Aperçu sur l'anneau en colonne 0 ou 9 : quand la place manque au-delà du rebord, la case qui déborde est
  rognée à 4 dp du bord de l'écran, sa croix entière dedans ; avec assez de place, elle est entière. La case
  rognée garde sa hauteur, son joint et ses coins. Sa croix, du même trait que celle d'une case entière, vaut
  le remplissage moins le halo et 2 dp de chaque côté (11 dp à 390 dp, 14 dp à 320 dp), en pixels entiers,
  plafonnée à la croix d'une case entière ; au repos, croix et halo gardent 2 dp de fond de chaque côté. Si la
  croix tombait sous 10 dp (écrans étroits à forte densité), la case est rognée plus près du bord, à 3 dp au
  plus près ; si même là elle reste sous 10 dp, la case est rognée à 3 dp et la croix est la plus grande
  possible, sans amincir le trait. Exception à la marge des écrans du design system, notée dans son README.
  Pendant le tremblement d'un refus, la case de côté tremble avec la serrure : son bord intérieur suit sa
  voisine, son bord extérieur ne s'approche jamais du bord de l'écran plus qu'au repos (elle s'étrécit, en
  pixels entiers), sa croix garde sa taille et se recentre sur ce qui est montré. Au pic du tremblement, le
  halo de la croix couvre le contour de la case pendant quelques images : toléré (décision de P7.23), la
  croix et son halo restent entiers dans la case montrée.
  **Tests :** `PadlockSizesTest › at 390 dp, a cell of the preview on the ring at the side is cut 4 dp from the edge, its cross whole within`,
  `PadlockSizesTest › with room enough beyond the rim, a cell of the preview on the ring at the side is whole`,
  `PadlockSizesTest › a cell of the preview on the ring at the side never passes the edge of the screen less the margin`,
  `PadlockSizesTest › the cross of a cell cut at the side keeps 2 dp of its fill on each side of its halo, and 10 dp at least`,
  `PadlockSizesTest › on the narrowest screen, 320 dp, the cross of a cell cut at the side is 14 dp`,
  `PadlockSizesTest › where its cross would fall under 10 dp, a cell at the side is cut a little closer to the edge, 3 dp at least`,
  `PadlockSizesTest › where even 3 dp from the edge its cross falls under 10 dp, a cell at the side is cut 3 dp from it`,
  `PadlockSizesTest › shownAt keeps the inner edge of a cell at the side where the padlock puts it, at rest or shaken`,
  `PadlockSizesTest › shownAt trims a cell at the side by whole pixels, its outer edge never closer to the edge than at rest`,
  `PadlockSizesTest › shaken by a fraction of a pixel, a cell at the side is trimmed by the whole pixel above`,
  `PadlockSizesTest › the narrowest a cell at the side is drawn is reached at the height of the shake, never passed during it`,
  `PadlockSizesTest › at the peak of the shake, the cross of a side cell and its halo stay within what is shown, joints included`,
  `PadlockSizesTest › at 390 dp, a cell at the side stands 4 dp from the edge of the screen at rest`,
  `LockCutCrossTest › a cell of 42 cut to 23, its cross takes its fill less its halo and 2 dp on each side`,
  `LockCutCrossTest › at rest, the cross of a cut cell and its halo keep 2 dp on each side within its joints`,
  `LockCutCrossTest › a cell cut wide enough keeps the cross of a whole cell, across half its fill`,
  `LockCutCrossTest › a whole cell of an odd fill keeps the cross of the cells that are not cut, half a pixel included`,
  `LockCutCrossTest › a whole cell at the side, its cut without a cross of its own, keeps the cross of a cell not cut`,
  `LockCutCrossTest › the cross of a cut cell is a whole number of pixels`,
  `LockCutCrossTest › the cross of a cut cell fits a width as wide as its reach, and no narrower`,
  `LockCutCrossTest › the cross of a cut cell is the same in every Colorful variant and for every piece`,
  `LocksmithScreenTest › at the peak of a shake to the right, the right cell keeps to its neighbour, its cross centred`,
  `LocksmithScreenTest › a cell of the preview at the side, narrowed as the padlock shakes, recomposes nothing`,
  `CellLayoutTest › a cell cut to a narrower width keeps its height, its joint and its corners, its fill as narrow`.
  **Vues :** V35 (captures `LocksmithRefusedLeft`, `LocksmithRefusedRight`, `LocksmithRefusedLeftShake` et
  `LocksmithRefusedRightShake`, en `Day`).
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
  L'écran garde les Effets réduits de l'application (le `BloxTheme` Couleurs les perdait, corrigé). La lumière
  d'une case est peinte par le peintre des cases (`LockCellCanvas`, `core:board`), entre le corps de la case et
  ses marques : le point d'encre de l'indice et la croix d'un refus restent en encre au sommet de la lumière.
  **Tests :** `LocksmithScreenTest › at the height of its swell, the dot of a cell marked by a hint stays in ink`,
  `LocksmithScreenTest › in reduced effects, the cross of a refused preview stays in ink at the height of its light`,
  `LocksmithScreenTest › the grid does not recompose while the cells of a hint swell`.
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
  **Avancement sur la carte** (P7.33, règles d'écriture dans la section [Progression](#progression)) :
  - **Meilleure ouverture** gardée de la serrure : « Meilleure ouverture : *n* coups, *n* refus, *n* indice(s) »
    (« indice » a son pluriel ; « coups » est toujours au pluriel, une serrure ayant 3 pièces au moins, et
    « refus » ne change pas). Elle n'est montrée que si elle diffère de cette ouverture, et seulement une fois
    l'écriture confirmée ou refusée : sinon elle apparaîtrait, puis disparaîtrait (scintillement).
  - **Autocollant** : « Parfaite » pour une ouverture parfaite (un fait de la partie, quoi qu'en dise
    l'avancement), sinon « Nouveau meilleur » quand l'avancement dit une nouvelle meilleure ; Parfaite l'emporte.
    Une première ouverture, une ouverture qui ne fait pas mieux ou non enregistrée n'en portent pas, sauf parfaite.
    Il arrive en rebond (un peu au-delà de sa taille, puis à sa taille à la fin de `motion-sticker`) ; en Effets
    réduits, il est fixe, à sa taille dès la première image.
  - **« Reprendre à la serrure *N* »**, bouton secondaire, quand la serrure suivante est déjà ouverte (après le
    rejeu d'une ancienne) : *N* est la frontière. Rien quand la suivante est la frontière (« Serrure suivante » y
    mène) ou quand la frontière n'est pas connue.
  - **« Résultat non enregistré »** quand l'ouverture n'a pas pu s'écrire.
  - Pour TalkBack, l'autocollant et « Résultat non enregistré », qui arrivent après l'ouverture, sont des régions
    annoncées poliment.
  **Tests :** `OpenCardTest › the card shows the best opening kept of the padlock, when it is not this one`,
  `OpenCardTest › the card leaves the best opening out when it is this one`,
  `OpenCardTest › the best opening waits for the progress to tell what the opening was, so that it does not flicker`,
  `OpenCardTest › a new best bears the sticker NOUVEAU MEILLEUR`,
  `OpenCardTest › a perfect opening bears the sticker PARFAITE, over a new best, whatever the progress said`,
  `OpenCardTest › a first opening, an opening no better or one not recorded bears no sticker, unless it is perfect`,
  `OpenCardTest › the sticker bounces in, a little past its size, and rests at it once motion-sticker is over`,
  `OpenCardTest › in reduced effects the sticker is fixed, at its size from its first frame`,
  `OpenCardTest › after replaying a padlock whose next one is open, the card offers to resume at the frontier`,
  `OpenCardTest › no resume when the next padlock is the frontier, its own button, or when the frontier is not known`,
  `OpenCardTest › an opening that could not be recorded is told on the card`,
  `LocksmithScreenTest › on the card, the best opening kept of the padlock is read, when it is not this one`,
  `LocksmithScreenTest › on the card, a new best and a perfect opening are told by a sticker that TalkBack reads`,
  `LocksmithScreenTest › on the card of an old padlock replayed, Reprendre à la serrure N resumes at the frontier`,
  `LocksmithScreenTest › on the card, an opening that could not be recorded is told`,
  `LocksmithScreenTest › the sticker and the line not recorded, which come after the opening, are polite live regions`.
  **Vues :** V35 (captures `LocksmithOpenPerfectDay`, `LocksmithOpenNewBestDay`, `LocksmithOpenReplayDay`,
  `LocksmithOpenReplayNight`, `LocksmithOpenReplaySmallDay` et `LocksmithOpenUnrecordedDay`, de
  `LocksmithScreenshots.kt`).
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
