# core:audio — lecture du son

Module **Android**. Transforme une partition de `core:music` en son, et lit un fichier
audio personnel.

## Contenu prévu

- `MusicPlayer` : interface consommée par les features (`play(track)`, `stop()`, `setTempoFactor()`).
- `ChiptuneSequencer` : synthèse en temps réel (onde carrée, triangle) écrite dans un `AudioTrack`.
  Le rendu des échantillons est isolé dans une classe pure, testable sur la JVM.
- `FocusedMusicPlayer` : ne joue qu'avec le focus audio (`AudioFocus`, `SystemAudioFocus` sur
  `AudioManager`) ; l'abandonne à l'arrêt, s'arrête à une perte durable ou au débranchement du
  casque, se met en pause pendant une perte passagère.
- `AudioFilePlayer` : lecture d'un fichier choisi par l'utilisateur (sélecteur de documents
  du système), via `MediaPlayer` ou Media3.
- `SoundEffects` : effets courts (rotation, ligne complète, fin de partie), plus tard.
- `RingtoneExporter` : écrit une musique du joueur en WAV comme sonnerie ou réveil (ADR 0031) ;
  `MediaStoreRingtoneExporter` par `MediaStore` (Android 10 et plus), `FakeRingtoneExporter` en doublure.

Voir [docs/musique.md](../../docs/musique.md) pour les choix et les limites de droits.
