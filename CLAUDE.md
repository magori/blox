# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Ce fichier décrit comment on travaille sur BloX, un jeu de blocs Android (Kotlin, Compose) dont le
mode Classique reproduit les règles de la version NES de 1989. Il complète
[README.md](README.md), [SUIVI.md](SUIVI.md) (tâches et avancement), [JOURNAL.md](JOURNAL.md)
(ce qui a été fait et décidé, tâche par tâche) et [docs/](docs/).

## État du projet

- Premier build local validé le 2026-10-18 (P0.5) : Gradle 9.8.1, AGP 9.4.1, Kotlin 2.4.21,
  `compileSdk` 37, `targetSdk` 36, `minSdk` 26. Aucune règle de jeu n'est encore implémentée :
  le travail commence en P1 (`core:game`).
- Toutes les versions vivent dans `gradle/libs.versions.toml`. AGP et Gradle montent ensemble (AGP
  impose un Gradle minimal), AGP et Kotlin aussi (Kotlin intégré, ADR 0010). Les bibliothèques
  AndroidX récentes imposent un `compileSdk` minimal via leurs métadonnées AAR : si
  `checkDebugAarMetadata` échoue après une montée de version, c'est ce réglage qu'il faut relever.
- Les PR Dependabot (`.github/dependabot.yml`, hebdomadaires, groupées) passent par la même CI. On ne
  fusionne la PR du groupe `agp-kotlin-gradle` qu'avec AGP, Kotlin et le wrapper montés ensemble et
  un build complet vert ; une PR qui monte DataStore emporte Okio. Foojay et la version du moteur
  ktlint se surveillent à la main.
- Les worktrees (`blox-wt/…`) partagent le cache de build local. La sortie des tâches ktlint cite
  des chemins absolus (plugin 14.2.0, issues #532 et #1041) : `build-logic` lie leur clé de cache au
  chemin du module (P5.31), si bien qu'un worktree neuf réanalyse tout une fois (environ 7 s). Si
  `ktlintCheck` signale les fichiers d'un autre dossier, la parade a sauté :
  `./gradlew ktlintCheck --rerun-tasks` remet la sortie au propre, et
  `scripts/verifier-cache-ktlint.sh` reproduit le défaut (code 0 si corrigé, 1 sinon). À chaque
  montée du plugin, relancer ce script sans la parade, et la retirer s'il passe.

## Avant de commencer une tâche

1. Lire [SUIVI.md](SUIVI.md) : prendre la **prochaine tâche non cochée** de la phase en cours,
   sauf demande contraire. Les identifiants (`P1.3`) servent de référence dans les commits.
2. Lire la documentation qui la concerne (règles, architecture, thèmes, musique).
3. Si une règle est marquée **[à vérifier]** dans `docs/regles-mode-classique.md`, le signaler
   et proposer la valeur à confirmer **avant** d'écrire le test.

Deux commandes slash encapsulent ce flux : `/prochaine-tache [Px.y]` (reprendre une tâche en
TDD) et `/revue [branche|fichier]` (revue des changements selon ces conventions).

## TDD strict

Le développeur pratique le TDD de façon stricte. Pour tout code de `core:game`, `core:music`, `core:locksmith`
et `core:garden`, et pour toute logique testable ailleurs :

1. **Rouge** : écrire un seul test qui échoue, pour la plus petite règle suivante. Le lancer
   et montrer l'échec.
2. **Vert** : écrire le code minimal pour le faire passer. Relancer.
3. **Refactoring** : nettoyer code **et** tests, tests au vert à chaque étape.

Ne jamais écrire de code de production sans test qui l'exige. Proposer la liste des cas de
test d'une tâche avant de commencer, pour validation.

## Commandes

Sous Windows, `./gradlew` fonctionne dans Git Bash ; dans PowerShell utiliser `.\gradlew.bat`.

```bash
./gradlew :core:game:test       # domaine (JUnit 5, JVM, sans émulateur)
./gradlew :core:music:test      # partitions
./gradlew :core:locksmith:test  # le Serrurier
./gradlew :core:garden:test     # le Jardin musical
./gradlew testDebugUnitTest     # tous les tests unitaires (modules Android inclus)
./gradlew ktlintCheck           # style Kotlin (ktlintFormat corrige)
./gradlew -p build-logic ktlintCheck   # idem pour les plugins de convention
./gradlew buildHealth           # dépendances déclarées sans usage ou mal déclarées
./gradlew lintDebug             # lint Android, règles Compose de Slack comprises
./gradlew compileDebugKotlin -Pblox.composeReports=true --rerun   # rapports de stabilité Compose (build/compose-reports) ; types stables : compose-stability.conf
./gradlew assembleDebug         # APK
./gradlew assembleRelease       # APK release minifié (R8), non signé ; lance aussi lintVitalRelease
./gradlew assembleDebugAndroidTest   # compile les tests instrumentés (androidTest) de tous les modules Android
./gradlew installDebug          # installation sur un appareil connecté
./gradlew :benchmark:assemble   # compile les mesures Macrobenchmark (CI)
./gradlew :app:generateBaselineProfile   # régénère le profil de référence (appareil Android 13+), à versionner
./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest   # mesures, sur un téléphone de préférence
#   sur émulateur : -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.skipOnEmulator=false
#                   -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
scripts/verifier-tracabilite.sh # chaque `Classe › nom du test` des docs désigne un test existant
```

Un seul test ou une seule classe :

```bash
# Module Kotlin pur (JUnit 5) : classe, ou classe + méthode
./gradlew :core:game:test --tests "ch.magori.blox.core.game.model.BoardTest"
./gradlew :core:game:test --tests "ch.magori.blox.core.game.model.BoardTest.a full line is cleared*"

# Module Android : la tâche est testDebugUnitTest
./gradlew :feature:game:testDebugUnitTest --tests "ch.magori.blox.feature.game.GameViewModelTest"
```

La CI (`.github/workflows/ci.yml`) enchaîne exactement : `scripts/verifier-tracabilite.sh` et
`wrapper-validation` (avant même l'installation du JDK), `ktlintCheck` (projet et `build-logic`),
`buildHealth`, tests du domaine, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease` (R8 et
`lintVitalRelease`), `assembleDebugAndroidTest` et `compileDebugScreenshotTestKotlin` (compilation
seule des tests instrumentés et des tests de captures, pas d'émulateur), `validateDebugScreenshotTest` (captures
comparées aux références, P5.8), puis `:benchmark:assemble`. Ce qui passe
localement avec ces commandes passe en CI. En cas d'échec, les `build/reports`, les captures rendues et leurs
différences sont publiés
comme artefact `rapports`. Les actions sont épinglées par SHA (tag en commentaire, tenus à jour par
Dependabot `github-actions`) et le jeton est en lecture seule ; un second job, sur `push` vers
`main` seulement, soumet le graphe des dépendances Gradle (`contents: write`).

**Zéro avertissement** (P5.25, `build-logic`) : un avertissement fait échouer le build.
- Kotlin : `allWarningsAsErrors` partout, production et tests (`KotlinConfiguration.kt`). Les tests
  unitaires Android consentent à `ExperimentalCoroutinesApi` (`runCurrent`, `setMain`…) par la
  convention : pas d'`@OptIn` local pour ça. Un avertissement assumé se tait au plus près, par
  `@Suppress("…")` avec sa raison.
- Une PR Dependabot qui casse sur une nouvelle dépréciation : 1. corriger l'appel ; 2. sinon,
  `@Suppress("DEPRECATION")` au plus près, avec sa raison ; 3. en dernier recours, un assouplissement
  ciblé et temporaire dans `build-logic`, `-Xwarning-level=<NOM>:warning` (préférable à
  `-Xsuppress-warning`, qui masque tout à fait), avec une tâche dans `SUIVI.md` pour le retirer.
- Lint : `warningsAsErrors` dans tous les modules Android (`LintConfiguration.kt`). Un avertissement
  assumé se traite à la source (`tools:ignore`, `@Suppress`, `lint.xml` du module) avec sa raison ;
  sinon dans la base `lint-baseline.xml` du module (seul `app` en a une, pour `OldTargetApi`), dont la
  raison est aussi écrite dans `LintConfiguration.kt`, car `updateLintBaseline` efface les commentaires.
  `GradleDependency`, `NewerVersionAvailable` et `AndroidGradlePluginVersion` sont désactivées :
  Dependabot propose les montées, et ces règles feraient échouer `main` à chaque nouvelle version.

## Architecture

Détail dans [docs/architecture.md](docs/architecture.md) et dans les ADR
([docs/adr/](docs/adr/README.md)). Les points qu'il faut connaître avant de toucher au code :

### Modules et plugins de convention

Un module n'applique qu'un plugin de convention (`build-logic/convention`) et déclare ses
dépendances propres ; il ne configure jamais `compileSdk`, `minSdk`, JUnit, Compose, etc.
lui-même.

| Type de module | Plugin(s) à appliquer | Tests fournis par le plugin |
|---|---|---|
| Kotlin pur (`core:game`, `core:music`, `core:locksmith`, `core:garden`) | `blox.jvm.library` | JUnit 5 + AssertJ (`useJUnitPlatform`) |
| Bibliothèque Android (`core:audio`, `core:data`, `core:haptics`) | `blox.android.library` | JUnit 5 (plugin `android-junit`) + AssertJ + coroutines-test + Turbine ; tests instrumentés JUnit 5 |
| Bibliothèque Compose (`core:designsystem`, `core:board`) | `blox.android.library` + `blox.android.library.compose` | idem |
| Feature (`feature:cartridges`, `feature:home`, `feature:game`, `feature:daily`…) | `blox.android.feature` (inclut library + compose + serialization, dépend de `core:designsystem` et de Navigation 3 (`navigation3-runtime`) ; ViewModel et lifecycle arrivent en P2.2) | idem + Compose UI Test |
| Application (`app`) | `blox.android.application` + `blox.android.application.compose` | idem + Compose UI Test (parcours entre features), `ui-test-manifest` en debug |
| Mesures (`benchmark`, `com.android.test`) | `blox.android.benchmark` | JUnit 5 branché à la main, règles Macrobenchmark par l'adaptateur `RuleExtension` (amendement de l'ADR 0009) |

- Les dépendances inter-modules utilisent les accesseurs typés : `implementation(projects.core.game)`.
- Un module qui publie des doublures de test (`src/testFixtures` : `core:game`, `core:audio`,
  `core:data`, `core:designsystem`, `core:board`) applique en plus `blox.test.fixtures`, dans n'importe quel ordre :
  `java-test-fixtures` pour un module Kotlin pur, les testFixtures d'AGP pour une bibliothèque Android.
  Les autres modules les consomment avec `testImplementation(testFixtures(projects.core.data))` (ou `androidTestImplementation(...)` pour les tests instrumentés). Les
  faux dépôts de `core:data` passent le même contrat que les vrais (`HighScoreRepositoryContract`,
  `UserPreferencesRepositoryContract`), et `MainDispatcherExtension` y est partagée. De même, le faux
  lecteur de `core:audio` passe le contrat du séquenceur (`MusicPlayerContract`).
- Un module dont le code reprend des jetons de `docs/design-system/tokens.json` (`core:designsystem`, `core:board`) applique
  en plus `blox.design.tokens` (ADR 0019) : ses tests JVM reçoivent le chemin du fichier par la propriété système
  `blox.designTokens` (entrée à chemin relatif, clé de cache indépendante du worktree) et le lisent par
  `DesignTokens`, lecteur publié dans les doublures de test de `core:designsystem` (`kotlinx-serialization-json` y
  est en `testFixturesImplementation`). `PaletteTokensTest` compare chaque jeton à la palette, `PieceTokensTest`
  (`core:board`) les couleurs des pièces.
- Les modules Kotlin purs compilent avec `explicitApi()` strict : ce qui est `public` est un
  choix, pas un oubli.
- **JUnit 5 partout**, tests unitaires et instrumentés (ADR 0009). Jamais de `junit:junit`
  ni de `ui-test-junit4` ; pour Compose, `createComposeExtension()` avec
  `@RegisterExtension`.

### Règles de dépendance (à ne pas enfreindre)

- `core:game` et `core:music` : **aucune dépendance** (ni Android, ni coroutines). Une nouvelle
  dépendance demande un ADR.
- `core:locksmith` : seulement `core:game` et `core:music`, en `api` (ADR 0025), ni
  Android ni coroutines. Il suit le TDD strict comme eux.
- `core:garden` : seulement `core:game` et `core:music`, en `api` (ADR 0027), ni Android ni
  coroutines. Il suit le TDD strict comme eux.
- Une feature **ne dépend jamais** d'une autre feature. La navigation entre features est
  assemblée dans `app`, seul module qui les connaît toutes : une feature expose une clé `NavKey` et une
  extension d'`EntryProviderScope` ; `app` possède la pile et l'affiche avec `NavDisplay` (ADR 0011). La
  racine de la pile est le choix du jeu (`CartridgesRoute`, ADR 0022) ; `changeGame`, fonction pure de `app`, y
  revient depuis l'accueil d'un jeu.
- Aucune couleur ni son dans le domaine. `core:designsystem` ne dépend pas du domaine :
  l'association « `PieceType` → style de cellule » vit dans `core:board` (ADR 0023), qui dépend du
  domaine (`core:game`, `core:music`) et de `core:designsystem`, et que `feature:game` utilise ; de même, les noms de
  notes et d’accords lus par TalkBack sont dans `core:designsystem` (`noteText`, `chordText`) et leur lien au domaine
  (`chordTellingOf`) dans `core:board` (P10.19).
- Toute configuration Gradle partagée va dans `build-logic`, toute version dans
  `gradle/libs.versions.toml`. Deux exceptions : les plugins de `settings.gradle.kts`, résolus
  avant le catalogue (ADR 0010), et le bloc `dependencyAnalysis {}` du `build.gradle.kts` racine, que
  le plugin impose à cet endroit.

### Couches dans une feature

`GameScreen` (Compose, sans logique) → actions → `GameViewModel` → `Input` → `GameEngine`
(`core:game`) → `GameState` + événements → `GameUiState`. Pas de couche de use cases : le
moteur *est* le domaine. Les repositories (`core:data`) exposent des `Flow`. Injection
manuelle via un `AppContainer` dans `app` et des `ViewModelProvider.Factory` (ADR 0007) ; la
feature définit sa fabrique, `app` la construit et la passe à l'entrée de navigation (ADR 0013).

### Boucle de jeu et temps

Simulation à **pas fixe** : 1 tick = 1 image = 1/60 s (ADR 0006). Le domaine ne connaît que
les ticks, jamais de millisecondes ni d'horloge ; l'aléatoire est injecté (`Randomizer`). DAS,
délai d'apparition, gravité sont comptés en ticks, donc testables sans appareil. L'horloge
d'images appartient à l'écran (`withFrameNanos` dans un `LaunchedEffect`), jamais au ViewModel :
`viewModelScope` n'a pas de `MonotonicFrameClock` (ADR 0012). Une classe pure `FrameTicks` (`core:designsystem`) rend le nombre
de ticks dus ; le ViewModel les applique au moteur après les entrées.

### Organisation prévue de `core:game`

| Paquet | Contenu |
|---|---|
| `ch.magori.blox.core.game.model` | Value objects : `Position` (`minus`, `neighbours()`, `firstInReadingOrder()`), `PieceType`, `Orientation` (4 décalages autour du pivot), `Rotation`, `Piece`, `Board` (10×20 immuable, `Board.parse(ascii)` pour les tests) ; `AsciiGrid.parse(ascii)`, grille texte partagée par `Padlock.parse` et `Garden.parse` (P10.16) |
| `ch.magori.blox.core.game.rules` | Politiques interchangeables : `RotationSystem`, `Randomizer`, `GravityTable`, `ScoringRule`, `LevelProgression`, avec une implémentation `Classic*` de chacune ; `RotationSystem.pieceCovering(cells, types)`, la pièce qui couvre exactement des cases (Serrurier, Jardin) |
| `ch.magori.blox.core.game.engine` | `GameState`, `GameEngine` (`tick()`, `apply(Input)`), événements (`LinesCleared`, `PieceLocked`, `GameOver`, `StackDanger`…) |

Le mode Moderne (P6, règles dans [docs/regles-mode-moderne.md](docs/regles-mode-moderne.md))
s'ajoute au même moteur, dont les capacités sont déclarées dans `Rules` (réserve, chute
instantanée, pièces suivantes). Les comportements codés en dur deviennent des politiques
(`ControlScheme`, `LockDelay`, `LineClearDelay`, `TopOutRule`), extraites d'abord sous les tests
du Classique, qui doit rester vert (ADR 0015).

## Conventions de code

- **Code en anglais** (noms, commentaires techniques), **documentation en français**.
- Langage du domaine : `Board`, `Piece`, `PieceType`, `Orientation` (une forme), `Rotation`
  (un sens ou un état de rotation), `Position`, `GameState`, `GameEngine`, `Tick`, `Input`,
  `Level`, `Lines`, `LineClear` (lignes effacées d'un coup), `Points` (le score de la partie ;
  `Score` est la partition de `core:music`), `LinesCleared`, `StackDanger`. Mode Moderne :
  `Hold` (réserve), `HardDrop` (chute instantanée), `Ghost` (pièce fantôme), `LockDelay` (délai
  de verrouillage), `Bag` (sac de 7), `TSpin`, `Combo`, `BackToBack` (enchaînement de coups
  difficiles). Le Serrurier (`core:locksmith`) : `Padlock` (la serrure ; *lock* est le
  verrouillage du Classique), les pièces posées étant des `Piece`. La Graine du jour :
  `StableRandom` (générateur stable, SplitMix64, dont la suite est un contrat entre versions ;
  `ClassicRandomizer.stable(seed)`). Le Jardin musical (`core:garden`) : `Garden` (la grille de
  16 × 14 et les pièces qui y sont posées), `GardenMood` (une ambiance : gamme, accords, tempo).
  Garder ces noms.
- **Immuabilité** dans le domaine : `data class` et `val`, opérations qui renvoient un nouvel
  état. Pas de `var` public.
- Préférer les types dédiés aux primitives (`Level`, `Lines` plutôt que `Int`) quand ils portent
  une règle.
- **Duplication** : refactoriser dès la deuxième occurrence (« règle de 2 »).
- Style : `.editorconfig` (4 espaces, 120 colonnes, virgule finale autorisée en Kotlin).
- Tests : noms en phrases (`` `a full line is cleared and rows above fall down` ``), structure
  Given / When / Then (en `src/androidTest`, la phrase va dans `@DisplayName` : le dex refuse
  les espaces dans les noms de méthodes sous l'API 30), plateaux décrits en ASCII dans les tests
  quand c'est plus lisible :

  ```kotlin
  val board = Board.parse(
      """
      ..........
      IIIIOO.SLL
      """
  )
  ```

## Droits

- Ne jamais utiliser le nom, le logo, les visuels ou les sons de la marque du jeu d'origine.
- Ne jamais retranscrire ni intégrer les arrangements musicaux de 1989. Seules les mélodies
  du domaine public, arrangées pour BloX, sont acceptées (voir `docs/musique.md`).

## Définition de terminé (après chaque fonctionnalité)

Une tâche de [SUIVI.md](SUIVI.md) n'est terminée qu'après ces étapes, **dans cet ordre**.
La commande `/fin-de-tache` les déroule.

1. **Tests verts.**
   `./gradlew ktlintCheck buildHealth :core:game:test :core:music:test :core:locksmith:test :core:garden:test testDebugUnitTest`,
   `./gradlew -p build-logic ktlintCheck`, plus
   `./gradlew lintDebug assembleDebug assembleRelease assembleDebugAndroidTest` si un module Android
   ou une dépendance a changé ; si une interface a
   changé, `./gradlew validateDebugScreenshotTest` et, sur un émulateur ou un téléphone,
   `./gradlew connectedDebugAndroidTest` (pas encore en CI ; les captures, elles, y sont validées).
   Aucun test désactivé sans raison écrite dans le test et dans `JOURNAL.md`.
2. **Revue de code indépendante.** La revue (`/revue`) est faite par un sous-agent qui **n'a pas
   vu l'implémentation** : il ne reçoit que le diff de la tâche, ce fichier et la documentation.
   Chaque remarque retenue est corrigée **test d'abord** (un test qui montre le défaut, puis la
   correction), et on relance l'étape 1. Les remarques écartées sont notées dans `JOURNAL.md`,
   avec la raison.
3. **Spécification à jour** (`docs/regles-mode-classique.md`, `docs/musique.md`,
   `docs/themes.md`…). Pour chaque règle touchée :
   - la règle telle qu'elle est implémentée ;
   - le ou les tests qui la prouvent, sous la forme `ClasseDeTest › nom du test` ;
   - les vues de maquette concernées (`V1`, `V2`…) ;
   - le marqueur **[à vérifier]** retiré si la valeur a été confirmée, **avec la source** ;
   - `scripts/verifier-tracabilite.sh` vert.
4. **Maquette à jour**, si un écran a changé. Les vues sont numérotées dans
   [docs/maquette.md](docs/maquette.md) ; en cas de renumérotation, mettre à jour toutes les
   références (spécification, SUIVI). La maquette Claude Design (planche et prototype) se
   modifie depuis claude.ai, pas depuis Claude Code : ajouter alors une tâche
   « Mettre à jour la maquette : … » dans SUIVI.md.
5. **Décision consignée.** Un ADR dans `docs/adr/` si une décision structurante a été prise.
6. **SUIVI.md et JOURNAL.md à jour** : dans `SUIVI.md`, cocher la tâche, ajouter les tâches
   découvertes, avec un identifiant, dans la bonne phase ; dans `JOURNAL.md`, ajouter une ligne (ce qui a été fait,
   décisions, remarques de revue écartées, points ouverts). **Ne pas lancer `scripts/avancement.sh` dans une
   branche** : deux branches en parallèle y écriraient deux chiffres en conflit. L’avancement se recalcule sur
   `main`, dans un commit à lui, après chaque fusion (`📊 docs(suivi): avancement`). `JOURNAL.md` se fusionne en
   « union » (`.gitattributes`) : les lignes ajoutées par deux branches sont gardées toutes les deux.
7. **Commit, puis push.**
   - Branche de la tâche : `feat/P1.3-collisions`, `fix/…`, `docs/…`, `build/…`, créée depuis `main`.
   - Message au format **gitmoji + [Conventional Commits](https://www.conventionalcommits.org/fr/)**
     (`✨ feat(core-game): …`), corps qui **explique pourquoi**, référence de la tâche
     (`Tâche : P1.3`). Le skill `/commit` porte la table des gitmoji et rédige le message.
   - Push, attendre que la CI soit verte, puis fusionner dans `main`.
   - Sur `main` fusionné : `scripts/avancement.sh`, puis un commit `📊 docs(suivi): avancement` s’il a changé
     quelque chose (après plusieurs fusions d’ffilée, une seule fois à la fin).
