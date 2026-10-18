# Maquette

La maquette interactive a été réalisée avec Claude Design :
[Maquette BloX](https://claude.ai/artifact/SUpVjCbfxfphc9hbFAvaj5) (lien privé, accessible au
propriétaire du projet).

Format de référence : téléphone en portrait, 390 × 844 points.

## Vues numérotées

Les numéros de vue servent de référence dans la spécification et le suivi. Ils suivent l'ordre
de la planche, de gauche à droite. Toute renumérotation doit être reportée partout
(voir la définition de terminé dans `CLAUDE.md`).

| Vue | Écran | Planche de la maquette |
|---|---|---|
| V1 | Accueil | « V1 · Accueil » |
| V2 | Jeu, thème Original | « V2 · Jeu · thème Original » |
| V3 | Jeu, thème Couleurs | « V3 · Jeu · thème Couleurs » |
| V4 | Fin de partie | « V4 · Fin de partie » |

Le prototype (mode Play) relie V1 → V2 ou V3 selon le thème choisi, et V4 → V1, V2 ou V3.

## Écrans

### V1 — Accueil
- Titre **BloX**, avec un logo de quatre cellules aux couleurs du thème, et le record.
- **Mode de jeu** : *Classique* (règles 1989, sans réserve ni pièce fantôme) ; *Moderne*
  affiché grisé, « bientôt ».
- **Thème** : *Original* ou *Couleurs*. Le choix recolore immédiatement l'accueil.
- **Niveau de départ** : 0 à 9, sur une ligne.
- **Musique** : *Korobeïniki*, *Menuet*, *Mon fichier*, *Aucune*, avec un bouton
  *Écouter* pour les deux mélodies.
- Bouton **Jouer**.
- Les choix (thème, niveau, musique) sont gardés d'une ouverture à l'autre ; au premier
  lancement : *Original*, niveau 0, *Korobeïniki* (P3.1).
- Valeurs retenues en P3.4, à confirmer sur la maquette (P3.8) : rien n'est affiché tant que
  les réglages ne sont pas lus (pas de thème qui clignote, pas de « Jouer » au mauvais niveau) ;
  choix en boutons radio groupés (TalkBack), *Classique* sélectionné sans action, *Moderne*
  désactivé ; niveaux 0 à 9 sur une ligne en cellules égales d'au moins 48 dp de haut (360 dp) ;
  record affiché comme le score (6 chiffres, plafond 999 999), lu en clair par TalkBack. Le
  logo n'est pas encore dessiné (P3.7). Les niveaux proposés sont ceux du mode
  (`LevelProgression`) ; un niveau enregistré hors de la liste s'affiche comme le plus proche.

### V2 et V3 — Jeu (thèmes Original et Couleurs)
- Même disposition dans les deux thèmes, seuls les styles changent (voir `docs/themes.md`).
- Barre du haut : bouton pause, « MODE CLASSIQUE », record. Valeurs retenues en P2.6, à
  confirmer sur la maquette (P2.16) : bouton pause de 48 dp au moins, en `button`, pictogramme
  dessiné de 24 dp (deux barres d'un quart de la largeur, un quart d'écart, bords au pixel
  entier) ; titre centré, en gras, sur 2 lignes au plus ; record à droite, libellé 11 sp et
  valeur 18 sp comme la colonne latérale, sur 6 chiffres plafonnés à 999 999 (TalkBack lit la
  vraie valeur ; tant que le record n'est pas lu, sa place reste vide, sans « 000000 ») ; 8 dp
  entre les trois éléments.
- Plateau 10 × 20 (cellules de 24 points) et colonne latérale : score, niveau, lignes,
  pièce suivante. Valeurs retenues en P2.4, à confirmer sur la maquette (P2.13) : encarts
  avec cadre de 2 dp, marges de 8 dp et 4 dp, libellé en 11 sp, valeur en 18 sp (une seule
  ligne, réduite si la police du système est agrandie) ; aperçu de la pièce suivante dans un
  cadre de 4 × 2, pièce centrée au pixel près ; coins carrés (Original) ou arrondis de 10 dp
  (Couleurs). Score sur 6 chiffres, niveau sur 2 et lignes sur 3 au moins.
- Commandes : ◀ ▼ ▶ à gauche ; rotation **B** (anti-horaire) et **A** (horaire) à droite,
  décalés comme sur une console portable. Valeurs retenues en P2.5, à confirmer sur la
  maquette (P2.14) : boutons de direction de 52 dp, de rotation de 56 dp, espacés de 8 dp ;
  B plus bas de 24 dp ; flèches dessinées (triangles), A et B en lettres ; A en `accent`, les
  autres en `button` ; un bouton tenu inverse fond et encre. Le tout tient sur 360 dp de large
  avec une marge de 24 dp, sans cible sous 48 dp.
- Pas de bouton de chute instantanée (absent du mode Classique).

### V4 — Fin de partie
- « Partie terminée », badge « Nouveau record » le cas échéant.
- Score en grand, puis lignes, niveau, nombre de « 4 lignes », durée.
- Boutons **Rejouer** et **Menu**.
- Valeurs retenues en P2.7, à confirmer sur la maquette (P2.15) : titre en 22 sp, score dans un
  encart en 40 sp au plus, statistiques « libellé – valeur » (libellé 11 sp, valeur 18 sp), marges
  de 24, 16 et 8 dp ; badge en `accent` sur `accentInk` ; Rejouer plein (`accent`), Menu en
  contour (`panelBorder`, encre `ink`) ; lignes et niveau avec leurs zéros, comme en jeu. L'écran
  défile si la police est agrandie. TalkBack annonce l'écran et lit la durée « 1 minute et
  5 secondes ».

## Pas encore maquetté

- **Pause** : le plateau doit être masqué. Écran retenu en P2.6, à dessiner sur la maquette
  (P2.16) : fond `page` opaque qui remplace tout l'écran de jeu, titre « Pause » en 22 sp,
  boutons **Reprendre** (plein, `accent`) et **Menu** (contour, `panelBorder`), même mise en
  page que V4 (marges de 24 et 16 dp, cibles de 48 dp au moins). TalkBack annonce l'écran.
- **Réglages** (si les options se multiplient : volume, effets sonores, vibrations,
  sensibilité des commandes).

## Points ouverts

- Boutons tactiles ou gestes (glisser pour déplacer, taper pour tourner) : à tester sur
  téléphone. Le maintien de ▼ et la répétition automatique sont plus délicats au doigt.
- Rendu du thème Original pastel sur un écran OLED.
