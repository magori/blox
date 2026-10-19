# 0018 — Une musique : partition ou fichier

- **Statut** : Acceptée
- **Date** : 2026-10-19

## Contexte

Jusqu'à P4.6, toute musique était une partition (`Score`) jouée par le séquenceur
(`ChiptuneSequencer`) derrière le focus audio. « Mon fichier » ajoute un fichier audio de
l'appareil, désigné par une URI de contenu. Le jeu et l'aperçu de l'accueil doivent le traiter
comme une partition : jeton de lecture, pause et reprise, état `isPlaying`, focus audio, sans
dupliquer leur logique.

## Décision

- **`Music` scellé dans `core:music`** : `Score` ou `MusicFile(uri)`, l'URI gardée en texte pour
  que le module reste en Kotlin pur.
- **`Player<in M : Music>`** dans `core:audio`, avec `typealias MusicPlayer = Player<Music>` ;
  `ChiptuneSequencer` est un `Player<Score>`. La contravariance permet d'écrire le contrat
  `MusicPlayerContract` une fois (sur des partitions) et de le jouer sur le séquenceur, le faux,
  le lecteur à focus et le commutateur.
- **`MusicSwitch`** joue une musique à la fois : les partitions par le séquenceur, les fichiers
  par `AudioFilePlayer` (sur `MediaPlayer`, sans nouvelle dépendance) ; lancer ou reprendre l'une
  arrête l'autre ; un fichier qui ne s'ouvre pas ou ne se décode pas joue une partition de repli
  (Korobeïniki), à la lecture comme à la reprise.
- Le tempo (accélération du danger) ne s'applique qu'aux partitions ; un fichier garde sa vitesse
  (`PlaybackParams.setSpeed` changerait la hauteur ou étirerait le son).
- L'URI vit dans un DataStore à part, exclu de la sauvegarde (P5.28).

## Conséquences

- Le contrat commun ne couvre que les partitions ; les fichiers ont leurs tests propres
  (`AudioFilePlayerTest`, `MusicSwitchTest`) et un test instrumenté de `MediaPlayer`.
- Un fichier qui échoue après avoir commencé à jouer se tait au lieu de basculer sur le repli.
- Une autre sorte de musique (par exemple un flux) s'ajouterait comme un cas de `Music` et un
  lecteur de plus dans `MusicSwitch`.
