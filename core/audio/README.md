# core:audio — lecture du son

Module **Android**. Transforme une partition de `core:music` en son, et lit un fichier
audio personnel.

## Contenu prévu

- `MusicPlayer` : interface consommée par les features (`play(track)`, `stop()`, `setTempoFactor()`).
- `ChiptuneSequencer` : synthèse en temps réel (onde carrée, triangle) écrite dans un `AudioTrack`.
  Le rendu des échantillons est isolé dans une classe pure, testable sur la JVM.
- `AudioFilePlayer` : lecture d'un fichier choisi par l'utilisateur (sélecteur de documents
  du système), via `MediaPlayer` ou Media3.
- `SoundEffects` : effets courts (rotation, ligne complète, fin de partie), plus tard.

Voir [docs/musique.md](../../docs/musique.md) pour les choix et les limites de droits.
