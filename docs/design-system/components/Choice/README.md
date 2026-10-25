# Choice

Les choix de l'accueil (mode, thème) : un groupe de segments dont un seul est retenu, encadré comme un seul bouton.

- **Groupe** : boîte de 48 px en pilule, cadre de 2 px `button-outline` autour du groupe (3,7:1 de jour) ; c'est lui qui rend les choix repérables, pas le cadre de chaque segment.
- **Segment** : 44 px de haut dans le groupe (cible de 48), fond transparent, `ink` 15/500.
- **Retenu** : `accent`, coche de 16 px et libellé en `accent-ink`.
- **Indisponible** (Moderne) : libellé `label` et étiquette `BIENTÔT` (11/700, capitales, 7:1) ; TalkBack lit « Moderne, bientôt, indisponible ».
- Si le groupe ne tient pas sur la ligne (police agrandie), son libellé passe au-dessus et il prend toute la largeur.
- **Niveau de départ** : ce n'est pas un groupe mais un curseur, jauge de 10 barres (8 px de large, 8 à 26 px de haut, `accent` jusqu'au niveau, `panel-border` au-delà) qu'on touche ou glisse, avec − et + de 48 px ; − désactivé à 0, + à 9.
