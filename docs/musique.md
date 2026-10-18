# Musique

## Ce qu'on peut faire, et ce qu'on ne fait pas

- **Oui** : jouer des **mélodies du domaine public** dans des **arrangements écrits pour BloX**.
  *Korobeïniki* est une chanson populaire russe du XIXe siècle. Le *Menuet en sol* (Petit
  livre d'Anna Magdalena Bach, 1725, longtemps attribué à J.-S. Bach, aujourd'hui à
  Christian Petzold) est lui aussi libre.
- **Non** : reproduire les arrangements ou enregistrements des jeux de 1989 (compositions de
  Hirokazu Tanaka, propriété de Nintendo). Ils ne sont ni retranscrits ni extraits dans ce dépôt.
- **Fichier personnel** : l'application peut lire un fichier audio que l'utilisateur choisit
  sur son téléphone. Le fichier reste sur l'appareil et **n'est jamais versionné**
  (`app/src/main/assets/music/` est ignoré par git).

Voir l'[ADR 0008](adr/0008-musique-domaine-public.md).

## Choix proposés à l'accueil

*Korobeïniki* · *Menuet* · *Mon fichier* · *Aucune*

## Modèle

```
Score(title, bpm, beatsPerBar, voices)   // bpm > 0, beatsPerBar > 0, au moins une voix,
                                         // voix de même longueur, nombre entier de mesures
Voice(waveform: SQUARE | TRIANGLE, volume, notes)   // volume dans [0, 1], au moins une note
Note(pitch: Pitch?, beats: Double)   // pitch null = silence ; beats > 0 et fini
Pitch.parse("A4") -> 440 Hz ; midi = 12 × (octave + 1) + demi-ton ; f = 440 × 2^((midi − 69) / 12)
```

`Pitch` est un type valeur sur le numéro MIDI : deux hauteurs de même nom sont égales, et une
hauteur s'affiche par son nom. Notation : lettre majuscule A à G, dièse `#` facultatif (pas de
bémol), octave 0 à 8 ; tout autre nom est refusé. Le numéro MIDI va de 12 (C0) à 119 (B8),
pour que toute hauteur se relise depuis son nom. `SQUARE` sert aux mélodies, `TRIANGLE` aux
basses.

**Tests :** `PitchTest › A4 is 440 Hz`,
`PitchTest › the MIDI number is 12 times the octave plus one, plus the semitone`,
`PitchTest › one octave higher doubles the frequency and one octave lower halves it`,
`PitchTest › middle C is about 261 point 63 Hz`, `PitchTest › an invalid name is refused`,
`PitchTest › two pitches with the same name are equal, and so are two identical notes`,
`PitchTest › a pitch prints as its name`, `NoteTest › the duration of a note must be positive and finite`,
`NoteTest › a note without pitch is a rest`, `NoteTest › a note with a pitch is not a rest`,
`VoiceTest › the volume must be between 0 and 1`, `VoiceTest › a volume of 0 or 1 is accepted`,
`VoiceTest › the total duration is the sum of the durations of the notes`,
`VoiceTest › a voice needs at least one note`, `ScoreTest › a well-formed score is built`,
`ScoreTest › the tempo must be positive`, `ScoreTest › a score needs at least one voice`,
`ScoreTest › all voices of a score must have the same length`,
`ScoreTest › the number of beats per bar must be positive`,
`ScoreTest › every voice lasts a whole number of bars`,
`PitchTest › a MIDI number outside C0 to B8 is refused`,
`PitchTest › the MIDI numbers of C0 and B8 are accepted`,
`PitchTest › a pitch at either end of the range reads back from its name`.

Le séquenceur lit les voix en parallèle. Chaque note joue à volume constant pendant 80 % de
sa durée puis s'éteint en fondu jusqu'à 95 %, pour séparer les notes répétées.

## Accélération

Quand la pile atteint une hauteur critique, le tempo augmente (par exemple × 1,25), et revient
à la normale quand elle redescend. Le domaine émet un événement `StackDanger(on/off)`, le
ViewModel le traduit en `setTempoFactor()`. Seuil exact à décider en jouant.

## Partitions (prototype de la maquette)

Durées en temps (1 = noire, 0,5 = croche). Notation anglo-saxonne (A4 = la 440 Hz).

### Korobeïniki — 150 temps/min, 4/4

Mélodie, onde carrée, volume 0,07 :

| Mesure | Notes |
|---|---|
| 1 | E5 1 · B4 ½ · C5 ½ · D5 1 · C5 ½ · B4 ½ |
| 2 | A4 1 · A4 ½ · C5 ½ · E5 1 · D5 ½ · C5 ½ |
| 3 | B4 1½ · C5 ½ · D5 1 · E5 1 |
| 4 | C5 1 · A4 1 · A4 1 · silence 1 |
| 5 | silence ½ · D5 1 · F5 ½ · A5 1 · G5 ½ · F5 ½ |
| 6 | E5 1½ · C5 ½ · E5 1 · D5 ½ · C5 ½ |
| 7 | B4 1 · B4 ½ · C5 ½ · D5 1 · E5 1 |
| 8 | C5 1 · A4 1 · A4 1 · silence 1 |

Basse maison, onde triangle, volume 0,16, en noires « fondamentale – quinte – fondamentale –
quinte » : E2/B2 · A2/E3 · E2/B2 · A2/E3 · D2/A2 · C3/G2 · E2/B2 · A2/E3.

### Menuet en sol — 132 temps/min, 3/4

Mélodie, onde carrée, volume 0,07 :

| Mesure | Notes |
|---|---|
| 1 | D5 1 · G4 ½ · A4 ½ · B4 ½ · C5 ½ |
| 2 | D5 1 · G4 1 · G4 1 |
| 3 | E5 1 · C5 ½ · D5 ½ · E5 ½ · F#5 ½ |
| 4 | G5 1 · G4 1 · G4 1 |
| 5 | C5 1 · D5 ½ · C5 ½ · B4 ½ · A4 ½ |
| 6 | B4 1 · C5 ½ · B4 ½ · A4 ½ · G4 ½ |
| 7 | F#4 1 · G4 ½ · A4 ½ · B4 ½ · G4 ½ |
| 8 | A4 3 |

Basse maison, onde triangle, volume 0,16, une blanche pointée par mesure :
G2 · B2 · C3 · B2 · A2 · G2 · D3 · D2.

> Les basses sont volontairement simples. Ce sont elles qui font de ces versions des
> arrangements BloX : on pourra les enrichir.

Les deux partitions sont transcrites dans `Scores` (`core:music`). Les tests vérifient la
structure (tempo, mesure, ondes, volumes), les durées (aucune note ne chevauche une barre,
8 mesures complètes), la tonalité (la mineur naturel, sol majeur), la plage de chaque voix
(mélodies A4..A5 et F#4..G5, basses C2..E3), la première mesure de chaque mélodie et
l'alternance fondamentale / quinte de la basse de *Korobeïniki*.

**Tests :** `ScoresTest › Korobeiniki is played at 150 bpm in 4-4 with a square melody at 0_07 and a triangle bass at 0_16`,
`ScoresTest › the minuet is played at 132 bpm in 3-4 with a square melody at 0_07 and a triangle bass at 0_16`,
`ScoresTest › in every voice no note crosses a bar line and every bar is complete`,
`ScoresTest › every voice has 8 bars`,
`ScoresTest › the first bar of the Korobeiniki melody is the one of docs musique md`,
`ScoresTest › the first bar of the minuet melody is the one of docs musique md`,
`ScoresTest › the Korobeiniki bass alternates a root and its fifth, octave ignored`,
`ScoresTest › a melody only uses the pitch classes of its key`,
`ScoresTest › every voice stays within its range`.

> **[à vérifier] (droits)** : le rythme de la mélodie de *Korobeïniki* ci-dessus (mesure 5 :
> demi-temps de silence puis syncopes) ressemble à la version popularisée par le jeu de 1989
> plus qu'au chant populaire, noté en 2/4. La mélodie est du domaine public, l'arrangement du
> jeu ne l'est pas : comparer à une partition publique du chant et citer la source (P4.9).
