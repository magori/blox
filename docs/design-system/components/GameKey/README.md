# GameKey

Les touches de l'écran de jeu : trois flèches d'un côté, B et A de l'autre (option « Gaucher » pour les inverser), collées en bas, à portée du pouce.

- **Flèches** : carrés `key-arrow` (64 px), coins `radius-md`, fond `button`, cadre de 2 px `button-outline`, reflet `button-gloss`, triangle plein `button-ink` de 24 px.
- **B** : disque `key-rotate` (68 px), mêmes couleurs que les flèches, **pictogramme de rotation anti-horaire seul** (28 px), sans lettre. Il descend de 24 px.
- **A** : disque `key-rotate`, fond `accent`, cadre de 2 px `accent-pressed` (3,2:1 sur la page de jour, que l'accent seul n'atteint pas), pictogramme de rotation horaire seul en `accent-ink`. C'est la seule touche en couleur d'action.
- Noms pour TalkBack : « B, rotation anti-horaire », « A, rotation horaire ».
- **Tenue** : le fond passe en `button-pressed` (`accent-pressed` pour A), le cadre reste, la touche descend de 2 px avec `button-gloss-pressed`. De jour, A tenue ne se distingue de A au repos que par ce décalage (une couleur ne peut pas être à la fois à 1,5:1 de l'accent et à 4,5:1 sous l'encre). À `PieceRotated`, le pictogramme tourne de 90° dans son sens (6 ticks ; rien en Effets réduits).
- **Tablette** : `key-arrow-tablet` (76 px, coins de 22) et `key-rotate-tablet` (84 px), B descendue de 36 px. **Fenêtre étroite** : `key-arrow-compact` (52) et `key-rotate-compact` (56).
- Écart de 8 px entre deux flèches, 16 px entre B et A (demande du développeur), 18 px au moins entre les deux groupes ; ▼ une demi-touche plus bas que ◀ et ▶ ; zones exclues des gestes système. Taille choisie par la largeur : pleine dès 410 dp de large, mi-chemin (58 et 62) dès 380 dp, compacte en dessous (marges de 16 dp).
