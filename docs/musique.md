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

Quand la pile atteint une hauteur critique, le tempo augmente de **× 1,25**, et revient à la
normale (× 1,0) quand elle redescend. Le domaine émet un événement `StackDanger(on/off)`, le
ViewModel le traduit en `setTempoFactor()`.

**Dans le jeu** (P4.11) : un seul lecteur (`MusicPlayer`, implémenté par `ChiptuneSequencer` sur
`AudioTrackSink`) pour tout le processus, créé par l'`AppContainer`. Au début de la partie, la
musique choisie à l'accueil est jouée au tempo normal (*Korobeïniki*, *Menuet* ; *Aucune* ne joue
rien ; *Mon fichier* ne joue rien en attendant P4.6), sauf si la partie est déjà en pause ou
finie quand les préférences répondent. La pause (bouton, retour système, passage en
arrière-plan) arrête la musique ; la reprise la fait **repartir où elle s'était arrêtée**
(P4.12 : `Playback.resume()`, au début du dernier temps où toutes les voix commencent une note,
au tempo en cours, position ramenée au tampon de ce tempo ; une partition pas encore lancée part
du début) ; la fin de partie et la sortie de l'écran l'arrêtent ; une nouvelle partie et
Rejouer repartent du début. La position gardée est un peu en avance sur ce qu'on a entendu (le
tampon d'`AudioTrack`, 100 à 350 ms, est vidé à l'arrêt) : le recul au temps le compense le plus
souvent, au pire la reprise saute un fragment jamais entendu. `play` rend
un jeton (`Playback`) : une partie n'arrête que sa propre musique, si bien qu'après Rejouer
l'arrêt de l'ancienne partie ne coupe pas la nouvelle. Un échec audio coupe la musique, jamais le
jeu, et il est journalisé.

**Tests (jeu) :** `MusicScoresTest › each music choice plays its score, or none`,
`MusicScoresTest › the music speeds up by a quarter while the stack is in danger, and goes back to its tempo after`,
`GameViewModelTest › at the start of the game the music of the settings plays, at the normal tempo`,
`GameViewModelTest › with no music chosen nothing plays`,
`GameViewModelTest › the game over stops the music`,
`GameViewModelTest › when the stack enters the danger the music speeds up`,
`GameViewModelTest › when the stack leaves the danger the music goes back to its tempo`,
`GameViewModelTest › leaving the game screen stops the music`,
`GameViewModelTest › leaving a game after the next one started leaves the music of the next one playing`,
`GameViewModelTest › a game paused before the settings answered starts no music once they answer`,
`GameViewModelTest › resuming a game that is not paused plays nothing again`,
`GameViewModelTest › resuming once the game is over plays no music`,
`ChiptuneSequencerTest › stopping a playback that was replaced leaves the new one playing`,
`ChiptuneSequencerTest › a failure that stops the music is reported, to be logged`,
`ChiptuneSequencerTempoTest › a stopped playback resumes from the start of the beat where it stopped`,
`ChiptuneSequencerTempoTest › a playback resumed then stopped before it wrote anything resumes from the same place`,
`ChiptuneSequencerTempoTest › a whole note stopped in its middle resumes from the start of the loop`,
`ChiptuneSequencerTempoTest › playing the score again, as a new game does, starts from its start`,
`ChiptuneSequencerTempoTest › a playback stopped at the danger tempo resumes at that tempo, where it stood`,
`ChiptuneSequencerTempoTest › a playback resumed at another tempo resumes at the same place in the score`,
`ChiptuneSequencerTempoTest › a place that falls on the end of the buffer at the new tempo resumes from its start`,
`ResumePositionTest › a whole note resumes from the start of the loop`,
`ResumePositionTest › a place in a beat where a note goes on resumes from the last beat where every note starts`,
`ResumePositionTest › a place rounded onto the end of the new buffer resumes from its start`,
`ResumePositionTest › the start of the loop stays the start of the loop`,
`GameViewModelTest › a pause stops the music, and resuming goes on where it stopped`,
`GameViewModelTest › a game paused before its music started starts it from the start when it resumes`.

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

Mélodie du chant populaire, onde carrée, volume 0,07, d'après deux notations publiques du
chant (« Russian Folk Song, 19th Century ») : [Wikimedia Commons, Korobeiniki_Music.png](https://commons.wikimedia.org/wiki/File:Korobeiniki_Music.png)
et [Wikipédia, Korobeiniki, section Melody](https://en.wikipedia.org/wiki/Korobeiniki).
Les deux sources reprennent les mesures 5 à 8 ; BloX ne joue que les 8 mesures, en boucle.

| Mesure | Notes |
|---|---|
| 1 | E4 1½ · G#4 ½ · B4 1 · G#4 ½ · E4 ½ |
| 2 | A4 1½ · C5 ½ · E5 1 · D5 ½ · C5 ½ |
| 3 | B4 1½ · C5 ½ · D5 1 · E5 1 |
| 4 | C5 1 · A4 1 · A4 2 |
| 5 | F5 1½ · G5 ½ · A5 1 · G5 ½ · F5 ½ |
| 6 | E5 1½ · F5 ½ · E5 1 · D5 ½ · C5 ½ |
| 7 | B4 1½ · C5 ½ · D5 1 · E5 1 |
| 8 | C5 1 · A4 1 · A4 2 |

Basse maison, onde triangle, volume 0,16, en noires « fondamentale – quinte – fondamentale –
quinte » : E2/B2 · A2/E3 · E2/B2 · A2/E3 · D2/A2 · A2/E3 · E2/B2 · A2/E3.

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
8 mesures complètes), la tonalité (la mineur avec G# comme sensible du mineur harmonique,
sol majeur), la plage de chaque voix (mélodies E4..A5 et F#4..G5, basses D2..E3 et C2..E3), les 8 mesures
entières de la mélodie de *Korobeïniki* (rythme pointé du chant compris), la première mesure du
Menuet et l'alternance fondamentale / quinte de la basse de *Korobeïniki*.

**Tests :** `ScoresTest › Korobeiniki is played at 150 bpm in 4-4 with a square melody at 0_07 and a triangle bass at 0_16`,
`ScoresTest › the minuet is played at 132 bpm in 3-4 with a square melody at 0_07 and a triangle bass at 0_16`,
`ScoresTest › in every voice no note crosses a bar line and every bar is complete`,
`ScoresTest › every voice has 8 bars`,
`ScoresTest › the Korobeiniki melody is the folk song of docs musique md, its 8 bars in full, with its dotted rhythm`,
`ScoresTest › the first bar of the minuet melody is the one of docs musique md`,
`ScoresTest › the Korobeiniki bass alternates a root and its fifth, octave ignored`,
`ScoresTest › the Korobeiniki basse is the one of docs musique md, its 8 bars in full`,
`ScoresTest › a melody only uses the pitch classes of its key`,
`ScoresTest › every voice stays within its range`.

> **[à vérifier] (droits)** : la première version de cette partition (syncope en mesure 5,
> notes répétées, rythme pointé perdu) ressemblait à la version popularisée par le jeu de 1989 ;
> elle a été remplacée en P4.9 par la mélodie du chant ci-dessus. Les deux sources nettes
> consultées sont des notations modernes, probablement de la même main ; une troisième
> ([a-pesni.org](http://a-pesni.org/popular20/ojpolna.htm), recueil de romances, sol mineur)
> confirme le rythme sans être lisible note à note. Pour retirer la mention, comparer à une
> édition d'époque (Prigojy, 1898 ; recueils de Panina vers 1905), par exemple à la
> Bibliothèque nationale de Russie (НЭБ).
