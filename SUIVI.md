# Suivi de l'implémentation

Une tâche = un incrément livrable, testé, commité. Les identifiants (`P1.3`) servent de
référence dans les commits et avec la commande `/prochaine-tache`.

Légende : `[ ]` à faire · `[~]` en cours · `[x]` terminé · **[à vérifier]** = règle à
confirmer sur une source avant d'écrire le test.

## Vue d'ensemble

| Phase | Objectif | État |
|---|---|---|
| P0 | Structure du projet et documentation | Build local validé ; reste P0.6 et P0.9 à P0.12 |
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
- [ ] **P0.6** Ajouter une analyse statique : ktlint (via le plugin Gradle) ou detekt, branchée
  dans `build-logic` et dans la CI, plus les règles lint Compose de Slack
  (`com.slack.lint.compose:compose-lint-checks`) sur les modules Compose.
- [x] **P0.7** Définition de terminé (`CLAUDE.md`), commande `/fin-de-tache`, revue
  indépendante par sous-agent, vues de maquette numérotées V1 à V4.
- [x] **P0.8** JUnit 5 partout : plugin `de.mannodermaus.android-junit` dans les plugins de
  convention, JUnit 4 retiré du catalogue, ADR 0009.
- [ ] **P0.9** Plugin `dependency-analysis` (autonomousapps) branché dans `build-logic` et la CI,
  puis retrait des dépendances déclarées sans usage : `feature:home` → `core:audio`, `core:data` ;
  `app` → `core:audio`, `core:data`. Elles reviennent quand le code les exige (P3, P4).
- [ ] **P0.10** `explicitApi()` strict (au lieu de `explicitApiWarning()`) sur les modules Kotlin
  purs, tant qu'ils sont vides ; retirer `allWarningsAsErrors.set(false)` (valeur par défaut).
- [ ] **P0.11** Mémoire Gradle : `org.gradle.jvmargs` à 4 Go et `kotlin.daemon.jvmargs` dans
  `gradle.properties` (AGP 9 + Kotlin 2.4 + Compose sur huit modules).
- [ ] **P0.12** Mises à jour automatiques de dépendances : Dependabot ou Renovate configuré pour
  `gradle/libs.versions.toml` et le wrapper, PR vérifiées par la CI.
- [x] **P0.13** **Navigation 3** retenue (ADR 0011) : `navigation3-runtime` / `navigation3-ui` 1.2.0 et
  `lifecycle-viewmodel-navigation3` au catalogue, Navigation Compose retirée. Chaque feature expose
  une clé `NavKey` et une extension d'`EntryProviderScope` ; `app` possède la pile
  (`BloxNavDisplay`). Skills Android officiels copiés dans `.claude/skills/`.

## P1 — Domaine du mode Classique (`core:game`)

Référence : [docs/regles-mode-classique.md](docs/regles-mode-classique.md). Tout en TDD.

- [ ] **P1.1** `Position`, `Board` immuable 10×20 : cellule vide/occupée, hors limites,
  `Board.parse(ascii)` pour les tests.
- [ ] **P1.2** `PieceType` et formes des 7 pièces dans chaque orientation.
  Cas : nombre de cellules, nombre d'états par pièce (O : 1 ; I, S, Z : 2 ; T, J, L : 4).
  Orientations de départ **[à vérifier]**.
- [ ] **P1.3** Placement d'une pièce : collision avec les bords, le fond et les cellules occupées.
- [ ] **P1.4** Déplacements gauche / droite / bas, refusés en cas de collision.
- [ ] **P1.5** `ClassicRotationSystem` : rotations horaire et anti-horaire, refus sans décalage.
  Centres de rotation **[à vérifier]**, un jeu de tests par pièce.
- [ ] **P1.6** Verrouillage : la pièce rejoint le plateau quand elle ne peut plus descendre.
- [ ] **P1.7** Effacement des lignes : 1 à 4 lignes, lignes non contiguës, chute des lignes
  supérieures.
- [ ] **P1.8** `ClassicRandomizer` avec graine : un seul nouveau tirage si 7 ou répétition
  **[à vérifier]**. Tester avec une source d'aléatoire scriptée.
- [ ] **P1.9** `ClassicGravity` : table des images par ligne, niveaux 0 à 29+.
- [ ] **P1.10** `ClassicScoring` : 40/100/300/1200 × (niveau + 1), points de descente rapide.
- [ ] **P1.11** `ClassicLevelProgression` : premier passage selon le niveau de départ, puis
  toutes les 10 lignes. Cas : départs 0, 5, 9.
- [ ] **P1.12** `GameEngine` : machine à états (apparition, chute, verrouillage, délai
  d'apparition, effacement, fin de partie). `tick()` et `apply(Input)` renvoient un nouvel état
  et des événements.
- [ ] **P1.13** Commandes maintenues : DAS 16/6, descente rapide **[à vérifier : 1 ligne toutes
  les 2 images]**.
- [ ] **P1.14** Délai d'apparition paramétrable (10 à 18 images selon la hauteur)
  **[à vérifier]**.
- [ ] **P1.15** Pause (l'état ne change plus pendant la pause) et statistiques de fin de partie.
- [ ] **P1.16** Événement `StackDanger` (pile proche du sommet), pour la musique.

## P2 — Écran de jeu jouable (`feature:game`)

- [ ] **P2.9** **Avant P2.1.** Amender l'ADR 0006 : `withFrameNanos` exige un `MonotonicFrameClock`
  absent de `viewModelScope`, la boucle ne peut pas tourner dans le ViewModel. L'horloge d'images
  vit côté Compose (`LaunchedEffect` qui appelle `viewModel.onFrame(nanos)`), le ViewModel délègue
  à une classe `GameLoop` pure. Le jeu se suspend de lui-même quand l'écran n'est plus affiché
  (couvre en partie P5.4). Mettre `docs/architecture.md` à jour.
- [ ] **P2.1** `GameLoop` à pas fixe : accumulateur testé avec une horloge factice
  (ADR 0006 amendé en P2.9).
- [ ] **P2.10** **Avant P2.2.** Chemin d'injection de `app` vers les features sans dépendance
  inverse (ADR 0007) : l'extension `gameEntry(...)` reçoit une `ViewModelProvider.Factory`
  construite dans `BloxNavDisplay` (option retenue par défaut, voir ADR 0011), ou un
  `CompositionLocal` déclaré dans un module partagé expose le conteneur. Consigner dans
  l'ADR 0007 ; P3.3 s'y conforme.
- [ ] **P2.2** `GameViewModel` : `GameUiState`, conversion des actions en `Input`, tests avec
  coroutines-test et Turbine. Ajouter alors `lifecycle-viewmodel-navigation3` et les deux
  décorateurs de `NavDisplay` (ADR 0011), et envelopper `onPlay` dans `dropUnlessResumed` côté
  feature pour qu'un double appui ne crée pas deux parties.
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
- [ ] **P3.3** `AppContainer` et fabriques de ViewModels (ADR 0007).
- [ ] **P3.4** Écran d'accueil complet, conforme à la maquette.
- [ ] **P3.5** Changement de thème appliqué à toute l'application. La couleur de fond de fenêtre
  (`app/src/main/res/values/colors.xml`) duplique `BloxPalettes.Original.page` : ajouter un
  commentaire croisé ou dériver l'une de l'autre pour qu'elles ne se désynchronisent pas.
- [ ] **P3.6** Polices : embarquer *Press Start 2P* et *Chakra Petch* (OFL) ou utiliser les polices
  téléchargeables. Décider et consigner.

## P4 — Musique et sons

Référence : [docs/musique.md](docs/musique.md).

- [ ] **P4.1** `core:music` : `Pitch` (fréquence), `Note`, `Voice`, `Score`, en TDD.
- [ ] **P4.2** Partitions *Korobeïniki* et *Menuet* : tests de cohérence (durée de chaque
  mesure, voix de même longueur).
- [ ] **P4.3** Générateur d'échantillons pur (onde carrée, triangle, enveloppe, mixage), testé
  sur la JVM.
- [ ] **P4.4** `ChiptuneSequencer` sur `AudioTrack`, lecture en boucle, arrêt propre.
- [ ] **P4.5** Accélération du tempo sur `StackDanger`.
- [ ] **P4.6** `AudioFilePlayer` : choix d'un fichier personnel via le sélecteur du système.
- [ ] **P4.7** Aperçu « Écouter » sur l'accueil.
- [ ] **P4.8** Effets sonores courts (rotation, ligne, 4 lignes, fin de partie), optionnels.

## P5 — Finitions

- [ ] **P5.1** Icône de l'application et écran de démarrage.
- [ ] **P5.2** Vibrations (optionnelles) sur ligne effacée et fin de partie.
- [ ] **P5.3** Accessibilité : descriptions des boutons, contrastes, tailles de cible.
- [ ] **P5.4** Gestion du cycle de vie : pause automatique quand l'app passe en arrière-plan.
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
