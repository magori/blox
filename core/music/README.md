# core:music — modèle musical

Module **Kotlin pur** : une partition n'est que de la donnée. Aucun son n'est produit ici,
c'est le rôle de `core:audio`.

Détails : [docs/musique.md](../../docs/musique.md).

## Contenu prévu

- `Pitch` (nom de note + octave, conversion en fréquence), `Note(pitch | silence, beats)`.
- `Voice(waveform, volume, notes)`, `Score(title, bpm, voices)`.
- `Scores.korobeiniki` et `Scores.minuet` : les deux arrangements maison, domaine public.
- `TempoPolicy` : tempo de base et accélération quand la pile est haute.

## Tests

JUnit 5 + AssertJ. Exemples : la fréquence de A4 vaut 440 Hz, chaque mesure d'une
partition a la bonne durée, deux voix d'un même morceau ont la même longueur totale.
