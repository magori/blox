# Jardin musical

Le Jardin musical est un nouveau jeu de BloX, un **bac à sable sans échec ni chronomètre** : on
pose les sept pièces sur une grille de notes, et la grille se joue en boucle, de gauche à droite.
Chaque colonne est une croche, chaque ligne une note de la gamme de l'ambiance choisie : toute
pose sonne juste. Comme le [Serrurier](regles-serrurier.md), il mêle les pièces et les rotations
de `core:game` aux gammes, accords et partitions de `core:music`.

Origine : cadrage « quatre modes musicaux » des agents `architecte` et `designer-jeu`
(2026-10-26), décisions du développeur le 2026-10-26. Architecture :
[ADR 0027](adr/0027-le-jardin-musical.md), calqué sur l'[ADR 0025](adr/0025-le-serrurier.md).

> Traçabilité : le domaine est implémenté au fil de la phase P10 de [SUIVI.md](../SUIVI.md) (`core:garden` : jardin P10.4, ambiances P10.5, partition P10.6, dé P10.7) ; l'écran viendra avec `feature:garden`.
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
  décision du développeur) à chaque pose, reprise, effacement ou changement d'ambiance, et
  retrouvé à l'ouverture. Pas de bouton Enregistrer.
- **Pas d'export** vers « Mes musiques » en v1 (décision du développeur).
- Un jardin illisible (format inconnu) est remplacé par un jardin vide, sans erreur à l'écran ;
  le format porte une version.
- Inclus dans la sauvegarde du téléphone **[à vérifier]**.
- Pas de records ni de compteurs : rien n'est noté.

## Son

- **Écouter / Arrêter** : la partition du jardin se joue en boucle par le lecteur de musique
  (`MusicPlayer`), comme un aperçu de « Mes musiques » : un seul aperçu à la fois dans
  l'application, focus audio demandé, arrêt quand l'écran passe en arrière-plan ou qu'on le
  quitte.
- Pendant l'écoute, chaque changement (pose, reprise, ambiance, Au hasard) passe à la nouvelle
  partition par `Player.switchTo` : à la **mesure suivante, au même rang**, sans couper la boucle
  ([musique-vivante.md](musique-vivante.md#changer-de-partition-à-la-mesure)).
- **Aperçu sonore** : présenter, déplacer ou tourner l'aperçu fait entendre ses notes une fois,
  en arpège montant, par des effets sonores (`SoundEffect`, `SoundEffectPlayer`), soumis au réglage
  des effets sonores. `SoundEffectPlayer` n'offre que `warmUp` et `play`, et ses pistes ne sont
  jamais libérées (ADR 0025) : ces effets forment donc un **ensemble fini, sans libération**,
  l'**union des hauteurs des 4 ambiances** (toutes sur Do, environ 30 hauteurs), préchauffé à
  l'ouverture du jardin. Le nombre de pistes est **[à vérifier]** sur un téléphone.
- Pas de vibration.

## Écran

**Thème : Couleurs v2 seulement** (jour et nuit selon le téléphone), comme le Serrurier
(décision du développeur, 2026-10-26) ; barres système selon cette variante. Conception de
`designer-jeu` (2026-10-26), téléphone debout (390 × 844) ; vue **V40** de la
[maquette](maquette.md) (à dessiner).

**Intention** : un bac à sable où chaque pose sonne juste, sans échec ni chronomètre.

De haut en bas :

- **Barre** : Changer de jeu, « JARDIN MUSICAL », dé (Au hasard), Écouter / Arrêter.
- **Ambiance** : groupe de choix de 4 segments de 48 dp (Joyeux, Calme, Mystère, Épique).
- **Rangée d'information** : « Do pentatonique · 84 bpm » et un bouton « Tout effacer ».
- **Bande des accords** : 4 pilules (noms de `RomanNumeral.name`), alignées sur les groupes de
  4 croches.
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
- **Message** sur 2 lignes, en `ink` : ce qu'on peut faire, ou la raison d'un refus.
- **Bac** de 2 rangées de 4 tuiles de 84 × 52 dp : les 7 pièces (`PieceTile`, partagée avec le
  Serrurier dans `core:board`) et « Retirer ».
- **Commandes** : tourner à gauche, **Poser**, tourner à droite.

**Interactions** (celles du Serrurier, aperçu pivot sous le doigt) :

- toucher une case présente la pièce choisie en aperçu, pivot sous le doigt ; un autre toucher la
  déplace ; Poser (ou un second toucher sur l'aperçu) la pose ; on entend l'aperçu ;
- toucher une pièce posée la **reprend** en aperçu, là où elle était ;
- « Retirer » supprime l'aperçu (la pièce reprise disparaît) ;
- **Tout effacer** et **Au hasard** affichent une barre « Jardin effacé · Annuler » pendant
  **4 s** ; Annuler rend le jardin d'avant ;
- cases de 22 dp : écart admis aux cibles de 48 dp, comme au Serrurier (la pose se fait par
  l'aperçu, qu'on ajuste) ;
- TalkBack lit chaque case « Croche 5, mi : libre » (ou le type de la pièce, et « note de
  l'accord ») ; la grille est une collection.

**Retours**, chacun avec sa version Effets réduits ([ADR 0024](adr/0024-retours-de-jeu-et-effets-reduits.md)),
cadencés en ticks :

- **pose** : les bulles de la pièce s'éclairent pendant 8 ticks ;
- **refus** : une croix sur chaque case fautive de l'aperçu ;
- **tête de lecture** (`LoopPlayHead`, sortie de l'éditeur de musique vers
  `core:designsystem/frame`) : la colonne jouée en `accent-ring` cerné `ink`, ses notes qui
  rebondissent à × 1,1 pendant 6 ticks, la pilule de l'accord en cours remplie ;
- **Au hasard** : les pièces tombent une à une, avec 2 ticks d'écart ;
- **changement d'ambiance** : fondu de 8 ticks.

**Cartouche** « Jardin musical » sur le choix du jeu (V36) : elle ouvre le jardin gardé.

## Points ouverts

- Tempos, basse, volume des voix, note du composeur : valeurs de départ, à
  régler à l'oreille (ci-dessus, **[à vérifier]**).
- Export vers « Mes musiques », plusieurs jardins, partage : hors v1.
- Téléphone couché et tablette : la grille de 16 colonnes s'y prête mieux ; à dessiner plus tard.
