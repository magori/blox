# 0023 — Le rendu du plateau dans `core:board`

- **Statut** : Acceptée ; amende [0005](0005-themes-hors-du-domaine.md)
- **Date** : 2026-10-25

## Contexte

L'accueil v2 montre un plateau d'aperçu qui dessine les cases comme le jeu. Le rendu du plateau
(`BoardCanvas`, `CellPainter`, `CellStyles`…) vit dans `feature:game`, et une feature ne dépend
jamais d'une autre. `core:designsystem` ne connaît pas le domaine, donc pas `PieceType`.

## Décision

- Nouvelle bibliothèque Compose **`core:board`**, qui dépend de `core:game` et de
  `core:designsystem`. Elle porte l'association `PieceType` → style de case, la géométrie, les
  peintres et l'aperçu de la pièce suivante.
- `feature:game`, puis `feature:home`, l'utilisent.
- Le déplacement (P5.73) ne change aucun pixel ; les cases « bulles » viennent ensuite (P5.74).

## Conséquences

- La règle de l'ADR 0005 « les styles de pièces vivent dans `feature:game` » devient « dans
  `core:board` » ; le domaine ne connaît toujours aucune couleur.
- `core:board` applique `blox.design.tokens` pour les jetons `piece-*` (ADR 0019).
