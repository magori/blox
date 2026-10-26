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
| V19 | Pause (thème au choix) | « V19 · Pause » |
| V20 | Jeu, fenêtre très étroite (220 × 640, thème au choix) | « V20 · Jeu · fenêtre très étroite » |
| V21 | Paramètres (thème au choix) | « V21 · Paramètres » |
| V22 | Jeu, thème Couleurs v2 nuit (V3 est le jour) | « V22 · Jeu · thème Couleurs sombre (piste C) » |
| V23 | Mes musiques, thème Couleurs v2 nuit | « V23 · Mes musiques · thème Couleurs sombre » |
| V24 | Éditeur de musique, thème Couleurs v2 nuit | « V24 · Éditeur de musique · thème Couleurs sombre » |
| V25 | Enregistrer la musique, thème Couleurs v2 nuit | « V25 · Enregistrer la musique · thème Couleurs sombre » |
| V26 | Choix du jeu, cartouche Classique seule (écran d'ouverture, Couleurs v2) | « V26 · Choix du jeu · cartouche Classique · jour », « … nuit », « … console chargée » |
| V27 | Accueil v2 du mode Classique | rangée « Couleurs v2 — Accueil » |
| V28 | Jeu v2, avec ses retours (effacement, points, 4 lignes, danger, niveau, record battu) | rangées « Couleurs v2 — Jeu… » et « Retours de jeu » |
| V29 | Pause v2 (rideau et carte) | « Pause · téléphone » |
| V30 | Fin de partie v2 | « Fin de partie · téléphone », « … paysage » |
| V31 | Paramètres v2 (Affichage, Sons et vibrations, Record) | rangée « Ouverture… puis Paramètres » |
| V32 | Mes musiques v2, jour (écoute, menu, supprimer, vide) | rangée « Couleurs v2 — Musique (jour) » |
| V33 | Éditeur de musique v2, jour (écoute, dé en feuille) | même rangée |
| V34 | Enregistrer la musique v2, jour (nom pris, grille vide) | même rangée |
| V35 | Le Serrurier, Couleurs v2 (en cours, aperçu refusé, indice, serrure ouverte) | à dessiner (P7.16) ; prototype d'origine « Idée · Le Serrurier » |

V3 et V10 à V12 montrent désormais le thème Couleurs v2 de jour, V22 à V25 de nuit (piste C,
2026-10-25). V26 à V31 sont les écrans refondus du lot « Couleurs v2 » (P5.56 à P5.86), dessinés
en téléphone debout, jour et nuit, avec en plus le téléphone couché et les tablettes (formats
optionnels, P5.84) ; V32 à V34 sont les écrans de musique v2, en jour seulement (P5.86). Chaque écran v2 est un composant de la planche (`V2Accueil`, `V2Jeu`,
`V2Parametres`) dont les réglages Format, Sombre, Vue et Effets réduits donnent toutes les
variantes. Leur spécification est le [design system](design-system/README.md). Exportés de la
maquette pour le code dans [design-system/pictogrammes.md](design-system/pictogrammes.md) (P5.87) : les pictogrammes cartouche, moins, étoile, note barrée,
égaliseur, effets réduits, schéma des touches, vibreur, vibration au toucher, volume, trophée, en
pixels 8 × 8 (Original) et en traits (Couleurs, Brume), le logo BloX v2 (`LogoBloX`) et la
mascotte (`Mascotte`).

Mise à jour du 2026-10-20 : la planche suit désormais le code (captures de référence du dépôt)
pour V1 à V18 ; V19 et V20 sont nouvelles. V1 fait 1360 px de haut (l'accueil défile sur un
téléphone). Seule la police normale est dessinée : les variantes en police ×2 (barre d'outils de
l'éditeur en deux rangées, réponses des fenêtres empilées) ne le sont pas. Brume n'étant pas codé,
quelques jetons de ses vues ont été choisis sur la planche et restent à confirmer (P5.41) : cadre
des touches `#8B8799` (clair) et `#7E7B92` (sombre), teintes tenues `#E2E0EA` et `#4A4760`, accent
tenu `#3730A3` et `#6366F1`.

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
- En-tête (P5.51) : logo de quatre cellules de 16 dp aux couleurs du thème et titre **BloX**
  (36 sp, une ligne, réduit en police agrandie) sur une même ligne, le record dessous ; marge du
  haut de 16 dp, 16 dp entre les sections. Bouton engrenage de 48 dp en haut à droite, lu
  « Paramètres » par TalkBack, qui ouvre V21 (P5.50) ; une place vide de 48 dp à gauche garde
  le titre centré.
- **Mode de jeu** : *Classique* (règles 1989, sans réserve ni pièce fantôme) ; *Moderne*
  affiché grisé, sans « (bientôt) » (P5.51) ; les deux sur une ligne à 360 dp en police normale,
  *Moderne* sous *Classique* en police ×2.
- **Thème** : *Original* ou *Couleurs*. Le choix recolore immédiatement l'accueil.
- **Niveau de départ** : 0 à 9, sur deux lignes de 5 (P5.29).
- **Musique** : *Korobeïniki*, *Menuet*, *Mon fichier*, la musique personnelle « En jeu »,
  *Aucune*, en rangées qui passent à la ligne, sans rien dessous. Une seule ligne d'actions, sous
  tous les choix, vaut pour la musique sélectionnée (P5.51) : *Écouter* / *Arrêter*, avec un
  triangle ou un carré que TalkBack ne lit pas (« Écouter Menuet ») ; pour *Mon fichier*,
  *Écouter* et *Changer* côte à côte, empilés s'ils ne tiennent pas ; rien pour *Aucune*. Un
  seul aperçu à la fois (P4.7), arrêté en changeant de choix, en ouvrant les Paramètres ou
  « Mes musiques ».
- **Musiques personnelles** (P4.22, maquette mise au code le 2026-10-20) :
  - la musique personnelle « En jeu » est un choix comme les autres, sans icône de note ni
    grille de trois colonnes (« Thème du soir » dans la maquette), qu'on peut écouter. Choisir
    une autre musique la retire des parties ;
  - un lien **MES MUSIQUES**, souligné, dans l'en-tête de la section mène à la liste du thème en
    cours (V7, V10, V13 ou V16) ;
  - un bouton **COMPOSER UNE MUSIQUE** sous le choix (48 dp, en capitales, icône note et crayon,
    cadre à l'encre) ouvre l'éditeur sur une musique vierge (V8, V11, V14 ou V17).
- L'accueil défile sur un petit téléphone ; la planche V1 fait 861 px de haut.
  **Tests :** `HomeScreenTest › the gear button, 48 dp, read Paramètres, opens the settings`,
  `HomeScreenTest › the gear button stands at the top right of the home screen, beside the title`,
  `HomeScreenTest › the logo and the title BloX stand on one line, the record under them`,
  `HomeScreenTest › the home screen no longer shows the settings, gone to their own page`,
  `HomeScreenTest › on 360 dp, the two modes stand on one line`,
  `HomeScreenTest › the home screen leaves 16 dp above its header, and 16 dp between two sections`,
  `HomeScreenTest › the music choices bear nothing under them: one line of actions, under them all, is for the music selected`,
  `HomeScreenTest › with Mon fichier selected, the line of actions offers to listen to the file and to change it, side by side`,
  `HomeScreenTest › with Aucune selected, there is nothing to listen to nor to change`,
  `HomeScreenTest › the music selected being listened to offers to stop it, under a glyph that TalkBack does not read`,
  `HomeScreenTest › in a font twice as large, in a window too narrow for both, Changer goes under Écouter, each label on one line`,
  `HomeViewModelTest › opening the settings stops the preview`,
  `HomeViewModelTest › opening the settings stops the music of the player listened to`,
  `BloxNavDisplayTest › opening the settings stops the music listened to on the home screen`,
  `BloxNavDisplayTest › opening the settings stops the music of the player listened to on the home screen`.
- Bouton **Jouer**, 48 dp de haut, en `accent`.
- Les choix (thème, niveau, musique) sont gardés d'une ouverture à l'autre ; au premier
  lancement : *Original*, niveau 0, *Korobeïniki* (P3.1).
- Valeurs retenues en P3.4, reportées sur la maquette (P3.8, 2026-10-20) : rien n'est affiché tant que
  les réglages ne sont pas lus (pas de thème qui clignote, pas de « Jouer » au mauvais niveau) ;
  choix en boutons radio groupés (TalkBack), *Classique* sélectionné sans action, *Moderne*
  désactivé ; choix sélectionné rempli en `accent`, sans soulignement (retiré en P5.36) et marqué d'une coche (P5.42), les
  autres en encart (`panel`, cadre de 2 dp `panelBorder`, libellé `ink`) (P5.26,
  `docs/themes.md`, Choix de l'accueil) ;
  niveaux en deux lignes de 5 (0 à 4, puis 5 à 9 ; P5.29, décision du développeur), en cellules
  égales d'au moins 48 dp de large et de haut à 360 dp, écart de 4 dp ; une dernière ligne
  incomplète s'aligne sous les colonnes du dessus ; un seul groupe de boutons radio, lu de 0 à 9
  (`ChoiceRowsTest › ten choices in five columns stand on two rows of five, 0 to 4 then 5 to 9`,
  `ChoiceRowsTest › a last row of fewer choices than columns holds those left, in order`,
  `ChoiceRowsTest › as many choices as columns stand on a single full row`,
  `ChoiceRowsTest › eleven choices in five columns stand on three rows, the last holding a single choice`,
  `HomeScreenTest › the start levels stand on two rows of five, 0 to 4 above 5 to 9, in cells as wide as one another`,
  `HomeScreenTest › a last row of fewer than five start levels is aligned on the cells of the row above, as wide`,
  `HomeScreenTest › TalkBack reads the start levels as one group of radio buttons, from 0 to 9`) ;
  record affiché comme le score (6 chiffres, plafond 999 999), lu en clair par TalkBack. Logo
  (P3.7) : 2 × 2 cellules de 20 dp, joint fin, couleurs des pièces du thème
  (`docs/themes.md`), décoratif pour TalkBack. Les niveaux proposés sont ceux du mode
  (`LevelProgression`) ; un niveau enregistré hors de la liste s'affiche comme le plus proche.
  Parcours (P5.5) : `HomeScreenTest › TalkBack reads the record as it is, neither capped nor padded with zeros as it is shown`,
  `HomeScreenTest › the Modern mode is disabled, as TalkBack reads it, beside the Classic mode, selected`,
  `BloxNavDisplayTest › Jouer starts a game at the start level chosen on the home screen`.

### V2 et V3 — Jeu (thèmes Original et Couleurs)
- Même disposition dans les deux thèmes, seuls les styles changent (voir `docs/themes.md`).
- Barre du haut : bouton pause, « MODE CLASSIQUE », record. Valeurs retenues en P2.6,
  reportées sur la maquette (P2.16) : bouton pause en disque de 48 dp (famille Icône des boutons bulles, P5.78 ; avant, en `button` cadré de
  `buttonOutline`, P2.23), pictogramme de 24 dp ; titre centré, en gras, sur 2 lignes au plus ; record à droite, libellé 11 sp et
  valeur 18 sp comme la colonne latérale, sur 6 chiffres plafonnés à 999 999 (TalkBack lit la
  vraie valeur ; tant que le record n'est pas lu, sa place reste vide, sans « 000000 ») ; 8 dp
  entre les trois éléments. Une fois le record battu (un score au-dessus d'un record supérieur à 0, P5.56),
  « RECORD » devient l'autocollant NOUVEAU RECORD (`accent`, sur deux lignes s'il le faut, sans inclinaison en
  Original) et la valeur suit le score ; rien ne bouge dans la barre (P5.78).
- Plateau 10 × 20 (cellules de 21 points à 390 dp, comme le code, un peu moins depuis le cadre du plateau de P5.78 ; 24 sur l'ancienne maquette, P2.19) et colonne latérale : pièce suivante, score (avec sa barre de record de 4 dp, P5.78), lignes,
  niveau ; lue dans cet ordre par TalkBack dans toutes les mises en page. Le plateau est cadré (`BoardFrame` :
  2 dp de `panelBorder` et 6 dp du fond du plateau, dessinés dans les marges, sans changer la taille des cases
  ni les seuils de fenêtre).
  **Tests (P5.78) :** `GameViewModelTest › the record is beaten once the score is above a record above 0`,
  `GameViewModelTest › without a record the game shows no new record`,
  `GameViewModelTest › the record stays beaten once the game over saves the score as the record`,
  `RecordBarTest › the record bar fills with the score up to the record`,
  `RecordBarTest › the filled part of the record bar stands out from its track by 3 to 1 at least`,
  `ShownRecordTest › once beaten the record shown follows the score, as the new record`,
  `GameLayoutTest › the frame of the board stays within the width of the space, lined up with the top bar and the controls`,
  `ScoreGridTest › the score above the board shows the next piece and the score, then the lines and the level, in order`. Valeurs retenues en P2.4, reportées sur la maquette (P2.13) : encarts
  avec cadre de 2 dp, marges de 8 dp et 4 dp, libellé en 11 sp, valeur en 18 sp (une seule
  ligne, réduite si la police du système est agrandie) ; aperçu de la pièce suivante dans un
  cadre de 4 × 2, pièce centrée au pixel près ; coins carrés (Original) ou arrondis de 16 dp (`md`, P5.59)
  (Couleurs). Score sur 6 chiffres, niveau sur 2 et lignes sur 3 au moins.
- Commandes : ◀ ▼ ▶ à gauche ; rotation **B** (anti-horaire) et **A** (horaire) à droite,
  décalés comme sur une console portable. Valeurs retenues en P2.5, reportées sur la
  maquette (P2.14) : boutons de direction de 52 dp, de rotation de 56 dp, espacés de 8 dp ;
  B plus bas de 24 dp ; flèches dessinées (triangles) ; sur B et A, flèche de rotation de 24 dp
  (anti-horaire sur B, horaire sur A) au-dessus de la lettre (P5.54a), en 20 sp gras et non 13 sp
  comme la maquette : à 13 sp la lettre serait du texte normal, à 4,5:1, que B au repos en
  Original (4,04:1) et A tenu en Couleurs (4,48:1) n'atteignent pas ; en police agrandie, la
  lettre se réduit à la place qui reste sous la flèche (28 dp) ; A en `accent`, les
  autres en `button` ; un bouton tenu prend sa teinte « pressée » (`buttonPressed`, `accentPressed`) et garde son encre (P2.17) ; A et B en gras. Un bouton au repos
  est entouré d'un cadre de 2 dp en `buttonOutline`, `#646B87` en thème Couleurs, invisible en
  Original (P2.22, contraste de 3:1 avec la page). Le tout tient sur 360 dp de large
  avec une marge de 24 dp, sans cible sous 48 dp.
- Pas de bouton de chute instantanée (absent du mode Classique).
- Gestes système (P5.16) : les deux groupes de commandes, ◀ ▼ ▶ (64 dp) et B A (80 dp), sont
  exclus des gestes système à partir d'Android 10. En navigation par gestes, un glissement qui
  part du bord sur une commande ne déclenche donc plus Retour, qui annulait les doigts et mettait
  la partie en pause, même quand la zone du geste dépasse les marges de 24 dp. Chaque groupe
  tient sur son bord dans les 200 dp que le système retient par bord ; le bas de l'écran (geste
  Accueil) ne peut pas être exclu. À vérifier au doigt sur un téléphone, sensibilité du geste
  Retour au maximum.

  **Tests :** `GameScreenGesturesTest › every control is excluded from the system gestures, by rectangles of 200 dp high at most that the system honours`,
  `GameLayoutTest › each group of controls holds to its edge, within the 200 dp of gesture exclusion honoured per edge`.
- Autres formats (P2.11, retenus par le code ; fenêtre très étroite maquettée en V20) : la disposition qui donne le
  plus grand plateau l'emporte. Portrait comme ci-dessus ; paysage (téléphone couché, tablette) :
  ◀ ▼ ▶ en bas à gauche, B A en bas à droite, score, niveau et lignes à gauche du plateau, pièce
  suivante à droite ; fenêtre étroite : B A sous les flèches. La pièce suivante a la même taille
  de cellule que le plateau. La colonne de score défile si la police agrandie ne tient pas.
  Une fenêtre trop petite rogne plutôt que de superposer (cellule d'au moins 1 px).
- Fenêtre très étroite (P2.20, multi-fenêtre) : sous environ 288 dp de large, là où le plateau
  serait plus étroit que la colonne de score, une disposition de plus est candidate : score et
  niveau, puis lignes et pièce suivante, en deux rangées au-dessus du plateau, les commandes
  empilées dessous. Sa cellule est plafonnée à celle de la disposition habituelle à cette largeur,
  et elle n'est retenue que si elle donne un plateau strictement plus grand et tient : un plateau
  ne rétrécit jamais quand la fenêtre s'élargit, et les téléphones en portrait ne changent pas. À
  220 × 640 dp, la cellule passe de 4 dp à environ 10 dp. La hauteur réservée aux panneaux suit la
  police du système. Les flèches se rapprochent (jusqu'à se toucher) quand elles ne tiennent pas
  dans la largeur, 20 px au lieu de 21 à 220 dp et ×2,625 ; B A garde ses 8 dp ; les touches gardent
  leur taille (52 et 56 dp) et leur exclusion des gestes système.
- Fenêtre trop petite pour jouer (P2.24, décision du développeur) : quand la cellule calculée pour
  la fenêtre (sans les barres système, barre du haut comptée sur une rangée, en police normale)
  descendrait sous 8 dp, « Agrandissez la fenêtre » remplace le plateau, ou l'écran de pause si la
  fenêtre rétrécit pendant la pause. La partie se met en pause avant la première image ; la barre du
  haut reste, son bouton pause désactivé ; le message, `ink` sur `page`, en mots entiers, est
  annoncé par TalkBack (région vive polie) ; les commandes sont ignorées. Quand la fenêtre redevient
  assez grande, l'écran de pause revient et seul Reprendre relance la partie. Le retour système
  quitte alors la partie, comme Menu (sinon Reprendre remettrait aussitôt en pause). Bascules :
  220 × 591 dp trop petite, 220 × 593 jouable (dès 592 à 1,5 et 3,5 px par dp) ; sous environ 204 dp de large, toujours trop petite ;
  téléphones, tablettes et la fenêtre de 220 × 640 jamais touchés. Le seuil ignore la police : en
  police agrandie ou avec la barre sur deux rangées (P2.26), la cellule réelle peut descendre un peu
  sous 8 dp près du seuil.

  **Tests :** `GameLayoutTest › the narrowest multi-window is too small to play 591 dp high, its cells under 8 dp, and playable 593 dp high`,
  `GameLayoutTest › a window too narrow for the arrows is too small to play, however high`,
  `GameLayoutTest › phones and tablets, in portrait and in landscape, and the narrowest multi-window are never too small to play`,
  `BackActionTest › while the window is too small to play, the system back leaves the paused game, as Menu does`,
  `GameScreenLifecycleTest › in a window too small to play, « Agrandissez la fenêtre » replaces the board, the game paused`,
  `GameScreenLifecycleTest › paused, a window that becomes too small to play shows the message in place of the pause screen`,
  `GameScreenLifecycleTest › under the message, the controls of the game are ignored, even once the game resumes`,
  `GameScreenLifecycleTest › once the window is large enough again, the pause screen shows, and Reprendre resumes the game`,
  `GameScreenLifecycleTest › while the window is too small to play, the system back leaves the game`.
  Captures : `GameScreenTooSmallOriginal`, `GameScreenTooSmallColorful`, `GameScreenTooSmallLargeFont`.

  **Tests :** `GameLayoutTest › a very narrow window puts the score above the board, for a board larger than beside the side column`,
  `GameLayoutTest › the board never shrinks as the window widens, from the narrowest multi-window to a phone`,
  `GameLayoutTest › when the score above gives a board of the same size, the usual layout stays`,
  `GameLayoutTest › when the score above does not fit either, the usual layout stays, cut`,
  `GameLayoutTest › a phone in portrait keeps the side column beside the board, though the score above would give a larger one`,
  `GameLayoutTest › with the font twice as large, the score above reserves the height of its larger texts`,
  `GameLayoutTest › in the narrowest window at 2,625 px per dp the arrows come closer, to fit its width`,
  `GameLayoutTest › a window too narrow for the arrows even touching puts them touching, cut at the right`,
  `SidePanelSizesTest › every next piece fits the preview the layout reserves, 4 cells wide and 2 high`,
  `ScoreGridTest › the score above the board fits the height the layout reserves for it, in both themes, font enlarged`.
  Captures : `GameScreenNarrow`, `GameScreenNarrowOriginal`, `GameScreenNarrowLargeFont`.
- Barre du haut dans une fenêtre très étroite (P2.26) : le titre et le record restent sur une
  rangée, à côté du bouton pause, si chacun tient entier dans sa moitié de la place restante, à sa
  plus petite taille (1 px de marge contre les arrondis de la rangée) ; sinon le titre passe
  au-dessus, sur toute la largeur et une ligne si possible, sans couper de mot, avec dessous le
  bouton pause et le record, plafonné à sa taille en police normale pour rester à la hauteur du
  bouton (`notEnlarged`). Le plateau y perd un peu de hauteur (à 220 × 640 dp, cellule d'environ
  10,3 à 9,1 dp en Original ×1). Le titre est alors lu le premier par TalkBack ; le record lu reste
  la vraie valeur. Sur un téléphone de 360 dp, la barre garde sa rangée, même à ×2 ; en Original
  ×2 la marge est d'un demi-pixel (titre de 324 px pour 324,5) : à vérifier sur d'autres densités.

  **Tests :** `TopBarRowsTest › the title and the record stay on one row when each fits whole in half the room beside the pause button`,
  `TopBarRowsTest › the title goes above the pause button and the record when it does not fit whole in its half`,
  `TopBarRowsTest › the title goes above the pause button and the record when the record does not fit whole in its half`,
  `TopBarRowsTest › a title one pixel narrower than its half stays on the row, a margin against the rounding of the row`,
  `TopBarRowsTest › a title within the last pixel of its half goes above, rather than risk being cut`,
  `TopBarRowsTest › a record within the last pixel of its half sends the title above, rather than risk being cut`,
  `GameTopBarTest › in the narrowest window the title is shown whole, no word cut, in both themes, font enlarged`,
  `GameTopBarTest › in the narrowest window the record is shown whole, in both themes, font enlarged`,
  `GameTopBarTest › in the narrowest window the record is no taller than the pause button: the board keeps its height`,
  `GameTopBarTest › in the narrowest window the pause button keeps its 48 dp, in both themes, font enlarged`,
  `GameTopBarTest › in the narrowest window TalkBack reads the real record, beyond the 999 999 shown`,
  `GameTopBarTest › on a phone of 360 dp the bar stays on one row, the title right of the pause button, font enlarged`.
  Captures : `GameScreenNarrowOriginal`, `GameScreenNarrowLargeFont`.

### V4 — Fin de partie
- « Partie terminée », badge « Nouveau record » le cas échéant.
- Score en grand, puis lignes, niveau, nombre de « 4 lignes », durée.
- Boutons **Rejouer** et **Menu**.
- Valeurs retenues en P2.7, reportées sur la maquette (P2.15) : titre en 22 sp, score dans un
  encart en 40 sp au plus, statistiques « libellé – valeur » (libellé 11 sp, valeur 18 sp), marges
  de 24, 16 et 8 dp ; badge en `accent` sur `accentInk` ; Rejouer plein (`accent`), Menu en
  contour (`panelBorder`, encre `ink`) ; lignes et niveau avec leurs zéros, comme en jeu. L'écran
  défile si la police est agrandie. TalkBack annonce l'écran et lit la durée « 1 minute et
  5 secondes ».

### V19 — Pause
- Le plateau est masqué : fond `page` opaque qui remplace tout l'écran de jeu, titre « Pause » en
  22 sp, boutons **Reprendre** (plein, `accent`) et **Menu** (contour, `panelBorder`), même mise
  en page que V4 (marges de 24 et 16 dp, cibles de 48 dp au moins). TalkBack annonce l'écran
  (P2.6, maquetté le 2026-10-20). Dans le prototype, le bouton pause de V2, V3, V5 et V6 l'ouvre.

### V20 — Jeu, fenêtre très étroite
- 220 × 640 : score et niveau, puis lignes et pièce suivante en grille 2 × 2 au-dessus du plateau
  (cellule de 10), flèches rapprochées, B A dessous ; en Original, le titre passe au-dessus du
  bouton pause et du record (P2.20, P2.26).

### V21 — Paramètres
- Demandée par le développeur (2026-10-20), codée en P5.50 : `SettingsScreen` et
  `SettingsViewModel` dans `feature:home`, clé `SettingsRoute`, entrée `settingsEntry` assemblée
  par `app`. On y arrive depuis l'accueil (V1) par le bouton engrenage ; la flèche Retour et le
  retour du système ramènent à l'accueil.
- Barre du haut comme « Mes musiques » : flèche **Retour** (48 dp) et titre « Paramètres »
  (titre pour TalkBack), affichés dès l'ouverture, avant même la lecture des réglages. Section
  **Sons et vibrations**, marges de 16 dp : les interrupteurs **Vibrations** et **Effets
  sonores** (P5.37 ; Vibrations désactivées, Effets sonores activés par défaut depuis P5.65), rangées pleine largeur en encart (couleurs d'un choix
  non sélectionné) d'au moins 48 dp, libellé à gauche, interrupteur à droite. Éteint : piste
  `panel`, contour et bouton `ink` à gauche ; allumé : piste `accent`, bouton `accentInk` à
  droite (l'état se lit à la position). Toute la rangée est un interrupteur pour TalkBack
  (« Vibrations, interrupteur, désactivé »). En police agrandie, le libellé garde sa taille et
  passe à la ligne aux espaces ; s'il ne tient pas à côté de l'interrupteur, celui-ci passe
  dessous, à droite. Puis la mention « Valable dès la partie suivante. » : les réglages sont lus
  à la création de la partie. Une écriture qui échoue laisse les réglages enregistrés affichés.
- Les réglages du mode Moderne (P6.17, P6.21) s'ajouteront à cette page.
  **Tests :** `SettingsScreenTest › TalkBack reads Vibrations and Effets sonores as switches, read off when off`,
  `SettingsScreenTest › TalkBack reads a setting turned on as on, the other staying off`,
  `SettingsScreenTest › tapping a setting off turns it on, tapping a setting on turns it off`,
  `SettingsScreenTest › each setting is a row as wide as the screen within its margins, 48 dp high at least`,
  `SettingsScreenTest › the settings stand under their heading Sons et vibrations, and Valable dès la partie suivante under them`,
  `SettingsScreenTest › in a font twice as large, on 360 dp, a setting label keeps its words whole and 90 % of its size at least`,
  `SettingsScreenTest › the back arrow, 48 dp, read Retour, goes back to the home screen`,
  `SettingsScreenTest › the title Paramètres is a heading, beside the back arrow`,
  `SettingsScreenTest › while the saved settings are not read, the page shows its back arrow and its title, and no switch`,
  `BloxNavDisplayTest › while the saved settings are not read, the settings show their back arrow and title, and no switch`,
  `SettingsViewModelTest › the vibrations and the sound effects are shown as saved`,
  `SettingsViewModelTest › turning the vibrations on saves them on and shows them on, then off again`,
  `SettingsViewModelTest › turning the sound effects on saves them on and shows them on, then off again`,
  `SettingsViewModelTest › the settings page is loading until the saved settings are read, then shows them`,
  `SettingsViewModelTest › a setting that cannot be saved leaves the saved settings shown, without crashing`,
  `SettingsViewModelTest › the factory builds the settings page on the settings of the application`,
  `BloxNavDisplayTest › the gear of the home screen opens the settings, whose switches save, and Retour goes back home`,
  `BloxNavDisplayTest › on the settings, the system back goes back to the home screen`,
  `SwitchLookTest › the label and the sub-line stand out from their row by 4,5 to 1, at rest on the page and pressed`,
  `SwitchLookTest › the thumb stands out from its track by 3 to 1, on and off, and the check from the thumb`,
  `SwitchLookTest › the track is framed in button outline at rest and in the ink of its row pressed, by 3 to 1, on and off`.

## Icônes

Maquettées le 2026-10-20 (P5.53, demande du développeur). Codées : jeu d'icônes
(`BloxIcon`, `core:designsystem`), écran de jeu, pause, fin de partie et éditeur (P5.54a),
accueil et « Mes musiques » (P5.54b). Une icône
accompagne toujours un libellé, sauf l'engrenage de V1 et les boutons écouter et « … » des
cartes, qui ont leur nom pour TalkBack ; les autres sont décoratives (pas lues).

- **Style par thème** : en Original, dessins de 8 × 8 pixels pleins (2 dp par pixel à 16 dp, bords
  nets), dans l'esprit de la police Press Start 2P ; en Couleurs et Brume, traits arrondis de
  2,25 sur une grille de 24. Couleur du texte du bouton.
- **Tailles** : 16 dp à côté d'un libellé, 14 dp au-dessus d'un libellé dans la barre d'outils
  de l'éditeur et devant MES MUSIQUES et Écouter, 18 dp sur *Au hasard* et *Nouvelle*, 24 dp
  pour l'engrenage ; 6 à 8 dp entre l'icône et le libellé, 12 dp dans les menus.
- **Jeu d'icônes** : lecture et arrêt (Jouer, Écouter / Arrêter, Reprendre), liste (MES
  MUSIQUES), note (COMPOSER UNE MUSIQUE, *Utiliser* et l'entrée du menu « … »), engrenage
  (Paramètres), dé (*Au hasard*), plus (*Nouvelle*), crayon (*Modifier*), T (*Renommer*),
  corbeille (*Supprimer*), maison (*Menu* de V4 et V19), flèche en boucle (*Rejouer*), et pour
  la barre d'outils de l'éditeur, au-dessus du libellé : case (*Poser*), flèche (*Allonger*),
  flèche retour (*Annuler*), deux cadres (*Copier*), gomme (*Effacer*) ; touches de rotation :
  flèche anti-horaire sur B, horaire sur A, de 24 dp, au-dessus de la lettre (20 sp, voir V2), qui reste
  pour faire le lien avec les boutons de la manette (P5.27).
- Vues touchées : V1 à V6, V7 à V18 (liste, menu, éditeur), V19, V20. La coche des choix sélectionnés et
  la flèche Retour ne changent pas.
- Dans le code (P5.54a) : en Original, chaque pixel de l'icône est un carré d'un nombre entier de
  pixels de l'écran, le dessin centré (l'icône fait donc 8 × ⌊taille / 8⌋ pixels de l'écran, un peu
  moins que la taille demandée) ; une suite de pixels pleins sur une ligne est un seul rectangle.
  Les icônes restent en dp en police agrandie. Écart de 6 dp à côté du dé de l'éditeur (ligne
  partagée avec les catégories en police ×2), 8 dp ailleurs ; menu de l'éditeur à l'écart de
  Material (12 dp).
  **Tests :** `PixelIconTest › a full pixel becomes a square of whole device pixels`,
  `PixelIconTest › each pixel is as many whole device pixels as fit eight times in the box, the drawing centred to the pixel`,
  `PixelIconTest › a run of full pixels on a row is one rectangle`,
  `BloxIconTest › every icon has a pixel drawing of 8 lines of 8 pixels, each full or empty`,
  `StrokeIconTest › a stroke icon is drawn on a grid of 24, in unfilled lines 2,25 thick, rounded at their ends and joins`,
  `StrokeIconTest › every icon has strokes to draw`,
  `IconStyleTest › the Original theme draws its icons in pixels`,
  `IconStyleTest › the Colorful theme draws its icons in strokes`,
  `KeyGlyphTest › the letter keeps its size when it holds under the arrow in the key`,
  `KeyGlyphTest › the letter shrinks to hold under the arrow in the key when the font is enlarged`,
  `KeyGlyphTest › a letter exactly as high as the room under the arrow keeps its size`.
- Dans le code (P5.54b) : l'engrenage, le bouton écouter des cartes et la note d'*Utiliser* sont
  des icônes seules de 24 dp (`GlyphButton(icon)`, `IconSizes.alone`), lues une fois par le nom
  de leur bouton ; *En jeu* garde sa coche. *Au hasard* et *Nouvelle* : 18 dp, écart de 6 dp et
  marge latérale réduite à 2 dp dans le cadre, pour tenir en Original ×2 à 360 dp (comme le
  bouton *Annuler* du message de suppression, qui partage ce bouton). Menu « … » des cartes :
  icônes en tête, la corbeille en `danger` comme *Supprimer*.
  **Tests :** `HomeScreenTest › Jouer, MES MUSIQUES, Écouter and Composer une musique bear an icon that TalkBack does not read`,
  `HomeScreenTest › the gear button is read Paramètres once, its icon adding nothing`,
  `HomeScreenTest › in a font twice as large, on 360 dp, Jouer, MES MUSIQUES and Écouter are shown whole, after their icons`,
  `GlyphButtonTest › a button of an icon alone is 48 dp, read as a button of its description, its icon adding nothing`,
  `PlayTagTest › the tag of the play button stays home_play, which the journey of benchmark copies as it is`,
  `MyMusicsScreenTest › Au hasard and Nouvelle bear an icon that TalkBack does not read, their label read alone`,
  `MyMusicsScreenTest › the listen button and Utiliser of a card are read once, their icons adding nothing`,
  `MyMusicsScreenTest › each item of the menu of a card bears an icon that TalkBack does not read`,
  `MyMusicsScreenTest › in a font twice as large, on 360 dp, Au hasard and Nouvelle are shown whole, after their icons`.

## Pas encore codé

- **Nom du fichier** sous « Mon fichier » (maquette : « orage-d-ete.ogg ») : P5.38.

## Points ouverts

- Boutons tactiles ou gestes (glisser pour déplacer, taper pour tourner) : à tester sur
  téléphone. Le maintien de ▼ et la répétition automatique sont plus délicats au doigt.
- Rendu du thème Original pastel sur un écran OLED.
