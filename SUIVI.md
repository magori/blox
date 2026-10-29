# Suivi de l'implémentation

Une tâche = un incrément livrable, testé, commité. Les identifiants (`P1.3`) servent de
référence dans les commits et avec la commande `/prochaine-tache`.

Légende : `[ ]` à faire · `[~]` en cours · `[x]` terminé · **[à vérifier]** = règle à
confirmer sur une source avant d'écrire le test.

## Vue d'ensemble

<!-- avancement:debut -->
| Avancement | Tâches | % |
|---|---|---|
| **Général** | 285 / 378 | `███████████████░░░░░` **75 %** |
| **MVP audio** | 28 / 28 | `████████████████████` **100 %** |
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

<!-- mvp-audio: P4.1 P4.2 P4.3 P4.4 P4.5 P4.6 P4.7 P4.9 P4.11 P4.12 P4.13 P4.14 P4.15 P4.16 P4.17 P4.18 P4.19a P4.19b P4.19c P4.19d P4.19e P4.20 P4.22a P4.22b P4.22c P4.22d P4.23 P4.24 -->

| Phase | Objectif | État |
|---|---|---|
| P0 | Structure du projet et documentation | Build local validé ; reste P0.12 |
| P1 | Domaine du mode Classique (`core:game`) | En cours : reste la rotation, les commandes maintenues et le moteur |
| P2 | Écran de jeu jouable (`feature:game`) | En cours : `GameLoop` fait |
| P3 | Accueil, préférences, records | À faire |
| P4 | Musique et sons | En cours : modèle et partitions faits |
| P5 | Finitions | À faire |
| P6 | Mode Moderne | Plus tard |
| P7 | Le Serrurier, second jeu | Jouable ; progression des serrures à faire (P7.27 à P7.39) |
| P8 | Musique vivante, la musique du Classique qui suit la partie | Spécification faite ; à faire |
| P9 | Graine du jour, les mêmes pièces pour tous chaque jour | Spécification faite ; à faire après P1.20 et P8 |
| P10 | Le Jardin musical, troisième jeu | Spécification faite ; à faire, en parallèle de P9 |
| P11 | Au tempo, la pièce tombe en rythme | Spécification faite ; à faire en dernier, après une mesure sur téléphone |
| P12 | Réglages généraux et réglages par jeu | Proposition du designer validée (2026-10-28) ; à faire après P10 |

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
- [x] **P1.20** Générateur stable `StableRandom` (`rules`), **préalable de la Graine du jour (P9)** et du dé du
  Jardin (P10.7) : générateur maison dont la suite ne dépend pas de la version de Kotlin
  (`kotlin.random.Random` ne garantit pas la sienne). `StableRandom` étend `kotlin.random.Random` ; `nextInt(n)`
  tire sans biais, par tirage avec rejet. Point d'entrée **public** `ClassicRandomizer.stable(seed)`
  (`explicitApi`), le constructeur `ClassicRandomizer(random)` restant `internal` ; un test fige les 20 premières
  pièces de la graine 20261026 : la suite des pièces d'une graine devient un contrat (ADR 0026, qui amende
  l'ADR 0014). Algorithme à choisir ici ([graine-du-jour.md](docs/graine-du-jour.md)) ; glossaire de `CLAUDE.md`
  (`StableRandom`). Cas : `the same seed gives the same numbers`, `a number drawn in a range takes every value of
  the range`, `the first 20 pieces of seed 20261026 are fixed`.

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
- [x] **P2.12** Mettre à jour la maquette : confirmer la taille du carré central des pièces du
  thème Original (8 px sur 24 retenu en P2.3) sur la maquette Claude Design.
- [x] **P2.4** Colonne latérale : score, niveau, lignes, pièce suivante. `SidePanel`,
  `NextPieceCanvas`, `BloxTheme.shapes`.
- [x] **P2.13** Mettre à jour la maquette : confirmer les mesures des encarts de la colonne
  latérale (cadre, marges, tailles de texte) retenues en P2.4 (`docs/maquette.md`, V2 et V3).
- [x] **P2.5** Commandes tactiles : ◀ ▼ ▶, A, B, avec appui maintenu. `HeldButtons`,
  `GameControls(onChange)`, `BloxShapes.button`. Pas encore branché dans `GameScreen` (P2.2).
- [x] **P2.14** Mettre à jour la maquette : confirmer la taille, l'espacement et le retour
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
- [x] **P2.16** Mettre à jour la maquette : écran de pause et barre du haut, valeurs retenues
  en P2.6 (`docs/maquette.md`).
- [x] **P2.17** Lisibilité vue sur les captures (P5.7) : touche A enfoncée invisible en thème
  Couleurs (son fond égalait la page) ; cellules en aplat du thème Original fondues entre elles.
  Fait : `keyColors`, jetons `buttonPressed` et `accentPressed`, lettres A et B en gras ; joint
  de 1/24 de la couleur du fond (`cellLayout`, `CellPainter(background)`).
- [x] **P2.21** Mettre à jour la maquette : touches tenues (teinte « pressée »), lettres A et B
  en gras, joint des cellules du thème Original (P2.17) ; cadre `buttonOutline` `#646B87` des
  touches au repos et du bouton pause en thème Couleurs, V3 (P2.22, P2.23).
- [x] **P2.22** Contraste des flèches au repos en thème Couleurs : `button` `#2A2E3E` sur la page
  `#12141C` ne fait que 1,36:1, sous le 3:1 des composants (revue de P2.17). Fait : jeton
  `buttonOutline`, cadre de 2 dp autour des touches au repos (3,50:1).
- [x] **P2.23** Contraste du bouton pause en thème Couleurs : même défaut que les flèches (1,36:1
  contre la page) ; lui donner le cadre `buttonOutline` (`topbar/GameTopBar.kt`, `PauseButton`). Fait : `pauseButtonColors`, couleurs d'une touche au repos (3,50:1).
- [x] **P2.18** Police ×2 vue sur les captures (P5.7) : « SUIVANTE » rogné en « SUIVANT », titre
  tronqué en « CLASSI… », aperçu de la pièce suivante écrasé en barre. Fait : plancher d'un texte
  qui rétrécit au plus petit de 8 sp et de sa taille en police normale (`shrinkFloor`) ; l'aperçu
  n'était pas écrasé (pièce I).
- [x] **P2.19** Taille du plateau en portrait : 18 dp par cellule à 360 dp, environ 21 à 390 dp,
  contre 24 points sur la maquette (marges de 24 dp et colonne de 112 dp) ; grands vides au-dessus
  et en dessous à 360 × 760. Trancher entre une colonne plus étroite, des marges réduites ou la
  maquette (revues de P5.7 et P2.11). Tranché : on garde tel quel (décision du développeur,
  2026-10-20) ; la maquette dessine désormais des cellules de 21 comme le code à 390 dp.
- [x] **P2.7** Écran de fin de partie, Rejouer / Menu : état de l'écran de jeu, Rejouer
  remplace la clé de la pile (ADR 0011 amendé). `GameOverScreen(GameOverUiState)`,
  `LabelledPanel` partagé avec la colonne latérale.
- [x] **P2.15** Mettre à jour la maquette : confirmer les mesures et couleurs de V4 retenues en
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
- [x] **P2.20** Fenêtre très étroite (220 dp) : cellule de 4 dp, injouable ; essayer le score
  au-dessus du plateau (revue de P2.11). À 220 dp et ×2,625, les flèches débordent de 2 px :
  réduire l'espacement des commandes à cette largeur. Fait : score au-dessus sous 288 dp, cellule de
  10 dp à 220 × 640 dp, flèches rapprochées.
- [x] **P2.24** Sous 8 dp de cellule (fenêtre de moins de 200 dp de large ou de 560 dp de haut
  environ), le jeu est peu jouable : afficher « Agrandissez la fenêtre » à la place ? À trancher
  par le développeur (revue de P2.20). Fait (oui, décision du développeur) : message à la place du
  plateau, partie en pause, retour qui quitte.
- [ ] **P2.28** Mettre à jour la maquette : vue « Agrandissez la fenêtre » (fenêtre trop petite pour
  jouer, barre du haut gardée, bouton pause grisé) (P2.24).
- [x] **P2.25** Mettre à jour la maquette : fenêtre très étroite, score et niveau puis lignes et
  pièce suivante au-dessus du plateau, flèches rapprochées (P2.20) ; barre du haut en deux rangées,
  titre au-dessus (P2.26).
- [x] **P2.26** Barre du haut dans une fenêtre très étroite : « MODE CLASS… » et le record (« 09876… »)
  sont tronqués à 220 dp, surtout en police agrandie (vu en P2.20). Fait : le titre passe au-dessus
  quand titre et record ne tiennent pas chacun dans leur moitié (`topBarRows`).
- [ ] **P2.27** Barre du haut à 360 dp en Original ×2 : le titre tient à un demi-pixel près
  (324 px pour 324,5 à ×2,625) ; vérifier sur un téléphone d'une autre densité qu'il ne passe pas
  au-dessus, et sinon gagner un peu de place (écarts, bouton pause) (revue de P2.26).

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
- [x] **P3.8** Mettre à jour la maquette : valeurs de l'accueil retenues en P3.4
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
- [ ] **P4.25** Position de lecture exposée par `Playback` : la colonne jouée de l'éditeur est
  calculée à partir du temps ; elle part avant le son, change de tempo un temps trop tôt et
  continue pendant une perte passagère du focus. Le séquenceur dirait où il en est, et l'éditeur
  le suivrait (revue de P4.19c, à faire si l'écart s'entend sur téléphone). La liste « Mes
  musiques » calcule de même sa « mesure n sur 8 » (P4.22c).
- [x] **P4.26** Mettre à jour la maquette : « Mes musiques » telle que codée (V7, V10, V13, V16) :
  *Utiliser* / *En jeu* sous le nom, menu déroulant, *Renommer* dans le menu de la carte, pas de
  message après *Utiliser*, lien « MES MUSIQUES » souligné à l'accueil (P4.22c).
- [x] **P4.27** Nom proposé à l'enregistrement en rapport avec la musique (demande du développeur,
  2026-10-20) : un nom évocateur tiré selon le style, suivi de la tonalité (« Matin de fête en
  sol », « Brume du lac en ré mineur ») ; pour une musique « Libre », d'après le mode et le tempo
  (« Rêverie en la mineur ») ; tirage déterministe, 40 caractères au plus, « 2 » ajouté si le nom
  est pris (`freeName`), aucun nom de marque. Remplace « Catégorie n » (`proposedName`) ; listes de
  mots par style dans la spécification (docs/generateur-musique.md), maquette V9, V12, V15, V18.
- [x] **P4.28** Style choisi dans la fenêtre Enregistrer (demande du développeur, 2026-10-20) : choix
  « Style » parmi les 7 catégories et « Libre », pré-rempli avec la catégorie du dé (« Libre » pour
  une musique faite à la main), enregistré avec la musique (`SavedMusic.category`, null pour
  « Libre ») ; le style change le nom proposé tant que le joueur n'a pas tapé le sien (P4.27).
  Maquette V9, V12, V15, V18.
- [x] **P4.29** Informations des cartes de « Mes musiques » (demande du développeur, 2026-10-20) :
  sous le nom, style et tonalité (« Joyeux · sol majeur », « Libre · la mineur »), tempo et durée
  des 8 mesures (« 120 temps/min · 0:16 »), date de dernière modification (« Modifiée le 20 oct. ») ;
  la date est enregistrée à chaque enregistrement ou renommage (horloge injectée), d'où une nouvelle
  version de `musics.bin` avec migration des musiques existantes (ADR 0017 amendé). Dans l'éditeur,
  la ligne sous le titre devient « Style · tonalité · modifiée ». Maquette V7 à V18.
- [x] **P3.9** Mettre à jour la maquette : bouton Écouter / Arrêter de l'accueil (P4.7).
- [x] **P4.8** Effets sonores courts (rotation, ligne, 4 lignes, fin de partie), optionnels. Fait :
  désactivés par défaut (décision du développeur), arpèges synthétisés, événement `PieceRotated` ;
  interrupteur dans P5.37.
- [x] **P4.30** Effet sonore de passage de niveau (optionnel, vu en P4.8) : repris par P5.69, sur
  l'événement `LevelUp` (P5.64), qui tombe sur le même tick que `LinesCleared` : le fusionner ou
  choisir lequel jouer.
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
  - [x] **P4.19b** Écran de l'éditeur (V8, V11, V14, V17) : `ComposerViewModel`, clé `NavKey` et
    entrée dans `app`, grille d'une mesure avec ses gestes, onglets, barre d'outils, boutons de
    mesure, noms de notes pour le lecteur d'écran, filigrane de l'autre piste, cases à deux
    doubles croches, captures des thèmes ; retirer l'exclusion `buildHealth` de `feature:composer`.
  - [x] **P4.19c** Écoute dans l'éditeur : *Écouter* par `MusicPlayer`, colonne jouée, *Suivre la
    lecture*.
  - [x] **P4.19d** Enregistrer, quitter, renommer, supprimer, avec le stockage de P4.22 : fenêtre
    d'enregistrement sur `MusicNames`, question en quittant. Reportés de la revue de P4.19b : garder
    le brouillon dans `SavedStateHandle` (rotation, mort du processus), Retour qui demande avant de
    quitter une musique modifiée, aperçu du glissé (cases traversées surlignées). Fait, avec
    l'ouverture d'une musique depuis « Mes musiques » (nom, *Modifier*).
  - [x] **P4.19e** Annuler une suppression faite depuis l'éditeur : il se ferme en rendant
    `DeletedMusic`, que `app` jette ; la spec veut le retour à « Mes musiques » avec *Annuler*.
    Relais dans `AppContainer` (flux consommé par `MyMusicsViewModel` à la reprise) ou entrée de la
    liste avec la suppression en paramètre (revue de P4.19d). Fait : relais `DeletedMusicRelay` ; depuis l'accueil, la liste prend la place de l'éditeur.
- [x] **P4.20** Bouton *Au hasard* dans l'éditeur : choix de la catégorie, annulation. Fait : aussi
  depuis « Mes musiques » (`ComposerRoute(rollDice = true)`).
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
  - [x] **P4.22c** Écran « Mes musiques » (V7, V10, V13, V16) : liste, écoute « mesure n sur 8 »,
    Utiliser / En jeu, menu « … », suppression avec confirmation et annulation, *Au hasard* et
    *Nouvelle*, navigation depuis l'accueil. Fait : module `feature:musics` ; *Au hasard* et
    *Nouvelle* ouvrent l'éditeur vierge, ouvrir une musique enregistrée attend P4.19d (toucher du
    nom, *Modifier*) et la génération P4.20.
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
- [x] **P5.5** Tests d'interface des parcours principaux (Compose UI Test). Au premier test
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
  module en a besoin. Fait : `GameScreenLifecycleTest`, `BloxNavDisplayTest` (app), deux tests de
  l'accueil ; Jupiter, `CreationExtras` et `MainDispatcherExtension` étaient déjà réglés.
- [ ] **P5.6** Build de release signé pour installation personnelle. Vérifier la restauration de
  la pile de navigation après mort du processus en build minifié (règles R8 de
  kotlinx-serialization, ADR 0011).
- [x] **P5.7** Tests de captures d'écran à partir des previews existantes (plugin officiel Compose
  Preview Screenshot Testing), pour les deux thèmes, dans la CI. Revue de P2.3 : le dessin de
  `BoardCanvas` n'est vérifié par aucun test ni à l'œil ; ajouter aussi un fichier de
  configuration de stabilité Compose (`build-logic`) listant `ch.magori.blox.core.game.model.*`.
  Fait : plugin `com.android.compose.screenshot` 0.0.1-alpha16 par la convention
  `blox.android.screenshot`, 21 captures (`docs/architecture.md`, Tests). Reste en P5.8 et P5.9.
- [x] **P5.8** Captures dans la CI : générer les références sous Linux (job ou conteneur) puis
  ajouter `validateDebugScreenshotTest` à `.github/workflows/ci.yml` ; revoir le plugin à la
  prochaine montée d'AGP (déconseillé à partir d'AGP 9.5 au profit des « test suites »).
  Revue qualité (C2) : tant que Dependabot fusionne sur CI verte, une montée de Compose peut
  changer le rendu sans signal. Fait : références Windows identiques au pixel près sous Linux (42
  captures à 0 %), validation en CI sans référence Linux ; la revue du plugin passe en P5.46.
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
- [x] **P5.29** Niveaux de départ : cibles d'au moins 48 dp de large, en deux lignes de 5
  (décision du développeur, 2026-10-19, plutôt qu'un sélecteur − / +) ; maquette à mettre à jour (C5). Fait : cellules de 59 dp à 360 dp, coche dans le coin, maquette dans P5.30.
- [x] **P5.30** Mettre à jour la maquette : choix de l'accueil (V1), choix sélectionné plein (sans
  soulignement, P5.36) et choix non sélectionnés en encart (`panel`, cadre `panelBorder`), coins carrés en Original (P5.26) ;
  bouton « Jouer » de 48 dp de haut et en `accent` (P5.3). Écart du code avec V1 (P4.22d) : la
  musique personnelle « En jeu » sans icône de note ni grille de trois colonnes, à trancher. Coche du choix sélectionné (P5.42) ; niveaux de départ sur deux lignes de 5, 52 dp de plus en hauteur (P5.29).
- [x] **P5.31** Cache de build partagé entre worktrees : deux fois, `ktlintCheck` a échoué sur les
  rapports d'un autre worktree (chemins `blox-wt/q-jeu`), vert après `--rerun-tasks --no-build-cache` ;
  trouver la tâche dont la sortie mise en cache contient des chemins absolus, et la rendre relocalisable
  ou non cacheable (revue P5.24).
- [x] **P5.32** Doublures des tests instrumentés : `GameViewModelDoubles.kt` (`NoRecords`,
  `DefaultSettings`) recopie encore les faux de `core:data`, et `NoRecords` ne respecte pas le
  contrat ; passer par `androidTestImplementation(testFixtures(projects.core.data))` (revue P0.15).
- [ ] **P5.33** Mesures sur téléphone (suite de P5.17) : `GameBenchmark` n'a jamais rendu de durée
  d'image (sur émulateur, aucune image, puis `home_play` introuvable en `benchmarkRelease` :
  lenteur ou tags perdus par R8 ; P7.21 : `uiautomator dump` du `benchmarkRelease` montre bien les tags, R8
  n'est donc pas en cause ; `LocksmithBenchmark` échoue de même sur `cartridge_locksmith`) ;
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
- [x] **P5.37** Interrupteurs « Vibrations » et « Effets sonores » de l'accueil (maquette V1 faite le
  2026-10-20, section Réglages) : les brancher (`HomeViewModel`, `HomeUiState`, `HomeScreen`, captures) et fixer
  leur valeur par défaut : désactivés (décisions du développeur, 2026-10-19 et 2026-10-20 ; P5.2,
  P4.8). Fait : section Réglages, `settingColors`, désactivés par défaut.
- [ ] **P5.38** Nom du fichier retenu sous « Mon fichier » (maquette V1 faite le 2026-10-20, « Changer »
  déjà codé), à afficher
  (`OpenableColumns.DISPLAY_NAME`) (P4.6).
- [x] **P5.36** Retirer le soulignement des choix sélectionnés de l'accueil (demande du
  développeur) : le choix sélectionné reste plein, à 3:1 au moins en luminance des choix cadrés.
- [x] **P5.39** En police agrandie (×2), l'action « Changer » sous « Mon fichier » est coupée sur
  trois lignes (captures de P4.22d) : la faire tenir (libellé, mise en page ou retour à la ligne).
- [x] **P5.40** Mettre à jour la maquette de l'éditeur (V8, V11, V14, V17) selon le code de P4.19b :
  onglets à la couleur de la piste en Couleurs, noms de ligne hors gamme en italique, hauteur
  minimale des lignes (26 dp), bouton « COMPOSER UNE MUSIQUE » de l'accueil en capitales avec icône.
  Écoute (P4.19c) : bouton rond Écouter / Arrêter à droite du tempo, contour en deux traits de la
  colonne jouée, trait sous la mesure jouée, « Suivre la lecture » dans le menu, message en bas
  de la grille. Enregistrement (P4.19d) : bouton Enregistrer sur sa ligne sous le tempo, « • » et
  plein quand des changements seraient perdus, crayon après le titre, ligne « Catégorie · modifiée »,
  fenêtre en échec. Dé (P4.20) : ligne Au hasard et catégories en rangée défilante, choix rempli
  sans coche, message d'annulation. Police ×2 (P5.47, P5.49) : barre d'outils en deux rangées,
  réponses des fenêtres empilées.
- [ ] **P5.41** Thème Brume (clair et sombre) dans le code : relever ses jetons dans la maquette
  (V5, V6, V13 à V18) et les écrire dans `docs/themes.md` ; `BloxThemeKind` et palettes, choix à
  l'accueil (V1) et à la fin de partie (V4), clair ou sombre selon le réglage du système ; jetons
  de l'éditeur (déjà relevés) ; tests de contraste (`PaletteContrastTest`, `KeyColorsTest`,
  `ChoiceColorsTest`, `TrackColorsTest`) et captures dans les deux variantes. Jetons choisis sur la
  maquette le 2026-10-20, à confirmer : cadre des touches, teintes tenues, accent tenu (docs/maquette.md).
  Amendée le 2026-10-25 (ADR 0020) : Brume entre comme deux variantes de `BloxThemeVariant`
  (`BRUME_LIGHT`, `BRUME_DARK`), après P5.57, sur les mises en page v2.
- [x] **P5.42** Repère du choix sélectionné à l'accueil, autre que la couleur (WCAG 1.4.1) : essayer
  une coche dans le choix sélectionné (décision du développeur, à tenter) ; place et taille en
  police normale et ×2, niveaux de départ compris, captures, puis maquette V1 (P5.30). Fait : coche dessinée avant le libellé (1 em, 24 dp au plus), dans le coin pour les niveaux.
- [x] **P5.43** Accueil en police agrandie : appliquer la règle de `shrinkFloor` (P2.18) aux textes
  qui rétrécissent de l'accueil (`HomeScreen`, aujourd'hui `MinimumFontSize`), en montant
  `shrinkFloor` et `shrinkToFit` dans `core:designsystem` ; captures ×2 (revue de P2.18). Vu en P5.42 : « COMPOSER UNE MUSIQUE » coupé en « COMPOSER UNE MU » à ×2 (P4.19b). Fait : `shrinkToFewestLines`, « COMPOSER UNE MUSIQUE » sur deux lignes à ×2 en Original.
- [x] **P5.44** Horloge d'images commune : `FrameTicks` (`feature:composer`, P4.19c) recopie
  `GameLoop` (`feature:game`) à son plafond de ticks par image près ; la monter dans un module
  partagé avec un plafond en paramètre (revue de P4.19c, règle de 2). Fait : `FrameTicks` dans `core:designsystem` (ADR 0012 amendé).
- [x] **P5.45** Doublons laissés par P4.22c (règle de 2) : `ComposerHeader` garde ses copies
  privées de `GlyphButton`, `drawBack` et `drawDots`, à remplacer par `GlyphButton` et `Glyphs` de
  `core:designsystem` ; `NameProblem` de la liste et `SaveProblem` de l'éditeur (nom vide, pris,
  trop long) à réunir dans `core:data` (`MusicNames`) ; fenêtres Renommer et confirmation de
  suppression, et leurs chaînes, en double dans `ComposerDialogs` et `MyMusicsDialogs` (revue de
  P4.19d). Après P4.19d. Fait : `nameProblem`, `cutName`, `NamingWindow`, `DeleteWindow`.
- [ ] **P5.46** Plugin des captures (Compose Preview Screenshot Testing, alpha) : le revoir à la
  prochaine montée d'AGP, déconseillé à partir d'AGP 9.5 au profit des « test suites » ; garder
  la validation en CI (P5.8).
- [x] **P5.47** Éditeur en police ×2 (captures de P4.20) : le bouton « + » du tempo sort de l'écran,
  poussé par « 120 temps/min » sur deux lignes ; le message d'annulation couvre presque toute la
  grille ; une catégorie longue (« Mélancolique ») dépasse la rangée du dé. Fait : tempo mesuré après ses boutons, message sur trois lignes,
  choix plafonnés à la rangée.
- [x] **P5.48** Fenêtres en police ×2, thème Original : les libellés des réponses se coupent au
  milieu d'un mot (« Annul/er », « Enreg/istre/r ») ; les faire passer à la ligne entre les mots, ou
  les empiler (vu en P5.45). Fait : réponses empilées quand elles ne tiennent pas côte à côte,
  retour à la ligne seulement après une espace.
- [x] **P5.49** Barre d'outils de l'éditeur en Original ×2 : ses libellés restent coupés même au
  plancher (« Pose », « Allon », « Annul », « Copi », « Effac ») ; fenêtre Quitter : le point final
  de « enregistrés. » passe seul à la ligne (vus en P5.47). Fait : barre sur deux ou plusieurs
  rangées quand elle ne tient pas, `WindowText` en mots entiers ; maquette ajoutée à P5.40.
- [x] **P5.50** Page Paramètres (maquette V21 faite le 2026-10-20, demande du développeur) : les
  interrupteurs Vibrations et Effets sonores (P5.37) quittent l'accueil pour une page à part, ouverte
  par un bouton engrenage de 48 dp en haut à droite de V1 (« Paramètres » pour TalkBack) ; clé
  `NavKey` et écran (dans `feature:home` ou une feature dédiée), retour vers l'accueil ; tests de
  `HomeScreenTest` déplacés, captures, `docs/maquette.md` (V1, V21). Les réglages Moderne (P6.17,
  P6.21) iront sur cette page. Fait : `SettingsScreen`, `SettingsViewModel`, `SettingsRoute` dans
  `feature:home` ; en-tête affiché pendant la lecture des réglages.
- [x] **P5.51** Accueil raccourci (maquette V1 du 2026-10-20, 940 px au lieu de 1360, demande du
  développeur) : logo et titre sur une ligne, mode de jeu sur une ligne (*Moderne* grisé, sans
  « (bientôt) », `feature_home_mode_modern`), sections à 16 dp, une seule ligne d'actions
  (*Écouter* / *Arrêter*, fichier, *Changer*) sous les choix de musique, pour la musique
  sélectionnée ; TalkBack (« Écouter Menuet »), police ×2, captures (`docs/maquette.md`, V1).
  Fait : nom du fichier sous « Mon fichier » laissé à P5.38 ; *Moderne* sous *Classique* en
  police ×2.
- [x] **P5.52** Cartes de « Mes musiques » sur une rangée (maquette V7, V10, V13, V16 du
  2026-10-20, demande du développeur) : nom sur une ligne, une ligne style · tonalité · durée,
  *Utiliser* / *En jeu* en bouton carré de 48 dp (note, coche en jeu), tempo et date retirés de la
  carte ; police ×2, TalkBack, captures, tests de P4.29 à revoir (`docs/generateur-musique.md`). Fait : rangée de 48 dp (56 avec la marge), ligne d'info lue sans « · », `dayText` et `spokenDay` retirés.
- [x] **P5.53** Mettre à jour la maquette : icônes (demande du développeur, 2026-10-20) ; en pixels
  8 × 8 pour Original, en traits arrondis pour Couleurs et Brume ; Jouer, MES MUSIQUES, Écouter,
  Paramètres, Reprendre, Rejouer, Menu, Au hasard, Nouvelle, menus « … », barre d'outils de
  l'éditeur, touches A et B (flèches de rotation) (V1 à V20). Fait : `docs/maquette.md`, section Icônes.
- [x] **P5.54** Icônes dans le code (maquette P5.53), en deux tranches. Jeu d'icônes dans
  `core:designsystem`, une variante par thème (vecteurs en pixels pour Original, traits pour
  Couleurs et Brume), choisie par le thème ; décoratives à côté d'un libellé,
  `contentDescription` seulement quand l'icône est seule ; captures dans tous les thèmes.
  - [x] **P5.54a** Le jeu d'icônes, puis l'écran de jeu (touches A et B : flèche de rotation
    au-dessus de la lettre, P2.17, P2.5), la pause, la fin de partie et l'éditeur (barre d'outils
    icône au-dessus du libellé, police ×2 comprise, P5.49 ; Au hasard ; menu). Fait : `BloxIcon`,
    lettres A et B gardées en 20 sp (contraste) ; tests instrumentés verts le 2026-10-21
    (`BloxIconSemanticsTest`, `ComposerScreenTest`, `GameScreenKeysTest`).
  - [x] **P5.54b** L'accueil (Jouer, MES MUSIQUES, Écouter, COMPOSER UNE MUSIQUE, engrenage) et
    « Mes musiques » (écoute, Au hasard, Nouvelle, menu « … »), après P5.50 à P5.52. Fait : `GlyphButton(icon)` pour les icônes seules, `IconSizes` complété ;
    tests instrumentés verts (`HomeScreenTest`, `MyMusicsScreenTest`, `GlyphButtonTest`).
- [ ] **P5.55** Imports inutilisés non détectés : la règle `no-unused-imports` de ktlint 1.8.0 est
  dépréciée et ne se déclenche pas, et Kotlin ne les signale pas (vu à la revue de P5.54a) ;
  ajouter une vérification (detekt `UnusedImports` ou équivalent) à `build-logic` et à la CI.

Lot « Couleurs v2 » (conçu avec les agents `designer-jeu` et `architecte`, validé par le développeur le
2026-10-25, phase P5 à sa demande) : thème Couleurs refait en piste C « Y2K irisé », jour et nuit selon le
mode du téléphone ; écran de choix du jeu (cartouche Classique seule) ; accueil, jeu, pause, fin de partie et
Paramètres v2. Source : le [design system](docs/design-system/README.md) (`tokens.json`, `components/`) et les
rangées « Couleurs v2 » de la maquette. Décisions : ADR 0019 à 0024. Ordre : fondations (jetons, polices,
formes, composants), puis données et domaine (indépendants, menables en parallèle), puis écrans ; Paramètres
d'abord, chaque réglage arrivant avec ce qu'il règle (aucun interrupteur sans effet). Composants partagés dans
`core:designsystem`, jamais recopiés ; revue indépendante à chaque tâche (définition de terminé).
**Formats** (décision du développeur) : le téléphone debout d'abord ; le téléphone couché et les tablettes
sont optionnels et viennent en dernier (P5.84), mais le code les prévoit dès le départ : mise en page choisie
par une fonction pure de la taille disponible, aucune hypothèse « portrait » ni largeur d'écran fixe dans les
composants.

- [x] **P5.56** Préparer le lot (documentation, sans code) : corriger les écarts du design system (`space-5`
  cité mais absent, `type.fonts` vide, graisses de `button-label` et `choice` contraires à Button et Switch,
  usage de `radius-md`, largeur et ordre des encarts de StatPanel contraires à la section Jeu, README de
  `Cover`) ; numéroter dans `docs/maquette.md` les vues Couleurs v2 (choix du jeu, accueil, jeu, pause, fin de
  partie, Paramètres) et les V22 à V25 que cite le design system ; lister les pictogrammes et le logo à
  exporter de la maquette (cartouche, moins, étoile, note barrée, égaliseur, rangées des Paramètres, logo BloX
  v2, mascotte), en pixels 8 × 8 et en traits ; écrire et accepter les ADR 0019 à 0024 ; trancher les points
  ouverts (valeurs Original des rôles nouveaux, NOUVEAU RECORD sans record, volume à 0, fondu de thème) ;
  noter dans `JOURNAL.md` les décisions qui reviennent sur P5.29 (niveaux), P4.20 (dé, coches) et l'ordre des
  réponses de P5.45 ; amender P5.41 (Brume codé en variantes, ADR 0020). Cas : `scripts/verifier-tracabilite.sh`
  vert.
- [x] **P5.57** Couleurs v2, jour et nuit (`core:designsystem`, ADR 0019, 0020) : `BloxThemeVariant`
  (`ORIGINAL`, `COLORFUL_LIGHT`, `COLORFUL_DARK`) résolue par `BloxTheme` depuis `BloxThemeKind` et le mode
  sombre du système, exposée par `BloxTheme.variant` ; palettes `ColorfulLight` et `ColorfulDark` aux valeurs de
  `tokens.json`, à la place de `Colorful` ; rôles nouveaux de `BloxPalette` (`emptyCell`, `panelInset`, `scrim`,
  `accentRing`, `clearGlow`, `dangerSoft`, `cellDead`, et les trois arrêts `iris1` à `iris3`, ou une structure `IrisColors`) avec leurs valeurs Original (P5.56) ; table explicite jeton → propriété (ADR 0019 : les noms ne se déduisent pas tous, ex. `board` → `boardBackground`, `label-off` → `outOfScale`) ; `CLAUDE.md` et `docs/architecture.md` (plugin `blox.design.tokens`) mis à jour ; convention
  `blox.design.tokens` (le fichier en entrée des tâches de test, chemin relatif, rien d'absolu dans le cache,
  P5.31) ; `kotlinx-serialization-json` en dépendance de test. Écrans inchangés hormis les couleurs.
  Cas : `each colour token of tokens json has its value in the palette of the day and in the palette of the night`,
  `every colour token of tokens json is in the palette or listed as not coded, with its reason`,
  `a colour written rgba in tokens json is read with its alpha`,
  `the Colorful theme is the day variant in light mode and the night variant in dark mode`,
  `the Original theme has a single variant, whatever the mode of the system`,
  `each variant has its palette and its Material colour scheme, light or dark`,
  `the painters are built again when the variant changes` (clé des peintres `(variante, taille, fond)`, P5.73) ; `PaletteContrastTest`,
  `KeyColorsTest`, `PauseButtonColorsTest`, `ChoiceColorsTest`, `SettingColorsTest`, `TrackColorsTest`,
  `LogoColorsTest` sur chaque variante ; captures jour et nuit.
- [x] **P5.58** Polices v2 (ADR 0021) : Manrope, Unbounded et Bagel Fat One embarquées telles que publiées
  (OFL, licences dans les assets), Chakra Petch retirée ; styles de texte du design system (`screen-title` à
  `logo`) dans `BloxTypography`, comparés à `tokens.json` ; Original inchangé ; valeurs à chiffres fixes
  (`tnum` de Manrope s'il existe, sinon `DigitWidths`) sur le fichier (confirmé : `tnum`) ; taille de l'APK mesurée et notée dans l'ADR 0021 (+1,0 Mo). Après P5.57.
  Cas : `each text style of tokens json has its family, size, line height and weight in the Colorful typography`,
  `the logo style is in Bagel Fat One and is used for the logo only`, `EmbeddedFontsTest` (licence de chaque
  police, plus de Chakra Petch), `ValueDigitsTest` (dix chiffres de même largeur, sans crénage),
  `BloxTypographyTest` revu ; captures, police ×2 comprise.
- [x] **P5.59** Formes, reliefs, tailles et durées v2 : `BloxShapes` (`radius-cell`, `-md`, `-window`, `-card`,
  `-pill`, `-snack`), reliefs `button-gloss`, `button-gloss-pressed` et `cell-gloss` dessinés en ombres intérieures (aucun
  en Original), tailles (`touch-target`, `setting-row`, `switch-track`, `switch-thumb`, `key-*`), espacements,
  durées `motion-*` en ticks. Après P5.57.
  Cas : `each radius, size and space of tokens json is the one of the code`,
  `each duration of tokens json is a whole number of ticks, the nearest to its milliseconds`,
  `the Original theme has square corners and no gloss` ; captures des reliefs.
- [x] **P5.60** Boutons bulles (`core:designsystem`, composant Button) : Principal XL, Principal, Secondaire,
  Icône ; repos, pressé (2 dp plus bas, remplissage pressé tout de suite, retour en `motion-press`), désactivé,
  focus (contour de 3 dp `ink` à 3 dp) ; ils remplacent `PrimaryButton` et `SecondaryButton`
  (`feature:game`, `ui/TitledScreen.kt`), `SettingsButton` et le fond de `GlyphButton`. Après P5.58, P5.59.
  Cas : `a pressed button goes 2 dp down and takes its pressed fill at once`,
  `a released button comes back in 6 ticks`, `a disabled button has no gloss, its label in label and its frame in
  panel border`, `every family is 48 dp high at least, Play 72 and a main or secondary button 56`,
  `the label of each family stands out from its fill by 4,5 to 1 at least, at rest, pressed and coming back`,
  `a focused button bears a 3 dp outline in ink` ; captures de chaque famille et état, jour, nuit, Original.
- [x] **P5.61** Interrupteur et rangée de réglage (composant Switch) : `SettingSwitchRow` (pictogramme, libellé,
  sous-ligne, piste), toute la rangée est l'interrupteur ; état désactivé avec sa raison ; remplace `Setting` et
  `settingColors` (`feature:home`). Après P5.60.
  Cas : `TalkBack reads one switch per row, its label, its sub-line and its state`,
  `a disabled row reads as unavailable and its sub-line says why`,
  `a switch on has its thumb on the right, larger, checked, a switch off on the left`,
  `a pressed row fills with button pressed and its thumb grows to 28 dp`,
  `the track is framed in button outline, which stands out from the page by 3 to 1, on and off` ; captures.
- [x] **P5.62** Curseur à pas (composants Choice et Switch) : logique pure `SteppedValue` (valeur la plus proche
  d'une position, bornes, pas), curseur de volume (pas de 10 %, valeur en %) et jauge du niveau (10 barres, − et
  +), un seul curseur pour TalkBack. Après P5.60.
  Cas : `a touch on the track gives the nearest step`, `a drag never takes the value out of its range`,
  `minus is disabled at the lowest step and plus at the highest`,
  `TalkBack reads one slider with its value and moves it one step at a time`,
  `each bar of the gauge is 8 dp wide and from 8 to 26 dp high, in accent up to the value` ; captures.
- [x] **P5.63** Groupe de choix (composant Choice) : `ChoiceGroup` (cadre `button-outline` autour du groupe,
  segment retenu en `accent` avec coche, segment indisponible avec `BIENTÔT`) ; remplace `Choice`, `ChoiceRow`,
  `ChoiceCheck` et `choiceColors` (`feature:home`). Après P5.60.
  Cas : `exactly one segment is selected, checked and in accent`,
  `an unavailable segment reads « Moderne, bientôt, indisponible » and cannot be chosen`,
  `a group too wide for its line puts its label above and takes the whole width`,
  `each segment is 48 dp high as a target` ; captures, police ×2.
- [x] **P5.64** Événement `GameEvent.LevelUp(level)` (`core:game`) : envoyé au tick où le niveau monte, après
  `LinesCleared` et avant `StackDanger` ; branche `null` dans `soundEffectFor` et `hapticFor` en attendant
  P5.69. Spécification `docs/regles-mode-classique.md` (Niveaux).
  Cas : `a line clear that reaches the next level sends a level up with the new level`,
  `a line clear that stays at the same level sends no level up`,
  `the level up comes after the lines cleared and before the stack danger of that tick`,
  `starting at level 5 the first level up comes at 60 lines, not at 10`,
  `a game that starts sends no level up`.
- [x] **P5.65** Préférences v2 (`core:data`, `core:music`) : `Volume` (pourcentage de 0 à 100 par pas de 10,
  `core:music`) ; `UserPreferences.reducedEffects`, `leftHanded`, `keyVibration`, `musicVolume`,
  `effectsVolume` ; effets sonores activés par défaut (décision du développeur, 2026-10-25), tests de P4.8 revus.
  Cas : `a volume is a multiple of 10 from 0 to 100`, contrat (`without saved preferences the sound effects are on`,
  `without saved preferences effects are not reduced, the player is not left handed, the keys do not vibrate and
  both volumes are 100`, `each new setting saved is read back`),
  `settings saved before Couleurs v2 keep their choices`, `each new setting is stored under a key that must not
  change`.
- [x] **P5.66** Effacer le record (`core:data`) : `HighScoreRepository.clear()`, `IOException` si non écrit.
  Cas (contrat, vrai dépôt et faux) : `a cleared record is 0`, `a score after a clear is a new record`,
  `clearing without a record leaves 0`, `a record that cannot be written fails with an IOException`.
- [x] **P5.67** Vibreur et vibration des touches (`core:haptics`, `feature:game`) : `Haptics.canVibrate` ;
  `Haptic.KEY_PRESS`, effet le plus léger, attributs du toucher (`USAGE_TOUCH`, que le réglage « vibration au
  toucher » d'Android coupe) ; en jeu, l'appui d'une touche vibre si `keyVibration` est allumé. Après P5.65.
  Cas : `the key press is the predefined tick from Android 10 and a pulse of 10 ms before` **[à vérifier]**,
  `a pressed key vibrates once when key vibration is on`, `a released key does not vibrate`,
  `with key vibration off no key vibrates`.
- [x] **P5.68** Volumes (`core:audio`, `app`) : `setVolume(Volume)` du séquenceur, du lecteur de fichier, du
  sélecteur et du décorateur de focus, et des effets ; courbe perceptive `Volume.gain` ; `AppContainer` applique
  les volumes enregistrés au lecteur partagé, en direct (aperçus et parties). Après P5.65.
  Cas : `a volume of 0 gives a gain of 0 and 100 a gain of 1`, `the gain grows with the volume`,
  `MusicPlayerContract › a volume set before a play applies to it`, `the volume of the effects applies to each
  track`, `a volume changed in the settings applies to the music playing`.
- [x] **P5.69** Son « niveau » (`core:music`, `feature:game`) : `SoundEffects.levelUp`, réchauffé avec les
  autres, joué à `LevelUp` ; aucune vibration. Après P5.64.
  Cas : `the level up sound is an effect of its own, shorter than the four lines`,
  `a level up plays the level up sound when the sound effects are on`, `a level up does not vibrate`.
- [x] **P5.70** Paramètres v2, Sons et vibrations et Record (`feature:home`, maquette Couleurs v2) : barre
  (Retour, « Paramètres » en `screen-title`), cartes ; Effets sonores, Volume des effets (grisé sans effets),
  Volume de la musique, Vibrations, Vibration des touches, état sans vibreur, « Valable dès la partie
  suivante. » ; Record : Effacer, `ConfirmWindow` générique (sur laquelle `DeleteWindow` est réécrite ; réponse
  sûre « Garder le record » d'abord, P5.45), « Record effacé » lu par TalkBack ; un réglage allumé se fait
  sentir une fois. Après P5.60 à P5.63, P5.65 à P5.68.
  Cas : `turning the sound effects on plays the rotation once`, `turning a vibration on vibrates once`,
  `without a vibrator both vibrations are disabled and say « Cet appareil ne vibre pas »`,
  `the volume of the effects is disabled while the sound effects are off`,
  `clearing the record asks first, and keeping it changes nothing`,
  `a confirmed clear clears the record and says « Record effacé »`,
  `a clear that cannot be written keeps the window open and says it failed` ; captures (téléphone, police ×2,
  jour, nuit, Original).
- [x] **P5.71** Effets réduits (`core:designsystem`, `app`, `feature:home`, ADR 0024) : `BloxEffects(reduced,
  forcedBySystem)` porté par `BloxTheme`, résolu par une fonction pure depuis la préférence et
  `ValueAnimator.areAnimatorsEnabled()` (lu par `MainActivity` à chaque reprise) ; carte Affichage avec Effets
  réduits (allumé et bloqué quand Android l'impose, sans écraser le choix) et la note sur le thème ; les boutons
  pressés ne descendent plus en Effets réduits, mais une touche de jeu tenue garde sa descente de 2 dp : ce n'est pas
  une animation, et c'est le seul repère de A tenue de jour (1,4:1 contre A au repos ; décision de la revue de
  P5.57). Cas : `a held key goes 2 dp down with its tag and what TalkBack reads, reduced effects or not`. Après P5.70.
  Cas : `effects are reduced when the player asks for it`, `effects are reduced and forced when Android removes
  the animations, whatever the choice`, `the choice of the player comes back once Android animates again`,
  `a forced switch reads « Imposé par les réglages d'Android » and cannot be turned off`,
  `in reduced effects a pressed button changes its fill only`.
- [x] **P5.72** Choix du jeu, cartouches (nouvelle feature `feature:cartridges`, ADR 0022) : `CartridgesRoute`
  à la racine de la pile, cartouche Classique seule (liste à une entrée ; aucune cartouche endormie, décision
  du développeur), qui ouvre l'accueil ; disque « Changer de jeu » de l'accueil par `changeGame` (`app`,
  fonction pure : dépile jusqu'aux cartouches, ou les met à la racine si la pile restaurée n'en a pas) ;
  `BloxNavDisplayTest`, `BloxJourney`, profil de référence regénéré sur appareil ; `CLAUDE.md` (tableau des
  modules et des plugins) et `docs/architecture.md` mis à jour. Après P5.60.
  Cas : `the application opens on the choice of the game`, `the Classic cartridge opens the Classic home`,
  `changing the game from the home goes back to the cartridges`,
  `changing the game from a restored stack without cartridges puts them at its root`,
  `the system back on the cartridges leaves the application`, TalkBack (nom de chaque cartouche) ; captures.
- [x] **P5.73** Rendu du plateau partagé (nouveau module `core:board`, ADR 0023) : `BoardCanvas`,
  `CellPainter`, `CellStyles`, `CellStyle`, `BoardGeometry`, `DisplayedCells`, `CellLayout`, `CellPixels`,
  `NextPieceCanvas` et `NextPieceCells` quittent `feature:game`. Pur déplacement, aucun pixel ne change
  (`CLAUDE.md`, `docs/architecture.md` et `docs/themes.md` mis à jour) ; fait avant P5.57, la clé des peintres
  `(variante, taille, fond)` et son test `the painters are built again when the variant changes` passent en P5.57.
  Cas : tests déplacés verts (`BoardCanvasTest`, `CellStylesTest`, `BoardGeometryTest`, `DisplayedCellsTest`,
  `CellPixelsTest`, `CellLayoutTest`), captures à 0 % d'écart, `buildHealth` vert.
- [x] **P5.74** Cases « bulles » (`core:board`, composant Piece) : dégradé `piece-<x>-hi` → `piece-<x>`,
  `radius-cell` (2 dp sous 16 dp), `cell-gloss` à 16 dp et plus, case vide `empty-cell` sur `board`, marge de
  1 px ; Original inchangé. Après P5.59, P5.73.
  Cas : `each piece token of tokens json is the fill and the highlight of its piece, day and night`,
  `a cell under 16 dp has 2 dp corners and no gloss`, `an empty cell has the corners and the margin of a piece,
  without gloss`, `the painters are still built once while the piece moves` ; captures.
- [x] **P5.75** Accueil v2, structure (`feature:home`, maquette Couleurs v2, téléphone debout ; mise en page
  choisie par la taille disponible, P5.84) : barre (Changer de jeu, puce Record, Paramètres), Jouer XL avec le
  rappel « Classique · Niveau 05 · Korobeïniki », carte des réglages (Mode avec Moderne `BIENTÔT`, Thème, Niveau
  en jauge, rangée Musique), tuiles Mes musiques et Composer. Remplace les niveaux en deux lignes de 5 (P5.29,
  décision revue par la maquette v2). Après P5.60 à P5.63, P5.72.
  Cas : `the reminder under Play says the mode, the level on two digits and the music, read whole by TalkBack`,
  `the gauge chooses the start level and saves it`, `every target of the home is 48 dp at least on 360 dp`,
  `TalkBack reads the parts in the order of the screen` ; `HomeScreenTest` revu ; captures, police ×2.
- [x] **P5.76** Feuille Musique (`feature:home`) : (à trancher ici : le rappel sous Jouer dit « Mon fichier » même si le fichier manque, P5.75) feuille qui monte (`motion-card`), une rangée de 56 dp par
  musique (Korobeïniki, Menuet, Mon fichier, musique en jeu, Aucune), coche et Écouter ; choisir enregistre sans
  fermer ; un seul aperçu à la fois ; feuille en `BloxSheet` et rangées en `SheetChoiceRow` (P5.96), Écouter en
  `ListenButton` (P5.94). Après P5.75, P5.94, P5.96.
  Cas : `choosing a music saves it and keeps the sheet open`, `listening to a music stops the one previewed`,
  `closing the sheet stops the preview`, `Aucune shows the note barred`, `the sheet is read as a dialog by
  TalkBack` ; captures.
- [x] **P5.77** Héros de l'accueil : (mascotte aussi sur la carte de la pause, P5.82) logo BloX v2 (Bagel Fat One en dégradé `iris`, mascotte ; Original garde
  ses quatre cases), plateau d'aperçu (`core:board`) qui rejoue un T tombant à la gravité du niveau choisi et
  effaçant deux lignes, score réel ; halo de Jouer (3 impulsions après 3 s, puis une toutes les 10 s, aucun en
  Effets réduits). Après P5.71, P5.74, P5.75, P5.87 (logo exporté).
  Cas : `the preview drops a row every ticks per row of the chosen level`, `the preview scores 100 times the
  level plus one for its two lines`, `the preview starts again with the new level when the level changes`,
  `in reduced effects the preview and the halo stand still`, `nothing on the home recomposes while the preview
  plays`.
- [x] **P5.78** Jeu v2, barre et colonne (`feature:game`) : Pause en disque, « MODE CLASSIQUE », record qui
  devient l'autocollant NOUVEAU RECORD une fois battu et suit le score (`GameUiState.recordBeaten`) ; plateau en
  cadre `board` ; colonne SUIVANTE, SCORE avec barre de record de 4 dp, LIGNES, NIVEAU. Après P5.60, P5.74.
  Cas : `the record is beaten once the score is above a record above 0`, `without a record the game shows no new
  record` (P5.56), `the record bar fills with the score up to the record`, `nothing on the screen recomposes
  when only the piece moves` ; captures.
- [x] **P5.79** Touches v2 et Gaucher (`feature:game`, `feature:home`) : flèches de 60 dp, B et A de 68 dp au
  pictogramme seul (nom lu par TalkBack), B plus bas ; tailles réduites sous 390 dp de large pour tenir à 360 dp
  ; préférence Gaucher lue à la création de la partie, `gameLayout(…, leftHanded)` ; rangée Gaucher dans
  Affichage avec son schéma. Après P5.65, P5.71, P5.78.
  Cas : `left handed puts B and A on the left and the arrows on the right`, `the keys fit a 360 dp phone with 16
  dp margins`, `TalkBack reads « B, rotation anti-horaire » and « A, rotation horaire »`, `the reading order
  stays arrows then B and A, left handed or not`, `KeyColorsTest` revu ; captures.
- [x] **P5.80** Retours de jeu, première tranche (`feature:game`, ADR 0024) : `GameFeedback` (pur, en ticks)
  dans `GameUiState` ; calque dessiné par-dessus le plateau, en phase de dessin ; pièce posée qui s'éclaire,
  pictogramme de A ou B qui tourne, effacement calé sur `Phase.Clearing` (rangées `clear-glow`, cases qui
  éclatent par paires), rangées qui glissent, autocollant « +points », score qui compte ; variantes Effets
  réduits. Après P5.71, P5.78, P5.79.
  Cas : `a lock lights its four cells for 8 ticks`, `a rotation turns the icon of its key by 90 degrees in 6
  ticks`, `the clear pops the cells by pairs from the centre within the clear delay`, `the points sticker rises
  and fades in 45 ticks`, `in reduced effects nothing moves and the score shows at once`, `a paused game freezes
  its effects`, `nothing recomposes while an effect plays`, `the painters are built once while effects play`.
- [ ] **P5.81** Retours de jeu, seconde tranche : 4 lignes (reflet irisé autour du cadre, autocollant « 4
  LIGNES »), `LevelUp` (encart NIVEAU qui bascule et s'encadre d'`accent` 1 s, annonce TalkBack), `StackDanger`
  (cadre `danger` de 4 dp, 5 rangées du haut en `danger-soft` qui respirent), record battu (rebond, barre
  irisée). Après P5.64, P5.80.
  Cas : `four lines run the iridescent border once`, `a level up announces the new level to TalkBack`,
  `the danger frame is 4 dp and stays until the danger ends`, `nothing flashes more than 3 times a second`,
  `there are 40 particles at most`, variantes Effets réduits.
- [x] **P5.82** Pause en rideau : rideau de cases vides sur le plateau et la pièce suivante (`motion-curtain`),
  puis la carte (mascotte, score, niveau, lignes, Reprendre, Menu) ; Reprendre rouvre le rideau puis relance la
  partie ; horloge de Compose, la boucle de jeu reste arrêtée. Après P5.60, P5.78.
  Cas : `the curtain hides the board and the next piece before the card shows`, `the game resumes only once the
  curtain is open`, `in reduced effects the curtain fades in 8 ticks at most`, `the system back resumes as
  Reprendre does` ; captures.
- [ ] **P5.83** Fin de partie v2 : pile qui s'éteint rangée par rangée de bas en haut (`cell-dead`, 2 ticks par
  rangée, sur l'horloge de Compose), carte, NOUVEAU RECORD avec confettis, score qui compte, « Record » ou «
  Ancien record » (`GameOverUiState.previousBest`, P9.14, de `RecordResult.previous`), lignes, niveau, 4 lignes, durée,
  Rejouer, Menu. Après P5.81, P5.82.
  En fin de partie, un premier score positif reste un nouveau record (spécification actuelle), contrairement au jeu (P5.78).
  Cas : `a new record shows the old record as « Ancien record »`, `a first positive score is shown as a new record, without an old record`, `without a new record the record is shown as «
  Record »`, `the stack goes out from the bottom, 2 ticks a row`, `in reduced effects the stack goes out at once
  and the score shows at once`, `the game loop stays stopped while the game over plays` ; captures.
- [ ] **P5.84** Formats supplémentaires (optionnel, en dernier, décision du développeur) : téléphone couché et (avec les tailles de touches tablette, `key-*-tablet`, que P5.79 ne prend pas encore)
  tablettes debout et couchée pour l'accueil (deux colonnes), les Paramètres (Affichage et Record à gauche, Sons
  et vibrations à droite) et le jeu (mesures v2 par format) ; retrait de `screenOrientation="portrait"` (à
  trancher avec P2.8). Après P5.70, P5.77, P5.83.
  Cas : `on a phone in landscape the home puts logo, preview and tiles on the left`, `the settings take two
  columns in landscape`, `each format keeps every target at 48 dp`, captures des quatre formats.
- [x] **P5.85** (abandonnée le 2026-10-26 : le thème Original est figé, décision du développeur) Mettre à jour la maquette : écrans v2 en thème Original (choix du jeu, accueil, jeu, pause, fin de
  partie, Paramètres), valeurs Original des rôles nouveaux (P5.57) ; tailles des touches sous 390 dp (P5.79).
- [ ] **P5.86** Couleurs v2 de « Mes musiques », de l'éditeur et d'Enregistrer (V32 à V34, jour, dessinées le
  2026-10-25 ; conception de l'agent `designer-jeu` validée le 2026-10-25 : carte « Pendant les parties », notes
  tenues bordées, mesures en 2 × 4, dé en feuille, une seule couleur d'action, Enregistrer en feuille au-dessus du
  clavier, coches sur les choix retenus). Composants partagés d'abord (`core:designsystem`), puis les écrans ;
  Original n'a pas de travail propre, il doit seulement rester utilisable. Découpée le 2026-10-26 par l'agent
  `architecte` ; décisions du développeur : taille compacte de 48 dp, réponses des fenêtres toujours empilées (la
  sûre d'abord), Renommer en fenêtre v2. Tranches :
  - [x] **P5.93** Boutons bulles, états et variantes de la musique (`core:designsystem`, composant Button) :
    état retenu (`selected` : fond et cadre `accent`, encre `accent-ink`, pressé `accent-pressed`) pour les mesures
    et le disque Utiliser / En jeu ; état en cours (`active` : fond `button-pressed` sans descente) pour Écouter
    pendant l'écoute ; disque nu (`GlyphButton(…, bare = true)` : ni fond, ni cadre, ni relief au repos,
    `button-pressed` pressé, 48 dp) pour « … », − et + du tempo et la croix des messages et des feuilles ; taille
    compacte de 48 dp (`compact`) des familles Principal et Secondaire, en pilule ou en tuile `radius-md` ;
    pictogrammes `CHECK` et `ALERT` dans `BloxIcon`. Design system (README, Boutons) mis à jour des deux côtés.
    Après P5.60, P5.75.
    Cas : `a selected button is filled and framed in accent, its ink in accent ink, and pressed in accent pressed`,
    `the ink of a selected button stands out from its fill by 4,5 to 1 at least, at rest and pressed, in every
    variant`, `an active button keeps the pressed fill at rest, without going down`, `a bare icon button has no
    fill, no frame and no gloss at rest, and takes button pressed when pressed`, `a bare icon button is 48 dp wide
    and high at least`, `a compact button is 48 dp high, in the colours of its family` ; captures.
  - [x] **P5.94** Écouter partagé et égaliseur (`core:designsystem`, `feature:home`, `feature:composer`) :
    `ListenButton(playing, description, size)` (disque de la famille Icône, triangle puis carré, état en cours
    pendant l'écoute ; 48 dp, 56 dans l'éditeur) remplace les copies de `feature:home` et de `ComposerControls.kt` ;
    `Equalizer` (trois barres de 3 × 14 en `accent`, de 35 % à 100 % en 30 ticks, sur l'horloge de Compose,
    dessinées sans recomposer), immobile en Effets réduits et en Original. Après P5.71, P5.75, P5.93.
    Cas : `the listen button shows a triangle, and a square while playing, read Écouter or Arrêter with the name of
    the music`, `a music playing keeps its listen button in the pressed fill`, `the equalizer moves its three bars
    between 35 and 100 percent of their height in 30 ticks`, `in reduced effects or in Original the equalizer stands
    still`, `the equalizer is left out by TalkBack`, `nothing recomposes while the equalizer moves` ; captures.
  - [x] **P5.95** Segments de choix sans titre, à pictogramme et à couleur de piste (`core:designsystem`, composant
    Choice) : `ChoiceSegments` public (segments de `ChoiceGroup` sans `LabelledControl`, nommés pour TalkBack),
    `ChoiceItem.icon` (pictogramme avant le libellé, remplacé par la coche sur le segment retenu), couleurs
    retenues par segment (`ChoiceItem.selectedColors`, ici `TrackColors.tab` et `tabInk`) ; pistes et mesures lues
    comme des boutons radio. Après P5.63, P5.75.
    Cas : `segments without a visible label are one group of radio buttons, read under the name given to TalkBack`,
    `a segment bears its icon before its label, and the selected one its check in its place`, `a selected segment
    takes the colours given to it, its label standing out by 4,5 to 1 at least, day and night`, `each segment is 48 dp
    high as a target` ; captures.
  - [x] **P5.96** Feuille du bas (`core:designsystem`, ADR 0029) : `BloxSheet` (fenêtre plein écran, voile `scrim`,
    panneau `panel` cadré de `panel-border` sans bord bas, coins `radius-card` en haut, poignée, titre ; monte en
    `motion-card`, fondu de `motion-reduced-fade` en Effets réduits ; retour du système et toucher du voile la
    ferment, sans glissement ; au-dessus du clavier et des barres système, le contenu défile, la dernière rangée
    d'actions restant visible) ; `SheetChoiceRow` (rangée de 56 dp, rond puis `CheckMark` une fois retenue, libellé
    et sous-ligne). ADR 0029 (feuilles du bas maison dans une fenêtre, ouvertes par l'état de l'écran). Après
    P5.71, P5.93.
    Cas : `a sheet is read as a dialog by TalkBack, its title first`, `the system back and a touch on the scrim close
    the sheet`, `a sheet rises in 18 ticks, and in reduced effects fades in within 8 ticks`, `a sheet lifted by a
    bottom inset keeps its last row of actions in sight, its content scrolling`, `a choice row is 56 dp high, read as
    a radio button with its label and its sub-line, the chosen one checked` ; captures.
  - [x] **P5.97** Menu « … » et message temporaire (`core:designsystem`) : `BloxMenu` et `BloxMenuItem` (panneau
    `panel` cadré `radius-md`, rangées de 48 dp, pictogramme décoratif, variante `danger`, variante cochée ; ombre
    par un jeton à ajouter, ADR 0019), à la place du `DropdownMenu` de Material ; `SnackMessage` (fond `ink`, texte
    `page`, `radius-snack`, action en texte et croix en disque nu, région vive polie ; l'action passe sous le texte
    plutôt que de le couper), à la place de `UndoMessage` et `MessageBar`. Après P5.93.
    Cas : `a menu item is 48 dp high, read as its label, its icon adding nothing`, `a deleting item is in danger,
    icon and label`, `a checked item is read as a checked check box and bears a check`, `a message is read politely
    by TalkBack as it shows`, `the text of a message stands out from ink by 4,5 to 1 at least, day and night`, `in a
    font twice as large the action of a message goes under its text, no word cut` ; captures.
  - [x] **P5.98** Fenêtres v2 et champ du nom (`core:designsystem`) : titre de `WindowPanel` en `screen-title` ;
    `WindowButton` en bulles (Secondaire, Principal, Danger plein avec la corbeille), réponses toujours empilées sur
    toute la largeur, la sûre d'abord ; `NameField` extrait de `NamingWindow` (fond `panel-inset`, 56 dp,
    `radius-md`, cadre au focus et `danger` en erreur, compteur « n/40 », erreur avec `ALERT`) ; Renommer reste une
    fenêtre (v2) ; `ButtonRow` et `holdSideBySide` retirés s'ils ne servent plus. Après P5.70, P5.93.
    Cas : `the title of a window is in screen title, read as a heading`, `the answers of a window stand one above the
    other, the safe one first, as TalkBack reads them`, `a deleting answer is filled in danger with its trash icon, its
    label standing out by 4,5 to 1`, `the name field counts its characters out of 40, as TalkBack reads it`, `a refused
    name frames the field in danger and says why, read by TalkBack as it shows` ; captures.
  - [x] **P5.99** Mes musiques v2, liste (`feature:musics`, V32) : barre (disque Retour, « Mes musiques ») ; carte
    « PENDANT LES PARTIES » ; une rangée par musique (Écouter, nom et ligne d'infos qui ouvrent l'éditeur,
    égaliseur et mesure jouée pendant l'écoute, jauge des huit mesures, pastille EN JEU, disque Utiliser / En jeu
    retenu avec coche, « … » en disque nu) ; état vide ; Au hasard et Nouvelle collés en bas sur un fondu.
    `UseSwitch`, `TextButtonBox`, `BottomButtons` retirés. Après P5.93, P5.94.
    Cas : `the card Pendant les parties names the music played during the games, or the music chosen on the home
    screen`, `a row is read as listen, the name that opens the editor, the switch and the menu, in this order`,
    `listening to a music shows the equalizer and the bar played, and a gauge of eight bars that TalkBack leaves out`,
    `the music played during the games bears EN JEU and its switch is selected`, `without music the page says
    « Aucune musique pour l'instant. » and keeps Au hasard and Nouvelle`, `every target of the page is 48 dp at least
    on 360 dp, in a font twice as large` ; captures.
  - [ ] **P5.100** Mes musiques v2, menu, suppression et annulation (`feature:musics`, V32) : menu « … » en
    `BloxMenu` (Modifier, Renommer, La jouer ou Ne plus la jouer pendant les parties, Supprimer en `danger`) ;
    `DeleteWindow` v2 ; Renommer en fenêtre (« « X » est supprimée. » en `SnackMessage`, `UndoMessage` retiré : faits en P5.99) ;
    v2. `CardMenu`, `MenuItem` retirés. Après P5.97, P5.98, P5.99.
    Cas : `the menu of a row offers Modifier, Renommer, the games and Supprimer in danger`, `deleting asks first,
    Garder la musique first and Supprimer in danger`, `a confirmed deletion says « X est supprimée » with Annuler,
    read by TalkBack`, `Annuler puts the music back and the cross closes the message` ; captures.
  - [x] **P5.101** Éditeur v2, dé en feuille (`feature:composer`, V33) : disque dé dans la barre qui ouvre la
    feuille « Au hasard » ; Surprise et les sept catégories en `SheetChoiceRow` avec leur sous-ligne, la retenue
    cochée ; « Tirer une musique » tire et ferme ; choisir garde la feuille ouverte. `DicePanel` retiré. Après
    P5.93, P5.96.
    Cas : `the dice of the bar opens the sheet Au hasard, read as a dialog`, `the sheet offers Surprise and the seven
    categories, each with what it sounds like, the chosen one checked`, `choosing a category keeps the sheet open and
    the choice`, `Tirer une musique rolls in the category chosen and closes the sheet, and a message tells Annuler` ;
    captures.
  - [ ] **P5.102** Éditeur v2, barre et commandes (`feature:composer`, V33) : barre (Retour, titre-bouton Renommer,
    dé, « … » en `BloxMenu`) ; pistes et Poser / Allonger en `ChoiceSegments` (coche sur le retenu) ; Annuler,
    Copier, Effacer compacts ; mesures en 2 × 4, mesure jouée lue « en lecture » ; tempo en pilule (disques nus de
    48 dp) et Écouter de 56 dp ; Enregistrer seul en `accent`, toujours en accent, point devant tant que la musique a
    changé ; messages en `SnackMessage`. Composants locaux de l'éditeur retirés. Après P5.94, P5.95, P5.97, P5.101.
    Cas : `the bar of the editor reads Retour, the name to rename, Au hasard and the menu, in this order`, `the open
    track is filled in its colour and checked`, `the bars stand on two rows of four, the bar played marked and read
    en lecture`, `Annuler, Copier and Effacer are disabled when they have no effect`, `Enregistrer is the only button
    in accent on the editor, its dot shown while the music has changed`, `on 360 by 640 dp in a font twice as large
    every control is 48 dp and the grid keeps rows of 26 dp, scrolling` ; captures.
  - [x] **P5.103** Éditeur v2, grille (`feature:composer`, `core:designsystem`, V33) : notes en bulles (dégradé du
    reflet de la piste à sa couleur, coins `radius-cell`, relief `cell-gloss` à 16 dp et plus) ; jetons `melody-hi`
    et `bass-hi` ajoutés à `tokens.json` (jour et nuit) et comparés ; grille dans un bloc `board` cadré
    `panel-border` ; ligne « TEMPS 1 2 3 4 » en `stat-label` ; fondu du bas tant que des lignes restent dessous.
    Après P5.88.
    Cas : `the highlight and the colour of each track are those of tokens json, day and night`, `the lower part of a
    note stands out from the cell by 3 to 1 at least`, `a note under 16 dp has 2 dp corners and no gloss`, `the
    painters of the notes are built once per size and variant, not at each touch` ; captures.
  - [ ] **P5.104** Enregistrer en feuille (`feature:composer`, V34) : `ComposerDialog.Save` en `BloxSheet`
    au-dessus du clavier ; « La grille est vide. » en tête ; `NameField` ; Style en `ChoiceGroup` de huit segments,
    le retenu coché ; « Jouer pendant les parties » en `SettingSwitchRow` ; Annuler (Secondaire) et Enregistrer
    (Principal, désactivé tant qu'un problème demeure). `StyleChoice`, `ForGamesBox` retirés. Après P5.95, P5.96,
    P5.98, P5.102.
    Cas : `Enregistrer opens a sheet above the keyboard, the name field focused`, `an empty grid says « La grille
    est vide. » at the top of the sheet and Enregistrer is disabled`, `a taken name frames the field in danger, says why
    and offers the free name`, `the eight styles are one group of radio buttons, the chosen one checked`, `Annuler and
    the system back close the sheet and save nothing`, `in a font twice as large on 360 by 640 dp the sheet scrolls
    and keeps Annuler and Enregistrer in sight` ; captures.
  - [ ] **P5.105** Mettre à jour la maquette (depuis claude.ai) : V32 à V34 de nuit ; Renommer et Quitter en
    fenêtres v2 ; écarts retenus au codage (hauteurs compactes, coche des outils, cibles de 48 dp du tempo) ; valeurs
    de la planche alignées sur `tokens.json` (`button-pressed`, `label-off`, `grid-line` de nuit).
- [ ] **P5.106** Outil de mesure des recompositions partagé (`core:board`) : remplacer la copie de
  `RecompositionRecorder` des doublures de `core:board` (`ComposeTestTools.kt`) par celle de `core:designsystem`
  (P5.94), règle de 2. Découverte à la revue de P5.94.
- [ ] **P5.107** Barre d'écran partagée (`core:designsystem`) : `ScreenBar(title, backDescription, onBack)` (disque
  Retour et titre `screen-title` lu comme un titre), qui remplace les barres de Paramètres, Mes musiques et du choix
  du jeu, déjà divergentes (écart `space3` contre `space2`) ; règle de 2. Découverte à la revue de P5.99. Après
  P5.76, P5.92. Cas : `the bar of a screen reads its title as a heading after its back disc of 48 dp`.
- [x] **P5.87** Exporter de la maquette (depuis claude.ai, comme P5.85) : pictogrammes cartouche, moins, étoile,
  note barrée, égaliseur, effets réduits, schéma des touches, vibreur, vibration au toucher, volume, trophée, en
  pixels 8 × 8 (Original) et en traits (Couleurs, Brume), logo BloX v2 (`LogoBloX`) et mascotte (`Mascotte`), en
  vecteurs prêts pour `core:designsystem` ; avant P5.70, P5.71, P5.77 et P5.79.
- [x] **P5.88** Grille de l'éditeur en Couleurs v2 (`feature:composer`) : jetons `cell-ground` et `grid-line` de
  `tokens.json`, listés comme non codés dans `PaletteTokensTest` (P5.57), à coder dans `BloxPalette` avec leur
  valeur Original et à retirer de la liste ; cases et lignes de la grille de `NoteGrid` dans ces couleurs.
  Avec P5.86. Cas : la case et la ligne de grille tirées de `tokens.json`, la note et sa partie tenue toujours à
  3:1 de la case.
- [x] **P5.89** Barres système suivant la variante (`app`, découvert en P5.57) : en Original avec le téléphone en
  mode sombre, la page reste claire alors que les icônes des barres système prennent le style sombre. Régler
  `enableEdgeToEdge` (`SystemBarStyle`) selon `BloxTheme.variant.isDark`, pas selon le mode du système.
  Cas : icônes sombres sur une variante claire, claires sur une variante sombre (test instrumenté).
- [ ] **P5.90** Curseurs au clavier (`core:designsystem`, découvert en P5.62) : flèches gauche et droite (et
  haut, bas) sur `VolumeSlider` et `LevelGauge` quand ils ont le focus, d'un pas, sans sortir des bornes, avec
  l'anneau de focus des boutons. Cas : `an arrow key moves the slider one step, never out of its range`,
  `a focused slider bears the focus ring`.
- [x] **P5.91** (abandonnée le 2026-10-26 : le thème Original est figé, décision du développeur) Bouton Icône désactivé en Original (`core:designsystem`, découvert en P5.62) : fond
  `panel-inset` et cadre sombre le font ressortir plus qu'un bouton actif (− au niveau 0 sur la jauge). Repère
  du désactivé qui le fait reculer, à valider avec le designer, comme le pouce creux de P5.61. Cas : `in
  Original a disabled icon button stands out from the page less than an enabled one`.
- [x] **P5.92** Choix du jeu au dessin de la maquette (`feature:cartridges`, V26, découvert en P5.72) : la
  console, la mascotte et la cartouche dessinée de la planche remplacent l'écran provisoire (titre, encart,
  étiquette) ; appui de 2 dp des boutons bulles. Cas : captures jour, nuit, Original, police ×2 ; TalkBack
  inchangé (`CartridgesScreenTest`).
- [x] **P5.108** Touches de direction plus grandes (`core:designsystem`, `feature:game`, demande du développeur,
  2026-10-28) : flèches de 64 dp sur téléphone (`key-arrow`, auparavant 60), ▼ une demi-touche plus bas que ◀ et ▶
  (auparavant un tiers) ; pleine taille dès 410 dp de large, mi-chemin (58 et 62) dès 380 dp. Cas :
  `a phone wide enough has arrows of 64 dp and B and A of 68 dp`,
  `the down arrow stands half a key below the left and right arrows`.
- [ ] **P5.109** Mettre à jour la maquette (depuis claude.ai) : touches de direction de 64 dp, ▼ une demi-touche plus
  bas (V2, V3, planche GameKey du design system). Après P5.108.
- [ ] **P5.110** Geste retour de l’éditeur en échec sur emulator-5556 (API 37), aussi sur main au 2026-10-28 :
  `ComposerScreenTest › before its music is read, the back gesture leaves the editor, as the navigation would` et
  `ComposerScreenTest › the back gesture on a changed music asks first: Continue stays, Leave without saving leaves`.
  Trouver si c'est le test (geste retour simulé) ou l'éditeur, et corriger test d'abord.

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
- [ ] **P6.17** Page Paramètres (V21, P5.50) : nombre de pièces suivantes (3 par défaut), commandes par
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

## P7 — Le Serrurier, second jeu (`core:locksmith`, `feature:locksmith`)

Règles : [docs/regles-serrurier.md](docs/regles-serrurier.md). Architecture : ADR 0025. Le domaine
(P7.3 à P7.8) ne demande que P7.1 ; chaque tâche d'interface dit ce qu'elle attend. Interface en thème
Couleurs v2 seulement, jour et nuit (décision du développeur, 2026-10-26) : pas d'aspect Original.

- [x] **P7.1** Spécification et décision : `docs/regles-serrurier.md`, ADR 0025 (amende 0002 et
  0022), glossaire et règle de dépendance dans `CLAUDE.md`, modules prévus dans
  `docs/architecture.md`, vue V35.
- [x] **P7.2** `Orientation.broughtToOrigin()`, ramenée en haut à gauche (`core:game`), partagée par les tuiles des
  pièces données et `nextPieceCells` (`core:board`), règle de 2. Avant P7.10.
- [x] **P7.3** Module `core:locksmith` (`blox.jvm.library`, `api(core:game)` ; `api(core:music)` arrive avec P7.8),
  `:core:locksmith:test` dans la CI et dans les commandes et la définition de terminé de
  `CLAUDE.md` ; `Padlock` (grille 10 × 10, trou, solution de `Piece`) et `Padlock.parse(ascii)`
  pour les tests (pièces de la solution marquées par leur rang, `1` à `6`). Cas : `the hole is the
  union of the cells of the solution`, `two pieces of a solution cannot overlap`, `a padlock holds 3
  to 6 pieces`, `the hole stays inside the outer ring`, `every piece but the first touches an earlier
  one edge to edge`, `parse reads the hole and the pieces of the solution`.
- [x] **P7.4** Candidat (`PadlockGenerator`, interne) : 200 essais par pièce, pivot et cases dans
  les 8 × 8 intérieures, collées bord à bord, abandon sinon. Cas : `a candidate has the number of
  pieces asked`, `a candidate is a valid padlock whatever the random`, `a candidate that cannot place a piece is ruled
  out`, `the same random gives the same candidate`.
- [x] **P7.5** Composer, puis évaluer : `generate(pieceCount, seed)`, 20 candidats par graine
  (abandonnés compris), chacun avec son `Random` tiré d'un maître, trou rectangulaire éliminé, note
  entière `10 × contour + 30 × variété − 3 × boîte`, premier composé à égalité, `seed + 1` si aucun
  ne reste, au plus 100 graines puis `IllegalStateException` (graines et composition injectables),
  pièces données dans l'ordre de `PieceType`, `min(6, 2 + niveau)` pièces. Cas : `a rectangular hole
  is ruled out`, `the rating is 10 per edge of the outline, 30 per kind of piece, minus 3 per cell of
  the box`, `the best candidate is kept, the first one among equals`, `a candidate does not depend on
  the draws of the previous ones`, `when every candidate is ruled out the next seed is tried`, `the
  same seed gives the same padlock` ; propriété : toute serrure de 500 graines, de 3 à 6 pièces, est
  valide.
- [x] **P7.6** `PadlockGame`, partie immuable : pièces données par rang, chacune avec son
  orientation ; choix, quart de tour, pose acceptée ou refusée (`Refusal.OUT_OF_HOLE` l'emporte sur
  `OVERLAP`), reprise, ordre des poses, serrure ouverte (`PiecePlaced` puis `PadlockOpened`, puis
  tout ignoré sauf Recommencer), recommencer, compteurs coups et refus (indices : P7.7) ; événements
  `PiecePlaced`, `PlacementRefused`, `PieceTakenBack`, `PadlockOpened` dans un `PadlockUpdate`.
- [x] **P7.7** Indice (`PadlockGame.hint`, `PadlockSolver`) : solveur de couverture exacte dans l'ordre fixé par les règles (rang,
  orientation, pivot) ; sinon la pièce posée la plus récente dont le retrait seul débloque, à
  défaut la dernière posée ; `Hint.Fit` ou `Hint.Blocking` dans `HintGiven`, compteur `hints`. Cas : `the hint shows where a free piece fits and counts a hint`, `the hint
  never names a piece already placed`, `the hint finds a way even when the player left the
  solution`, `the hint names the piece that blocks when no way is left`, `when no single piece
  unblocks the padlock the hint names the last piece placed` ; propriété : suivre les indices ouvre 200 serrures par nombre de pièces.
- [x] **P7.8** Suites d'accords (`PadlockChords`, `Progression` de `core:music` ajouté en `api` à `core:locksmith`) : une par nombre de pièces, finissant
  par V–I, Do majeur sur C4 ; `PadlockGame.progression` et `PadlockGame.chords` (accord de rang *n*
  après une pose, *n* pièces sur la grille).
- [x] **P7.9** Effets d'accord (`core:music`, `LocksmithSounds`) : arpège d'un accord, basse deux octaves plus bas,
  fanfare de serrure ouverte, son de refus ; `effectsOf(chord)` pour jouer et préchauffer.
- [x] **P7.10** Cases de la serrure (`core:board`, `LockCell`, `LockCellCanvas`, Couleurs seulement) pour une grille d'une autre taille que 10 × 20 :
  styles métal, trou, pièce, aperçu, indice et refus ; jetons `lock-metal`, `lock-rivet`,
  `lock-hint`, `lock-hole-inset` dans `tokens.json`, gardés par un test ; repères non colorés ; captures.
- [x] **P7.11** `feature:locksmith`, squelette : `LocksmithRoute(level, seed)`, `locksmithEntry`,
  `LocksmithViewModel.Factory`, `LocksmithUiState`, écran minimal toujours en Couleurs. Une seule entrée
  publique du générateur, `Padlock.generate(level, seed)`, qui délègue à la surcharge interne :
  `seeds`, `compose`, `candidate` et `rating` restent internes.
- [x] **P7.12** Écran jouable : grille 10 × 10 (anneau de métal compris, cases de 42 dp au plus),
  bande des accords, message, Indice et Recommencer, tuiles des pièces, tourner, Poser ; toucher qui
  vise, pose par un second toucher, reprend une pièce posée ; aperçu, cases fautives barrées,
  indice ; TalkBack (cases, tuiles, message) ; captures jour et nuit. Retours animés reportés (P7.20).
- [x] **P7.13** Bande des accords et son (après P7.17) : effets de chaque événement, préchauffage, réglage des
  effets sonores respecté. Après P7.8, P7.9 et P7.12. La bande des accords est celle de P7.12 ; sa paire V–I
  reste en P7.20.
- [x] **P7.14** Carte « Serrure ouverte » (compteurs, Serrure suivante, Rejouer cette serrure), Indice et Recommencer
  étant faits en P7.12. Après P7.7 et P7.13.
- [x] **P7.15** Cartouche « Serrurier » (`feature:cartridges`, `app`, `AppContainer`) ; serrure
  suivante par remplacement de la clé dans `app` (`nextPadlock`), bouton « Serrure suivante » de P7.12.
- [x] **P7.16** Mettre à jour la maquette (depuis claude.ai) : V35, Serrurier en Couleurs v2 jour
  et nuit, cartouche Serrurier sur V26 ; reporter les jetons `lock-*` (P7.10) dans le design system publié
  (claude.ai), dont `tokens.json` est la copie (ADR 0019).
- [x] **P7.17** Nom affichable d'un chiffre romain (`core:music`, `RomanNumeral.name`, l'inverse de `parse` :
  « I », « vi », « vii° », « ♭VII »), pour les pilules de la bande des accords. Avant P7.13.
- [x] **P7.18** Barres système du Serrurier : leurs icônes suivent la variante Couleurs de l'écran, non le thème choisi
  pour le Classique (Original et mode nuit : icônes sombres sur page sombre). `app` choisit la variante d'après la clé
  du haut de la pile (fonction pure), test JVM et `BloxNavDisplayTest` (aller et retour). Avec P7.15 (revue de P7.11).
- [x] **P7.19** Lecture des `CreationExtras` d'une entrée (propriétaire, extras par défaut, clé) écrite trois fois
  (`GameNavigation`, `ComposerNavigation`, `LocksmithNavigation`) : une fonction partagée, règle de 2 (revue de P7.11). Fait sans fonction partagée : l'API de Lifecycle 2.11
  suffit (amendement de l'ADR 0013).
- [x] **P7.20** Serrurier, mise en page de l'écran (revue de P7.12) : rebord de métal mince autour des 8 × 8
  intérieures pour des cases de 42 dp (cibles de 48 dp approchées), anneau non touchable ; commandes ancrées
  en bas ; bouton retour et cadenas d'état de la barre ; cadence V–I marquée comme une paire ; captures (aperçu
  refusé, pièce qui bloque, serrure ouverte, petit écran). Le reste est passé en P7.22.
- [x] **P7.22** Serrurier, finitions restantes (de P7.20) : retours animés de la pose, du refus (tremblement),
  de la serrure ouverte et de l'indice, chacun en « Effets réduits » (ADR 0024) ; TalkBack : grille en
  collection, orientation de la pièce choisie, accords entendus, refus répété annoncé, pièce désignée par
  l'indice nommée, « Serrure ouverte » annoncé une seule fois et focus sur le titre de la carte ;
  tuile choisie dont l'anneau descend avec la tuile ; UI Test instrumenté (poser, refuser, reprendre) ;
  stabilité Compose des types de `core:locksmith` (`-Pblox.composeReports=true`) ; aperçu sur l'anneau en
  colonne 0 ou 9 rogné à 4 dp du bord de l'écran. La mesure des pistes d'effets passe en P7.24.
- [x] **P7.23** Serrurier, rendu jugé par `designer-jeu` (vu sur les captures de P7.22) : la croix de la case
  rognée frôle ses bords ; le point d'encre de l'indice tourne au gris au sommet de la lueur ; la case rognée,
  immobile, chevauche un peu sa voisine pendant le tremblement. Corrigés tous trois ; le halo qui couvre le
  contour au pic du tremblement est toléré.
- [ ] **P7.24** Mesurer sur téléphone le nombre de pistes d'effets jamais libérées par le Serrurier (ADR 0025,
  P7.13), avec les mesures de P5.33.
- [x] **P7.25** Écarts relevés en mettant la maquette V35 à jour (décision du développeur, 2026-10-27) : titre de la
  barre en capitales, « SERRURE 03 », lu en casse normale, rapetissé en mots entiers en grande police ; trou de la
  serrure enfoncé : le code suivait déjà le jeton `lock-hole-inset`, un test le fige (case vide et case de l'Indice).
- [ ] **P7.26** Juger avec `designer-jeu` : le trou de la serrure paraît bombé de jour (plus clair que le métal,
  jeton `lock-hole-inset` à revoir ?) ; sur écran étroit, le titre rapetisse sur deux lignes à côté de « Coups » (le
  compteur devrait-il céder d'abord ?) ; mettre la maquette V35 à jour (titre en capitales). Revue de P7.25.
- [x] **P7.21** Parcours Macrobenchmark et profil de référence du Serrurier : ouvrir la cartouche, poser une
  pièce ; régénérer le profil (`:app:generateBaselineProfile`), le code du Serrurier n'y figure pas (revue de
  P7.15).

Progression du Serrurier (demande du développeur, 2026-10-28 : garder l'avancement, poursuivre sur de nouvelles
serrures, rouvrir les anciennes). Proposition de `designer-jeu` retenue : **la serrure n est la même pour tous et pour
toujours** (graine = n, générateur sur `StableRandom` figé par un test de contrat) ; on ne garde que la meilleure
ouverture de chaque serrure (indices, puis coups, puis refus ; « parfaite » en déduite) ; la cartouche ouvre la
première serrure non ouverte ; un écran « Serrures » (grille) depuis la barre de V35 ; « Recommencer depuis le
début » avec confirmation ; progression dans la sauvegarde Android. Décision du développeur : concevoir d'abord une
difficulté croissante au-delà de la serrure 4, avant de figer le générateur.

- [x] **P7.27** Courbe de difficulté du Serrurier au-delà de la serrure 4 (nombre de pièces, trou, paliers), avec
  `designer-jeu`, décidée par le développeur avant P7.29 : ce qui sera figé par le contrat de génération.
- [x] **P7.28** Spécification et décision : section « Progression » de `regles-serrurier.md` (le point ouvert
  « Reprendre à la dernière serrure » est fermé), amendements des ADR 0025 et 0014 (la génération d'une serrure
  devient un contrat). Après P7.27.
- [x] **P7.29** Génération stable (`core:locksmith`) : `StableRandom`, graine = niveau, courbe de P7.27,
  `Padlock.ofLevel(level)`, test de contrat (serrures 1 à 5 en ASCII, empreinte des serrures 1 à 200). Après P7.28.
- [x] **P7.30** `LocksmithRoute(level)` sans graine, `next()`, route restaurée ; `app` (`nextPadlock`,
  `OpenCartridgeTest`, `NextPadlockTest`). Après P7.29.
- [x] **P7.31** `PadlockResult` (pur, `core:locksmith`) : coups, refus, indices ; ordre du meilleur (indices, coups,
  refus) et parfaite (un coup par pièce, sans refus ni indice). Après P7.28.
- [x] **P7.32** `PadlockProgressRepository` (`core:data`) : meilleures ouvertures des serrures 1 à N, fichier binaire
  versionné avec numéro de génération, contrat, faux, `DataStoreFileContract`, fichier mis de côté, sauvegarde
  incluse (`BackupRulesTest`). Chaque ouverture : 4 entiers (pièces, coups, refus, indices, `PadlockResult`) ; un
  résultat impossible lu dans le fichier (`require` de `PadlockResult`) rend le fichier corrompu, pas un plantage
  (revue de P7.31). Après P7.31.
- [x] **P7.33** Écriture à l'ouverture (ViewModel, portée d'application, `IOException` qui laisse jouer) ; carte de
  V35 : meilleure ouverture, NOUVEAU MEILLEUR ou PARFAITE, « Reprendre à la serrure N » après un rejeu ; Effets
  réduits ; captures. L'adaptateur `PadlockResult` → `PadlockOpening` vient avec un test qui garde les deux en accord
  (rejets et ordre du meilleur), écrit en premier ; un avancement d'une version ou génération plus récente (écriture
  impossible) laisse jouer avec un état compréhensible, sans relancer la lecture en boucle (revue de P7.32). Après
  P7.30, P7.32.
- [x] **P7.34** Cartouche : ouvre la première serrure non ouverte, sous-ligne « Serrure N » (`feature:cartridges`,
  `app`), parcours Macrobenchmark vérifié. Après P7.32.
- [x] **P7.35** Écran « Serrures » (`PadlocksRoute`, `feature:locksmith`) : Reprendre, grille (ouverte, parfaite, à
  ouvrir, en cours, verrouillées de sa rangée), état vide, TalkBack, police ×2, captures jour, nuit, grande police.
  Après P7.32.
- [x] **P7.36** Le cadenas de la barre de V35 devient le disque « Serrures » (48 dp) ; navigation dans `app`
  (choisir remplace la serrure sous la liste, le retour garde la serrure en cours). Après P7.35.
- [ ] **P7.37** Recommencer depuis le début : confirmation dans la page, résultats effacés, pile remise à la
  serrure 1. Après P7.35.
- [ ] **P7.38** Mettre à jour la maquette (depuis claude.ai) : V35 (disque Serrures, carte), V36 (sous-ligne de la
  cartouche), nouvelle vue « Serrures » (jour, nuit, vide, grande police, confirmation), pictogrammes. Numéro de vue
  à attribuer avec ceux de P10.33 et P12.10.
- [ ] **P7.39** Juger avec `designer-jeu` les captures de P7.33 à P7.37 (contraste de la case verrouillée, carte à
  trois boutons ; à 390 dp, elle pousse le message de la cadence hors de l'écran, capture
  `LocksmithOpenReplaySmallDay`, trouvé en P7.33). Écran « Serrures » (P7.35, captures `Padlocks*`) : l'en-tête
  « Maître · 7 ou 8 pièces » alors que les serrures 21 à 30 en ont toujours 8 ; le contraste de la case verrouillée ;
  les pictogrammes restés à 16 dp en grande police ; l'état vide sans Reprendre ; la serrure en cours au-delà de la
  frontière, qui ne s'affiche pas.
- [ ] **P7.40** Revoir le placement des pièces au toucher, « pas très fluide ni pratique » (retour du développeur,
  2026-10-28, à reprendre plus tard) : Serrurier et Jardin musical, qui le partagent (aperçu, visée, pose au second
  toucher). Observer d'abord sur téléphone ce qui gêne, puis faire proposer par `designer-jeu` (glisser la pièce,
  pose au relâcher, décalage sous le doigt…) avant de coder.
- [x] **P7.41** Bac du Serrurier à la largeur (demande du développeur, 2026-10-28, trouvé par `designer-jeu`) : à 360 dp,
  la 6e tuile n'avait que 27 dp ; les tuiles rétrécissent jusqu'à 48 dp, puis passent sur deux rangées de 4 au plus
  (`trayLayout`, en pixels entiers). Une tuile de 48 dp dessinait sa pièce en noir (canevas sans largeur dans
  `PieceTile`) : corrigé. Prêt pour les serrures de 7 et 8 pièces (P7.29).
- [ ] **P7.42** « Recommencer » coupé au milieu du mot à 300 et 320 dp (captures `LocksmithSixTilesNarrowDay`,
  `LocksmithFiveTilesNarrowDay`, défaut antérieur à P7.41) : garder le mot entier (réduire, ou passer sous Indice).
  Juger aussi avec P7.39 la pièce I choisie dans une tuile de 48 dp, à 1 dp de l'anneau (`PieceTilesShrunk`).
- [ ] **P7.43** Régénérer le profil de référence du Serrurier (`:app:generateBaselineProfile`, sur un appareil
  Android 13 ou plus) : il cite encore `Padlock.generate`, supprimé en P7.30.
- [ ] **P7.44** Mesurer le pire temps de `Padlock.ofLevel` sur les serrures 1 à 200 (`PadlockMeasure`) : il tourne
  sur le fil principal au toucher de « Serrure suivante » (P7.30) ; s'il dépasse une image (16 ms), le sortir de ce fil.
- [ ] **P7.45** Extraire l'autocollant commun (`Sticker`) dans `core:designsystem` : le jeu (`GameTopBar`), les
  cartouches, le Serrurier (P7.33) et Mes musiques en ont chacun leur copie ; la règle de 2 est dépassée.
- [ ] **P7.46** Vérifier sur un téléphone en français que « 0 indice » s'affiche au singulier sur la carte du
  Serrurier (« Meilleure ouverture ») : les captures montrent « 0 indices » (P7.33).
- [ ] **P7.47** Mettre en commun le motif de valeur chaude de `app` (`stateIn(Eagerly, null)`, `current`, `await`)
  de `PadlockFrontier` et `NewCartridges` (règle de 2, revue de P7.34).
- [ ] **P7.48** Lecture en grille des cases de l'écran Serrures par TalkBack (`CollectionItemInfo` : rang et
  colonne), alors que le résumé, Reprendre et les en-têtes de palier occupent des lignes entières (revue de P7.35).
- [ ] **P7.50** Bande des accords du Serrurier trop large à 360 dp en police ×2 avec 8 pièces : le dernier accord
  sort de l'écran et « cadence » se coupe au milieu du mot (capture `LocksmithBarLargeFontDay`, défaut antérieur à
  P7.36).
- [x] **P7.49** Barres système sous le Serrurier en Original de nuit : `SystemBarsAppearanceTest` échouait sur main, car il partait du choix du jeu, dessiné en Couleurs nuit depuis la grille de la Graine du jour (icônes claires, voulu, `SystemBarsTest`). Le test part désormais de l'accueil du Classique, dans le thème choisi ; le produit était juste.

## P8 — Musique vivante (`core:music`, `core:audio`, `feature:game`)

Règles : [docs/musique-vivante.md](docs/musique-vivante.md). Architecture : ADR 0026. Un choix de musique du
Classique, « Vivante », sans cartouche ni écran à lui ; les règles du jeu ne changent pas. Ordre : P8 d'abord,
la Graine du jour l'impose.

- [x] **P8.1** Spécification et décision : `docs/musique-vivante.md`, ADR 0026 (amende 0014 et 0022), vue V37
  numérotée (à dessiner). Fait le 2026-10-26, avec les spécifications de P9 à P11 et les ADR 0027 et 0028.
- [x] **P8.2** `Player.switchTo(score)` (`core:audio`) : la nouvelle partition commence à la mesure suivante, au
  même rang, au même facteur de tempo, sous le même jeton ; ajouté à `MusicPlayerContract` et au faux lecteur,
  et à chaque étage du lecteur (`ChiptuneSequencer`, `FocusedMusicPlayer`, `MusicSwitch`) ; réservé aux
  partitions, sans effet pour un `MusicFile` ; la partition demandée est préparée au tempo du moment (pour la
  tendue, celui du danger), sans rendu à l'entrée en danger. Cas : `a score switched to starts on the next bar, at the same rank`, `a switch keeps the tempo factor and the
  playback`, `a second switch before the bar line replaces the first`, `a switch while the music is stopped is
  played at the resume`.
- [x] **P8.3** `LiveMusic.compose(seed)` (`core:music`) : une musique calme (`Category.CALM`) et une tendue
  (`Category.EPIC`), de même longueur et de même tonique, par `MusicGenerator.generate`. Après P8.9. Cas : `a live
  music is a calm music and a tense one`, `the calm and the tense music have the same number of bars`, `the calm
  and the tense music share their tonic`, `the same seed gives the same two musics`, `the calm music does not
  depend on the tense one`.
- [x] **P8.4** `MusicChoice.LIVE` (`core:data`) et `musicOf` (`app`) : le choix enregistré, qui demande de composer
  les deux ambiances au lieu d'une partition fixe (`ChosenMusic.Live`, devenu `GameMusic.Live` en P8.6) ; règles de sauvegarde. Cas : `the live
  choice is saved and read back` (couvert par le test paramétré du contrat), `the live choice asks for a live music
  rather than a score`. Après P8.3. Choix caché de la feuille Musique jusqu'à P8.7 ; adaptateur provisoire de
  `AppContainer` (Vivante sans musique) à retirer en P8.6, avec le déplacement de `ChosenMusic` dans
  `feature:game` (revue de P8.4 : ce n'est pas un type de persistance).
- [x] **P8.5** Extraire `MusicDirection` (`feature:game`), pur refactoring : `ClassicMusicDirection` reprend
  `MusicTempo.kt` tel quel, sous les tests existants du tempo, qui restent verts.
- [x] **P8.6** `LiveMusicDirection` : calme au départ, tendue sur `StackDanger(on)`, calme sur `StackDanger(off)`,
  par `switchTo` ; facteur de tempo = min(1 + 0,02 × niveaux gagnés, 1,30) **[à vérifier]**, multiplié au × 1,25
  du danger ; graine de la musique = graine de la partie ; chaque `LevelUp` déclenche un rendu, dont le coût est
  à mesurer sur téléphone. Après P8.2, P8.3, P8.5. Cas : `a live game starts on the
  calm music`, `entering the danger switches to the tense music`, `leaving the danger switches back to the calm
  music`, `each level up speeds the music up by 2 percent, up to 1 point 3`, `the speed of the levels and the
  speed of the danger multiply`, `the music resumes after a pause in the mood of the moment`, `a game already in
  danger when its music starts plays the tense music, a quarter faster` (`onStart` après des `onEvents`, revue de P8.5).
  Fait avec le branchement de P8.4 : `GameMusic` (`feature:game`) remplace `ChosenMusic`, l'adaptateur provisoire
  est retiré ; le lecteur libère les rendus des tempos quittés (revue de P8.6).
- [ ] **P8.7** Choix « Vivante » dans la feuille Musique (`feature:home`) : rangée, Écouter (musique calme d'une
  graine au hasard **[à vérifier]**), rappel « Classique · Niveau 05 · Vivante », carte Pause « Vivante · calme »
  ou « Vivante · tendue » ; TalkBack ; captures. Après P5.76, P8.4, P8.6.
- [x] **P8.8** Mettre à jour la maquette (depuis claude.ai) : V37, choix Vivante, rappel et carte Pause.
- [x] **P8.9** Capacité de `MusicGenerator` (`core:music`) : **tonique imposée**, pour que la musique tendue de la
  Vivante soit dans la tonalité de la calme ([musique-vivante.md](docs/musique-vivante.md)). Avant P8.3. Cas : `a
  music composed with an imposed tonic is in that tonic`, `without an imposed tonic the tonic is drawn as before`.
- [ ] **P8.10** Préparer une partition d'avance (`prepareScore` ou switch préparé), pour que la première entrée en
  danger ne rende rien ; à décider avec la mesure de P8.6 sur téléphone (revue de P8.2). À mesurer aussi : le rendu
  de chaque `LevelUp` de la Vivante, et le changement d'ambiance après un niveau gagné, qui rend l'autre partition
  au nouveau facteur (les rendus des tempos quittés sont libérés depuis P8.6).

## P9 — Graine du jour (`core:game`, `core:data`, `feature:game`, `app`)

Règles : [docs/graine-du-jour.md](docs/graine-du-jour.md). Architecture : ADR 0026 (amende 0014 et 0022).
**Préalable : P1.20** (`StableRandom`, suite des pièces figée pour la graine 20261026), et la Musique vivante
(P8), imposée. Écrans en Couleurs v2 seulement, jour et nuit.

- [x] **P9.1** Spécification et décision : `docs/graine-du-jour.md`, amendement de l'ADR 0014 (la suite des pièces
  d'une graine devient un contrat) dans l'ADR 0026, vues V38 et V39 numérotées (à dessiner). Fait le 2026-10-26.
- [x] **P9.2** `DailySeed.of(LocalDate)` (`app`), horloge injectée : la graine `AAAAMMJJ` de la date locale du
  téléphone. Cas : `the seed of a day is its date written year month day`, `the date is the local date of the
  phone`, `two days give two seeds`.
- [x] **P9.3** `DailyChallengeRepository` (`core:data`, DataStore, fichier à part), avec son contrat et son faux
  en testFixtures : date en cours, essais, premier essai, meilleur ; un essai est compté **à son début**
  (premier essai à 0 tant qu'il n'est pas fini) et le fichier est exclu de la sauvegarde (décisions du
  développeur, 2026-10-27) ; `startTry` / `endTry`, qui rend un `RecordResult`. Cas :
  `without a try there is no result of the day`, `a try is counted as soon as it starts`, `the first try is worth 0
  until it ends`, `the first try is the first and the best`, `a better try replaces the best, never the first`,
  `every try is counted`, `a try of a newer date starts that date over`, `a try of an
  older date is ignored`.
- [x] **P9.10** Contrat commun des fichiers Preferences de `core:data` : les tests « fichier illisible », « fichier
  qui ne peut être lu » et « échec autre qu'une `IOException` » sont recopiés dans chaque dépôt DataStore (quatre
  fois depuis P9.3, revue de P9.3) ; les mettre en commun (règle de 2). Y ajouter le cas d'une date hors des bornes
  de `LocalDate` dans le fichier du jour, qui lèverait une `DateTimeException` à la lecture.
- [x] **P9.4** Variante `DAILY` de `GameRoute` (`feature:game`, `app`) : graine du jour par
  `ClassicRandomizer.stable(seed)`, niveau 5 et musique Vivante imposés (y compris sur la musique en jeu de « Mes
  musiques »), résultats dans le dépôt du jour et jamais dans les records ; Rejouer garde la graine ; l'essai est
  compté à son début (décision du développeur, 2026-10-27), une partie restaurée après la mort du processus reste le même essai ; thème
  Couleurs imposé et barres système (`systemBarVariant` connaît la variante, ADR 0026). Après P1.20, P8.6, P9.2,
  P9.3. Cas : `a daily game starts at
  level 5 whatever the level chosen`, `a daily game plays the live music whatever the music chosen`, `a daily game
  draws the pieces of its seed`, `a daily game never enters the Classic records`, `replaying a daily game keeps
  its seed, even after midnight`, `a daily game left before its end counts as a try`, `a daily game restored after
  the process died is the same try`, `the bars of a daily game follow its colorful variant`.
- [x] **P9.5** Écran d'entrée (V38) : barre (Changer de jeu, « MEILLEUR DU JOUR », Paramètres), titre « Graine du
  26 octobre », les 7 premières pièces, tuiles Essais, Premier essai, Meilleur, Jouer XL et son rappel court
  « Niveau 05 · Vivante », « Nouvelle graine à minuit » ; TalkBack ; captures jour et nuit. Dans une feature à
  part, `feature:daily`, qui amende l'ADR 0002 (décision du développeur, 2026-10-27). La date est relue à chaque
  affichage, sans bascule en direct à minuit (décision du développeur, 2026-10-27) ; Jouer joue la date affichée.
  Après P9.4.
- [x] **P9.11** La partie du jour à l'écran (V39, `feature:game`, découpée de P9.5) : titre « GRAINE DU JOUR »,
  meilleur du jour à la place du record, « NOUVEAU MEILLEUR » en jeu une fois battu ; fin de partie « MEILLEUR DU
  JOUR », Rejouer, Copier (lu « Copier mon score ») ; les libellés par variante viennent de `GameLabels`, donnés
  par `GameVariants.setupOf` (ADR 0026). Après P9.4.
- [x] **P9.6** « Copier mon score » : texte pur (date en lettres et `AAAAMMJJ`, premier essai, meilleur, essais),
  presse-papiers, bouton désactivé avant le premier essai ; nombres à leur valeur réelle, groupés par trois avec
  U+202F, sans plafond ni zéros de tête. Brancher les deux boutons, laissés sans effet par P9.5 et P9.11 : l'écran
  d'entrée et la fin de partie (`gameEntry(onCopy)`, dans `app`). Cas d'une fin d'essai dont l'écriture a échoué
  (`IOException`) : Copier en fin de partie ne doit pas copier un essai sans score (revue de P9.11). Cas : `the copied text gives the date, the first try,
  the best and the tries`, `one try is written in the singular`, `numbers are grouped by three`, `copying is not
  offered before the first try`. Après P9.3, P9.5.
- [x] **P9.7** Cartouche « Graine du jour » (`feature:cartridges`, `app`) : choix du jeu à cinq cartouches sur deux
  colonnes (amendement de l'ADR 0022), autocollant « NOUVELLE » jusqu'au premier essai du jour, passé par `app` ;
  parcours Macrobenchmark et profil de référence. Après P9.3, P9.5, P9.6 (aucun bouton Copier sans effet ne doit
  être accessible au joueur).
- [x] **P9.12** Renommer ce qui, dans `feature:game`, sert désormais au record et au meilleur du jour :
  `GameOverUiState.newRecord`, `NewRecordBadge`, `GameUiState.recordBeaten` (vers `newBest`, `bestBeaten`), en
  accord avec `GameLabels.newBest` (revue de P9.11).
- [ ] **P9.13** Poursuivre le vocabulaire « meilleur » dans `feature:game` : `GameUiState.record` (le meilleur à battre,
  générique), `isRecord()`, `ShownRecord`, `Record`, `RecordBar`, `recordBarFill` ; touche des noms de tests cités par la
  spécification (traçabilité à reprendre). Revue de P9.12.
- [x] **P9.14** Ligne « Ancien record » / « Ancien meilleur » en fin de partie, écart relevé par la maquette V30 et V39
  (décision du développeur, 2026-10-27) : sous le score, le meilleur d'avant la partie (`GameEnd.previousBest`), « Ancien
  … » s'il est battu, « Record » / « Meilleur » sinon ; rien s'il valait 0, si l'écriture a échoué ou si l'essai du jour
  n'est pas gardé ; libellés par variante (`GameLabels`, ADR 0026).
- [x] **P9.8** Mettre à jour la maquette (depuis claude.ai) : V36 (cinq cartouches), V38 et V39.
- [x] **P9.9** Point ouvert : prévenir le joueur quand une partie rejouée avec la graine d'hier n'est pas comptée
  (dépôt déjà passé à aujourd'hui) : « Partie d’entraînement » en fin de partie (décision du développeur, 2026-10-27).
  Après P9.4.
- [ ] **P9.15** Vérifier sur téléphone « Copier mon score » : presse-papiers et message « Score copié » sur un appareil
  Android 12 ou moins, et annonce de la copie par TalkBack à partir d'Android 13 (surcouche du fabricant) ; sinon
  `announceForAccessibility` ou région vivante (revue de P9.6). Après P9.6.
- [ ] **P9.16** Régénérer le profil de référence sur un téléphone Android 13+ (`./gradlew :app:generateBaselineProfile`), parcours du Jardin compris (P10.13), de préférence après l'écran jouable (P10.10),
  avec le parcours de la Graine du jour (cartouche, entrée, premières secondes de la partie) ajouté en P9.7.
- [ ] **P9.17** Juger avec `designer-jeu` les écarts de V36 codée (P9.7) : contraste de la pousse menthe sur le coin
  menthe de la fenêtre `iris` (jour), animation d'entrée de l'autocollant « NOUVELLE » et son appui, engrenage
  Paramètres dans la barre ; reporter dans le design system publié les jetons `lock-*` et la mascotte de la Graine
  (`pictogrammes.md`), restés hors de P7.16.

## P10 — Le Jardin musical, troisième jeu (`core:garden`, `feature:garden`)

Règles : [docs/jardin-musical.md](docs/jardin-musical.md). Architecture : ADR 0027 (amende 0002 et 0022), sur le
modèle du Serrurier (ADR 0025). Peut avancer en parallèle de P9. Écran en Couleurs v2 seulement, jour et nuit.

- [x] **P10.1** Spécification et décision : `docs/jardin-musical.md`, ADR 0027, modules prévus dans
  `docs/architecture.md`, vue V40 numérotée (à dessiner). Fait le 2026-10-26.
- [x] **P10.2** `PieceTile` dans `core:board` : les tuiles du bac du Serrurier (`Tray`, `TilePiece`) partagées,
  règle de 2 ; captures du Serrurier inchangées. Avant P10.10.
- [x] **P10.3** `LoopPlayHead` dans `core:designsystem/frame` : la tête de lecture de l'éditeur (`PlayHead`)
  partagée, règle de 2 ; tests de l'éditeur verts. `LoopPlayHead` prend la longueur de boucle et le tempo en
  paramètres, comptée en croches (64 pour l'éditeur, ses 8 mesures ; 16 pour le jardin, ses 2 mesures), sans `Tempo` ni `Cell`, internes à `feature:composer`.
  Avant P10.12.
- [x] **P10.4** Module `core:garden` (`blox.jvm.library`, `api(core:game)`, `api(core:music)`),
  `:core:garden:test` dans la CI, commandes, glossaire (`Garden`, `GardenMood`) et règle de dépendance de
  `CLAUDE.md` ; `Garden` immuable (16 × 14, poser, refuser, reprendre, effacer) et `Garden.parse(ascii)`. Cas : `a
  piece inside the grid on free cells is placed`, `a piece off the grid is refused`, `a piece over a placed one is
  refused as an overlap`, `a piece both off the grid and over a placed one is refused as off the grid`, `a placed
  piece is taken back`, `clearing the garden leaves it empty`, `parse reads the pieces of a garden`.
- [x] **P10.16** Mettre en commun ce que `core:garden` a repris de `core:locksmith` (revue de P10.4, règle de 2) : voisins
  par les côtés, ordre de lecture, `Position.minus`, la pièce Classique qui couvre des cases (`pieceCovering`) et la
  lecture d'une grille ASCII, dans `core:game` sous ses tests, puis réutilisés par `Padlock` et `Garden`. Change l'API
  publique de `core:game` (`explicitApi`).
- [ ] **P10.17** Régler à l'oreille, sur un téléphone, les valeurs de départ du Jardin (décisions du développeur
  des 2026-10-27 et 28, en constantes nommées) : tempos des ambiances, tonique de Calme, octave de la basse, volumes
  (0,06 et 0,16) et poids de la note du dé (`GardenRating`). Après P10.12.
- [x] **P10.18** Brancher le jardin gardé dans `feature:garden` : la fabrique reçoit `GardenRepository` (`keptGarden`
  d'`AppContainer`), l'écran s'ouvre sur le jardin gardé et chaque changement est gardé ; une écriture ratée
  (`IOException`, par exemple un fichier d'une version plus récente) laisse le jardin à l'écran sans planter, toute
  autre exception passe (revue de P10.11). Cas : `a garden that cannot be kept leaves the garden on screen`. Après
  P10.9, P10.11.
- [x] **P10.19** Mettre en commun les noms de notes et d'accords lus par TalkBack (éditeur de musique, Serrurier,
  Jardin ; revue de P10.10, règle de 2) : `chordTellingOf` et ses chaînes vers `core:designsystem`, comme `ChordPill`.
  Les lectures divergent déjà : le Serrurier lit un ♭VII « 7 majeur », le Jardin « 7 bémol majeur ». Cas : `a flat
  seventh is told 7 flat major on the Locksmith too`.
- [ ] **P10.20** Juger avec `designer-jeu` la lecture des cases du Jardin sans octave (« mi » revient 2 ou 3 fois dans
  une colonne : « mi grave / aigu » ?), les 4 ambiances sur 2 lignes à 360 dp, et Poser sur un aperçu refusé qui
  n'annonce rien de nouveau (revue de P10.10).
- [ ] **P10.21** Mesurer sur un téléphone le nombre de pistes audio ouvertes avec le Jardin : ses 25 notes
  d'aperçu s'ajoutent aux effets du jeu, jamais libérés (ADR 0025) ; si la limite approche, réduire l'ensemble ou
  libérer à la sortie du jardin. Après P10.12.
- [ ] **P10.22** Recaler la tête de lecture (Jardin, éditeur de musique) sur le son réellement entendu : elle suit
  le temps des images, pas la position du lecteur, d'où un décalage de la latence audio (revue de P10.12). Après P10.12.
- [x] **P10.23** Mettre en commun l'arrêt de la musique quand l'écran n'est plus visible (`ON_STOP`), répété dans
  les 4 écrans qui écoutent (accueil, Mes musiques, éditeur, Jardin), par un `LifecycleEventEffect` partagé (revue de
  P10.12, règle de 2). Après P10.12.

Mes jardins (demande du développeur, 2026-10-28 : « enregistrer les musiques » du Jardin). Proposition de
`designer-jeu` retenue : des jardins nommés, rouverts intacts (pièces et ambiance) ; jardin lié (« Enregistrer « X » »
remplace X, « Garder une copie ») ; 30 au plus ; pas d'export vers Mes musiques, que l'éditeur ne saurait ouvrir
(2 mesures, jusqu'à 15 voix, triolets, tempos hors pas de 10).

- [x] **P10.24** Spécification « Mes jardins » dans `jardin-musical.md` et amendement de l'ADR 0027 (plusieurs
  jardins, lien du jardin en cours, limite de 30, rôle de `garden.bin` et `gardens.bin`) ; trancher la place du disque
  dans la barre V40 à 360 dp (4ᵉ disque, ou le dé dans la rangée d'information) et les noms proposés.
- [x] **P10.25** Règles de nom communes (`MusicNames` rendu générique dans `core:data` : `nameProblem`, `freeName` sur
  une liste de noms), `MusicNamesTest` vert. Avant P10.27.
- [x] **P10.26** Extraire l'encodage d'un jardin de `GardenFile`, sans changer le format v1 de `garden.bin`. Avant
  P10.27.
- [x] **P10.27** `GardenLibrary` (`core:data`) : `KeptGarden` (identité, nom, jardin, date), contrat partagé par le
  faux et le vrai dépôt, `gardens.bin` v1 versionné, limite de 30, restauration après suppression, fichier corrompu
  et fichier plus récent ; règles de sauvegarde Android et `BackupRulesTest`. Après P10.24 à P10.26.
- [x] **P10.28** `garden.bin` en v2 : le lien du jardin en cours vers le jardin gardé d'où il vient (un fichier v1 se
  lit sans lien). Après P10.27.
- [x] **P10.29** Disque « Mes jardins » dans la barre (point « modifié » lu par TalkBack), feuille Mes jardins :
  Garder ce jardin, Enregistrer « X », Garder une copie, nommage (`NamingWindow`), limite, états vide, chargement et
  erreur ; `AppContainer` et fabrique dans `app`. Barre vérifiée à 360 dp, police ×1 et ×2. Après P10.27, P10.28.
- [x] **P10.30** Rouvrir un jardin gardé (`adopt`, Annuler, `switchTo` pendant l'écoute), le renommer, le supprimer
  avec Annuler ; actions TalkBack de la rangée. Après P10.29.
- [x] **P10.31** Miniature `GardenThumbnail` (`core:board`), jeton `garden-thumb-cell`, captures jour et nuit. Avant
  P10.29.
- [x] **P10.32** Retours animés de Mes jardins, chacun en « Effets réduits » (ADR 0024), avec P10.15.
- [ ] **P10.33** Mettre à jour la maquette (depuis claude.ai) : V40 avec le disque Mes jardins à 390 et 360 dp, la
  feuille Mes jardins (vide, liste, jardin ouvert et modifié, limite), les fenêtres Garder, Renommer, Supprimer ;
  pictogramme « herbier » et jeton `garden-thumb-cell` dans le design system.
- [x] **P10.5** `GardenMood` : Joyeux, Calme, Mystère, Épique ; gamme, 4 accords, tempo (132, 84, 100, 120 ; tonique Do 4, Do 3 pour Calme : décision du développeur, 2026-10-27), 14
  lignes depuis la tonique. Cas : `each mood has 14 rows of its scale, the tonic on the bottom row`, `each mood
  plays 4 chords, one per group of 4 eighths`, `a cell is a note of the chord when its note belongs to the chord
  of its group`, `changing the mood keeps the pieces where they are`. L'ambiance devient une propriété du jardin (`Garden.mood`, calme par défaut).
- [x] **P10.6** `Garden.toScore()` (l'ambiance est celle du jardin, P10.5) : une case = une croche ; colonne par colonne, les cases d'une même pièce se
  partagent la croche en arpège montant ; une voix par ligne à volume fixe (0,06) et la basse (0,16, octave Do 2), pleine échelle jamais dépassée (décision du développeur, 2026-10-28), sans
  changer `core:music` ; durées exactes (la dernière part d'une croche partagée en prend le reste). Cas : `a cell
  plays its note for its eighth`, `a piece with one cell per column plays its notes column after column`, `a
  standing piece plays its notes as a rising arpeggio within the eighth`, `a column of three cells keeps the length
  of the bar exact`, `two pieces in the same column sound together`, `an empty garden plays the bass alone`,
  `fourteen notes at once and the bass reach full scale without going above it`.
- [x] **P10.7** `GardenComposer.compose(mood, seed)`, le dé : composer, puis évaluer, sur `StableRandom` ; 6 pièces,
  20 candidats (décision du développeur, 2026-10-27), note de départ à régler à l'oreille (P10.17). Après P1.20, P10.5. Cas : `the same mood and seed give the same garden`, `a
  composed garden holds 6 pieces placed without overlap` (devenu `every seed composes a garden of 6 pieces`), `the best candidate is kept, the first one among equals`,
  `a candidate does not depend on the draws of the previous ones`.
- [x] **P10.8** Cases et jetons du jardin (`core:board`, Couleurs seulement), sur le modèle de `LockCellCanvas` :
  case libre, groupe en bande (`garden-band`), note en bulle, point de note de l'accord, aperçu, refus barré ; repères non
  colorés ; jetons gardés par un test ; captures jour et nuit. Reporter dans `docs/design-system/tokens.json` les jetons
  `garden-*` publiés en P10.14 (design system, version 24), que les tests de jetons exigent codés ; `garden-band-cell`
  pour une case libre d'un groupe en bande (sinon invisible de jour) ; point de note de l'accord sur les notes posées
  seulement ; contour d'aperçu et croix d'au moins 2 dp à 22 dp (designer-jeu, P10.14).
- [x] **P10.9** `feature:garden`, squelette : `GardenRoute`, `gardenEntry`, `GardenViewModel.Factory`,
  `GardenUiState`, écran minimal toujours en Couleurs ; barres système selon la variante (`systemBarVariant`).
  Après P10.4.
- [x] **P10.10** Écran jouable (V40) : barre, ambiances, rangée d'information, bande des accords, grille 16 × 14 de
  22 dp, message, bac (7 pièces et Retirer), tourner, Poser ; aperçu pivot sous le doigt, reprise, Retirer, Tout
  effacer et Au hasard avec « Jardin effacé · Annuler » 4 s ; TalkBack (« Croche 5, mi : libre ») ; captures jour et
  nuit. Après P10.2, P10.5, P10.7, P10.8, P10.9. Maquette V40 (P10.14) : bac de 4 × 84 dp à 6 dp d'écart, « Retirer »
  hors du groupe de choix pour TalkBack, rangée d'information sur 2 lignes (« Do mineur harmonique · 100 bpm » ne
  tient pas à côté de « Tout effacer »).
- [x] **P10.11** Jardin gardé (`core:data`, DataStore, fichier à part, format versionné), écrit à chaque changement,
  lu à l'ouverture ; pas d'export. Cas : `without a garden kept the garden is empty and calm`, `the garden kept is
  read back, its mood too`, `an unreadable garden gives an empty one`.
- [x] **P10.12** Son : Écouter / Arrêter en boucle par le lecteur de musique, changements par `switchTo`, tête de
  lecture (`LoopPlayHead`) et pilule d'accord ; aperçu sonore par un ensemble fini d'effets jamais libérés
  (l'union des hauteurs des 4 ambiances, 25), préchauffé à l'ouverture ; mesure des pistes reportée en P10.21 (ADR 0025, ADR 0027). Après P8.2, P10.3, P10.6, P10.10.
- [x] **P10.13** Cartouche « Jardin musical » (`feature:cartridges`, `app`) : ouvre le jardin gardé ; parcours
  Macrobenchmark. Après P10.9, P10.11 ; la mise en page à cinq cartouches vient de P9.7 (ou est faite ici si P10 passe
  le premier). Ordre de V36 à quatre cartouches (P10.14) : Classique, Serrurier, Jardin musical, Graine du jour.
- [x] **P10.14** Mettre à jour la maquette (depuis claude.ai) : V40, cartouche Jardin sur V36 ; reporter les jetons
  du jardin dans le design system publié (ADR 0019).
- [x] **P10.15** Retours animés du jardin, chacun en « Effets réduits » (ADR 0024) : pose (8 ticks), refus, tête de
  lecture (rebond × 1,1 pendant 6 ticks), Au hasard (une pièce toutes les 2 ticks), changement d'ambiance (fondu de
  8 ticks). Après P10.10, P10.12.
- [x] **P10.34** Mettre en commun les textes des problèmes de nom (`NameProblem` → texte) de Mes musiques, de
  l'éditeur et du Jardin, troisième occurrence (revue de P10.29). `core:designsystem` ne voit pas `core:data`, où vit
  `NameProblem` : il faut un endroit commun aux trois features. Fait : `NameTexts` (`core:designsystem`), « Donne-lui
  un nom. » partout.
- [ ] **P10.35** En police ×2, la pastille « ouvert » d'une rangée de Mes jardins pourrait passer sous la ligne
  d'information (à juger, P10.33). De même, à 360 dp, le « … » de 48 dp (P10.30) serre le texte de la rangée
  (« Calme · 6 pièces » sur 3 lignes à côté d'« ouvert ») : à juger ensemble.
- [ ] **P10.36** Mettre en commun le menu « … » de rangée et les fenêtres Renommer et Supprimer de Mes musiques et de
  Mes jardins, deuxième occurrence (règle de 2, revue de P10.30).
- [ ] **P10.37** Jeton de mouvement pour la montée d'une feuille qui descend (`SHEET_RISE`, 40 dp, sans jeton
  d'espacement qui lui corresponde) et, si on le veut, `motion-row` pour le repli d'une rangée (aujourd'hui
  `motion-curtain`) : à publier dans le design system avec P10.33.

## P11 — Au tempo (`core:game`, `core:audio`, `feature:game`)

Règles : [docs/regles-au-tempo.md](docs/regles-au-tempo.md). Architecture : ADR 0026 et ADR 0028 (amende 0012 et
0015). En dernier : la phase commence par une mesure sur téléphone. Le Classique reste vert à chaque tâche ; P6
reste gelée (`ScoringRule` intouchée, chevauchement avec P6.11 noté dans l'ADR 0028). Écran en Couleurs v2
seulement, jour et nuit.

- [x] **P11.1** Spécification et décision : `docs/regles-au-tempo.md`, ADR 0028, vue V41 numérotée (à dessiner).
  Fait le 2026-10-26.
- [ ] **P11.2** Mesure sur téléphone (sans code de production) : précision d'`AudioTrack.getTimestamp`, latence
  (haut-parleur, casque filaire, Bluetooth), dérive entre horloge audio et horloge d'images ; fixe la fenêtre
  « sur le temps » (N = 5 ou 6 ticks **[à vérifier]**) ; résultats dans la spécification et `JOURNAL.md`.
- [ ] **P11.3** `Playback.heardNanos()` (`core:audio`) et ticks d'Au tempo nourris par le temps entendu, sans
  plafond, repli sur l'horloge d'images (ADR 0028, amendement de l'ADR 0012) : le ViewModel lit `heardNanos()` à
  chaque image ; temps monotone depuis `play`, tick 0 au premier échantillon entendu, reprise à la position
  `tickCount mod phrase`. Après P11.2. Cas : `the heard time is the time of the music played, latency included`,
  `a stopped playback has no heard time`, `the heard time never goes back, even when the music loops`, `the ticks
  follow the heard time without a cap`, `the ticks stay in phase with the accompaniment after a pause`, `when the
  music fails the ticks fall back on the frame clock without a jump`.
- [ ] **P11.4** Extraire `DropTiming` de `Rules`, pur refactoring : `ClassicDropTiming` délègue à `GravityTable`,
  tests du Classique verts ; parties de référence enregistrées avant l'extraction, pour la propriété de P11.8 ;
  glossaire de `CLAUDE.md` (`DropTiming`). Cas
  : `the classic drop timing follows the gravity table of the level`.
- [ ] **P11.5** `Meter` (`core:game`, paquet `tempo`) : croche en ticks entiers, noire, mesure, phrase, temps et
  mesure d'un `tickCount`, fenêtre « sur le temps » ; glossaire de `CLAUDE.md` (`Meter`). Cas : `an eighth lasts a whole number of ticks`, `tick 0 is
  beat 1 of bar 1`, `a bar lasts 8 eighths and a phrase 4 bars`, `a tick within N ticks of a quarter note is on the
  beat`, `the tempo is 1800 over the ticks per eighth`.
- [ ] **P11.6** `TempoDropTiming` : une ligne par croche, calée sur `tickCount`. Après P11.4, P11.5. Cas : `the piece
  falls one row on every eighth`, `a piece that appears between two eighths falls on the next one`, `the level does
  not change the fall`, `the soft drop stays the one of the Classic`.
- [ ] **P11.7** `TempoScore`, bonus hors du moteur, et l'information « verrouillage amené par la descente rapide »
  (dans `PieceLocked` ou l'état, à trancher) ; événements `OnBeat` et `CadenceCleared` ; + 80 et × 2 sur le temps
  **[à vérifier]** ; le tick de verrouillage est retenu et la cadence appliquée au `LinesCleared` qui suit, ses
  points lus par l'écart des `Points` ; glossaire de `CLAUDE.md` (`TempoScore`). Après P11.5. Cas : `lines locked
  in bar 4 and cleared in bar 1 still make a cadence`, `a piece locked on the beat by the held soft drop earns 80 points`, `a piece
  locked by gravity alone earns no beat bonus`, `a piece locked out of the window earns nothing`, `lines cleared on
  the beat in bar 4 earn their points twice`, `lines cleared off the beat in bar 4 earn no cadence bonus`, `the
  total is the points of the engine plus the bonuses`, `the pieces on the beat are counted against every piece
  locked`.
- [ ] **P11.8** `TempoRules = ClassicRules.copy(dropTiming = …)` et sa fabrique ; propriété de non-régression du
  Classique : les parties de référence de P11.4 rendent les mêmes états. Après P11.6. Cas : `tempo rules are the
  classic rules but for the drop timing`, `the classic games recorded before the drop timing are played the same`.
- [ ] **P11.9** Accompagnement (`core:music`) : métronome (temps 1 marqué) et basse I–V–vi–IV, écrits à 100
  temps/min, joués au facteur `18 / ticksPerEighth` ; préférence Métronome **[à vérifier]** (activé par défaut).
  Cas : `the accompaniment clicks on every quarter, louder on beat 1`, `the bass plays I V vi IV, one bar each`, `the
  tempo factor is 18 over the ticks per eighth`, `without the metronome the bass plays on`.
- [ ] **P11.10** Variante `TEMPO` de `GameRoute` : `TempoRules`, horloge audio, accompagnement, total moteur +
  bonus, records dans un fichier DataStore à part (inclus dans la sauvegarde **[à vérifier]**, `BackupRulesTest`) ;
  `SteadyMusicDirection`, sans accélération (le danger ne change pas le tempo) ; thème Couleurs imposé et barres
  système (`systemBarVariant` connaît la variante, ADR 0026) ; tempo de la v1 120 temps/min et départ au niveau 0
  **[à vérifier]**. Après P11.3, P11.7, P11.8, P11.9. Cas : `a tempo game falls on the eighths of its tempo`, `a tempo
  game enters only its own records`, `the score shown is the points plus the bonuses`, `the danger does not change
  the tempo of a tempo game`.
- [ ] **P11.11** Panneau des temps (V41) : titre « AU TEMPO » et son record, encarts TEMPO (4 disques, « MESURE
  2/4 », bpm) et SUR LE TEMPS (« 12 / 40 »), autocollants « SUR LE TEMPS +80 » et « CADENCE × 2 », reflet du
  temps 1 (3 par seconde au plus), interrupteur Métronome dans la carte Pause, chacun en « Effets réduits » (ADR
  0024) ; TalkBack ; captures jour et nuit. Après P11.10.
- [ ] **P11.12** Cartouche « Au tempo » (`feature:cartridges`, `app`) : ouvre une partie ; parcours Macrobenchmark.
  Après P11.10 et P9.7.
- [ ] **P11.13** Mettre à jour la maquette (depuis claude.ai) : V41, cartouche Au tempo sur V36.

## P12 — Réglages généraux et réglages par jeu (`feature:settings`, `app`)

Demande du développeur (2026-10-28) : des réglages communs à tous les jeux, ouverts depuis le choix du jeu, et des
réglages propres à chaque jeu. Proposition de `designer-jeu` retenue : **une seule page Paramètres**, ouverte depuis
le choix du jeu (réglages généraux seuls) ou depuis un jeu (sa section en tête, puis « Tous les jeux »), toujours en
Couleurs. Le Serrurier, le Jardin et la Graine n'ont pas de réglage propre ; le Métronome d'Au tempo reste dans sa
pause (P11.9).

- [ ] **P12.1** ADR : réglages généraux et réglages par jeu (portée de chaque réglage, page unique avec une section
  par jeu, page toujours en Couleurs, module `feature:settings`) ; amende les ADR 0011 et 0022.
- [ ] **P12.2** Créer `feature:settings` et y déplacer la page Paramètres de `feature:home` (`SettingsScreen`,
  `SettingsViewModel`, `SettingsUiState`, `SettingsActions`, `KeysSketch`, la clé de route), sans changement de
  comportement, tests déplacés et verts ; `app` et `buildHealth` à jour. Après P12.1.
- [ ] **P12.3** Portée de la page, `SettingsRoute(game)` : la section « Classique » (Record, plus tard le Moderne)
  seulement depuis le Classique, sous un titre lu « Réglages du Classique », puis « Tous les jeux ». Cas : `the page
  opened from the choice of the game shows no record`, `the section of the Classic is read Réglages du Classique, a
  heading`. Après P12.2.
- [ ] **P12.4** Page Paramètres toujours en Couleurs, barres système comprises, quel que soit le thème du Classique
  (l'aspect Original de V21 disparaît). Après P12.2.
- [ ] **P12.5** Roue dentée sur le choix du jeu (V36, `feature:cartridges`, `app`) : disque de 48 dp à droite de la
  barre, lu « Paramètres », avant le titre dans l'ordre de TalkBack ; retour vers les cartouches ; retirer le test
  `CartridgesScreenTest › the only buttons, focusable or disabled nodes of the screen are the cartridges`. Après P12.3.
- [ ] **P12.6** La roue dentée de la Graine du jour ouvre la page sans section de jeu (aujourd'hui elle montre
  « Record du Classique »). Après P12.3.
- [ ] **P12.7** Textes de la page : sous-lignes de Gaucher, Vibrations et Vibration des touches qui nomment les jeux
  concernés (Classique, Graine du jour, Au tempo) ; note « Volumes : tout de suite. Le reste : dès le prochain jeu
  ouvert. » (aujourd'hui « Valable dès la partie suivante », faux pour les volumes et les Effets réduits) ; « Le
  thème du Classique se choisit dans son accueil. ». Après P12.3.
- [ ] **P12.8** Page Paramètres couchée et sur tablette (deux colonnes), après P5.84 et P12.3.
- [ ] **P12.9** Réorienter P6.17 et P6.21 vers la section « Classique » de la page. Après P12.3.
- [ ] **P12.10** Mettre à jour la maquette (depuis claude.ai) : V36 avec sa roue dentée ; nouvelle vue des Paramètres
  ouverts depuis le choix du jeu (jour, nuit, police ×2) ; V31 avec la section Classique et « Tous les jeux » ; note
  sur V38 ; V21 remplacée.

---

Le journal (ce qui a été fait, décisions, remarques de revue) est dans [JOURNAL.md](JOURNAL.md).
