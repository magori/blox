---
name: commit
description: Rédiger et créer un commit BloX au format gitmoji + Conventional Commits, avec un corps qui explique le pourquoi et la référence de la tâche. À utiliser pour tout commit dans ce dépôt, y compris à l'étape 7 de /fin-de-tache.
---

Crée un commit pour les changements en cours ($ARGUMENTS : identifiant de tâche `P1.3` ou
indication sur le contenu, facultatif).

## Avant d'écrire le message

1. `git status` et `git diff` : comprends ce qui a changé. Ne commite que ce qui relève de la
   tâche ; liste explicitement les fichiers à indexer plutôt que `git add -A`.
2. Vérifie la branche. Jamais de commit directement sur `main` : si on y est, crée la branche
   de la tâche depuis `main` (`feat/P1.3-collisions`, `fix/…`, `docs/…`, `build/…`).
3. Si les changements couvrent plusieurs sujets indépendants, propose plusieurs commits.

## Format du message

```
<gitmoji> <type>(<portée>): <sujet>

<corps : pourquoi ce changement, pas ce qu'il fait — le diff le montre déjà>

Tâche : P1.3
```

- **Gitmoji** : le caractère Unicode (`✨`), jamais le code (`:sparkles:`). Un seul, en tête.
- **Type** : [Conventional Commits](https://www.conventionalcommits.org/fr/).
- **Portée** : le module touché, sans deux-points ni préfixe `:` — `core-game`, `core-music`,
  `core-audio`, `core-data`, `designsystem`, `feature-home`, `feature-game`, `app`,
  `build-logic`. Omise pour `docs`, `ci`, ou un changement transverse.
- **Sujet** : en français, minuscule initiale, sans point final, 72 caractères au plus.
- **Corps** : phrases courtes, 72 colonnes. Il explique la motivation, les choix faits et les
  alternatives écartées. Il cite l'ADR si une décision a été consignée (`Voir ADR 0009`).
- **Pied** : `Tâche : Px.y` quand le commit relève d'une tâche de `SUIVI.md`, puis la ligne
  d'attribution si l'environnement en demande une.

### Correspondance type → gitmoji

| Type | Gitmoji | Quand |
|---|---|---|
| `feat` | ✨ | Nouvelle règle, nouvel écran, nouvelle capacité |
| `fix` | 🐛 | Correction d'un comportement erroné |
| `test` | ✅ | Tests seuls (ajout, correction, réorganisation) |
| `refactor` | ♻️ | Restructuration sans changement de comportement |
| `perf` | ⚡️ | Amélioration de performance |
| `style` | 🎨 | Format, nommage, organisation du code |
| `docs` | 📝 | Documentation, spécification, ADR, SUIVI |
| `build` | 🔧 | Gradle, plugins de convention, catalogue, configuration |
| `build` | ⬆️ | Montée de version d'une dépendance ou d'un outil |
| `ci` | 👷 | Workflow GitHub Actions |
| `chore` | 🔨 | Scripts, outillage, tâches de maintenance |
| `refactor` | 🔥 | Suppression de code ou de fichiers |
| `feat` | 🎉 | Premier commit, naissance d'un module |

Si deux lignes conviennent, choisis celle qui décrit la **motivation** du commit.

### Exemples

```
✨ feat(core-game): effacement des lignes complètes

Une pièce verrouillée peut compléter de une à quatre lignes, contiguës ou
non. Les lignes supérieures descendent d'autant ; le plateau reste
immuable, chaque effacement renvoie un nouveau Board.

Tâche : P1.7
```

```
🔧 build(build-logic): JUnit 5 pour tous les tests, modules Android compris

Un seul style de test dans tout le dépôt. Le plugin
de.mannodermaus.android-junit est appliqué par les plugins de convention,
JUnit 4 disparaît du catalogue. Voir ADR 0009.

Tâche : P0.8
```

## Ensuite

1. Montre le message complet avant de commiter.
2. Commite avec un heredoc pour préserver les sauts de ligne. Jamais `--no-verify`.
3. Affiche `git log --oneline -1` pour confirmer, et ne pousse que si on te le demande.
