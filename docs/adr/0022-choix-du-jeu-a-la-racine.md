# 0022 — Choix du jeu : une feature à la racine de la pile

- **Statut** : Acceptée ; amende [0011](0011-navigation-3.md) ; amendée par [0025](0025-le-serrurier.md)
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
