# Suivi de l'implémentation

Une tâche = un incrément livrable, testé, commité. Les identifiants (`P1.3`) servent de
référence dans les commits et avec la commande `/prochaine-tache`.

Légende : `[ ]` à faire · `[~]` en cours · `[x]` terminé · **[à vérifier]** = règle à
confirmer sur une source avant d'écrire le test.

## Vue d'ensemble

<!-- avancement:debut -->
| Avancement | Tâches | % |
|---|---|---|
| **Général** | 110 / 160 | `██████████████░░░░░░` **69 %** |
| **MVP audio** | 22 / 27 | `████████████████░░░░` **81 %** |
<!-- avancement:fin -->

Pourcentages en **nombre de tâches**, pas en effort : le moteur (P1.12) pèse bien plus qu'une
politique. Ils sont recalculés par `scripts/avancement.sh` à chaque fin de tâche, ne pas les
modifier à la main.

**MVP audio** : l'expérience musicale complète. Musique des parties et aperçu, générateur,
éditeur, « Mes musiques » et fichier personnel : la phase P4, les tâches découpées comptées par
tranches, sans les effets sonores optionnels (P4.8), la confirmation des droits hors code
(P4.10) ni les mises à jour de maquette (P3.9, P4.21). Le premier MVP, une partie complète
jouable sur le téléphone (P0 à P3), est atteint depuis le 2026-10-18. Pour changer le
périmètre, modifier la liste ci-dessous.

<!-- mvp-audio: P4.1 P4.2 P4.3 P4.4 P4.5 P4.6 P4.7 P4.9 P4.11 P4.12 P4.13 P4.14 P4.15 P4.16 P4.17 P4.18 P4.19a P4.19b P4.19c P4.19d P4.20 P4.22a P4.22b P4.22c P4.22d P4.23 P4.24 -->

| Phase | Objectif | État |
|---|---|---|
| P0 | Structure du projet et documentation | Build local validé ; reste P0.12 |
| P1 | Domaine du mode Classique (`core:game`) | En cours : reste la rotation, les commandes maintenues et le moteur |
| P2 | Écran de jeu jouable (`feature:game`) | En cours : `GameLoop` fait |
| P3 | Accueil, préférences, records | À faire |
| P4 | Musique et sons | En cours : modèle et partitions faits |
| P5 | Finitions | À faire |
| P6 | Mode Moderne | Plus tard |

---

## P0 — Structure et documentation

- [x] **P0.1** Structure Gradle multi-module, catalogue de versions, plugins de convention.
- [x] **P0.2** Squelette d'application : activité unique, navigation Accueil → Jeu, thème BloX.
- [x] **P0.3** Documentation : architecture, règles, thèmes, musique, maquette, ADR 0001 à 0008.
- [x] **P0.4** CI GitHub Actions (tests, lint, APK debug).
- [x] **P0.5** **Premier build local.** Wrapper Gradle 9.8.1, catalogue mis à jour (AGP 9.4.1,
  Kotlin 2.4.21, BOM Compose 2026.09.00, `compileSdk` 37), `build-logic` adapté au DSL d'AGP 9
  et au Kotlin intégré (ADR 0010). Tests, APK debug et lint verts.
- [x] **P0.6** Analyse statique : ktlint 1.8 (plugin `org.jlleitschuh.gradle.ktlint` 14.2) appliqué par
  les plugins de convention, réglé dans `.editorconfig` (style IntelliJ, composables en
  PascalCase, paramètres sur plusieurs lignes dès deux) ; règles lint Compose de Slack 1.6.1
  (`lintChecks`) sur les modules Compose ; `ktlintCheck` en CI.
- [x] **P0.7** Définition de terminé (`CLAUDE.md`), commande `/fin-de-tache`, revue
  indépendante par sous-agent, vues de maquette numérotées V1 à V4.
- [x] **P0.8** JUnit 5 partout : plugin `de.mannodermaus.android-junit` dans les plugins de
  convention, JUnit 4 retiré du catalogue, ADR 0009.
- [x] **P0.9** Plugin `dependency-analysis` 3.19.2 configuré à la racine, appliqué par les plugins de
  convention, `buildHealth` en CI : une dépendance de production sans usage fait échouer le
  build (sources de test ignorées, Compose regroupé). Retirées : `feature:home` → `core:audio`,
  `core:data` ; `feature:game` → `core:game`, `core:audio`, `core:data` ; `app` → `core:audio`,
  `core:data`, `core-ktx` ; `core:audio` → `core:music` ; `core:data` → DataStore ; coroutines
  partout. Elles reviennent quand le code les exige. `serialization-json` remplacé par `-core`.
- [x] **P0.10** `explicitApi()` strict sur les modules Kotlin purs ; `allWarningsAsErrors.set(false)`
  retiré (valeur par défaut). ADR 0003 amendé.
- [x] **P0.11** Mémoire Gradle : `org.gradle.jvmargs` à 4 Go avec ParallelGC, `kotlin.daemon.jvmargs`
  à 2 Go.
- [x] **P0.12** Mises à jour automatiques de dépendances : Dependabot ou Renovate configuré pour
  `gradle/libs.versions.toml` et le wrapper, PR vérifiées par la CI. Fait : Dependabot (`.github/dependabot.yml`), Gradle et GitHub Actions, groupes AGP/Kotlin/Gradle, Compose, DataStore/Okio, tests, ktlint, AndroidX, kotlinx ; règle de fusion dans CLAUDE.md. À vérifier à la première PR : regroupement du wrapper (nom `gradle-wrapper`) et préfixe avec emoji.
- [x] **P0.14** Doublures de test partagées : `testFixtures` de `core:game` (`ScriptedRandomizer`,
  helpers d'état) pour que `feature:game` ne les recopie pas (revue de P2.2). Fait :
  `ScriptedRandomizer` (strict, et `cycling`) et `spawned` dans `core/game/src/testFixtures`.
- [x] **P0.15** Doublures de `core:data` partagées : `FakeUserPreferencesRepository` et
  `FakeHighScoreRepository` existent dans `feature:game` et `feature:home` ; les placer dans des
  `testFixtures` de bibliothèque Android (revue de P0.14). Revue qualité (B9) : une seule
  `MainDispatcherExtension` partagée, et un test de contrat `HighScoreRepositoryContract`
  exécuté sur le faux et sur `DataStoreHighScoreRepository` (le faux du jeu ne refuse pas un score
  négatif et ne met pas le record à jour).
- [x] **P0.13** **Navigation 3** retenue (ADR 0011) : `navigation3-runtime` / `navigation3-ui` 1.2.0 et
  `lifecycle-viewmodel-navigation3` au catalogue, Navigation Compose retirée. Chaque feature expose
  une clé `NavKey` et une extension d'`EntryProviderScope` ; `app` possède la pile
  (`BloxNavDisplay`). Skills Android officiels copiés dans `.claude/skills/`.

## P1 — Domaine du mode Classique (`core:game`)

Référence : [docs/regles-mode-classique.md](docs/regles-mode-classique.md). Tout en TDD.

- [x] **P1.1** `Position`, `Board` immuable 10×20 : cellule vide/occupée, hors limites,
  `Board.parse(ascii)` pour les tests.
- [x] **P1.2** Formes des 7 pièces dans chaque orientation (l'énumération `PieceType` existe depuis P1.1).
  Cas : nombre de cellules, nombre d'états par pièce (O : 1 ; I, S, Z : 2 ; T, J, L : 4).
  Orientations de départ confirmées par la ROM (table $8A9C).
- [x] **P1.3** Placement d'une pièce : collision avec les bords, le fond et les cellules occupées.
  Lignes cachées −2 et −1 libres (source ROM) ; `Piece`, `Board.fits(piece)`.
- [x] **P1.4** Déplacements gauche / droite / bas, refusés en cas de collision. `Direction`,
  `Board.move(piece, direction): Piece?`, `Position.plus`.
- [x] **P1.5** `ClassicRotationSystem` : rotations horaire et anti-horaire, refus sans décalage.
  `Rotation`, `RotationSystem.candidates`, `Board.place`.
- [x] **P1.6** Verrouillage : la pièce rejoint le plateau quand elle ne peut plus descendre.
  `Board.lock(piece)` : cellules cachées perdues (ROM), refus d'une pièce qui ne tient pas.
- [x] **P1.7** Effacement des lignes : 1 à 4 lignes, lignes non contiguës, chute des lignes
  supérieures. `Board.clearFullRows(): RowsCleared`.
- [x] **P1.8** `ClassicRandomizer` avec graine : un seul nouveau tirage si 7 ou répétition
  (règle TetrisWiki, ordre ROM, biais de la ROM non reproduit). Source d'aléatoire scriptée
  dans les tests. ADR 0014.
- [x] **P1.9** `ClassicGravityTable` : table des ticks par ligne, niveaux 0 à 29+ ; type `Level`.
- [x] **P1.10** `ClassicScoringRule` : 40/100/300/1200 × (niveau + 1), points de descente rapide.
- [x] **P1.11** `ClassicLevelProgression` : premier passage selon le niveau de départ, puis
  toutes les 10 lignes. Cas : départs 0, 5, 9. Type `Lines`.
- [x] **P1.12** `GameEngine` : machine à états (apparition, chute, verrouillage, délai
  d'apparition, effacement, fin de partie). `tick()` et `apply(Input)` renvoient un nouvel état
  et des événements. Issu de la revue de P1.11 : `LevelProgression` expose sa plage de départ
  et la partie valide le départ une seule fois, à sa création (l'écran de choix lira la même
  plage) ; `GameState` garde le niveau de départ, le niveau est recalculé à chaque effacement ;
  `Lines` reçoit son addition ; points comptés avec le niveau **après** la montée (source ROM,
  voir la spécification). Issu de la revue de P1.8 : le moteur possède le `Randomizer` (ADR 0014)
  et le crée depuis la graine ; à l'apparition, la pièce suivante est tirée avec `previous` =
  la pièce qui vient d'apparaître ; une partie commence par deux tirages (`null`, puis la
  première pièce). Un test du moteur le prouve. Issu de P1.14 : le moteur ajoute au délai
  d'apparition l'animation d'effacement (5 passes, une toutes les 4 images, 17 à 20 images
  selon l'image du verrouillage, **[à vérifier]**) ; un test du moteur fige le cas du I
  vertical verrouillé pivot en ligne 18 (cellules 16 à 19) : 10 images. Issu de P1.13 (ROM,
  meatfighter.com), un test du moteur par point : un seul `AutoRepeat` pour ◀ et ▶, recréé
  quand la direction change (tout nouvel appui remet le compteur à zéro), un autre pour ▼ ;
  ▶ l'emporte si ◀ et ▶ sont appuyés ensemble ; ▼ maintenu bloque le déplacement latéral, ◀ ou ▶
  maintenu empêche la descente rapide et remet à zéro son compteur et ses points ; au plus une
  descente par image, un pas de descente rapide remet la gravité à zéro ; points de descente
  rapide = pas réellement faits d'affilée, passés à `pointsForSoftDrop` au verrouillage (le pas
  refusé qui verrouille compte-t-il ? **[à vérifier]**) ; `AutoRepeat.charged()` après un
  déplacement latéral refusé ; DAS figé pendant le délai d'apparition, compteur de descente
  rapide remis à zéro à l'apparition. Revue de P1.5 : rotation par
  `rotationSystem.candidates(piece, rotation).firstNotNullOfOrNull(board::place)` ; événement
  de rotation seulement si la pièce change (O) **[à vérifier]**. Revue de P1.7 : pendant
  l'animation d'effacement, `GameState` garde le plateau verrouillé (non effacé) et le
  `RowsCleared`, et ne publie le plateau effacé qu'à la fin. Découpe validée, une revue et une
  PR par incrément (P1.12 n'est cochée qu'après le dernier) :
  - [x] P1.12a État et apparition : `GameState` (constructeur interne, pièce portée par la
    phase), `Rules` / `ClassicRules` (politiques et position d'apparition), `GameEngine(rules,
    randomizer)`, `GameEngineFactory.create(seed)` (un moteur par partie, ADR 0014),
    `start(niveau de départ)`, deux tirages, apparition pivot en (5, 0), fin de partie.
  - [x] P1.12b Gravité et verrouillage : `tick(state): GameUpdate`, `Phase.Falling(piece,
    ticksSinceDrop)`, `Phase.EntryDelay(ticksLeft)`, `GameEvent.PieceLocked`, `Rules` reçoit
    `gravityTable` et `entryDelay`. Conforme à la ROM au tick près.
  - [x] P1.12c Effacement, score et niveau : `Phase.Clearing`, animation de 17 à 20 ticks
    (compteur de la partie `tickCount`, écart assumé), points au nouveau niveau (`Points`),
    `LinesCleared`, `StackDanger` au franchissement (à la fin de l'animation s'il y a
    effacement), « 4 lignes » dans les statistiques. Pour l'écran : le domaine fournira la
    progression de l'animation (passes faites) quand une tâche la dessinera.
  - [x] P1.12d Commandes : `apply(state, input): GameUpdate`, `Input` scellé (`Press(button)`,
    `Release(button)`, place pour `Pause` / `Resume`), touches tenues et appuis depuis le
    dernier tick (A, B et changement de direction à front montant), `AutoRepeat` de
    déplacement avec sa direction, recréé quand elle change ; compteur de descente rapide
    dans `Falling` ; interactions issues de la ROM (ci-dessus) ; points de descente rapide au
    verrouillage. Revue de P1.12b : les touches tenues et l'`AutoRepeat` latéral vivent dans
    `GameState`, pas dans la phase (le DAS doit survivre au verrouillage et rester figé pendant
    le délai). Revue de P2.5 : un appui bref (A pressé puis relâché entre deux ticks) doit
    tourner la pièce au tick suivant (front montant mémorisé). Revue de P1.12c : le DAS reste
    figé pendant le délai **et** l'effacement ; tout verrouillage, même par descente rapide, a
    lieu dans `tick` (sinon l'alignement de l'animation se décale) ; regrouper niveau, lignes,
    score et statistiques dans un objet « progression » avant d'ajouter les touches à
    `GameState` ; exposer les règles du moteur en lecture seule, pour retirer le système de
    rotation passé à part à la fabrique du ViewModel (P2.2). Fait : `Input`, `Button`,
    `Controls`, `GameProgress`, `GameEngine.rules`, `Rules.inputRepeat`. Revue : un seul
    `AutoRepeat` latéral relancé seulement par un nouvel appui (la ROM continue le compteur
    quand la direction change sans appui, contrairement à la note de P1.13). Pour P2.2 : lire
    `state.progress`, prendre le système de rotation dans `engine.rules`.
  - [x] P1.12e Fin de partie : événement `GameOver`, l'état ne change plus ensuite (le tick
    après la fin est déjà prouvé en P1.12c). La fin de partie n'émet que `GameOver`, jamais
    `StackDanger(off)` (revue de P1.12c). Fait : `GameEvent.GameOver` sans données ; l'écran lit
    le reste dans `GameState`. Pour le mode Moderne (P6) : la garantie « jamais off » tient aux
    seuils de `ClassicStackDangerRule`, une autre règle pourrait émettre `off` au verrouillage.
- [x] **P1.13** Commandes maintenues : DAS 16/6, descente rapide 2/2, DAS chargé contre un mur
  (sources ROM) ; `AutoRepeat`, `InputRepeat`, `ClassicInputRepeat`.
- [x] **P1.14** Délai d'apparition paramétrable (10 à 18 images selon la hauteur) :
  `EntryDelay`, `ClassicEntryDelay`, `Board.ROWS_WITH_HIDDEN`.
- [x] **P1.15** Pause (l'état ne change plus pendant la pause) et statistiques de fin de partie.
  Fait : `GameStatistics` (« 4 lignes », branché en P1.12c), durée = `GameState.tickCount`
  (figé après la fin) ; pause : `GameState.paused`, `Input.Pause` / `Input.Resume`, commandes
  vidées, `tickCount` figé en pause. Pour P2.2 (incrément 3) : `Pause` à l'appui du bouton
  pause et au retour, `Resume` sur Reprendre ; P5.4 envoie `Pause` à `ON_PAUSE`.
- [x] **P1.16** Événement `StackDanger` (pile proche du sommet), pour la musique :
  `Board.stackHeight`, `ClassicStackDangerRule` (15 / 12, **[à vérifier]** en jouant), événement
  émis par le moteur au franchissement (P1.12c). La fin de partie n'émet pas `StackDanger(off)`.
- [x] **P1.17** Type `Ticks` (strictement positif) pour le résultat de `GravityTable.ticksPerRow`
  et d'`EntryDelay.ticksAfterLock`. Revue de P1.12b : une valeur nulle ou négative ne bloque
  rien, elle est confondue en silence avec 1 (descente à chaque tick, apparition au tick
  suivant) ; les phases refusent déjà ces valeurs (`require`), le type reste à faire. Fait : `Ticks` (`model`), rendu par `GravityTable` et `EntryDelay`, porté par `Phase.Clearing.entryDelayAfter` ; les compteurs restent en `Int`.
- [x] **P1.18** Outillage : `ignoreSourceSet` retiré de `dependencyAnalysis`, remplacé par des
  règles par jeu de sources de test (échec, avec exclusion des bibliothèques posées par les
  conventions) ; JUnit 5 déclaré en `-api` / `-params` + moteur en `testRuntimeOnly`.
- [x] **P1.19** Types des points et des « lignes d'un coup » pour `ScoringRule` : `LineClear`
  (lot P1.16 / P1.15) et `Points` (P1.12c, rendu par `ScoringRule`) ; `CLAUDE.md` dit `Points`.
- [ ] **P1.20** Graine stable entre versions, seulement si une graine est gardée ou partagée
  (partie rejouée, défi du jour) : générateur maison dans `rules` et test qui fige les 20
  premières pièces d'une graine (`kotlin.random.Random` ne garantit pas sa suite, ADR 0014).

## P2 — Écran de jeu jouable (`feature:game`)

- [x] **P2.9** ADR 0012 : l'horloge d'images appartient à l'écran (`LaunchedEffect` + `withFrameNanos`
  → `viewModel.onFrame(nanos)`), le ViewModel délègue à une classe pure `GameLoop`. `viewModelScope`
  n'a pas de `MonotonicFrameClock`. ADR 0006 amendé, `docs/architecture.md` et CLAUDE.md à jour.
- [x] **P2.1** `GameLoop` à pas fixe : accumulateur testé avec une horloge factice
  (ADR 0006 et 0012).
- [x] **P2.10** ADR 0013 : la feature définit la fabrique de son ViewModel (dépendances en
  paramètres), `app` la construit depuis `AppContainer` et la passe à l'entrée de navigation
  (`gameEntry(onBack, viewModelFactory)`). Pas de `CompositionLocal`. ADR 0007 précisé.
- [x] **P2.2** `GameViewModel` : `GameUiState`, conversion des actions en `Input`, tests avec
  coroutines-test et Turbine. Ajouter alors `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`,
  `lifecycle-viewmodel-navigation3` (catalogue et plugin de feature) et les deux
  décorateurs de `NavDisplay` (ADR 0011), et envelopper `onPlay` dans `dropUnlessResumed` côté
  feature pour qu'un double appui ne crée pas deux parties. Appeler `GameLoop.reset()` (via le
  ViewModel) en tête du `LaunchedEffect` de l'horloge, sinon la première image après un retour à
  l'écran rend le plafond de ticks (revue de P2.1). Brancher `BoardCanvas` (P2.3) dans
  `GameScreen` : `board` et `piece` du domaine passent tels quels dans `GameUiState`. Brancher
  `SidePanel` (P2.4) : le système de rotation vient des règles du moteur ; `levelText` et
  `linesText` accepteront `Level` et `Lines` si `GameUiState` les porte. Revue de P2.5 : les
  appuis et relâchements arrivés entre deux images sont mis en file et appliqués avant les
  ticks, sans en perdre aucun. Revue de P2.7 : brancher `GameOverScreen` dans `GameScreen` selon
  `GameUiState` (`gameOver: GameOverUiState?`) ; `onMenu` = retour ; `onReplay` remplace la clé
  de la pile par une nouvelle `GameRoute` (ADR 0011). Revue de P2.6 : `GameScreen` met
  `PauseScreen` **à la place** du jeu (écran opaque, plateau masqué), et `GameTopBar` en haut
  du jeu ; `BackHandler` « retour = pause » en jeu, désactivé en pause (retour = Reprendre) et
  en fin de partie ; Menu depuis la pause quitte sans confirmation pour l'instant (une question
  de confirmation est à trancher au test sur téléphone, P2.8) ; record à 0 tant que
  `core:data` n'est pas branché (P3.2).
  Fait (incrément 1) : `GameViewModel`
  (moteur créé depuis la `GameRoute`, `onFrame`, `onFramesStarted`), `GameUiState`, horloge dans
  `repeatOnLifecycle(STARTED)` (ADR 0012 amendé), plateau et colonne latérale branchés
  (`SidePanel(Level, Lines)`), route ajoutée aux `CreationExtras` par défaut, `GameRoute`
  refuse un niveau négatif, `AppContainer` et `newGameRoute` dans `app`. Reste (incrément 2) :
  commandes (après P1.12d), fin de partie et Rejouer (après P1.12e ; `GameOverUiState` construit
  depuis les types du domaine), pause (après P1.15). Fait (incrément 2) : commandes
  (`onControl` → `engine.apply`, sans file : `apply` ne fait qu'enregistrer), système de
  rotation pris dans `engine.rules`, fin de partie (`submit` du score une fois, à l'abri de
  l'annulation et des erreurs d'écriture, puis `GameOverScreen` à la place du jeu), Rejouer
  (`replay` dans `app`, sans effet au second appui), `BloxApplication` qui crée
  l'`AppContainer` (ADR 0007) et le DataStore du record une fois. Fait (incrément 3) :
  `onPause` / `onResume` vers le moteur, `PauseScreen` à la place du jeu, `GameTopBar` avec le
  record suivi (inconnu tant qu'il n'est pas lu, sans plantage si la lecture échoue), retour
  système décidé par `backAction` (pause, reprise, sortie) et appliqué par
  `NavigationBackHandler`. Note de l'incrément 2, « Reste (incrément 3) » : pause
  (`Input.Pause` / `Resume`, `PauseScreen`, `BackHandler`), `GameTopBar` avec le record.
- [x] **P2.3** Rendu du plateau en `Canvas`, styles de cellules par thème (`docs/themes.md`).
  Logique pure testée (styles, géométrie, cellules affichées), `BoardCanvas` sur `drawWithCache`.
  Pour P1.12c : l'animation d'effacement passera par un paramètre de `displayedCells`. Pour
  P2.6 : `BoardCanvas(Board.empty(), piece = null)` masque le plateau.
- [ ] **P2.12** Mettre à jour la maquette : confirmer la taille du carré central des pièces du
  thème Original (8 px sur 24 retenu en P2.3) sur la maquette Claude Design.
- [x] **P2.4** Colonne latérale : score, niveau, lignes, pièce suivante. `SidePanel`,
  `NextPieceCanvas`, `BloxTheme.shapes`.
- [ ] **P2.13** Mettre à jour la maquette : confirmer les mesures des encarts de la colonne
  latérale (cadre, marges, tailles de texte) retenues en P2.4 (`docs/maquette.md`, V2 et V3).
- [x] **P2.5** Commandes tactiles : ◀ ▼ ▶, A, B, avec appui maintenu. `HeldButtons`,
  `GameControls(onChange)`, `BloxShapes.button`. Pas encore branché dans `GameScreen` (P2.2).
- [ ] **P2.14** Mettre à jour la maquette : confirmer la taille, l'espacement et le retour
  visuel des boutons de commande retenus en P2.5 (`docs/maquette.md`, V2 et V3).
- [x] **P2.6** Écran de pause (plateau masqué) et reprise. Revue de P2.5 : à la pause, le
  moteur oublie les touches tenues, pour ne pas reprendre avec un DAS chargé ; le bouton pause
  est dans la barre du haut (maquette), pas dans `GameControls`. Revue de P2.7 : un éventuel
  `BackHandler` « retour = pause » est désactivé en fin de partie ; aucun tick n'est donné au
  moteur pendant la pause, pour que la durée de jeu n'avance pas. Fait : `PauseScreen(onResume,
  onMenu)`, `GameTopBar(record, onPause)` (pictogramme `pauseBars` au pixel près, record par
  `topBarTexts`), `TitledScreen` partagé avec la fin de partie, `PanelLabel`,
  `ScreenSizes.minTouchTarget`. Pause du moteur (P1.15) et branchement (P2.2, incrément 3)
  faits.
- [ ] **P2.16** Mettre à jour la maquette : écran de pause et barre du haut, valeurs retenues
  en P2.6 (`docs/maquette.md`).
- [x] **P2.17** Lisibilité vue sur les captures (P5.7) : touche A enfoncée invisible en thème
  Couleurs (son fond égalait la page) ; cellules en aplat du thème Original fondues entre elles.
  Fait : `keyColors`, jetons `buttonPressed` et `accentPressed`, lettres A et B en gras ; joint
  de 1/24 de la couleur du fond (`cellLayout`, `CellPainter(background)`).
- [ ] **P2.21** Mettre à jour la maquette : touches tenues (teinte « pressée »), lettres A et B
  en gras, joint des cellules du thème Original (P2.17) ; cadre `buttonOutline` `#646B87` des
  touches au repos en thème Couleurs, V3 (P2.22).
- [x] **P2.22** Contraste des flèches au repos en thème Couleurs : `button` `#2A2E3E` sur la page
  `#12141C` ne fait que 1,36:1, sous le 3:1 des composants (revue de P2.17). Fait : jeton
  `buttonOutline`, cadre de 2 dp autour des touches au repos (3,50:1).
- [ ] **P2.23** Contraste du bouton pause en thème Couleurs : même défaut que les flèches (1,36:1
  contre la page) ; lui donner le cadre `buttonOutline` (`topbar/GameTopBar.kt`, `PauseButton`).
- [ ] **P2.18** Police ×2 vue sur les captures (P5.7) : « SUIVANTE » rogné en « SUIVANT », titre
  tronqué en « CLASSI… », aperçu de la pièce suivante écrasé en barre.
- [ ] **P2.19** Taille du plateau en portrait : 18 dp par cellule à 360 dp, environ 21 à 390 dp,
  contre 24 points sur la maquette (marges de 24 dp et colonne de 112 dp) ; grands vides au-dessus
  et en dessous à 360 × 760. Trancher entre une colonne plus étroite, des marges réduites ou la
  maquette (revues de P5.7 et P2.11).
- [x] **P2.7** Écran de fin de partie, Rejouer / Menu : état de l'écran de jeu, Rejouer
  remplace la clé de la pile (ADR 0011 amendé). `GameOverScreen(GameOverUiState)`,
  `LabelledPanel` partagé avec la colonne latérale.
- [ ] **P2.15** Mettre à jour la maquette : confirmer les mesures et couleurs de V4 retenues en
  P2.7 (`docs/maquette.md`).
- [ ] **P2.8** Test sur téléphone : boutons ou gestes ? Décider et consigner (ADR si changement).
  Revue de P2.5 : évaluer le glissement d'un doigt de ◀ à ▼ ou ▶ (aujourd'hui le doigt reste
  attaché au premier bouton) ; le gérer demande un seul `pointerInput` sur ◀ ▼ ▶, une fonction
  pure `buttonAt(position)` et `HeldButtons.move`, testables sur la JVM.
  P5.12 : sur un écran à 90 ou 120 Hz, vérifier le passage à 60 Hz sans clignotement, la chute
  régulière et le retour à la fréquence d'origine sur l'accueil (`adb shell dumpsys display`).
  P5.27 : manette réelle (croix en axe HAT, boutons A/B/X selon le modèle), clavier Bluetooth
  avec le toucher, relâchement annulé quand la fenêtre perd le focus, Échap et B en pause.
- [x] **P2.11** Mise en page tolérante au paysage dès le rendu du plateau : Android 16 ignore
  `screenOrientation="portrait"` sur grands écrans (lint `DiscouragedApi`). Décider avec P2.8 si
  l'attribut reste dans le manifeste. Passer la taille de cellule du plateau à l'aperçu de la
  pièce suivante, pour des cellules de même taille comme sur la NES (revue de P2.4). Fait :
  `gameLayout` (fonction pure, trois dispositions), `PlacedParts`, aperçu à la taille de cellule
  du plateau ; `screenOrientation="portrait"` gardé jusqu'au test sur téléphone (P2.8).
- [ ] **P2.20** Fenêtre très étroite (220 dp) : cellule de 4 dp, injouable ; essayer le score
  au-dessus du plateau (revue de P2.11). À 220 dp et ×2,625, les flèches débordent de 2 px :
  réduire l'espacement des commandes à cette largeur.

## P3 — Accueil, préférences, records

- [x] **P3.1** `UserPreferencesRepository` (DataStore) : thème, musique, niveau de départ.
  Fait : `UserPreferences`, `ThemeChoice`, `MusicChoice`, `DataStoreUserPreferencesRepository`,
  fabrique commune `preferencesDataStore` (remplace `recordsDataStore`), `SETTINGS_FILE`. Le
  niveau de départ n'est borné qu'à 0 et plus : l'accueil (P3.4) borne le choix à la plage du
  mode. Rien n'est branché (P3.4, P3.5).
- [x] **P3.2** `HighScoreRepository` : meilleur score, détection d'un nouveau record. Le record
  garde la vraie valeur ; il s'affiche par `scoreText` (plafond de 999 999, P2.4). Revue de
  P2.7 : enregistrer le record dès l'événement `GameOver` (V4 n'est pas restauré après la mort
  du processus), et calculer « nouveau record » contre le record d'avant la partie. Fait :
  `HighScoreRepository`, `DataStoreHighScoreRepository`, `RecordResult`, `recordsDataStore`.
  Branché en P2.2 (incrément 2) : DataStore dans `AppContainer`, `submit` à `GameOver`,
  « Nouveau record » sur V4. Reste : record dans la barre du haut (P2.2, incrément 3) et à
  l'accueil (P3.4).
- [x] **P3.3** `AppContainer` et fabriques de ViewModels (ADR 0007 et 0013). Fait avec P2.2 :
  `AppContainer` construit `GameViewModel.Factory` ; les repositories (P3.1, P3.2) et la
  fabrique de l'accueil (P3.4) s'y ajouteront.
- [x] **P3.4** Écran d'accueil complet, conforme à la maquette. Fait : `HomeViewModel`
  (préférences, record, niveaux du mode), `HomeUiState`, `homeEntry(onPlay, factory)`,
  `HomeViewModel.Factory` construite par l'`AppContainer` (DataStore des réglages) ;
  `scoreText` déplacé dans `core:designsystem`. Écouter : P4.7.
- [x] **P3.7** Logo de quatre cellules aux couleurs du thème sur l'accueil (maquette V1, revue
  de P3.4). Fait : `HomeLogo`, `logoCells`, jeton `BloxPalette.logoCells` (`LogoColors`), garde-fou
  `LogoColorsTest` contre la dérive des couleurs des pièces.
- [ ] **P3.8** Mettre à jour la maquette : valeurs de l'accueil retenues en P3.4
  (`docs/maquette.md`, V1).
- [x] **P3.5** Changement de thème appliqué à toute l'application. La couleur de fond de fenêtre
  (`app/src/main/res/values/colors.xml`) duplique `BloxPalettes.Original.page` : ajouter un
  commentaire croisé ou dériver l'une de l'autre pour qu'elles ne se désynchronisent pas. Revue
  de P3.4 : la correspondance `ThemeChoice` → `BloxThemeKind` est privée à `feature:home` ; la
  déplacer (règle de 2) quand `app` en aura besoin. Fait : `AppTheme` (`StateFlow`, `app`),
  `themeKindOf` déplacé dans `app` (seul utilisateur), `BloxTheme` autour de toute la navigation,
  première image retenue avant la lecture, `WindowBackgroundTest` sur `colors.xml`.
- [x] **P3.6** Polices : embarquer *Press Start 2P* et *Chakra Petch* (OFL) ou utiliser les polices
  téléchargeables. Décider et consigner. Les chiffres de la colonne latérale sont en
  `FontFamily.Monospace` en attendant (P2.4) ; les tailles sont regroupées dans `SidePanelSizes`. Fait :
  polices embarquées (ADR 0016), licences dans l'APK, `BloxTypography`, chiffres complétés pour
  Chakra Petch, taille minimale 8 sp. Reste avec la police agrandie (P2.18) : à ×2 et 8 sp, la
  police pixel ne tient plus dans l'encart latéral (« SUIVANTE », « NIVEAU », score) ni dans les
  cases de niveau de l'accueil ; pistes : encart élargi selon la police, niveaux sur deux rangées.

## P4 — Musique et sons

Référence : [docs/musique.md](docs/musique.md).

- [x] **P4.1** `core:music` : `Pitch` (fréquence), `Note`, `Voice`, `Score`, en TDD.
- [x] **P4.2** Partitions *Korobeïniki* et *Menuet* : tests de cohérence (durée de chaque
  mesure, voix de même longueur). `Score.beatsPerBar`, invariants de longueur, `TRIANGLE`,
  plage de `Pitch` C0..B8.
- [x] **P4.3** Générateur d'échantillons pur (onde carrée, triangle, enveloppe, mixage), testé
  sur la JVM : `Synthesizer`, `mix` dans `core:audio`.
- [x] **P4.4** `ChiptuneSequencer` sur `AudioTrack`, lecture en boucle, arrêt propre. Avant
  d'écrire le séquenceur, choisir entre durées entières (sous-unités du temps, par exemple
  48 par noire) et comparaison avec tolérance : les durées sont des `Double` exacts tant qu'il
  n'y a ni triolet ni position cumulée sur une boucle infinie (revue de P4.2). Revue de P4.3 :
  `render(score, tempoFactor)` comme seule entrée publique (`renderVoice` et `mix` deviennent
  internes) ; rendre hors du thread audio (`Dispatchers.Default`), une fois, et garder le
  résultat en cache (environ 2,3 Mo par voix) ; écrire des tranches en `MODE_STREAM` ; éviter la
  double allocation par note (écrire directement dans le tableau de la voix). Fait :
  `Synthesizer.render`, `LoopCursor`, `AudioSink`, `AudioTrackSink`, `ChiptuneSequencer`
  (durées restées en `Double`, voir `docs/musique.md`).
- [x] **P4.5** Accélération du tempo sur `StackDanger`. Le facteur s'applique dans le
  séquenceur, sans recréer de `Score` (`bpm` entier : 150 × 1,25 = 187,5). Revue de P4.3 :
  passer le tempo du rendu en `Double`, rendre les deux tempos à l'avance et basculer à une
  frontière de temps (`pos' = round(pos × len' / len)`) ; ne jamais rééchantillonner, ce qui
  changerait la hauteur. Revue de P4.4 : le rendu et le cache acceptent déjà le facteur ; lire
  entre deux tranches un facteur demandé (`StateFlow` ou `@Volatile`) sans rouvrir la sortie ;
  calculer la position en `Long` ; arrondir au temps suivant (un temps = `len / totalBeats`
  échantillons). Au branchement, décider si `play` de la partition déjà jouée est sans effet
  (utile si le ViewModel rappelle `play` à la reprise), et journaliser l'échec audio attrapé.
  Fait : `setTempoFactor`, `BeatGrid`, `switchBeats`, bascule au prochain temps commun à toutes
  les voix ; le branchement est la tâche P4.11.
- [x] **P4.11** Brancher la musique dans le jeu : `ChiptuneSequencer` et `AudioTrackSink` créés
  par l'`AppContainer`, partition choisie dans les préférences (P3.1), `play` au début de la
  partie, `stop` à `GameOver` et en pause, `StackDanger(on/off)` → `setTempoFactor(1,25 / 1,0)`
  par le ViewModel. Décider alors si `play` de la partition déjà jouée est sans effet, et
  journaliser l'échec audio attrapé (revue de P4.4). Fait : `MusicPlayer` et `Playback`
  (`core:audio`), `scoreFor`, `tempoFactorFor` ; la reprise rejoue depuis le début (décidé).
- [x] **P4.12** Reprendre la musique là où elle s'était arrêtée après une pause (revue de
  P4.11). Fait autrement que prévu : `Playback.resume()` sur le jeton (`play` repart toujours du
  début), position mise à l'échelle du tempo en cours puis reculée au dernier temps de
  `switchBeats` (`resumePosition`). Position exacte de ce qui a été entendu
  (`playbackHeadPosition`) non lue : écart accepté (`docs/musique.md`).
- [x] **P4.6** `AudioFilePlayer` : choix d'un fichier personnel via le sélecteur du système.
  Revue de P3.1, amendée par P5.28 : garder l'URI (`setMusicFile(uri)`, permission persistante)
  dans un fichier DataStore à part, par exemple `music_file.preferences_pb` créé par
  `dataStoreFile`, que les règles de sauvegarde en liste blanche excluent d'office (ne pas
  l'ajouter à `BackupRulesTest`) ; `MusicChoice.MY_FILE` reste dans les réglages. Repli sur
  *Korobeïniki* si l'URI n'est plus lisible, et pour un `MY_FILE` sans fichier retenu, ce qui
  couvre aussi un `MY_FILE` restauré sur un autre appareil sans son URI.
- [x] **P4.7** Aperçu « Écouter » sur l'accueil. Remplacer `Score.title` (texte affiché dans le
  domaine, « Menuet en sol » alors que l'accueil dit « Menuet ») par un identifiant que la
  feature traduit via `strings.xml` (revue de P4.2). Fait :
  `ScoreId` remplace `Score.title` (aucun texte affiché dans le domaine), `scoreOf` dans `app`,
  aperçu dans `HomeViewModel` par le jeton de lecture, `FakeMusicPlayer` dans les testFixtures de
  `core:audio`.
- [x] **P4.23** Aperçu et lecteur en échec : si le séquenceur échoue, l'accueil affiche encore
  « Arrêter » alors que plus rien ne joue ; `MusicPlayer` n'expose aucun état (revue de P4.7).
- [x] **P4.24** Test de contrat `MusicPlayerContract` exécuté sur `FakeMusicPlayer` et sur
  `ChiptuneSequencer` (un jeton remplacé ne coupe rien), pour que le faux suive le vrai.
- [ ] **P3.9** Mettre à jour la maquette : bouton Écouter / Arrêter de l'accueil (P4.7).
- [ ] **P4.8** Effets sonores courts (rotation, ligne, 4 lignes, fin de partie), optionnels.
- [x] **P4.9** Droits : comparer la mélodie de *Korobeïniki* (`docs/musique.md`) à une partition
  publique du chant populaire, citer la source, et réécrire le rythme s'il suit la version du
  jeu de 1989 plutôt que le chant (revue de P4.2). Fait : 5 mesures sur 8 s'écartaient du chant ;
  mélodie remplacée par celle du chant (sources citées dans `docs/musique.md`), basse de la
  mesure 6 en A2/E3.
- [ ] **P4.10** Droits : confirmer la mélodie de *Korobeïniki* sur une édition d'époque
  (Prigojy 1898, recueils de Panina vers 1905, НЭБ) ; les deux sources nettes consultées en P4.9
  sont des notations modernes. Retirer alors « [à vérifier] (droits) » de `docs/musique.md`. Recherche faite
  (P4.10, voir `docs/musique.md`) : deux éditions d'époque confirment le rythme et les mesures 3 à 8,
  pas les mesures 1 et 2 ; mention gardée par décision du développeur, tâche laissée ouverte.

Musiques composées dans l'application, à la main ou par le générateur : spécification dans
[docs/generateur-musique.md](docs/generateur-musique.md), prototype dans la maquette
[Éditeur de musique BloX](https://claude.ai/artifact/M91HH7KRJWTMHeDvLaV8wz). Le générateur
compose selon des règles d'écriture (accords éprouvés, motif repris, degrés conjoints, notes de
l'accord sur les temps forts) et garde le meilleur de plusieurs candidats ; le hasard ne choisit
qu'entre des possibilités musicales. Valeurs et sources **[à vérifier]**.

- [x] **P4.13** Modèle harmonique dans `core:music` : `Scale` (majeure, mineures naturelle et
  harmonique, dorienne, mixolydienne, pentatonique), `Chord`, `Progression`.
- [x] **P4.14** Catégories du générateur (Joyeux, Calme, Épique, Mélancolique, Mystérieux,
  Rétro, Danse, Surprise) : gamme, tempo, suites d'accords, rythmes, basse de chacune.
- [x] **P4.15** Forme : motif d'une mesure, plan `A A' B A''` sur 8 mesures, cadences
  suspendues aux mesures 4 et 8 pour la boucle ; même graine, même musique. Accord de cadence
  à choisir quand le V de la gamme est mineur ou absent ; deux accords par mesure (Rétro).
- [x] **P4.16** Mélodie : degrés conjoints, sauts compensés, notes de l'accord sur les temps
  forts, courbe en arche, fins de phrase longues ; traits de la catégorie en paramètres (sauts
  de quinte, descentes, arpèges) ; tessiture des voix.
- [x] **P4.17** Basse selon la catégorie, passage vers l'accord suivant, sans quintes ni octaves
  parallèles avec la mélodie ; styles de basse ramenés aux motifs de la spécification.
- [x] **P4.18** Note de qualité et règles éliminatoires ; choix du meilleur candidat. Revue de P4.16 :
  mesurer les notes étrangères approchées et quittées par degré ; mesures homogènes (sur la
  forme), sans NaN pour une mélodie sans mouvement. Revue de P4.17 : écarter les candidats sans
  basse (Mystérieux environ 28 %) ; harmoniser l'échec de `Melody` (exception) et de `Bass`
  (`null`) ; critère de conduite de basse (sauts).
- [ ] **P4.19** Éditeur de musique (`feature:composer`) : grille selon la gamme, notes longues,
  tempo, écoute ; enregistrement avec un nom (proposé d'après la catégorie, modifiable),
  demande avant de quitter sans enregistrer. Aux couleurs des thèmes Original, Couleurs et
  Brume (clair et sombre), sans couleur propre : `core:designsystem` fournit les jetons.
  Découpée en tranches :
  - [x] **P4.19a** Modèle de l'éditeur, sans écran : grille, gestes, barre d'outils, annuler,
    tempo, « modifiée », noms, conversions partition ↔ brouillon.
  - [ ] **P4.19b** Écran de l'éditeur (V8, V11, V14, V17) : `ComposerViewModel`, clé `NavKey` et
    entrée dans `app`, grille d'une mesure avec ses gestes, onglets, barre d'outils, boutons de
    mesure, noms de notes pour le lecteur d'écran, filigrane de l'autre piste, cases à deux
    doubles croches, captures des thèmes ; retirer l'exclusion `buildHealth` de `feature:composer`.
  - [ ] **P4.19c** Écoute dans l'éditeur : *Écouter* par `MusicPlayer`, colonne jouée, *Suivre la
    lecture*.
  - [ ] **P4.19d** Enregistrer, quitter, renommer, supprimer, avec le stockage de P4.22 : fenêtre
    d'enregistrement sur `MusicNames`, question en quittant.
- [ ] **P4.20** Bouton *Au hasard* dans l'éditeur : choix de la catégorie, annulation.
- [ ] **P4.22** Liste « Mes musiques » : partitions enregistrées dans `core:data`, écoute depuis
  la liste, ouverture dans l'éditeur, choix de la musique jouée pendant les parties (une à la
  fois, proposée aussi dans la musique de l'accueil, V1), renommer et supprimer. Découpée en
  tranches :
  - [x] **P4.22a** Stockage dans `core:data` : `ScoreId` scellé (intégré ou personnel),
    `SavedMusic`, dépôt `MusicLibrary` (enregistrer, renommer, supprimer et restaurer, musique
    des parties), règles de nom en une seule source, format `musics.bin` (ADR 0017), sauvegarde.
  - [x] **P4.22b** Jouer pendant les parties la musique choisie (`gameMusic`), à la place du
    choix de l'accueil : source suspendue dans la fabrique de `GameViewModel`, construite dans
    `app`.
  - [ ] **P4.22c** Écran « Mes musiques » (V7, V10, V13, V16) : liste, écoute « mesure n sur 8 »,
    Utiliser / En jeu, menu « … », suppression avec confirmation et annulation, *Au hasard* et
    *Nouvelle*, navigation depuis l'accueil.
  - [x] **P4.22d** Musique choisie proposée dans le choix de musique de l'accueil (V1). Revue de P4.22b : choisir une autre musique à l'accueil (Aucune comprise) retire
    la musique personnelle des parties (`setGameMusic(null)`), test de `HomeViewModel`. Choisir
    un fichier à l'accueil la retire aussi (revue de P4.6).
- [x] **P4.21** Mettre à jour la maquette : liste « Mes musiques » et éditeur dans la maquette BloX
  (accès depuis V1), dans les quatre thèmes ; choix de la catégorie, fenêtre d'enregistrement.

## P5 — Finitions

- [x] **P5.1** Icône de l'application et écran de démarrage. Fait : icône adaptative aux couleurs du
  logo, couche monochrome, écran de démarrage d'Android 12 par le thème (`docs/themes.md`).
- [x] **P5.2** Vibrations (optionnelles) sur ligne effacée et fin de partie.
- [x] **P5.3** Accessibilité : descriptions des boutons, contrastes, tailles de cible ;
  `semantics { contentDescription }` sur `BoardCanvas` (revue de P2.3).
- [x] **P5.4** Gestion du cycle de vie : pause automatique quand l'app passe en arrière-plan, déclenchée
  à `ON_PAUSE` (l'horloge d'images ne s'arrête qu'à `ON_STOP`, ADR 0012). Revue de P2.2
  (incrément 3) : en pause, les images tournent encore à 60 Hz sans donner de tick ; suspendre
  la boucle tant que `paused` est vrai, puis `onFramesStarted()` à la reprise. Fait :
  `LifecycleEventEffect(ON_PAUSE)` → `onPause()`, boucle suspendue en pause (ADR 0012 amendé).
- [ ] **P5.5** Tests d'interface des parcours principaux (Compose UI Test). Au premier test
  instrumenté, vérifier que le moteur Jupiter arrive sur `debugAndroidTestRuntimeClasspath`
  (`:feature:game:dependencies`), ou le déclarer en `androidTestRuntimeOnly` (revue de P1.18).
  Parcours à couvrir : fin de partie → Rejouer (nouvelle partie) → Menu ; retour système (revue
  de P2.2, incrément 3 : en jeu il met en pause sans dépiler, en pause il reprend, en fin de
  partie il revient au menu) ; accueil (revue de P3.4 : record lu en clair, Moderne désactivé,
  « Jouer » au niveau choisi) ; cycle de vie (revue de P5.4 : `ON_PAUSE` met en pause avec un
  `TestLifecycleOwner`, la boucle ne donne aucun tick en pause et reprend sans rattrapage).
  Revue de P2.2 : le
  ViewModel du jeu garde les `CreationExtras` par défaut (`SavedStateHandle` disponible), sans
  test JVM possible aujourd'hui. Revue de P2.2 (incrément 2) : `GameScreen` affiche
  `GameOverScreen` à la place du jeu, `GameControls` branché, `dropUnlessResumed` sur Rejouer
  et Menu ; `MainDispatcherExtension` (tests de `feature:game`) à mutualiser dès qu'un autre
  module en a besoin.
- [ ] **P5.6** Build de release signé pour installation personnelle. Vérifier la restauration de
  la pile de navigation après mort du processus en build minifié (règles R8 de
  kotlinx-serialization, ADR 0011).
- [x] **P5.7** Tests de captures d'écran à partir des previews existantes (plugin officiel Compose
  Preview Screenshot Testing), pour les deux thèmes, dans la CI. Revue de P2.3 : le dessin de
  `BoardCanvas` n'est vérifié par aucun test ni à l'œil ; ajouter aussi un fichier de
  configuration de stabilité Compose (`build-logic`) listant `ch.magori.blox.core.game.model.*`.
  Fait : plugin `com.android.compose.screenshot` 0.0.1-alpha16 par la convention
  `blox.android.screenshot`, 21 captures (`docs/architecture.md`, Tests). Reste en P5.8 et P5.9.
- [ ] **P5.8** Captures dans la CI : générer les références sous Linux (job ou conteneur) puis
  ajouter `validateDebugScreenshotTest` à `.github/workflows/ci.yml` ; revoir le plugin à la
  prochaine montée d'AGP (déconseillé à partir d'AGP 9.5 au profit des « test suites »).
  Revue qualité (C2) : tant que Dependabot fusionne sur CI verte, une montée de Compose peut
  changer le rendu sans signal.
- [x] **P5.9** Fichier de configuration de stabilité Compose (`compose-stability.conf`, branché par `build-logic`) listant
  `ch.magori.blox.core.game.model.*` (revue de P2.3).

Revue qualité du 2026-10-19 (Android et performance, qualité logicielle, sécurité,
accessibilité, build et CI) : constats et raisons des écarts dans
[docs/revue-qualite.md](docs/revue-qualite.md). Chaque tâche cite ses constats (A1, B2, C3…).

- [x] **P5.10** Musique au tempo normal hors partie : remettre le tempo à la fin de partie et en
  quittant le jeu, pour que « Écouter » à l'accueil ne joue pas à ×1,25 (A1). Fait : `play` part toujours au tempo normal, `resume` garde le tempo en cours (`docs/musique.md`).
- [x] **P5.11** Rendu du plateau : peintres et `Path` construits une fois par thème et par
  taille, plateau et pièce lus dans la phase de dessin, racine du jeu qui ne recompose plus à
  chaque mouvement de la pièce (A2, A7).
- [x] **P5.12** Boucle d'images arrêtée sur l'écran de fin de partie ; 60 Hz demandés pendant la
  partie sur les écrans à 90 ou 120 Hz, rétablis en quittant le jeu (A5, A6).
- [x] **P5.13** Mort du processus : une partie recréée par le système démarre en pause (marqueur
  « partie commencée » dans le `SavedStateHandle`), sans reprendre l'état (ADR 0014) (A3).
- [x] **P5.14** Focus audio : le demander à la lecture, l'abandonner à l'arrêt, couper la musique
  quand il est perdu durablement et la baisser quand il l'est passagèrement ; arrêter sur
  débranchement du casque (`ACTION_AUDIO_BECOMING_NOISY`) (A4). Fait : décorateur `FocusedMusicPlayer` (`core:audio`), `SystemAudioFocus` sans test JVM ; une perte passagère met en pause (`docs/musique.md`).
- [x] **P5.15** Audio robuste : sortie rouverte une fois après `ERROR_DEAD_OBJECT` ; les deux
  tempos rendus au lancement de la partie, cache limité à la partition en cours (A8, A11).
- [x] **P5.16** Zone du geste Retour : `systemGestureExclusion` sur les deux groupes de commandes
  (A10).
- [x] **P5.17** Profil de référence (baseline profile) et Macrobenchmark du parcours accueil →
  partie (A9). Demande un émulateur ou un téléphone pour générer le profil.
- [x] **P5.18** Traçabilité « règle → test » : corriger les quatre références introuvables de
  `docs/regles-mode-classique.md` et `docs/musique.md`, puis un script dans `scripts/` qui vérifie
  chaque `Classe › nom` des docs, lancé en CI (B1). Fait : cinq références corrigées (une de plus, `CellStylesTest`), `scripts/verifier-tracabilite.sh` en tête de la CI. Reste hors script : les classes citées seules dans `docs/architecture.md`.
- [x] **P5.19** Appui pendant l'effacement ou le délai d'apparition : tests de la règle « appui
  perdu » (A, B, ◀ dans `EntryDelay` et `Clearing`), compteur de déplacement de départ nommé et
  sourcé, ou marqué **[à vérifier]** (B2). Fait : 5 tests (A, B, ◀, ▶, ▼, dernier tick du délai, DAS chargé), `AutoRepeat.heldWithoutPress` sourcé (`autorepeatX`), valeur 0 **[à vérifier]**.
- [x] **P5.20** Invariants de progression : le niveau dérive du niveau de départ et des lignes
  (vérifié à la construction), fixture `gameState(...)` corrigée, test « le niveau ne baisse
  jamais après un effacement », KDoc de `GameState` (B3, B7).
- [x] **P5.21** Moteur : `Ticks` pour les décomptes de `Phase` et de `RepeatTiming`, décompte
  partagé entre effacement et délai d'apparition ; `AutoRepeat`, `GameState.controls` et les
  constructeurs de `Clearing` et `EntryDelay` en `internal` ; KDoc de `GameEvent.StackDanger`,
  `GameUiState` et du ViewModel (B4, B5, B7).
- [x] **P5.22** Erreurs de lecture des records : contrat écrit dans `HighScoreRepository`, seule
  `IOException` absorbée, même traitement à l'accueil et en jeu (B6).
- [x] **P5.23** État de l'écran de jeu : `Points`, `Lines` et `Level` gardés dans
  `GameOverUiState` ; cellules de la pièce suivante calculées dans `toUiState`, sans
  `RotationSystem` dans `GameUiState` (B8, B11).
- [x] **P5.24** CI : `assembleRelease` (R8) et `assembleDebugAndroidTest`, rapports publiés en
  cas d'échec, `permissions: contents: read`, actions épinglées par SHA, soumission du graphe de
  dépendances sur `main` (C1, C7, C9).
- [x] **P5.25** Avertissements bloquants : opt-in des coroutines expérimentales dans les tests,
  avertissements Kotlin en erreurs, lint en erreurs avec une base des avertissements assumés,
  chacun avec sa raison (C8).
- [x] **P5.26** Accueil : un choix sélectionné se distingue sans la couleur (cadre ou coche), avec
  au moins 3:1 entre les deux états dans chaque thème ; fonction `choiceColors` testée comme
  `keyColors` (C3). Fait : soulignement et choix non sélectionnés en encart, 5,7:1 au moins ; contraste en `testFixtures` de `core:designsystem` (`docs/themes.md`, Choix de l'accueil).
- [x] **P5.27** Clavier et manette : flèches, ZQSD/WASD, croix et boutons A/B traduits en
  appuis et relâchements tenus, Échap ou Start pour la pause (C4).
- [x] **P5.28** Règles de sauvegarde explicites : `data_extraction_rules.xml` et
  `backup_rules.xml`, qui gardent `datastore/` et excluront les URI de fichiers (C6).
- [ ] **P5.29** Niveaux de départ : cibles d'au moins 48 dp de large (deux lignes de 5 ou
  sélecteur − / +) ; décision du développeur et maquette à mettre à jour (C5).
- [ ] **P5.30** Mettre à jour la maquette : choix de l'accueil (V1), choix sélectionné plein (sans
  soulignement, P5.36) et choix non sélectionnés en encart (`panel`, cadre `panelBorder`), coins carrés en Original (P5.26) ;
  bouton « Jouer » de 48 dp de haut et en `accent` (P5.3). Écart du code avec V1 (P4.22d) : la
  musique personnelle « En jeu » sans icône de note ni grille de trois colonnes, à trancher.
- [x] **P5.31** Cache de build partagé entre worktrees : deux fois, `ktlintCheck` a échoué sur les
  rapports d'un autre worktree (chemins `blox-wt/q-jeu`), vert après `--rerun-tasks --no-build-cache` ;
  trouver la tâche dont la sortie mise en cache contient des chemins absolus, et la rendre relocalisable
  ou non cacheable (revue P5.24).
- [x] **P5.32** Doublures des tests instrumentés : `GameViewModelDoubles.kt` (`NoRecords`,
  `DefaultSettings`) recopie encore les faux de `core:data`, et `NoRecords` ne respecte pas le
  contrat ; passer par `androidTestImplementation(testFixtures(projects.core.data))` (revue P0.15).
- [ ] **P5.33** Mesures sur téléphone (suite de P5.17) : `GameBenchmark` n'a jamais rendu de durée
  d'image (sur émulateur, aucune image, puis `home_play` introuvable en `benchmarkRelease` :
  lenteur ou tags perdus par R8, à vérifier par `uiautomator dump`, `UI_TIMEOUT` à 10 s) ;
  remesurer le démarrage à froid avec et sans profil sur un appareil non partagé ; lancer
  `GameScreenTest` et `BoardCanvasTest` pour confirmer que les `testTag` n'ajoutent aucune
  recomposition (P5.11) ; les tests écartés par `enabledRules` sont ignorés pour Gradle mais
  écrits en `<failure>` vide dans les XML d'AGP. Passe complète de `HomeScreenTest` (P5.36, P4.22d), de `GameScreenTest` et
  `BoardCanvasTest` sur un émulateur peu chargé (P5.3 : chaque test passé seul, jamais une passe
  sans erreur « PixelCopy ») ; essai de TalkBack sur téléphone (ordre de lecture, plateau).
- [ ] **P5.34** Commandes tenables via TalkBack et Switch Access (suite de C4) : action sémantique
  « Maintenir / Relâcher » ou appui long, annonce de l'état tenu, relâchement à la pause et en fin
  de partie, lien avec le DAS ; essai sur appareil, maquette si besoin (revue de P5.27).
- [x] **P5.35** En jeu, une musique morte (séquenceur en échec) garde le focus audio jusqu'à la
  pause ou la fin : `GameViewModel` pourrait suivre `Playback.isPlaying` et rendre le focus,
  sans couper la partie (revue de P4.23).
- [ ] **P5.37** Mettre à jour la maquette : interrupteur « Vibrations » (accueil V1 ou écran de
  réglages), puis le brancher (`HomeViewModel`, `HomeUiState`, `HomeScreen`, captures) et fixer
  sa valeur par défaut : désactivé (décision du développeur, 2026-10-19 ; P5.2).
- [ ] **P5.38** Mettre à jour la maquette : action « Changer » (Changer de fichier) sous « Mon
  fichier », à côté d'« Écouter », et le nom du fichier retenu, à afficher ensuite
  (`OpenableColumns.DISPLAY_NAME`) (P4.6).
- [x] **P5.36** Retirer le soulignement des choix sélectionnés de l'accueil (demande du
  développeur) : le choix sélectionné reste plein, à 3:1 au moins en luminance des choix cadrés.
- [x] **P5.39** En police agrandie (×2), l'action « Changer » sous « Mon fichier » est coupée sur
  trois lignes (captures de P4.22d) : la faire tenir (libellé, mise en page ou retour à la ligne).
- [ ] **P5.41** Thème Brume (clair et sombre) dans le code : relever ses jetons dans la maquette
  (V5, V6, V13 à V18) et les écrire dans `docs/themes.md` ; `BloxThemeKind` et palettes, choix à
  l'accueil (V1) et à la fin de partie (V4), clair ou sombre selon le réglage du système ; jetons
  de l'éditeur (déjà relevés) ; tests de contraste (`PaletteContrastTest`, `KeyColorsTest`,
  `ChoiceColorsTest`, `TrackColorsTest`) et captures dans les deux variantes.
- [ ] **P5.42** Repère du choix sélectionné à l'accueil, autre que la couleur (WCAG 1.4.1) : essayer
  une coche dans le choix sélectionné (décision du développeur, à tenter) ; place et taille en
  police normale et ×2, niveaux de départ compris, captures, puis maquette V1 (P5.30).

## P6 — Mode Moderne (plus tard)

Analyse : [docs/analyse-mode-moderne.md](docs/analyse-mode-moderne.md). Un seul moteur pour
les deux modes : `Rules` déclare ce que le mode permet, les comportements codés en dur
deviennent des politiques, et le Classique leur donne des valeurs neutres. Chaque tâche laisse
les tests du Classique verts ; les refactorings passent avant les ajouts. Décisions prises :
3 pièces suivantes par défaut (réglable), commandes par boutons ou gestes (réglable), cellules
au-dessus du plateau perdues comme sur la NES (option de les garder plus tard), départ au
niveau 1, gravité de plus d'une ligne par tick jusqu'à la chute immédiate au sol.

- [x] **P6.1** Spécification `docs/regles-mode-moderne.md`, valeurs sourcées (les plus répandues
  parmi les jeux récents) ; ADR 0015 « un moteur, des capacités » qui amende l'ADR 0004 ;
  promesse « sans modifier le moteur » et glossaire (`Hold`, `HardDrop`, `Ghost`, `LockDelay`,
  `Bag`, `TSpin`, `Combo`, `BackToBack`) mis à jour dans `CLAUDE.md`. Fait : `docs/regles-mode-moderne.md` (18 valeurs [à vérifier]), ADR 0015, glossaire.
- [x] **P6.2** Politique `LineClearDelay` : durée de l'effacement des lignes sortie du moteur,
  valeurs Classique (refactoring). Fait : `LineClearDelay`, `ClassicLineClearDelay`, `Rules.lineClearDelay` ; au moins 1 tick (décidé).
- [ ] **P6.3** File de pièces suivantes : `GameState.next` devient une file, `Rules.previewCount`
  (1 en Classique) ; `feature:game` lit la première pièce.
- [x] **P6.4** `SevenBagRandomizer` : les sept pièces mélangées, puis un nouveau sac. Fait : `SevenBagRandomizer` (ordre de base I J L O S T Z, suite de référence par graine).
- [ ] **P6.5** `SrsRotationSystem` : quatre états par pièce, tables de décalage, table propre au I,
  O fixe.
- [x] **P6.6** Position de chute d'une pièce (`Board`), base de la pièce fantôme et de la chute
  instantanée. Fait : `Board.landing(piece)` (nom retenu à la place de `dropPosition`).
- [ ] **P6.7** Chute instantanée : bouton, capacité dans `Rules` (absente en Classique), points
  par ligne descendue.
- [ ] **P6.8** Politique `LockDelay` : temps au sol, réinitialisations limitées par déplacement
  ou rotation (0 en Classique, comportement identique).
- [ ] **P6.9** Réserve : bouton, pièce réservée dans `GameState`, une fois par pièce ; capacité
  absente en Classique.
- [ ] **P6.10** Politique `ControlScheme` : règles de la manette NES sorties de `shift` et `drop`
  (refactoring), puis schéma Moderne (dernier sens pressé gagnant, déplacement pendant la
  descente rapide). À découper en incréments.
- [ ] **P6.11** Gravité de plus d'une ligne par tick (contrat de `GravityTable`), courbe Moderne
  jusqu'à la chute immédiate au sol.
- [ ] **P6.12** Contexte de verrouillage et détection du T-spin (règle des 3 coins, mini).
- [ ] **P6.13** Score Moderne : nouveau contrat de `ScoringRule` (T-spin, combos,
  enchaînements, niveau d'avant l'effacement), nouveaux événements et statistiques. À découper
  en incréments.
- [ ] **P6.14** Fin de partie Moderne : pièce verrouillée entièrement au-dessus du plateau
  (revue de P1.6), apparition décalée d'une ligne ; cellules au-dessus du plateau perdues comme
  sur la NES.
- [ ] **P6.15** `ModernRules`, `ModernGameEngineFactory`, progression Moderne (départ au
  niveau 1, un niveau toutes les 10 lignes).
- [ ] **P6.16** Choix du mode : mode enregistré dans les préférences, *Moderne* activé dans V1,
  fabrique du moteur choisie par `app` ; records par mode.
- [ ] **P6.17** Écran Réglages : nombre de pièces suivantes (3 par défaut), commandes par
  boutons ou par gestes ; enregistrés dans `core:data`.
- [ ] **P6.18** Écran de jeu Moderne : réserve, file de pièces suivantes, pièce fantôme
  (style par thème), boutons de chute instantanée et de réserve.
- [ ] **P6.19** Commandes par gestes : glisser pour déplacer, taper pour tourner, glisser vers
  le bas pour la chute instantanée.
- [ ] **P6.20** Fin de partie Moderne : statistiques (T-spins, plus long combo) dans V4 ; sons
  des nouveaux événements.
- [ ] **P6.21** Option « garder les cellules au-dessus du plateau » : zone cachée stockée dans
  `Board`, réglable dans l'écran Réglages.
- [ ] **P6.22** Mettre à jour la maquette : écran de jeu et fin de partie du mode Moderne,
  écran Réglages.


---

Le journal (ce qui a été fait, décisions, remarques de revue) est dans [JOURNAL.md](JOURNAL.md).
