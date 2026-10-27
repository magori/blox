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

**Écouter** (P4.7) : sous *Korobeïniki* et *Menuet* (les choix qui ont une partition BloX), un
bouton Écouter / Arrêter joue un aperçu de la partition, par le même lecteur que le jeu. Un seul
aperçu à la fois : écouter une autre musique arrête le précédent, la réécouter l'arrête ;
choisir une autre musique l'arrête, choisir celle qu'on écoute la laisse jouer. L'aperçu
s'arrête à Jouer, quand l'accueil passe en arrière-plan (`ON_STOP`, donc aussi à une recréation
de l'activité) et quand on quitte l'accueil ; il continue derrière un dialogue. L'arrêt passe
par le jeton de lecture : il ne coupe jamais la musique d'une partie lancée entre-temps. La
correspondance choix → partition (`scoreOf`) vit dans `app` ; une partition est désignée par
un identifiant (`ScoreId`), plus par un titre. L'aperçu demande le focus audio comme le jeu
(voir Focus audio).

**Tests :** `MusicScoresTest › each music choice plays its score, or none`,
`HomeViewModelTest › only the musics with a score can be listened to`,
`HomeViewModelTest › listening to a music plays its score and shows it as previewed`,
`HomeViewModelTest › listening again to the music being previewed stops it`,
`HomeViewModelTest › listening to another music stops the preview and plays the other`,
`HomeViewModelTest › a music without a BloX score cannot be listened to`,
`HomeViewModelTest › choosing another music stops the preview`,
`HomeViewModelTest › choosing the music being previewed lets it play on`,
`HomeViewModelTest › playing stops the preview`,
`HomeViewModelTest › the preview stops when the screen goes to the background, never the music of a game started since`,
`HomeViewModelTest › the preview plays on while the screen is still visible`,
`HomeViewModelTest › leaving the home screen stops the preview`,
`HomeViewModelTest › leaving the home screen with no music listened to lets the music of the game play, without any stop`,
`ListenFaceTest › the listen button shows a triangle, and a square while playing, read Écouter or Arrêter with the name of the music`,
`ListenButtonTest › a listen button is a button of 48 dp or the size given, read with the name of its music, or its verb alone`,
`ScoresTest › each arrangement is known by its own identifier`. Vue : V1.

**Aperçu qui s'arrête de lui-même** (P4.23) : chaque lecture expose son état
(`Playback.isPlaying`), vrai dès qu'elle joue ou attend le focus, faux pour de bon quand elle
est arrêtée, remplacée, refusée par le focus, quand le séquenceur échoue, que le focus est perdu
pour de bon ou que le casque est débranché. Une interruption brève (assistant vocal) laisse la
lecture « en cours » : la musique reprend seule. L'accueil suit son aperçu : quand il s'arrête de
lui-même, le bouton redevient « Écouter » et le focus est rendu. Une lecture morte ne redevient
jamais « en cours ». En jeu aussi (P5.35), la partie suit sa lecture : quand la musique meurt,
elle l'arrête et rend le focus, puis continue en silence ; la reprise après une pause relance la
musique, au tempo du moment (un passage en danger pendant le silence ne joue rien mais est gardé
pour la reprise). Avec « Rejouer », l'ancienne partie n'arrête jamais la musique de la nouvelle.
L'état se suit sur le thread principal (sinon un « faux » passager peut apparaître pendant une
pause de focus).
`GameViewModelTest › a music that stops by itself is stopped, to give the audio focus back, and the game goes on`,
`GameViewModelTest › a music that stopped by itself plays again when the game resumes after a pause`,
`GameViewModelTest › a music that stopped by itself again after the resume is stopped again`,
`GameViewModelTest › entering the danger while the music is stopped by itself plays nothing, and the resume is faster`,
`GameViewModelTest › the music of a new game is not stopped by the earlier game whose music it replaced`.

**Tests :** `HomeViewModelTest › a preview whose music stops by itself, as the audio output fails, offers to listen again`,
`MusicPlayerContract › a playback plays as soon as its score is played`,
`MusicPlayerContract › a stopped playback no longer plays`,
`MusicPlayerContract › a playback replaced by another play no longer plays, the new one does`,
`MusicPlayerContract › a resumed playback plays, the playback it resumes stays stopped`,
`MusicPlayerContract › a playback whose music stops by itself, as the audio output fails, no longer plays`,
`MusicPlayerContract › the playing state of a playback follows it, from its start to its end`
(contrat joué par `ChiptuneSequencerTest`, `FakeMusicPlayerTest` et `FocusedMusicPlayerContractTest`),
`ChiptuneSequencerTest › a failure that stops the music tells its playback no longer plays`,
`ChiptuneSequencerTest › a sink that keeps dying stops the playback`,
`FocusedMusicPlayerTest › a playback stopped by a loss of the focus or unplugged headphones no longer plays`,
`FocusedMusicPlayerTest › a playback paused by a focus lost for a while still plays, waiting for the focus`,
`FocusedMusicPlayerTest › a playback paused then resumed by the focus never tells it stopped`,
`FocusedMusicPlayerTest › a stopped playback tells it stopped once, and nothing more`,
`FocusedMusicPlayerTest › a playback resumed by the focus that comes back plays, and follows the music resumed`,
`FocusedMusicPlayerTest › a playback refused the focus does not play`,
`FocusedMusicPlayerTest › a playback whose music stopped by itself never plays again with the focus going and coming back`,
`FocusedMusicPlayerTest › a playback waiting for the focus no longer plays once another score replaces it`.
Vue : V1. À vérifier sur téléphone : casque Bluetooth débranché, podcast lancé, assistant vocal
pendant l'aperçu.

**Musique personnelle à l'accueil** (P4.22d) : la musique de « Mes musiques » retenue pour les
parties apparaît parmi les choix de musique, sous son nom (sur deux lignes au besoin), comme le
seul choix sélectionné ; TalkBack la lit « nom, en jeu ». Elle s'écoute (*Écouter*) comme les
autres. Elle n'a pas d'action : choisir une autre musique de l'accueil (Korobeïniki, Menuet,
Aucune, Mon fichier) la retire des parties (la musique de l'accueil est enregistrée d'abord), et
elle disparaît de l'accueil ; on la retrouvera dans « Mes musiques » (P4.22c). Son aperçu
s'arrête si elle n'est plus la musique des parties. Écart avec la maquette V1 : ni icône de note
ni grille de trois colonnes (P5.30).

**Tests :** `HomeViewModelTest › a music of the player played during the games is shown by its name, as the only music selected`,
`HomeViewModelTest › without a music of the player for the games, the music chosen on the home screen is selected`,
`HomeViewModelTest › choosing another music on the home screen leaves the music of the player and chooses it`,
`HomeViewModelTest › choosing my file with a music file kept leaves the music of the player`,
`HomeViewModelTest › choosing a music file leaves the music of the player`,
`HomeViewModelTest › choosing my file without a music file leaves the music of the player only once a file is picked`,
`HomeViewModelTest › a music of the player that cannot be left stays selected, without crashing`,
`HomeViewModelTest › a music that cannot be chosen leaves the music of the player for the games`,
`HomeViewModelTest › listening to the music of the player plays its score and shows it as previewed`,
`HomeViewModelTest › listening again to the music of the player stops it`,
`HomeViewModelTest › listening to a music of the home screen stops the music of the player listened to`,
`HomeViewModelTest › choosing another music stops the music of the player listened to`,
`HomeViewModelTest › a music of the player listened to stops once it is no longer played during the games`,
`HomeViewModelTest › without a music of the player for the games, there is nothing of it to listen to`,
`HomeScreenTest › a music of the player for the games is read as such, the only music selected, a 48 dp radio button`,
`HomeScreenTest › listening to the music of the player for the games calls for it`,
`HomeScreenTest › the music of the player being listened to offers to stop it`. Vue : V1.

Prévu : des musiques composées dans l'application, à la main ou par le générateur
(bouton *Au hasard*, par catégorie). Spécification : [generateur-musique.md](generateur-musique.md).

Prévu (P8) : un sixième choix, *Vivante*, une musique composée pour chaque partie, calme puis
tendue quand la pile monte, qui accélère avec les niveaux. Spécification :
[musique-vivante.md](musique-vivante.md) ; la Graine du jour l'impose
([graine-du-jour.md](graine-du-jour.md)). Les jeux musicaux Au tempo et Jardin musical ont leur
propre son : [regles-au-tempo.md](regles-au-tempo.md), [jardin-musical.md](jardin-musical.md).

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
  appelés depuis le fil principal. Le rendu se fait hors du fil audio (`Dispatchers.Default`) ;
  l'écriture, sur `Dispatchers.IO`, par tranches de 1 024 échantillons qui font le tour du tampon
  sans trou (`LoopCursor`). La sortie s'ouvre à la fréquence du synthétiseur.
- **Cache** (P5.15) : `TempoBuffers` ne garde que la partition en cours, un tampon par facteur
  de tempo (environ 2,3 Mo pour 8 mesures à 44 100 Hz ; en pratique le tempo normal et celui
  du danger) ; une autre partition le vide, et abandonne ses rendus, avant d'être rendue.
  `prepareTempo(facteur)`, appelé par la partie juste après `play` (l'aperçu de l'accueil n'en
  appelle aucun), fait rendre ce tempo en tâche de fond dès le lancement, sans retarder la
  première tranche, au lieu de le rendre à l'entrée en danger ; il est gardé pour `resume` et
  oublié par le `play` suivant. Les rendus survivent à une pause et s'arrêtent avec le scope du
  séquenceur.
- **Arrêt** : `stop()` arrête l'écriture à la tranche suivante, même si la sortie bloque au lieu
  de suspendre, et libère la sortie. Un nouveau `play` arrête aussitôt la boucle en cours, puis
  attend la sortie sous un verrou, dans l'ordre des lectures : jamais deux sorties à la fois,
  même pour un `play` arrêté avant d'avoir démarré. Une sortie qui ne s'ouvre pas ou une
  écriture qui échoue arrête la musique sans faire planter le jeu, et l'échec est journalisé.
- **Sortie morte** (P5.15) : une écriture qui rend `ERROR_DEAD_OBJECT` (redémarrage du serveur
  audio) lève `DeadSinkException` ; `ReopeningSink` libère la sortie morte, en ouvre une autre et
  y réécrit la tranche, si bien que la musique reprend où elle en était. Au plus une réouverture
  par écriture, et 3 par fenêtre de 2 048 tranches (environ 48 s) ; jamais pendant un arrêt.
  Au-delà, ou si la sortie rouverte meurt aussitôt, la musique s'arrête, journalisée. Sur un
  Android récent, un changement de sortie Bluetooth est le plus souvent rerouté sans cette
  erreur.
- **Sortie** : `AudioTrackSink`, `AudioTrack` en `MODE_STREAM`, mono, échantillons flottants,
  écriture bloquante ; tampon de 4 fois le minimum (au moins deux tranches) ; une écriture en
  erreur lève une exception ; arrêt par `pause`, `flush`, `release`, la piste étant libérée même
  si `pause` échoue. Sans test JVM.

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
`ChiptuneSequencerTest › a stop during the rendering opens no sink`,
`ChiptuneSequencerTest › only the score playing stays in the cache`,
`ChiptuneSequencerTest › a play stopped before it starts still stops the score playing before it`,
`ChiptuneSequencerTest › a play after a play stopped before it starts waits for the first sink to be released`,
`ChiptuneSequencerTest › a play of a cached score stopped before it starts opens no sink`,
`ChiptuneSequencerTest › the tempo prepared for a new score is never rendered for the score it replaces`,
`ChiptuneSequencerTest › a sink that dies once is reopened and the music goes on where it stood`,
`ChiptuneSequencerTest › a sink reopened that dies at once stops the music, reported, without a crash`,
`ChiptuneSequencerTest › a sink that dies again after playing a while is reopened again`,
`ChiptuneSequencerTest › a sink that dies and cannot be reopened stops the music, reported, released once`,
`ChiptuneSequencerTest › a sink that keeps dying stops the music, reported`,
`ChiptuneSequencerTest › a sink that dies while the music stops is not reopened`,
`ChiptuneSequencerTest › a dead sink whose release fails is released only once, and the music stops, reported`,
`ReopeningSinkTest › deaths far apart from one another are each reopened, beyond three`,
`MusicPlayerContract › the tempo factor prepared must be positive and finite`,
`ChiptuneSequencerTempoTest › the danger tempo prepared is rendered as soon as the score plays, without holding back its first slice`,
`MusicPlayerContract › a new play forgets the tempos prepared before, as a preview prepares no other tempo`,
`ChiptuneSequencerTempoTest › a pause during the preparation of the danger tempo does not render it twice`,
`ChiptuneSequencerTempoTest › playing another score abandons the preparations of the one before`,
`ChiptuneSequencerTempoTest › a danger tempo prepared stays in the cache when the score is resumed`,
`ChiptuneSequencerTempoTest › a playback resumed at the danger tempo, once another score emptied the cache, prepares the normal tempo`,
`ChiptuneSequencerTempoTest › the renderings under way end with the scope of the sequencer`,
`GameViewModelTest › at the start of the game the music prepares the danger tempo`,
`GameViewModelTest › a game paused before its music started prepares the danger tempo when its music starts`,
`HomeViewModelTest › a preview prepares no other tempo`.

## Focus audio

La musique ne joue qu'avec le focus audio (P5.14) : `FocusedMusicPlayer`, décorateur du
lecteur dans `core:audio`, le demande (`AUDIOFOCUS_GAIN`, attributs jeu et musique) quand une
partition démarre ou reprend, et l'abandonne à son arrêt. Rejouer une autre partition garde le
focus sans le redemander. Refusé (un appel en cours), le focus ne fait rien jouer, et la reprise
suivante le redemande. Une perte durable (un podcast qui démarre) ou le casque débranché
(`ACTION_AUDIO_BECOMING_NOISY`) arrêtent la musique et abandonnent le focus ; elle revient à la
reprise de la partie. Une perte passagère (assistant vocal, voix de navigation) met la musique en
pause, et elle reprend là où elle était au retour du focus ; une partition jouée ou reprise pendant
cette perte attend le retour du focus. Pour une notification ou une voix de navigation, le système
baisse lui-même le volume (Android 8 et plus) ; une demande de baisser le volume adressée à
l'application (seulement quand le système ne peut pas la faire lui-même, par exemple pour une
application qui vise une version antérieure à Android 8) est traitée comme une perte passagère.
Une partie sans musique (*Aucune*) ne demande jamais le focus.
L'implémentation Android (`SystemAudioFocus`) reste fine, sans test JVM,
comme `AudioTrackSink`.

**Tests :** `FocusedMusicPlayerTest › playing requests the audio focus, then plays`,
`FocusedMusicPlayerTest › a focus refused plays nothing`,
`FocusedMusicPlayerTest › stopping abandons the audio focus`,
`FocusedMusicPlayerTest › stopping a playback that was replaced keeps the focus`,
`FocusedMusicPlayerTest › playing another score keeps the focus without asking for it again`,
`FocusedMusicPlayerTest › resuming requests the audio focus again, then resumes`,
`FocusedMusicPlayerTest › a playback refused the focus plays from its start once resumed with it`,
`FocusedMusicPlayerTest › losing the focus for good stops the music and abandons the focus`,
`FocusedMusicPlayerTest › losing the focus for a while pauses the music, waiting for the focus to come back`,
`FocusedMusicPlayerTest › getting the focus back resumes the music where it paused`,
`FocusedMusicPlayerTest › a playback resumed by the focus still gives it back when stopped`,
`FocusedMusicPlayerTest › playing another score while the focus is lost for a while waits for the focus to come back`,
`FocusedMusicPlayerTest › resuming while the focus is lost for a while waits for the focus to come back`,
`FocusedMusicPlayerTest › pausing the game while the focus is lost for a while gives it back, and its return resumes nothing`,
`FocusedMusicPlayerTest › a playback stopped by a loss of the focus asks for it again when the game resumes`,
`FocusedMusicPlayerTest › getting the focus while playing changes nothing`,
`FocusedMusicPlayerTest › the headphones being unplugged stops the music and abandons the focus`,
`FocusedMusicPlayerTest › setting the tempo, as a game without music does, asks for no focus`,
`FocusedMusicPlayerTest › preparing a tempo asks for no focus, and goes to the player`,
`FocusedMusicPlayerTest › a tempo prepared while the focus is refused is prepared once the score plays`,
`FocusedMusicPlayerTest › a new play forgets the tempos prepared for the score refused the focus`.

## Effets sonores

Effets courts du jeu (P4.8), **activés par défaut** depuis P5.65 (règles et réglage : `docs/regles-mode-classique.md`,
Effets sonores). Arpèges de l'accord de do, synthétisés et écrits pour BloX : aucun fichier son,
rien du jeu de 1989.

| Effet | Onde | Volume | Enveloppe (montée / descente) | Notes | Durée |
|---|---|---|---|---|---|
| Rotation | carrée | 0,05 | 1 / 10 ms | E6 30 ms | 30 ms |
| 1 à 3 lignes | carrée | 0,08 | 2 / 15 ms | C6 40, E6 40, G6 80 | 160 ms |
| 4 lignes | carrée | 0,09 | 2 / 15 ms | C5, E5, G5, C6, E6, G6 (50 ms chacune), C7 150 ms | 450 ms |
| Fin de partie | triangle | 0,2 | 2 / 30 ms | G4 120, E4 120, C4 120, G3 240 | 600 ms |

Le Serrurier a ses propres effets (`LocksmithSounds`, P7.9), hors de `SoundEffects.all` que le
Classique préchauffe ; leurs volumes et enveloppes se lisent sur ceux du Classique :

| Effet | Onde | Volume | Enveloppe | Notes | Durée |
|---|---|---|---|---|---|
| Arpège d'un accord | carrée | celui des lignes (0,08) | 2 / 15 ms | fondamentale 70, tierce 70, quinte 300 | 440 ms |
| Basse d'un accord | triangle | celui de la fin de partie (0,2) | 2 / 30 ms | fondamentale deux octaves plus bas, de C2 à C4 | 440 ms |
| Serrure ouverte | carrée | celui des 4 lignes (0,09) | 2 / 15 ms | C5, E5, G5 (60 ms chacune), C6 240 | 420 ms |
| Refus | carrée | 0,06 | 2 / 15 ms | C3 90, B2 90 | 180 ms |

L'arpège et la basse d'un accord (`effectsOf(chord)`) sont deux effets, joués ensemble sur deux
pistes.
**Tests :** `LocksmithSoundsTest › an arpeggio plays the root the third and the fifth upwards, the fifth held, as loud as the lines cleared`,
`LocksmithSoundsTest › the bass plays the root two octaves down for the whole arpeggio, on the triangle, as loud as the game over`,
`LocksmithSoundsTest › the arpeggio and the bass of a chord are two distinct effects, so that they play on two tracks`,
`LocksmithSoundsTest › a chord whose bass would fall out of the range of the basses, C2 to C4, is refused`,
`LocksmithSoundsTest › the bass of a chord on C4 to C6 is in the range of the basses, its ends included`,
`LocksmithSoundsTest › every triad on C4 to B4 has an arpeggio and a bass in the range of the basses`,
`LocksmithSoundsTest › the opening flourish climbs the tonic chord, C5 to C6, in 420 ms, as loud as four lines cleared`,
`LocksmithSoundsTest › the refusal falls a semitone, C3 then B2, in 180 ms on the square`,
`LocksmithSoundsTest › every locksmith effect stays within the volume of the effects on its waveform`.

Pour situer : la mélodie des musiques est à 0,07 et la basse à 0,16. Ces volumes sont ensuite multipliés par le gain du
réglage « Volume des effets » ou « Volume de la musique » (P5.68, `docs/regles-mode-classique.md`,
Préférences v2) : chaque lecteur prend un `Volume` par l'interface `VolumeControl`, et les sorties
(`AudioSink`, pistes de fichier et d'effets) un gain, réappliqué à une sortie rouverte.

- **Donnée pure** dans `core:music` (`SoundEffect`, `Blip`, `Envelope`, catalogue `SoundEffects`),
  testée sur la JVM ; chaque bip monte et descend sur quelques millisecondes pour ne pas claquer.
- **Lecture** dans `core:audio` (`SoundEffectPlayer`, `SoundEffectTracks`) : chaque effet est rendu
  une fois, au taux d'échantillonnage natif de la sortie (`PROPERTY_OUTPUT_SAMPLE_RATE`, 48 000 Hz
  à défaut), dans sa propre piste `AudioTrack` statique, puis seulement rejoué ; deux effets
  différents se superposent, un effet rejoué repart du début (coupure nette possible). Les pistes
  demandent le mode basse latence, que le système peut refuser. Elles sont préparées dès que la
  partie sait les effets activés, et vivent autant que le processus : cinq petits tampons pour le
  Classique, et jusqu'à douze de plus pour le Serrurier (cinq accords en arpège et basse, la
  serrure ouverte, le refus), nombre à mesurer sur appareil (ADR 0025).
- **Sans focus audio** (`USAGE_GAME`, `CONTENT_TYPE_SONIFICATION`) : le système mixe l'effet
  par-dessus la musique sans la couper.
- **Échecs** silencieux et journalisés : un rendu, une ouverture ou une lecture qui échoue ne
  remonte jamais à la partie ; l'effet n'est pas réessayé avant 5 s, son rendu gardé.
- La traduction des événements du moteur en effets vit dans `feature:game` (`soundEffectFor`),
  comme celle des vibrations.

**Tests :** `SoundEffectTest › an effect lasts the sum of its blips`,
`SoundEffectTest › an effect without blips is refused`,
`SoundEffectTest › a blip lasts a positive number of milliseconds`,
`SoundEffectTest › the volume of an effect is above 0 and at most 1`,
`SoundEffectTest › an envelope that rises or falls over a negative time is refused`,
`SoundEffectTest › an envelope longer than a blip is refused, one as long as the shortest blip is accepted`,
`SoundEffectsTest › the rotation is a single E6 of 30 ms on the square, the quietest effect, as it comes so often`,
`SoundEffectsTest › the lines cleared rise up the chord of C, C6, E6, G6, in 160 ms on the square`,
`SoundEffectsTest › four lines cleared rise up the chord of C over two octaves, C5 to C7, in 450 ms on the square`,
`SoundEffectsTest › the game over falls down the chord of C, G4, E4, C4, G3, in 600 ms on the triangle`,
`SoundEffectsTest › every effect lasts from a few tens to a few hundred milliseconds`,
`SoundEffectsTest › every blip rises and falls over a few milliseconds, so that none clicks`,
`SoundEffectRenderTest › an effect lasts its milliseconds at the sample rate`,
`SoundEffectRenderTest › each blip rises over the attack from silence to the volume, holds it, then falls over the release`,
`SoundEffectRenderTest › a blip plays its pitch on the waveform of the effect`,
`SoundEffectTracksTest › an effect is rendered and its track opened at its first play, then each play replays the same track`,
`SoundEffectTracksTest › each effect has its own track, so that an effect playing over another does not cut it`,
`SoundEffectTracksTest › play returns at once, the effect is opened and played on the scope of the player`,
`SoundEffectTracksTest › several effects played before the scope runs open one track each`,
`SoundEffectTracksTest › warming up renders and opens the effects on the scope without playing them, their first play only replays`,
`SoundEffectTracksTest › an output that cannot open plays nothing, reports its failure, and opens at a play 5 s later`,
`SoundEffectTracksTest › after an output that cannot open, the effect is not tried again for 5 s, its rendering kept`,
`SoundEffectTracksTest › a track that fails to play reports its failure, is released, and another one opens at a play 5 s later`,
`SoundEffectTracksTest › a track that fails to play, then to be released, reports both failures and never reaches the caller`,
`SoundEffectTracksTest › a rendering that fails plays nothing and reports its failure`,
`EffectSampleRateTest › the effects are rendered at the sample rate of the output, so that it may play them on its fast path`,
`EffectSampleRateTest › an output sample rate that is not a positive number gives 48 000 Hz`,
`EffectSampleRateTest › an output that does not tell its sample rate gives 48 000 Hz`,
`StaticAudioTrackTest › every effect opens, plays, plays again over itself, and is released without failing`.
À écouter sur un téléphone.

## Accélération

Quand la pile atteint une hauteur critique, le tempo augmente de **× 1,25**, et revient à la
normale (× 1,0) quand elle redescend. Le domaine émet un événement `StackDanger(on/off)`, le
ViewModel le traduit en `setTempoFactor()`.

**Dans le jeu** (P4.11) : un seul lecteur (`MusicPlayer`, implémenté par `ChiptuneSequencer` sur
`AudioTrackSink`) pour tout le processus, créé par l'`AppContainer`. Au début de la partie, la
musique personnelle choisie pour les parties (« Mes musiques », P4.22b), lue une fois par
partie, est jouée en priorité, même si l'accueil est sur *Aucune* : dans la maquette (V1), elle
est un choix de l'accueil exclusif des autres. Sans elle, la musique choisie à l'accueil est
jouée au tempo normal (*Korobeïniki*, *Menuet* ; *Aucune* ne joue
rien ; *Mon fichier* joue le fichier choisi, voir plus bas), sauf si la partie est déjà en pause ou
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
jeu, et il est journalisé. Toute nouvelle lecture (`play`) part au tempo normal (P5.10) : l'aperçu
*Écouter* de l'accueil, une nouvelle partie et Rejouer ne sont jamais accélérés par une partie
finie ou quittée en danger (fin de partie, Pause puis Menu). La reprise après une pause garde le
tempo en cours, × 1,25 si la pile est en danger.

**Mon fichier** (P4.6) : le joueur choisit un fichier audio de l'appareil par le sélecteur du
système (documents locaux seulement, `audio/*`), avec une permission persistante ; la
permission d'un fichier précédent est rendue. L'URI est gardée dans un fichier DataStore à part
(`music_file.preferences_pb`), exclu de la sauvegarde : la permission ne suit pas l'appareil et
l'identifiant révélerait le nom d'un fichier. Si un fichier est déjà retenu, toucher *Mon
fichier* le choisit directement ; l'action *Changer* (lue « Changer de fichier ») rouvre le
sélecteur, à côté d'*Écouter*. Annuler le sélecteur garde le choix précédent. Le fichier est lu
par `MediaPlayer`, en boucle, et reprend à sa position après une pause ; il garde sa vitesse
(l'accélération du danger ne vaut que pour les partitions). Une partition et un fichier sont
deux sortes de `Music` (`core:music`), jouées par un seul lecteur (`MusicSwitch` derrière le
focus audio, [ADR 0018](adr/0018-une-musique-partition-ou-fichier.md)). **Repli sur
Korobeïniki** : sans fichier retenu (y compris des réglages restaurés sur un autre appareil),
pour un fichier qui ne s'ouvre plus ou ne se décode pas, à la lecture comme à la reprise. Un
fichier qui échoue après avoir commencé à jouer se tait (la partie rend le focus, P5.35) et
réessaie à la reprise. La musique personnelle de « Mes musiques » garde la priorité en partie
(voir P4.22d). Vue : V1 (maquette à mettre à jour). À essayer sur un téléphone.

**Tests :** (plus `MediaPlayerTracksTest`, instrumenté : boucle, reprise à une position, document illisible)
`MusicFileRepositoryContract › without a music file chosen there is none`,
`MusicFileRepositoryContract › the music file chosen is read back`,
`MusicFileRepositoryContract › a music file chosen again replaces the one before`,
`MusicFileRepositoryContract › the music file flow emits each change once, and nothing when the same file is chosen again`,
`DataStoreMusicFileRepositoryTest › the music file survives a new repository on the same file`,
`DataStoreMusicFileRepositoryTest › the uri is stored as text under a key that must not change`,
`DataStoreMusicFileRepositoryTest › a blank uri in the file gives no music file`,
`DataStoreMusicFileRepositoryTest › a corrupted file gives no music file, and a new one is kept`,
`DataStoreMusicFileRepositoryTest › a file that cannot be read gives no music file`,
`DataStoreMusicFileRepositoryTest › a failure other than an IOException while the music file is read is not absorbed`,
`MusicSwitchTest › a score plays on the score player`,
`MusicSwitchTest › a music file plays on the file player`,
`MusicSwitchTest › a music file played while a score plays stops the score`,
`MusicSwitchTest › a score played while a music file plays stops the file`,
`MusicSwitchTest › a music file that cannot be opened plays the fallback score in its place`,
`MusicSwitchTest › a score resumed while a music file plays stops the file`,
`MusicSwitchTest › a music file resumed while a score plays stops the score`,
`MusicSwitchTest › stopping a music file replaced by a score leaves the score playing`,
`MusicSwitchTest › the tempo goes to the scores, a music file plays at its own speed`,
`MusicSwitchTest › a music file that opens but cannot be decoded plays the fallback score in its place`,
`MusicSwitchTest › a music file that cannot be decoded resumes its fallback score`,
`MusicSwitchTest › a music file that cannot be decoded once resumed plays the fallback score`,
`MusicSwitchTest › a music file resumed once it can no longer be opened plays the fallback score`,
`MusicSwitchTest › a music file that cannot be decoded after it was replaced plays nothing in place of the new music`,
`MusicSwitchTest › a music file played after a fallback stops the fallback score`,
`AudioFilePlayerTest › a file plays from its start`,
`AudioFilePlayerTest › a file that cannot be opened gives no playback, and the file playing goes on`,
`AudioFilePlayerTest › a new play replaces the file playing, whose playback no longer plays`,
`AudioFilePlayerTest › a stopped playback releases its track and no longer plays`,
`AudioFilePlayerTest › stopping a playback that was replaced leaves the new one playing`,
`AudioFilePlayerTest › a stopped playback resumes the file from where it stopped`,
`AudioFilePlayerTest › a playback replaced by another file resumes from where it was replaced`,
`AudioFilePlayerTest › a track that fails while it plays ends its playback once, and its resume plays again from where it stood`,
`AudioFilePlayerTest › a track that fails before it starts tells the file cannot be played, then ends its playback`,
`AudioFilePlayerTest › a track that fails before it starts, once its playback was stopped, tells nothing`,
`AudioFilePlayerTest › a playback resumed once its file can no longer be opened gives no playback`,
`AudioFilePlayerTest › a playback resumed twice plays once`,
`MusicFileTest › a music file is known by the uri of the document the player chose`,
`MusicFileTest › a music file without a uri is refused`,
`MusicFileTest › a score and a music file are both a music to play`,
`HomeViewModelTest › the home screen is loading until the music file is read, so that its listen button never shows up late`,
`HomeViewModelTest › my file can be listened to once a music file is chosen, in the order of the choices`,
`HomeViewModelTest › listening to my file plays the music file chosen and shows it as previewed`,
`HomeViewModelTest › choosing my file with a music file kept chooses it without opening the picker`,
`HomeViewModelTest › choosing my file without a music file opens the picker, and chooses nothing yet`,
`HomeViewModelTest › changing the music file opens the picker`,
`HomeViewModelTest › the picker once opened is no longer asked for`,
`HomeViewModelTest › a music file kept is shown, so that it may be changed`,
`HomeViewModelTest › choosing a music file keeps it and chooses my file`,
`HomeViewModelTest › choosing a music file stops the preview, even that of the file before`,
`HomeViewModelTest › a music file that cannot be kept leaves the music chosen before`,
`MusicScoresTest › my file plays the music file chosen`,
`MusicScoresTest › my file without a file chosen plays Korobeiniki, as on another device the settings were restored on`,
`MusicScoresTest › a music file that cannot be played falls back on the score of my file without a file`,
`MusicScoresTest › the music of a choice is read with the music file kept`,
`BackupRulesTest › the music file of my file never leaves the device`,
`GameViewModelTest › with my file chosen on the home screen the music file of the player plays, its tempo left to the player`.


**Tests (jeu) :** `MusicScoresTest › each music choice plays its score, or none`,
`GameViewModelTest › at the start of the game the music of the player chosen for the games plays instead of the home screen's`,
`GameViewModelTest › without a music of the player chosen for the games the music of the home screen plays`,
`GameViewModelTest › the music of the player chosen for the games plays without waiting for the settings`,
`GameViewModelTest › a game paused before the music of the player was read plays it, its danger tempo prepared, when it resumes`,
`GameViewModelTest › a programming error while the music of the player chosen for the games is read is not swallowed`,
`GameMusicScoreTest › without a music chosen for the games no score is read`,
`GameMusicScoreTest › the score read is the one of the music chosen for the games`,
`ClassicMusicDirectionTest › the music speeds up by a quarter while the stack is in danger, and goes back to its tempo after`,
`ClassicMusicDirectionTest › the danger tempo is prepared as soon as the music starts`,
`ClassicMusicDirectionTest › each change of danger of the game gives its tempo to the player, in order, and nothing else does`,
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
`MusicPlayerContract › stopping a playback that was replaced leaves the new one playing` (contrat commun au séquenceur, au faux et au lecteur à focus : `ChiptuneSequencerTest`, `FakeMusicPlayerTest`, `FocusedMusicPlayerContractTest`),
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
`GameViewModelTest › a game paused before its music started starts it from the start when it resumes`,
`GameViewModelTest › the next game after a game over in danger plays its music at the normal tempo`,
`GameViewModelTest › a game paused in danger goes on at the danger tempo when it resumes`,
`HomeViewModelTest › a preview plays at the normal tempo, though a game left in danger had sped the music up`.

**Changement de tempo dans le séquenceur** (P4.5) : `ChiptuneSequencer.setTempoFactor(facteur)`
(positif et fini ; retenu pour la prochaine reprise, `resume`, si rien ne joue ; un `play` repart
toujours à 1,0, P5.10). Le tampon au nouveau facteur est pris dans le cache de la partition en
cours, où le tempo du danger a déjà été préparé dès le lancement (`prepareTempo`, P5.15), ou
rendu en tâche de fond ; la sortie
reste ouverte. Pour ne jamais couper une note en pleine amplitude (clic), la lecture finit le
temps en cours dans l'ancien tampon et ne bascule qu'au prochain temps où **toutes** les voix
commencent une note (`switchBeats`), au début de ce temps dans le nouveau tampon, retour au début
de la boucle compris. Positions calculées en `Long`, arrondies à l'échantillon (`BeatGrid`).
Jamais de rééchantillonnage : la hauteur ne change pas. Si le rendu n'est pas prêt à la
frontière, l'ancien tempo continue jusqu'à la suivante ; de deux changements rapides, seul le
dernier est rendu et joué ; un rendu de tempo qui échoue laisse jouer l'ancien tempo, et un
`play` après `stop` n'attend pas un rendu en cours.

**Tests (séquenceur) :** `MusicPlayerContract › the tempo factor must be positive and finite`,
`MusicPlayerContract › a new play starts at the normal tempo, though the music played before was faster`,
`MusicPlayerContract › a tempo factor set while nothing plays is not kept for the next play`,
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
d'effacement, sinon à la fin de l'animation, avec `LinesCleared` (et `LevelUp` éventuel, émis entre les deux), pour que le tempo change
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
`ScoresTest › the Korobeiniki bass is the one of docs musique md, its 8 bars in full`,
`ScoresTest › a melody only uses the pitch classes of its key`,
`ScoresTest › every voice stays within its range`.

> **[à vérifier] (droits)** : la première version de cette partition (syncope en mesure 5,
> notes répétées, rythme pointé perdu) ressemblait à la version popularisée par le jeu de 1989 ;
> elle a été remplacée en P4.9 par la mélodie du chant ci-dessus. Les deux sources nettes
> consultées sont des notations modernes, probablement de la même main ; une troisième
> ([a-pesni.org](http://a-pesni.org/popular20/ojpolna.htm), recueil de romances, sol mineur)
> confirme le rythme sans être lisible note à note.
>
> Éditions d'époque trouvées en P4.10 : A. Tchesnokov, *Variations sur le thème du chant russe
> « Korobotchka »*, op. 4, Moscou, P. Jurgenson, cotage 32220, [1907], Thème, p. 3
> ([archive.org](https://archive.org/details/imslp-on-korobotchka-op4-chesnokov-aleksandr)) ;
> M. I. Nikolaïevski, *Fantaisie sur « Ой, полна, полна коробушка » (Коробейники)*, 5ᵉ éd.,
> Moscou, A. Seywang, [avant 1918], p. 2 (RGB, notice 01004477157). Elles confirment le rythme
> pointé et les mesures 3 à 8 (Tchesnokov met D5 au lieu de F5 en 2ᵉ note de la mesure 6). Les
> mesures 1 et 2 ne sont pas attestées note à note : absentes chez Tchesnokov, variante plus
> resserrée chez Nikolaïevski (fin de m1 B4 1½ · G#4 ½ ; m2 A4 1½ · B4 ½ · C5 1 · B4 ½ · A4 ½).
> La mention reste, par décision du développeur ; non trouvés : Prigojy (1898), Panina (vers
> 1905), НЭБ inaccessible.
