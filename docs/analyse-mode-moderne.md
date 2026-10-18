# Analyse du mode Moderne (P6)

Analyse du 2026-10-18, faite sur le moteur à la fin de P1.12. Elle prépare la phase P6 : ce
que le mode doit contenir, ce que le code actuel accueille déjà, ce qui demande de modifier le
moteur, et un découpage en tâches. Aucune règle n'est encore spécifiée : les valeurs citées
sont **[à vérifier]**, à confirmer sur une source avant d'écrire le test.

## En bref

- Sur les quinze éléments du mode Moderne, **cinq s'ajoutent sans toucher au moteur** (sac de
  7, rotation SRS, position d'apparition, niveaux, pièce fantôme). Les autres modifient le
  moteur, `GameState` ou un contrat de politique.
- Changements les plus lourds : le **délai de verrouillage**, la **réserve**, le **score
  contextuel** (T-spin, combos, enchaînements) et un **schéma de commandes** différent de la
  manette NES, aujourd'hui codé en dur dans le moteur.
- La promesse « le mode Moderne s'ajoutera sans modifier le moteur » (`CLAUDE.md`, ADR 0004)
  ne tient pas. Elle est remplacée par : **un seul moteur, dont les capacités sont
  déclarées dans `Rules`**. Le Classique les désactive, et ses 411 tests restent le filet de
  sécurité. Cette décision demande un ADR.
- Découpage : **22 tâches** (P6.1 à P6.22), inscrites dans `SUIVI.md`. Chacune laisse le mode Classique vert.

## Ce que le mode Moderne contient

Ce sont les règles des jeux récents, telles que la communauté les décrit. Le nom de la marque
n'apparaît ni dans le code ni dans l'application.

| Élément | Règle visée | Valeur à confirmer |
|---|---|---|
| Tirage | Sac de 7 : les sept pièces, mélangées, puis un nouveau sac | — |
| Pièces suivantes | File visible de plusieurs pièces | 5 ; 3 sur téléphone ? |
| Rotation | SRS : 4 états, 5 essais de décalage (*wall kicks*) par rotation, table propre au I, O fixe | tables SRS |
| Apparition | Au-dessus du plateau visible, pièces centrées (biais à gauche pour I et O), puis une ligne plus bas si possible | lignes 21–22 (−2 et −1 ici) |
| Pièce fantôme | Montre où la pièce tomberait | — |
| Chute instantanée | La pièce tombe et se verrouille dans le même tick | — |
| Délai de verrouillage | Temps au sol avant verrouillage, réinitialisé par un déplacement ou une rotation, avec un nombre de réinitialisations limité | 30 ticks, 15 réinitialisations |
| Réserve (*hold*) | Échange la pièce en jeu avec la pièce réservée, une fois par pièce | — |
| Commandes | Le dernier sens pressé gagne ; déplacement possible pendant la descente rapide ; descente rapide tant que ▼ est tenu | DAS 10 ticks, ARR 2 ticks |
| Gravité | Courbe en secondes par ligne, plus d'une ligne par tick à haut niveau | `(0,8 − (n − 1) × 0,007)^(n − 1)` s |
| Score | 100 / 300 / 500 / 800 × niveau ; descente rapide 1 par ligne, chute instantanée 2 par ligne | — |
| Bonus | T-spin (complet et mini), combos (50 × combo × niveau), enchaînement de coups difficiles (× 1,5), *perfect clear* | tables de score |
| Niveaux | Un niveau toutes les 10 lignes, départ au niveau 1 | niveaux 1 à 15 ? |
| Fin de partie | Pièce qui ne peut pas apparaître (*block out*) ; pièce verrouillée entièrement hors du plateau visible (*lock out*) | — |
| Délais | Délai d'apparition et effacement des lignes courts, voire nuls | à fixer |

Avec la courbe de gravité, le niveau 13 donne environ 1,1 tick par ligne et le niveau 14
environ 0,7 : au-delà du niveau 13, la pièce doit descendre de plusieurs lignes en un tick.

## Ce que le code accueille déjà

| Élément | Où | Pourquoi ça suffit |
|---|---|---|
| Sac de 7 | `Randomizer` | `next(previous)` peut ignorer `previous`. Le sac est un état du tirage, déjà admis comme seul état modifiable (ADR 0014). |
| Rotation SRS | `RotationSystem.candidates` | Le contrat rend déjà une liste ordonnée de candidats, et le moteur garde le premier qui tient (revue de P1.5). Les états SRS s'écrivent comme quatre `Orientation` autour d'un pivot, y compris le I. |
| Position d'apparition | `Rules.spawnPivot` | Sortie du moteur en P1.12a. |
| Niveaux | `LevelProgression` | `startLevels` et `levelFor` suffisent. La NES commence à 0 et le Moderne à 1 : soit le mode propose `startLevels` à partir de 1, soit l'écran affiche `n + 1`. À trancher. |
| Danger de la pile | `StackDangerRule` | Seuils propres au mode. La garantie « jamais off avant GameOver » tient aux seuils Classique (P1.12e) et sera à revérifier. |
| DAS et répétition | `InputRepeat`, `RepeatTiming` | Valeurs différentes, même forme. Un ARR de 0 (déplacement instantané jusqu'au mur) n'est pas exprimable (`interval ≥ 1`) : on garde 1 tick au plus vite. |
| Pièce fantôme | Fonction pure | Elle se déduit du plateau et de la pièce (descendre tant que `Board.move` réussit). Pas d'état : une fonction du domaine (`Board.dropPosition`) que la chute instantanée réutilise. |

## Ce qui oblige à modifier le moteur

| Élément | Changement | Ampleur |
|---|---|---|
| File de pièces suivantes | `GameState.next: PieceType` devient une file ; `Rules.previewCount` (1 en Classique). `GameScreen` et le panneau latéral lisent la première pièce. | Moyenne : touche l'interface publique et `feature:game`. |
| Effacement des lignes | La durée (16 ticks + alignement sur 4) est codée dans le moteur : la sortir dans une politique (`LineClearDelay`). | Faible, refactoring pur. |
| Chute instantanée | Nouveau bouton, capacité dans `Rules` (absente en Classique), `ScoringRule.pointsForHardDrop`. | Faible. |
| Délai de verrouillage | Aujourd'hui, une descente refusée verrouille aussitôt. Il faut une politique `LockDelay` (0 en Classique, pour un comportement identique) et dans `Phase.Falling` : ticks au sol, réinitialisations restantes, ligne la plus basse atteinte. | Forte : cœur de `drop` et `lock`. |
| Réserve | Nouveau bouton, `GameState.hold` (pièce réservée et « déjà utilisée pour cette pièce ») ; l'échange fait apparaître une pièce sans passer par le délai d'apparition. Capacité absente en Classique. | Moyenne. |
| Commandes | Les règles de la manette NES sont dans `shift` et `drop` : ▼ bloque le déplacement, droite gagne sur gauche, descente rapide seulement sur une nouvelle pression. Les sortir dans une politique `ControlScheme`, Classique et Moderne. | Forte : c'est la partie la plus fine du moteur, et la plus testée. |
| Gravité rapide | `GravityTable.ticksPerRow` ne peut pas descendre sous 1 tick. Contrat à revoir (lignes par tick en fraction, comme les sous-lignes de la NES), ou niveaux du Moderne plafonnés à 13. | Moyenne, ou nulle avec le plafond. |
| Score contextuel | `pointsForLinesCleared(lineClear, level)` ne voit ni le T-spin, ni le combo, ni l'enchaînement. Il faut un contexte de verrouillage (dernière action = rotation, décalage utilisé, coins occupés) et, dans `GameProgress`, un compteur de combo et un indicateur d'enchaînement. Le contrat de `ScoringRule` change (noté au journal en P1.10). Le niveau de calcul diffère aussi : la NES compte au niveau atteint, le Moderne au niveau d'avant. | Forte. |
| Lock out | Une pièce verrouillée entièrement dans les lignes cachées termine la partie. Politique `TopOutRule` ; en Classique, c'est impossible (P1.6). Le Moderne garde-t-il les cellules partiellement cachées ? Cela demanderait de stocker les lignes cachées dans `Board`. | Faible à forte selon la décision. |
| Événements | `PieceHeld`, `HardDropped`, `TSpin`, `Combo`, `BackToBack`, `PerfectClear` pour l'écran et les sons. `GameEvent` est scellé : chaque `when` exhaustif devra traiter les nouveaux cas. | Faible. |
| Statistiques | T-spins, plus long combo, pièces posées, pour V4. `GameStatistics.afterClear` ne suffit plus. | Faible. |

## Architecture proposée

Trois options ont été envisagées :

1. **Un moteur à capacités** (recommandée). `Rules` déclare ce que le mode permet (réserve,
   chute instantanée, nombre de pièces suivantes), et les comportements codés en dur deviennent
   des politiques (`ControlScheme`, `LockDelay`, `LineClearDelay`, `TopOutRule`). Le Classique
   fournit des valeurs neutres. Chaque extraction est un refactoring sous les tests existants,
   avant d'ajouter le Moderne.
2. **Deux moteurs** partageant `model` et `rules`. Le Classique ne bouge plus, mais la chute, le
   verrouillage et l'effacement seraient dupliqués (règle de 2), et le ViewModel devrait
   abstraire deux `GameState`.
3. **Un moteur générique et des étapes branchables** (chaîne de traitements par tick).
   C'est la plus souple, mais aussi la plus abstraite pour deux modes seulement.

L'option 1 reste dans l'esprit actuel : des politiques interchangeables, avec un état
immuable et un moteur par partie. Ce qu'il faut consigner :

- **ADR 0015** « Mode Moderne : un moteur, des capacités » qui amende l'ADR 0004 ;
  promesse de `CLAUDE.md` réécrite (section « Organisation prévue de `core:game` »).
- Nouveaux termes du domaine, à ajouter au glossaire de `CLAUDE.md` : `Hold` (réserve),
  `HardDrop`, `Ghost`, `LockDelay`, `Bag`, `TSpin`, `Combo`, `BackToBack`.
- `ModernRules` et `ModernGameEngineFactory`. `app` choisit la fabrique selon le mode
  enregistré (ADR 0013 inchangé).

## Hors du domaine

| Module | Changement |
|---|---|
| `core:data` | Mode choisi dans les préférences ; **records par mode** (les scores ne sont pas comparables). |
| `feature:home` | *Moderne* activé dans V1 ; niveaux proposés par le `LevelProgression` du mode (déjà le cas). |
| `feature:game` | Cadres de réserve et de file ; fantôme dessiné par `CellStyles`, avec un style par thème (contour ou cellule translucide) ; deux boutons de plus, qui portent l'écran à 7 commandes sur 360 dp, et des gestes, au choix du joueur ; statistiques de V4. |
| `core:audio` | Sons de chute instantanée, de réserve, de T-spin, si les effets sonores existent. |
| Maquette | Nouvelles vues pour le jeu Moderne et sa fin de partie, à faire sur claude.ai (P6.22), avec l'écran Réglages. |

## Découpage

Les 22 tâches (P6.1 à P6.22) sont inscrites dans [SUIVI.md](../SUIVI.md). Elles suivent un
ordre qui garde chaque tâche livrable, testée et sans régression du Classique : les
refactorings (P6.2, la première moitié de P6.10) passent avant les ajouts. P6.10 et P6.13 sont
les plus grosses et se découperont en incréments, comme P1.12 (a à e).

## Décisions

Tranchées le 2026-10-18 :

- **Pièces suivantes** : 3 par défaut, nombre réglable par le joueur.
- **Commandes** : boutons et gestes, au choix du joueur.
- **Cellules hors du plateau visible** : perdues au verrouillage, comme sur la NES, dans un
  premier temps ; conservées en option, réglable plus tard.
- **Niveaux** : le mode Moderne commence au niveau 1.
- **Vitesse maximale** : la gravité dépasse une ligne par tick à partir du niveau 14, jusqu'à
  la chute immédiate au sol ; pas de plafond.
- **Architecture** : un seul moteur pour les deux modes (option 1).
- **Valeurs de référence** : les plus répandues parmi les jeux récents, sourcées dans la
  spécification (P6.1).

Les réglages demandent un écran **Réglages** (« pas encore maquetté » dans
[maquette.md](maquette.md)) et leur enregistrement dans `core:data` : tâches P6.17 et P6.21.
