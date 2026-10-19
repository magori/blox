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

## Catégories

L'utilisateur choisit une catégorie avant de lancer le dé. *Surprise* tire la catégorie
elle-même au hasard.

| Catégorie | Gamme | Tempo (temps/min) | Suites d'accords | Rythme de la mélodie | Basse |
|---|---|---|---|---|---|
| **Joyeux** | majeure | 120–150 | I–V–vi–IV, I–IV–V–I | croches, croches pointées, sautillant | bondissante (octaves) |
| **Calme** | pentatonique majeure | 70–95 | I–vi–IV–V, IV–I | blanches et noires, beaucoup d'air | rondes ou blanches |
| **Épique** | mineure naturelle | 110–140 | i–VI–III–VII, i–VII–VI–VII | noires fortes, sauts de quinte | galop (croches répétées) |
| **Mélancolique** | mineure naturelle | 80–100 | i–VI–iv–v, i–iv–VII–III | notes longues, descentes | fondamentale et quinte |
| **Mystérieux** | dorienne ou mineure harmonique | 90–110 | i–IV (dorien), i–♭VII–VI–V (harmonique) | notes tenues, chromatisme de passage | notes tenues |
| **Rétro** | majeure ou mixolydienne | 140–170 | I–VII–IV–I (mixolydien), I–V–vi–IV (majeur) | doubles croches, arpèges rapides | octaves rapides |
| **Danse** | mineure harmonique | 130–160 | i–iv–V–i | 2 temps marqués, croches | fondamentale et quinte, marquée |
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
- **Toniques** : do, ré, fa, sol, la (octave 4), communes à toutes les catégories. Aucune
  tessiture n'est encore définie dans le code : la règle « tient dans les tessitures » sera
  vérifiée avec la mélodie (P4.16).
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
  seulement (la lecture d'une demi-mesure viendra avec la forme, P4.15). Danse est à 4 temps,
  ses « 2 temps marqués » étant les temps 1 et 3.
  `CategoryTest › each category has the bass of its character`,
  `CategoryTest › only mysterious lets its melody pass through a note out of the scale`,
  `CategoryTest › only retro, the fastest category, changes chord twice a bar`.
- **Surprise** tire une des sept catégories, jamais elle-même ; même graine, même catégorie.
  `CategoryChoiceTest › surprise may draw every category`,
  `CategoryChoiceTest › surprise draws the same category from the same seed`,
  `CategoryChoiceTest › a category chosen by the player is played as it is, whatever the draw`.

Points ouverts : l'accord qui ferme la boucle **[à vérifier]** dans les gammes dont le V est
mineur ou absent des suites (mineure naturelle, dorien, mixolydien ; P4.15) ; les traits
mélodiques de la table (sauts de quinte, descentes, arpèges) ne sont pas encore des
paramètres (P4.16) ; les sept styles de basse pourront se réduire aux motifs de la section
Basse (P4.17). Aucune vue de maquette n'est concernée.

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
| Écriture à deux voix | aucune quinte ni octave parallèle | faible |

Règles **éliminatoires** : une note hors de la gamme (hors chromatisme permis), un saut de
plus d'une octave, un triton, une boucle qui ne finit pas sur l'accord de V, plus de trois
notes identiques de suite.

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
