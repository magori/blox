# Jardin musical

Le Jardin musical est un nouveau jeu de BloX, un **bac à sable sans échec ni chronomètre** : on
pose les sept pièces sur une grille de notes, et la grille se joue en boucle, de gauche à droite.
Chaque colonne est une croche, chaque ligne une note de la gamme de l'ambiance choisie : toute
pose sonne juste. Comme le [Serrurier](regles-serrurier.md), il mêle les pièces et les rotations
de `core:game` aux gammes, accords et partitions de `core:music`.

Origine : cadrage « quatre modes musicaux » des agents `architecte` et `designer-jeu`
(2026-10-26), décisions du développeur le 2026-10-26. Architecture :
[ADR 0027](adr/0027-le-jardin-musical.md), calqué sur l'[ADR 0025](adr/0025-le-serrurier.md).

> Traçabilité : Aucune règle n'est encore implémentée (phase P10 de [SUIVI.md](../SUIVI.md)).
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
  au plus aigu (ligne 0). La ligne 13 est la **tonique Do 4** (C4) dans toutes les ambiances
  **[à vérifier]** à l'oreille : une gamme de 7 notes couvre alors deux octaves exactes (Do 4 à
  Si 5), la pentatonique près de trois (Do 4 à Sol 6), ce qui peut être trop aigu ; on pourrait
  partir de Do 3 pour la pentatonique.
- Une pièce posée a toutes ses cases dans la grille et ne chevauche aucune autre pièce ; sinon la
  pose est **refusée**, avec sa raison : `OUT_OF_GRID` (l'équivalent du `OUT_OF_HOLE` du
  Serrurier), puis `OVERLAP`, la première l'emportant, comme au Serrurier. Il n'y a pas de limite au nombre de pièces, ni d'état « fini ».
- Pièces : les sept, avec les **orientations du Classique** (`ClassicRotationSystem`) et leur
  pivot ; une pièce donnée commence dans son orientation d'apparition. On en pose autant qu'on
  veut de chaque type.
- Pour les tests, un jardin s'écrit en ASCII, comme une serrure : `.` pour une case libre, la
  lettre du type sur les cases d'une pièce, chaque pièce reconnue par son orientation et son
  pivot ; deux pièces de même type qui se touchent s'écrivent avec un chiffre de rang
  **[à vérifier]** en P10.4.

## Ambiances

Une ambiance fixe la **gamme**, la **suite des 4 accords** (un par groupe de 4 croches) et le
**tempo**. Toutes ont Do pour tonique. Elles reprennent les catégories du
[générateur](generateur-musique.md) (`Category`), sans leur rythme ni leur mélodie :

| Ambiance | `GardenMood` | Catégorie | Gamme | Accords | Tempo |
|---|---|---|---|---|---|
| Joyeux | `JOYFUL` | `JOYFUL` | Do majeur | I – V – vi – IV | 132 temps/min **[à vérifier]** |
| Calme | `CALM` | `CALM` | Do pentatonique majeure | I – vi – IV – V | 84 temps/min |
| Mystère | `MYSTERIOUS` | `MYSTERIOUS` | Do mineur harmonique | i – ♭VII – VI – V | 100 temps/min **[à vérifier]** |
| Épique | `EPIC` | `EPIC` | Do mineur naturel | i – VI – III – VII | 120 temps/min **[à vérifier]** |

- Les accords sont des `Progression` de `core:music` ; leurs noms (pilules) viennent de
  `RomanNumeral.name` (P7.17).
- Le tempo de chaque ambiance est pris dans l'intervalle de sa catégorie ; Calme vaut 84 (la
  conception l'affiche ainsi : « Do pentatonique · 84 bpm »), les autres sont à régler à
  l'oreille.
- Une **note de l'accord** est une case dont la note appartient (à l'octave près) à l'accord de
  son groupe : l'écran la marque d'un point.
- Changer d'ambiance garde les pièces à leur place : les mêmes cases sonnent dans l'autre gamme.
  L'ambiance par défaut est Calme.

## Partition : `Garden.toScore`

`Garden.toScore(mood)` rend la partition du jardin, une `Score` de 2 mesures de 4 temps au tempo
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
  **[à vérifier]** à l'oreille (une liaison donnerait une noire).
- **Durées exactes** : `Score` vérifie que chaque voix dure exactement un nombre entier de mesures
  (longueur `% beatsPerBar == 0.0`, en `Double`). Une croche partagée en 3 (triolet) ne tombe pas
  juste en `Double` : la dernière part prend le **reste** de la croche (croche moins les parts
  précédentes), si bien que la somme vaut exactement la croche. Un test le vérifie
  (`a column of three cells keeps the length of the bar exact`).
- **Deux pièces dans la même colonne** sonnent ensemble, chacune selon sa propre règle.
- **Basse** : la fondamentale de l'accord de chaque groupe, deux octaves sous la tonique, une
  noire par temps, à faible volume **[à vérifier]** : elle fait entendre la suite d'accords même
  sur un jardin vide. La retirer est une option.
- **Sans saturation** : jusqu'à 14 notes peuvent sonner ensemble. Une `Voice` de `core:music` est
  monophonique, à volume constant (`Voice(waveform, volume, notes)`) : la partition a donc **une
  voix par ligne** (14 voix de mélodie) et la basse, chacune à un **volume fixe**, choisi pour que
  14 voix ensemble ne dépassent pas la crête d'une musique générée (point de départ :
  `MELODY_VOLUME` 0,07 et `BASS_VOLUME` 0,16) **[à vérifier]** à l'oreille, avec un test sur
  l'amplitude crête (P10.6). `core:music` ne change pas.
- Un jardin vide rend la partition de la seule basse.
- Les voix et leurs formes d'onde reprennent celles des musiques générées (mélodie et basse).

## Composer un jardin : `GardenComposer`

Le dé « Au hasard » compose un jardin, sur le principe du générateur et du Serrurier : composer
des candidats, évaluer, garder le meilleur.

- `GardenComposer.compose(mood, seed)` est une **fonction pure de la graine**, sur le générateur
  stable `StableRandom` (P1.20) : la même graine et la même ambiance rendent le même jardin.
- Un candidat pose **6 pièces** **[à vérifier]** : pour chacune, des essais d'un type, d'une
  orientation et d'un pivot, gardés si la pose est acceptée ; un candidat qui ne pose pas toutes
  ses pièces est abandonné.
- Note d'un candidat **[à vérifier]**, entière : la part des notes de l'accord (au moins une note
  de l'accord par groupe), la variété des types, l'étalement sur les 4 groupes ; à égalité, le
  premier composé l'emporte. Les poids se règlent en P10.7, à l'oreille.
- 20 candidats par graine **[à vérifier]** ; chaque candidat a son propre `Random`, tiré d'un
  maître.
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
- **Grille** de 16 × 14 cases de 22 dp, dans un cadre `board` ; un groupe de 4 croches sur deux
  en `panel-inset` ; les notes en bulles de pièces (celles du plateau) ; un point `ink` sur halo
  `page` pour les notes de l'accord. Cases sur le modèle de `LockCellCanvas` et `LockCellStyles`.
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

- Hauteur de la grille (Do 3 ou Do 4), tempos, basse, volume des voix, note du composeur : à
  régler à l'oreille (ci-dessus, **[à vérifier]**).
- Export vers « Mes musiques », plusieurs jardins, partage : hors v1.
- Téléphone couché et tablette : la grille de 16 colonnes s'y prête mieux ; à dessiner plus tard.
