# 0027 — Le Jardin musical : `core:garden` et `feature:garden`

- **Statut** : Acceptée ; amende [0002](0002-modularisation.md) et [0022](0022-choix-du-jeu-a-la-racine.md)
- **Date** : 2026-10-26

## Contexte

Le Jardin musical ([règles](../jardin-musical.md)) est un bac à sable : on pose les sept pièces
sur une grille de notes (16 croches × 14 hauteurs), qui se joue en boucle. Comme le Serrurier
(ADR 0025), il réutilise les pièces et les rotations de `core:game` et les gammes, accords et
partitions de `core:music`, deux modules qui ne dépendent de rien. Une feature ne dépend d'aucune
autre. Le développeur a décidé (2026-10-26) d'une cartouche pour le Jardin, d'un jardin gardé
automatiquement sans export vers « Mes musiques » en v1, et d'une pièce debout jouée en arpège
montant dans la croche.

Options écartées pour le domaine : le mettre dans `core:game` (qui ne doit pas dépendre de
`core:music`), dans `core:music` (qui ne doit pas dépendre de `core:game`), dans
`core:locksmith` (deux jeux mêlés), ou dans `feature:garden` (logique testée en Android, mêlée à
l'écran).

## Décision

- **`core:garden`**, Kotlin pur (`blox.jvm.library`, `explicitApi`), avec `api(core:game)` et
  `api(core:music)` : son API expose `Piece`, `PieceType`, `Position`, `Orientation`, `Score`,
  `Scale` et `Progression`. Les pièces posées sont des `Piece` de `core:game`, sans type parallèle.
  Il porte le jardin immuable (`Garden` : poser, refuser, reprendre, effacer, changer d'ambiance — le jardin porte son ambiance, amendement P10.5), les ambiances
  (`GardenMood` : gamme, accords, tempo), la partition (`Garden.toScore`) et le dé
  (`GardenComposer`). Ni son joué, ni couleur, ni temps réel.
- **Le dé est une fonction pure de la graine**, sur le générateur stable `StableRandom` (P1.20,
  ADR 0026) : il n'ajoute aucun état modifiable au domaine (ADR 0014).
- **`feature:garden`** (`blox.android.feature`) : l'écran, sa clé `GardenRoute`, son ViewModel et
  sa fabrique (ADR 0013). Le jardin est gardé dans un dépôt de `core:data` (DataStore, fichier à
  part, format versionné), lu à l'ouverture et écrit à chaque changement.
- **Son** : la boucle passe par le lecteur de musique (`MusicPlayer`, un seul aperçu à la fois),
  qui gagne `Player.switchTo` (ADR 0026) pour changer de partition à la mesure ; l'aperçu d'une
  pièce passe par `SoundEffectPlayer`, sur un **ensemble fini d'effets, jamais libérés** :
  l'union des hauteurs des 4 ambiances, toutes sur Do (environ 30). `SoundEffectPlayer` n'offre
  pas de libération, et on ne lui en ajoute pas.
- **Partition** : une voix par ligne (14 voix de mélodie) et la basse, à volume fixe, car une
  `Voice` est monophonique et à volume constant ; `core:music` ne change pas.
- **Partagé** : `PieceTile` (les tuiles du bac, sorties du Serrurier) va dans `core:board` ; la
  tête de lecture de l'éditeur, `PlayHead`, devient `LoopPlayHead` dans `core:designsystem`
  (paquet `frame`). Règle de 2.
- **Cartouches** : une entrée « Jardin musical » dans `feature:cartridges`, que `app` traduit en
  `GardenRoute`. Écran en Couleurs v2 seulement, jour et nuit.

## Conséquences

- Deux modules de plus ; le glossaire gagne `Garden` et `GardenMood`. La CI et la définition de
  terminé lancent aussi `:core:garden:test`.
- `core:garden` et `core:locksmith` sont les deux modules du domaine qui dépendent de `core:game`
  et `core:music`.
- Le mélange peut faire sonner 14 notes ensemble : le volume fixe des voix doit borner
  l'amplitude, avec un test sur la crête.
- Les effets de l'aperçu ajoutent des pistes statiques ; Android limite le nombre d'`AudioTrack`
  par processus (ADR 0025) : l'ensemble doit rester fini et être mesuré sur un téléphone.
- La cartouche attend le choix du jeu à cinq cartouches (ADR 0026).
- L'export d'un jardin vers « Mes musiques » demandera de le traduire au format des musiques
  enregistrées (ADR 0017) : hors v1.

## Amendement (P10.11, 2026-10-28) : le format du jardin gardé

Le jardin gardé est un fichier binaire à lui, `garden.bin`, dans un format de BloX versionné
(version 1), écrit sur Okio comme les musiques (ADR 0017) : « BLXG », la version, le nom de
l'ambiance, le nombre de pièces (56 au plus), puis chaque pièce par le nom de son type, son pivot
et ses 4 cases en décalages depuis le pivot, triées ligne par ligne. Les cases plutôt qu'un numéro
d'orientation : le fichier ne dépend pas de l'ordre des orientations de `ClassicRotationSystem`, et
une forme étrangère à son type se détecte au lieu de donner une autre forme en silence ; un test
d'octets fige le format. À la lecture, chaque pièce est reposée par `Garden.place` ; tout ce
qu'aucun jardin ne peut contenir rend le fichier corrompu, copié dans `garden.bin.corrupt` puis
remplacé par un jardin vide et calme. Une version plus récente est gardée telle quelle, sans
écriture, comme les musiques. `core:data` dépend de `core:garden` en `api` (le `Garden` est dans
son API publique, sans type parallèle) ; le fichier entre dans la sauvegarde Android (décision du
développeur).
