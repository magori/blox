# 0025 — Le Serrurier : un second jeu, `core:locksmith` et `feature:locksmith`

- **Statut** : Acceptée ; amende [0002](0002-modularisation.md) et [0022](0022-choix-du-jeu-a-la-racine.md)
- **Date** : 2026-10-26

## Contexte

Un second jeu, le Serrurier ([règles](../regles-serrurier.md)), a été prototypé dans la maquette :
poser des pièces dans le trou d'une serrure, chaque pose jouant l'accord suivant d'une cadence.
Il réutilise les pièces et les rotations de `core:game` et les accords de `core:music`, deux
modules qui ne dépendent de rien. Une feature ne dépend d'aucune autre. L'ADR 0022 n'affichait
que la cartouche Classique ; le développeur accepte une seconde cartouche (2026-10-26).

Options écartées pour le domaine : le mettre dans `core:game` (qui ne doit pas dépendre de
`core:music`) ou dans `feature:locksmith` (logique testée en Android, mêlée à l'écran).

## Décision

- **`core:locksmith`**, Kotlin pur (`blox.jvm.library`, `explicitApi`), avec `api(core:game)` et
  `api(core:music)` : son API expose `Piece`, `PieceType`, `Position`, `Orientation` et
  `Progression`. Les pièces posées sont des `Piece` de `core:game`, sans type parallèle. Il porte
  la génération « composer, puis évaluer » (`PadlockGenerator`), la serrure (`Padlock`), la
  partie immuable (`PadlockGame`) et ses événements, l'indice et les suites d'accords. Ni son, ni
  couleur, ni temps.
- **La génération est une fonction pure de la graine** : elle construit son `Random(seed)`
  elle-même et n'en garde aucun. Le tirage des pièces du Classique reste le seul état modifiable
  du domaine (ADR 0014).
- **`feature:locksmith`** (`blox.android.feature`) : l'écran. Sa clé `LocksmithRoute(level, seed)`
  est remplacée par `app` à chaque serrure suivante, comme Rejouer (ADR 0011) ; le ViewModel ne
  tire jamais lui-même la serrure suivante. Après la mort du processus, la clé restaurée
  régénère la même serrure, sans pièce posée.
- **Son** : les accords sont des `SoundEffect` superposés (arpège et basse) joués par
  `SoundEffectPlayer`, sans nouvelle API audio. Leur description va dans `core:music`, comme
  `SoundEffects` ; la traduction « événement → effets » va dans la feature.
- **Cartouches** : une seconde entrée dans la liste de `feature:cartridges`, que `app` traduit en
  `LocksmithRoute(1, graine)`.

## Conséquences

- Deux modules de plus ; le glossaire gagne `Padlock`. La CI et la définition de terminé lancent
  aussi `:core:locksmith:test`.
- `core:board` doit s'ouvrir à une grille d'une autre taille que 10 × 20 (case partagée, jetons
  métal et trou dans `tokens.json`).
- La cartouche attend `feature:cartridges` (P5.72) ; d'ici là, l'écran se teste par sa route.
- La suite de `Random(seed)` n'est pas un contrat (ADR 0014) : afficher ou partager une graine
  demandera un générateur maison.
- Les effets superposés ajoutent une douzaine de pistes statiques (cinq accords en arpège et
  basse, la fanfare, le refus), jamais libérées par `SoundEffectTracks`. Android limite le nombre
  d'`AudioTrack` par processus : à mesurer avant P7.12, et revoir s'il le faut (un `SoundEffect`
  à plusieurs voix demanderait un nouvel ADR).

## Amendement (P7.28, 2026-10-28) : progression des serrures

Demande du développeur : garder l'avancement, poursuivre, rouvrir les anciennes serrures. La serrure *n* est la même
pour tous (ADR 0014, amendement P7.28), avec des paliers de difficulté jusqu'à 8 pièces. On garde la meilleure
ouverture de chaque serrure dans un fichier versionné de `core:data` (entiers seulement : `core:data` ne dépend pas de
`core:locksmith`), inclus dans la sauvegarde Android. `LocksmithRoute` ne porte plus que le niveau ; l'écran
« Serrures » (`PadlocksRoute`) vit dans `feature:locksmith`, et `app` assemble la navigation (choisir une serrure
remplace celle de la pile ; recommencer remet la pile sur la serrure 1).

Fait en P7.30 (2026-10-29) : `LocksmithRoute(level)` sans graine, serrure `Padlock.ofLevel(level)` ; l'ancien
`Padlock.generate(level, seed)` sur `kotlin.random.Random` est supprimé, et une clé sauvegardée avec sa graine par une
version antérieure redonne la serrure de son niveau.
