# Graine du jour

La Graine du jour est une **partie Classique** dont la graine est la date : le même jour, tous
les joueurs reçoivent **les mêmes pièces**, dans le même ordre. On joue autant de fois qu'on veut
dans la journée ; l'application garde le **premier essai** et le **meilleur score** du jour, et
« Copier mon score » met un court texte dans le presse-papiers, pour le partager. Une nouvelle
graine vient à minuit.

Origine : cadrage « quatre modes musicaux » des agents `architecte` et `designer-jeu`
(2026-10-26), décisions du développeur le 2026-10-26. Architecture :
[ADR 0026](adr/0026-jeux-musicaux-variantes-du-classique.md) ; la suite des pièces d'une graine
devient un contrat (amendement de l'[ADR 0014](adr/0014-tirage-seul-etat-modifiable.md)).

> Traçabilité : Aucune règle n'est encore implémentée (phase P9 de [SUIVI.md](../SUIVI.md),
> après P1.20). Chaque règle recevra sa ligne **Tests :** `ClasseDeTest › nom du test` et, si
> elle se voit à l'écran, **Vues :** `V…`, à mesure qu'elle sera codée.

## Vocabulaire

| Terme | Code prévu | Sens |
|---|---|---|
| Graine du jour | `DailySeed` (`app`) | La graine d'une date, `AAAAMMJJ` (20261026 pour le 26 octobre 2026). |
| Générateur stable | `StableRandom` (`core:game`, P1.20) | Un générateur maison dont la suite est figée par un test, quelle que soit la version de Kotlin. |
| Résultats du jour | `DailyChallengeRepository` (`core:data`) | Les essais, le premier essai et le meilleur score de la date en cours. |
| Essai | — | Une partie de la Graine du jour, terminée ou quittée. |

## Règles de la partie

- Les règles sont celles du Classique (`ClassicRules`), sans aucun changement : rotation,
  gravité, score, niveaux, danger ([règles du mode Classique](regles-mode-classique.md)).
- **Niveau de départ imposé : 5**, quel que soit le niveau choisi à l'accueil (décision du
  développeur).
- **Musique imposée : Vivante** ([musique-vivante.md](musique-vivante.md)), composée sur la graine
  du jour, quelle que soit la musique choisie à l'accueil. Elle prime aussi sur la « musique en
  jeu » de « Mes musiques », dont la priorité sur le choix de l'accueil est décrite dans
  [musique.md](musique.md) : la Graine du jour joue toujours la Vivante. Le volume de la musique et le réglage
  des effets sonores restent ceux du joueur. La musique n'est **pas** un contrat : deux versions
  de l'application peuvent jouer deux musiques différentes pour une même date.
- Thème, commandes, vibrations, Effets réduits : ceux du joueur.

## Graine

- **Date** : la date **locale du téléphone** (fuseau du téléphone) au moment de Jouer, écrite
  `AAAAMMJJ` : `DailySeed.of(LocalDate)` rend la graine `20261026` pour le 26 octobre 2026.
  L'horloge est injectée (`Clock`), pour que les tests fixent la date.
- **Une partie commencée un jour compte pour ce jour-là** : la date est dans la clé de la partie
  (`GameRoute`, variante `DAILY`), pas relue à la fin. Rejouer garde la graine de la partie
  terminée, même après minuit.
- Deux joueurs dans deux fuseaux n'ont pas la même graine au même instant, mais ont la même
  graine pour la même date : c'est voulu (« la graine du 26 octobre »).

## Pièces : la suite est un contrat

- Le tirage reste celui du Classique (`ClassicRandomizer` : un premier tirage parmi 8, un second
  parmi 7 si on tire « aucune pièce » ou la précédente), mais branché sur `StableRandom` au lieu de
  `kotlin.random.Random(seed)`. Le constructeur `ClassicRandomizer(random)` est `internal` à
  `core:game` : P1.20 ajoute un point d'entrée **public**, `ClassicRandomizer.stable(seed)`
  (`explicitApi`), que `feature:game` appelle. `StableRandom` **étend `kotlin.random.Random`**,
  pour que le tirage l'accepte sans changer.
- **La suite des pièces d'une graine est un contrat** entre versions de l'application : un test
  fige les **20 premières pièces** de la graine `20261026` (P1.20). Changer le générateur ou le
  tirage casse ce test : c'est voulu, une telle modification demande un nouvel ADR.
- `StableRandom` est une fonction pure de sa graine (algorithme maison, documenté dans son code)
  **[à vérifier]** : l'algorithme exact (par exemple SplitMix64 ou xorshift) se choisit en P1.20.
  `nextInt(n)` tire **sans biais, par tirage avec rejet** ; un test vérifie qu'il prend chaque
  valeur de l'intervalle (`takes every value of the range`).
- Les parties Classiques ordinaires gardent leur graine tirée au hasard ; elles peuvent passer à
  `StableRandom` sans rien changer à leurs règles.

## Résultats du jour

- Un dépôt à part, `DailyChallengeRepository` (fichier DataStore à lui), **jamais** les records
  du Classique : une partie de la Graine du jour n'entre pas dans les records, et une partie
  Classique n'entre pas dans les résultats du jour.
- Il garde **la date en cours** et, pour elle :
  - le nombre d'**essais** ;
  - le score du **premier essai** ;
  - le **meilleur** score.
- Un essai est compté **à son début** **[à vérifier]** : le dépôt enregistre « essai commencé »
  dès Jouer (essais + 1) ; tant qu'il n'est pas fini, le premier essai vaut **0**, puis son score.
  Quitter (Pause puis Menu, Changer de jeu) garde les points du moment. Quitter ou tuer
  l'application pour « refaire » son premier essai ne permet donc pas de connaître les pièces
  sans que l'essai compte. Après la mort du processus, le ViewModel rejoue la partie de la route
  depuis sa graine (ADR 0014) : c'est **le même essai**, qui n'est pas compté une seconde fois.
- Un résultat se range sous **la date de sa partie** :
  - même date que le dépôt : essais + 1, meilleur mis à jour ;
  - date plus récente (ou dépôt vide) : le dépôt passe à cette date, premier essai = meilleur =
    ce score, 1 essai ;
  - date plus ancienne (une partie d'hier finie après une partie d'aujourd'hui) : ignoré.
- Le dépôt ne garde pas d'historique ni de série de jours (hors périmètre).
- Le dépôt a son contrat de test et son faux, partagés par les tests des features
  (`DailyChallengeRepositoryContract`, en testFixtures, comme les autres dépôts de `core:data`).
- Fichier exclu de la sauvegarde **[à vérifier]** : restauré sur un autre appareil, un premier
  essai n'aurait plus de sens.

## Copier mon score

- Texte copié (presse-papiers du système), pour la date en cours :
  « BloX · Graine du 26 octobre (20261026) · 1er essai 9 800 · meilleur 18 200 en 5 essais ».
  La date `AAAAMMJJ` y est écrite (décision du développeur), pour qu'on compare des scores de la
  même graine ; le jour en toutes lettres la rend lisible. Le libellé exact est **[à vérifier]**.
- Nombres groupés par trois avec U+202F (espace fine insécable), à leur **valeur réelle**, sans
  plafond ni zéros de tête (contrairement à l'écran de jeu, qui affiche 6 chiffres plafonnés,
  `ScoreText.kt`) ; « en 1 essai » au singulier.
- Désactivé avant le premier essai du jour.
- Android 13 et plus montrent déjà une confirmation de copie ; en dessous, un message
  « Score copié » s'affiche **[à vérifier]**.

## Écran

**Thème : Couleurs v2 seulement** (jour et nuit selon le téléphone), comme le Serrurier
(décision du développeur, 2026-10-26) : l'écran d'entrée et la partie de la Graine du jour sont
toujours dessinés en Couleurs, quel que soit le thème choisi pour le Classique ; leurs barres
système suivent cette variante (`systemBarVariant`, P7.18), qui doit donc connaître la variante
`DAILY` de `GameRoute` : c'est une différence par variante admise par l'ADR 0026 (thème imposé et
barres système).

**Écran d'entrée** (vue **V38**, à dessiner), sur le modèle de l'accueil v2 (V27) :

- barre : Changer de jeu, puce « MEILLEUR DU JOUR » (le meilleur score du jour, ou « — »),
  Paramètres ;
- titre « Graine du 26 octobre » ;
- encart « LES 7 PREMIÈRES » : les 7 premières pièces de la graine, numérotées de 1 à 7, tirées du
  `ClassicRandomizer` de la partie (non touchables, lues par TalkBack « Pièce 1 : T… ») ;
- trois tuiles : **Essais**, **Premier essai**, **Meilleur** ;
- **Jouer** XL, avec le rappel « Graine du jour · Niveau 05 · Vivante » ;
- « Copier mon score », désactivé avant le premier essai, avec la sous-ligne « Joue une partie
  pour copier ton score » ;
- en bas : « Nouvelle graine à minuit ».
- L'écran suit le changement de date : ouvert à 23 h 59, il passe à la graine du lendemain à
  minuit **[à vérifier]** (sinon, à son prochain affichage).

**En jeu** (vue **V39**, à dessiner) : l'écran de jeu v2 (V28), titre « GRAINE DU JOUR », le
meilleur du jour à la place du record. Pause comme V29.

**Fin de partie** (V39) : comme V30 ; « MEILLEUR DU JOUR » si le meilleur du jour est battu (au
premier essai aussi), puis **Rejouer** (même graine) et **Copier**.

**Cartouche** « Graine du jour » sur le choix du jeu (V36) : elle porte l'autocollant
« NOUVELLE » **jusqu'au premier essai du jour** ; elle ouvre l'écran d'entrée.

## Points ouverts

- L'essai compté à son début (ci-dessus) et la sauvegarde du fichier.
- Rejouer après minuit : une partie rejouée avec la graine d'hier est ignorée si le dépôt est déjà
  passé à aujourd'hui, sans que rien ne prévienne le joueur. Proposition : un message en fin de
  partie, « Graine d'hier : non comptée ».
- Une série de jours, un historique, un classement en ligne : hors périmètre.
