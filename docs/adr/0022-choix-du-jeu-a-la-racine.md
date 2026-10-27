# 0022 — Choix du jeu : une feature à la racine de la pile

- **Statut** : Acceptée ; amende [0011](0011-navigation-3.md) ; amendée par [0025](0025-le-serrurier.md), [0026](0026-jeux-musicaux-variantes-du-classique.md) et [0027](0027-le-jardin-musical.md)
- **Date** : 2026-10-25

## Contexte

L'application s'ouvre désormais sur le choix du jeu, des cartouches (maquette « Choix des jeux
4 »). L'accueil devient celui du mode Classique. D'autres jeux pourront venir, chacun dans sa
feature, et `feature:home` ne doit pas connaître l'écran qui le précède. Le développeur a décidé
de n'afficher que la cartouche Classique, sans cartouche « endormie » (2026-10-25).

## Décision

- Nouveau module **`feature:cartridges`**, sans ViewModel : une liste statique de cartouches (une
  seule entrée, Classique). Ajouter un jeu, c'est ajouter une entrée.
- **`CartridgesRoute` est la racine de la pile** (`rememberNavBackStack(CartridgesRoute)`) ;
  `cartridgesEntry(onOpen)` ouvre l'accueil, `homeEntry(onChangeGame)` y revient.
- `changeGame` est une **fonction pure de `app`** : elle dépile jusqu'aux cartouches, ou les met
  à la racine quand la pile restaurée (sauvegardée avant la mise à jour) n'en a pas.
- Le mode Moderne reste un choix de l'accueil (rangée « Mode »), pas une cartouche.

## Conséquences

- Un toucher de plus à chaque lancement.
- Le parcours Macrobenchmark (`BloxJourney`) et le profil de référence sont à refaire (P5.72).
- Le retour système sur les cartouches quitte l'application.

## Amendement (ADR 0026 et 0027, 2026-10-26)

Cinq cartouches, sur deux colonnes : Classique, Serrurier, Jardin musical, Au tempo et Graine du
jour. La cartouche Graine du jour porte l'autocollant « NOUVELLE » jusqu'au premier essai du jour,
d'après un état que `app` lui passe. La Musique vivante n'a pas de cartouche : c'est un choix de
musique de l'accueil Classique, comme le mode Moderne en est un réglage.

## Amendement (P9.7, 2026-10-27)

La grille de V36 remplace la console de V26 (décision du développeur). Deux colonnes d'au plus
171 dp ; une dernière cartouche seule est centrée dessous, si bien que trois puis cinq cartouches
tiennent dans la même grille ; une seule colonne quand un mot d'un nom ne tient plus dans sa
colonne à la plus petite taille. Trois cartouches pour l'instant (Classique, Serrurier, Graine du
jour) : le Jardin et Au tempo s'ajouteront avec P10.13 et P11.12, sans cartouche endormie. Le
choix du jeu est dessiné en Couleurs v2 quel que soit le thème choisi (`ColorfulTheme`, barres
système comprises), l'Original étant figé. La feature reste sans ViewModel : l'autocollant
« NOUVELLE » vient d'un flux `Flow<Set<Cartridge>>` passé à `cartridgesEntry`, que `app` tire des
résultats de la Graine du jour et de `today(clock)` (`NewCartridges`, résultats gardés chauds pour que l'autocollant ne clignote pas au retour), la date relue à chaque
retour de l'écran ou de l'application au premier plan (pas de bascule en direct à minuit, comme
l'écran d'entrée). La feature ne connaît ni les résultats ni la date.
