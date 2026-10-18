# Suivi de l'implémentation

Une tâche = un incrément livrable, testé, commité. Les identifiants (`P1.3`) servent de
référence dans les commits et avec la commande `/prochaine-tache`.

Légende : `[ ]` à faire · `[~]` en cours · `[x]` terminé · **[à vérifier]** = règle à
confirmer sur une source avant d'écrire le test.

## Vue d'ensemble

<!-- avancement:debut -->
| Avancement | Tâches | % |
|---|---|---|
| **Général** | 31 / 66 | `█████████░░░░░░░░░░░` **47 %** |
| **MVP** (jouable sur téléphone) | 27 / 37 | `███████████████░░░░░` **73 %** |
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
- [ ] **P1.6** Verrouillage : la pièce rejoint le plateau quand elle ne peut plus descendre.
  Si un test l'exige : remplir les quatre cellules en une seule copie, refuser d'écraser une
  cellule occupée (revue de P1.1). Les cellules restées dans les lignes cachées sont perdues,
  la partie continue (source ROM, voir Plateau) ; `fill` reste strict, c'est le verrouillage
  qui les ignore. Ne pas appeler le verrouillage « place » : `Board.place` est le contrôle d'un
  candidat (revue de P1.5).
- [x] **P1.7** Effacement des lignes : 1 à 4 lignes, lignes non contiguës, chute des lignes
  supérieures. `Board.clearFullRows(): RowsCleared`.
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
  de rotation seulement si la pièce change (O) **[à vérifier]**.
- [x] **P1.13** Commandes maintenues : DAS 16/6, descente rapide 2/2, DAS chargé contre un mur
  (sources ROM) ; `AutoRepeat`, `InputRepeat`, `ClassicInputRepeat`.
- [x] **P1.14** Délai d'apparition paramétrable (10 à 18 images selon la hauteur) :
  `EntryDelay`, `ClassicEntryDelay`, `Board.ROWS_WITH_HIDDEN`.
- [ ] **P1.15** Pause (l'état ne change plus pendant la pause) et statistiques de fin de partie.
- [ ] **P1.16** Événement `StackDanger` (pile proche du sommet), pour la musique.
- [ ] **P1.17** Type `Ticks` (strictement positif) pour le résultat de `GravityTable.ticksPerRow`
  et d'`EntryDelay.ticksAfterLock`, quand le moteur (P1.12) l'exigera : une table qui rendrait 0
  bloquerait la gravité.
- [x] **P1.18** Outillage : `ignoreSourceSet` retiré de `dependencyAnalysis`, remplacé par des
  règles par jeu de sources de test (échec, avec exclusion des bibliothèques posées par les
  conventions) ; JUnit 5 déclaré en `-api` / `-params` + moteur en `testRuntimeOnly`.
- [ ] **P1.19** Types `Score` (points) et « lignes d'un coup » (0 à 4, nom autre que
  `LinesCleared`, réservé à l'événement, par exemple `LineClear`) pour `ScoringRule`, quand le
  moteur (P1.7, P1.12) les exigera. Revue de P1.7 : `RowsCleared` donnera alors ce type par
  une propriété calculée (`clearedRows.size` validé 0 à 4), pour `ScoringRule` et `Lines`.
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
- [ ] **P5.5** Tests d'interface des parcours principaux (Compose UI Test). Au premier test
  instrumenté, vérifier que le moteur Jupiter arrive sur `debugAndroidTestRuntimeClasspath`
  (`:feature:game:dependencies`), ou le déclarer en `androidTestRuntimeOnly` (revue de P1.18).
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

Le journal (ce qui a été fait, décisions, remarques de revue) est dans [JOURNAL.md](JOURNAL.md).
