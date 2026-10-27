# Choice

Les choix de l'accueil (mode, thème) : un groupe de segments dont un seul est retenu, encadré comme un seul bouton.

- **Groupe** : boîte de 48 px en pilule, cadre de 2 px `button-outline` autour du groupe (3,7:1 de jour) ; c'est lui qui rend les choix repérables, pas le cadre de chaque segment.
- **Segment** : 44 px de haut dans le groupe (cible de 48), fond transparent, `ink` 15/500.
- **Retenu** : `accent`, coche aussi haute que le libellé (24 px au plus) et libellé en `accent-ink`.
- **Indisponible** (Moderne) : libellé `label` et étiquette `BIENTÔT` (11/700, capitales, 7:1) ; TalkBack lit « Moderne, bientôt », puis l'état désactivé.
- Si le groupe ne tient pas sur la ligne (police agrandie), son libellé passe au-dessus et il prend toute la largeur ; si un libellé devait encore rétrécir sous 75 % de sa taille, les segments s'empilent.
- **Niveau de départ** : ce n'est pas un groupe mais un curseur, jauge de 10 barres (8 px de large, 8 à 26 px de haut, `accent` jusqu'au niveau, `panel-border` au-delà) qu'on touche ou glisse, avec − et + de 48 px ; − désactivé à 0, + à 9. Les barres au-delà du niveau sont creuses (le niveau ne se lit pas qu'à la couleur). Dans le code, en attendant l'accueil v2 (P5.75), le niveau et la musique sont des groupes sur plusieurs rangées, coche dans le coin.
- **Segments sans titre** (pistes, outils de l'éditeur) : mêmes segments, sans libellé visible, nommés pour TalkBack ; un pictogramme peut précéder le libellé, remplacé par la coche sur le retenu ; le retenu peut prendre la couleur de sa piste (`melody`, `bass`), son encre à 4,5:1.
