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

### V2 et V3 — Jeu (thèmes Original et Couleurs)
- Même disposition dans les deux thèmes, seuls les styles changent (voir `docs/themes.md`).
- Barre du haut : bouton pause, « MODE CLASSIQUE », record.
- Plateau 10 × 20 (cellules de 24 points) et colonne latérale : score, niveau, lignes,
  pièce suivante. Valeurs retenues en P2.4, à confirmer sur la maquette (P2.13) : encarts
  avec cadre de 2 dp, marges de 8 dp et 4 dp, libellé en 11 sp, valeur en 18 sp (une seule
  ligne, réduite si la police du système est agrandie) ; aperçu de la pièce suivante dans un
  cadre de 4 × 2, pièce centrée au pixel près ; coins carrés (Original) ou arrondis de 10 dp
  (Couleurs). Score sur 6 chiffres, niveau sur 2 et lignes sur 3 au moins.
- Commandes : ◀ ▼ ▶ à gauche ; rotation **B** (anti-horaire) et **A** (horaire) à droite,
  décalés comme sur une console portable.
- Pas de bouton de chute instantanée (absent du mode Classique).

### V4 — Fin de partie
- « Partie terminée », badge « Nouveau record » le cas échéant.
- Score en grand, puis lignes, niveau, nombre de « 4 lignes », durée.
- Boutons **Rejouer** et **Menu**.

## Pas encore maquetté

- **Pause** : le plateau doit être masqué.
- **Réglages** (si les options se multiplient : volume, effets sonores, vibrations,
  sensibilité des commandes).

## Points ouverts

- Boutons tactiles ou gestes (glisser pour déplacer, taper pour tourner) : à tester sur
  téléphone. Le maintien de ▼ et la répétition automatique sont plus délicats au doigt.
- Rendu du thème Original pastel sur un écran OLED.
