# 0005 — Les thèmes restent hors du domaine

- **Statut** : Acceptée
- **Date** : 2026-10-18

## Contexte

Le jeu a au moins deux thèmes, et d'autres viendront peut-être. La tentation classique est de
mettre une couleur dans l'énumération des pièces.

## Décision

`PieceType` ne porte **aucune information visuelle**. Un thème est une association
`PieceType → CellStyle` (fond, bordure, motif central) définie dans `feature:game`, et un jeu
de jetons d'interface (`BloxPalette`) défini dans `core:designsystem`.

## Conséquences

- Ajouter un thème = ajouter une palette et une table de styles, sans toucher au moteur ni aux
  tests du domaine.
- Le thème Original peut différencier les pièces par leur motif, ce qu'une simple couleur
  dans le domaine n'aurait pas permis.
