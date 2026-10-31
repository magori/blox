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
`HomeViewModelTest › only the musics with a score, and the live music, can be listened to`,
`HomeViewModelTest › listening to a music plays its score and shows it as previewed`,
`HomeViewModelTest › listening again to the music being previewed stops it`,
`HomeViewModelTest › listening to another music stops the preview and plays the other`,
`HomeViewModelTest › a music without a BloX score cannot be listened to`,
`HomeViewModelTest › choosing another music stops the preview`,
`HomeViewModelTest › choosing the music being previewed lets it play on`,
`HomeViewModelTest › playing stops the preview`,
`HomeViewModelTest › the preview stops when the screen goes to the background, never the music of a game started since`,
`ScreenHiddenEffectTest › a pause alone, behind a dialog or in a split screen, hides nothing`,
`HomeViewModelTest › leaving the home screen stops the preview`,
`HomeViewModelTest › leaving the home screen with no music listened to lets the music of the game play, without any stop`,
`ListenFaceTest › the listen button shows a triangle, and a square while playing, read Écouter or Arrêter with the name of the music`,
`ListenButtonTest › a listen button is a button of 48 dp or the size given, read with the name of its music, or its verb alone`,
`ScoresTest › each arrangement is known by its own identifier`. Vue : V1.

**Identités** (P11.15) : `ScoreId` est scellée. `KOROBEINIKI` et `MINUET` désignent les arrangements ;
`ScoreId.Personal(key)` désigne tout le reste, soit une musique du joueur (clé UUID donnée à
l'enregistrement par `DataStoreMusicLibrary`), soit une musique composée par un jeu et jamais gardée,
sous une clé fixe qui lui est propre : `live-calm` et `live-tense` (`LiveMusic.CALM_ID`, `TENSE_ID`),
`garden` (`GARDEN_SCORE_ID`), `tempo-accompaniment` (`Accompaniment`). Pas d'identité dédiée aux
musiques de jeu : elles ne croisent jamais celles du joueur. Une clé de jeu n'est pas un UUID ; la
bibliothèque ne connaît que les musiques qu'elle a enregistrées (choisir pour les parties une musique
absente est refusé), si bien qu'aucune musique de jeu n'est listée dans Mes musiques, renommée,
enregistrée ni choisie pour les parties ; le séquenceur reconnaît une partition à toute la `Score`
(notes comprises), pas à son seul identifiant. Chaque musique de jeu a sa propre clé fixe, distincte
de celle des autres.

**Tests :** `MusicLibraryContract › a music that is not saved cannot be chosen for the games`,
`GameMusicKeysTest › the musics composed by the games each have their own key`.

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

**Feuille Musique** (P5.76, `feature:home`, ADR 0029, V27) : dans la carte des réglages, la rangée Musique montre,
après l'étiquette « Musique », Écouter (`ListenButton`) pour la musique retenue, la note barrée (`NOTE_OFF`, que
TalkBack ne lit pas) pour *Aucune*, ou une place vide de 48 dp pour *Mon fichier* sans fichier ; puis le nom de la
musique et un chevron, bouton lu « Musique : X. Choisir une autre musique », qui ouvre une feuille du bas (`BloxSheet`,
ouverture gardée par l'écran). La feuille offre Korobeïniki, Menuet, Mon fichier, la musique du joueur pour les
parties (s'il en a une) et Aucune, en `SheetChoiceRow` cochées, chacune avec sa ligne (*Mon fichier* : le nom du
fichier gardé, « Un fichier audio de l'appareil » si ce nom n'est pas connu, ou « Choisir un fichier audio » sans
fichier ; textes du développeur) et Écouter quand elle
s'écoute. Choisir enregistre sans fermer ; *Mon fichier* sans fichier ouvre le sélecteur du système. *Changer*
n'est que dans la feuille, sur la rangée *Mon fichier* qui a un fichier, entre le choix et Écouter, ou sous la
rangée, au bout, quand la place manque (police agrandie, fenêtre étroite). Un seul aperçu à la fois ; fermer la
feuille (retour ou voile) l'arrête. Écarts avec la maquette : *Changer* absent de la carte (trop étroit à 360 dp),
pas de croix dans le titre de la feuille, lignes sur deux lignes au lieu d'être coupées (sauf le nom du fichier),
*Changer* sous la rangée *Mon fichier*, au bout, dès que le nom du fichier est affiché (à 360 dp en police normale
aussi ; la maquette le met à côté du nom, qui serait alors coupé : P5.116).
**Nom du fichier** (P5.38) : sous *Mon fichier*, le nom que montre le sélecteur du système (« orage-d-ete.ogg »,
`OpenableColumns.DISPLAY_NAME`), sur une ligne, à la taille de la police (×2 compris), coupé au milieu par une ellipse
s'il est trop long, pour garder son début et son extension ; *Changer* passe sous la rangée plutôt que de couper un
nom qui tiendrait seul (`SheetChoiceRow`, `oneLineSubLine` : la ligne demande sa largeur entière) ; TalkBack le lit en entier, avec le choix (« Mon fichier,
orage-d-ete.ogg, case d'option »). Il est lu **une fois, au choix du fichier** (`MusicFileNames`, `core:data` ;
implémentation `DocumentMusicFileNames` dans `app`, requête du `ContentResolver` hors du fil principal, attendue 2 s
au plus : le choix attend le nom, et un fournisseur muet ne doit pas le retenir ; passé ce délai, pas de nom), puis gardé
avec l'URI dans `music_file.preferences_pb` (clé `name`) : l'accueil ne redemande jamais le document, et le nom reste
même si la permission se perd ou si le fichier est supprimé (le repli sur Korobeïniki, lui, ne se découvre qu'à la
partie). Un nom illisible (document hors d'atteinte, sans fournisseur, sans nom ou nom vide) n'empêche pas le choix :
le fichier est gardé sans nom. Sans nom connu (fichier choisi avant P5.38, ancien fichier de préférences sans la clé
`name`, relu tel quel), la ligne reste « Un fichier audio de l'appareil ». Le nom, comme l'URI, ne quitte pas
l'appareil : le fichier est déjà hors des règles de sauvegarde (`BackupRulesTest` inchangé).
**Tests :** `MusicFileRepositoryContract › the name of the music file chosen is read back with it`,
`MusicFileRepositoryContract › a music file chosen without a name has none, even when it was kept with one before`,
`MusicFileRepositoryContract › another music file chosen with its name replaces the name of the one before`,
`MusicFileRepositoryContract › the named music file flow emits each change once, and nothing when the same file is chosen again`,
`ReadWithinTest › a read that answers in time gives its answer`,
`ReadWithinTest › a read that does not answer in time gives nothing, without waiting for it any longer`,
`DataStoreMusicFileRepositoryTest › a music file kept by an earlier version, without a name, is read back without one`,
`DataStoreMusicFileRepositoryTest › the name is stored as text under a key that must not change`,
`HomeViewModelTest › choosing a music file keeps the name the picker shows, shown under my file`,
`HomeViewModelTest › choosing a music file whose name cannot be read keeps it without a name, shown without one`,
`DocumentMusicFileNamesTest › the name of an audio file of the device is its display name, and it has none once deleted`,
`DocumentMusicFileNamesTest › a document out of reach, its permission lost or never given, has no name`,
`DocumentMusicFileNamesTest › a document of no provider, or no document at all, has no name`,
`DocumentMusicFileNamesTest › the name is the display name of the first row; none without a row, a column, or with a blank one`,
`SheetChoiceRowTest › a sub-line kept on one line, too long for it, is cut in its middle, and read whole by TalkBack`,
`HomeScreenTest › in the sheet, Mon fichier with a file shows its name under it, read by TalkBack with the choice`,
`HomeScreenTest › in the sheet, Mon fichier with a file of unknown name says it is an audio file of the device`,
`HomeScreenTest › in a normal font, on 360 dp, a name of a file stands whole under Mon fichier, Changer under the row`,
`HomeScreenTest › in a font twice as large, a long name of a file holds one line under Mon fichier, cut in its middle, apart` ;
captures `HomeMusicSheet*`. Vue : V1.
**Rappel sous Jouer** (décision du développeur) : il nomme la musique qui jouera ; *Mon fichier* sans
fichier gardé joue Korobeïniki (`MusicChoice.MY_FILE_FALLBACK`, `core:data`, partagé avec `app`), et le rappel le dit,
tandis que la rangée de la carte garde « Mon fichier », le réglage choisi. Un fichier gardé mais illisible n'est
découvert qu'à la partie : le rappel dit alors encore « Mon fichier ».
**Tests :** `HomeScreenTest › choosing a music saves it and keeps the sheet open`,
`HomeScreenTest › listening to a music stops the one previewed`,
`HomeScreenTest › closing the sheet stops the preview`,
`HomeScreenTest › Aucune shows the note barred`,
`HomeScreenTest › the sheet is read as a dialog by TalkBack`,
`HomeScreenTest › with Mon fichier chosen without a file, the reminder under Jouer names Korobeïniki, played in its place`,
`HomeScreenTest › with Mon fichier chosen without a file, the row Musique keeps its name, after an empty slot of 48 dp`,
`HomeScreenTest › in the sheet, Mon fichier with a file offers Changer, which opens the picker, before its listen button`,
`HomeScreenTest › in a normal font, Changer stands beside Écouter, before it, apart from the name Mon fichier`,
`HomeScreenTest › in a font twice as large, the row Mon fichier keeps its words whole, Changer beside or under Écouter`,
`HomeScreenTest › in a font twice as large, in a narrow window, Changer goes under Écouter, at the end of the row, apart`,
`HomeScreenTest › in the sheet, a music of the player for the games is the only music selected, read with its sub-line`,
`HomeViewModelTest › closing the sheet stops the preview`,
`HomeUiStateTest › with Mon fichier chosen but no music file kept, the reminder names Korobeiniki, played in its place`,
`HomeUiStateTest › with Mon fichier chosen and a file kept, the reminder names Mon fichier`,
`MusicScoresTest › the fallback of my file is a music with a BloX score` ;
captures `HomeMusicSheet*`.

**Musique personnelle à l'accueil** (P4.22d) : la musique de « Mes musiques » retenue pour les
parties apparaît dans la feuille Musique (P5.76), sous son nom, avant *Aucune*, comme le seul choix
sélectionné, avec la ligne « Ma musique · en jeu ». Elle s'écoute (*Écouter*) comme les
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
`HomeScreenTest › in the sheet, a music of the player for the games is the only music selected, read with its sub-line`,
`HomeScreenTest › listening to the music of the player for the games calls for it`,
`HomeScreenTest › the music of the player being listened to offers to stop it`. Vue : V1.

Prévu : des musiques composées dans l'application, à la main ou par le générateur
(bouton *Au hasard*, par catégorie). Spécification : [generateur-musique.md](generateur-musique.md).

Un sixième choix, *Vivante* (P8, proposé dans la feuille depuis P8.7, entre *Menuet* et *Mon fichier*),
une musique composée pour chaque partie, calme puis tendue quand la pile monte, qui accélère avec les
niveaux ; *Écouter* y joue la musique calme d'une graine tirée au hasard **[à vérifier]**, et la carte
Pause nomme son ambiance. Spécification et tests : [musique-vivante.md](musique-vivante.md) ; la Graine du jour l'impose
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
- **Ondes des instruments** (P14.4, `Tone`, voir Instruments) : sinus (`sin 2π·phase`) ; impulsion d'un quart
  (+1 sur le premier quart de la période, −1 ensuite) et dent de scie (de −1 à 1 sur la période), **limitées en
  bande par PolyBLEP** : chaque saut est arrondi sur l'échantillon qui le précède et celui qui le suit, ce qui
  retire environ 15 dB du repliement (les harmoniques au-delà de la fréquence de Nyquist, qui reviendraient en
  fréquences parasites). La carrée et la triangle restent **sans** correction : elles rendent exactement les
  échantillons d'avant (empreintes de P14.2). L'impulsion garde sa composante continue (moyenne −0,5) ; à revoir à
  l'écoute (P14.16) si la basse de Néon le demande.
- **Enveloppe** : chaque note joue à volume constant pendant 80 % de sa durée, puis s'éteint en
  fondu linéaire jusqu'à 95 %, pour séparer les notes répétées ; elle est muette ensuite. C'est la forme `Gate`
  d'Arcade, dont `Synthesizer` lit les bornes (`Gate.heldUntil`, `Gate.silentFrom`, `core:music`).
- **Forme de note des instruments** (P14.5) : `Synthesizer.render(score, tempoFactor, style)` joue chaque voix sur
  l'instrument que `style` donne à sa `Waveform` (par défaut `SoundStyle.ARCADE` : aucun appelant ne change) ;
  ton de l'instrument et forme de ses notes, `Gate` ou `Adsr`. Une `Adsr` monte linéairement du silence en
  `attackMillis`, descend linéairement jusqu'à `sustain` en `decayMillis`, le tient, puis s'éteint linéairement
  vers le silence sur les `releaseMillis` **qui finissent la note** (temps arrondis à l'échantillon) : l'extinction
  se termine sur le dernier échantillon de la note et ne déborde jamais sur la suivante. Dans une note plus courte
  que l'extinction, celle-ci est raccourcie et part du niveau atteint, une fois l'attaque finie. Le gain de
  l'instrument (tous à 1, P14.17) n'est pas encore appliqué.
- **Filtre de l'instrument** (P14.6) : un instrument qui a un `lowPass` fait passer **toute sa voix**, une fois
  rendue, par un passe-bas à un pôle de cette fréquence de coupure : chaque échantillon de sortie s'approche du
  précédent vers l'entrée d'une fraction `1 − e^(−2π × coupure / taux)`, en partant du silence. Une constante passe
  inchangée, un ton à la fréquence de coupure sort environ 3 dB plus bas, et l'atténuation croît d'environ 6 dB par
  octave au-dessus. Un instrument sans filtre (`lowPass` nul, Arcade) n'est pas touché.
- **Durée** : une note dure `temps × 60 / bpm × fréquence` échantillons ; les bornes de chaque
  note se calculent depuis le temps cumulé, si bien qu'une voix entière dure exactement sa
  durée totale, sans dérive d'arrondi.
- **Mixage** : `mix` additionne les voix, échantillon par échantillon, puis écrête à [−1, 1].
  Les voix doivent avoir la même longueur (garanti par `Score`).
- **Écho du style** (P14.7) : un style qui a un `Echo` l'applique **au mixage**, une fois les voix additionnées. Le
  son revient après `delayBeats` temps, compté au tempo joué (`sampleAt(delayBeats, bpm × facteur)`) : à × 1,25,
  l'écho reste sur la mesure. Chaque retour vaut `feedback` fois le précédent, et la somme des retours s'ajoute au son
  à la part `wet`. L'écho est **circulaire** : ce qui revient au-delà de la fin du tampon résonne sur son début, comme
  à la reprise de la boucle, si bien que la boucle reste sans raccord et garde la longueur de la partition. Le calcul
  rejoue le tampon assez de fois pour que les retours venus d'avant son début tombent sous 10⁻⁶ de leur niveau. La
  somme est écrêtée à [−1, 1] ; avec les volumes des partitions et les échos des styles (retour au plus 0,3, part au
  plus 0,25), elle n'a jamais à l'être. Arcade n'a pas d'écho.
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
`MixTest › mixing no track is refused`, `MixTest › mixing tracks of different lengths is refused`,
`ToneTest › a sine at 440 Hz crosses zero rising every 100 point 2 samples`,
`ToneTest › a pulse of a quarter is high for a quarter of its period`,
`ToneTest › a band-limited saw stays within -1 and 1 and loses its aliasing above the Nyquist frequency`,
`ToneTest › a band-limited pulse of a quarter loses its aliasing above the Nyquist frequency`,
`ToneTest › the square and the triangle render as before`,
`SynthesizerTest › an ADSR rises over its attack, falls to its sustain over its decay, and releases at the end of the note`,
`SynthesizerTest › a release never runs into the next note of the voice`,
`SynthesizerTest › a release longer than the note starts once the attack is over`,
`ScoreRenderTest › a note still lasts its beats times 60 over the tempo, whatever the style`,
`LowPassTest › a constant signal passes the low-pass unchanged`,
`LowPassTest › a tone at the cutoff comes out about 3 dB lower`,
`LowPassTest › a tone far above the cutoff is attenuated`,
`LowPassTest › an instrument without filter is not changed`,
`EchoMixTest › a click comes back after the delay of the echo at its feedback`,
`EchoMixTest › the delay of the echo follows the tempo, in beats`,
`EchoMixTest › the echo of the end of the loop sounds at its start, so that the loop has no seam`,
`EchoMixTest › the rendering keeps the length of the score`,
`EchoMixTest › the echo never takes the mix beyond -1 and 1`.

## Lecture en boucle

- **Rendu** : `Synthesizer.render(score, tempoFactor = 1.0, style = SoundStyle.ARCADE)` est la seule entrée
  publique ; il mixe toutes les voix et dure exactement la partition. Le tempo est un `Double` (150 × 1,25 = 187,5
  accepté) ; le facteur doit être positif et fini. Les durées restent en
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
`DataStoreFileContract › an unreadable file reads as an empty one, and what is written next is kept`,
`DataStoreFileContract › a file that cannot be read reads as an empty one`,
`DataStoreFileContract › a failure other than an IOException while the file is read is not absorbed`,
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

## Sonnerie ou réveil

Spécifiée en P13.1 ([ADR 0031](adr/0031-sonnerie-et-reveil.md)). Codés : le synthétiseur de la musique, un seul
dans `AppContainer` à `MUSIC_SAMPLE_RATE` (public dans `core:audio`, P13.2), la longueur, le niveau, le format et le
nom du fichier (P13.3 à P13.5, fonctions pures de `core:audio`), l'écriture par `MediaStore` (P13.6,
`RingtoneExporter`, que `AppContainer` construit sans encore le donner à un écran) ; **pas encore** l'interface
(P13.7 à P13.14). Vues : V42
(feuille « Sonnerie ou réveil »), V32 et V33 (menus « … »), dans [maquette.md](maquette.md).

**Périmètre**

- Seules les musiques de « Mes musiques » deviennent une sonnerie ou un réveil. Ni *Korobeïniki* (confirmation des
  droits encore ouverte, P4.10, et associée au jeu de 1989), ni le *Menuet* (on n'exporte que ce que le joueur a
  créé), ni les musiques composées par un jeu (Musique vivante, Jardin, accompagnement d'Au tempo), ni « Mon
  fichier ».
- Deux usages : **Sonnerie** (appels entrants) et **Réveil** (pour l'application Horloge). Pas de notification :
  un son de notification dure moins de 3 s et ne boucle pas.
- **Android 10 (API 29) et plus** seulement : avant l'API 29, la ligne « Sonnerie ou réveil » n'apparaît dans aucun
  menu.
- Points d'entrée : le menu « … » d'une musique de Mes musiques et celui de l'éditeur. L'éditeur exporte la
  version **enregistrée** de la musique.

**Le fichier**

- Rendu par le synthétiseur de la musique (`Synthesizer`, `MUSIC_SAMPLE_RATE`, 44 100 Hz), au tempo de la musique,
  dans l'instrument choisi dans la feuille (Instruments, ci-dessous ; présélectionné sur le réglage). Un seul
  synthétiseur de musique, dans `AppContainer`, sert le séquenceur et l'export : le fichier sonne comme le jeu. Le
  fichier est rendu, découpé et encodé avant toute écriture.

  **Tests :** `RingtoneWriterTest › the exporter renders the score with the synthesizer of the music, at its tempo`,
  `RingtoneWriterTest › the file holds the ringtone samples of the rendering, encoded as WAV`,
  `RingtoneWriterTest › a music longer than 60 s is cut at its last whole bar, at the tempo of the score`.
- **Longueur** : des boucles entières de la musique, le plus petit nombre qui dure **au moins 20 s**. Une boucle de
  20 à 60 s est gardée une fois ; une boucle de moins de 20 s est répétée (le fichier dure alors moins de 40 s) ; une
  boucle de plus de 60 s est **coupée à sa dernière mesure entière avant 60 s** : le fichier ne dépasse jamais 60 s
  (5,3 Mo), et le système le reprend au début d'une mesure. **Sans fondu** : le système rejoue le fichier en
  boucle ; fait de boucles entières, il se raccorde sans couture. Une mesure dure rarement un nombre entier
  d'échantillons (`samplesPerBar` est un `Double`) : la coupe tombe sur l'échantillon le plus proche de la fin de la
  dernière mesure entière, comme le synthétiseur place le début de chaque note. `ringtoneSamples` refuse un rendu
  vide, et une mesure nulle, négative ou de plus de 60 s (aucune mesure entière n'y tiendrait).
- **Niveau** : la crête est normalisée à **−1 dBFS** (0,891) ; les musiques culminent vers 0,23 (environ −13 dBFS)
  dans le jeu. Tous les échantillons sont multipliés par le même gain. Un rendu silencieux reste silencieux.

  **Tests :** `RingtoneSamplesTest › a loop shorter than 20 seconds is repeated until the ringtone lasts at least
  20 seconds`, `RingtoneSamplesTest › the ringtone holds only whole loops, so that it loops without a seam`,
  `RingtoneSamplesTest › a loop of 20 to 60 seconds is kept once`,
  `RingtoneSamplesTest › a loop longer than 60 seconds is cut at its last whole bar before 60 seconds`,
  `RingtoneSamplesTest › the ringtone never lasts more than 60 seconds`,
  `RingtoneSamplesTest › the loudest sample is brought to minus 1 dBFS`,
  `RingtoneSamplesTest › a silent rendering stays silent, without dividing by zero`,
  `RingtoneSamplesTest › an empty rendering is refused`,
  `RingtoneSamplesTest › a bar of no length or less is refused`,
  `RingtoneSamplesTest › a bar longer than 60 seconds is refused`.
- **Format** : WAV PCM 16 bits, mono, 44 100 Hz (en-tête RIFF `WAVE`, morceau `fmt ` PCM, morceau `data`, petit
  boutiste) ; un échantillon de 1 s'écrit 32 767, de −1 −32 767, au-delà de [−1, 1] il est écrêté. De 1,8 à 5,3 Mo.
  `WavEncoder` refuse lui aussi un rendu vide (un fichier sans échantillon).

  **Tests :** `WavEncoderTest › the header is RIFF WAVE with a fmt chunk of PCM, mono, 16 bits, at the sample rate
  given`, `WavEncoderTest › the RIFF and data sizes match the number of samples`,
  `WavEncoderTest › a sample of 1 is written as 32767 and a sample of -1 as -32767, little endian`,
  `WavEncoderTest › a sample beyond -1 or 1 is clipped`, `WavEncoderTest › zero is written as zero`,
  `WavEncoderTest › an empty rendering is refused`.
- **Nom** : le nom de la musique, extension `.wav` ; les caractères interdits dans un nom de fichier
  (`/ \ : * ? " < > |`, ceux de FAT, et les caractères de contrôle) sont remplacés par `_` ; un nom sans lettre ni
  chiffre (vide, fait seulement d'espaces ou de symboles) donne « BloX ». Un point en tête est remplacé lui aussi par
  `_` (« .xyz » donne « _xyz.wav ») : un fichier caché, `MediaStore` ne le montre pas, ni le sélecteur du système (vu
  sur Android 17). Le nom n'est pas tronqué : un nom de Mes musiques tient en 40 caractères au plus
  (`SavedMusic.MAX_NAME_LENGTH`), soit 160 octets en UTF-8, loin des 255 octets d'un nom de fichier.
- **Titre** : celui que lit le sélecteur du système, que `MediaStore` tire du nom du fichier, sans l'extension. Une
  application ne peut pas le changer (une mise à jour de `TITLE` ne change aucune ligne, et celui donné à l'insertion
  est remplacé à la publication ; vu sur Android 17) : le titre est donc le nom de la musique, ses caractères
  interdits remplacés par `_` comme dans le nom du fichier (« Pluie: matin » s'intitule « Pluie_ matin »).

  **Tests :** `RingtoneFileNameTest › the file is named after the music, with the wav extension`,
  `RingtoneFileNameTest › the characters a file name may not hold are replaced`,
  `RingtoneFileNameTest › a name starting with a dot does not make a hidden file`,
  `RingtoneFileNameTest › a name of spaces or symbols only gives BloX`,
  `MediaStoreRingtoneExporterTest › the title of the file is the name of the music`.
- **Où** : `Ringtones/BloX/` (Sonnerie) ou `Alarms/BloX/` (Réveil), écrit par `MediaStore`, marqué comme sonnerie ou
  réveil, sans aucune permission. Chaque export crée un nouveau fichier ; si le nom est pris, le fichier devient
  « Nom (1).wav » et son titre « Nom (1) », pour que le sélecteur du système distingue les deux. Les
  fichiers restent dans le téléphone après la désinstallation de BloX, et quand la musique est supprimée.
- **Écriture** (`MediaStoreRingtoneExporter`, Android 10 et plus) : collection `Audio.Media` du volume principal,
  type `audio/x-wav`, `RELATIVE_PATH` `Ringtones/BloX/` et `IS_RINGTONE`, ou `Alarms/BloX/` et `IS_ALARM` ;
  l'entrée naît en attente (`IS_PENDING`), invisible des autres applications, et n'est publiée qu'une fois son
  fichier écrit en entier et fermé ; une publication qui ne change pas exactement une ligne est un échec. Tout échec
  de `MediaStore` sort en `IOException`, même une `IllegalStateException` ou une `SecurityException`, enveloppée :
  c'est la promesse de `RingtoneExporter`. Une écriture ou une publication qui échoue supprime d'abord l'entrée ; si
  la suppression échoue aussi, son exception est jointe à la première (`addSuppressed`), qui reste celle rendue. Une
  partition dont la mesure dépasse 60 s est refusée (`IllegalArgumentException`) avant toute écriture ; aucune
  musique du joueur n'en approche. Le rendu et l'écriture se font sur le dispatcher donné (`Dispatchers.IO` dans
  `AppContainer`), jamais sur le fil principal : le rendu, quelques secondes de calcul au plus, n'a pas de dispatcher
  à lui. **Annulation** : l'écriture, bloquante, ne s'interrompt pas ; une coroutine annulée pendant qu'elle écrit
  laisse un fichier publié dont l'appelant ne reçoit pas l'URI (`CancellationException`). La feuille (P13.7) ne
  doit donc pas annuler un export en cours, ou accepter ce fichier orphelin.
  `export` rend l'URI `content://` du fichier écrit, en texte (comme `MusicFile.uri`) : un `android.net.Uri` ne se
  construit pas dans les tests sur la JVM, où `FakeRingtoneExporter` (doublures de test de `core:audio`) note les
  exports et peut échouer. Les appels à `MediaStore` passent par `RingtoneStore` (insertion, flux, publication,
  suppression ; `ContentResolverRingtoneStore` en production), si bien que l'échec et le nettoyage se testent sur la
  JVM.

  **Tests :** `RingtoneWriterTest › an entry is published only once its file is whole`,
  `RingtoneWriterTest › a write that fails leaves no entry behind`,
  `RingtoneWriterTest › a write that fails otherwise than on input or output leaves no entry behind, and fails as
  one`, `RingtoneWriterTest › a publication that fails leaves no entry behind`,
  `RingtoneWriterTest › an entry that cannot be made fails as input or output`,
  `RingtoneWriterTest › a removal that fails too keeps the failure of the write`,
  `FakeRingtoneExporterTest › each export of the fake gets a URI of its own`,
  `FakeRingtoneExporterTest › the fake fails while its failure is set, and notes the export asked for`,
  `MediaStoreRingtoneExporterTest › a ringtone is written in Ringtones BloX, marked as a ringtone and no longer
  pending`, `MediaStoreRingtoneExporterTest › an alarm is written in Alarms BloX, marked as an alarm`,
  `MediaStoreRingtoneExporterTest › the file written reads back as the WAV encoded`,
  `MediaStoreRingtoneExporterTest › two exports of the same name give two files, the second titled Nom (1)`.

**Ce que BloX ne fait pas** : il ne règle jamais la sonnerie ni un réveil lui-même (ni `WRITE_SETTINGS`, ni aucune
permission de stockage). Après l'écriture, la feuille propose d'ouvrir les réglages du son (Sonnerie) ou l'Horloge
(Réveil), seulement si une application du téléphone y répond.

**[à vérifier] sur téléphone** (P13.13) : la boucle se raccorde sans claquement dans le sélecteur du système ; le
niveau à côté des sonneries du système ; l'Horloge (Pixel, Samsung si possible) liste bien `Alarms/BloX/` ; ce que
devient un réveil réglé sur un fichier supprimé (Android 10 et plus).

**Écarts** : une boucle de plus de 60 s est coupée, donc ne se raccorde qu'au début d'une mesure, pas à celui de sa
phrase ; pas de mise à jour d'un fichier déjà exporté dans cette première version (tâche facultative P13.14).

## Instruments

Spécifiés en P14.1 ([ADR 0032](adr/0032-instruments.md)). Codés : les empreintes de référence du son d'aujourd'hui
(P14.2), la description des instruments (P14.3, `core:music`), leurs ondes (P14.4) et le rendu d'une partition
dans un style, ton et forme de note (P14.5, voir Synthèse), le filtre de l'instrument (P14.6) et l'écho du style
(P14.7, voir Synthèse), le rendu des effets dans un style (P14.14, voir Effets sonores ci-dessous) ; réglage,
branchement des effets et interface **pas encore codés** (P14.8 à P14.13, P14.15 à P14.20). Vues : V43 (feuille
Instruments), V31 (rangée Instruments des Paramètres), V42 (choix pour un fichier de sonnerie), V27 (sous-ligne de
« Mon fichier »).

**Le réglage**

- « Instruments », un réglage **général** (« Tous les jeux », carte Sons et vibrations, ADR 0030), trois choix :
  **Arcade** (le son d'aujourd'hui, par défaut), **Velours** (doux, apaisant : boîte à musique, lo-fi du soir),
  **Néon** (dynamique, tendance : synthpop, en écho aux néons du thème nuit). Dans le code,
  `enum class SoundStyle { ARCADE, VELVET, NEON }`, enregistré par nom ; un nom inconnu se lit Arcade.
- Pas de choix par musique : `musics.bin` ne change pas. La feuille Sonnerie peut choisir un autre instrument pour
  le fichier seul, sans changer le réglage.
- Un changement vaut **dès la prochaine musique jouée** (prochain lancement, reprise ou changement de musique), effets
  sonores compris : les effets prennent le nouvel instrument en même temps que la musique.

**Ce que l'instrument habille** : toutes les partitions jouées par BloX (Korobeïniki, Menuet, ma musique, Musique
vivante, Graine du jour, Écouter de l'accueil, Mes musiques, éditeur, Jardin, accompagnement d'Au tempo), le fichier
d'une sonnerie, et les effets sonores (Classique, Serrurier, Jardin). **Pas « Mon fichier »**, un fichier audio
joué tel quel.

**Comment** : une voix garde sa `Waveform`, qui dit désormais son **rôle** (`SQUARE` la mélodie, `TRIANGLE` la
basse) ; l'instrument lui donne un **ton** (`Tone` : carrée, triangle, impulsion d'un quart, sinus, dent de scie), une
**forme de note** (`NoteShape` : `Gate`, l'enveloppe d'aujourd'hui, ou `Adsr`, en millisecondes), un passe-bas à un
pôle facultatif et un gain ; le style ajoute un écho facultatif (`Echo` : délai en temps, retour, part), appliqué
au mixage et enroulé sur le début du tampon, si bien que la boucle reste sans raccord.

Règles communes :

- **Arcade rend exactement les échantillons d'avant les instruments**, musiques et effets : des empreintes sont
  figées par des tests sur l'API d'aujourd'hui (P14.2), avant tout changement du synthétiseur, P13.2 compris :
  Korobeïniki, le Menuet et une musique générée, à × 1 et × 1,25, et chaque effet du Classique, du Serrurier et du
  Jardin. Une empreinte est le nombre d'échantillons et la somme de contrôle CRC-32 des échantillons quantifiés en
  16 bits (petit-boutiste) : elle tolère un écart d'amplitude bien au-dessous d'un pas de 16 bits (un calcul
  différent de `volume × onde × gain`), mais **aucun écart de phase** : un échantillon du carré posé sur une
  frontière de sa période bascule de +v à −v (A4 à 48 kHz tombe exactement à la moitié de sa période à
  l'échantillon 600). Arcade garde donc le calcul de phase `(index × fréquence / taux) mod 1`, sans le réécrire en
  `index × (fréquence / taux)` ni accumuler la phase d'un échantillon à l'autre.
  La musique est rendue à 44 100 Hz, les effets à 48 000 Hz (le taux par défaut des effets). Sont couverts : les
  5 effets du Classique (`SoundEffects.all`) ; pour le Serrurier, l'arpège et la basse de chaque accord d'un cadenas
  (degrés I à vi de do majeur sur C4, confrontés aux progressions de `PadlockChords`), l'ouverture et le refus ; pour
  le Jardin, la note de chacune des 25 hauteurs de ses 4 ambiances (confrontées à `GardenMood.allPitches`). La
  musique générée est joyeuse, de graine 2026 : son empreinte suit aussi le générateur et l'algorithme de
  `kotlin.random.Random` (ADR 0014) ; un changement de `MusicGenerator` ou une montée de Kotlin peut obliger à la
  refaire (Arcade n'y est pour rien), ce que
  `FormTest › the seed 42 on joyful gives this form, which pins the order of the draws` signalerait en même temps.
  **Tests :** `SoundFingerprintTest › Korobeiniki renders the samples of its fingerprint, at tempo 1 and 1,25`,
  `SoundFingerprintTest › the Minuet renders the samples of its fingerprint, at tempo 1 and 1,25`,
  `SoundFingerprintTest › a generated music renders the samples of its fingerprint, at tempo 1 and 1,25`,
  `SoundFingerprintTest › every effect of the Classic renders the samples of its fingerprint`,
  `SoundFingerprintTest › every effect of the Locksmith renders the samples of its fingerprint`,
  `SoundFingerprintTest › every effect of the garden renders the samples of its fingerprint`. Rendues dans le style
  Arcade, nommé (P14.5), les trois musiques gardent leurs empreintes :
  `SoundFingerprintTest › the Arcade style renders the fingerprints of P14.2`.
- Chaque instrument **attaque en 5 ms au plus** : le temps reste net dans Au tempo et les retours en jeu restent
  lisibles ; la douceur vient de l'onde, du filtre, de l'extinction et de l'écho.
- Une note dure toujours `temps × 60 / bpm` ; l'extinction d'une `Adsr` tient dans la note, sans déborder sur la
  suivante de la voix.
- Scie et impulsion sont limitées en bande (pas de repliement audible dans les aigus).
- Les trois instruments ont le **même niveau à ±1 dB RMS** (Korobeïniki et les musiques générées de chaque
  catégorie) ; le mixage reste dans [−1, 1].
- L'écho suit le tempo (délai en temps) : il reste sur la mesure quand la musique accélère de × 1,25.

Valeurs de départ, **[à vérifier] à l'oreille** sur un téléphone (P14.16) ; les gains se règlent ensuite par la
mesure du niveau (P14.17), une fois les timbres arrêtés à l'oreille :

- **Arcade** : mélodie (voix `SQUARE`) carrée, basse (voix `TRIANGLE`) triangle, toutes deux en `Gate` (80 % de la
  note tenus, fondu jusqu'à 95 %) ; ni filtre, ni écho.
- **Velours** : mélodie en sinus, `Adsr` 4 / 250 / 0,6 / 200 ms, sans filtre ; basse triangle, passe-bas 600 Hz,
  `Adsr` 5 / 300 / 0,7 / 150 ms ; écho de ¾ de temps, retour 0,3, part 0,25.
- **Néon** : mélodie en dent de scie, passe-bas 2 500 Hz, `Adsr` 2 / 120 / 0,7 / 60 ms ; basse en impulsion d'un
  quart, passe-bas 900 Hz, `Adsr` 2 / 80 / 0,5 / 40 ms ; écho d'un demi-temps, retour 0,2, part 0,2.

(`Adsr` : attaque / déclin / maintien / extinction.)

**Description codée** (`core:music`, P14.3) : `SoundStyle.instrumentFor(waveform)` donne l'`Instrument(tone, shape,
lowPass, gain)` de la voix (`lowPass` : fréquence de coupure en Hz, ou `null` sans filtre) ; `SoundStyle.echo`,
l'écho du style ou `null`. `Gate` tient la note de son début jusqu'à `heldUntil` (0,80) et s'éteint jusqu'à
`silentFrom` (0,95), sans attaque ; `Adsr(attackMillis, decayMillis, sustain, releaseMillis)` refuse un temps négatif
et un maintien hors de [0, 1] ; `Echo(delayBeats, feedback, wet)` refuse un délai qui n'est pas positif, un retour
de 1 ou plus (il ne s'éteindrait jamais) ou négatif, et une part hors de [0, 1] (bornes qui aident l'écho à ne
jamais mener le mixage au-delà de −1 et 1, P14.7). Les gains valent tous 1 en attendant la mesure du niveau (P14.17). Les
valeurs de Velours et de Néon sont celles ci-dessus, figées par un test pour qu'un réglage à l'oreille les change
ici et dans le code ensemble.

**Tests :** `SoundStyleTest › the names of the sound styles are frozen`,
`SoundStyleTest › in the Arcade style the square plays a square gated as today and the triangle a triangle`,
`SoundStyleTest › every style has an instrument for every waveform`,
`SoundStyleTest › every instrument of every style starts within 5 milliseconds`,
`SoundStyleTest › the Arcade style has no echo and no filter`,
`SoundStyleTest › the Velvet style takes the starting values of the specification`,
`SoundStyleTest › the Neon style takes the starting values of the specification`,
`AdsrTest › an ADSR refuses a negative time and a sustain outside 0 and 1`,
`AdsrTest › an ADSR accepts times of 0 and a sustain of 0 or 1`,
`EchoTest › an echo refuses a feedback of 1 or more`, `EchoTest › an echo refuses a negative feedback`,
`EchoTest › an echo refuses a part outside 0 and 1`, `EchoTest › an echo refuses a delay of no beat`.

**Effets sonores** : un effet prend le **ton** et le filtre de l'instrument de sa `Waveform` (`SQUARE` celui de la
mélodie, `TRIANGLE` celui de la basse), mais garde **sa propre enveloppe** (`Envelope`, 1 à 2 ms de montée, voir
Effets sonores) et **aucun écho** : un retour reste court et lisible. Exemple : en Velours, les lignes effacées
montent l'accord de do sur le sinus de la mélodie. À la prochaine musique jouée, les effets rendus dans l'ancien
instrument sont oubliés et rendus de nouveau (branchement : P14.15, **pas encore codé** ; d'ici là, les effets se
jouent en Arcade). Leur niveau suit la même règle de ±1 dB RMS.

Codé en P14.14 : `Synthesizer.render(effect, style = SoundStyle.ARCADE)` rend chaque blip sur le ton de
`style.instrumentFor(effect.waveform)`, avec l'enveloppe de l'effet (pas la forme de note de l'instrument), puis fait
passer l'effet entier par le passe-bas de l'instrument s'il en a un (même filtre que les voix, voir Synthèse) ;
aucun écho, si bien qu'un effet dure exactement ses millisecondes. En Arcade, les effets rendent les empreintes de
P14.2.

**Tests :** `SoundFingerprintTest › in the Arcade style the effects render the fingerprints of P14.2`,
`SoundEffectStyleTest › in the Velvet style the lines cleared play on the tone of the velvet melody`,
`SoundEffectStyleTest › in the Neon style an effect passes the low-pass of its instrument`,
`SoundEffectStyleTest › an effect keeps its own envelope, starting within 5 milliseconds, whatever the style`,
`SoundEffectStyleTest › an effect has no echo, so that it lasts its milliseconds`,
`SoundEffectStyleTest › in every style the effects of the locksmith and of the garden take the tone of their waveform`.

**[à vérifier] à l'oreille** (P14.16) : Velours pâteux et Néon saturé à × 1,25 (pile en danger, niveaux 18 et
19) ; le métronome d'Au tempo dans chaque instrument ; le raccord de la boucle avec l'écho ; les effets du
Classique, du Serrurier et du Jardin dans chaque instrument ; la sonie d'une sonnerie en Néon, qui, normalisée en
crête, peut sonner plus fort que les autres. **[à vérifier] par la mesure** (P14.18) : le rendu
d'une musique de 8 mesures en moins de 150 ms sur un téléphone d'entrée de gamme, dans chaque instrument.

**Écarts** :

- Néon sans batterie dans cette phase (la proposition du designer en avait une : grosse caisse sur les temps,
  claquement sur 2 et 4, charleston en croches) ; tâche facultative P14.19, jamais dans Au tempo.
- Néon à un seul oscillateur par voix : les dents de scie désaccordées du designer demanderaient deux oscillateurs
  par note ; à reprendre si l'écoute le demande.
- Velours sans réverbération : l'écho en tient lieu (une réverbération coûterait plus au rendu).

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

### Accompagnement d'Au tempo — 100 temps/min, 4/4

Arrangement maison, sans mélodie, écrit pour Au tempo ([règles](regles-au-tempo.md#accompagnement)) :
`Accompaniment.score(metronome)` (`core:music`), 4 mesures jouées en boucle, au facteur de tempo
`Accompaniment.tempoFactor(ticksPerEighth)` = 18 / `ticksPerEighth` (× 1,2 à 120 temps/min). Une
seule identité (`ScoreId.Personal("tempo-accompaniment")`) avec ou sans métronome. Valeurs proposées
en P11.9, **[à vérifier]** à l'oreille sur téléphone.

- Basse, onde triangle, volume 0,16, I – V – vi – IV de Do majeur, la fondamentale sur chaque
  croche, ramenée dans l'octave qui part de F2 (`Pitch.inOctaveFrom`, que partagent aussi la basse
  générée et celle du Jardin) : C3 ×8 · G2 ×8 · A2 ×8 · F2 ×8.
- Métronome (s'il est activé), onde carrée, un clic d'1/8 de temps par noire : C6 à 0,10 sur le
  temps 1, G5 à 0,05 sur les temps 2 à 4. Les silences sont découpés par temps : chaque voix
  commence une note ou un silence sur chaque temps, si bien que le tempo peut changer et la lecture
  reprendre sur tous les temps (`switchBeats`, [Lecture en boucle](#lecture-en-boucle)).

**Tests :** `AccompanimentTest › the bass plays I V vi IV, one bar each`,
`PitchTest › a pitch is brought into the octave starting at a lower pitch`,
`AccompanimentTest › the accompaniment clicks on every quarter, louder on beat 1`,
`AccompanimentTest › every voice of the accompaniment starts a note or a rest on every beat`,
`SwitchBeatsTest › the accompaniment of Au tempo with its metronome lets the tempo switch on every beat`,
`AccompanimentTest › without the metronome the bass plays on`,
`AccompanimentTest › the tempo factor is 18 over the ticks per eighth`,
`AccompanimentTest › an eighth lasts a positive number of ticks`.
