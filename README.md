# BloX

Jeu de blocs pour Android, à usage personnel. Premier mode : **Classique**, fidèle aux règles
de la version NES de 1989. Un mode **Moderne** (réserve, pièce fantôme, sac de 7) viendra
ensuite. Deux thèmes : **Original** (vert sauge pastel) et **Couleurs**.

> **Statut** : structure du projet, documentation et chaîne de build en place, aucune règle de jeu encore
> implémentée. L'avancement est suivi dans [SUIVI.md](SUIVI.md).

## Démarrer

Prérequis : Android Studio récent (avec un JDK 17 ou plus), SDK Android 37.

```bash
./gradlew :core:game:test      # tests du domaine, sur la JVM
./gradlew testDebugUnitTest    # tests unitaires de tous les modules
./gradlew assembleDebug        # APK de debug
./gradlew installDebug         # installation sur un appareil connecté
```

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

Kotlin 2.4 · Jetpack Compose (Material 3) · Navigation Compose typée · DataStore ·
Coroutines · JUnit 5 + AssertJ (domaine) · JUnit 4 (modules Android) · Gradle 9.8 (ADR [0010](docs/adr/0010-kotlin-integre-agp9.md)),
catalogue de versions et plugins de convention · `minSdk` 26, `compileSdk` 37, `targetSdk` 36.

## Droits

Projet personnel, sans lien avec les détenteurs de la marque du jeu d'origine : aucun nom,
visuel ni son de la marque n'est utilisé. Les musiques intégrées sont des mélodies du domaine
public, arrangées pour BloX (voir [docs/musique.md](docs/musique.md)).
