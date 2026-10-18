# 0003 — Domaine en Kotlin pur, développé en TDD

- **Statut** : Acceptée ; le point « JUnit 4 côté Android » est remplacé par [0009](0009-junit5-partout.md) ; `explicitApi` passé en mode strict (P0.10)
- **Date** : 2026-10-18

## Contexte

Les règles d'un jeu de blocs sont nombreuses, précises et faciles à casser (rotation contre un
mur, effacement de plusieurs lignes, passage de niveau). Ce sont exactement les règles qu'on
veut verrouiller par des tests rapides et nombreux.

## Décision

- `core:game` et `core:music` sont des modules **Kotlin/JVM purs**, sans dépendance.
- Ils sont développés en **TDD strict** (rouge → vert → refactoring), avec **JUnit 5** et
  **AssertJ**.
- Le hasard et le temps sont **injectés** : générateur de pièces avec graine, temps compté en
  ticks.
- Les modules Android utilisent **JUnit 4** pour leurs tests unitaires, parce qu'AGP le prend
  en charge nativement, sans plugin tiers.

## Conséquences

- Les tests du domaine tournent en quelques secondes sur la JVM, sans émulateur.
- Deux versions de JUnit coexistent dans le projet. C'est un compromis assumé : si cela gêne,
  le plugin `de.mannodermaus.android-junit5` permettra d'unifier sur JUnit 5.
- Le mode `explicitApi` est activé en avertissement sur les modules purs.
