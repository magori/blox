# CLAUDE.md — conventions de travail sur BloX

Ce fichier est lu par Claude Code au début de chaque session. Il décrit comment on travaille
sur ce dépôt.

## Avant de commencer une tâche

1. Lire [SUIVI.md](SUIVI.md) : prendre la **prochaine tâche non cochée** de la phase en cours,
   sauf demande contraire.
2. Lire la documentation qui la concerne (règles, architecture, thèmes, musique).
3. Si une règle est marquée **[à vérifier]** dans `docs/regles-mode-classique.md`, le signaler
   et proposer la valeur à confirmer **avant** d'écrire le test.

## TDD strict

Le développeur pratique le TDD de façon stricte. Pour tout code de `core:game` et `core:music`,
et pour toute logique testable ailleurs :

1. **Rouge** : écrire un seul test qui échoue, pour la plus petite règle suivante. Le lancer
   et montrer l'échec.
2. **Vert** : écrire le code minimal pour le faire passer. Relancer.
3. **Refactoring** : nettoyer code **et** tests, tests au vert à chaque étape.

Ne jamais écrire de code de production sans test qui l'exige. Proposer la liste des cas de
test d'une tâche avant de commencer, pour validation.

## Conventions de code

- **Code en anglais** (noms, commentaires techniques), **documentation en français**.
- Langage du domaine : `Board`, `Piece`, `PieceType`, `Rotation`, `Position`, `GameState`,
  `GameEngine`, `Tick`, `Input`, `Level`, `Score`, `LinesCleared`. Garder ces noms.
- **Immuabilité** dans le domaine : `data class` et `val`, opérations qui renvoient un nouvel
  état. Pas de `var` public.
- Préférer les types dédiés aux primitives (`Level`, `Lines` plutôt que `Int`) quand ils portent
  une règle.
- Politiques interchangeables par interfaces (`RotationSystem`, `Randomizer`…) : le mode Moderne
  doit pouvoir s'ajouter sans modifier le moteur.
- **Duplication** : refactoriser dès la deuxième occurrence (« règle de 2 »).
- Tests : noms en phrases (`` `a full line is cleared and rows above fall down` ``), structure
  Given / When / Then, plateaux décrits en ASCII dans les tests quand c'est plus lisible :

  ```kotlin
  val board = Board.parse(
      """
      ..........
      IIIIOO.SLL
      """
  )
  ```

## Règles d'architecture (à ne pas enfreindre)

Détail dans [docs/architecture.md](docs/architecture.md).

- `core:game` et `core:music` : **aucune dépendance** (ni Android, ni coroutines). Une nouvelle
  dépendance demande un ADR.
- Une feature **ne dépend jamais** d'une autre feature.
- Aucune couleur ni son dans le domaine.
- Toute configuration Gradle partagée va dans `build-logic`, toute version dans
  `gradle/libs.versions.toml`.

## Droits

- Ne jamais utiliser le nom, le logo, les visuels ou les sons de la marque du jeu d'origine.
- Ne jamais retranscrire ni intégrer les arrangements musicaux de 1989. Seules les mélodies
  du domaine public, arrangées pour BloX, sont acceptées (voir `docs/musique.md`).

## Commandes

```bash
./gradlew :core:game:test       # domaine
./gradlew :core:music:test      # partitions
./gradlew testDebugUnitTest     # tous les tests unitaires
./gradlew lintDebug             # lint Android
./gradlew assembleDebug         # APK
```

## Après chaque tâche

1. Tous les tests au vert.
2. **Mettre à jour [SUIVI.md](SUIVI.md)** : cocher la tâche, ajouter une ligne au journal
   (date, ce qui a été fait, décision prise, point ouvert).
3. Si une décision structurante a été prise, ajouter un ADR dans `docs/adr/`.
4. Commit en [Conventional Commits](https://www.conventionalcommits.org/fr/) :
   `feat(core-game): clear full lines`, `test(core-game): …`, `docs: …`, `build: …`.
