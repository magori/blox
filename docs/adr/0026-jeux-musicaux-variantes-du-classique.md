# 0026 — Jeux musicaux : variantes du Classique et records

- **Statut** : Acceptée ; amende [0014](0014-tirage-seul-etat-modifiable.md) et [0022](0022-choix-du-jeu-a-la-racine.md)
- **Date** : 2026-10-26

## Contexte

Quatre modes musicaux s'ajoutent à BloX (cadrage des agents `architecte` et `designer-jeu`,
décisions du développeur le 2026-10-26) : la [Musique vivante](../musique-vivante.md), la
[Graine du jour](../graine-du-jour.md), [Au tempo](../regles-au-tempo.md) et le
[Jardin musical](../jardin-musical.md). Les trois premiers sont des parties Classiques dont seule
une chose change : la musique, la graine, ou la cadence de chute et les points. Le Jardin est un
jeu à part (ADR 0027).

Options envisagées pour les trois variantes :

- une énumération `Mode` dans `core:game`, que le moteur consulterait : écartée par l'ADR 0015
  (chaque différence passe par une politique ou une capacité de `Rules`) ;
- une feature par variante : l'écran de jeu, sa boucle, ses commandes et sa fin de partie
  seraient dupliqués trois fois ;
- des variantes de `feature:game`, chacune réglée par sa clé de navigation et sa fabrique.

Côté records, mêler les scores d'une Graine du jour (niveau 5 imposé) ou d'Au tempo (bonus) à
ceux du Classique les rendrait incomparables.

Côté tirage, l'ADR 0014 notait que la suite de `kotlin.random.Random(seed)` n'est pas un contrat
entre versions de Kotlin, et qu'une graine partagée demanderait un générateur maison (P1.20).
La Graine du jour partage justement une graine entre joueurs.

## Décision

- **Musique vivante est un choix de musique**, `MusicChoice.LIVE`, dans la feuille Musique de
  l'accueil Classique ; elle n'a pas de cartouche. Les règles restent `ClassicRules`. La
  traduction « événements → lecteur » sort de `feature:game` dans une stratégie `MusicDirection`
  (`ClassicMusicDirection`, qui reprend `MusicTempo.kt` tel quel, `LiveMusicDirection`, et
  `SteadyMusicDirection`, sans accélération, pour Au tempo, dont le tempo fixe le `Meter`) ; la
  composition des deux ambiances, `LiveMusic.compose(seed)`, va dans `core:music`, sur
  `MusicGenerator`. Le lecteur gagne `Player.switchTo(score)`.
- **Graine du jour est une partie Classique** dont la graine est la date locale `AAAAMMJJ`
  (`DailySeed`, dans `app`, horloge injectée), au niveau 5 et en musique Vivante imposés. Elle a
  **son propre dépôt**, `DailyChallengeRepository` (`core:data`, fichier à part) : essais, premier
  essai, meilleur du jour. Elle n'écrit jamais dans les records du Classique.
- **Au tempo est une variante de `feature:game`** à règles propres (`TempoRules`, ADR 0028), avec
  **son propre fichier de records** (une seconde instance de `DataStoreHighScoreRepository`).
- `GameRoute` porte la variante (`CLASSIC`, `DAILY`, `TEMPO`) ; la fabrique du ViewModel choisit
  les règles, la direction musicale et le dépôt d'après elle. Rejouer garde la variante et, pour
  la Graine du jour, la graine.
- **Pas d'énumération `Mode` dans `core:game`** : le domaine ne sait pas qu'il joue une Graine du
  jour ; Au tempo n'y entre que par une politique de `Rules`.
- **Amendement de l'ADR 0014** : la **suite des pièces d'une graine devient un contrat**. Le
  tirage du Classique est branché sur un générateur maison, `StableRandom` (`core:game`, P1.20),
  par un point d'entrée public, `ClassicRandomizer.stable(seed)` (le constructeur `ClassicRandomizer(random)`
  reste `internal` à `core:game`) ; `StableRandom` étend `kotlin.random.Random`. Un test fige les 20 premières pièces de
  la graine 20261026. La musique, elle, n'est pas un contrat. Le tirage reste le seul état
  modifiable du domaine.
- **Amendement de l'ADR 0022** : le choix du jeu passe à **cinq cartouches**, sur deux colonnes :
  Classique, Serrurier, Jardin musical (ADR 0027), Au tempo et Graine du jour. La cartouche Graine
  du jour porte l'autocollant « NOUVELLE » jusqu'au premier essai du jour ; `app` lui passe cet
  état, `feature:cartridges` reste sans ViewModel.
- Les écrans propres à ces modes (entrée et partie de la Graine du jour, partie Au tempo) sont
  dessinés en **Couleurs v2 seulement**, jour et nuit, comme le Serrurier ; le thème Original
  n'évolue plus, il reste jouable pour le Classique.

## Conséquences

- `feature:game` porte trois variantes : chaque différence doit passer par la fabrique (règles,
  direction musicale, dépôt), jamais par un `when (variant)` dans l'écran ou le ViewModel au-delà de
  la fabrique et des encarts propres à Au tempo. Autre différence admise : **le thème imposé et ses
  barres système**. Le Classique suit le thème choisi, alors que les parties Graine du jour et Au
  tempo sont toujours en Couleurs v2 ; `systemBarVariant`, dans `app`, devra donc connaître les
  variantes de `GameRoute`.
- Un dépôt et un fichier de records de plus ; tous deux ont leur contrat et leur faux en
  testFixtures, et doivent être déclarés (ou exclus) dans les règles de sauvegarde.
- Changer `StableRandom` ou `ClassicRandomizer` casse le test qui fige la graine 20261026 : c'est le
  but. Une telle modification demande un nouvel ADR.
- La Graine du jour dépend de P1.20, et de la Musique vivante (P8).
- Le parcours Macrobenchmark et le profil de référence sont à refaire quand les cartouches
  changent (cinq cartouches, deux colonnes).
- La cartouche « NOUVELLE » demande à `app` de lire le dépôt du jour pour le choix du jeu.
