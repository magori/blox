# Piece

Une case de pièce est une bulle brillante : reflet en haut, couleur de la pièce en bas, coins arrondis, sans cadre.

- Remplissage : dégradé vertical de `piece-<x>-hi` (0 %) à `piece-<x>` (55 %).
- Coins `radius-cell` (6 px) à 16 px et plus, 2 px en dessous ; 1 px de marge autour de chaque case, sans joint coloré.
- Relief : ombre `cell-gloss` à 16 px et plus, aucune en dessous. De jour, son liseré détache les pastels du plateau blanc.
- Case vide : `empty-cell`, mêmes coins et même marge, sans ombre, sur le fond `board`.
- Le domaine ne connaît que `PieceType` : l'association pièce → jetons vit dans `feature:game` (`CellStyles`), comme pour les autres thèmes (ADR 0005).
- Les sept couleurs diffèrent aussi par leur clarté, la nuit surtout ; T (lilas) et J (bleu) restent proches le jour, la forme les distingue.
