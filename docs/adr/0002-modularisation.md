# 0002 — Modularisation `app` / `core` / `feature` et plugins de convention

- **Statut** : Acceptée ; amendée par [0025](0025-le-serrurier.md) et [0027](0027-le-jardin-musical.md)
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
- **Amendement (P4.22c, 2026-10-20).** Module `feature:musics` pour la liste « Mes musiques »,
  sur le patron des features existantes : un autre écran, avec son ViewModel, que l'accueil et
  l'éditeur ouvrent sans le connaître. Le mettre dans `feature:composer` aurait mêlé deux écrans
  dans un module ; une feature ne dépendant d'aucune autre, la liste ouvre l'éditeur par `app`
  (ADR 0011), qui passe les rappels à son entrée. `feature:home`, `feature:composer` et
  `feature:musics` partagent le même lecteur de musique, construit une fois dans `AppContainer` :
  un seul aperçu joue à la fois dans toute l'app.
- **Amendement (P4.8, 2026-10-20).** Les effets sonores n'ont pas de module : leur description
  (`SoundEffect`, donnée pure) va dans `core:music`, leur rendu et leur lecture (`SoundEffectPlayer`,
  pistes `AudioTrack` statiques, sans focus audio, `USAGE_GAME`) dans `core:audio`, qui a déjà le
  synthétiseur et `AudioTrack`, et dont la feature de jeu dépend déjà. À l'inverse de `core:haptics`,
  ils passent par le même service système que la musique. Le moteur gagne l'événement
  `PieceRotated`, sans dépendance : il dit ce qui s'est passé, la feature en tire un son.
- **Amendement (ADR 0027, 2026-10-26).** Modules `core:garden` (Kotlin pur, `api(core:game)` et
  `api(core:music)`) et `feature:garden` pour le Jardin musical, sur le modèle du Serrurier.
- **Amendement (P9.5, 2026-10-27).** Module `feature:daily` pour l'écran d'entrée de la Graine du
  jour (V38), plutôt qu'un écran de plus dans `feature:game` (décision du développeur) :
  `feature:game` reste l'écran de jeu, de pause et de fin de partie, pour toutes ses variantes
  (ADR 0026), et l'entrée a son propre ViewModel, ses résultats et son cycle de vie. Le module
  ne dépend d'aucune autre feature : `core:data` (`DailyChallengeRepository`), `core:game`
  (`ClassicRandomizer.stable`, pour les 7 premières pièces), `core:board` (leur dessin) et
  `core:designsystem`. Il expose `DailyRoute` et `dailyEntry` ; `app` l'assemble (ADR 0011),
  lui donne la date du jour, la graine (`DailySeed`) et le niveau imposé (ADR 0007, 0013), et
  décide où mènent Jouer (la partie du jour de `feature:game`, à la date affichée), Paramètres,
  Changer de jeu et Copier mon score.
