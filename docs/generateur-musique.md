# Générateur de musique

Spécification du bouton **Au hasard** de l'éditeur de musique (prototype :
[Éditeur de musique BloX](https://claude.ai/artifact/M91HH7KRJWTMHeDvLaV8wz), lien privé).
Le générateur ne tire pas des notes au hasard. Il **compose** selon les règles d'écriture
qui rendent une mélodie agréable et facile à retenir. Le hasard choisit seulement entre des
possibilités qui sonnent toutes juste. L'utilisateur choisit une **catégorie** (Joyeux,
Calme, Épique…), qui fixe le caractère de la musique.

Rien n'est encore implémenté. Les valeurs et les sources sont **[à vérifier]** : elles
viennent de mémoire et doivent être confirmées avant d'écrire le test correspondant.

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
| **Mélancolique** | mineure naturelle | 80–100 | vi–IV–I–V, i–iv–VII–III | notes longues, descentes | fondamentale et quinte |
| **Mystérieux** | dorienne ou mineure harmonique | 90–110 | i–IV (dorien), i–VII–VI–V | notes tenues, chromatisme de passage | notes tenues |
| **Rétro** | majeure ou mixolydienne | 140–170 | I–♭VII–IV–I, I–V–vi–IV | doubles croches, arpèges rapides | octaves rapides |
| **Danse** | mineure harmonique | 130–160 | i–iv–V–i | 2 temps marqués, croches | fondamentale et quinte, marquée |
| **Surprise** | une des catégories ci-dessus, tirée au hasard | | | | |

Chaque catégorie est un jeu de paramètres, pas un code à part : un seul générateur, des
catégories interchangeables, comme les politiques de `core:game`. Ajouter une catégorie ne
modifie pas le générateur.

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

Le prototype montre deux écrans, dans les quatre thèmes (réglage *theme* des Tweaks) :

- **Mes musiques** : une carte par musique (écouter, nom, catégorie et tempo, *Utiliser* /
  *En jeu*, modifier) ; la musique des parties est rappelée en haut. Une seule musique
  personnelle est jouée à la fois ; sans elle, c'est la musique choisie à l'accueil.
  Boutons *Au hasard* (ouvre l'éditeur sur une musique générée) et *Nouvelle*.
- **Éditeur** : le titre se touche pour renommer ; *Enregistrer* ouvre une fenêtre avec le nom
  (proposé d'après la catégorie, par exemple « Joyeux 2 ») et la case « Jouer cette musique
  pendant les parties ». Quitter avec des changements non enregistrés demande d'enregistrer,
  de quitter sans enregistrer ou de continuer.
- **Suppression** : pour une musique déjà enregistrée, la fenêtre d'enregistrement propose
  *Supprimer cette musique*. Une confirmation suit (*Supprimer* ou *Garder la musique*) ; si
  la musique est jouée pendant les parties, elle prévient que la musique de l'accueil la
  remplacera. Après la suppression, on revient à *Mes musiques*, et un message propose
  *Annuler* pendant 6 secondes : la musique revient à sa place, avec son choix *En jeu*. Le
  bouton de suppression prend la couleur `danger` du thème (rouge en Couleurs et Brume, encre
  en Original, faute de rouge dans sa palette).
- Les notes tenues s'affichent dans une teinte plus claire que leur attaque ; les lignes hors
  de la gamme de la catégorie ont un libellé estompé.
