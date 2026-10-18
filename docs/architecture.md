# Architecture

BloX suit les [recommandations d'architecture Android](https://developer.android.com/topic/architecture)
(couches UI, domaine, données ; flux de données unidirectionnel) et le
[guide de modularisation](https://developer.android.com/topic/modularization)
(modules `app`, `core:*`, `feature:*`, plugins de convention). La taille reste
volontairement modeste : un module n'existe que s'il porte une responsabilité claire.

## Modules

```mermaid
graph TD
    app --> featureHome[feature:home]
    app --> featureGame[feature:game]
    app --> designsystem[core:designsystem]
    app --> data[core:data]
    app --> audio[core:audio]

    featureHome --> designsystem
    featureHome --> data
    featureHome --> audio

    featureGame --> designsystem
    featureGame --> game[core:game]
    featureGame --> data
    featureGame --> audio

    audio --> music[core:music]

    classDef pure fill:#dcebd5,stroke:#34503a,color:#34503a
    class game,music pure
```

Les modules en vert sont en **Kotlin pur** (aucune dépendance Android).

| Module | Type | Responsabilité |
|---|---|---|
| `app` | Application Android | Point d'entrée : `MainActivity`, graphe de navigation, assemblage des dépendances. |
| `feature:home` | Feature | Écran d'accueil : mode, thème, niveau de départ, musique. |
| `feature:game` | Feature | Écran de jeu, pause et fin de partie. Boucle de jeu, rendu du plateau, commandes. |
| `core:game` | Kotlin pur | **Domaine** : règles et moteur du jeu. Le cœur du projet, développé en TDD. |
| `core:music` | Kotlin pur | Modèle musical : partitions, notes, tempo. Pure donnée. |
| `core:audio` | Bibliothèque Android | Séquenceur chiptune (`AudioTrack`), lecture d'un fichier personnel. |
| `core:data` | Bibliothèque Android | Préférences et records (DataStore). |
| `core:designsystem` | Bibliothèque Compose | Thèmes BloX (jetons de couleur, typographie), composants partagés. |
| `build-logic` | Build inclus | Plugins de convention Gradle : la configuration n'est écrite qu'une fois. |

### Règles de dépendance

1. **Une feature ne dépend jamais d'une autre feature.** La navigation entre features est
   assemblée dans `app`, qui seul les connaît toutes.
2. **`core:game` et `core:music` ne dépendent de rien.** Ajouter une dépendance à ces modules
   demande un ADR.
3. **Le domaine ne connaît ni couleur, ni son, ni temps réel.** Un thème, un effet sonore,
   une horloge sont des détails de l'application.
4. **`core:designsystem` ne dépend pas du domaine.** L'association « type de pièce → style de
   cellule » vit dans `feature:game`.

## Couches dans une feature

Le schéma recommandé par Android, appliqué à l'écran de jeu :

```mermaid
graph LR
    subgraph feature:game
        Screen[GameScreen<br/>Compose] -- actions --> VM[GameViewModel]
        VM -- GameUiState --> Screen
        Loop[GameLoop<br/>60 Hz] -- tick --> VM
    end
    VM -- tick / Input --> Engine[GameEngine<br/>core:game]
    Engine -- GameState + événements --> VM
    VM --> Repo[HighScoreRepository<br/>core:data]
    VM --> Music[MusicPlayer<br/>core:audio]
```

- **UI (Compose)** : sans logique de jeu. Elle affiche un `GameUiState` et remonte des actions
  (`MoveLeft`, `RotateClockwise`…).
- **ViewModel** : traduit les actions en `Input` du domaine, appelle le moteur, convertit le
  `GameState` en `GameUiState`, réagit aux événements (record, musique qui accélère).
- **Domaine (`core:game`)** : joue le rôle de la couche domaine. Il n'y a pas de module
  `core:domain` séparé avec des use cases : ici le moteur *est* le domaine, et une couche de
  use cases qui ne ferait que déléguer serait de la cérémonie.
- **Données (`core:data`)** : repositories exposant des `Flow`.

## Boucle de jeu

Le jeu est simulé à **pas fixe** : 1 tick = 1 image = 1/60 s, comme sur la NES. La boucle
(une coroutine dans le scope du ViewModel, cadencée par `withFrameNanos`) accumule le temps
réel écoulé et appelle `tick()` autant de fois que nécessaire. Le domaine ne connaît que
les ticks, jamais les millisecondes : la vitesse ne dépend pas de la fréquence d'écran
(60, 90 ou 120 Hz). Voir l'[ADR 0006](adr/0006-boucle-de-jeu-a-pas-fixe.md).

Les commandes (maintien d'une flèche, répétition automatique) sont aussi comptées en ticks
dans le domaine. Ainsi le comportement du « DAS » de la NES est testable sans appareil.

## Injection de dépendances

Manuelle pour l'instant : un `AppContainer` dans `app` construit les repositories et le
lecteur de musique, et les ViewModels les reçoivent via une `ViewModelProvider.Factory`.
Hilt reste la recommandation officielle pour les projets plus gros : la migration est simple
tant que les constructeurs reçoivent leurs dépendances. Voir l'[ADR 0007](adr/0007-injection-manuelle.md).

## Tests

| Où | Outils | Ce qu'on teste |
|---|---|---|
| `core:game`, `core:music` | JUnit 5, AssertJ | Toutes les règles, en TDD. Le gros de la valeur est ici. |
| Modules Android (`src/test`) | JUnit 5, AssertJ, coroutines-test, Turbine | ViewModels, repositories, rendu des échantillons audio. |
| `src/androidTest` | JUnit 5 (extensions `android-junit`), Compose UI Test | Parcours d'écrans, quand l'UI sera stable. |

JUnit 5 partout, y compris dans les modules Android grâce au plugin
`de.mannodermaus.android-junit` appliqué par les plugins de convention. Voir les
[ADR 0003](adr/0003-domaine-en-kotlin-pur.md) et [0009](adr/0009-junit5-partout.md).
