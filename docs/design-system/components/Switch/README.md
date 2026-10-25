# Switch

Les réglages des Paramètres : toute la rangée est l'interrupteur, avec un pictogramme, un libellé, une sous-ligne qui dit ce que fait le réglage, et la piste à droite.

- **Rangée** : `setting-row` (56 px, 64 sur tablette), emplacement d'icône de 40 px (pictogramme de 24 en `label`), libellé Manrope 15/700 `ink` (`choice` en gras), sous-ligne `small` `label` sur deux lignes au plus.
- **Piste** : `switch-track` (52 × 32), pilule, cadre de 2 px `button-outline` dans les deux états (le 3:1 est tenu par le cadre, pas par l'accent).
- **Éteint** : piste `button`, pouce de 20 px `label` à gauche.
- **Allumé** : piste `accent` avec `button-gloss`, pouce `switch-thumb` (24) en `accent-ink` à droite, coche de 14 px `accent` dans le pouce : l'état se lit par la position, la taille et la coche, pas par la couleur seule.
- **Pressé** : la rangée se remplit de `button-pressed` (coins `radius-md`), le pouce s'allonge à 28 ; retour en `motion-press`. Effets réduits : couleur seule.
- **Désactivé** (Effets réduits imposé par Android, appareil sans vibreur) : libellé `label`, piste `panel-inset`, cadre `panel-border`, pouce `label` ; TalkBack ajoute « indisponible » et la sous-ligne dit pourquoi.
- Rôle `switch` avec `aria-checked`. Effets sonores allumés par défaut ; Vibrations, Vibration des touches, Effets réduits et Gaucher éteints. Allumer Effets sonores joue le son de rotation une fois ; allumer une vibration vibre une fois.
