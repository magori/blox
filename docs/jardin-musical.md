# Jardin musical

Le Jardin musical est un nouveau jeu de BloX, un **bac à sable sans échec ni chronomètre** : on
pose les sept pièces sur une grille de notes, et la grille se joue en boucle, de gauche à droite.
Chaque colonne est une croche, chaque ligne une note de la gamme de l'ambiance choisie : toute
pose sonne juste. Comme le [Serrurier](regles-serrurier.md), il mêle les pièces et les rotations
de `core:game` aux gammes, accords et partitions de `core:music`.

Origine : cadrage « quatre modes musicaux » des agents `architecte` et `designer-jeu`
(2026-10-26), décisions du développeur le 2026-10-26. Architecture :
[ADR 0027](adr/0027-le-jardin-musical.md), calqué sur l'[ADR 0025](adr/0025-le-serrurier.md).

> Traçabilité : le domaine est implémenté au fil de la phase P10 de [SUIVI.md](../SUIVI.md) (`core:garden` : jardin P10.4, ambiances P10.5, partition P10.6, dé P10.7) ; l'écran est dans `feature:garden` (squelette P10.9, écran jouable P10.10).
> Chaque règle recevra sa ligne **Tests :** `ClasseDeTest › nom du test` et, si elle se voit à
> l'écran, **Vues :** `V…`, à mesure qu'elle sera codée.

> Coordonnées : comme le plateau du Classique et la serrure, la grille a son origine en haut à
> gauche, `Position(column, row)`. La colonne est le **rang de la croche** (0 à 15), la ligne la
> **hauteur** : la ligne 0, en haut, est la note la plus aiguë.

## Vocabulaire

| Terme | Code prévu | Sens |
|---|---|---|
| Jardin | `Garden` (`core:garden`) | La grille de 16 × 14 cases et les pièces qui y sont posées. Valeur immuable. |
| Ambiance | `GardenMood` | Une gamme, une suite de 4 accords et un tempo : Joyeux, Calme, Mystère, Épique. |
| Pièce posée | `Piece` (`core:game`) | Un type, une orientation et la position de son pivot, sans type parallèle. |
| Composer un jardin | `GardenComposer` | Le dé « Au hasard » : composer, puis évaluer. |
| Tête de lecture | `LoopPlayHead` (`core:designsystem`) | La colonne jouée, comptée en ticks. |

## Grille

- **16 colonnes × 14 lignes.** 16 croches font **2 mesures de 4/4** ; elles se rangent en
  **4 groupes de 4 croches** (une demi-mesure chacun), un accord par groupe.
- Les 14 lignes sont **14 degrés consécutifs** de la gamme de l'ambiance, du plus grave (ligne 13)
  au plus aigu (ligne 0). La ligne 13 est la **tonique** : **Do 4** (C4) dans toutes les ambiances,
  si bien qu'une gamme de 7 notes couvre deux octaves exactes (Do 4 à Si 5) ; **Do 3** pour Calme,
  dont la pentatonique couvre alors Do 3 à Sol 5 au lieu de monter jusqu'à Sol 6 (décision du
  développeur, 2026-10-27 : valeurs de départ en constantes nommées, réglage à l'oreille plus tard).
  **Tests :** `GardenMoodTest › the bottom row is the tonic, C4 for every mood but the calm one, on C3`,
  `GardenMoodTest › each mood has 14 rows of its scale, the tonic on the bottom row`,
  `GardenMoodTest › a cell off the grid has neither note nor chord`.
- Une pièce posée a toutes ses cases dans la grille et ne chevauche aucune autre pièce ; sinon la
  pose est **refusée**, avec sa raison : `OUT_OF_GRID` (l'équivalent du `OUT_OF_HOLE` du
  Serrurier), puis `OVERLAP`, la première l'emportant, comme au Serrurier. Une pièce peut toucher
  une autre pièce. Il n'y a pas de limite au nombre de pièces, ni d'état « fini ». Une pièce posée
  se reprend (ses cases redeviennent libres ; reprendre une pièce absente ne change rien) ; « Tout
  effacer » vide le jardin. Le jardin (`Garden`, `core:garden`) est une valeur immuable : chaque
  action en rend un nouveau.
  **Tests :** `GardenTest › a piece inside the grid on free cells is placed`,
  `GardenTest › a piece off the grid is refused`,
  `GardenTest › a piece along the edges of the grid is placed`,
  `GardenTest › a piece over a placed one is refused as an overlap`,
  `GardenTest › a piece both off the grid and over a placed one is refused as off the grid`,
  `GardenTest › the piece placed on a cell is found there`,
  `GardenTest › a placed piece is taken back`,
  `GardenTest › taking back a piece that is not placed leaves the garden as is`,
  `GardenTest › clearing the garden leaves it empty`.
- Pièces : les sept, avec les **orientations du Classique** (`ClassicRotationSystem`) et leur
  pivot ; une pièce donnée commence dans son orientation d'apparition. On en pose autant qu'on
  veut de chaque type. Une orientation qui n'est pas une orientation Classique du type est une
  erreur de programmation.
  **Tests :** `GardenTest › a piece takes a Classic orientation of its type`.
- Pour les tests, un jardin s'écrit en ASCII (`Garden.parse`), 14 lignes de 16 caractères : `.`
  pour une case libre, la lettre du type sur les cases d'une pièce. Chaque région d'une même
  marque reliée par les côtés est une pièce, reconnue par son orientation Classique et son pivot.
  Deux pièces de même type qui se touchent : l'une s'écrit avec un chiffre (`1` à `9`), dont le
  type se lit à sa forme (décision P10.4 ; les 19 formes Classiques sont distinctes ; au
  Serrurier, toutes les pièces s'écrivent par leur rang). Une grille d'une autre taille, une marque
  inconnue ou une région qui ne forme pas une pièce de sa marque sont refusées, avec un message
  qui dit où.
  **Tests :** `GardenTest › parse reads the pieces of a garden`,
  `GardenTest › parse reads apart two pieces of the same type that do not touch`,
  `GardenTest › parse tells apart two touching pieces of the same type when one is written with a digit`,
  `GardenTest › parse reads back every Classic piece placed in a garden, by its letter or by a digit`,
  `GardenTest › parse rejects a grid not 16 by 14, an unknown mark, cells that make no piece of their mark`.

## Ambiances

Une ambiance fixe la **gamme**, la **suite des 4 accords** (un par groupe de 4 croches) et le
**tempo**. Toutes ont Do pour tonique. Elles reprennent les catégories du
[générateur](generateur-musique.md) (`Category`), sans leur rythme ni leur mélodie :

| Ambiance | `GardenMood` | Catégorie | Gamme | Accords | Tempo |
|---|---|---|---|---|---|
| Joyeux | `JOYFUL` | `JOYFUL` | Do majeur | I – V – vi – IV | 132 temps/min |
| Calme | `CALM` | `CALM` | Do pentatonique majeure | I – vi – IV – V | 84 temps/min |
| Mystère | `MYSTERIOUS` | `MYSTERIOUS` | Do mineur harmonique | i – ♭VII – VI – V | 100 temps/min |
| Épique | `EPIC` | `EPIC` | Do mineur naturel | i – VI – III – VII | 120 temps/min |

- Les tempos sont les valeurs de départ décidées par le développeur (2026-10-27), en constantes
  nommées, à régler à l'oreille sur un téléphone.
- Les accords sont des `Progression` de `core:music`, déclarées par chaque ambiance (`GardenMood`)
  comme dans le tableau, un accord par groupe de 4 croches (colonnes 0–3, 4–7, 8–11, 12–15) ; un
  test garde le lien avec la catégorie : la suite fait partie des progressions de son harmonie sur
  la gamme de l'ambiance. Leurs noms (pilules) viennent de `RomanNumeral.name` (P7.17).
  **Tests :** `GardenMoodTest › each mood plays 4 chords, one per group of 4 eighths`,
  `GardenMoodTest › each mood plays a progression of its category`.
- Le tempo de chaque ambiance est pris dans l'intervalle de sa catégorie ; Calme vaut 84 (la
  conception l'affiche ainsi : « Do pentatonique · 84 bpm »).
  **Tests :** `GardenMoodTest › each mood plays at its tempo, within the range of its category`.
- Une **note de l'accord** est une case dont la note appartient (à l'octave près) à l'accord de
  son groupe, même quand la fondamentale de l'accord est hors de la gamme (le ♭VII de Mystère) :
  l'écran la marque d'un point.
  **Tests :** `GardenMoodTest › a cell is a note of the chord when its note belongs to the chord of its group`.
- L'ambiance fait partie du jardin (`Garden.mood`, décision P10.5) : changer d'ambiance
  (`withMood`) garde les pièces à leur place, et les mêmes cases sonnent dans l'autre gamme ;
  poser, reprendre et tout effacer gardent l'ambiance. L'ambiance par défaut est Calme
  (`Garden.EMPTY`, `Garden.parse`).
  **Tests :** `GardenTest › changing the mood keeps the pieces where they are`,
  `GardenTest › placing or taking back a piece keeps the mood`,
  `GardenTest › clearing the garden keeps its mood`,
  `GardenTest › an empty garden is calm`.

## Partition : `Garden.toScore`

`Garden.toScore()` rend la partition du jardin dans son ambiance (`Garden.mood`), une `Score` de 2 mesures de 4 temps au tempo
de l'ambiance, jouée **en boucle**. C'est une fonction pure.

- **Une case = une croche** : la note de sa ligne, jouée pendant la croche de sa colonne.
- La règle se lit **colonne par colonne**, pour chaque pièce : dans chaque colonne, les *n* cases
  d'une même pièce se partagent la croche en *n* parts égales, de la plus grave à la plus aiguë
  (**arpège montant**, décision du développeur) ; une seule case joue la croche entière. Ainsi un
  I couché joue 4 croches dans l'ordre de ses colonnes, un I debout 4 notes en une croche (des
  triples croches), un T debout 3 notes en triolet dans sa colonne de 3 cases et une croche
  entière dans l'autre. Presque toutes les pièces mêlent les deux cas (O, S, Z, L, J, T ont au
  moins une colonne de 2 cases).
- Deux cases voisines de la même pièce sur la même ligne font deux croches répétées, sans liaison
  (décision du développeur, 2026-10-27 ; une liaison donnerait une noire).
- **Durées exactes** : `Score` vérifie que chaque voix dure exactement un nombre entier de mesures
  (longueur `% beatsPerBar == 0.0`, en `Double`). La part k sur n de la colonne c commence au temps
  c × 0,5 + 0,5 × k / n ; chaque note ou silence dure de là où en est sa voix jusqu'à sa fin, et non
  sa propre longueur, si bien que les arrondis ne s'additionnent pas : la dernière part d'une croche
  partagée prend le **reste**, et chaque voix dure exactement 8 temps.
- **Deux pièces dans la même colonne** sonnent ensemble, chacune selon sa propre règle ; deux pièces
  sur la même ligne jouent dans la même voix, avec un silence entre elles.
- **Basse** : la fondamentale de l'accord de chaque groupe, une noire par temps, dans l'octave de Do 2
  à Si 2 (deux octaves sous Do 4) pour toutes les ambiances : Do 2 est le bas de la tessiture des
  basses générées (`BASS_RANGE`), et « deux octaves sous la tonique » aurait mis Calme (tonique Do 3)
  en Do 1 (décision P10.6). Elle fait entendre la suite d'accords même sur un jardin vide.
- **Une voix par ligne qui sonne** (14 au plus), de la ligne du haut à celle du bas, puis la basse :
  une `Voice` de `core:music` est monophonique, à volume constant, et la partition ne dépend pas de
  l'ordre de pose. Un jardin vide rend la partition de la seule basse.
- **Sans saturation** : `MELODY_VOLUME` 0,06 par ligne et `BASS_VOLUME` 0,16 : 14 notes et la basse
  ensemble font exactement la pleine échelle, 1,0, que `mix` n'écrête pas, même jardin plein
  (décision du développeur, 2026-10-28, réglage à l'oreille plus tard ; les 0,07 de départ
  donnaient 1,14, au-delà de l'écrêtage). `core:music` ne change pas.
- Les voix et leurs formes d'onde reprennent celles des musiques générées (carrée pour la mélodie,
  triangle pour la basse) ; identité `GARDEN_SCORE_ID`, qui ne désigne aucune musique de la
  bibliothèque.

**Tests :** `GardenScoreTest › a cell plays its note for its eighth`,
`GardenScoreTest › a piece with one cell per column plays its notes column after column`,
`GardenScoreTest › a standing piece plays its notes as a rising arpeggio within the eighth`,
`GardenScoreTest › a column of three cells keeps the length of the bar exact`,
`GardenScoreTest › two pieces in the same column sound together`,
`GardenScoreTest › two pieces on the same row play in one voice, with a rest between them`,
`GardenScoreTest › an empty garden plays the bass alone`,
`GardenScoreTest › the bass plays the root of each chord in the octave from C2, whatever the tonic of the mood`,
`GardenScoreTest › the score lasts two bars of four beats at the tempo of the mood`,
`GardenScoreTest › the score does not depend on the order in which the pieces were placed`,
`GardenScoreTest › fourteen notes at once and the bass reach full scale without going above it`.

## Composer un jardin : `GardenComposer`

Le dé « Au hasard » compose un jardin, sur le principe du générateur et du Serrurier : composer
des candidats, évaluer, garder le meilleur.

- `GardenComposer.compose(mood, seed)` rend un jardin **dans cette ambiance** (`Garden.mood`) ;
  c'est une **fonction pure de la graine**, sur le générateur stable `StableRandom` (P1.20) : la même
  graine et la même ambiance rendent le même jardin. Les tirages restent dans le contrat de
  `StableRandom` (`List.random`, `nextInt(until)`) ; le jardin d'une graine n'est pas figé entre
  versions, puisque les poids de la note se régleront à l'oreille.
  **Tests :** `GardenComposerTest › the same mood and seed give the same garden`,
  `GardenComposerTest › the composed garden is in the mood asked`,
  `GardenComposerTest › different seeds give different gardens`.
- Un candidat pose **6 pièces** (décision du développeur, 2026-10-27, constante `PIECES`) : pour
  chacune, on tire un type, une orientation Classique et un pivot sur une case de la grille, et on
  garde le tirage si la pose est acceptée ; **200 tirages** au plus par pièce
  (`ATTEMPTS_PER_PIECE`, comme au Serrurier). Un candidat qui ne pose pas toutes ses pièces est
  **abandonné**, même après en avoir posé.
  **Tests :** `GardenComposerTest › every seed composes a garden of 6 pieces`,
  `GardenComposerTest › a candidate that does not place all its pieces is given up`,
  `GardenComposerTest › a candidate that places some pieces then misses one is given up`.
- **20 candidats** par graine (décision du développeur, 2026-10-27, constante `CANDIDATES`),
  abandonnés compris ; chaque candidat a son propre `StableRandom`, tiré d'un maître construit sur la
  graine, et ne dépend pas des tirages des précédents. Le jardin rendu est le candidat de meilleure
  note ; à égalité, le premier composé l'emporte. Si les 20 sont abandonnés, c'est une erreur
  (`IllegalStateException`) : 6 pièces ne couvrent que 24 des 224 cases, la pose réussit plus d'une
  fois sur deux, et cet échec relèverait d'un invariant brisé, pas du hasard.
  **Tests :** `GardenComposerTest › the best candidate is kept, the first one among equals`,
  `GardenComposerTest › the composed garden is the best rated of its candidates`,
  `GardenComposerTest › a candidate does not depend on the draws of the previous ones`,
  `GardenComposerTest › when the 20 candidates of a seed are given up the composer fails`.
- La note d'un candidat (`GardenRating`) est **entière**, premier réglage en constantes nommées, à
  affiner à l'oreille : **+10 par case sur une note de l'accord de son groupe** (les 24 cases étant
  fixes, ce compte est leur part), **+30 par groupe contenant au moins une note de son accord**
  (chaque accord doit s'entendre), **+10 par type de pièce distinct** (variété), **−5 par case
  d'écart entre le groupe le plus rempli et le moins rempli** (étalement sur les 4 groupes). Le
  groupe d'une colonne est celui de ses 4 croches (`GardenMood.groupOf`).
  **Tests :** `GardenRatingTest › the notes of the chord of their group are counted cell by cell`,
  `GardenRatingTest › the groups holding a note of their chord are counted`,
  `GardenRatingTest › each type of piece is counted once`,
  `GardenRatingTest › the gap between the fullest and the emptiest group is counted in cells`,
  `GardenRatingTest › the points add 10 per note of the chord, 30 per group holding one, 10 per type, minus 5 per cell of gap`,
  `GardenMoodTest › a column belongs to the group of its 4 eighths`.
- Au hasard remplace le jardin en cours, ambiance gardée.

## Jardin gardé

- Le dernier jardin (pièces et ambiance) est **gardé automatiquement** (DataStore, fichier à lui,
  `garden.bin`, décision du développeur) à chaque pose, reprise, effacement ou changement
  d'ambiance, et retrouvé à l'ouverture. Pas de bouton Enregistrer. Dépôt `GardenRepository`
  (`core:data`, P10.11) : le jardin en `Flow`, vide et calme (`Garden.EMPTY`) tant que rien n'est
  gardé ; `keep` le remplace, ou échoue par une `IOException` (l'écran la rattrape, P10.18).
- **Écran et jardin gardé** (P10.18) : `GardenViewModel` lit le jardin gardé une fois, à
  l'ouverture (seul cet écran l'écrit), et l'écran s'ouvre sur ce jardin ; tant qu'il n'est pas lu,
  l'écran n'a pas de jardin et montre la barre seule. La lecture attend la fin de l'écriture en cours
  (une visite s'ouvre sur le jardin de la précédente), et un jardin adopté pendant la lecture n'est
  pas remplacé par elle. Tout nouveau jardin passe par un point unique, `adopt` : affiché aussitôt,
  puis gardé dans la portée de l'application (`mainScope` d'`AppContainer`), si bien qu'un
  changement fait juste avant de quitter l'écran est gardé ; les écritures passent une à une, dans
  l'ordre des changements et d'une visite à l'autre (un verrou partagé par la fabrique) : le dernier
  jardin adopté est celui qui reste gardé. Une écriture ratée (`IOException` : disque plein, fichier
  d'une version plus récente) laisse le jardin à l'écran, sans planter, et le dernier jardin gardé
  sur l'appareil ; toute autre erreur est un bogue et n'est pas avalée.
  **Tests :** `GardenViewModelTest › the garden opens on the garden kept`,
  `GardenViewModelTest › a garden adopted is shown and kept`,
  `GardenViewModelTest › a garden that cannot be kept leaves the garden on screen`,
  `GardenViewModelTest › a failure other than an unwritable garden is not swallowed`,
  `GardenViewModelTest › the gardens adopted are kept in the order they were adopted`,
  `GardenViewModelTest › a garden adopted while the garden kept is read stays on screen`,
  `GardenViewModelTest › a garden opens once the garden of the previous visit is kept`,
  `GardenViewModelTest › the gardens adopted in two visits are kept in their order`.
- **Pas d'export** vers « Mes musiques » en v1 (décision du développeur).
- **Format versionné** (version 1, amendement de l'ADR 0027) : « BLXG », la version, le nom de
  l'ambiance, le nombre de pièces (56 au plus), puis chaque pièce : nom du type, pivot (colonne,
  ligne), ses 4 cases en décalages depuis le pivot, ligne par ligne (le fichier ne dépend pas de
  l'ordre des orientations). Les noms d'ambiance et de type ne changent plus. À la lecture, chaque
  pièce est reposée dans le jardin comme par le joueur.
- Un jardin illisible (pas un jardin, tronqué, octets après la dernière pièce, ambiance ou type
  inconnus, pièce hors grille, superposée ou de forme étrangère à son type) est d'abord copié dans
  `garden.bin.corrupt`, puis remplacé par un jardin vide et calme, sans erreur à l'écran. Un
  fichier d'une version plus récente (restauré depuis un BloX plus récent) donne un jardin vide mais
  est gardé tel quel : aucune écriture tant que cette version n'est pas là, comme les musiques.
- Inclus dans la sauvegarde du téléphone (décision du développeur, 2026-10-28 : une création du
  joueur, comme « Mes musiques »).
- Pas de records ni de compteurs : rien n'est noté.

  **Tests :** `GardenRepositoryContract › without a garden kept the garden is empty and calm`,
  `GardenRepositoryContract › the last garden kept is the one read, in place of the one before`,
  `GardenRepositoryContract › a garden that cannot be kept fails with an IOException, and the garden kept before is read`,
  `GardenRepositoryContract › the garden read follows each garden kept`,
  `DataStoreGardenRepositoryTest › the garden kept is read back, its mood too`,
  `DataStoreGardenRepositoryTest › an unreadable garden gives an empty one`,
  `DataStoreGardenRepositoryTest › a corrupted garden file is copied aside before it is replaced`,
  `DataStoreGardenRepositoryTest › a garden file of version 1 is read as it was written, a format that must not change`,
  `DataStoreGardenRepositoryTest › a garden is written in version 1, its mood then each piece by its type, its pivot and its cells`,
  `DataStoreGardenRepositoryTest › the cells of a piece are written row by row from the top, whatever the order of its orientation`,
  `DataStoreGardenRepositoryTest › a garden file of a later version gives an empty garden, and is kept for that version`,
  `DataStoreGardenRepositoryTest › a garden file of a version below 1 is corrupted, and gives an empty garden`,
  `DataStoreGardenRepositoryTest › a garden file holding two pieces on the same cells is corrupted, and gives an empty garden`,
  `DataStoreGardenRepositoryTest › a garden file holding what no garden can be is corrupted, and gives an empty garden`,
  `DataStoreGardenRepositoryTest › a garden file that counts billions of pieces gives an empty garden, without allocating them`,
  `DataStoreGardenRepositoryTest › the names of the enums written in the garden file do not change`,
  `BackupRulesTest › from Android 12, the cloud and device backups keep only the record, the settings, the musics, the garden`.

## Son

- **Écouter / Arrêter** (disque de la barre, `ListenButton`, P10.12) : la partition du jardin
  (`Garden.toScore`) se joue en boucle par le lecteur de musique partagé de l'application
  (`MusicPlayer`), comme un aperçu de « Mes musiques » : un seul aperçu à la fois, focus audio
  demandé ; arrêt quand l'écran n'est plus visible (`ON_STOP`, pas sur une simple pause) ou qu'on le
  quitte ; une musique qui s'arrête d'elle-même ou est refusée d'emblée rend « Écouter ». Rien n'est
  joué tant que le jardin gardé n'est pas lu. L'écoute (partition jouée, surveillance de
  `isPlaying`) est partagée avec l'éditeur de musique (`LoopListening`, `core:audio`).
  **Tests :** `GardenListeningTest › Écouter plays the score of the garden in a loop, from its first column`,
  `GardenListeningTest › Arrêter stops the score of the garden, and no column is played`,
  `GardenListeningTest › nothing is listened to while the garden kept is being read`,
  `GardenListeningTest › the score stops once the garden is no longer visible, not on a pause`,
  `GardenListeningTest › the score stops when the garden is left`,
  `GardenListeningTest › a score that stops by itself shows Écouter again`,
  `GardenListeningTest › a score refused at once shows Écouter again`,
  `LoopListeningTest › a score that stops by itself is told, once watched`,
  `LoopListeningTest › a score refused at once is told as soon as it is watched, not before`,
  `LoopListeningTest › stop stops the score, and the score is no longer watched`,
  `GardenScreenTest › Écouter is read Écouter le jardin, plays the garden, then is read Arrêter le jardin and stops it`.
- Pendant l'écoute, chaque changement (pose, reprise, ambiance, Au hasard, Tout effacer, Annuler),
  tous par `adopt`, passe à la nouvelle partition par `Player.switchTo` : à la **mesure suivante, au
  même rang**, sans couper la boucle
  ([musique-vivante.md](musique-vivante.md#changer-de-partition-à-la-mesure)) ; sans écoute, ou après
  un arrêt de la musique, rien n'est joué. Le séquenceur ne garde prêtes que les 4 dernières
  partitions jouées ou basculées (le Jardin en crée une à chaque changement ; la Vivante n'en a que
  deux).
- **Tête de lecture** : pendant l'écoute, la colonne jouée est cernée de haut en bas et la pilule
  de l'accord de son groupe est enfoncée ; elle avance image après image au tempo de l'ambiance
  (`FramePlayHead`, `core:designsystem`, partagé avec l'éditeur de musique ; les images sautées
  sont rattrapées), revient au début après la 16ᵉ colonne, et prend le tempo d'une nouvelle
  ambiance à la mesure suivante, comme la partition. Sans écoute, aucune colonne ni accord n'est
  marqué. Limite connue : la tête suit le temps des images, pas le son réellement entendu (latence
  du lecteur) ; le recalage est la tâche P10.22.
  **Tests :** `GardenListeningTest › the play head moves on, frame after frame, at the tempo of the mood`,
  `GardenListeningTest › the play head loops with the score, after its 16th column`,
  `GardenListeningTest › a new mood moves the play head at its tempo from the next bar, as the score switched to`,
  `GardenListeningTest › a frame without listening moves nothing`,
  `GardenCellsTest › the column played is framed from its top cell to its bottom one, and no other column`,
  `GardenCellsTest › without listening no column is framed`,
  `GardenCellsTest › the chord played is the one of the group of the column played, none without listening`,
  `FramePlayHeadTest › the first frame only sets the time, the next ones move the play head on at the tempo`,
  `FramePlayHeadTest › frames skipped are caught up, every tick of them`.
  **Tests :** `GardenListeningTest › while listening, each change of the garden goes on with its score at the bar line`,
  `GardenListeningTest › without listening, a change of the garden plays nothing`,
  `GardenListeningTest › a change of the garden after the score stopped by itself switches nothing`,
  `ChiptuneSequencerSwitchTest › only the last scores switched to stay ready, those switched to long ago are let go`.
- **Aperçu sonore** : présenter, déplacer ou tourner l'aperçu, choisir une autre pièce ou en
  reprendre une, fait entendre ses notes une fois (une hauteur jouée deux fois ne sonne qu'une
  fois ; les cases hors de la grille ne sonnent pas ; un toucher qui laisse l'aperçu où il est ne
  rejoue rien), en arpège montant, une note toutes les 80 ms (`GardenSounds.NOTE_STEP_MILLIS`),
  chacune un blip carré de 100 ms au volume des lignes effacées (`GardenSounds.note`), valeurs de
  départ à régler à l'oreille (P10.17). Un nouvel aperçu coupe les notes du précédent pas encore
  jouées (`playCues`, `SoundCue`, `core:audio`, partagés avec le Serrurier) ; poser, Retirer et les
  changements du jardin ne jouent pas d'aperçu. Soumis au réglage des effets sonores, lu à
  l'ouverture. `SoundEffectPlayer` n'offre que `warmUp` et `play`, et ses pistes ne sont jamais
  libérées (ADR 0025) : ces effets forment un **ensemble fini**, une note par hauteur de l'**union
  des 4 ambiances** (`GardenMood.allPitches`, toutes sur Do : **25 hauteurs**), préchauffé à
  l'ouverture du jardin ; une note hors de cet ensemble est un bogue, refusée plutôt que jouée sur
  une piste nouvelle. Le nombre de pistes (jusqu'à 25 de plus) est **[à vérifier]** sur un
  téléphone (P10.21).
  **Tests :** `GardenMoodTest › the pitches of the garden are those of the rows of the 4 moods, 25 in all`,
  `GardenMoodTest › every row of every mood is one of the pitches of the garden`,
  `GardenSoundsTest › a note of the preview is a single blip of its pitch on the square, as loud as the lines cleared`,
  `GardenSoundsTest › the notes of the preview follow each other a little before the end of the one before`,
  `GardenSoundCuesTest › a preview plays its notes once, in a rising arpeggio, one note every 80 ms`,
  `GardenSoundCuesTest › a standing piece plays its notes from the lowest, at the bottom of the grid, to the highest`,
  `GardenSoundCuesTest › a note held by several cells of the preview plays once`,
  `GardenSoundCuesTest › the cells of a preview off the grid play nothing, the others play`,
  `GardenSoundCuesTest › the effects warmed up are one note per pitch of the garden, 25 in all`,
  `GardenSoundCuesTest › every note a preview may play, in any mood, is one of the effects warmed up`,
  `GardenSoundCuesTest › a note out of the effects warmed up is a bug, refused rather than played on a new track`,
  `GardenPreviewSoundTest › the notes of the garden are warmed up as it opens, when the settings want the sound effects`,
  `GardenPreviewSoundTest › presenting the preview plays its notes once, as a rising arpeggio`,
  `GardenPreviewSoundTest › moving, turning or choosing another piece plays the notes of the new preview`,
  `GardenPreviewSoundTest › a new preview cuts the notes of the one before not yet heard`,
  `GardenPreviewSoundTest › placing the preview, Retirer or a change of the garden play no preview`,
  `GardenPreviewSoundTest › with the sound effects off, nothing is warmed up nor played`,
  `GardenPreviewSoundTest › taking a piece back plays the notes of the preview it becomes`,
  `GardenPreviewSoundTest › a touch that leaves the preview where it is plays nothing more`,
  `SoundCueTest › the cues play at once or once their wait is over, in the order of their waits`,
  `SoundCueTest › cues cancelled before their wait is over play no more`,
  `SoundCueTest › a cue cannot wait a negative time`.
- Pas de vibration.

## Écran

**Thème : Couleurs v2 seulement** (jour et nuit selon le téléphone), comme le Serrurier
(décision du développeur, 2026-10-26) ; barres système selon cette variante. Conception de
`designer-jeu` (2026-10-26), téléphone debout (390 × 844) ; vue **V40** de la
[maquette](maquette.md). L'écran est dans `feature:garden` (clé `GardenRoute`, entrée `gardenEntry`,
`GardenViewModel` et sa fabrique construite par `app`), sous `ColorfulTheme`.
**Tests :** `SystemBarsTest › the bars of a screen drawn in colorful only follow its colorful variant, whatever the theme chosen`.

**Intention** : un bac à sable où chaque pose sonne juste, sans échec ni chronomètre.

De haut en bas :

- **Barre** : Changer de jeu, « JARDIN MUSICAL », dé (Au hasard), Écouter / Arrêter. Codé (squelette,
  P10.9) : Changer de jeu, qui revient au choix du jeu et ne compte qu'écran actif (`dropUnlessResumed`),
  et le titre « JARDIN MUSICAL », en capitales, lu comme un en-tête ; l'écran s'ouvre sur le jardin
  gardé (voir Jardin gardé).
  **Tests :** `GardenScreenTest › the title of the bar shows JARDIN MUSICAL and is read as a heading`,
  `GardenScreenTest › Changer de jeu is handed to app`,
  `GardenScreenTest › Changer de jeu counts only while the garden is resumed`,
  `BloxNavDisplayTest › changing the game from the musical garden goes back to the cartridges`,
  `GardenViewModelTest › the garden opens on the garden kept`.
  Le dé « Au hasard » (P10.10) et Écouter / Arrêter (P10.12 ; disque triangle lu « Écouter le jardin », carré lu
  « Arrêter le jardin », enfoncé pendant l'écoute) sont grisés tant que le jardin gardé n'est pas lu.
- **Ambiance** : groupe de choix « Ambiance » de 4 segments de 48 dp (`ChoiceSegments`) : Joyeux,
  Calme, Mystère, Épique ; choisir une ambiance garde les pièces à leur place (`withMood`, par
  `adopt`). Sur 360 dp, les 4 segments passent sur 2 lignes.
  **Tests :** `GardenChangesTest › choosing a mood keeps the pieces where they are, heard in that mood, and is kept`.
- **Rangée d'information** : « Do pentatonique · 84 bpm » (nom de la gamme et tempo ; TalkBack dit
  « 84 battements par minute ») et « Tout effacer », grisé sur un jardin vide. Le texte reste à côté
  du bouton tant qu'il tient en 2 lignes de mots entiers, sinon le bouton passe dessous.
  **Tests :** `GardenReadingTest › each mood is told by the name of its scale`,
  `GardenLayoutTest › the information stands beside Tout effacer while it holds on 2 lines, whole words, under it otherwise`.
- **Bande des accords** : 4 pilules (`ChordPill`, partagée avec le Serrurier dans
  `core:designsystem`), chacune centrée sur son groupe de 4 croches, lues d'un trait : « Accords :
  1 majeur, 6 mineur, 4 majeur, 5 majeur. », « 7 bémol majeur » pour le ♭VII de Mystère.
  **Tests :** `GardenReadingTest › a chord is told by its degree, then its accidental, then its quality`.
- **Grille** de 16 × 14 cases de 22 dp (`garden-cell`), dans un cadre `board` ; un groupe de 4
  croches sur deux sur une bande `garden-band` (`gardenBand`), ses cases libres en
  `garden-band-cell` : de jour, `empty-cell` a la valeur de la bande et ne s'y verrait pas.
  Cases dessinées par `GardenCellCanvas` (`core:board`, Couleurs seulement, P10.8), sur le modèle
  de `LockCellCanvas` ; l'écran passe l'état de chaque case : `GardenCell` (libre, note, aperçu),
  sa bande, sa place sous la tête de lecture (`GardenPlayHead`).
  - case libre : la case vide du plateau, ou `garden-band-cell` dans une bande ;
  - note : la bulle de sa pièce, celle du plateau ;
  - note de l'accord de son groupe (notes posées seulement) : point `garden-chord-dot` de 6 dp sur
    un halo `garden-chord-halo` de 2 dp ;
  - aperçu (style partagé avec le Serrurier, `previewStyle`) : bulle estompée sur la case libre où
    il se trouve, plate, cernée d'`ink` de 2 dp ; refusé, barré d'une croix `ink` de 2 dp sur un
    halo `page`, case par case ;
  - tête de lecture : colonne teintée `garden-play-head`, cernée de 2 dp en `ink` (2/22 de la
    case) ; les cases libres sous la teinte, les notes et les aperçus par-dessus, le point et la
    croix au-dessus du cadre.

  Les repères ne reposent pas sur la couleur : le point, le contour, la croix et le cadre se lisent
  à 3:1 au moins par leur encre ou leur halo (WCAG 1.4.11) ; le cadre de la tête de lecture se lit
  contre les cases voisines non teintées et les cases libres teintées, pas contre les notes claires
  de nuit qu'il recouvre. Jetons : `docs/design-system/tokens.json` (`garden-*`), codés dans
  `core:board` ou listés non codés jusqu'à l'écran (P10.10) et aux retours (P10.15).
  **Tests :** `GardenCellStylesTest › a free cell is the empty cell of the board`,
  `GardenCellStylesTest › a free cell of a band has its own colour, apart from the band as the empty cell from the board`,
  `GardenCellStylesTest › a note is a bubble of its piece, as on the board, in a band or not`,
  `GardenCellStylesTest › a note of the chord bears a dot of 6 dp in a cell of 22 dp, on a halo of 2 dp more`,
  `GardenCellStylesTest › the dot of the chord reads on every piece, by its ink or by its halo, and on no free cell`,
  `GardenCellStylesTest › a preview that fits is flat and outlined in ink of 2 dp in a cell of 22 dp`,
  `GardenCellStylesTest › a preview in a band fades over the free cell of the band`,
  `GardenCellStylesTest › a refused preview is crossed out in ink of 2 dp in a cell of 22 dp, on a halo of the page`,
  `GardenCellStylesTest › the cross of a refused preview reads on every piece, by its ink or by its halo`,
  `GardenCellStylesTest › the play head is framed in ink of 2 dp, the frame standing out from the tinted cells by 3 to 1`,
  `GardenCellStylesTest › the frame of the play head stands out from the untinted cells beside the column by 3 to 1`,
  `GardenCellStylesTest › the garden has no style in the Original theme, it is drawn in the Colorful theme only`,
  `GardenCellStylesTest › the band the screen draws behind a group is garden band`,
  `PlayHeadFrameTest › the frame of the play head closes the column at its top cell and runs on below it`,
  `PlayHeadFrameTest › the frame of the play head runs on above and below a cell in the middle of the column`,
  `PlayHeadFrameTest › the frame of the play head closes the column at its bottom cell and runs on above it`,
  `GardenTokensTest › each colour token of the garden is its colour in the code, day and night`,
  `GardenTokensTest › each size token of the garden is its size in the code`,
  `GardenTokensTest › every garden token of tokens json is coded in core board or listed as not coded, with its reason` ;
  captures `GardenCells`, `GardenCellsNight`. Vues : V40.

  À l'écran (P10.10) : cases de 22 dp, plus étroites sur un écran plus étroit pour que les 16
  colonnes tiennent (`gridCellFor`) ; une note posée porte le point de l'accord quand sa note est de
  l'accord de son groupe ; l'aperçu couvre ses cases, chacune barrée quand elle tombe sur une note,
  ses cases hors de la grille n'étant pas dessinées (le message le dit). La grille est une
  collection de 14 rangées et 16 colonnes.
  **Tests :** `GardenLayoutTest › the grid keeps cells of 22 dp where it fits, and narrows them to fit a narrower screen`,
  `GardenCellsTest › every cell of the 16 by 14 grid is shown, a free one free, one of a placed piece a note of its type`,
  `GardenCellsTest › a note is marked of the chord when its note belongs to the chord of its group`,
  `GardenCellsTest › the preview covers its cells, each crossed out over a note, those off the grid left out`,
  `GardenScreenTest › the grid is a collection of 14 rows and 16 columns`.
- **Message** sur 2 lignes, en `ink`, lu par TalkBack quand il change : la raison du refus de
  l'aperçu, hors de la grille avant case prise (« L'aperçu sort de la grille : déplace-le ou tourne
  la pièce. », « Une case est déjà prise : déplace l'aperçu ou tourne la pièce. ») ; sinon, avec un
  aperçu, « Touche l'aperçu ou Poser pour la poser ; tourne-la si tu veux. » ; sinon « Choisis une
  pièce, puis touche la grille pour la poser : chaque pose sonne juste. » sur un jardin vide,
  « Touche la grille pour poser la pièce choisie, ou touche une note pour la reprendre. » sinon.
  **Tests :** `GardenMessageTest › without a preview, an empty garden says to choose a piece and touch the grid`,
  `GardenMessageTest › without a preview, a garden with pieces says a note can be taken back`,
  `GardenMessageTest › a preview that fits says how to place it`,
  `GardenMessageTest › a preview over a note says a cell is taken`,
  `GardenMessageTest › a preview off the grid says so`.
- **Bac** de 2 rangées de 4 tuiles de 84 × 52 dp au plus, à 6 dp d'écart : I, O, T, S, Z, J, L
  (`PieceTile`, partagée avec le Serrurier dans `core:board`), groupe de choix « Pièces », la
  choisie (T à l'ouverture) dessinée dans son orientation ; « Retirer » hors du groupe, grisé sans
  aperçu.
  **Tests :** `GardenScreenTest › the tray offers the 7 pieces as one group of choice, the chosen one selected, Retirer apart`.
- **Commandes** : tourner à gauche, **Poser** (grisé sans aperçu), tourner à droite, ancrées en bas.

**Interactions** (P10.10, celles du Serrurier, aperçu pivot sous le doigt) :

- toucher une case présente la pièce choisie en aperçu, pivot sous le doigt ; un autre toucher la
  déplace ; Poser ou un second toucher sur l'aperçu la pose ; un aperçu refusé n'est pas posé et
  reste où il est, et toucher une de ses cases le déplace là (action TalkBack « Déplacer l'aperçu
  ici ») sans jamais reprendre la note qu'il couvre ; choisir une tuile remet la pièce dans son
  orientation d'apparition ; tourner tourne la pièce choisie autour du pivot de l'aperçu ;
- toucher une pièce posée la **reprend** en aperçu, là où elle était, dans son type et son
  orientation ; « Retirer » supprime l'aperçu (la pièce reprise disparaît) ;
- **Tout effacer** et **Au hasard** (`GardenComposer.compose`, ambiance gardée, graine tirée d'un
  `Random` injecté) affichent « Jardin effacé · Annuler » pendant **4 s**, ou plus longtemps si le
  joueur l'a demandé à Android (WCAG 2.2.1) ; Annuler rend le jardin d'avant ; Au hasard sur un
  jardin vide ne propose pas Annuler ; tout changement du jardin retire l'offre, mais choisir une
  pièce, tourner et Retirer la gardent ; tant qu'elle est là, la page laisse de la place sous le bac
  pour que la barre ne couvre aucune tuile ;
- tout changement du jardin passe par `adopt` ; rien n'est fait tant que le jardin gardé n'est pas
  lu ; les retours animés sont décrits plus bas (P10.15) ;
- cases de 22 dp : écart admis aux cibles de 48 dp, comme au Serrurier (la pose se fait par
  l'aperçu, qu'on ajuste) ;
- TalkBack lit chaque case « Croche 5, mi : libre » (ou « pièce T », « pièce T, note de
  l'accord », « aperçu de la pièce Z, case prise »), la note sans son octave, écrite comme la gamme
  (« mi bémol » en mineur), avec l'action d'un toucher (Poser, Reprendre, Déplacer l'aperçu ici,
  Montrer la pièce ici) ; la grille est une collection.

**Tests :** `GardenTapTest › touching a free cell shows the chosen piece as a preview, its pivot on that cell, nothing placed`,
`GardenTapTest › a second touch on the preview places it, kept, and the preview is gone`,
`GardenTapTest › touching a placed piece takes it back as the preview, where it lay, in its type and orientation`,
`GardenTapTest › Retirer takes the preview away, a piece taken back with it`,
`GardenTapTest › Poser on a refused preview places nothing, and the preview stays where it is`,
`GardenTapTest › touching a refused preview over a note moves it there, placing nothing, taking nothing back`,
`GardenTapTest › turning turns the chosen piece a quarter turn round the pivot of the preview`,
`GardenTapTest › turning a piece taken back keeps its pivot where it lay`,
`GardenCellActionTest › a touch places a preview that fits, moves a refused one, takes a note back, shows the piece on a free cell`,
`GardenChangesTest › Tout effacer empties the garden in its mood, kept, and offers Annuler`,
`GardenChangesTest › Annuler gives the garden from before back, kept, and is no longer offered`,
`GardenChangesTest › Annuler is offered for 4 seconds`,
`GardenChangesTest › Au hasard composes a garden in the mood from a seed drawn for this roll, kept, and offers Annuler`,
`GardenChangesTest › Au hasard on an empty garden does not offer Annuler, nothing was cleared`,
`GardenChangesTest › another change of the garden ends Annuler, so that it never undoes more than the garden cleared`,
`GardenChangesTest › choosing a mood or taking a piece back ends Annuler, as any change of the garden`,
`GardenChangesTest › choosing a piece, turning it or Retirer keep Annuler offered, the garden unchanged`,
`GardenChangesTest › nothing is changed nor kept while the garden kept is being read`,
`GardenReadingTest › each row is named by its note in the scale of the mood, from the bottom, flats in the minor moods`,
`GardenReadingTest › the rows of the calm mood are named by the natural notes of its pentatonic scale`,
`GardenScreenTest › each cell is read with its eighth, from 1, its note in the mood, and what it holds`,
`GardenScreenTest › touching a cell shows the chosen piece there, and a second touch on it places it`,
`GardenScreenTest › Tout effacer shows Jardin effacé with Annuler, which gives the garden back`,
`GardenScreenTest › Tout effacer offers Annuler as long as the accessibility of the phone asks`,
`GardenScreenTest › while Annuler is offered, the page scrolls far enough that it covers no tile of the tray` ;
captures `GardenEmptyDay`, `GardenEmptyNight`, `GardenRefusedDay`, `GardenRefusedNight`, `GardenLargeFont`,
`GardenClearedDay`. Vues : V40.

**Retours** (P10.15), chacun avec sa version Effets réduits ([ADR 0024](adr/0024-retours-de-jeu-et-effets-reduits.md)),
cadencés en ticks (`motion-garden-glow`, `-bounce`, `-fall-step`, codés dans `BloxMotion`) et lus au dessin
(`GardenCellEffects`, `GardenCellCanvas`), sans recomposition par image. Le retour d'un changement affiché à
l'arrivée sur l'écran n'est pas rejoué, comme au Serrurier ; les courbes (`fadeOut`, `swell`) et le compteur
d'images (`rememberEffectTick`) sont partagés avec lui (`core:designsystem`).

- **pose** : les bulles de la pièce s'éclairent (`clearGlow` × 0,7) puis s'éteignent en 8 ticks ; de même en
  Effets réduits, un fondu sans mouvement ;
- **refus** : une croix sur chaque case fautive de l'aperçu, fixe (P10.8, P10.10), donc la même en Effets réduits ;
- **tête de lecture** (`LoopPlayHead`, sortie de l'éditeur de musique vers
  `core:designsystem/frame`) : la colonne jouée en `accent-ring` cerné `ink`, ses notes qui
  rebondissent à × 1,1 et reviennent en 6 ticks à chaque croche, la pilule de l'accord en cours remplie ; en
  Effets réduits, pas de rebond, la tête passe de colonne en colonne ;
- **Au hasard** : les pièces tombent une à une, de gauche à droite (première colonne de chaque pièce), avec 2 ticks
  d'écart, chacune d'une case en 8 ticks en accélérant, invisible jusqu'à son tour ; en Effets réduits, toutes
  apparaissent ensemble à leur place en fondu de 8 ticks. Durée et hauteur de chute choisies à l'implémentation,
  à juger avec P10.17 ;
- **changement d'ambiance** : toutes les notes s'éclairent puis s'éteignent en 8 ticks dans la nouvelle ambiance
  (le « fondu »), de même en Effets réduits.

Reprendre une pièce, Retirer, Tout effacer et Annuler ne sont pas animés ; toucher l'ambiance déjà choisie ne change rien.
**Tests :** `EffectCurvesTest › a fade goes from 1 at its first tick down to 0 at its length, nothing before nor after it`,
`EffectCurvesTest › a swell goes from 0 at its first tick up to 1 halfway, back down to 0 at its length, nothing outside it`,
`GardenTapTest › a piece placed is shown placed, for its feedback, each placement a move of its own`,
`GardenChangesTest › Au hasard and the choice of a mood are shown as such, for their feedback, each a move of its own`,
`GardenChangesTest › taking a piece back, Tout effacer and Annuler are changes the screen does not animate`,
`GardenFeedbackTest › a placement lights the bubbles of its piece for 8 ticks, dimmer at each tick`,
`GardenFeedbackTest › in reduced effects a placement lights its piece the same, a fade without a move`,
`GardenFeedbackTest › Au hasard drops its pieces one by one from the first column, each 2 ticks after the one before`,
`GardenFeedbackTest › a piece of Au hasard falls faster and faster to its place, the last one there once the feedback is over`,
`GardenFeedbackTest › in reduced effects the pieces of Au hasard appear together where they lie, fading in over 8 ticks`,
`GardenFeedbackTest › a change of mood fades the whole garden in its new mood over 8 ticks, the same in reduced effects`,
`GardenFeedbackTest › the notes of the column played bounce to 1,1 times their size and back within motion-garden-bounce`,
`GardenFeedbackTest › the notes of a column played long ago are at rest, however many ticks went by`,
`GardenFeedbackTest › a state without a change to animate shows no feedback, and two same changes in a row are two feedbacks`,
`GardenChangesTest › choosing the mood already chosen changes nothing, neither shown, nor kept, nor heard`,
`GardenScreenTest › shown again on a garden rolled before, as on coming back to it, the screen does not replay the fall`,
`GardenFeedbackTest › in reduced effects the notes of the column played do not bounce, the play head going from column to column` ;
captures `GardenPlacedDay`, `GardenPlacedNight`, `GardenRolledDay`, `GardenRolledReduced`, `GardenCellEffects`,
`GardenCellEffectsNight`. Vues : V40.

**Cartouche** « Jardin musical » sur le choix du jeu (V36 à quatre cartouches, P10.13), troisième,
entre le Serrurier et la Graine du jour : elle ouvre le jardin (`GardenRoute`), qui lit lui-même le
jardin gardé. Mascotte : un L debout sur son pied, une feuille au-dessus (corps `piece-l`, feuille
`piece-s`, comme la pousse de la Graine). Parcours Macrobenchmark : choix du jeu, cartouche
(`cartridge_garden`), écran du jardin (`garden_change_game`) ; ajouté au générateur du profil de
référence, à régénérer sur un téléphone (P9.16).
**Tests :** `CartridgeTest › the cartridges are the Classic, the Locksmith, the Jardin musical, the Graine du jour, in this order`,
`CartridgeTest › the tag of the Jardin musical cartridge is cartridge_garden, which benchmark copies as it is`,
`OpenCartridgeTest › the Jardin musical cartridge opens the musical garden over the cartridges`,
`GardenTagsTest › the tag of Changer de jeu stays garden_change_game, which the journey of benchmark copies as it is`,
`MascotLookTest › the Jardin musical mascot is an L standing on its foot, the foot to the right`,
`MascotLookTest › the Jardin musical mascot is a bubble of the L, its leaf in the mint of the S`,
`BloxNavDisplayTest › the Jardin musical cartridge opens the musical garden, and the system back goes back to the cartridges` ;
captures `Cartridges*`, `Mascots*`. Vue V36.

## Points ouverts

- Tempos, basse, volume des voix, note du composeur : valeurs de départ, à
  régler à l'oreille (ci-dessus, **[à vérifier]**).
- Export vers « Mes musiques », plusieurs jardins, partage : hors v1.
- Téléphone couché et tablette : la grille de 16 colonnes s'y prête mieux ; à dessiner plus tard.
