# Suivi de l'implémentation

Une tâche = un incrément livrable, testé, commité. Les identifiants (`P1.3`) servent de
référence dans les commits et avec la commande `/prochaine-tache`.

Légende : `[ ]` à faire · `[~]` en cours · `[x]` terminé · **[à vérifier]** = règle à
confirmer sur une source avant d'écrire le test.

## Vue d'ensemble

| Phase | Objectif | État |
|---|---|---|
| P0 | Structure du projet et documentation | Build local validé ; reste P0.12 |
| P1 | Domaine du mode Classique (`core:game`) | À faire |
| P2 | Écran de jeu jouable (`feature:game`) | À faire |
| P3 | Accueil, préférences, records | À faire |
| P4 | Musique et sons | À faire |
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
- [ ] **P1.4** Déplacements gauche / droite / bas, refusés en cas de collision.
- [ ] **P1.5** `ClassicRotationSystem` : rotations horaire et anti-horaire, refus sans décalage,
  un jeu de tests par pièce. Les formes et leur ordre horaire existent (P1.2). `Piece` garde
  son `Orientation` (revue de P1.3) : la rotation la retrouve dans la liste circulaire du
  système, par exemple `rotate(type, orientation, direction): Orientation`, avec un `require`
  si l'orientation n'appartient pas au type.
- [ ] **P1.6** Verrouillage : la pièce rejoint le plateau quand elle ne peut plus descendre.
  Si un test l'exige : remplir les quatre cellules en une seule copie, refuser d'écraser une
  cellule occupée (revue de P1.1). Les cellules restées dans les lignes cachées sont perdues,
  la partie continue (source ROM, voir Plateau) ; `fill` reste strict, c'est le verrouillage
  qui les ignore. Introduire `Position.plus` quand l'addition apparaît une deuxième fois (P1.4).
- [ ] **P1.7** Effacement des lignes : 1 à 4 lignes, lignes non contiguës, chute des lignes
  supérieures.
- [x] **P1.8** `ClassicRandomizer` avec graine : un seul nouveau tirage si 7 ou répétition
  (règle TetrisWiki, ordre ROM, biais de la ROM non reproduit). Source d'aléatoire scriptée
  dans les tests. ADR 0014.
- [x] **P1.9** `ClassicGravityTable` : table des ticks par ligne, niveaux 0 à 29+ ; type `Level`.
- [x] **P1.10** `ClassicScoringRule` : 40/100/300/1200 × (niveau + 1), points de descente rapide.
- [x] **P1.11** `ClassicLevelProgression` : premier passage selon le niveau de départ, puis
  toutes les 10 lignes. Cas : départs 0, 5, 9. Type `Lines`.
- [ ] **P1.12** `GameEngine` : machine à états (apparition, chute, verrouillage, délai
  d'apparition, effacement, fin de partie). `tick()` et `apply(Input)` renvoient un nouvel état
  et des événements. Issu de la revue de P1.11 : `LevelProgression` expose sa plage de départ
  et la partie valide le départ une seule fois, à sa création (l'écran de choix lira la même
  plage) ; `GameState` garde le niveau de départ, le niveau est recalculé à chaque effacement ;
  `Lines` reçoit son addition ; points comptés avec le niveau **après** la montée (source ROM,
  voir la spécification). Issu de la revue de P1.8 : le moteur possède le `Randomizer` (ADR 0014)
  et le crée depuis la graine ; à l'apparition, la pièce suivante est tirée avec `previous` =
  la pièce qui vient d'apparaître ; une partie commence par deux tirages (`null`, puis la
  première pièce). Un test du moteur le prouve.
- [ ] **P1.13** Commandes maintenues : DAS 16/6, descente rapide **[à vérifier : 1 ligne toutes
  les 2 images]**.
- [ ] **P1.14** Délai d'apparition paramétrable (10 à 18 images selon la hauteur)
  **[à vérifier]**.
- [ ] **P1.15** Pause (l'état ne change plus pendant la pause) et statistiques de fin de partie.
- [ ] **P1.16** Événement `StackDanger` (pile proche du sommet), pour la musique.
- [ ] **P1.17** Type `Ticks` (strictement positif) pour le résultat de `GravityTable.ticksPerRow`,
  quand le moteur (P1.12) l'exigera : une table qui rendrait 0 bloquerait la gravité.
- [ ] **P1.18** Outillage : retirer `ignoreSourceSet("test", "androidTest")` de `dependencyAnalysis`.
  Essai en P1.1 : `buildHealth` échoue, car les bibliothèques de test posées par les plugins de
  convention sont inutilisées dans les modules sans tests. Voies : sévérité `warn` pour les jeux
  de sources de test, ou plugins qui ne posent que ce que le module utilise. Depuis P2.1,
  `buildHealth` conseille aussi (avertissement) de déclarer directement dans `feature:game`
  `junit-jupiter-params`, `assertj-core` et `junit-jupiter-api`, apportés par le plugin ; même
  avis sur `core:audio` depuis P4.3. Cause probable (revue de P4.3) : le plugin de convention
  pose l'agrégat `junit-jupiter` en `testImplementation` (`build-logic/.../TestConfiguration.kt`)
  au lieu de `junit-jupiter-api` / `-params`, le moteur en `testRuntimeOnly`. Corriger dans
  `build-logic`, pas dans les modules.
- [ ] **P1.19** Types `Score` (points) et « lignes d'un coup » (0 à 4, nom autre que
  `LinesCleared`, réservé à l'événement, par exemple `LineClear`) pour `ScoringRule`, quand le
  moteur (P1.7, P1.12) les exigera.
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
  l'écran rend le plafond de ticks (revue de P2.1).
- [ ] **P2.3** Rendu du plateau en `Canvas`, styles de cellules par thème (`docs/themes.md`).
- [ ] **P2.4** Colonne latérale : score, niveau, lignes, pièce suivante.
- [ ] **P2.5** Commandes tactiles : ◀ ▼ ▶, A, B, avec appui maintenu.
- [ ] **P2.6** Écran de pause (plateau masqué) et reprise.
- [ ] **P2.7** Écran de fin de partie, Rejouer / Menu. Entrée de pile distincte ou état de
  l'écran de jeu : trancher, voir ADR 0011.
- [ ] **P2.8** Test sur téléphone : boutons ou gestes ? Décider et consigner (ADR si changement).
- [ ] **P2.11** Mise en page tolérante au paysage dès le rendu du plateau : Android 16 ignore
  `screenOrientation="portrait"` sur grands écrans (lint `DiscouragedApi`). Décider avec P2.8 si
  l'attribut reste dans le manifeste.

## P3 — Accueil, préférences, records

- [ ] **P3.1** `UserPreferencesRepository` (DataStore) : thème, musique, niveau de départ.
- [ ] **P3.2** `HighScoreRepository` : meilleur score, détection d'un nouveau record.
- [ ] **P3.3** `AppContainer` et fabriques de ViewModels (ADR 0007 et 0013).
- [ ] **P3.4** Écran d'accueil complet, conforme à la maquette.
- [ ] **P3.5** Changement de thème appliqué à toute l'application. La couleur de fond de fenêtre
  (`app/src/main/res/values/colors.xml`) duplique `BloxPalettes.Original.page` : ajouter un
  commentaire croisé ou dériver l'une de l'autre pour qu'elles ne se désynchronisent pas.
- [ ] **P3.6** Polices : embarquer *Press Start 2P* et *Chakra Petch* (OFL) ou utiliser les polices
  téléchargeables. Décider et consigner.

## P4 — Musique et sons

Référence : [docs/musique.md](docs/musique.md).

- [x] **P4.1** `core:music` : `Pitch` (fréquence), `Note`, `Voice`, `Score`, en TDD.
- [x] **P4.2** Partitions *Korobeïniki* et *Menuet* : tests de cohérence (durée de chaque
  mesure, voix de même longueur). `Score.beatsPerBar`, invariants de longueur, `TRIANGLE`,
  plage de `Pitch` C0..B8.
- [x] **P4.3** Générateur d'échantillons pur (onde carrée, triangle, enveloppe, mixage), testé
  sur la JVM : `Synthesizer`, `mix` dans `core:audio`.
- [ ] **P4.4** `ChiptuneSequencer` sur `AudioTrack`, lecture en boucle, arrêt propre. Avant
  d'écrire le séquenceur, choisir entre durées entières (sous-unités du temps, par exemple
  48 par noire) et comparaison avec tolérance : les durées sont des `Double` exacts tant qu'il
  n'y a ni triolet ni position cumulée sur une boucle infinie (revue de P4.2). Revue de P4.3 :
  `render(score, tempoFactor)` comme seule entrée publique (`renderVoice` et `mix` deviennent
  internes) ; rendre hors du thread audio (`Dispatchers.Default`), une fois, et garder le
  résultat en cache (environ 2,3 Mo par voix) ; écrire des tranches en `MODE_STREAM` ; éviter la
  double allocation par note (écrire directement dans le tableau de la voix).
- [ ] **P4.5** Accélération du tempo sur `StackDanger`. Le facteur s'applique dans le
  séquenceur, sans recréer de `Score` (`bpm` entier : 150 × 1,25 = 187,5). Revue de P4.3 :
  passer le tempo du rendu en `Double`, rendre les deux tempos à l'avance et basculer à une
  frontière de temps (`pos' = round(pos × len' / len)`) ; ne jamais rééchantillonner, ce qui
  changerait la hauteur.
- [ ] **P4.6** `AudioFilePlayer` : choix d'un fichier personnel via le sélecteur du système.
- [ ] **P4.7** Aperçu « Écouter » sur l'accueil. Remplacer `Score.title` (texte affiché dans le
  domaine, « Menuet en sol » alors que l'accueil dit « Menuet ») par un identifiant que la
  feature traduit via `strings.xml` (revue de P4.2).
- [ ] **P4.8** Effets sonores courts (rotation, ligne, 4 lignes, fin de partie), optionnels.
- [ ] **P4.9** Droits : comparer la mélodie de *Korobeïniki* (`docs/musique.md`) à une partition
  publique du chant populaire, citer la source, et réécrire le rythme s'il suit la version du
  jeu de 1989 plutôt que le chant (revue de P4.2). **[à vérifier]**

## P5 — Finitions

- [ ] **P5.1** Icône de l'application et écran de démarrage.
- [ ] **P5.2** Vibrations (optionnelles) sur ligne effacée et fin de partie.
- [ ] **P5.3** Accessibilité : descriptions des boutons, contrastes, tailles de cible.
- [ ] **P5.4** Gestion du cycle de vie : pause automatique quand l'app passe en arrière-plan, déclenchée
  à `ON_PAUSE` (l'horloge d'images ne s'arrête qu'à `ON_STOP`, ADR 0012).
- [ ] **P5.5** Tests d'interface des parcours principaux (Compose UI Test).
- [ ] **P5.6** Build de release signé pour installation personnelle. Vérifier la restauration de
  la pile de navigation après mort du processus en build minifié (règles R8 de
  kotlinx-serialization, ADR 0011).
- [ ] **P5.7** Tests de captures d'écran à partir des previews existantes (plugin officiel Compose
  Preview Screenshot Testing), pour les deux thèmes, dans la CI.

## P6 — Mode Moderne (plus tard)

Nouvelles implémentations des politiques de `core:game`, sans modifier le moteur :
rotation SRS avec décalages, sac de 7, réserve, pièce fantôme, chute instantanée, délai de
verrouillage, plusieurs pièces suivantes.

---

## Journal

| Date | Tâche | Ce qui a été fait | Décisions / points ouverts |
|---|---|---|---|
| 2026-10-18 | P0.1 – P0.4 | Structure Gradle (8 modules, `build-logic`, catalogue), squelette Compose, documentation, ADR 0001 à 0008, CI. | Créé sans accès réseau : **aucun build exécuté**, à faire en P0.5. Versions figées à des valeurs connues de fin 2025, à mettre à jour. Identifiant d'application `ch.magori.blox`. |
| 2026-10-18 | P0.7 | Procédure de fin de fonctionnalité reprise d'un autre projet et adaptée : modules BloX, lint et APK, revue sans contexte, remarques écartées tracées, sources des valeurs confirmées, ADR, branche par tâche. | La maquette Claude Design ne se modifie que depuis claude.ai : Claude Code ajoute une tâche au suivi au lieu de la mettre à jour. |
| 2026-10-18 | P0.7 (complément) | Skill `/commit` (`.claude/skills/commit/SKILL.md`) : gitmoji + Conventional Commits, corps « pourquoi », pied `Tâche : Px.y` ; `/fin-de-tache` y renvoie ; CLAUDE.md sans doublons (commandes, règles d'architecture). | `/commit` ne pousse que sur demande ; `/fin-de-tache` est le demandeur. |
| 2026-10-18 | P0.8 | Tests des modules Android passés sur JUnit 5 (unitaires et instrumentés) via `de.mannodermaus.android-junit` 2.0.2, appliqué par `blox.android.library` et `blox.android.application`. `junit4`, `androidx-test-ext-junit` et `ui-test-junit4` retirés du catalogue, remplacés par `junit-jupiter-api` et `ui-test-android`. JUnit BOM 5.14.4. ADR 0009, ADR 0003 amendé. | JUnit 4 ne subsiste que par transitivité dans le lanceur AndroidX. JUnit 6 écarté : API 35 requise sur l'appareil. |
| 2026-10-18 | P0.5 | Premier build local réussi : wrapper Gradle 9.8.1 (somme SHA-256 vérifiée), AGP 9.4.1, Kotlin 2.4.21 compilé par le support intégré d'AGP (plugin `kotlin-android` retiré), coroutines et serialization 1.11.0, AndroidX et BOM Compose 2026.09.00 à jour, `compileSdk` 37. `build-logic` adapté au DSL d'AGP 9 (`CommonExtension` non générique, blocs `defaultConfig` / `compileOptions` / `buildFeatures` devenus propriétés, extension `kotlin` typée `KotlinAndroidExtension`). Domaine, tests unitaires Android, APK debug, lint et cache de configuration verts. ADR 0010. | AGP 9.4.1 exige Gradle ≥ 9.6 (9.5.1 refusé) : AGP et Gradle montent ensemble. Compose 1.12.1 et Navigation 2.10.2 exigent `compileSdk` 37 ; `targetSdk` reste 36. Lint (`app`, avertissements seulement) : `OldTargetApi` (targetSdk 36 < 37), orientation portrait verrouillée (`LockedOrientationActivity`, `DiscouragedApi` : Android 16 ignore l'orientation fixe, à décider en P2.8), `MissingApplicationIcon` (P5.1), `NewerVersionAvailable` junit-bom 6.1.3 (écarté par l'ADR 0009). Dépréciation Gradle 10 (`Configuration.setVisible`) émise par un plugin tiers, hors de notre code. `org.gradle.tooling.parallel=true` ajouté par Android Studio dans `gradle.properties`, conservé. Revue indépendante : remarques écartées — `kotlinx-coroutines-core` gardé au catalogue pour `core:audio` ; `junit-platform-launcher` en `testRuntimeOnly` des modules Android, redondance avec le plugin `android-junit` à vérifier au prochain build ; phrase d'en-tête anglaise de CLAUDE.md imposée par `/init` ; KDoc de `build-logic` en français, exception à trancher. |
| 2026-10-18 | Analyse d'architecture | Revue de la structure au regard du guide de modularisation Android et de *Now in Android* : fondations conformes, rien à restructurer. Tâches ajoutées : P0.9 à P0.13, P2.9 à P2.11, P5.7 ; P0.6, P2.1 et P3.5 complétés. | Deux décisions documentées à corriger avant le code concerné : boucle de jeu dans `viewModelScope` impossible avec `withFrameNanos` (P2.9) ; aucun chemin d'injection de `app` vers les features (P2.10). Navigation 3 à trancher tant que c'est gratuit (P0.13). |
| 2026-10-18 | P0.5 (complément) | Version de Java (`java = "17"`) dans le catalogue, lue par `build-logic` (modules et plugins de convention). Toolchain Java déclarée sur l'extension `java` de chaque module ; résolveur `foojay-resolver-convention` 1.0.0 dans les deux `settings.gradle.kts`. | La version du résolveur est écrite en dur dans les settings : les plugins de settings se résolvent avant le catalogue. Le JDK 17 est téléchargé au premier build sur une machine qui n'en a pas. |
| 2026-10-18 | P0.13 | Navigation 3 retenue, ADR 0011 ; ADR 0001 amendé. Catalogue : `navigation3-runtime` et `navigation3-ui` 1.2.0, Navigation Compose retirée. Plugin `blox.android.feature` : `navigation3-runtime` à la place de `navigation-compose`. `HomeRoute` et `GameRoute` deviennent des `NavKey`, `homeEntry` / `gameEntry` des extensions d'`EntryProviderScope`, `BloxNavHost` devient `BloxNavDisplay` (`rememberNavBackStack` + `NavDisplay`). Skills Android officiels (`navigation-3`, `edge-to-edge`, `testing-setup`, `r8-analyzer`, `jetpack-compose-performance`) copiés dans `.claude/skills/` (révision `5bda39f`). Build, lint (5 avertissements inchangés) et APK verts. | Revue indépendante : commentaire ProGuard corrigé (`navigation3-runtime` n'embarque aucune règle R8, seules celles de kotlinx-serialization comptent ; vérification en build minifié ajoutée à P5.6) ; décorateur ViewModel et `lifecycle-viewmodel-navigation3` retirés jusqu'à P2.2 ; `goBack` factorisé ; `dropUnlessResumed` noté pour P2.2 ; fin de partie (entrée ou état) renvoyée à P2.7. Écarté : renommage `HomeRoute` → `HomeKey`, vocabulaire des recettes officielles gardé et expliqué dans l'ADR. Pas de test JVM possible pour le parcours : preuve = build vert, test instrumenté en P5.5. |
| 2026-10-18 | P0.6, P0.9, P0.10, P0.11 | Outillage sur une seule branche. ktlint 1.8 (plugin 14.2) appliqué par les plugins de convention et à `build-logic`, réglé dans `.editorconfig` (clés IDE et ktlint alignées : style IntelliJ, paramètres un par ligne dès deux, composables en PascalCase) ; règles Compose de Slack 1.6.1 en `lintChecks`, `LocalBloxPalette` autorisé par `core/designsystem/lint.xml`. Plugin `dependency-analysis` 3.19.2 : configuré à la racine, appliqué par les plugins de convention, échec sur dépendance de production sans usage, sources de test ignorées, Compose regroupé ; dépendances anticipées retirées de `app`, `feature:*`, `core:audio`, `core:data` ; `serialization-json` → `-core` ; `navigation3-runtime` et `serialization-core` en `api` dans le plugin de feature ; `androidx.activity` déclaré dans `app`. `explicitApi()` strict, `allWarningsAsErrors` retiré. Mémoire Gradle 4 Go + daemon Kotlin 2 Go. CI : `ktlintCheck` (projet et `build-logic`), `buildHealth`. | Revue indépendante : retenu 1 à 7, 9 à 13 et 15 — mécanisme d'arrêt de l'horloge corrigé dans l'ADR 0012 ; `CreationExtras` dans l'ADR 0013 ; ktlint sur `build-logic` ; commentaires racine ; `.editorconfig` fusionné avec clés IDE ; `lifecycle-*` retiré du plugin de feature jusqu'à P2.2 et exclusions réduites à `ui-tooling-preview` ; commentaires des modules vidés allégés ; `GameLoop` ne rend que le nombre de ticks, le ViewModel seul écrit le moteur ; renvoi ADR 0011 → 0013 ; vérifications rapides en tête de CI ; seconde exception racine dans CLAUDE.md ; catalogue réduit aux entrées utilisées (coroutines, DataStore, lifecycle reviennent avec leur tâche, ce qui annule la décision « garder » du journal P0.5). Écartées : `ignoreSourceSet` par source set (réglage global plus simple, tâche de retrait notée) ; seuil de signature pour les constructeurs (value objects du domaine sur une ligne, voulu). À surveiller : plugin d'analyse « connu jusqu'à AGP 9.3.1 » avec AGP 9.4.1 ; retirer `ignoreSourceSet` au premier test (P1.1) et les exclusions `lifecycle-*` en P2.2 ; mémoire CI 4 Go + 2 Go sur ubuntu-latest. |
| 2026-10-18 | P2.9, P2.10 | ADR 0012 : l'horloge d'images appartient à l'écran (`LaunchedEffect` + `withFrameNanos` → `onFrame(nanos)`), `GameLoop` pure qui rend le nombre de ticks dus, le ViewModel seul écrivain du moteur, plafond de ticks par image obligatoire ; ADR 0006 amendé. ADR 0013 : fabrique définie par la feature, construite par `app`, passée à l'entrée, arguments de route via `CreationExtras` ; ADR 0007 précisé. `docs/architecture.md` (diagramme et sections) et CLAUDE.md à jour. | Pause automatique (P5.4) à déclencher sur `ON_PAUSE`, l'horloge ne s'arrêtant qu'à `ON_STOP`. Fin de partie entrée ou état : toujours P2.7. |
| 2026-10-18 | P1.9 | Premier code du domaine, en TDD (agent B, worktree parallèle). `Level` (value class, refuse le négatif), interface `GravityTable.ticksPerRow(Level)`, `ClassicGravityTable` : un `when` qui recopie la table de la spécification. 22 tests. | Revue indépendante : retenu — helper de test (règle de 2), vocabulaire `Tick` au lieu de « frames », `fun interface` retiré (aucun test ne l'exige), table en un seul `when`, nom `Classic` + politique (`ClassicGravityTable` ; P1.10 renommée `ClassicScoringRule`), test de la borne 0 (vert d'emblée, test de borne). Écarté : type `Ticks` dès maintenant, renvoyé à P1.17 quand le moteur l'exigera. |
| 2026-10-18 | P1.1 | Premier modèle du domaine, en TDD (agent A, worktree parallèle) : `Position(column, row)` (origine en haut à gauche), énumération `PieceType` (sans formes), `Cell` scellé (`Empty`, `Filled(type)`), `Board` immuable 10×20 (liste à plat, `get`, `contains`, `fill`, `equals`, `toString` ASCII, `Board.parse`). 12 tests (32 exécutions). Correction mermaid de `docs/architecture.md` embarquée en commit séparé. | Développement parallèle : trois agents dans des worktrees (`blox-wt/plateau`, `regles`, `boucle`), revue et suivi centralisés. Revue indépendante : retenu — limites vérifiées dans `get` / `fill` (bloquant : `Position(10, 0)` lisait (0, 1)), `toString` ASCII et aller-retour avec `parse` (vert d'emblée, contrôlé par mutation), `argumentSet` pour les tests paramétrés, `columns` / `rows` sans champ, tests de `fill` en ASCII. Écarté : remplissage groupé et refus d'écraser (renvoyés à P1.6), table explicite pour `parse` (les noms d'enum sont des lettres). Retrait d'`ignoreSourceSet` essayé, échec, renvoyé à P1.18. Ouvert : cellules au-dessus de la ligne 0 en P1.3 **[à vérifier]**. |
| 2026-10-18 | P2.1 | `GameLoop` (`feature:game`, `internal`, sans coroutine ni `core:game`) en TDD (agent C, worktree parallèle) : `advance(frameNanos): Int`, `reset()`, plafond par image en paramètre (4, refusé s'il est nul ou négatif), calcul entier (ns × 60 comparées à 10⁹), fraction de tick gardée après plafonnement, image qui recule sans tick ni dette. 11 tests (12 exécutions). `docs/architecture.md` précisé. | Tests verts d'emblée à cause de l'ordre des cas (trois images à 16,7 ms ; 60 images sans dérive ; plafond en paramètre ; reliquat à 510 ms) : chacun contrôlé par une mutation qui le fait échouer. Revue indépendante : retenu — image qui recule, plafond exigé par un test, reliquat après plafonnement, validation du plafond, `scaledNanos`, KDoc du ralentissement sous 15 images/s, appel à `reset()` noté en P2.2. Écarté : `Long?` boxé (60 allocations/s, négligeable), test à grande origine de temps (vert d'emblée, n'apprend rien). |
| 2026-10-18 | P1.10 | `ScoringRule` (KDoc du contrat : 0 à 4 lignes ; descente rapide comptée une fois au verrouillage) et `ClassicScoringRule` (`object`, table 40/100/300/1200 × (niveau + 1), 1 point par ligne descendue). 13 tests. | Revue indépendante : retenu — contrat en KDoc, validation par `require` en tête, commentaires Given/When/Then paraphrasants retirés (même style que la gravité), types dédiés renvoyés à P1.19. Écarté : cas « 0 ligne » dans le test paramétré (gardé à part, comme le niveau 0 de la gravité). Points ouverts : le mode Moderne (T-spins, combos, chute instantanée) devra élargir `ScoringRule`, donc le moteur, contrairement à la promesse « sans modifier le moteur » de P6 ; la NES plafonne le score affiché à 999 999, à trancher en P2.4. Descente rapide toujours **[à vérifier]**. |
| 2026-10-18 | P1.11 | `Lines` (value class, total cumulé, refuse le négatif), `LevelProgression.levelFor(start, lines)` et `ClassicLevelProgression` (`object`, départ 0 à 9, niveau = `max(départ, lignes ÷ 10)`, formule NES complète citée en KDoc). 16 tests. Spécification : ordre score / niveau tranché par la source ROM (points comptés avec le nouveau niveau). | TDD : les cas validés du premier passage étaient verts avec `lignes ÷ 10` ; deux cas « 0 ligne → niveau de départ » ajoutés pour obtenir le rouge. Test « +1 toutes les 10 lignes » vert d'emblée, contrôlé par mutation. Revue indépendante : retenu — test du premier passage découpé (avant / au seuil, cas intermédiaire 5 → 30 lignes), plage dans le message d'erreur, borne `Lines(0)`, validation du départ et état du moteur inscrits en P1.12. Écarté : oracle exhaustif contre la formule NES (réécrirait la règle dans le test). |
| 2026-10-18 | P1.2 | `Orientation` (ensemble de 4 décalages autour du pivot, invariant vérifié), `RotationSystem` (contrat en KDoc : ordre horaire circulaire, départ inclus, index 0 ≠ départ) et `ClassicRotationSystem` : les 19 orientations de la ROM NES ($8A9C, meatfighter.com) en `when` exhaustif, orientations de départ T, J, L pointe en bas, S, Z, I à plat. Helper de test `cellsOf` (dessin ASCII, `@` = pivot) et ses propres tests. `[à vérifier]` retirés : orientations de départ et formes de rotation, avec la source. `Orientation` et `Lines` ajoutés au langage du domaine (CLAUDE.md). | Table vérifiée cellule par cellule par la revue indépendante, aucune erreur. Verts d'emblée : « aucune cellule au-dessus du pivot » (contrôlé par mutation : départ du T à l'index 0) et la table de O. Revue : retenu — contrat de `RotationSystem`, `Orientation` en `Set` avec invariant, dessins de départ en constantes (règle de 2), `cellsOf` strict, `when` exhaustif, table en paires. Écarté pour l'instant : déplacer `cellsOf` dans un fichier commun (quand un deuxième paquet en aura besoin). Pas de type `Rotation` avant P1.5. |
| 2026-10-18 | P4.1 | Modèle musical de `core:music` (agent C, worktree parallèle) : `Pitch` (type valeur sur le numéro MIDI, `Pitch.parse`, fréquence, `toString` = nom), `Note` (silence = hauteur nulle, durée positive et finie), `Waveform` (`SQUARE`), `Voice` (volume dans [0, 1], au moins une note, durée totale), `Score` (tempo positif, au moins une voix). 17 tests (39 exécutions). `docs/musique.md` : notation `F#` (le `♯` Unicode n'était pas lu), règles du modèle, tests. | Verts d'emblée, contrôlés ou justifiés : C4 ≈ 261,63 Hz (mutation), bornes du volume (mutation), cas valide de `Score`, noms déjà refusés par l'analyseur (seul `A9` a donné le rouge de l'octave 0 à 8). Revue indépendante : retenu — égalité de `Pitch` par MIDI, bornes du volume, durée infinie refusée, analyseur strict, `TRIANGLE` retiré jusqu'à P4.2, voix non vide, cas valide et helper de test partagé. Écarté : types `Beats` / `Volume` / `Bpm` (alourdiraient l'écriture des partitions), refus de `E#` / `B#` (justes en hauteur). Notés en P4.2 et P4.5 : mesure, invariant de longueur, fabriques, plage MIDI, tempo. |
| 2026-10-18 | P1.3 | `Piece` (type, `Orientation`, pivot ; cellules = pivot + décalages) et `Board.fits(piece)` : colonnes 0 à 9, lignes −2 à 19, case vide ; lignes −2 et −1 cachées, toujours vides (ROM). `cellsOf` déplacé dans un fichier de test commun (deuxième paquet). 7 tests (14 exécutions). Spécification : lignes cachées et perte des cellules cachées au verrouillage (ROM), `[à vérifier]` de P1.3 retiré. | Mutants tués : bornes des colonnes dans les lignes cachées, dernière ligne avant le fond, première ligne cachée à −1 ou −3, case occupée ignorée. Revue indépendante : retenu — test « touche le bord sans le dépasser », une seule vérification des colonnes, KDoc de `Board` et de `Piece`, `canHold` renommé `fits`, formes dessinées dans les tests de `model` (plus de dépendance à `rules`), un seul « When » par test. Écarté : un paramètre par ligne pour `Piece` (ktlint `class-signature` impose la forme courte sous 120 colonnes, conforme au choix de P0.6 pour les value objects) ; `Position.plus` attendu en P1.4. |
| 2026-10-18 | P1.8 | `Randomizer.next(previous)` (KDoc : `previous` = dernière pièce tirée, celle de l'aperçu) et `ClassicRandomizer` (constructeur public par graine, constructeur `internal` sur `kotlin.random.Random` pour les tests) : premier tirage sur 8 valeurs, relance unique sur 7 si « aucune pièce » ou répétition, ordre ROM T, J, Z, O, S, L, I. 21 tests, faux `Random` scripté qui vérifie les bornes. Spécification : `[à vérifier]` retiré (TetrisWiki, ordre d'après la ROM). ADR 0014 : le tirage est le seul état modifiable du domaine. | Règle TetrisWiki retenue ; écart assumé : le biais de la ROM au second tirage n'est pas reproduit. Verts d'emblée, contrôlés par mutation : première pièce, graines identiques ou différentes. Revue indépendante : retenu — test « graines différentes », sens de `previous` en KDoc et test du moteur inscrit en P1.12, constructeur `Random` interne, faux `Random` plus strict, test de l'ordre ROM renommé, `romOrder` une pièce par ligne et constante « aucune pièce », ADR 0014 plutôt qu'une forme pure. Découvert : P1.20 (graine stable entre versions, si une graine est gardée). |
| 2026-10-18 | P4.2 | Partitions *Korobeïniki* (150 bpm, 4/4) et *Menuet* (132 bpm, 3/4) dans `Scores`, transcrites depuis `docs/musique.md` avec des fabriques privées (`bar`, `melody`, `bass`, `rootAndFifth`). `Score` reçoit `beatsPerBar` et deux invariants (voix de même longueur, nombre entier de mesures) ; `Waveform.TRIANGLE` revient ; `Pitch` limité à C0..B8. Tests de structure, de durées, de tonalité et de plage par voix. 64 exécutions dans `core:music`. | Transcription comparée note à note par la revue indépendante : aucune divergence sur les 32 mesures. Tests de données verts d'emblée par nature, chacun contrôlé par mutation (note à cheval sur une barre, hauteur changée, demi-ton, octave, silence ou cinquième note dans la basse). Revue : retenu — `beatsPerBar > 0`, nombre entier de mesures, plage de `Pitch`, basse à 4 notes sans silence, tests de tonalité et de plage. Écarté ou reporté : tolérance ou durées entières (P4.4), titre remplacé par un identifiant (P4.7), écriture des silences (goût). **Droits** : le rythme de *Korobeïniki* ressemble à la version du jeu de 1989 ; à vérifier contre le chant populaire (P4.9). |
