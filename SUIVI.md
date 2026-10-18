# Suivi de l'implémentation

Une tâche = un incrément livrable, testé, commité. Les identifiants (`P1.3`) servent de
référence dans les commits et avec la commande `/prochaine-tache`.

Légende : `[ ]` à faire · `[~]` en cours · `[x]` terminé · **[à vérifier]** = règle à
confirmer sur une source avant d'écrire le test.

## Vue d'ensemble

<!-- avancement:debut -->
| Avancement | Tâches | % |
|---|---|---|
| **Général** | 46 / 73 | `█████████████░░░░░░░` **63 %** |
| **MVP** (jouable sur téléphone) | 35 / 37 | `███████████████████░` **95 %** |
<!-- avancement:fin -->

Pourcentages en **nombre de tâches**, pas en effort : le moteur (P1.12) pèse bien plus qu'une
politique. Ils sont recalculés par `scripts/avancement.sh` à chaque fin de tâche, ne pas les
modifier à la main.

**MVP** : une partie complète jouable sur le téléphone, mode Classique, thème par défaut, sans
musique ni records. Fondations (P0, sauf P0.12), domaine jusqu'à la pause (P1.1 à P1.15),
écran de jeu (P2.1 à P2.7, P2.9, P2.10), injection (P3.3). Le test sur téléphone qui ouvre la
phase de jeu réelle est P2.8. Pour changer le périmètre, modifier la liste ci-dessous.

<!-- mvp: P0.1 P0.2 P0.3 P0.4 P0.5 P0.6 P0.7 P0.8 P0.9 P0.10 P0.11 P0.13 P1.1 P1.2 P1.3 P1.4 P1.5 P1.6 P1.7 P1.8 P1.9 P1.10 P1.11 P1.12 P1.13 P1.14 P1.15 P2.1 P2.2 P2.3 P2.4 P2.5 P2.6 P2.7 P2.9 P2.10 P3.3 -->

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
- [ ] **P0.12** Mises à jour automatiques de dépendances : Dependabot ou Renovate configuré pour
  `gradle/libs.versions.toml` et le wrapper, PR vérifiées par la CI.
- [ ] **P0.14** Doublures de test partagées : `testFixtures` de `core:game` (`ScriptedRandomizer`,
  helpers d'état) pour que `feature:game` ne les recopie pas (revue de P2.2).
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
- [ ] **P2.2** `GameViewModel` : `GameUiState`, conversion des actions en `Input`, tests avec
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
  depuis les types du domaine), pause (après P1.15).
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
- [ ] **P2.6** Écran de pause (plateau masqué) et reprise. Revue de P2.5 : à la pause, le
  moteur oublie les touches tenues, pour ne pas reprendre avec un DAS chargé ; le bouton pause
  est dans la barre du haut (maquette), pas dans `GameControls`. Revue de P2.7 : un éventuel
  `BackHandler` « retour = pause » est désactivé en fin de partie ; aucun tick n'est donné au
  moteur pendant la pause, pour que la durée de jeu n'avance pas. Fait : `PauseScreen(onResume,
  onMenu)`, `GameTopBar(record, onPause)` (pictogramme `pauseBars` au pixel près, record par
  `topBarTexts`), `TitledScreen` partagé avec la fin de partie, `PanelLabel`,
  `ScreenSizes.minTouchTarget`. Reste, avec P1.15 et P2.2 : la pause dans le moteur et le
  branchement.
- [ ] **P2.16** Mettre à jour la maquette : écran de pause et barre du haut, valeurs retenues
  en P2.6 (`docs/maquette.md`).
- [x] **P2.7** Écran de fin de partie, Rejouer / Menu : état de l'écran de jeu, Rejouer
  remplace la clé de la pile (ADR 0011 amendé). `GameOverScreen(GameOverUiState)`,
  `LabelledPanel` partagé avec la colonne latérale.
- [ ] **P2.15** Mettre à jour la maquette : confirmer les mesures et couleurs de V4 retenues en
  P2.7 (`docs/maquette.md`).
- [ ] **P2.8** Test sur téléphone : boutons ou gestes ? Décider et consigner (ADR si changement).
  Revue de P2.5 : évaluer le glissement d'un doigt de ◀ à ▼ ou ▶ (aujourd'hui le doigt reste
  attaché au premier bouton) ; le gérer demande un seul `pointerInput` sur ◀ ▼ ▶, une fonction
  pure `buttonAt(position)` et `HeldButtons.move`, testables sur la JVM.
- [ ] **P2.11** Mise en page tolérante au paysage dès le rendu du plateau : Android 16 ignore
  `screenOrientation="portrait"` sur grands écrans (lint `DiscouragedApi`). Décider avec P2.8 si
  l'attribut reste dans le manifeste. Passer la taille de cellule du plateau à l'aperçu de la
  pièce suivante, pour des cellules de même taille comme sur la NES (revue de P2.4).

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
  Reste au branchement (P2.2, incrément 2) : créer le DataStore dans `AppContainer`
  (`filesDir/datastore/records.preferences_pb`), `submit` à `GameOver`, record dans la barre
  du haut et sur V4.
- [x] **P3.3** `AppContainer` et fabriques de ViewModels (ADR 0007 et 0013). Fait avec P2.2 :
  `AppContainer` construit `GameViewModel.Factory` ; les repositories (P3.1, P3.2) et la
  fabrique de l'accueil (P3.4) s'y ajouteront.
- [ ] **P3.4** Écran d'accueil complet, conforme à la maquette.
- [ ] **P3.5** Changement de thème appliqué à toute l'application. La couleur de fond de fenêtre
  (`app/src/main/res/values/colors.xml`) duplique `BloxPalettes.Original.page` : ajouter un
  commentaire croisé ou dériver l'une de l'autre pour qu'elles ne se désynchronisent pas.
- [ ] **P3.6** Polices : embarquer *Press Start 2P* et *Chakra Petch* (OFL) ou utiliser les polices
  téléchargeables. Décider et consigner. Les chiffres de la colonne latérale sont en
  `FontFamily.Monospace` en attendant (P2.4) ; les tailles sont regroupées dans `SidePanelSizes`.

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
- [ ] **P4.5** Accélération du tempo sur `StackDanger`. Le facteur s'applique dans le
  séquenceur, sans recréer de `Score` (`bpm` entier : 150 × 1,25 = 187,5). Revue de P4.3 :
  passer le tempo du rendu en `Double`, rendre les deux tempos à l'avance et basculer à une
  frontière de temps (`pos' = round(pos × len' / len)`) ; ne jamais rééchantillonner, ce qui
  changerait la hauteur. Revue de P4.4 : le rendu et le cache acceptent déjà le facteur ; lire
  entre deux tranches un facteur demandé (`StateFlow` ou `@Volatile`) sans rouvrir la sortie ;
  calculer la position en `Long` ; arrondir au temps suivant (un temps = `len / totalBeats`
  échantillons). Au branchement, décider si `play` de la partition déjà jouée est sans effet
  (utile si le ViewModel rappelle `play` à la reprise), et journaliser l'échec audio attrapé.
- [ ] **P4.6** `AudioFilePlayer` : choix d'un fichier personnel via le sélecteur du système.
  Revue de P3.1 : garder l'URI dans `settings.preferences_pb` (clé `music_file_uri`,
  `setMusicFile(uri)`, permission persistante), avec repli sur *Korobeïniki* si l'URI n'est
  plus lisible ; `MusicChoice.MY_FILE` sans fichier retenu se comporte de même.
- [ ] **P4.7** Aperçu « Écouter » sur l'accueil. Remplacer `Score.title` (texte affiché dans le
  domaine, « Menuet en sol » alors que l'accueil dit « Menuet ») par un identifiant que la
  feature traduit via `strings.xml` (revue de P4.2).
- [ ] **P4.8** Effets sonores courts (rotation, ligne, 4 lignes, fin de partie), optionnels.
- [x] **P4.9** Droits : comparer la mélodie de *Korobeïniki* (`docs/musique.md`) à une partition
  publique du chant populaire, citer la source, et réécrire le rythme s'il suit la version du
  jeu de 1989 plutôt que le chant (revue de P4.2). Fait : 5 mesures sur 8 s'écartaient du chant ;
  mélodie remplacée par celle du chant (sources citées dans `docs/musique.md`), basse de la
  mesure 6 en A2/E3.
- [ ] **P4.10** Droits : confirmer la mélodie de *Korobeïniki* sur une édition d'époque
  (Prigojy 1898, recueils de Panina vers 1905, НЭБ) ; les deux sources nettes consultées en P4.9
  sont des notations modernes. Retirer alors « [à vérifier] (droits) » de `docs/musique.md`.

## P5 — Finitions

- [ ] **P5.1** Icône de l'application et écran de démarrage.
- [ ] **P5.2** Vibrations (optionnelles) sur ligne effacée et fin de partie.
- [ ] **P5.3** Accessibilité : descriptions des boutons, contrastes, tailles de cible ;
  `semantics { contentDescription }` sur `BoardCanvas` (revue de P2.3).
- [ ] **P5.4** Gestion du cycle de vie : pause automatique quand l'app passe en arrière-plan, déclenchée
  à `ON_PAUSE` (l'horloge d'images ne s'arrête qu'à `ON_STOP`, ADR 0012).
- [ ] **P5.5** Tests d'interface des parcours principaux (Compose UI Test). Au premier test
  instrumenté, vérifier que le moteur Jupiter arrive sur `debugAndroidTestRuntimeClasspath`
  (`:feature:game:dependencies`), ou le déclarer en `androidTestRuntimeOnly` (revue de P1.18).
  Parcours à couvrir : fin de partie → Rejouer (nouvelle partie) → Menu. Revue de P2.2 : le
  ViewModel du jeu garde les `CreationExtras` par défaut (`SavedStateHandle` disponible), sans
  test JVM possible aujourd'hui.
- [ ] **P5.6** Build de release signé pour installation personnelle. Vérifier la restauration de
  la pile de navigation après mort du processus en build minifié (règles R8 de
  kotlinx-serialization, ADR 0011).
- [ ] **P5.7** Tests de captures d'écran à partir des previews existantes (plugin officiel Compose
  Preview Screenshot Testing), pour les deux thèmes, dans la CI. Revue de P2.3 : le dessin de
  `BoardCanvas` n'est vérifié par aucun test ni à l'œil ; ajouter aussi un fichier de
  configuration de stabilité Compose (`build-logic`) listant `ch.magori.blox.core.game.model.*`.

## P6 — Mode Moderne (plus tard)

Revue de P1.12a : la promesse « sans modifier le moteur » ne tiendra pas entièrement (file de
plusieurs pièces suivantes, réserve, délai de verrouillage réinitialisable, chute instantanée,
T-spin et combos demandent de l'état). `Rules` et la position d'apparition sortie du moteur
limitent l'écart ; à amender dans CLAUDE.md au moment de P6.
Nouvelles implémentations des politiques de `core:game`, sans modifier le moteur :
rotation SRS avec décalages, sac de 7, réserve, pièce fantôme, chute instantanée, délai de
verrouillage, plusieurs pièces suivantes. Règle « lock out » (revue de P1.6) : une pièce
verrouillée entièrement au-dessus du plateau met fin à la partie, en politique ; impossible
en mode Classique, où toute pièce apparaît avec des cellules en ligne 0 ou plus bas.


---

Le journal (ce qui a été fait, décisions, remarques de revue) est dans [JOURNAL.md](JOURNAL.md).
