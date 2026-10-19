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
  une forme qu'aucune mélodie ne peut remplir est refusée. Pire temps mesuré : 32 ms à froid,
  6 ms à chaud sur la JVM.
  `MelodyTest › the same form and the same draw give the same melody`,
  `MelodyTest › the seed 42 on joyful gives this melody, which pins the order of the draws after the form`,
  `MelodyTest › the seed 1 on mysterious gives this melody in D dorian, which passes through D sharp and C sharp`,
  `MelodyTest › a form that no melody fits is refused in good time, rather than searched for ever`.

Ouverts pour P4.18 : notes étrangères approchées et quittées par degré (aujourd'hui une
tendance, non mesurée) ; renversement du motif en A'' non fait. Aucune vue de maquette n'est
concernée.

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

- **`core:music`** reçoit `MusicGenerator` (catégorie, tirage) → `Score`, avec des types
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
