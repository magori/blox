# 0002 — Modularisation `app` / `core` / `feature` et plugins de convention

- **Statut** : Acceptée
- **Date** : 2026-10-18

## Contexte

Le guide de modularisation Android (et l'application de référence *Now in Android*)
recommande de séparer l'application en modules `app`, `core:*` et `feature:*`, avec une
configuration Gradle partagée par des plugins de convention. Pour un petit jeu, le risque
inverse existe : trop de modules pour trop peu de code.

## Décision

Huit modules, chacun avec une responsabilité nette (voir [architecture](../architecture.md)) :
`app`, `feature:home`, `feature:game`, `core:game`, `core:music`, `core:audio`, `core:data`,
`core:designsystem`.

La configuration commune vit dans le build inclus `build-logic` (plugins
`blox.android.application`, `blox.android.library`, `blox.android.feature`,
`blox.jvm.library`…). Les versions sont centralisées dans `gradle/libs.versions.toml`.
Les dépendances entre modules utilisent les accesseurs typés (`projects.core.game`).

## Conséquences

- Le domaine (`core:game`) est isolé physiquement : impossible d'y importer Android par erreur.
- La configuration n'est écrite qu'une fois ; un nouveau module tient en cinq lignes.
- Pas de module `core:domain` à use cases, ni `core:model` séparé, ni module `feature` pour la
  fin de partie : ce serait de la structure sans contenu. On en créera si le besoin apparaît
  (par exemple un `feature:settings`).
- Le mode Moderne n'aura pas de module dédié : ce sont d'autres implémentations des politiques
  de `core:game` (rotation, tirage, verrouillage).
- **Amendement (P5.2, 2026-10-19).** Module `core:haptics` (bibliothèque Android, doublures en
  testFixtures) pour les vibrations : un autre service système (`Vibrator`) et une autre
  permission (`VIBRATE`) que la musique, un réglage indépendant ; le garder hors de `core:audio`
  évite qu'un module qui ne fait que vibrer tire `AudioTrack` et le séquenceur. La traduction
  des événements du jeu en vibrations vit dans `feature:game` (`hapticFor`), comme le tempo de
  la musique : `core:haptics` ne connaît pas le domaine. (De même, `feature:composer` et
  `benchmark` se sont ajoutés depuis, voir `docs/architecture.md`.)
