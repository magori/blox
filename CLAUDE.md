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

## Définition de terminé (après chaque fonctionnalité)

Une tâche de [SUIVI.md](SUIVI.md) n'est terminée qu'après ces étapes, **dans cet ordre**.
La commande `/fin-de-tache` les déroule.

1. **Tests verts.**
   `./gradlew :core:game:test :core:music:test testDebugUnitTest`, plus
   `./gradlew lintDebug assembleDebug` si un module Android a changé.
   Aucun test désactivé sans raison écrite dans le test et dans le journal.
2. **Revue de code indépendante.** La revue (`/revue`) est faite par un sous-agent qui **n'a pas
   vu l'implémentation** : il ne reçoit que le diff de la tâche, ce fichier et la documentation.
   Chaque remarque retenue est corrigée **test d'abord** (un test qui montre le défaut, puis la
   correction), et on relance l'étape 1. Les remarques écartées sont notées au journal, avec la
   raison.
3. **Spécification à jour** (`docs/regles-mode-classique.md`, `docs/musique.md`,
   `docs/themes.md`…). Pour chaque règle touchée :
   - la règle telle qu'elle est implémentée ;
   - le ou les tests qui la prouvent, sous la forme `ClasseDeTest › nom du test` ;
   - les vues de maquette concernées (`V1`, `V2`…) ;
   - le marqueur **[à vérifier]** retiré si la valeur a été confirmée, **avec la source**.
4. **Maquette à jour**, si un écran a changé. Les vues sont numérotées dans
   [docs/maquette.md](docs/maquette.md) ; en cas de renumérotation, mettre à jour toutes les
   références (spécification, SUIVI). La maquette Claude Design (planche et prototype) se
   modifie depuis claude.ai, pas depuis Claude Code : ajouter alors une tâche
   « Mettre à jour la maquette : … » dans SUIVI.md.
5. **Décision consignée.** Un ADR dans `docs/adr/` si une décision structurante a été prise.
6. **SUIVI.md à jour** : cocher la tâche ; ajouter les tâches découvertes, avec un identifiant,
   dans la bonne phase ; une ligne de journal (ce qui a été fait, décisions, remarques de revue
   écartées, points ouverts).
7. **Commit, puis push.**
   - Branche de la tâche : `feat/P1.3-collisions`, `fix/…`, `docs/…`, créée depuis `main`.
   - Message en [Conventional Commits](https://www.conventionalcommits.org/fr/)
     (`feat(core-game): …`, `test(core-game): …`, `docs: …`, `build: …`) dont le **corps explique
     pourquoi**, avec la référence de la tâche (`Tâche : P1.3`).
   - Push, attendre que la CI soit verte, puis fusionner dans `main`.
