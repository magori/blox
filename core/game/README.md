# core:game — domaine du jeu

Module **Kotlin pur** : les règles et le moteur de BloX. Aucune dépendance Android,
aucune coroutine, aucune horloge réelle. Tout ce qui est aléatoire ou temporel est
injecté, ce qui rend chaque règle testable de façon déterministe.

Règles de référence : [docs/regles-mode-classique.md](../../docs/regles-mode-classique.md).

## Organisation prévue des paquets

| Paquet | Contenu |
|---|---|
| `ch.magori.blox.core.game.model` | Value objects : `Position`, `PieceType`, `Rotation`, `Piece`, `Board` (grille immuable 10×20). |
| `ch.magori.blox.core.game.rules` | Politiques interchangeables : `RotationSystem`, `Randomizer`, `GravityTable`, `DropTiming` (cadence de chute, ADR 0028), `ScoringRule`, `LevelProgression`. Une implémentation `Classic*` de chacune pour le mode Classique. |
| `ch.magori.blox.core.game.engine` | Machine à états de la partie : `GameState`, `GameEngine` (`tick()`, `apply(Input)`), événements émis (`LinesCleared`, `PieceLocked`, `GameOver`, `StackDanger`…). |

## Principes

- **Immuable par défaut** : un `tick` ou une action produit un nouvel état, ne modifie rien.
- **Aucune couleur, aucun son** : le domaine ne connaît que `PieceType.T`, jamais « violet ».
- **Le temps est discret** : une image = un `tick` (60 par seconde). Le moteur ne lit jamais
  d'horloge, c'est l'application qui l'appelle.
- **API explicite** : le mode `explicitApi` est activé en mode strict. Ce qui est public
  est un choix, pas un oubli.

## Tests

JUnit 5 + AssertJ. Lancer : `./gradlew :core:game:test`.
