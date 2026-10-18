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
| V5 | Jeu, thème Brume clair | « V5 · Jeu · thème Brume clair » |
| V6 | Jeu, thème Brume sombre | « V6 · Jeu · thème Brume sombre » |
| V7 | Mes musiques, thème Original | « V7 · Mes musiques · thème Original » |
| V8 | Éditeur de musique, thème Original | « V8 · Éditeur de musique · thème Original » |
| V9 | Enregistrer la musique, thème Original | « V9 · Enregistrer la musique · thème Original » |
| V10 | Mes musiques, thème Couleurs | « V10 · Mes musiques · thème Couleurs » |
| V11 | Éditeur de musique, thème Couleurs | « V11 · Éditeur de musique · thème Couleurs » |
| V12 | Enregistrer la musique, thème Couleurs | « V12 · Enregistrer la musique · thème Couleurs » |
| V13 | Mes musiques, thème Brume clair | « V13 · Mes musiques · thème Brume clair » |
| V14 | Éditeur de musique, thème Brume clair | « V14 · Éditeur de musique · thème Brume clair » |
| V15 | Enregistrer la musique, thème Brume clair | « V15 · Enregistrer la musique · thème Brume clair » |
| V16 | Mes musiques, thème Brume sombre | « V16 · Mes musiques · thème Brume sombre » |
| V17 | Éditeur de musique, thème Brume sombre | « V17 · Éditeur de musique · thème Brume sombre » |
| V18 | Enregistrer la musique, thème Brume sombre | « V18 · Enregistrer la musique · thème Brume sombre » |

Les vues V7 à V18 sont rangées sous le titre « Musique — Mes musiques et éditeur », une rangée
par thème. Elles viennent toutes d'un seul fichier, `Musique.dc.html` (V7), que les autres
importent avec leur thème et leur vue.

Le prototype (mode Play) relie V1 → V2, V3 ou V5 selon le thème choisi, V4 → V1, V2 ou V3, et
V1 → Mes musiques du thème en cours (V7, V10, V13 ou V16). Les liens ne transmettent pas
l'état : une musique choisie « En jeu » dans V7 ne change pas V1.

Le prototype à part [Éditeur de musique BloX](https://claude.ai/artifact/M91HH7KRJWTMHeDvLaV8wz)
(lien privé) reste le banc d'essai des écrans de musique dans les quatre thèmes. Leur
spécification est dans [generateur-musique.md](generateur-musique.md), section Écrans.

## Écrans

### V1 — Accueil
- Titre **BloX**, avec un logo de quatre cellules aux couleurs du thème, et le record.
- **Mode de jeu** : *Classique* (règles 1989, sans réserve ni pièce fantôme) ; *Moderne*
  affiché grisé, « bientôt ».
- **Thème** : *Original* ou *Couleurs*. Le choix recolore immédiatement l'accueil.
- **Niveau de départ** : 0 à 9, sur une ligne.
- **Musique** : *Korobeïniki*, *Menuet*, *Mon fichier*, *Aucune*, avec un bouton
  *Écouter* pour les deux mélodies (P4.7 : bouton Écouter / Arrêter sous chaque musique qui a
  une partition, un seul aperçu à la fois, lu par TalkBack « Écouter Menuet »).
- **Musiques personnelles** (maquette, P4.21 ; à coder en P4.22) :
  - le choix de musique passe en grille de 3 colonnes : *Korobeïniki*, *Menuet*, *Mon fichier*,
    puis la musique personnelle « En jeu » (icône de note, sur deux colonnes ; « Thème du soir »
    dans la maquette, choisie par défaut et qu'on peut écouter) et *Aucune*. Choisir une autre
    musique retire la musique personnelle des parties ;
  - un lien **MES MUSIQUES** dans l'en-tête de la section mène à la liste du thème en cours
    (V7, V10, V13 ou V16) ;
  - un bouton **COMPOSER UNE MUSIQUE** sous le choix (48 px, icône note et crayon, cadre à
    l'encre) ouvre l'éditeur sur une musique vierge (V8, V11, V14 ou V17) ;
  - pour tenir dans 844 px, les espacements sont resserrés : 10 px entre sections au lieu de
    16, 6 px sous les titres de section au lieu de 8.
- Bouton **Jouer**.
- Les choix (thème, niveau, musique) sont gardés d'une ouverture à l'autre ; au premier
  lancement : *Original*, niveau 0, *Korobeïniki* (P3.1).
- Valeurs retenues en P3.4, à confirmer sur la maquette (P3.8) : rien n'est affiché tant que
  les réglages ne sont pas lus (pas de thème qui clignote, pas de « Jouer » au mauvais niveau) ;
  choix en boutons radio groupés (TalkBack), *Classique* sélectionné sans action, *Moderne*
  désactivé ; niveaux 0 à 9 sur une ligne en cellules égales d'au moins 48 dp de haut (360 dp) ;
  record affiché comme le score (6 chiffres, plafond 999 999), lu en clair par TalkBack. Logo
  (P3.7) : 2 × 2 cellules de 20 dp, joint fin, couleurs des pièces du thème
  (`docs/themes.md`), décoratif pour TalkBack. Les niveaux proposés sont ceux du mode
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
  autres en `button` ; un bouton tenu prend sa teinte « pressée » (`buttonPressed`, `accentPressed`) et garde son encre (P2.17) ; A et B en gras. Le tout tient sur 360 dp de large
  avec une marge de 24 dp, sans cible sous 48 dp.
- Pas de bouton de chute instantanée (absent du mode Classique).
- Autres formats (P2.11, retenus par le code, pas encore maquettés) : la disposition qui donne le
  plus grand plateau l'emporte. Portrait comme ci-dessus ; paysage (téléphone couché, tablette) :
  ◀ ▼ ▶ en bas à gauche, B A en bas à droite, score, niveau et lignes à gauche du plateau, pièce
  suivante à droite ; fenêtre étroite : B A sous les flèches. La pièce suivante a la même taille
  de cellule que le plateau. La colonne de score défile si la police agrandie ne tient pas.
  Une fenêtre trop petite rogne plutôt que de superposer (cellule d'au moins 1 px).

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
