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

> Traçabilité : seule la graine d'une date est implémentée (P9.2) ; le reste suit en phase P9 de
> [SUIVI.md](../SUIVI.md), après P1.20. Chaque règle recevra sa ligne **Tests :** `ClasseDeTest › nom du test` et, si
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
- La partie du jour est la variante `Daily(date, id)` de `GameRoute` (`feature:game`, ADR 0026) :
  `GameRoute.daily(date, seed, id)` ; `newDailyGameRoute(date)` (`app`) reçoit la date affichée par l'écran d'entrée à
  Jouer, et en tire la graine. La date est dans la route, sauvée en texte ISO ; l'`id` donne une clé
  neuve à chaque partie et à Rejouer (ADR 0011). Seul `GameVariants.setupOf` (`feature:game`)
  distingue les variantes : moteurs, résultats et musique ; Rejouer est porté par la variante
  (`GameRoute.replayed`).
  **Tests :** `GameRouteTest › the date of a daily route is saved as its ISO text`,
  `GameRouteTest › replaying a classic game draws a new seed, at the same level`,
  `GameRouteTest › replaying a daily game keeps its date and its seed, under a new id`,
  `DailyGameRouteTest › two daily games of the same day are two games, with two keys`,
  `DailyGameRouteTest › the daily game played is the game of the date shown, with its seed, even once the phone passed midnight`,
  `GameRouteSavingTest › a daily route comes back from the saved state with its date, its seed and its id`,
  `GameRouteSavingTest › a classic route comes back from the saved state as it was`.
- **Niveau de départ imposé : 5**, quel que soit le niveau choisi à l'accueil (décision du
  développeur) ; la route d'une partie du jour refuse un autre niveau.
  **Tests :** `GameRouteTest › a daily game starts at level 5 whatever the level chosen`.
- **Musique imposée : Vivante** ([musique-vivante.md](musique-vivante.md)), composée sur la graine
  du jour, quelle que soit la musique choisie à l'accueil. Elle prime aussi sur la « musique en
  jeu » de « Mes musiques », dont la priorité sur le choix de l'accueil est décrite dans
  [musique.md](musique.md) : la Graine du jour joue toujours la Vivante. Le volume de la musique et le réglage
  des effets sonores restent ceux du joueur. La musique n'est **pas** un contrat : deux versions
  de l'application peuvent jouer deux musiques différentes pour une même date.
  **Tests :** `GameViewModelTest › a daily game plays the live music whatever the music chosen`.
- Commandes, vibrations, Effets réduits : ceux du joueur. Le thème est imposé (Couleurs, voir
  **Écran**).

## Graine

- **Date** : la date **locale du téléphone** (fuseau du téléphone) affichée par l'écran d'entrée quand on appuie sur Jouer, écrite
  `AAAAMMJJ` : `DailySeed.of(LocalDate)` rend la graine `20261026` pour le 26 octobre 2026
  (`20270105` pour le 5 janvier 2027 : mois et jour toujours sur deux chiffres), du même type que la
  graine d'une partie (`Long`) ; un jour plus récent donne une graine plus grande.
  L'horloge est injectée (`today(clock)`, seule source du jour dans `app`), pour que les tests fixent la date ;
  par défaut une horloge dans le fuseau du système, lue à chaque affichage de l'écran d'entrée
  (`Clock.systemDefaultZone()`) : jamais l'horloge UTC de `AppContainer`, qui donnerait le mauvais
  jour autour de minuit, ni une horloge gardée pour la vie du processus, qui manquerait un
  changement de fuseau.
  **Tests :** `DailySeedTest › the seed of a day is its date written year month day`,
  `DailyGameRouteTest › the date is the local date of the phone`,
  `DailySeedTest › two days give two seeds`,
  `DailySeedTest › a later day gives a greater seed, from one year to the next`.
- **Une partie commencée un jour compte pour ce jour-là** : la date est dans la clé de la partie
  (`GameRoute`, variante `Daily`), pas relue à la fin. Rejouer garde la graine de la partie
  terminée, même après minuit.
  **Tests :** `ReplayTest › replaying a daily game keeps its seed, even after midnight`.
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
  tirage casse ce test : c'est voulu, une telle modification demande un nouvel ADR. Les 20 pièces
  de la graine `20261026` : `Z T I Z O Z J L S Z T I J L Z O L Z I O`.
  **Tests :** `ClassicRandomizerTest › the first 20 pieces of seed 20261026 are fixed`.
- `StableRandom` est une fonction pure de sa graine : **SplitMix64** (Steele, Lea et Flood, « Fast
  Splittable Pseudorandom Number Generators », OOPSLA 2014), constantes standard ; algorithme choisi
  par le développeur le 2026-10-27. Ses premières valeurs pour la graine 0 sont les valeurs publiées
  (`0xE220A8397B1DCDAF`…). `nextBits(n)` prend les n bits de poids fort du nombre suivant (0 à 32) ;
  `nextInt(n)` tire **sans biais, par tirage avec rejet** (les 31 bits de poids fort, leur reste
  par n, un nouveau nombre quand le reste tombe dans la dernière série incomplète) ;
  `nextInt(from, until)` vaut `from` plus un tirage dans la largeur. Le contrat couvre `nextLong`,
  `nextBits`, `nextInt(until)`, `nextInt(from, until)`, et ce qui passe par eux (`List.random`,
  `shuffle`) ; les autres tirages de `Random` viennent de la bibliothèque standard et ne sont pas
  garantis.
  **Tests :** `StableRandomTest › the first numbers of seed 0 are the published SplitMix64 values`,
  `StableRandomTest › the same seed gives the same numbers`,
  `StableRandomTest › bits are taken from the high end of the next number`,
  `StableRandomTest › bits are drawn 0 to 32 at a time`,
  `StableRandomTest › a number drawn in a range is the remainder of the high 31 bits of the next number`,
  `StableRandomTest › a number drawn in a range takes every value of the range`,
  `StableRandomTest › a number drawn in a large range is not biased toward its low values`,
  `StableRandomTest › a draw that would bias the range is replaced by the next number`,
  `StableRandomTest › a number drawn in an empty range is refused`,
  `StableRandomTest › a number drawn between two bounds is the lower bound plus a draw in the width`,
  `StableRandomTest › a number drawn between two bounds is refused when the range is empty or wider than the largest int`.
- Une partie du jour tire ses pièces par `ClassicRandomizer.stable(seed)` (`StableClassicEngines`,
  passé par la fabrique du jeu).
  **Tests :** `GameViewModelTest › a daily game draws the pieces of its seed`.
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
- Un essai est compté **à son début** (décision du développeur, 2026-10-27) : `startTry(date)`, dès
  Jouer, ajoute un essai ; tant qu'il n'est pas fini, le premier essai vaut **0**, puis son score
  (`endTry(date, points)`). Quitter (Pause puis Menu, Changer de jeu) garde les points du moment.
  Quitter ou tuer l'application pour « refaire » son premier essai ne permet donc pas de connaître
  les pièces sans que l'essai compte. Après la mort du processus, le ViewModel rejoue la partie de
  la route depuis sa graine (ADR 0014) : c'est **le même essai**, qui n'est pas compté une seconde
  fois (`startTry` n'est pas rappelé). Limite connue : la partie restaurée recommence au début, en
  pause, avec les mêmes pièces ; le premier essai se rejoue donc en connaissant la suite.
- Le premier essai ne se fige qu'une fois (`firstTryOver`) : à sa fin, ou dès qu'un deuxième essai
  commence, puisqu'il ne peut plus être restauré. Un premier essai tué et jamais restauré vaut 0 pour
  toujours ; une partie restaurée qui finit une seconde fois peut encore améliorer le meilleur.
- Un essai se range sous **la date de sa partie**, celle de la graine de la route, pas la date du
  téléphone à sa fin (Rejouer après minuit garde la date d'hier) :
  - même date que le dépôt : le début ajoute un essai, la fin donne le premier essai (une fois) et
    met à jour le meilleur ;
  - date plus récente (ou dépôt vide) : le début repart à cette date, 1 essai, premier essai et
    meilleur à 0 ; une fin sans début connu (début non écrit) y compte un essai entier, premier
    essai = meilleur = ce score ; à date égale, une fin sans début n'ajoute pas d'essai ;
  - date plus ancienne (une partie d'hier finie après une partie d'aujourd'hui) : ignoré, au début
    comme à la fin.

  La date gardée peut être plus récente qu'aujourd'hui (horloge reculée, voyage vers l'ouest) :
  les essais du jour réel sont alors ignorés jusqu'à ce que la date la rattrape.
- `endTry` rend, comme les records, un `RecordResult` calculé dans la même transaction : le meilleur
  du jour d'avant (0 pour une date nouvelle) et s'il est battu ; un essai ignoré ne bat rien. Des
  points négatifs sont refusés avant toute écriture. Deux débuts simultanés comptent deux essais.
- La date est gardée en `epochDay` ; à la lecture, des valeurs manquantes ou négatives donnent des
  valeurs sûres (au moins 1 essai quand une date est là, scores à 0 au moins). Un fichier illisible
  donne « aucun résultat », et l'essai suivant est le premier. Un jour hors des bornes de
  `LocalDate` n'est pas une date : le fichier se lit comme un fichier illisible, « aucun
  résultat », et le prochain début ou la prochaine fin repart à sa date, comme sur un dépôt vide
  (les essais gardés pour cette non-date sont perdus) ; la lecture n'échoue pas. Les bornes
  elles-mêmes (`LocalDate.MIN`, `LocalDate.MAX`) se lisent normalement. Comme chaque dépôt sur
  fichier, il passe `DataStoreFileContract` (P9.10).
  **Tests :** `DataStoreDailyChallengeRepositoryTest › a date out of the range of LocalDate in the file gives no result of the day, and the next try is the first`,
  `DataStoreDailyChallengeRepositoryTest › a date at a bound of the range of LocalDate in the file is read as it is`,
  `DataStoreDailyChallengeRepositoryTest › a try that ends on a file whose date is out of range counts as a whole first try`.
- Le dépôt ne garde pas d'historique ni de série de jours (hors périmètre).
- Le dépôt a son contrat de test (`DailyChallengeRepositoryContract`, dans les tests de `core:data`,
  comme les autres contrats) et son faux (`FakeDailyChallengeRepository`, en testFixtures),
  partagé par les tests des features.
  **Tests :** `DailyChallengeRepositoryContract › without a try there is no result of the day`,
  `DailyChallengeRepositoryContract › a try is counted as soon as it starts`,
  `DailyChallengeRepositoryContract › the first try is worth 0 until it ends`,
  `DailyChallengeRepositoryContract › the first try is the first and the best`,
  `DailyChallengeRepositoryContract › a better try replaces the best, never the first`,
  `DailyChallengeRepositoryContract › every try is counted`,
  `DailyChallengeRepositoryContract › a try of a newer date starts that date over`,
  `DailyChallengeRepositoryContract › a try of an older date is ignored`,
  `DailyChallengeRepositoryContract › a try that ends on a newer date without its start counts as a whole first try`,
  `DailyChallengeRepositoryContract › a first try that never ends stays worth 0`,
  `DailyChallengeRepositoryContract › the first try ends only once`,
  `DailyChallengeRepositoryContract › negative points are refused and leave the results`,
  `DailyChallengeRepositoryContract › negative points are refused even when nothing can be written`,
  `DailyChallengeRepositoryContract › the results flow emits each change, and nothing for a try that is ignored`,
  `DailyChallengeRepositoryContract › the end of a first try beats the best of the day`,
  `DailyChallengeRepositoryContract › an end below the best does not beat it`,
  `DailyChallengeRepositoryContract › an ignored end beats nothing`,
  `DailyChallengeRepositoryContract › concurrent starts are all counted`,
  `DailyChallengeRepositoryContract › a start that cannot be written fails with an IOException and keeps the results`,
  `DailyChallengeRepositoryContract › an end that cannot be written fails with an IOException and keeps the results`,
  `DataStoreDailyChallengeRepositoryTest › the results survive a new repository on the same file`,
  `DataStoreFileContract › an unreadable file reads as an empty one, and what is written next is kept`,
  `DataStoreFileContract › a file that cannot be read reads as an empty one`,
  `DataStoreDailyChallengeRepositoryTest › a file with missing or negative values reads as safe values`,
  `DataStoreFileContract › a failure other than an IOException while the file is read is not absorbed`.
- Fichier exclu de la sauvegarde (décision du développeur, 2026-10-27), transfert d'appareil
  compris : restauré sur un autre appareil, un premier essai n'aurait plus de sens. Un joueur qui
  change de téléphone en cours de journée perd donc ses essais du jour.
  **Tests :** `BackupRulesTest › the file of the daily challenge never leaves the device`.

**Côté partie** (P9.4). Le jeu passe par un port, `GameResults` (`feature:game`, interne : `best`,
`start`, `end`, `leave`), et deux adaptateurs : le Classique (`best` = record, `end` = `submit`, rien
au début ni en quittant) et le jour (`DailyGameResults`, lié à la date de la route : `startTry`,
`endTry`, meilleur de cette date, 0 pour une autre).

- L'essai commence avec la partie (`start`, dans une portée d'application, pour qu'un départ rapide
  ne l'annule pas) ; une partie restaurée après la mort du processus ne le recommence pas (marque
  dans le `SavedStateHandle`).
- La fin de partie, et le départ avant la fin (Pause puis Menu, Changer de jeu : `leave`, lancé
  depuis `onCleared`), attendent l'écriture du début avant d'écrire la fin ; une partie finie puis
  quittée ne finit son essai qu'une fois. Les écritures du jour passent par un même verrou équitable,
  demandé sur le fil principal dans l'ordre des parties : quitter un essai puis en lancer un autre
  garde les points du premier. Cas limite : si le début de A s'écrit encore quand on quitte A et que B
  démarre, la fin de A passe après le début de B.
- Une partie du jour n'entre jamais dans les records ; une partie Classique quittée avant sa fin
  n'envoie rien aux records.
- Si les résultats du jour ne peuvent être écrits (`IOException`), la partie continue et sa fin
  s'affiche sans nouveau meilleur ; toute autre erreur remonte.
- En jeu, le meilleur du jour remplace le record (0 pour les résultats d'une autre date).

**Tests :** `GameViewModelTest › a daily try is counted as soon as the game starts`,
`GameViewModelTest › a daily game restored after the process died is the same try`,
`GameViewModelTest › a daily game never enters the Classic records`,
`GameViewModelTest › a daily game over ends its try only after its start`,
`GameViewModelTest › a daily game left before its end counts as a try`,
`GameViewModelTest › a daily game left while its try is still being started ends it after its start`,
`GameViewModelTest › a try left then a new try started keeps the points of the first`,
`GameViewModelTest › a daily game over then left ends its try once`,
`GameViewModelTest › a daily game shows the best of its date in place of the record, 0 for the results of another date`,
`GameViewModelTest › a classic game left before its end submits nothing`,
`GameViewModelTest › when the results of the day cannot be written the game goes on, and its game over shows without a new best`,
`GameViewModelTest › when the results of the day cannot be written a game left before its end fails nothing`,
`GameViewModelTest › a programming error while a daily try starts is not swallowed`,
`GameEntryRestorationTest › a daily game entry restored after the process died is the same try`.

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
système suivent cette variante (`systemBarVariant`, P7.18), qui connaît la variante
`Daily` de `GameRoute` : c'est une différence par variante admise par l'ADR 0026 (thème imposé et
barres système). L'entrée du jeu dessine une partie du jour sous `ColorfulTheme`
(`core:designsystem`, partagé avec le Serrurier), qui garde les Effets réduits du joueur.
**Tests :** `GameRouteTest › a daily game is drawn in the colorful theme, a classic game in the theme chosen`,
`GameThemeTest › a daily game is drawn in the colorful variant whatever the theme chosen, with the effects of the player`,
`GameThemeTest › a classic game is drawn in the theme chosen`,
`ColorfulThemeTest › the colorful theme draws in the colorful variant of the system mode, keeping the effects around it`,
`SystemBarsTest › the bars of a daily game follow its colorful variant`.

**Écran d'entrée** (vue **V38**, à dessiner ; `feature:daily`, clé `DailyRoute`, amendement de
l'ADR 0002), sur le modèle de l'accueil v2 (V27), en Couleurs seulement, barres système comprises :

- barre : Changer de jeu (retour au choix du jeu), puce « MEILLEUR DU JOUR » (le meilleur du jour
  sur 6 chiffres, ou « — » ; TalkBack lit la vraie valeur, ou « aucun essai »), Paramètres (la
  page Paramètres de l'accueil). La puce et le bouton Jouer sont ceux de l'accueil, partagés dans
  `core:designsystem` (`ScoreChip`, `PlayButton`) ;
- titre « Graine du 26 octobre » : la date locale du téléphone en lettres, « 1er » le premier du
  mois ;
- encart « LES 7 PREMIÈRES » : les 7 premières pièces de `ClassicRandomizer.stable(DailySeed.of(date))`,
  la suite même de la partie, numérotées de 1 à 7, non touchables, lues « Pièce 1 : Z » ;
- trois tuiles : **Essais**, **Premier essai**, **Meilleur** : les résultats du dépôt si leur date
  est aujourd'hui, sinon « — » ; un essai commencé mais pas fini laisse « — » au premier essai, et
  au meilleur tant qu'il vaut 0 ; scores sur 6 chiffres, lus à leur vraie valeur ;
- **Jouer** XL, avec le rappel « Niveau 05 · Vivante », lu « Jouer, niveau 5, Vivante » (le niveau imposé, donné
  par `app`), ouvre une partie du jour neuve **à la date affichée** : les pièces vues sont celles
  jouées, même si minuit est passé entre-temps. Le rappel est court (le titre dit déjà « Graine du … ») pour tenir en entier sur 360 dp (décision du développeur, 2026-10-27) ;
- « Copier mon score », actif dès le premier essai du jour commencé ; désactivé avant, avec la
  sous-ligne « Joue une partie pour copier ton score ». Le texte copié vient en P9.6 ;
- en bas : « Nouvelle graine à minuit ».
- Jouer, Changer de jeu et Paramètres ne comptent qu'écran actif (`dropUnlessResumed`, comme
  l'accueil) : un double appui ne lance pas deux parties, donc pas deux essais.
- La date est relue à chaque affichage de l'écran et à chaque retour au premier plan, sans
  minuteur : ouvert à 23 h 59, l'écran passe à la graine du lendemain à son prochain affichage.
  C'est la version retenue (décision du développeur, 2026-10-27) : pas de bascule en direct à minuit.

**Tests :** `DailyViewModelTest › without a try today, no result is shown and the score cannot be copied`,
`DailyViewModelTest › the results of today show the tries, the first try and the best, and the score can be copied`,
`DailyViewModelTest › the results of another day are not those of today`,
`DailyViewModelTest › a try started but not ended shows a dash for the first try and the best`,
`DailyViewModelTest › a best of 0 is shown once the first try is over`,
`DailyViewModelTest › the seven first are the first pieces of the daily game, drawn from the seed of the day`,
`DailyViewModelTest › the date is read again each time the screen shows, with the pieces and results of that day`,
`DailyViewModelTest › the reminder under Jouer gives the start level of the daily game, as app gives it`,
`DailyViewModelTest › the factory builds the view model on the results, the date and the seeds given by app`,
`DayInLettersTest › the day is written in letters, the day of the month then the month`,
`DayInLettersTest › the first of the month is written 1er, as in French`,
`DailyGameRouteTest › the daily game played is the game of the date shown, with its seed, even once the phone passed midnight`,
`SystemBarsTest › the bars of the entry of the daily seed follow its colorful variant`,
`DailyScreenTest › the title gives the day in letters`,
`DailyScreenTest › TalkBack reads each of the seven first pieces with its rank, and none can be touched`,
`DailyScreenTest › before the first try, the tiles and the best of the day show a dash, and copying is disabled`,
`DailyScreenTest › after a try, the tiles read the results and Copier mon score hands the copy to app`,
`DailyScreenTest › Jouer, read with the level received and its reminder in words, hands the date shown to app`,
`DailyScreenTest › on 360 dp, in a normal font, the reminder under Jouer holds whole on one line, without an ellipsis`,
`DailyScreenTest › Jouer, Changer de jeu and Paramètres count only while the entry is resumed`,
`DailyScreenTest › the bar changes the game and opens the settings through app`,
`ScoreChipTest › a score chip is 48 dp high at least, as the discs beside it, even in a font twice as large`,
`PlayButtonTest › a reminder too long for Jouer is cut by an ellipsis on one line, and TalkBack still reads it whole`,
`BloxNavDisplayTest › the entry of the daily seed shows the day and its first pieces`,
`BloxNavDisplayTest › Jouer on the entry of the daily seed opens the daily game, at level 5`,
`BloxNavDisplayTest › changing the game from the entry of the daily seed goes back to the cartridges` ;
captures `DailyBeforeAnyTryDay`, `DailyBeforeAnyTryNight`, `DailyAfterTriesDay`, `DailyAfterTriesNight`,
`DailyLargeFontDay`.

**En jeu** (vue **V39**, à dessiner) : l'écran de jeu v2 (V28), titre « GRAINE DU JOUR », le
meilleur du jour à la place du record, sous le libellé « MEILLEUR ». Une fois battu, l'autocollant
« NOUVEAU MEILLEUR » et le score prennent sa place, comme « NOUVEAU RECORD » au Classique ; lu
« Nouveau meilleur : 12 380 ». « MEILLEUR DU JOUR » reste l'étiquette de la valeur (V38) et
l'annonce de fin de partie, pas l'autocollant en jeu. Un meilleur de 0 (premier essai du jour)
n'est jamais battu en cours de partie, comme le record (P5.56). La barre tient sur une rangée
avec ces libellés, police agrandie comprise. Pause comme V29. Ce qui change d'une variante à
l'autre (titre, libellé du meilleur, ses annonces, Copier proposé) vient de `GameLabels`, donné
par `GameVariants.setupOf` (ADR 0026) : l'écran ne connaît pas la variante.
**Tests :** `GameVariantsTest › a daily game is titled Graine du jour, shows the best of the day and offers to copy the score`,
`GameVariantsTest › a classic game is titled Mode classique, shows the record and offers no copy`,
`GameViewModelTest › the screen gets the labels of the variant of its game`,
`GameScreenTest › a daily game is titled GRAINE DU JOUR and shows the best of the day in place of the record`,
`GameScreenTest › once the best of the day is beaten, the top bar shows NOUVEAU MEILLEUR and the score, read as such`,
`GameTopBarTest › on a phone of 360 dp the bar stays on one row once the record is beaten, font enlarged`
et les autres cas de largeur de `GameTopBarTest`, joués aussi avec les libellés du jour ;
captures `GameScreenDaily`, `GameScreenDailyNight`.

**Fin de partie** (V39) : comme V30 ; le badge « MEILLEUR DU JOUR » si le meilleur du jour est
battu, au premier essai aussi (meilleur d'avant : 0), aucun badge sinon ; puis **Rejouer** (même
graine, `GameRoute.replayed`), **Copier** (lu « Copier mon score ») et Menu. Copier n'est proposé
qu'à la partie du jour ; il remonte la partie terminée à l'entrée de navigation
(`gameEntry(onCopy)`), que `app` branche en P9.6 : d'ici là, il ne fait rien, et la partie du jour
n'est pas encore accessible au joueur (sa cartouche vient en P9.7, après P9.6).
**Tests :** `GameViewModelTest › the first try of the day beats the best of the day at its game over`,
`GameViewModelTest › a daily game over below the best of the day shows no new best`,
`GameScreenTest › the game over of a daily game announces MEILLEUR DU JOUR; Rejouer and Copier, read Copier mon score, act`,
`GameScreenTest › the game over of a classic game offers no Copier`,
`GameRouteTest › replaying a daily game keeps its date and its seed, under a new id` ;
captures `GameOverDailyNewBest`, `GameOverDailyNewBestNight`.

**Cartouche** « Graine du jour » sur le choix du jeu (V36) : elle porte l'autocollant
« NOUVELLE » **jusqu'au premier essai du jour** ; elle ouvre l'écran d'entrée.

## Points ouverts

- L'essai compté à son début (ci-dessus) et la sauvegarde du fichier.
- Rejouer après minuit : une partie rejouée avec la graine d'hier est ignorée si le dépôt est déjà
  passé à aujourd'hui, sans que rien ne prévienne le joueur. Proposition : un message en fin de
  partie, « Graine d'hier : non comptée ».
- Une série de jours, un historique, un classement en ligne : hors périmètre.
