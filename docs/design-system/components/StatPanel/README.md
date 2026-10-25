# StatPanel

Les encarts de la colonne à droite du plateau : la pièce suivante en haut (près de l'apparition), puis score, lignes et niveau.

- Colonne de 104 px sur téléphone (200 sur tablette), encarts espacés de `space-2`.
- Ordre : SUIVANTE, SCORE (avec une barre de record de 4 px : piste `panel-border`, remplissage `accent`, largeur score ÷ record, cachée sans record ; irisée une fois le record battu), LIGNES, NIVEAU.
- Encart : fond `panel`, cadre de 2 px `panel-border`, coins `radius-md`, marges intérieures 4 / 8 / 8 px (8 / 14 / 12 sur tablette).
- Étiquette en `stat-label`, capitales, couleur `label` ; valeur en `stat-value`, couleur `ink`, chiffres à chasse fixe, zéros de tête gardés (`012340`).
- Pièce suivante : cases dessinées comme sur le plateau (voir Piece), sans fond de case vide, centrées sur deux rangées ; masquée (« ? ») pendant la pause.
- NIVEAU s'encadre d'`accent` pendant 1 s au passage de niveau ; en fenêtre étroite (220 px), les encarts passent sur deux colonnes au-dessus du plateau.
