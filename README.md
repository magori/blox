# BloX

Jeu de blocs pour Android, à usage personnel. Premier mode : **Classique**, fidèle aux règles
de la version NES de 1989. Un mode **Moderne** (réserve, pièce fantôme, sac de 7) viendra
ensuite. Deux thèmes : **Original** (vert sauge pastel) et **Couleurs**.

> **Statut** : structure du projet, documentation et chaîne de build en place, aucune règle de jeu encore
> implémentée. L'avancement est suivi dans [SUIVI.md](SUIVI.md).

## Démarrer

Prérequis : Android Studio récent, SDK Android 37. Le JDK 17 de compilation est téléchargé par la
toolchain Gradle s'il manque (résolveur foojay).

> **Note Windows** : `./gradlew` fonctionne sous Git Bash. Sous PowerShell ou CMD, utiliser `.\gradlew.bat`.

### Commandes principales

```bash
# Tests unitaires
./gradlew :core:game:test              # tests du domaine (JUnit 5, JVM)
./gradlew :core:music:test             # tests des partitions (JUnit 5, JVM)
./gradlew testDebugUnitTest            # tests unitaires de tous les modules

# Qualité et style de code
./gradlew ktlintCheck                  # vérification du style Kotlin (ktlintFormat pour corriger)
./gradlew -p build-logic ktlintCheck   # vérification du style des plugins de convention
./gradlew buildHealth                  # analyse des dépendances
./gradlew lintDebug                    # lint Android (inclut les règles Compose)

# Build et Déploiement
./gradlew assembleDebug                # compilation de l'APK de debug
./gradlew installDebug                 # installation sur un appareil connecté
```

### Lancer un test ciblé

```bash
# Test spécifique dans un module Kotlin pur
./gradlew :core:game:test --tests "ch.magori.blox.core.game.model.BoardTest"

# Test spécifique dans un module Android
./gradlew :feature:game:testDebugUnitTest --tests "ch.magori.blox.feature.game.GameViewModelTest"
```

### Tout exécuter en une seule commande

- **Dans Android Studio** (fenêtre *Run Anything* / configurations Gradle) :
  - Pour vérifier et compiler : `check assembleDebug`
  - Pour tout vérifier **et installer sur le mobile** : `check installDebug`
  - Pour juste **installer directement sur le mobile** : `installDebug`

- **Dans un Terminal** (Git Bash, PowerShell, CMD) :
  - Pour vérifier et compiler : `./gradlew check assembleDebug`
  - Pour tout vérifier **et installer sur le mobile** : `./gradlew check installDebug`
  - Pour juste **installer directement sur le mobile** : `./gradlew installDebug`

## Structure

```
blox/
├── app/                  Point d'entrée : activité, navigation, assemblage
├── feature/
│   ├── home/             Écran d'accueil
│   └── game/             Écran de jeu, pause, fin de partie
├── core/
│   ├── game/             Domaine du jeu (Kotlin pur, TDD)
│   ├── music/            Partitions (Kotlin pur)
│   ├── audio/            Séquenceur chiptune, lecture de fichier
│   ├── data/             Préférences et records (DataStore)
│   └── designsystem/     Thèmes et composants Compose
├── build-logic/          Plugins de convention Gradle
├── docs/                 Documentation et décisions (ADR)
└── SUIVI.md              Suivi de l'implémentation
```

## Documentation

| Document | Contenu |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Modules, couches, règles de dépendance, boucle de jeu, tests |
| [docs/regles-mode-classique.md](docs/regles-mode-classique.md) | Spécification des règles du mode Classique |
| [docs/themes.md](docs/themes.md) | Palettes et styles de pièces des deux thèmes |
| [docs/musique.md](docs/musique.md) | Séquenceur, partitions, droits |
| [docs/maquette.md](docs/maquette.md) | Description des écrans maquettés |
| [docs/adr/](docs/adr/README.md) | Décisions d'architecture |
| [CLAUDE.md](CLAUDE.md) | Conventions de travail pour Claude Code |

## Pile technique

Kotlin 2.4 · Jetpack Compose (Material 3) · Navigation 3 (ADR [0011](docs/adr/0011-navigation-3.md)) · DataStore ·
Coroutines · ktlint et analyse de dépendances en CI · JUnit 5 + AssertJ partout (ADR [0003](docs/adr/0003-domaine-en-kotlin-pur.md)
et [0009](docs/adr/0009-junit5-partout.md)) · Gradle 9.8 (ADR [0010](docs/adr/0010-kotlin-integre-agp9.md)),
catalogue de versions et plugins de convention · `minSdk` 26, `compileSdk` 37, `targetSdk` 36.

## Droits

Projet personnel, sans lien avec les détenteurs de la marque du jeu d'origine : aucun nom,
visuel ni son de la marque n'est utilisé. Les musiques intégrées sont des mélodies du domaine
public, arrangées pour BloX (voir [docs/musique.md](docs/musique.md)).
