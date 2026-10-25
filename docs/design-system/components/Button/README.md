# Button

Les boutons d'écran, en matière bulle : reflet en haut, épaisseur en bas, et ils s'enfoncent de 2 px quand on appuie.

- **Principal XL** (Jouer) : pilule de 72 px (64 en téléphone couché, 84 sur tablette), pleine largeur de la colonne, `accent` et `button-gloss`. « Jouer » en `play-label`, rappel des réglages dessous en `small` 600 (« Classique · Niveau 05 · Korobeïniki », coupé par une ellipse, lu en entier par TalkBack), disque de 52 px en `accent-ink` avec le triangle en `accent` à droite. Halo `accent-ring` : 3 impulsions après 3 s sans toucher, puis une toutes les 10 s ; aucun en Effets réduits.
- **Principal** (Reprendre, Rejouer) : pilule de 56 px, `accent`, `button-label-strong`, pictogramme de 24. Un seul par écran.
- **Secondaire** (Menu, Changer, tuiles) : pilule de 56 px (tuiles en `radius-md`), fond `button`, cadre de 2 px `button-outline`, `button-ink` 15/700, `button-gloss`.
- **Icône** (Paramètres, Écouter, Pause) : disque de 48 px (56 sur tablette), comme Secondaire.
- **Pressé** : `accent-pressed` ou `button-pressed`, `button-gloss-pressed`, 2 px plus bas ; retour en `motion-press`.
- **Désactivé** : libellé `label`, cadre `panel-border`, sans reflet.
- Focus clavier : contour de 3 px `ink` à 3 px du bord.
