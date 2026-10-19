# Générateur de musique

Spécification du bouton **Au hasard** de l'éditeur de musique (prototype :
[Éditeur de musique BloX](https://claude.ai/artifact/M91HH7KRJWTMHeDvLaV8wz), lien privé).
Le générateur ne tire pas des notes au hasard. Il **compose** selon les règles d'écriture
qui rendent une mélodie agréable et facile à retenir. Le hasard choisit seulement entre des
possibilités qui sonnent toutes juste. L'utilisateur choisit une **catégorie** (Joyeux,
Calme, Épique…), qui fixe le caractère de la musique.

Seul le modèle harmonique est implémenté (P4.13, section
[Modèle harmonique](#modèle-harmonique-implémenté)). Les autres valeurs et les sources sont
**[à vérifier]** : elles viennent de mémoire et doivent être confirmées avant d'écrire le test
correspondant.

## Principe

1. **Composer, puis évaluer.** Le générateur produit plusieurs candidats (par exemple 20),
   les note selon les règles de qualité ci-dessous, et garde le meilleur. Une règle
   éliminatoire écarte un candidat quelle que soit sa note.
2. **Du hasard dans un cadre.** Chaque tirage se fait parmi des choix déjà musicaux : une
   suite d'accords éprouvée, un rythme du répertoire de la catégorie, un mouvement mélodique
   probable.
3. **Reproductible.** Le tirage est injecté (une graine, comme le `Randomizer` du jeu) : la
   même graine et la même catégorie donnent la même musique. Les tests en dépendent, et la
   graine peut servir de « code » pour retrouver une musique.
4. **Original.** Aucune mélodie existante n'est reprise : seules des règles générales
   d'écriture sont utilisées (voir [musique.md](musique.md), droits).

## Ce qui rend une musique agréable

Les règles retenues, regroupées par dimension. Chacune deviendra un test.

### Tonalité et gamme

- La musique est dans **une seule tonalité**, tirée parmi celles qui tiennent dans les
  tessitures (do, ré, fa, sol, la).
- La gamme dépend de la catégorie : majeure (lumineuse), mineure naturelle (mélancolique),
  mineure harmonique (tendue), dorienne (mystérieuse, sans tristesse), mixolydienne
  (rétro), pentatonique (sans aucune dissonance, la plus « sûre »).
- La mélodie n'emploie que les notes de la gamme. Seule exception : une note de passage
  chromatique, sur un temps faible, dans les catégories qui l'autorisent.

### Harmonie

- **Une suite d'accords tirée d'une liste éprouvée** par catégorie, plutôt qu'accord par
  accord : I–V–vi–IV, vi–IV–I–V, I–vi–IV–V, I–IV–V–I en majeur ; i–VI–III–VII, i–iv–v–i,
  i–VII–VI–V en mineur.
- **Un accord par mesure** (deux par mesure pour les catégories rapides, en option).
- **La musique tourne en boucle pendant la partie.** La dernière mesure doit donc ramener à
  la première : elle finit sur l'accord de V (cadence suspendue), qui appelle le I du début.
  La mesure 4 finit elle aussi sur une cadence suspendue, pour couper la musique en deux
  phrases.

### Modèle harmonique (implémenté)

Types de `core:music` (P4.13), sans couleur ni son. Les intervalles des gammes et des
triades sont ceux de la théorie standard (source : définition des modes et des accords
parfaits, par exemple W. Piston, *Harmony*, et l'article « Mode (musique) »
de Wikipédia) ; ils ne sont plus **[à vérifier]**.

- **Gamme** (`Scale(tonique, ScaleType)`), en demi-tons entre degrés : majeure 2-2-1-2-2-2-1,
  mineure naturelle 2-1-2-2-1-2-2, mineure harmonique 2-1-2-2-1-3-1, dorienne 2-1-2-2-2-1-2,
  mixolydienne 2-2-1-2-2-1-2, pentatonique majeure 2-2-3-2-3. Chaque type couvre exactement
  une octave.
  `ScaleTest › each scale type stacks its own steps above the tonic`,
  `ScaleTest › every scale type spans exactly one octave, so the degree after the last one is the tonic an octave up`.
- **Degrés** comptés depuis 1 ; ils continuent dans les octaves au-dessus et sous la tonique
  (0 est le degré juste en dessous), pour la marche par degrés de la mélodie. Une note hors de
  do0..si8 est refusée ; l'appartenance à la gamme se juge par classe de hauteur, dans toutes
  les octaves.
  `ScaleTest › degrees past the seventh go on in the octaves above`,
  `ScaleTest › degrees below the first go down under the tonic, degree 0 being the one just below`,
  `ScaleTest › a scale holds the pitches of its degrees in every octave and no other`,
  `ScaleTest › a scale on the highest pitch still knows its degrees, though they go past B8`,
  `ScaleTest › a degree past B8 or under C0 is refused, and so are the pitches of a scale that goes past B8`.
- **Accord** (`Chord(fondamentale, ChordQuality)`) : triade par tierces, majeure (4 + 3),
  mineure (3 + 4) ou diminuée (3 + 3).
  `ChordTest › a triad stacks two thirds on its root, major then minor, minor then major, or two minor`,
  `ChordTest › a chord holds the pitches of its root, third and fifth in every octave and no other`,
  `ChordTest › a chord on the highest pitch still knows its notes, though its fifth goes past B8`,
  `ChordTest › the pitches of a chord whose fifth goes past B8 are refused`.
- **Chiffrage romain** (`RomanNumeral.parse`) : I à VII, capitales pour majeur, minuscules
  pour mineur, minuscules suivies de `°` pour diminué ; une altération `♭` ou `#` au plus,
  en tête, déplace la fondamentale d'un demi-ton. La qualité vient du chiffrage, pas de la
  gamme : le V majeur d'une gamme mineure s'écrit `V`. Les suites s'écrivent avec `-` ou `–`.
  `RomanNumeralTest › a numeral names the root degree, in capitals if major, small if minor, small with a ring if diminished`,
  `RomanNumeralTest › a flat or a sharp before the numeral moves its root a semitone from the degree`,
  `RomanNumeralTest › a name that is not one numeral from I to VII in one case, after one flat or sharp at most, is refused`,
  `RomanNumeralTest › a degree outside 1 to 7 is refused`,
  `ScaleTest › a numeral builds its chord on the matching degree of the scale`,
  `ScaleTest › the case of the numeral sets the chord, even with a note outside the scale like the major V in minor`,
  `ScaleTest › a flat numeral builds its chord a semitone under the degree of the scale`,
  `ScaleTest › a sharp numeral builds its chord a semitone above the degree of the scale`.
- **Pentatonique** : ses accords sont ceux de la gamme majeure sur la même tonique (le IV de
  Calme), sa mélodie garde ses cinq notes.
  `ScaleTest › a pentatonic scale takes its chords from the major scale on its tonic, but keeps its own five notes`,
  `ProgressionTest › a progression over a pentatonic scale plays the chords of the major scale on its tonic`.
- **Progression** : un accord par mesure, en boucle ; les mesures sont comptées depuis 0 dans
  le code (les mesures 4 et 8 de la forme sont `chordAt(3)` et `chordAt(7)`).
  `ProgressionTest › each bar holds the next chord of the progression`,
  `ProgressionTest › after its last chord the progression starts again, so four chords fill eight bars twice over`,
  `ProgressionTest › numerals may be separated by en dashes, as the specification writes them`,
  `ProgressionTest › a progression without chords is refused`,
  `ProgressionTest › bars are counted from 0, so a negative bar is refused`.

Aucune vue de maquette n'est concernée.

### Forme

- **8 mesures, deux phrases de 4** : une question (mesures 1 à 4) et une réponse (mesures 5
  à 8).
- **Un motif d'une mesure**, tiré au début, porte toute la musique. Il revient
  transformé : transposé sur l'accord en place, renversé, ou avec un rythme légèrement
  changé.
- Plan type : `A A' B A''`, une lettre par groupe de deux mesures. La réponse reprend le
  début de la question, puis s'en écarte pour conclure.
- **Répéter, mais pas trop** : au moins la moitié des mesures reprend le rythme du motif ;
  aucune mesure n'est la copie exacte de la précédente.

### Forme (implémentée, P4.15)

`Form.compose(catégorie, tirage)` (interne à `core:music`) donne le squelette que la mélodie
(P4.16) et la basse (P4.17) rempliront : tonique, gamme et suite (une `Harmony` de la
catégorie), tempo, motif, et pour chacune des 8 mesures sa section, son rôle, ses accords et
son rythme. Choix de conception, **[à vérifier]** à l'écoute.

- **Plan des rythmes.** Les lettres `A A' B A''` vont par deux mesures. Rôles : motif aux
  mesures 1, 2, 3, 6 et 7 ; fin de phrase aux mesures 4 et 8 ; contraste à la mesure 5. La
  réponse s'ouvre donc sur un contraste (B) et revient au motif dès la mesure 6, plutôt que de
  reprendre le début de la question : la phrase ci-dessus est tranchée ainsi. Le motif occupe
  5 mesures sur 8 ; il ne revient pour l'instant que par son rythme, ses transformations
  (transposé, renversé) viendront avec la mélodie (P4.16). La fin de phrase est tirée parmi les
  cellules qui respirent, hors motif ; le contraste parmi les autres cellules du répertoire,
  hors motif et hors fin de la mesure 4.
  `FormTest › a form spans eight bars in sections A A' B A'', one letter for each pair of bars`,
  `FormTest › bars 1, 2, 3, 6 and 7 take up the motif, bar 5 departs from it, bars 4 and 8 end the phrases`,
  `FormTest › the phrase ends depart from the motif, so that the motif fills five bars out of eight`,
  `FormTest › bar 5 departs from the phrase end of bar 4 too, so that B never takes up the end of the question`,
  `FormTest › over the seeds, bar 5 draws every cell but the motif and the phrase end before it`,
  `FormTest › over the seeds, bars 4 and 8 draw every phrase end but the motif`,
  `CategoryTest › whatever its motif, a category keeps a phrase end apart from it, and a contrast apart from both`.
- **Respirer.** Une cellule respire si elle finit sur une note de 2 temps au moins ou sur un
  silence. Joyeux, Épique, Rétro et Danse reçoivent des cellules de fin propres (`0.5 0.5 1 2`,
  `0.75 0.25 1 2` ; `1 1 2`, `1 1 1 r1` ; `0.5 0.5 0.5 0.5 2`, `0.25 0.25 0.25 0.25 0.5 0.5 2` ;
  `1 0.5 0.5 2`, `1 0.5 0.5 1 r1`) ; les autres finissent sur les cellules de leur répertoire
  qui respirent.
  `FormTest › each phrase breathes at its end, bars 4 and 8 closing on a long note or a silence`,
  `RhythmCellTest › a cell breathes when it ends on a long note, a half note at least, or on a silence`,
  `RhythmCellTest › a cell that ends on a note shorter than a half note does not breathe`,
  `CategoryTest › every category has cells to end its phrases on, that breathe after striking a chord note on beat 1`.
- **Accords et cadences.** Chaque mesure joue l'accord suivant de la suite ; Rétro en change à
  chaque demi-mesure (temps 1-2, puis 3-4). Toute suite commence sur la tonique, et les mesures
  4 et 8 finissent sur l'accord du 5e degré de la gamme d'harmonie, qui appelle le I de la
  mesure 1 : V en majeur, en mineure harmonique et en pentatonique, v (demi-cadence modale)
  en mineure naturelle, en dorien et en mixolydien. En Rétro, seule la dernière demi-mesure
  change. Calme joue donc `I–IV` (et non plus `IV–I`).
  `FormTest › outside the cadences, each bar plays the next chord of the progression, one chord a bar`,
  `FormTest › retro moves on to the next chord of the progression every half bar, beats 1 and 2 then 3 and 4`,
  `FormTest › bars 4 and 8 end on the chord of the fifth degree of the scale, a half cadence that calls back bar 1`,
  `FormTest › bar 1 of every form plays the tonic chord of its scale, which the half cadence of bar 8 calls back`,
  `ScaleTest › the triad on the fifth degree stacks two thirds of the scale, major V or minor v, major V for a pentatonic`,
  `HarmonyTest › a progression that does not start on the tonic is refused, as the half cadence of bar 8 calls it back`.
- **Pas de copie.** Une mesure copie la précédente si elle a le même rythme et les mêmes
  accords (boucle comprise : la mesure 1 après la mesure 8). Une `Harmony` refuse une suite
  qui jouerait les mêmes accords deux mesures de suite hors cadences, selon son rythme
  harmonique (porté par l'`Harmony`) ; la part qui dépend des hauteurs viendra avec la
  mélodie et la note de qualité.
  `FormTest › no bar copies the one before it, rhythm and chords alike, not even bar 1 after bar 8 in the loop`,
  `HarmonyTest › a progression that plays the same chords in two bars running, outside the cadences, is refused`,
  `HarmonyTest › a progression may come back to its tonic on bar 4, as the half cadence plays the fifth there`.
- **Reproductible.** Tirages dans cet ordre : catégorie (Surprise), harmonie, tonique, suite,
  tempo, motif, puis les cellules de chaque mesure ; la mélodie et la basse tireront après. Une
  caractérisation fige cet ordre (graine 42, Joyeux : do majeur, `I–IV–V–I`, 133 temps/min).
  `FormTest › a form plays in one of the keys, on one of the harmonies and at a tempo of its category`,
  `FormTest › the same category and the same seed give the same form, surprise included`,
  `FormTest › surprise draws its category first, from the same draw as the rest of the form`,
  `FormTest › different seeds end up giving different forms, in their key, tempo, motif and bars`,
  `FormTest › the seed 42 on joyful gives this form, which pins the order of the draws`.

L'algorithme de `kotlin.random.Random(graine)` n'est pas garanti d'une version de Kotlin à
l'autre : la caractérisation le signalera. Aucune vue de maquette n'est concernée.

### Mélodie

- **Surtout par degrés conjoints** : environ 70 % des intervalles sont d'une seconde, le reste
  des tierces, quartes, quintes ; pas de saut de plus d'une octave ; pas de triton.
- **Après un grand saut** (une quarte ou plus), la mélodie repart dans l'autre sens, par
  degré.
- **Une courbe en arche** : la note la plus aiguë arrive vers les deux tiers de la musique
  (mesures 5 ou 6), une seule fois.
- **Une note de l'accord sur chaque temps fort** (temps 1 et 3) ; les notes étrangères à
  l'accord (de passage, broderies) sur les temps faibles seulement.
- **Une tessiture d'une octave à une dixième** au plus.
- Jamais plus de trois fois la même note de suite.
- **Respirer** : chaque phrase se termine par une note longue (une blanche au moins) ou un
  silence.

### Mélodie (implémentée, P4.16)

`Melody.compose(forme, tirage)` (interne à `core:music`) remplit de hauteurs les rythmes de la
forme, note après note, par une recherche avec retour arrière : chaque règle dure est vérifiée
à chaque note, une impasse fait revenir sur le choix précédent. Valeurs **[à vérifier]** à
l'écoute.

- **Règles dures**, garanties par construction et vérifiées sur 100 graines et toutes les
  catégories, boucle comprise (de la mesure 8 à la mesure 1) :
  - chaque son de la forme reçoit une note, chaque silence un silence ;
  - une note de l'accord en place sur chaque attaque des temps 1 et 3 (de la demi-mesure en
    Rétro), et sur la dernière note de chaque phrase, même sur un temps faible ; la dernière
    note de la mesure 8 appartient à l'accord de cadence ;
  - toutes les notes dans la gamme, sauf une note de passage chromatique de Mystérieux, sur un
    temps faible, entre deux notes à un ton l'une de l'autre ;
  - sous un accord emprunté, aucune note à un demi-ton d'une note de l'accord qui sort de la
    gamme (fausse relation) ;
  - ni saut de plus d'une octave, ni triton ; après une quarte ou plus, retour par degré en
    sens inverse ; jamais plus de trois fois la même note de suite ;
  - une dixième au plus, dans la tessiture de la voix de mélodie, **do4 à do6** : la mélodie
    se place dans une fenêtre d'une dixième centrée sur cette tessiture (mi4 à sol#5), coupée
    à une tessiture plus étroite ;
  - aucune mesure ne copie la précédente, hauteurs et rythme compris.

  `MelodyTest › a melody fills the rhythm of each bar of its form, a note for each sound and a rest for each silence`,
  `MelodyTest › every note struck on beat 1 or 3 is a note of the chord in place, of its half bar when two chords share it`,
  `MelodyTest › each phrase ends on a note of the chord in place, even when its last note falls on a weak beat`,
  `MelodyTest › the last note struck in bar 8 is a note of the half cadence chord, which calls back bar 1`,
  `MelodyTest › every note is in the scale, but a chromatic passing note of mysterious, on a weak beat between two steps`,
  `MelodyTest › under a borrowed chord, no note stands a semitone from a note of the chord out of the scale, a false relation`,
  `MelodyTest › a melody never leaps past an octave, nor by a tritone, not even from bar 8 back to bar 1`,
  `MelodyTest › after a leap of a fourth or more, the melody turns back by step, across the loop too`,
  `MelodyTest › a melody never strikes the same note more than three times in a row, rests left out, loop included`,
  `MelodyTest › a melody spans a tenth at most, within the range of the melody voice, from C4 to C6`,
  `MelodyTest › a melody keeps within a narrower range given to it, its window cut to that range`,
  `MelodyTest › no bar of a melody copies the one before it, pitches and rhythm alike, not even bar 1 after bar 8`,
  `MelodyTest › a bar taking up the motif on the chord of the bar before still departs from it`.
- **Seconde** : un mouvement vers une note voisine de la gamme ; dans une gamme de 7 notes, au
  plus 2 demi-tons (la seconde augmentée de la mineure harmonique est un saut), et dans la
  pentatonique, la tierce mineure entre deux degrés voisins compte comme une seconde.
  `MelodyMeasuresTest › in a pentatonic scale, a minor third between two neighbouring degrees is a step`,
  `MelodyMeasuresTest › in a seven-note scale, the augmented second of the harmonic minor is a leap, not a step`.
- **Arche** : le sommet est tiré parmi les temps forts de la mesure 5 (la plus haute note de
  l'accord de cet emplacement dans la fenêtre), frappé une seule fois ; rien ne le dépasse
  ailleurs. Mesuré : 100 % des graines dans toutes les catégories. La recherche avec arche a un
  budget (5 000 étapes) ; au-delà, elle recommence sans arche.
  `MelodyTest › over the seeds, a melody almost always arches up to its peak, struck once on a strong beat of bar 5 or 6`,
  `MelodyTest › over the seeds, bars 5 and 6 mostly sing higher than bars 1 and 8, the foot of the arch`,
  `MelodyTest › a search for an arch that gives up leaves a melody without one, rather than no melody at all`.
- **Tendances**, tirées par poids : de l'unisson à l'octave 1 / 10 / 3 / 1,5 / 1 / 0,3 / 0,2 /
  0,2. Mesuré : 60 à 73 % de secondes selon la catégorie (fourchette du test : 55 à 85 %).
  Les mesures du motif reprennent ses mouvements en degrés, transposés sur l'accord en place
  (de 39 % des mouvements gardés pour Calme à 88 % pour Rétro).
  `MelodyTest › over the seeds, a melody goes mostly by step, about 70 percent of its moves`,
  `MelodyTest › over the seeds, the bars taking up the motif keep most of its moves, in degrees of the scale, calm the fewest`.
- **Traits de catégorie** (`MelodyTraits`, facteurs strictement positifs) : Épique multiplie
  les quintes par 8, Mélancolique les secondes descendantes par 3 (et divise les montantes
  par 3), Rétro les mouvements entre notes de l'accord par 4 ; les autres sont neutres.
  Mystérieux passe par une note chromatique dans environ 40 % des graines.
  `CategoryTest › a single category leans its melody each way, epic to fifths, melancholic down, retro to arpeggios`,
  `MelodyTraitsTest › a factor that is not positive and finite is refused, as it would cancel or swamp the weights of the draws`,
  `MelodyTest › over the seeds, epic leaps by a fifth more often than a melody leaning no way on the same forms`,
  `MelodyTest › over the seeds, melancholic goes down more often than a melody leaning no way on the same forms`,
  `MelodyTest › over the seeds, retro runs arpeggios more often than a melody leaning no way on the same forms`,
  `MelodyTest › over the seeds, mysterious passes now and then through a chromatic note, but not in most melodies`.
- **Mesures pour la note de qualité** (P4.18) : part des secondes, des quintes, des descentes,
  des arpèges, mesures du sommet.
  `MelodyMeasuresTest › the share of steps counts the moves to a neighbouring note of the scale, repeats and rests left out`,
  `MelodyMeasuresTest › the peak of a melody lists the bars where its highest note is struck, once for each time`,
  `MelodyMeasuresTest › the share of fifths counts the moves of a perfect fifth, up or down, repeats left out`,
  `MelodyMeasuresTest › the share of descents counts the moves down, repeats left out`,
  `MelodyMeasuresTest › the share of arpeggios counts the moves between two notes of the chord in place, repeats left out`.
- **Reproductible et borné** : les tirages suivent ceux de la forme sur le même tirage
  (recherches abandonnées d'abord, puis la recherche qui aboutit, puis les notes
  chromatiques). La recherche repart de zéro toutes les 1 000 étapes et s'arrête à 50 000 :
  une forme qu'aucune mélodie ne peut remplir ne donne aucune mélodie (`null`, comme la basse).
  Pire temps mesuré : 32 ms à froid, 6 ms à chaud sur la JVM.
  `MelodyTest › the same form and the same draw give the same melody`,
  `MelodyTest › the seed 42 on joyful gives this melody, which pins the order of the draws after the form`,
  `MelodyTest › the seed 1 on mysterious gives this melody in D dorian, which passes through D sharp and C sharp`,
  `MelodyTest › a form that no melody fits leaves no melody, found in good time rather than searched for ever`.

Ouvert : renversement du motif en A'' non fait. Aucune vue de maquette n'est concernée.

### Rythme

- Le rythme vient d'un répertoire de motifs propre à la catégorie : croches régulières,
  croches pointées, noire–deux croches…
- Syncope légère, au plus une par mesure, dans les catégories qui l'autorisent.
- La densité suit la catégorie : peu de notes et des valeurs longues pour Calme, des
  croches et des doubles croches pour Rétro.

### Basse

- La note fondamentale de l'accord sur le premier temps de chaque mesure.
- Ensuite, selon la catégorie : fondamentale et quinte en alternance, octaves sautées
  (« bondissante »), croches répétées (« galop »), ou rondes.
- Avant un changement d'accord, une note de passage peut rejoindre la fondamentale suivante
  par degré.
- Pas de quintes ni d'octaves parallèles entre la mélodie et la basse (règle d'écriture
  classique, qui évite l'effet « creux »).

### Basse (implémentée, P4.17)

`Bass.compose(forme, mélodie, tirage)` (interne à `core:music`) écrit la basse par une recherche
avec retour arrière et mémoire des impasses, bornée par un budget (10 000 étapes, partagé entre
les octaves de la première note). Quand aucune basse ne convient, elle rend `null` : c'est une
règle éliminatoire de la note de qualité (P4.18), jamais une basse fautive. Valeurs **[à vérifier]**
à l'écoute.

- **Trois motifs** au lieu de sept styles : fondamentale et quinte, octaves, fondamentale
  répétée, chacun avec sa valeur de note ; le motif repart à chaque changement d'accord et se
  joue depuis la fondamentale réellement placée (octave ou quinte au-dessus, en dessous si elle
  sort de la tessiture). Joyeux : octaves en noires ; Calme : fondamentale en blanches ;
  Épique : fondamentale en croches (galop) ; Mélancolique : fondamentale et quinte en noires ;
  Mystérieux : fondamentale en rondes ; Rétro : octaves en croches ; Danse : fondamentale et
  quinte en croches.
  `CategoryTest › each category has the bass of its character, a motif of the specification at a note value of its own`,
  `BassStyleTest › a bass style whose notes do not last a positive, finite number of beats is refused`,
  `BassTest › a bass strikes a note at each value of its style, starting anew on each change of chord, without a rest`,
  `BassTest › under a melody that never moves, a bass plays the motif of its category, each root nearest the note before`.
- **Règles dures**, vérifiées sur les graines et toutes les catégories, boucle comprise :
  - la fondamentale sur le temps 1 de chaque mesure, et sur le temps 3 quand l'accord y change ;
  - toutes les notes dans l'accord en place, sauf la note de passage ;
  - tessiture **do2 à do4**, toujours sous la note de mélodie qui sonne ;
  - aucun saut de plus d'une octave ; triton évité (aucun mesuré sur 671 basses) ;
  - aucune quinte ni octave parallèle : une verticale à chaque attaque de l'une ou l'autre voix,
    chaque voix tenant sa note jusqu'à la suivante ; deux verticales successives où les deux
    voix ont bougé dans le même sens, à la quinte ou à l'octave (unisson et redoublements
    compris), sont parallèles. Les quintes et octaves cachées (mouvement direct vers un
    intervalle juste) ne sont pas vérifiées.

  `BassTest › a bass strikes the root of the chord on beat 1 of every bar, and on beat 3 when the chord changes there`,
  `BassTest › every bass note is a note of the chord in place, but a passing note on the last weak beat before a new chord`,
  `BassTest › a bass keeps from C2 to C4, below every note of the melody sounding with it`,
  `BassTest › a bass goes under a melody that comes down into its range, even if it leaves its motif`,
  `BassTest › a bass never leaps past an octave, not even from bar 8 back to bar 1`,
  `BassTest › a bass never moves in parallel fifths or octaves with the melody, not even from bar 8 back to bar 1`,
  `BassTest › where its motif would move in parallel fifths with the melody, a bass takes another note of the chord`,
  `BassTest › where its root on beat 1 would move in parallel octaves with the melody, a bass strikes it an octave up`,
  `BassTest › a bass avoids the octaves the melody reaches while it holds, before both move on together`,
  `ParallelsTest › two voices struck together a fifth apart, then a fifth apart again, both going up, make parallel fifths`,
  `ParallelsTest › octaves and unisons in turn, both voices going the same way, make parallel octaves, compound ones too`,
  `ParallelsTest › fifths or octaves in contrary or oblique motion, or struck again on the same notes, are not parallel`,
  `ParallelsTest › each vertical moves to the next one where both voices have moved, whichever voice struck its notes`,
  `ParallelsTest › the music loops, so the last notes struck together lead to the first ones, and may move in parallel too`,
  `ParallelsTest › a voice reaching an octave while the other holds, then both moving together, makes parallel octaves`.
- **Note de passage** : sur le dernier temps faible avant un nouvel accord, tirée une fois sur
  deux, à un degré d'une fondamentale suivante, la plus proche de la note précédente ; hors
  accord, abordée par une quarte au plus, résolue par degré, et sans seconde, septième ni
  triton avec la mélodie qui sonne. Mesuré : 48 à 78 % d'approches par degré avec, 23 à 65 %
  sans. Calme et Mystérieux n'en ont pas.
  `BassTest › a bass trying a passing note plays, on the last weak beat before a new chord, a step from its root`,
  `BassTest › a note out of the chord is consonant with the melody sounding with it, and reached by a fourth at most`,
  `BassTest › over the seeds, a bass leads by step to the next root more often than a bass that never tries a passing note`.
- **Échecs et bornes** : Mystérieux (rondes, fondamentales imposées) n'a pas de basse sur environ
  28 % des graines, Calme sur 1 %, les autres jamais. Pire temps mesuré : 51 ms sur la JVM.
  `BassTest › over the seeds, a bass fits nearly every melody, but fewer of mysterious, whose held roots leave it no choice`,
  `BassTest › a melody that no bass fits leaves no bass, found in good time rather than searched for ever`,
  `BassTest › a dead end that shows only in the last bar is found in good time, each dead end searched once`,
  `BassTest › a search past its budget gives up, leaving no bass, even where one fits`.
- **Reproductible** : un tirage par place de note de passage, après ceux de la mélodie.
  `BassTest › the same form, the same melody and the same draw give the same bass`,
  `BassTest › the seed 42 on joyful gives this bass, which pins the order of the draws after the melody`.

Aucune vue de maquette n'est concernée.

## Catégories

L'utilisateur choisit une catégorie avant de lancer le dé. *Surprise* tire la catégorie
elle-même au hasard.

| Catégorie | Gamme | Tempo (temps/min) | Suites d'accords | Rythme de la mélodie | Basse |
|---|---|---|---|---|---|
| **Joyeux** | majeure | 120–150 | I–V–vi–IV, I–IV–V–I | croches, croches pointées, sautillant | bondissante (octaves) |
| **Calme** | pentatonique majeure | 70–95 | I–vi–IV–V, I–IV | blanches et noires, beaucoup d'air | fondamentale en blanches |
| **Épique** | mineure naturelle | 110–140 | i–VI–III–VII, i–VII–VI–VII | noires fortes, sauts de quinte | galop (croches répétées) |
| **Mélancolique** | mineure naturelle | 80–100 | i–VI–iv–v, i–iv–VII–III | notes longues, descentes | fondamentale et quinte |
| **Mystérieux** | dorienne ou mineure harmonique | 90–110 | i–IV (dorien), i–♭VII–VI–V (harmonique) | notes tenues, chromatisme de passage | notes tenues |
| **Rétro** | majeure ou mixolydienne | 140–170 | I–VII–IV–I (mixolydien), I–V–vi–IV (majeur) | doubles croches, arpèges rapides | octaves rapides |
| **Danse** | mineure harmonique | 130–160 | i–iv–V–i | 2 temps marqués, croches | fondamentale et quinte en croches |
| **Surprise** | une des catégories ci-dessus, tirée au hasard | | | | |

Chaque catégorie est un jeu de paramètres, pas un code à part : un seul générateur, des
catégories interchangeables, comme les politiques de `core:game`. Ajouter une catégorie ne
modifie pas le générateur.

### Catégories implémentées (P4.14)

`Category` (`core:music`) porte les paramètres de la table, sans rien générer. Les tempos,
les suites et les cellules sont des choix de conception du projet, sans source : ils restent
**[à vérifier]** à l'écoute.

- **Gammes et suites.** Chaque gamme d'une catégorie vient avec ses suites (`Harmony`) :
  Mystérieux joue `i–IV` en dorien et `i–♭VII–VI–V` en mineure harmonique, Rétro `I–VII–IV–I`
  en mixolydien et `I–V–vi–IV` en majeur. Le chiffrage compte les degrés de la gamme jouée : le
  VII du mixolydien est déjà le si♭ de do, et une mélodie mixolydienne garde son si♭ partout.
  Une suite ne s'accepte que si chaque accord sans altération se construit dans la gamme (pour
  la pentatonique, dans la majeure de même tonique) ; une altération doit signaler un vrai
  emprunt, dont la fondamentale sort de la gamme.
  `CategoryTest › each category plays on the scales of its character`,
  `CategoryTest › each scale of a category comes with the progressions that suit it`,
  `HarmonyTest › a scale without a progression to play on it is refused`,
  `HarmonyTest › a progression with a chord out of the scale, though written without an accidental, is refused`,
  `HarmonyTest › a numeral with an accidental borrows a chord whose root is out of the scale`,
  `HarmonyTest › a progression on a pentatonic scale may hold the chords of the major scale, not only its five notes`,
  `HarmonyTest › an accidental that lands the root on another degree of the scale is refused, as it would name that degree`.
- **Tempo** : un intervalle par catégorie (table ci-dessus), le tempo tiré dedans, bornes
  comprises.
  `CategoryTest › each category plays within its own range of tempos, in beats per minute`,
  `TempoRangeTest › a drawn tempo stays within the range and may be either end`,
  `TempoRangeTest › a range that is not a positive tempo up to a faster or equal one is refused`,
  `TempoRangeTest › a range of a single tempo always draws that tempo`.
- **Toniques** : do, ré, fa, sol, la (octave 4), communes à toutes les catégories. Toutes
  tiennent dans la fenêtre de la mélodie (do4 à do6, voir « Mélodie (implémentée) »).
  `CategoryTest › every category may be played in C, D, F, G or A`.
- **Cellules rythmiques** (`RhythmCell`, durées en temps, `r` pour un silence) : une mesure de
  4 temps exactement, des durées positives. Une cellule sait si elle se tait sur un temps fort
  (1 ou 3) et compte ses syncopes : une note attaquée ailleurs que sur un temps fort et tenue
  au-delà du temps suivant. Le répertoire des catégories ne se tait jamais sur 1 et 3 et a une
  syncope au plus ; une cellule de respiration (`2 r2`) reste possible pour les fins de phrase.
  `RhythmCellTest › a cell is read as durations in beats separated by spaces, r marking a rest`,
  `RhythmCellTest › a cell that does not fill exactly one bar of four beats is refused`,
  `RhythmCellTest › a step that is not a positive number of beats is refused, even if the bar adds up`,
  `RhythmCellTest › a cell that is silent on beat 1 or 3 rests on a strong beat`,
  `RhythmCellTest › a cell silent only on the weak beats does not rest on a strong beat`,
  `RhythmCellTest › a cell counts its syncopations, notes struck off the strong beats and held over the next beat`,
  `RhythmCellTest › a phrase may end by resting from beat 3, to breathe`,
  `CategoryTest › every cell of a category plays a chord note on beats 1 and 3, and holds one light syncopation at most`.

  | Catégorie | Cellules |
  |---|---|
  | Joyeux | `0.5 0.5 0.5 0.5 1 1` · `0.75 0.25 0.75 0.25 1 1` · `1 0.5 0.5 0.75 0.25 1` · `0.5 1 0.5 1 1` |
  | Calme | `2 2` · `1 1 2` · `2 1 r1` · `1 r1 2` |
  | Épique | `1 1 1 1` · `1 0.5 0.5 1 1` · `0.5 0.5 1 1 1` · `1 1 0.5 0.5 1` |
  | Mélancolique | `2 2` · `3 1` · `1 1 2` · `1.5 0.5 2` |
  | Mystérieux | `4` · `3 1` · `2 2` · `3 0.5 0.5` |
  | Rétro | huit doubles croches puis `0.5 0.5 1` · huit croches · `0.25×4 0.5 0.5 0.25×4 1` · `0.5 1 0.5 0.5 0.5 1` |
  | Danse | `1 0.5 0.5 1 0.5 0.5` · `1 0.5 0.5 1 1` · huit croches · `1 1 1 0.5 0.5` |

  Le caractère de chaque catégorie est vérifié sur ces cellules :
  `CategoryTest › calm leaves air, in half and quarter notes and some rests`,
  `CategoryTest › the busiest bar of calm holds fewer notes than the sparsest bar of retro`,
  `CategoryTest › retro runs a whole beat of sixteenth notes, for its fast arpeggios`,
  `CategoryTest › joyful skips along on a dotted eighth and a sixteenth`,
  `CategoryTest › epic strikes a strong note on every beat, and dance marks beats 1 and 3`,
  `CategoryTest › melancholic and mysterious hold a long note, a half note at least, in every bar`.
- **Basse, chromatisme, rythme harmonique** : un style de basse par catégorie (table), le
  chromatisme de passage pour Mystérieux seulement, deux accords par mesure pour Rétro
  seulement (le rythme harmonique est porté par chaque `Harmony` ; lecture par demi-mesure
  dans la forme, P4.15). Danse est à 4 temps,
  ses « 2 temps marqués » étant les temps 1 et 3.
  `CategoryTest › each category has the bass of its character, a motif of the specification at a note value of its own`,
  `CategoryTest › only mysterious lets its melody pass through a note out of the scale`,
  `CategoryTest › only retro, the fastest category, changes chord twice a bar`.
- **Surprise** tire une des sept catégories, jamais elle-même ; même graine, même catégorie.
  `CategoryChoiceTest › surprise may draw every category`,
  `CategoryChoiceTest › surprise draws the same category from the same seed`,
  `CategoryChoiceTest › a category chosen by the player is played as it is, whatever the draw`.

Points ouverts : l'accord qui ferme la boucle est tranché par la forme (P4.15, ci-dessus) ; les traits
mélodiques de la table sont des paramètres (`MelodyTraits`, P4.16) ; les styles de basse sont ramenés à trois motifs (P4.17). Aucune vue de maquette n'est concernée.

## Note de qualité

Chaque candidat reçoit une note sur 100. On garde le meilleur, et on recommence si aucun
n'atteint un seuil (par exemple 70).

### Note de qualité (implémentée, P4.18)

`MusicGenerator.generate(choix, graine)` (public, pour l'éditeur) compose 20 candidats par tour
(forme, mélodie, basse), garde le meilleur et recommence, trois tours au plus, tant qu'aucun
n'atteint le seuil de la catégorie ; à défaut, il garde le meilleur obtenu. Il rend une
`GeneratedMusic` : la partition à deux voix (mélodie en onde carrée, basse en onde triangle,
mêmes ondes et volumes que les arrangements, 8 mesures de 4 temps, tempo de la forme), la
catégorie tirée, la gamme, le choix et la graine. Le « code » d'une musique est le couple
(choix, graine) : Surprise consomme un tirage de plus. Coût mesuré : 22 ms en moyenne, 126 ms au
pire, 188 ms au premier appel sur la JVM ; l'appel se fait hors du thread principal. Un
candidat sans mélodie ou sans basse est écarté (`null`, sans exception) ; 60 candidats écartés de
suite lèveraient une `IllegalStateException`, invariant inatteignable en pratique.

Les critères garantis par construction (notes de l'accord sur les temps forts, sauts compensés,
répétition fixée par la forme) ne départageraient rien : la note ne retient que des critères qui
varient d'un candidat à l'autre (mesurés à l'intérieur d'un tour). Valeurs **[à vérifier]** à
l'écoute.

| Critère | Mesure | Valeur | Poids |
|---|---|---|---|
| Secondes | part des secondes parmi les mouvements | 1 à la part visée par la catégorie, 0 à 30 points de là | 25 |
| Notes préparées | notes étrangères non atteintes et non quittées par degré (répétitions sautées, boucle comprise) | 1 − n/16, 0 au moins | 25 |
| Motif | part des mouvements des mesures du motif qui suivent ceux de la mesure 1, en degrés | la part (1 sans mouvement) | 15 |
| Conduite de basse | part des changements d'accord faits par une quarte au plus, boucle comprise | la part (1 sans changement) | 15 |
| Penchant | part des mouvements que la catégorie favorise (quintes, descentes, arpèges) | min(1, part / visée) ; 1 sans penchant | 10 |
| Étendue | de la note la plus grave à la plus aiguë | 1 de 12 à 16 demi-tons, 0 une quarte au-delà | 5 |
| Arche (garde-fou) | sommet frappé une fois, mesure 5 ou 6 | 1 ou 0 | 5 |

Chaque catégorie porte son profil (part de secondes visée, seuil vers le 15e centile du meilleur
d'un premier tour, penchant) : sans penchant, la sélection choisissait l'Épique aux moins de
quintes et le Rétro aux moins d'arpèges. Un second tour se fait dans environ 19 % des
générations.

| Catégorie | Secondes visées | Seuil | Penchant (visée) |
|---|---|---|---|
| Joyeux | 0,69 | 85,5 | — |
| Calme | 0,69 | 84,5 | — |
| Épique | 0,69 | 80,5 | quintes (0,10) |
| Mélancolique | 0,63 | 87,5 | descentes (0,70) |
| Mystérieux | 0,59 | 88,5 | — |
| Rétro | 0,63 | 78,0 | arpèges (0,33) |
| Danse | 0,61 | 83,5 | — |

Le motif vise 100 % de mouvements gardés : la bande de 50 à 75 % de la spécification porte sur
la part des mesures qui reprennent le motif, fixée à 5 sur 8 par la forme. Ces visées et seuils
sont des constantes mesurées : un changement des traits ou des cellules oblige à les remesurer,
ce que les tests signalent.

- **Note** : `QualityTest › a candidate meeting every criterion in full scores 100, the weights of the criteria adding up to it`,
  `QualityTest › the points grow by the weight of a criterion as far as it is better met, the others staying`,
  `QualityTest › a criterion met less than not at all, more than in full, or by a share that is not a number, is refused`,
  `QualityTest › a quality that leaves a criterion out is refused`,
  `QualityTest › the steps are met in full at the share the category aims at, and not at all 30 points away or more`,
  `QualityTest › each unprepared note costs a sixteenth of the prepared notes, down to not at all from sixteen of them`,
  `QualityTest › a melody leans as its category does in full from the share of moves it aims at, and in part below it`,
  `QualityTest › a melody of retro leans as far as its share of arpeggios goes towards the third of its moves retro aims at`,
  `QualityTest › the span is met in full from an octave to a major tenth, and not at all a fourth further either way`,
  `QualityTest › the arch is met by a highest note struck once, in bar 5 or 6, and not at all otherwise`,
  `QualityTest › the quality of a candidate reads each criterion off the measures of its melody and its bass`,
  `QualityTest › under the same melody, a bass leaping less from chord to chord scores higher, by that criterion alone`,
  `QualityProfileTest › a share of steps to aim at outside 0 to 1 is refused`,
  `QualityProfileTest › a leaning that aims at no move, or at more than all of them, is refused`,
  `QualityProfileTest › a leaning may aim at all the moves`,
  `CategoryTest › each category aims at the share of steps of its melodies, and stops at the 15th percentile of its best ones`,
  `CategoryTest › the categories whose melody leans aim at a share of those moves, about the 90th percentile of candidates`,
  `MelodyTest › over the seeds, a melody goes by step about as much as its category aims at, within 5 points`.
- **Mesures** : `MelodyMeasuresTest › a melody that never moves makes no move of any kind, rather than a share that is not a number`,
  `MelodyMeasuresTest › a melody that strikes no note has no peak, rather than failing`,
  `MelodyMeasuresTest › the span of a melody counts the semitones from its lowest note to its highest, none without a note`,
  `MelodyMeasuresTest › the unprepared notes are the notes out of the chord in place not both reached and left by step`,
  `MelodyMeasuresTest › a note out of the chord that opens the music is reached from the last note, as the music loops`,
  `MelodyMeasuresTest › a melody striking only notes of the chord leaves none unprepared`,
  `MelodyMeasuresTest › a note out of the chord struck again is prepared when reached and left by step, the repeat in between`,
  `MelodyMeasuresTest › the share of the motif kept counts the moves of the bars taking it up that follow bar 1, in degrees`,
  `MelodyMeasuresTest › a motif of a single note keeps its contour whole, as it has no move to lose`,
  `BassMeasuresTest › the share of smooth changes counts the changes of chord the bass makes by a fourth at most, loop included`,
  `BassMeasuresTest › a bass under a single chord makes no change to leap at, so all of its changes are smooth`.
- **Générateur** : `CandidateTest › a form that no melody fits leaves no candidate, rather than an error`,
  `CandidateTest › over the seeds, a candidate of mysterious ruled out has a melody, but no bass that fits it`,
  `MusicGeneratorTest › over the seeds and the categories, the generator keeps the best of the candidates it composes`,
  `MusicGeneratorTest › over the seeds, a round composes twenty candidates, each from a draw of its own derived from the seed`,
  `MusicGeneratorTest › while no candidate reaches the threshold, a new round is composed, three at most, and the best one is kept`,
  `MusicGeneratorTest › a first round whose best candidate reaches the threshold exactly is the last one`,
  `MusicGeneratorTest › among candidates of equal points, the first one composed is kept`,
  `MusicGeneratorTest › over the seeds and the categories, a second round makes up for the weakest first rounds, about one in six`,
  `MusicGeneratorTest › over the seeds, the chosen melody leans as its category does no less than the candidates it is chosen from`,
  `MusicGeneratorTest › surprise gives back the category it drew from the seed, played by all the candidates, and the scale chosen`,
  `MusicGeneratorTest › a generated score, known by the id it is given, plays the best melody and bass as the arrangements do`,
  `MusicGeneratorTest › over the seeds, a generated score plays two voices of eight bars of four beats, at a tempo of its category`,
  `MusicGeneratorTest › the same choice and the same seed give the same music, surprise included, and other seeds other musics`,
  `MusicGeneratorTest › the seed 42 on joyful gives this music, which pins the derivation of the draws, the note and the threshold`,
  `MusicGeneratorTest › rounds whose candidates were all ruled out leave no best one, an error sixty ruled out in a row would make`.

Une musique générée porte l'identité personnelle (`ScoreId.Personal`) que lui donne celui qui
l'ouvre, l'éditeur ou la bibliothèque (P4.22). Aucune vue de maquette n'est concernée.

Spécification d'origine, pour mémoire :

| Critère | Mesure | Poids |
|---|---|---|
| Notes de l'accord sur les temps forts | part des temps forts sur une note de l'accord | élevé |
| Mouvement conjoint | part des secondes proche de 70 % | élevé |
| Sauts compensés | part des grands sauts suivis d'un retour par degré | moyen |
| Courbe | sommet unique, vers les deux tiers | moyen |
| Répétition | part des mesures qui reprennent le motif, entre 50 et 75 % | moyen |
| Tessiture | étendue entre une octave et une dixième | faible |

Règles **éliminatoires** : une note hors de la gamme (hors chromatisme permis), un saut de
plus d'une octave, un triton, une boucle qui ne finit pas sur l'accord de V, plus de trois
notes identiques de suite, une quinte ou une octave parallèle entre la mélodie et la basse,
une mélodie sans basse possible. Depuis P4.16 et P4.17, la mélodie et la basse les
garantissent par construction (la basse rend `null` plutôt qu'une basse fautive) : la note de
qualité n'a plus qu'à écarter les candidats sans basse.

## Conséquences sur le code

- **`core:music`** reçoit `MusicGenerator` (choix, graine) → `GeneratedMusic` (partition, catégorie, gamme), avec des types
  `Scale`, `Chord`, `Progression`, `Category`. Il reste en Kotlin pur et sans dépendance : le
  tirage vient de `kotlin.random.Random`, de la bibliothèque standard.
- **`Pitch`** n'a pas de bémols : les gammes qui en demandent (fa majeur, mixolydien) s'écrivent
  avec des dièses (si♭ = la#). Le nom affiché à l'utilisateur pourra venir plus tard.
- **Notes longues** : `Note.beats` les permet déjà. L'éditeur doit en placer (en glissant sur
  plusieurs cases), sinon une musique générée ne serait pas modifiable fidèlement.
- **L'éditeur suit la gamme** de la musique : ses lignes sont les notes de la gamme en cours,
  plus seulement la gamme de do.
- **Tests** : un test par règle, sur de nombreuses graines (`@ParameterizedTest`), plus
  « même graine, même musique » et « le tempo reste dans l'intervalle de la catégorie ».

## Sources

À consulter et citer règle par règle **[à vérifier]** :

- W. Piston, *Harmony* (cadences, enchaînements, quintes et octaves parallèles).
- D. Huron, *Sweet Anticipation* (2006) : prédominance des degrés conjoints, retour après un
  saut, courbe en arche.
- K. Jakubowski et al., « Dissecting an earworm » (2016) : ce qui rend un air facile à
  retenir (tempo assez vif, contour mélodique courant, quelques intervalles inattendus).
- Suites d'accords les plus répandues de la musique populaire (I–V–vi–IV et ses rotations).

## Tâches

Inscrites dans [SUIVI.md](../SUIVI.md), P4.13 à P4.22 :

1. Modèle harmonique : `Scale`, `Chord`, `Progression` (`core:music`).
2. Catégories : paramètres de chacune, *Surprise*.
3. Générateur de motif et de forme (`A A' B A''`), cadences de boucle.
4. Mélodie : degrés conjoints, sauts compensés, notes de l'accord sur les temps forts, arche.
5. Basse selon la catégorie, passages vers l'accord suivant.
6. Note de qualité et règles éliminatoires, choix du meilleur candidat.
7. Éditeur de musique (P4.19) : grille selon la gamme, notes longues, enregistrement avec un
   nom, aux couleurs de chaque thème.
8. Bouton *Au hasard* et choix de la catégorie, annulation (P4.20).
9. Liste « Mes musiques » (P4.22) : écouter, modifier, choisir la musique des parties,
   renommer, supprimer.
10. Mettre à jour la maquette (P4.21).

## Écrans

Les écrans existent deux fois : dans la maquette BloX (vues V7 à V18, une rangée par thème,
voir [maquette.md](maquette.md)), et dans le prototype à part, banc d'essai des quatre thèmes
(réglage *theme* des Tweaks).

### Mes musiques

- Bouton retour vers l'accueil ; en haut, la musique jouée pendant les parties.
- Une carte par musique : écouter, nom (le toucher ouvre l'éditeur), catégorie et tempo,
  *Utiliser* / *En jeu*, menu « … » (*Modifier*, *La jouer pendant les parties* ou *Ne plus la
  jouer*, *Supprimer*). Pendant l'écoute, la carte affiche « Lecture · mesure n sur 8 ».
- Une seule musique personnelle est jouée à la fois ; sans elle, c'est la musique choisie à
  l'accueil.
- Boutons *Au hasard* (ouvre l'éditeur sur une musique générée) et *Nouvelle*.


### Mes musiques : stockage (implémenté, P4.22 tranche 1)

`MusicLibrary` (`core:data`, instance dans `AppContainer`) garde les musiques enregistrées,
sans écran encore. Chaque musique (`SavedMusic`) a une identité personnelle
(`ScoreId.Personal`, clé tirée par `newId()`), un nom de 40 caractères au plus sans espaces
autour, une catégorie (nulle pour une musique faite à la main), une gamme et sa partition (qui
porte le tempo). Le dépôt garde l'ordre de la liste, enregistre (crée ou remplace à sa place),
renomme, supprime en rendant de quoi restaurer à sa place (`DeletedMusic`, avec l'état « en
jeu »), et retient la musique jouée pendant les parties, une au plus, effacée par sa
suppression et rétablie par la restauration. Les règles de nom (`MusicNames`, une seule source
pour le dépôt et l'éditeur) : un nom est pris sans tenir compte de la casse ni des espaces, la
musique en cours exceptée par son identité ; nom proposé « Catégorie n » (premier n libre) ;
nom libre « X 2 », ou le nombre suivant si le nom finit par un nombre. Le dépôt refuse un nom
pris (précondition vérifiée par l'appelant). La musique choisie pour les parties est jouée
à chaque partie à la place du choix de l'accueil (P4.22b, voir musique.md). Format du fichier : [ADR 0017](adr/0017-format-des-musiques-enregistrees.md).

`MusicLibraryContract › without a saved music the list is empty and no music is played during the games`,
`MusicLibraryContract › a new music saved goes after the others`,
`MusicLibraryContract › a music saved again replaces the saved one at its place`,
`MusicLibraryContract › a music whose name another one has, the case ignored, is refused and nothing is saved`,
`MusicLibraryContract › a music saved again may keep its name, in another case`,
`MusicLibraryContract › a renamed music keeps its place and its score`,
`MusicLibraryContract › a music cannot be renamed with the name of another one, the case ignored`,
`MusicLibraryContract › a music may be renamed with its own name in another case`,
`MusicLibraryContract › renaming a music that is not saved is refused`,
`MusicLibraryContract › the music chosen for the games is played during the games, as it is saved now`,
`MusicLibraryContract › no music chosen for the games plays the music of the home screen again`,
`MusicLibraryContract › a music that is not saved cannot be chosen for the games`,
`MusicLibraryContract › a deleted music leaves the list, and what is deleted tells its place`,
`MusicLibraryContract › the music played during the games, once deleted, is no longer played, and what is deleted tells it was`,
`MusicLibraryContract › deleting another music leaves the one played during the games`,
`MusicLibraryContract › deleting a music that is not saved gives nothing back and changes nothing`,
`MusicLibraryContract › a deleted music restored comes back at its place`,
`MusicLibraryContract › the music played during the games, restored, is played during the games again`,
`MusicLibraryContract › a music restored after the musics before it were deleted comes back at the end of the list`,
`MusicLibraryContract › a music restored twice is in the list once`,
`MusicLibraryContract › a music whose name was taken since its deletion cannot be restored`,
`MusicLibraryContract › each new identity is another one`,
`MusicLibraryContract › saving another music emits nothing on the music played during the games`,
`MusicLibraryContract › the list emits each change once`,
`MusicLibraryContract › restoring a music that was not played during the games leaves the one that is`,
`MusicLibraryContract › a new identity is none of the saved musics`,
`DataStoreMusicLibraryTest › the musics and the one played during the games survive a new library on the same file`,
`DataStoreMusicLibraryTest › a corrupted musics file gives an empty list, and a new music is saved`,
`DataStoreMusicLibraryTest › a musics file of version 1 is read as it was written, a format that must not change`,
`DataStoreMusicLibraryTest › a musics file of a later version gives an empty list, and is kept for that version`,
`DataStoreMusicLibraryTest › a musics file that counts billions of musics gives an empty list, without allocating them`,
`DataStoreMusicLibraryTest › a musics file that cannot be read gives an empty list and no music for the games`,
`DataStoreMusicLibraryTest › a failure other than an IOException while the musics are read is not absorbed`,
`DataStoreMusicLibraryTest › a music whose name is too long is refused and the saved musics are kept`,
`DataStoreMusicLibraryTest › a music the file cannot hold is refused before a byte is written, and the saved musics are kept`,
`DataStoreMusicLibraryTest › the names of the enums written in the musics file do not change`,
`DataStoreMusicLibraryTest › a music of a category the file does not know is read without a category`,
`DataStoreMusicLibraryTest › a musics file cut short in the middle of a music gives an empty list, and a new music is saved`,
`DataStoreMusicLibraryTest › a musics file of a version below 1 is corrupted, and gives an empty list`,
`DataStoreMusicLibraryTest › a corrupted musics file is copied aside before the list starts again empty`,
`DataStoreMusicLibraryTest › a music for the games the file does not hold is forgotten, even when a music of its identity is saved`,
`DataStoreMusicLibraryTest › a musics file holding two musics of the same identity is corrupted, and gives an empty list`,
`DataStoreMusicLibraryTest › a musics file of version 1 with a music for the games, written by hand, is read as it was written`,
`MusicNamesTest › a name is taken by a music of the same name, the case ignored`,
`MusicNamesTest › a name no music has is free`,
`MusicNamesTest › the music being saved does not take its own name, in another case or not`,
`MusicNamesTest › the spaces around a name do not make it another one`,
`MusicNamesTest › a free name is the name wanted when no music has it`,
`MusicNamesTest › a taken name is followed by the first number from 2 that makes it free`,
`MusicNamesTest › the music being saved keeps its own name as a free one`,
`MusicNamesTest › a taken name that ends with a number counts on from it`,
`MusicNamesTest › a free name leaves out the spaces around the name wanted`,
`MusicNamesTest › a taken name ending with a number too big to count on is followed by 2`,
`MusicNamesTest › the proposed name numbers the category, the first number from 1 that no music has`,
`MusicNamesTest › a name is taken whatever its case, its accents in capitals and the spaces around it`,
`MusicNamesTest › a taken name is offered free with the next number that no music has, the case of the name wanted kept`,
`MusicNamesTest › the music being saved keeps its own proposed name`,
`SavedMusicTest › a saved music is known by the identity of its score`,
`SavedMusicTest › a saved music has a name that is not blank`,
`SavedMusicTest › a saved music has no spaces around its name`,
`ScoreIdTest › a personal score is not known by a blank key`,
(contrat joué par `FakeMusicLibraryTest` et `DataStoreMusicLibraryTest`). Vues : V7, V10, V13, V16 (pas encore d'écran).
### Éditeur

De haut en bas :

1. **En-tête** : retour ; titre à toucher pour renommer (icône crayon), avec la ligne d'info
   (catégorie, « modifiée » ou « non enregistrée ») ; menu « … » (*Renommer*, *Suivre la
   lecture*, *Montrer l'autre piste en filigrane*, *Supprimer*).
2. **Onglets** *Mélodie* / *Basse*.
3. **Barre d'outils** : *Poser* / *Allonger*, *Annuler*, *Copier la mesure*, *Effacer la
   mesure*. Un bouton sans effet est désactivé (Copier sur la mesure 8, Effacer sur une mesure
   vide).
4. **Grille** d'une mesure, sous une ligne « Temps 1 2 3 4 » alignée sur les paires de cases.
   Les notes sont nommées sans chiffre d'octave (« Sol ») ; un espace sépare les deux octaves,
   et le lecteur d'écran dit « Sol haut » ou « Sol bas ». Grille vide : « Touche une case pour
   poser une note ».
5. **Huit boutons de mesure** ; la mesure jouée porte un repère distinct de la mesure
   sélectionnée.
6. **Panneau du bas** : *Au hasard* suivi des catégories (cibles de 44 px, coche sur la
   catégorie choisie), puis tempo en temps/min, *Écouter* (bouton rond) et *Enregistrer*,
   coloré et précédé d'un point quand il y a des changements.

### Règles de la grille

- **Poser** : toucher une case pose une croche ; toucher une note l'efface. Poser une note au
  milieu d'une note longue coupe celle-ci.
- **Notes longues** : toucher une case puis glisser vers la droite sur la ligne ; la note dure
  jusqu'à la case où l'on lève le doigt. Sans glisser, le mode *Allonger* donne le même
  résultat : toucher une case à droite d'une note l'allonge jusque-là, toucher une case tenue
  la raccourcit, toucher l'attaque la ramène à une case.
- Les notes tenues sont dessinées en bande au milieu des cases, dans une teinte plus claire que
  leur attaque.
- Pendant l'écoute, un contour entoure toute la colonne jouée.
- **Autre piste en filigrane** (désactivable) : de petits ronds marquent les notes de l'autre
  piste, sur la ligne du même nom de note.
- Les lignes hors de la gamme de la catégorie ont un libellé estompé et sont annoncées « hors
  gamme ».
- **Suivre la lecture**, activé par défaut : la grille suit la mesure jouée. Toucher une autre
  mesure pendant l'écoute coupe le suivi, et un message l'explique.
- **Annuler** (un seul niveau) : le dé, l'effacement et la copie d'une mesure. Chacune de ces
  actions affiche un message qui rappelle qu'on peut l'annuler. Le dé ne garde pas
  d'annulation quand la grille était vide.


### Éditeur : modèle (implémenté, P4.19 tranche 1)

Le modèle de l'éditeur vit dans `feature:composer` (paquet `model`, interne, sans Android,
testé sur la JVM) ; l'écran, l'écoute et l'enregistrement viendront dans les tranches
suivantes. Choix de conception, **[à vérifier]** à l'essai :

- **Grille** : 8 mesures de 8 cases (une croche par case) par piste ; les notes sont gardées à
  la double croche, deux par case, pour qu'une musique générée (doubles croches de Rétro,
  croches pointées de Joyeux) s'ouvre et revienne à l'identique ; un geste agit toujours sur
  une case entière. Lignes : les notes de la gamme dans la tessiture du générateur (mélodie do4
  à do6, basse do2 à do4, une seule source dans `core:music`) ; une note ouverte hors gamme ou
  hors tessiture reçoit sa propre ligne, marquée hors gamme. Musique neuve : do majeur, sans
  catégorie, 120 temps/min.
- **Poser** : toucher une case pose une croche ; toucher une note, son attaque ou sa partie
  tenue, l'efface entière ; poser au milieu d'une note longue la coupe. **Glisser** allonge
  jusqu'à la case levée, sans dépasser la barre. **Allonger** : une case à droite allonge
  (en recouvrant les notes d'autres lignes), une case tenue raccourcit, l'attaque ramène la note
  à la fin de sa case.
- **Barre d'outils** : copier la mesure sur la suivante (refusé en mesure 8), effacer la mesure
  (refusé sur une mesure vide ; la queue d'une note qui déborde sur la mesure suivante reste).
  **Annuler** (un niveau) couvre le dé, l'effacement et la copie ; toute autre modification le
  retire, et le dé sur une grille vide n'en garde pas.
- **Tempo** de 60 à 200 temps/min, par pas de 10 (toutes les catégories, Korobeïniki et le
  Menuet y tiennent).
- **Enregistrement** : « modifiée » compare le contenu à la version enregistrée ; une musique
  neuve restée vide se quitte sans question. Nom proposé « Catégorie n » (premier n libre),
  nom unique sans tenir compte de la casse ni des espaces, la musique en cours exceptée ; un nom
  pris est proposé libre avec le numéro suivant (« Thème 2 » donne « Thème 3 »).
- **Conversions** : une partition s'ouvre avec sa gamme et sa catégorie (fournies par le
  générateur ou la musique enregistrée) si elle est en 4/4, sur 8 mesures, à une ou deux voix,
  en doubles croches entières et au tempo de l'éditeur ; sinon elle ne s'ouvre pas (le Menuet,
  en 3/4). Elle revient avec les ondes et volumes des partitions ; les silences sont réécrits
  un par mesure (même musique entendue).

`CellTest › a cell lies in one of the eight bars, on one of their eight eighth notes`,
`EditorSavingTest › a new music is not saved`,
`EditorSavingTest › an opened music is saved until it is changed, then modified`,
`EditorSavingTest › a music changed back to its saved version is saved again`,
`EditorSavingTest › once saved, the music is saved, and changes are counted from there`,
`EditorSavingTest › leaving a new music asks first, unless it was left empty`,
`EditorSavingTest › leaving a saved music asks first only when it was changed, even if it was emptied`,
`EditorSavingTest › an empty grid cannot be saved`,
`EditorSavingTest › a music cannot be saved under the name of another music, and can under a free name`,
`EditorSavingTest › a music cannot be saved without a name`,
`EditorStateTest › touching a cell places a note in the selected bar of the selected track`,
`EditorStateTest › in Lengthen mode, touching a cell lengthens the note on its left`,
`EditorStateTest › sliding along a line places a long note in the selected bar of the selected track`,
`EditorStateTest › copying the bar copies it over the next one, on the open track only, and shows the next bar`,
`EditorStateTest › Copy the bar is disabled on the eighth bar, which has no bar after it`,
`EditorStateTest › copying the eighth bar is refused`,
`EditorStateTest › clearing the bar silences it on the open track only`,
`EditorStateTest › Clear the bar is disabled on a bar of the open track where no note sounds`,
`EditorStateTest › the tempo buttons speed the music up or slow it down, and take the undo away`,
`EditorStateTest › the editor shows one of the eight bars`,
`EditorStateTest › clearing a bar where no note sounds is refused`,
`EditorUndoTest › clearing the bar can be undone, once`,
`EditorUndoTest › copying the bar can be undone`,
`EditorUndoTest › touching or sliding a note takes the undo away, as touching the cell again undoes it`,
`EditorUndoTest › the dice replaces the music, shows its first bar, and can be undone`,
`EditorUndoTest › the dice keeps no undo when the grid was empty`,
`EditorUndoTest › the dice on an emptied grid takes away the undo left by clearing it`,
`GridNoteTest › a note lasts at least a sixteenth note`,
`ScoreDraftOpeningTest › a score opens with its long notes, its rests and its two voices, on its scale, category and tempo`,
`ScoreDraftOpeningTest › a score in three beats a bar, such as the Minuet, does not open`,
`ScoreDraftOpeningTest › a score of other than eight bars does not open`,
`ScoreDraftOpeningTest › a score with a note shorter than a sixteenth or off the sixteenth grid, such as a triplet, does not open`,
`ScoreDraftOpeningTest › a score of a single voice opens as a melody without bass`,
`ScoreDraftOpeningTest › a score of more than two voices does not open`,
`ScoreDraftOpeningTest › a note out of the scale, such as a chromatic passing note, opens on a line of its own, out of the scale`,
`ScoreDraftOpeningTest › a note out of the range of its track opens on a line of its own, once`,
`ScoreDraftOpeningTest › a score whose tempo is out of the tempos of the editor does not open`,
`ScoreDraftOpeningTest › a score in two beats a bar, sixteen bars long, does not open`,
`ScoreDraftRoundTripTest › every music of the generator opens on its scale and comes back unchanged`,
`ScoreDraftRoundTripTest › a generated music with sixteenth notes, dotted eighths and rests comes back unchanged`,
`ScoreDraftRoundTripTest › the arranged Korobeiniki opens and comes back unchanged`,
`ScoreDraftRoundTripTest › a score comes back with its silences written one rest per bar, the music heard unchanged`,
`ScoreDraftTest › an empty draft plays as eight bars of silence on the melody and bass sounds of the scores, at its tempo`,
`ScoreDraftTest › the notes of a track play with their length, the silences between them as rests that stop at the bar lines`,
`ScoreDraftTest › a draft is empty while neither track has a note`,
`TempoTest › a tempo is refused outside 60 to 200 beats per minute`,
`TempoTest › a tempo of 60 or 200 beats per minute is accepted`,
`TempoTest › speeding up adds 10 beats per minute, up to 200`,
`TempoTest › slowing down takes 10 beats per minute away, down to 60`,
`TrackGridBarsTest › clearing a bar silences it and leaves the other bars as they were`,
`TrackGridBarsTest › a bar is empty when no note sounds in it, even held from the bar before`,
`TrackGridBarsTest › copying a bar replaces the next bar with its notes`,
`TrackGridBarsTest › a note held over the bar line is copied up to the bar line only`,
`TrackGridBarsTest › clearing a bar leaves the next bar the tail of a note held over the bar line`,
`TrackGridBarsTest › clearing a bar that a long note crosses keeps the note before and after the bar`,
`TrackGridTest › the melody rows are the notes of the scale from C4 to C6`,
`TrackGridTest › the bass rows are the notes of the scale from C2 to C4, a pentatonic scale leaving out its missing degrees`,
`TrackGridTest › a note is placed only on a line of the grid`,
`TrackGridTest › touching an empty cell places an eighth note`,
`TrackGridTest › touching a note erases it`,
`TrackGridTest › placing a note in the middle of a long note cuts the long note there`,
`TrackGridTest › placing a note where another pitch starts replaces that note, even the part held after the cell`,
`TrackGridTest › sliding to the right from an empty cell places a note that lasts until the cell where the finger is lifted`,
`TrackGridTest › sliding from a note of the same line lengthens that note from its attack`,
`TrackGridTest › a note slid past the end of its bar stops at the bar line`,
`TrackGridTest › sliding to the left places an eighth note`,
`TrackGridTest › a slid note covers the notes it passes over`,
`TrackGridTest › in Lengthen mode, touching a cell to the right of a note lengthens the note up to there`,
`TrackGridTest › in Lengthen mode, touching a held cell shortens the note to end there`,
`TrackGridTest › in Lengthen mode, touching the attack of a note brings it back to one cell`,
`TrackGridTest › in Lengthen mode, touching a cell with no note of its line on its left in the bar places an eighth note`,
`TrackGridTest › touching the held part of a note erases the whole note`,
`TrackGridTest › touching one of two notes that share a cell erases that note only, and a held note whole`,
`TrackGridTest › in Lengthen mode, a lengthened note covers the notes of other lines between it and the cell`,
`TrackGridTest › in Lengthen mode, touching an attack in the middle of a cell brings the note back to the end of that cell`,
`TrackGridTest › a grid holds its notes within the eight bars, one at a time`,
`TrackTest › each track spans the range in which the generator writes its voice`.

Vues : V8, V11, V14, V17 (écran : section suivante).

### Éditeur : écran (implémenté, P4.19b)

Le bouton « COMPOSER UNE MUSIQUE » de l'accueil (sous les choix de musique) ouvre l'éditeur sur
une musique neuve ; ouvrir l'éditeur arrête l'aperçu. L'écran (`ComposerScreen`, sans logique)
porte l'en-tête et son menu (*Montrer l'autre piste en filigrane*), les onglets *Mélodie* /
*Basse* (l'onglet choisi prend la couleur de sa piste en Couleurs), la barre d'outils (*Poser* /
*Allonger*, *Annuler*, *Copier*, *Effacer*, désactivés quand ils sont sans effet), la grille d'une
mesure, les huit boutons de mesure et le tempo (− / +, ne font rien aux bornes). La grille : une
ligne par note, de la plus haute à la plus basse, un espace entre les octaves, au moins 26 dp de
haut (elle défile si la place manque) ; une case partagée en deux doubles croches dessinée en
deux moitiés, une note tenue en bande plus pâle, le filigrane de l'autre piste en rond par-dessus
(une note de l'autre piste sans ligne correspondante n'est pas marquée) ; les lignes hors gamme
estompées et en italique. Gestes : toucher pose ou efface, glisser sur la ligne allonge ; un geste
annulé par le système, un doigt revenu sur sa case ou un mouvement vertical (défilement) ne
modifient rien. TalkBack lit chaque case par sa note, son octave et son temps (« Sol haut, temps 2
et demi »), son état et l'action qu'un toucher ferait (poser, effacer, allonger, raccourcir,
ramener à une case) ; onglets et mesures en onglets, modes en boutons radio, tempo annoncé à
chaque changement. Les cases n'atteignent pas 48 dp de large sur un téléphone de 360 dp (huit par
ligne) ; elles font au moins 24 dp en tout sens, et le mode *Allonger* et l'action TalkBack de
chaque case compensent. L'enregistrement, l'écoute et le dé viennent ensuite (P4.19c, P4.19d,
P4.20). Maquette à mettre à jour (P5.40). À essayer sur un téléphone.

**Tests :**
`ComposerUiStateTest › a tempo button is disabled at the end of the tempos, where it has no effect`,
`ComposerUiStateTest › the hint of an empty grid shows while the open track has no note, whatever the other track holds`,
`ComposerViewModelTest › the editor opens a new music, with the other track shown in watermark`,
`ComposerViewModelTest › touching a cell or sliding along a line edits the open track, in the selected bar`,
`ComposerViewModelTest › the tabs, the bar buttons and the mode choose what the grid shows and what a touch does`,
`ComposerViewModelTest › the toolbar copies the bar, clears it, and undoes the last of them`,
`ComposerViewModelTest › a toolbar action without effect, its button disabled, does nothing when it comes all the same`,
`ComposerViewModelTest › the tempo buttons speed the music up and slow it down`,
`ComposerViewModelTest › the fastest tempo button, disabled, does nothing when it comes all the same, and keeps the undo`,
`ComposerViewModelTest › the slowest tempo button, disabled, does nothing when it comes all the same, and keeps the undo`,
`ComposerViewModelTest › the menu hides the other track in watermark, and shows it again`,
`ComposerViewModelTest › the factory that app builds makes an editor on a new music`,
`BeatTest › the eight cells of a bar are the four beats and their halves, two cells a beat`,
`CellActionTest › in Place mode touching a cell places a note on an empty cell, and erases the note it shows otherwise`,
`CellActionTest › in Lengthen mode a touch places, brings back to one cell, shortens or lengthens, as the model does`,
`CellActionTest › in Lengthen mode a note of the bar before is not lengthened, a note is placed`,
`CellGestureTest › a finger lifted without sliding farther than the touch slop touches its cell`,
`CellGestureTest › a finger slid along the line slides from its cell to the cell where it is lifted`,
`CellGestureTest › a finger slid then brought back to its cell does nothing, neither a touch nor a slide`,
`CellGestureTest › a cancelled gesture does nothing`,
`CellGestureTest › a finger moved up or down before sliding is left to the scroll of the grid, and does nothing`,
`CellGestureTest › a slide past the ends of the line stops at its first or last cell`,
`CellViewTest › a cell shows the attack of a note, its held part, or nothing`,
`CellViewTest › a cell is split in two sixteenth notes when its halves do not show one note`,
`GridRowsTest › the lines of the open track are shown from the highest down`,
`GridRowsTest › a space separates the octaves of the grid`,
`GridRowsTest › each line shows the eight cells of the selected bar of the open track`,
`GridRowsTest › the notes of the other track mark the cells where they sound, on the line of the same name and octave`,
`GridRowsTest › a line out of the scale is shown as such`,
`GridRowsTest › a note of the other track whose line the open track does not have is not marked`,
`GridRowsTest › each cell tells what touching it does, in the mode of the toolbar`,
`RowNameTest › a line is named after its note without octave, low in the first octave of the track and high in the second`,
`RowNameTest › the twelve notes of an octave are named with sharps, as the pitches are written`,
`RowNameTest › the top of the range, a third octave, is very high, and a note opened below the range very low`,
`TrackColorsTest › the attack and the held part of a note stand out from the cell by a contrast of 3 to 1 at least`,
`TrackColorsTest › the held part of a note is paler than its attack, standing out less from the cell`,
`TrackColorsTest › the selected tab of a track stands out from the unselected one, its label readable on it`,
`TrackColorsTest › the name of a line out of the scale is dimmer than the others, and still readable by a contrast of 4,5 to 1`,
`TrackTest › the other track of the melody is the bass, and of the bass the melody`,
`ComposerScreenTest › touching an empty cell places a note, and touching it again erases it`,
`ComposerScreenTest › a touch cancelled by the system, as the back gesture does, places no note`,
`ComposerScreenTest › on a small phone with the largest font, every cell is 24 dp wide and high at least`,
`ComposerScreenTest › sliding along a line places a note held up to the cell where the finger is lifted`,
`ComposerScreenTest › the tabs open the grid of the bass or of the melody, with the notes of the other track in watermark`,
`ComposerScreenTest › the menu hides the watermark of the other track`,
`ComposerScreenTest › a toolbar button without effect is disabled`,
`ComposerScreenTest › TalkBack names each cell by its note, its octave and its beat, and says what touching it does`,
`ComposerScreenTest › TalkBack says a line out of the scale, and two sixteenth notes in a cell`,
`ComposerScreenTest › touching a cell recomposes its line only, not the whole grid`,
`ComposerScreenTest › the tempo buttons change the tempo shown`,
`ComposerScreenTest › every button is 48 dp wide and high at least, but the bars and the cells, 48 dp high for the bars`,
`HomeViewModelTest › opening the music editor stops the preview`,
`HomeScreenTest › the button Composer une musique, shown in capitals and read as written, opens the music editor`.

### Éditeur : écoute (implémentée, P4.19c)

- **Écouter / Arrêter** : bouton rond à droite du tempo (triangle ou carré, 48 dp), lu « Écouter » ou
  « Arrêter ». Il joue la musique en cours par `MusicPlayer`, depuis la première case ; un seul
  aperçu à la fois, comme à l'accueil (P4.7). Désactivé tant qu'aucune piste n'a de note. Une
  musique qui s'arrête d'elle-même (focus refusé ou perdu, sortie audio en échec), même aussitôt
  lancée, ramène le bouton à « Écouter ».
- **Colonne jouée** : `Playback` ne dit pas sa position ; l'écran envoie ses images
  (`withFrameNanos`, ADR 0012), `FrameTicks` les convertit en ticks entiers (sans dérive, tous les
  ticks rattrapés après un saut d'images, aucun retiré si le temps recule) et `PlayHead` avance de
  ticks × bpm (une case par croche, 15 ticks à 120 temps par minute), boucle après la mesure 8 et
  suit un changement de tempo depuis l'endroit où elle se trouve. La colonne porte un contour en
  deux traits, une ligne `ink` de 2 dp et un halo intérieur `boardBackground` de 1 dp : aucune
  couleur unique ne se détache à 3:1 à la fois des cases claires et des notes foncées du thème
  Original. Il est dessiné par-dessus la grille, sans recomposer les lignes. La mesure jouée porte
  un trait sous son numéro et TalkBack la lit « en lecture ».
- **Suivre la lecture**, activé par défaut, dans le menu « … » : la grille montre la mesure jouée.
  Toucher une autre mesure pendant l'écoute coupe le suivi ; un message en bas de la grille le dit
  (« La grille ne suit plus la lecture. Menu « … » pour la suivre à nouveau. »), sans recouvrir
  Arrêter. Coupé ainsi, le suivi revient au prochain *Écouter* ; décoché dans le menu, il le reste.
- Choix **[à vérifier]** à l'essai :
  - modifier les notes pendant l'écoute (toucher, glisser, copier, effacer, annuler) arrête la
    musique, pour ne jamais entendre une version périmée ;
  - changer le tempo laisse la musique jouer au nouveau tempo (`setTempoFactor`) ; les onglets, le
    mode et le filigrane aussi ;
  - la musique s'arrête quand l'éditeur passe en arrière-plan (`ON_STOP`, donc aussi à une
    rotation) et quand on le quitte ; elle continue derrière un dialogue.
- Limites connues : la colonne part à la première image, avant le son (rendu de la partition,
  latence du tampon de 100 à 350 ms, attente du focus) ; après un changement de tempo, le
  séquenceur ne bascule qu'au temps suivant (décalage d'un temps au plus, sans cumul) ; une perte
  passagère du focus (assistant vocal) met le son en pause mais pas la colonne. Un `Playback` qui
  exposerait sa position les supprimerait (P4.25).
- La partition jouée a l'identité provisoire `Personal("editor")` ; P4.19d donnera celle de la
  musique enregistrée.

**Tests :** `PlayHeadTest › the play head starts on the first cell of the first bar`,
`PlayHeadTest › the play head moves on a cell each eighth note, 15 ticks at 120 beats per minute`,
`PlayHeadTest › the play head comes back to the first cell after the eighth bar, as the music loops`,
`PlayHeadTest › after a change of tempo, the play head moves on at the new tempo from where it stood`,
`FrameTicksTest › the first frame only sets the time reference`,
`FrameTicksTest › each frame gives the whole ticks of 1 60th of a second since the frame before, the rest carried over`,
`FrameTicksTest › a frame time going back takes no tick away, and the ticks go on from it`,
`ComposerUiStateTest › Listen is disabled while neither track has a note, as there is nothing to hear`,
`ComposerViewModelTest › listening plays the music being edited, from its first cell`,
`ComposerViewModelTest › listening again stops the music, and no cell is played`,
`ComposerViewModelTest › the played cell follows the frames of the screen, a cell each eighth note at the tempo of the music`,
`ComposerViewModelTest › following the playback, as it does at first, the grid shows the bar being played`,
`ComposerViewModelTest › choosing another bar than the one played stops following the playback, and a message says so`,
`ComposerViewModelTest › following stopped by choosing another bar comes back with the next listening, unlike following unchecked`,
`ComposerViewModelTest › a message shown goes away`,
`ComposerViewModelTest › the menu stops following the playback, and follows it again from the bar played`,
`ComposerViewModelTest › changing the notes while listening stops the music, never playing a version gone`,
`ComposerViewModelTest › the tabs, the mode and the watermark leave the music playing`,
`ComposerViewModelTest › changing the tempo while listening plays the music on, at the new tempo`,
`ComposerViewModelTest › after a change of tempo while listening, the played cell moves on at the new tempo`,
`ComposerViewModelTest › Listen, disabled on an empty music, plays nothing when it comes all the same`,
`ComposerViewModelTest › the music stops when the editor goes to the background, and plays on behind a dialog`,
`ComposerViewModelTest › leaving the editor stops the music`,
`ComposerViewModelTest › a music that stops by itself, as the audio output fails, offers to listen again`,
`ComposerViewModelTest › a music that stops as soon as it is played, as when the audio focus is refused, offers to listen again`,
`TrackColorsTest › the outline of the column played stands out from the cells, empty or filled, and the page by 3 to 1 at least`,
`ComposerScreenTest › Listen plays the music being edited, disabled while it has no note, and Stop stops it`,
`ComposerScreenTest › the bar played is read as such; choosing another stops following the playback, as the menu shows`,
`ComposerScreenTest › Stop stays within reach while the message about following the playback shows`.
Vues : V8, V11, V14, V17.

### Enregistrer

- *Enregistrer* ouvre une fenêtre : le nom (proposé d'après la catégorie, par exemple
  « Joyeux 2 ») et la case « Jouer cette musique pendant les parties ». Entrée valide.
- Enregistrer est impossible si la grille est vide, ou si le nom est déjà pris (comparaison
  sans tenir compte des majuscules, en ignorant la musique en cours) ; un bouton propose alors
  un nom libre, « Prendre « X 2 » ».
- Quitter avec des changements non enregistrés demande d'enregistrer, de quitter sans
  enregistrer ou de continuer. Quitter une nouvelle musique restée vide ne pose pas de
  question.

### Supprimer

- Depuis le menu « … » de la carte ou de l'éditeur. Une confirmation suit (*Supprimer* ou
  *Garder la musique*) ; si la musique est jouée pendant les parties, elle prévient que la
  musique de l'accueil la remplacera.
- Après la suppression, on revient à *Mes musiques*. Le message « Annuler » reste jusqu'à
  l'action suivante ou jusqu'à sa croix *Fermer le message* : la musique revient à sa place,
  avec son choix *En jeu*.
- Le bouton de suppression prend la couleur `danger` du thème (rouge en Couleurs et Brume,
  encre en Original, faute de rouge dans sa palette).
