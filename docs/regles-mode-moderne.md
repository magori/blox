# Règles du mode Moderne

Le mode Moderne reprend les règles **les plus répandues parmi les jeux récents**, telles que la
communauté les décrit dans sa « ligne directrice » publique (les mécaniques communes aux jeux
sortis depuis le milieu des années 2000). Aucune marque n'est citée : le mode s'appelle
« Moderne », et seules les règles du jeu, c'est-à-dire ses mécaniques, sont reprises.
L'[analyse du mode Moderne](analyse-mode-moderne.md) a fixé le périmètre et les décisions ; un
seul moteur sert les deux modes, ses capacités étant déclarées dans les règles
([ADR 0015](adr/0015-un-moteur-des-capacites.md)).

> Statut des valeurs : chaque valeur cite sa source, des pages de mécaniques du wiki
> communautaire *TetrisWiki* (liste en fin de document). Les valeurs marquées **[à vérifier]**
> n'ont pas de source sûre, ou varient d'un jeu à l'autre : on les confirme avant d'écrire le
> test correspondant, comme dans la spécification du [mode Classique](regles-mode-classique.md).

> Traçabilité : comme pour le Classique, une règle implémentée reçoit une ligne
> **Tests :** `ClasseDeTest › nom du test` et, si elle se voit à l'écran, **Vues :** `V…`.
> Trois règles sont implémentées : la durée de l'effacement (P6.2), le sac de 7 (P6.4) et la
> position d'atterrissage (P6.6).

> Coordonnées : le plateau de BloX a son origine en haut à gauche, les lignes croissant vers le
> bas. Les sources comptent les lignes de bas en haut (ligne 1 en bas) et notent les décalages
> (x, y) avec y positif vers le haut : un décalage (x, y) d'une source devient, dans BloX, une
> colonne + x et une ligne − y.

## Ce que le mode Moderne ajoute au Classique

Sac de 7, file de plusieurs pièces suivantes, rotation SRS avec décalages contre les murs,
pièce fantôme, chute instantanée, délai de verrouillage réinitialisable, réserve, gravité de
plus d'une ligne par tick, T-spins, combos, enchaînements de coups difficiles. Le plateau, les
pièces et les cellules au-dessus du plateau perdues au verrouillage restent ceux du Classique.

## Plateau

- Grille de **10 colonnes × 20 lignes** visibles, comme en Classique.
- **Au-dessus du plateau** : la ligne directrice prévoit une zone de 20 lignes cachées. BloX garde
  ses deux lignes cachées, −2 et −1 (décision de l'analyse) : au verrouillage, les cellules
  restées au-dessus du plateau visible sont **perdues** et la partie continue, comme en
  Classique ; les conserver deviendra une option plus tard. Source de la zone de 20 lignes :
  TetrisWiki, *Guideline*.
- **Conséquence voulue des deux lignes cachées** : la ligne −3 bloque, comme en Classique. Un
  décalage SRS vers le haut qui ferait passer une cellule au-dessus de la ligne −2 est donc
  refusé, alors que la ligne directrice, avec ses 20 lignes cachées, l'accepte. Cela n'arrive que
  près de l'apparition, quand la pile est presque au sommet. Cas limite testable : un I qui vient
  d'apparaître (état 0, pivot (5, −1)) et tourne vers R, avec des cellules occupées qui bloquent
  les essais 1 à 4 ; l'essai 5, (+1, +2), le mettrait en colonne 6, lignes −4 à −1. La ligne
  directrice l'accepte ; BloX refuse la rotation.
- **Apparition** : la ligne directrice fait apparaître les pièces sur les deux lignes au-dessus du
  plateau visible (lignes 21 et 22 de la source, −1 et −2 dans BloX), à plat, dans leur état 0,
  centrées, les pièces de 3 de large décalées à gauche, puis les fait descendre d'une ligne
  aussitôt si elles le peuvent. Source : TetrisWiki, *Guideline* et *Super Rotation System*.
  Dans BloX, avec les pivots définis sous *Rotation* :
  - J, L, S, T, Z : boîte de 3 × 3 sur les colonnes 3 à 5, pivot en (4, −1) ; la pièce occupe
    les lignes −2 et −1 ;
  - O : colonnes 4 et 5, lignes −2 et −1, pivot en (5, −1) ;
  - I : boîte de 4 × 4 sur les colonnes 3 à 6, pivot en (5, −1) ; la pièce occupe la ligne −1.
    La source fait apparaître le I « une ligne plus bas » dans les jeux récents que dans les
    plus anciens : la ligne −1 plutôt que −2 est **[à vérifier]**.

## Pièces suivantes

- **File de 3 pièces** visibles par défaut, nombre réglable par le joueur (décision de
  l'analyse). La ligne directrice en montre jusqu'à six, sans valeur par défaut fixée. Les pièces
  y sont montrées dans leur orientation de départ. Source : TetrisWiki, *Guideline*.
- Plage du réglage : **[à vérifier]** (1 à 6 proposé, d'après la même source).

## Tirage des pièces : sac de 7

Les sept pièces sont mélangées dans un « sac », puis tirées dans cet ordre ; un nouveau sac est
mélangé quand le précédent est vide. Chaque pièce revient donc au plus tard après douze autres,
et jamais plus de deux fois de suite. Source : TetrisWiki, *Random Generator* et *Guideline*.

Comme en Classique, le générateur se crée avec une graine, et le sac est le seul état modifiable
du domaine ([ADR 0014](adr/0014-tirage-seul-etat-modifiable.md)). La pièce précédente est
ignorée, contrairement au Classique.

**Implémentée (P6.4)** : `SevenBagRandomizer`. Chaque sac part de l'ordre de base I J L O S T Z,
mélangé par la source d'aléatoire de la graine.

**Tests :** `SevenBagRandomizerTest › the first seven draws are the seven pieces, each once`,
`SevenBagRandomizerTest › each following group of seven draws is again the seven pieces, each once`,
`SevenBagRandomizerTest › two randomizers created with the same seed draw the same sequence`,
`SevenBagRandomizerTest › two randomizers created with different seeds draw different sequences`,
`SevenBagRandomizerTest › at most twelve draws separate two appearances of the same piece`,
`SevenBagRandomizerTest › the previous piece is ignored, unlike in the Classic mode`,
`SevenBagRandomizerTest › a source that swaps no piece gives the bag in its base order I J L O S T Z, not in the order of the enum`,
`SevenBagRandomizerTest › the seed 42 gives these first fourteen pieces, an alarm if Kotlin changes Random or shuffled`.

## Rotation : SRS

- Quatre états par pièce : 0 (départ), R (un quart horaire), 2 (demi-tour), L (un quart
  anti-horaire). Rotations horaire et anti-horaire.
- Une rotation essaie **cinq positions** dans l'ordre : la rotation sur place, puis quatre
  décalages. La première qui tient est gardée ; si aucune ne tient, la rotation est refusée.
- **Le O ne tourne pas** visiblement : pas de décalage.
- Tables de décalage, en (x, y) avec y positif vers le haut, essais 1 à 5. Source : TetrisWiki,
  *Super Rotation System*.

**J, L, S, T, Z**

| Rotation | Essai 1 | Essai 2 | Essai 3 | Essai 4 | Essai 5 |
|---|---|---|---|---|---|
| 0 → R | (0, 0) | (−1, 0) | (−1, +1) | (0, −2) | (−1, −2) |
| R → 0 | (0, 0) | (+1, 0) | (+1, −1) | (0, +2) | (+1, +2) |
| R → 2 | (0, 0) | (+1, 0) | (+1, −1) | (0, +2) | (+1, +2) |
| 2 → R | (0, 0) | (−1, 0) | (−1, +1) | (0, −2) | (−1, −2) |
| 2 → L | (0, 0) | (+1, 0) | (+1, +1) | (0, −2) | (+1, −2) |
| L → 2 | (0, 0) | (−1, 0) | (−1, −1) | (0, +2) | (−1, +2) |
| L → 0 | (0, 0) | (−1, 0) | (−1, −1) | (0, +2) | (−1, +2) |
| 0 → L | (0, 0) | (+1, 0) | (+1, +1) | (0, −2) | (+1, −2) |

**I**

| Rotation | Essai 1 | Essai 2 | Essai 3 | Essai 4 | Essai 5 |
|---|---|---|---|---|---|
| 0 → R | (0, 0) | (−2, 0) | (+1, 0) | (−2, −1) | (+1, +2) |
| R → 0 | (0, 0) | (+2, 0) | (−1, 0) | (+2, +1) | (−1, −2) |
| R → 2 | (0, 0) | (−1, 0) | (+2, 0) | (−1, +2) | (+2, −1) |
| 2 → R | (0, 0) | (+1, 0) | (−2, 0) | (+1, −2) | (−2, +1) |
| 2 → L | (0, 0) | (+2, 0) | (−1, 0) | (+2, +1) | (−1, −2) |
| L → 2 | (0, 0) | (−2, 0) | (+1, 0) | (−2, −1) | (+1, +2) |
| L → 0 | (0, 0) | (+1, 0) | (−2, 0) | (+1, −2) | (−2, +1) |
| 0 → L | (0, 0) | (−1, 0) | (+2, 0) | (−1, +2) | (+2, −1) |

- Le moteur garde déjà le premier candidat qui tient (contrat de `RotationSystem`) : les cinq
  essais sont cinq candidats, dans l'ordre des tables. Un essai (x, y) déplace le pivot de
  x colonnes et de −y lignes.

### Formes des quatre états

Chaque pièce tourne d'un quart de tour autour du centre de sa boîte : 3 × 3 pour J, L, S, T, Z,
4 × 4 pour le I. En état 0, les pièces de 3 de large occupent les deux lignes du haut de leur
boîte (elles « flottent »), et le I la deuxième ligne de la sienne. Source : TetrisWiki, *Super
Rotation System* (états successifs dans le sens horaire, apparition à plat, centre de rotation).

Le **pivot** de BloX est une cellule fixe de la boîte, marquée `@` ; les décalages sont donnés en
(colonne, ligne), la ligne croissant vers le bas.

- **J, L, S, T, Z** : le pivot est le centre de la boîte, qui est aussi une cellule de la pièce,
  le centre de rotation de la source.
- **I** : le centre de sa boîte tombe entre quatre cellules. BloX prend comme pivot la cellule
  (2, 1) de la boîte, colonnes et lignes comptées à partir de 0. Les quatre formes ci-dessous
  reproduisent alors exactement la rotation de la source, et les tables de décalage s'appliquent
  telles quelles. Le pivot n'est qu'un repère : il n'est pas toujours une cellule de la pièce.
- **O** : il ne tourne pas. Ses quatre états ont la même forme, pivot en bas à droite.

Le choix du pivot du I et du O ne change rien à ce que le joueur voit. Il fixe seulement les
coordonnées d'apparition (voir *Plateau*).

| Pièce | 0 | R | 2 | L |
|---|---|---|---|---|
| T | `.T.` `T@T` `...` | `.T.` `.@T` `.T.` | `...` `T@T` `.T.` | `.T.` `T@.` `.T.` |
| J | `J..` `J@J` `...` | `.JJ` `.@.` `.J.` | `...` `J@J` `..J` | `.J.` `.@.` `JJ.` |
| L | `..L` `L@L` `...` | `.L.` `.@.` `.LL` | `...` `L@L` `L..` | `LL.` `.@.` `.L.` |
| S | `.SS` `S@.` `...` | `.S.` `.@S` `..S` | `...` `.@S` `SS.` | `S..` `S@.` `.S.` |
| Z | `ZZ.` `.@Z` `...` | `..Z` `.@Z` `.Z.` | `...` `Z@.` `.ZZ` | `.Z.` `Z@.` `Z..` |
| I | `....` `II@I` `....` `....` | `..I.` `..@.` `..I.` `..I.` | `....` `..@.` `IIII` `....` | `.I..` `.I@.` `.I..` `.I..` |
| O | `OO` `O@` | idem | idem | idem |

Les lignes de chaque dessin se lisent de haut en bas ; `@` est le pivot, occupé par la pièce
sauf pour le I en états 2 et L, où il tombe à côté. Décalages correspondants :

| Pièce | 0 | R | 2 | L |
|---|---|---|---|---|
| T | (−1, 0) (0, 0) (1, 0) (0, −1) | (0, −1) (0, 0) (1, 0) (0, 1) | (−1, 0) (0, 0) (1, 0) (0, 1) | (0, −1) (−1, 0) (0, 0) (0, 1) |
| J | (−1, −1) (−1, 0) (0, 0) (1, 0) | (0, −1) (1, −1) (0, 0) (0, 1) | (−1, 0) (0, 0) (1, 0) (1, 1) | (0, −1) (0, 0) (−1, 1) (0, 1) |
| L | (1, −1) (−1, 0) (0, 0) (1, 0) | (0, −1) (0, 0) (0, 1) (1, 1) | (−1, 0) (0, 0) (1, 0) (−1, 1) | (−1, −1) (0, −1) (0, 0) (0, 1) |
| S | (0, −1) (1, −1) (−1, 0) (0, 0) | (0, −1) (0, 0) (1, 0) (1, 1) | (0, 0) (1, 0) (−1, 1) (0, 1) | (−1, −1) (−1, 0) (0, 0) (0, 1) |
| Z | (−1, −1) (0, −1) (0, 0) (1, 0) | (1, −1) (0, 0) (1, 0) (0, 1) | (−1, 0) (0, 0) (0, 1) (1, 1) | (0, −1) (−1, 0) (0, 0) (−1, 1) |
| I | (−2, 0) (−1, 0) (0, 0) (1, 0) | (0, −1) (0, 0) (0, 1) (0, 2) | (−2, 1) (−1, 1) (0, 1) (1, 1) | (−1, −1) (−1, 0) (−1, 1) (−1, 2) |
| O | (−1, −1) (0, −1) (−1, 0) (0, 0) | idem | idem | idem |

Ces formes et ce choix de pivot sont une proposition fondée sur la source ; ils sont confirmés
par des tests au moment d'écrire `ModernRotationSystem` (P6.4), état par état et rotation par
rotation, avec les essais des tables. Le pivot du I est **[à vérifier]** à ce moment-là : la
cellule (2, 1) de la boîte, ou une autre qui donne les mêmes formes à une translation près.

## Pièce fantôme

Une silhouette montre où la pièce en jeu tomberait si elle descendait tout droit : sa position la
plus basse sans collision. Elle n'interagit avec rien, et son dessin (contour ou cellule
translucide) appartient au thème. Source : TetrisWiki, *Guideline*. Dans BloX, c'est une
fonction pure du plateau et de la pièce, que la chute instantanée réutilise.

**Position d'atterrissage, implémentée (P6.6)** : `Board.landing(piece)` rend la pièce descendue
tout droit aussi bas qu'elle tient, sans traverser de cellule occupée, ou la pièce telle quelle si
elle repose déjà. Une pièce dans les lignes cachées atterrit dans le plateau visible, ou y reste
si la pile atteint le sommet. Une pièce qui ne tient pas là où elle est est refusée, comme par
`lock`. Le plateau et la pièce ne changent pas.

**Tests :** `BoardLandingTest › a piece lands straight below, as low as it fits, onto the stack`,
`BoardLandingTest › a piece already resting on the floor or on the stack is returned as it is`,
`BoardLandingTest › a piece never goes through an occupied cell, even with a hole below it`,
`BoardLandingTest › a piece in the hidden rows lands in the visible board`,
`BoardLandingTest › a piece above a stack reaching the top stays in the hidden rows`,
`BoardLandingTest › a piece that does not fit where it is is refused, as by lock`,
`BoardLandingTest › finding where a piece lands changes neither the board nor the piece`.

## Chute instantanée et descente rapide

- **Chute instantanée** : la pièce descend jusqu'à la position de la pièce fantôme et se
  **verrouille aussitôt**, dans le même tick, sans délai de verrouillage. Source : TetrisWiki,
  *Guideline*.
- **Descente rapide** : la pièce descend plus vite tant que ▼ est tenu, **sans** se verrouiller
  plus tôt. Vitesse : 20 fois la gravité du niveau dans certains jeux, et variable d'un jeu à
  l'autre selon la même source : **[à vérifier]**.

## Gravité

La source donne une courbe, en secondes par ligne au niveau *n* :

    t = (0,8 − (n − 1) × 0,007)^(n − 1)

et la table de vitesses qui en découle, en lignes par image de 1/60 s, c'est-à-dire par tick
(« G »). Source : TetrisWiki, *Marathon* (courbe héritée de la ligne directrice).

**C'est la table qui fait foi, pas la formule.** Chaque valeur est lue comme une fraction
décimale exacte, écrite ici en **millionièmes de ligne par tick**, et le moteur calcule en
entiers : jamais de `Double`.

| Niveau | Millionièmes de ligne par tick | Niveau | Millionièmes de ligne par tick |
|---|---|---|---|
| 1 | 16 670 (0,01667) | 11 | 388 000 (0,388) |
| 2 | 21 017 (0,021017) | 12 | 590 000 (0,59) |
| 3 | 26 977 (0,026977) | 13 | 920 000 (0,92) |
| 4 | 35 256 (0,035256) | 14 | 1 460 000 (1,46) |
| 5 | 46 930 (0,04693) | 15 | 2 360 000 (2,36) |
| 6 | 63 610 (0,06361) | 16 | 3 910 000 (3,91) |
| 7 | 87 900 (0,0879) | 17 | 6 610 000 (6,61) |
| 8 | 123 600 (0,1236) | 18 | 11 430 000 (11,43) |
| 9 | 177 500 (0,1775) | 19 | 20 230 000 (20,23) |
| 10 | 259 800 (0,2598) | 20 | 36 600 000 (36,6) |

- **Accumulation** : à chaque tick, la vitesse du niveau s'ajoute à un compteur entier ; la pièce
  descend d'autant de lignes que le compteur contient de millions, et le reste est gardé pour le
  tick suivant. Exemple au niveau 1 : après 59 ticks, 983 530 ; au 60ᵉ, 1 000 200, la pièce
  descend d'une ligne et il reste 200.
- À partir du niveau 14, la pièce descend de **plus d'une ligne par tick**. À 20 lignes par tick
  et au-delà (niveau 19), elle atteint le sol dès qu'elle apparaît (« 20G »). Pas de plafond de
  niveau (décision de l'analyse).
- **Au-delà du niveau 20**, la table s'arrête et la source n'a pas de règle commune (certains jeux
  raccourcissent le délai de verrouillage) : BloX garde la vitesse du niveau 20, qui suffit à la
  chute immédiate, et le délai de verrouillage normal **[à vérifier]**.

## Délai de verrouillage

- Une pièce posée sur le sol ou sur la pile ne se verrouille qu'après **0,5 seconde**, soit
  **30 ticks**. Source : TetrisWiki, *Guideline*.
- **Réinitialisation par mouvement** : un déplacement ou une rotation réussis remettent le délai à
  zéro, au plus **15 fois**. Au-delà, la pièce se verrouille dès qu'elle touche le sol. Source :
  TetrisWiki, *Guideline* (« Move reset », limité à 15) et *Infinity* (« la plupart des
  jeux depuis 2007 limitent à 15 »).
- Le compteur de réinitialisations repart à zéro quand la pièce descend sous la ligne la plus
  basse qu'elle a atteinte : **[à vérifier]**, aucune des pages consultées ne le dit.
- La chute instantanée verrouille sans attendre ce délai.
- **Autres variantes** citées par la même source (TetrisWiki, *Guideline*), non retenues : la
  réinitialisation sans limite (*Infinity*), et la réinitialisation à chaque ligne descendue
  seulement (*step reset* : seule une descente remet la minuterie à zéro, ni un déplacement ni
  une rotation). Les jeux récents n'utilisent que la réinitialisation par mouvement, limitée à 15.

Cas limites, sans source sûre, proposés pour les tests de `LockDelay` (P6.10) :

- **Au tick 30** : la minuterie compte les ticks passés au sol sans mouvement réussi ; la pièce
  se verrouille au tick où ce compte atteint 30, et non au tick suivant **[à vérifier]**.
- **La pièce quitte le sol puis s'y repose**, par exemple glissée au bord d'une marche puis
  retombée : la minuterie repart de 0 à chaque nouvel appui au sol. Le déplacement qui l'a fait
  quitter le sol a déjà compté comme une réinitialisation **[à vérifier]**.
- **La gravité n'est pas une action** : une descente due à la gravité ou à la descente rapide ne
  consomme aucune des 15 réinitialisations. Seuls un déplacement latéral ou une rotation réussis
  en consomment une **[à vérifier]**.
- **Un mouvement refusé** (contre un mur, rotation sans essai qui tienne) ne réinitialise rien et
  ne consomme rien **[à vérifier]**.

## Réserve

- Le joueur peut mettre la pièce en jeu **en réserve** : si la réserve est vide, la pièce suivante
  apparaît ; sinon, la pièce en réserve prend sa place et réapparaît en haut du plateau, dans son
  orientation de départ.
- **Une fois par pièce** : la réserve ne resservira qu'après le verrouillage de la pièce en jeu.
  Source : TetrisWiki, *Guideline*.
- L'échange fait apparaître la pièce sans délai d'apparition (analyse) ; réserver pendant le
  délai d'apparition (« IHS » de certains jeux récents) n'est pas retenu **[à vérifier]**.

## T-spin

Un T-spin se reconnaît quand un T se verrouille, à deux conditions (règle des **3 coins**) :

1. la **dernière action** sur le T a été une **rotation** réussie ;
2. **trois des quatre cases** en diagonale du centre du T sont occupées.

Puis, parmi les T-spins :

- **T-spin complet** : les deux coins **avant** (du côté où pointe le T) sont occupés, avec au
  moins un coin arrière.
- **T-spin mini** : un seul coin avant est occupé, et les deux coins arrière.
- **Exception** : si la dernière rotation a réussi par l'**essai 5 des tables** (décalage de
  ±1 colonne et ±2 lignes), c'est un T-spin complet, même avec un seul coin avant.

Source : TetrisWiki, *T-Spin*. Selon cette source, contre un mur ou le sol, « les deux coins
arrière sont considérés comme occupés ». Que toute case hors du plateau compte comme occupée
pour les coins avant aussi : **[à vérifier]**.

**Descente après la rotation** : un T tourné, puis descendu d'une ligne ou plus par la gravité,
la descente rapide ou la chute instantanée avant de se verrouiller, a-t-il encore pour
« dernière action » une rotation ? Proposition : une descente annule le T-spin, seul un T
verrouillé sans avoir bougé depuis sa rotation peut en être un **[à vérifier]**.

## Lignes et score

Une ligne pleine est effacée comme en Classique. Les points d'une action sont multipliés par le
**niveau d'avant l'effacement**, contrairement au Classique, qui compte au niveau atteint.
Source de tout ce barème : TetrisWiki, *Scoring* (barème des jeux récents).

| Action | Points × niveau |
|---|---|
| 1 ligne | 100 |
| 2 lignes | 300 |
| 3 lignes | 500 |
| 4 lignes | 800 (difficile) |
| T-spin mini sans ligne | 100 |
| T-spin sans ligne | 400 |
| T-spin mini, 1 ligne | 200 (difficile) |
| T-spin mini, 2 lignes | 400 (difficile) |
| T-spin, 1 ligne | 800 (difficile) |
| T-spin, 2 lignes | 1200 (difficile) |
| T-spin, 3 lignes | 1600 (difficile) |

- **Descente rapide** : 1 point par ligne descendue. **Chute instantanée** : 2 points par ligne.
  Ni l'une ni l'autre n'est multipliée par le niveau, ni par l'enchaînement.
- **Plateau vidé** (*perfect clear*) : bonus en plus des points de la ligne dans les jeux les
  plus récents (800, 1200, 1800, 2000 × niveau ; 3200 pour 4 lignes en enchaînement). Hors du
  périmètre initial **[à vérifier]** : il dépend du jeu.

### Enchaînement de coups difficiles

Un coup **difficile** est un effacement de 4 lignes, un T-spin qui efface au moins une ligne, ou
un T-spin mini qui en efface une ou deux. Un coup difficile qui suit un autre coup difficile vaut
**1,5 fois** ses points (hors points de descente). Seul un effacement de 1, 2 ou 3 lignes **sans**
T-spin rompt l'enchaînement ; un T-spin sans ligne, ou une pièce qui n'efface rien, ne le rompt
pas. Source : TetrisWiki, *Scoring*.

### Combos

Le compteur de combo part de **−1**. Chaque pièce qui efface au moins une ligne l'augmente de 1 ;
une pièce qui n'efface rien le remet à −1. Deux effacements de suite font donc un combo de 1.
Bonus : **50 × combo × niveau**, dès que le compteur dépasse 0, au **niveau d'avant
l'effacement**, comme le reste du barème. Source : TetrisWiki, *Combo* et *Scoring*. Le bonus de
combo n'est pas multiplié par l'enchaînement de coups difficiles (le × 1,5 ne porte que sur les
points de l'action) **[à vérifier]**.

## Niveaux

- La partie commence au **niveau 1** (décision de l'analyse). Choisir un niveau de départ plus
  élevé : la ligne directrice le permet (TetrisWiki, *Guideline*) ; plage proposée
  **[à vérifier]**.
- **Un niveau de plus toutes les 10 lignes**, compté sur le **total cumulé** des lignes de la
  partie, quel que soit le niveau de départ (système à objectif fixe des jeux récents) : on passe
  au niveau *n* + 1 quand le total atteint 10 × *n*. Source : TetrisWiki, *Marathon* (« partir
  du niveau 5 demande toujours 50 lignes pour atteindre le niveau 6 »). Formule :

      niveau = max(niveau de départ, total ÷ 10 + 1)   (division entière)

  Exemples : départ au niveau 1, 9 lignes → niveau 1, 10 lignes → niveau 2, 25 lignes →
  niveau 3. Départ au niveau 5 : 0 à 49 lignes → niveau 5, 50 lignes → niveau 6, 60 lignes →
  niveau 7.
- Pas de plafond : la plupart des jeux s'arrêtent au niveau 15 ou à 150 lignes (même source),
  BloX continue (décision de l'analyse).

## Fin de partie

La partie se termine (source : TetrisWiki, *Top out*) :

- **Apparition bloquée** (*block out*) : la nouvelle pièce apparaît sur au moins une cellule
  occupée, comme en Classique ;
- **Verrouillage hors du plateau** (*lock out*) : une pièce se verrouille **entièrement** au-dessus
  du plateau visible. Une pièce verrouillée en partie au-dessus perd ses cellules cachées, et la
  partie continue (cellules perdues, voir *Plateau*).

La troisième condition de la source (une cellule poussée au-dessus de la zone cachée par des
lignes ajoutées) ne concerne que le jeu à plusieurs, absent de BloX.

## Délais

Délai d'apparition et durée de l'effacement des lignes courts, voire nuls, dans les jeux récents :
valeurs **[à vérifier]**, à fixer avec la politique `LineClearDelay` (P6.2).

**L'effacement des lignes dure au moins 1 tick**, en Moderne comme en Classique : la grille
pleine reste affichée au moins un tick, pour que l'écran puisse animer l'effacement et que
l'événement `LinesCleared` ait son propre tick. Décision prise en P6.2.

**Implémentée (P6.2)** : la durée de l'effacement est la politique `LineClearDelay` des règles ;
le Classique fournit `ClassicLineClearDelay`. La valeur du Moderne reste à fixer (voir plus haut).

**Tests :** `ClassicLineClearDelayTest › the clear lasts 16 ticks plus the ticks up to the next multiple of 4 of the lock tick`,
`ClassicLineClearDelayTest › a negative lock tick is refused`,
`RulesTest › the Classic rules clear lines with the line clear delay of the NES`,
`GameEngineClearTest › the clear lasts as long as the line clear delay of the rules says`.

## Sources

Pages de mécaniques du wiki communautaire *TetrisWiki*, consultées le 2026-10-18 :

- *Guideline* : <https://tetris.wiki/Tetris_Guideline> (plateau, apparition, file, réserve,
  fantôme, chutes, délai de verrouillage, fin de partie) ;
- *Super Rotation System* : <https://tetris.wiki/Super_Rotation_System> (tables de décalage,
  apparition) ;
- *Random Generator* : <https://tetris.wiki/Random_Generator> (sac de 7) ;
- *Marathon* : <https://tetris.wiki/Marathon> (gravité, niveaux) ;
- *Infinity* : <https://tetris.wiki/Infinity> (limite de 15 réinitialisations) ;
- *T-Spin* : <https://tetris.wiki/T-Spin> (règle des 3 coins, T-spin mini) ;
- *Scoring* : <https://tetris.wiki/Scoring> (barème, enchaînement, chutes) ;
- *Combo* : <https://tetris.wiki/Combo> (compteur et bonus) ;
- *Top out* : <https://tetris.wiki/Top_out> (fins de partie).
