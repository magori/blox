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

## Synthèse

`Synthesizer` (`core:audio`, code pur testé sur la JVM) transforme une voix en échantillons
`Float` dans [−1, 1], à une fréquence d'échantillonnage donnée :

- **Ondes** : carrée (+volume sur la première moitié de la période, −volume sur la seconde) et
  triangle (part de 0, +volume au quart de la période, linéaire). La phase se calcule depuis
  l'indice de l'échantillon, sans accumulation, et repart de 0 à chaque note.
- **Enveloppe** : chaque note joue à volume constant pendant 80 % de sa durée, puis s'éteint en
  fondu linéaire jusqu'à 95 %, pour séparer les notes répétées ; elle est muette ensuite.
- **Durée** : une note dure `temps × 60 / bpm × fréquence` échantillons ; les bornes de chaque
  note se calculent depuis le temps cumulé, si bien qu'une voix entière dure exactement sa
  durée totale, sans dérive d'arrondi.
- **Mixage** : `mix` additionne les voix, échantillon par échantillon, puis écrête à [−1, 1].
  Les voix doivent avoir la même longueur (garanti par `Score`).
- La fréquence d'échantillonnage et le tempo doivent être positifs.

**Tests :** `SynthesizerTest › a square wave at 440 Hz alternates plus and minus the volume with a period of about 100 point 2 samples`,
`SynthesizerTest › a triangle wave starts at 0, reaches the volume at a quarter period and varies linearly`,
`SynthesizerTest › a rest only produces zeros`,
`SynthesizerTest › the envelope keeps the volume up to 80 percent of the note, fades out up to 95 percent, then is silent`,
`SynthesizerTest › a note lasts its beats times 60 over the tempo times the sample rate`,
`SynthesizerTest › a whole voice lasts exactly its total beats, without drift from rounding each note`,
`SynthesizerTest › each note starts at the sample of its cumulated beats`,
`SynthesizerTest › the sample rate must be positive`, `SynthesizerTest › rendering a voice needs a positive tempo`,
`MixTest › mixing adds the voices sample by sample`, `MixTest › the mix is clipped to -1 and 1`,
`MixTest › mixing no track is refused`, `MixTest › mixing tracks of different lengths is refused`.

## Lecture en boucle

- **Rendu** : `Synthesizer.render(score, tempoFactor = 1.0)` est la seule entrée publique ; il
  mixe toutes les voix et dure exactement la partition. Le tempo est un `Double`
  (150 × 1,25 = 187,5 accepté) ; le facteur doit être positif et fini. Les durées restent en
  `Double` : le tampon est rendu une fois puis rejoué, rien ne se cumule sur la boucle.
- **Séquenceur** : `ChiptuneSequencer(synthesizer, openSink, scope)`, `play(score)` et `stop()`,
  appelés depuis le fil principal. Le rendu se fait hors du fil audio (`Dispatchers.Default`),
  une fois par partition et par facteur de tempo (cache, environ 2,3 Mo pour 8 mesures à
  44 100 Hz) ; l'écriture, sur `Dispatchers.IO`, par tranches de 1 024 échantillons qui font le
  tour du tampon sans trou (`LoopCursor`). La sortie s'ouvre à la fréquence du synthétiseur.
- **Arrêt** : `stop()` arrête l'écriture à la tranche suivante, même si la sortie bloque au lieu
  de suspendre, et libère la sortie ; un nouveau `play` n'ouvre sa sortie qu'une fois
  l'ancienne libérée (jamais deux à la fois). Une sortie qui ne s'ouvre pas ou une écriture qui
  échoue arrête la musique sans faire planter le jeu.
- **Sortie** : `AudioTrackSink`, `AudioTrack` en `MODE_STREAM`, mono, échantillons flottants,
  écriture bloquante ; tampon de 4 fois le minimum (au moins deux tranches) ; une écriture en
  erreur lève une exception ; arrêt par `pause`, `flush`, `release`. Sans test JVM.

**Tests :** `ScoreRenderTest › a score renders as the mix of its voices and lasts as long as the score`,
`ScoreRenderTest › a tempo factor of 2 halves the duration`,
`ScoreRenderTest › the tempo may become fractional, 150 bpm times 1 point 25 is 187 point 5 bpm`,
`ScoreRenderTest › the tempo factor must be positive and finite`,
`LoopCursorTest › slices follow one another from the start of the buffer`,
`LoopCursorTest › a slice across the end of the buffer goes on from its start, without a gap`,
`LoopCursorTest › a slice longer than the buffer goes round it as many times as needed`,
`LoopCursorTest › an empty buffer is refused`,
`ChiptuneSequencerTest › play renders the score on the rendering dispatcher, then writes it`,
`ChiptuneSequencerTest › a score is rendered once, then played again from the cache`,
`ChiptuneSequencerTest › the slices are written in order, round the loop`,
`ChiptuneSequencerTest › stop ends the writing and releases the sink`,
`ChiptuneSequencerTest › playing another score replaces the one playing, without leaving two loops`,
`ChiptuneSequencerTest › stop without play does nothing`,
`ChiptuneSequencerTest › the sink opens at the sample rate of the rendering`,
`ChiptuneSequencerTest › a sink that blocks instead of suspending still stops at the next slice`,
`ChiptuneSequencerTest › stop then play opens the new sink only once the old one is released`,
`ChiptuneSequencerTest › play then play opens the new sink only once the old one is released`,
`ChiptuneSequencerTest › a sink that cannot open stops the music without a crash`,
`ChiptuneSequencerTest › a write that fails releases the sink and stops the music without a crash`,
`ChiptuneSequencerTest › a stop during the rendering opens no sink`.

## Accélération

Quand la pile atteint une hauteur critique, le tempo augmente (par exemple × 1,25), et revient
à la normale quand elle redescend. Le domaine émet un événement `StackDanger(on/off)`, le
ViewModel le traduira en `setTempoFactor()` (branchement : P4.11).

**Changement de tempo dans le séquenceur** (P4.5) : `ChiptuneSequencer.setTempoFactor(facteur)`
(positif et fini ; retenu pour le prochain `play` si rien ne joue). Le tampon au nouveau facteur
est rendu en tâche de fond, une fois par (partition, facteur), ou pris dans le cache ; la sortie
reste ouverte. Pour ne jamais couper une note en pleine amplitude (clic), la lecture finit le
temps en cours dans l'ancien tampon et ne bascule qu'au prochain temps où **toutes** les voix
commencent une note (`switchBeats`), au début de ce temps dans le nouveau tampon, retour au début
de la boucle compris. Positions calculées en `Long`, arrondies à l'échantillon (`BeatGrid`).
Jamais de rééchantillonnage : la hauteur ne change pas. Si le rendu n'est pas prêt à la
frontière, l'ancien tempo continue jusqu'à la suivante ; de deux changements rapides, seul le
dernier est rendu et joué ; un rendu de tempo qui échoue laisse jouer l'ancien tempo, et un
`play` après `stop` n'attend pas un rendu en cours.

**Tests (séquenceur) :** `ChiptuneSequencerTempoTest › the tempo factor must be positive and finite`,
`ChiptuneSequencerTempoTest › a tempo factor set while nothing plays is kept for the next play`,
`ChiptuneSequencerTempoTest › a tempo change keeps the same sink, and the writing goes on at the new tempo`,
`ChiptuneSequencerTempoTest › a new tempo is rendered once on the rendering dispatcher, and going back to 1 point 0 renders nothing`,
`ChiptuneSequencerTempoTest › the tempo changes at the start of the next beat, once the current beat is played`,
`ChiptuneSequencerTempoTest › a rendering not ready when a beat starts waits for the next beat, the old tempo goes on`,
`ChiptuneSequencerTempoTest › after the change the samples written are exactly those of the buffer at the new tempo`,
`ChiptuneSequencerTempoTest › of two quick tempo changes only the last one is applied`,
`ChiptuneSequencerTempoTest › of two quick tempo changes to new tempos only the last one is rendered and played`,
`ChiptuneSequencerTempoTest › a stop while a new tempo renders leaves no sink open and throws nothing`,
`ChiptuneSequencerTempoTest › a note across the next beat delays the change to the next beat where every voice starts a note`,
`ChiptuneSequencerTempoTest › when the next beat is the end of the buffer, the new tempo starts from its first sample`,
`ChiptuneSequencerTempoTest › play after stop does not wait for a tempo rendering under way`,
`ChiptuneSequencerTempoTest › a tempo rendering that fails leaves the music playing at the old tempo`,
`SwitchBeatsTest › a whole note lets the tempo switch only on beat 0`,
`SwitchBeatsTest › quarter notes let the tempo switch on every beat`,
`SwitchBeatsTest › two voices with offset notes let the tempo switch only on the beats they share`,
`SwitchBeatsTest › eighth notes do not make beats of their own`,
`BeatGridTest › the next beat of a position inside a beat is the following one`,
`BeatGridTest › a position on the start of a beat is that beat`,
`BeatGridTest › the start of a beat is rounded to the nearest sample, and a position on it is that beat`,
`BeatGridTest › after the last beat comes the end of the loop`,
`BeatGridTest › near the end of a buffer of 1 point 3 million samples nothing overflows`,
`LoopCursorTest › a part of a slice can be filled, the rest of it is left as it was`,
`LoopCursorTest › a position outside the buffer is refused`.

La hauteur de pile vaut 20 moins la première ligne occupée (0 si le plateau est vide). La pile
**entre** en danger quand elle atteint 15 et n'en **sort** qu'en redescendant à 12 ou moins :
deux seuils, pour que le tempo ne bascule pas à chaque pièce. Valeurs à régler en jouant
**[à vérifier]**. La hauteur se mesure au verrouillage, sur le plateau **après** effacement des
lignes ; l'événement n'est émis qu'au franchissement, au verrouillage s'il n'y a pas
d'effacement, sinon à la fin de l'animation, avec `LinesCleared`, pour que le tempo change
quand la pile baisse à l'écran. La fin de partie n'émet pas `StackDanger(off)` : c'est l'audio
qui s'arrête sur `GameOver`.

**Tests (moteur) :** `GameEngineStackDangerTest › without a clear, the danger is measured when the piece locks and StackDanger is sent only when it changes`,
`GameEngineStackDangerTest › four lines locked at height 15 leave the stack 11 rows high, with no danger and no StackDanger`,
`GameEngineStackDangerTest › with a clear, StackDanger comes at the end of the animation, with LinesCleared, as the stack goes down`.

**Tests :** `BoardStackHeightTest › an empty board has a stack height of 0`,
`BoardStackHeightTest › the stack height is 20 minus the first filled row`,
`BoardStackHeightTest › an isolated cell high up counts, even above a hole`,
`ClassicStackDangerRuleTest › out of danger the stack enters danger when its height reaches 15`,
`ClassicStackDangerRuleTest › in danger the stack leaves danger only when its height falls to 12 or less`,
`ClassicStackDangerRuleTest › the same height gives a different answer depending on whether the stack was in danger`,
`ClassicStackDangerRuleTest › a stack height outside 0 to 20 is refused`.

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
