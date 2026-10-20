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
- Titre **BloX**, avec un logo de quatre cellules aux couleurs du thème, et le record.
- **Mode de jeu** : *Classique* (règles 1989, sans réserve ni pièce fantôme) ; *Moderne*
  affiché grisé, « bientôt » dans le code ; dans la maquette du 2026-10-20 (P5.51), les deux
  choix tiennent sur une ligne et *Moderne* est grisé sans « (bientôt) ».
- **Thème** : *Original* ou *Couleurs*. Le choix recolore immédiatement l'accueil.
- **Niveau de départ** : 0 à 9, sur deux lignes de 5 (P5.29).
- **Musique** : *Korobeïniki*, *Menuet*, *Mon fichier*, *Aucune*, avec un bouton
  *Écouter* pour les deux mélodies (P4.7 : bouton Écouter / Arrêter sous chaque musique qui a
  une partition, un seul aperçu à la fois, lu par TalkBack « Écouter Menuet »).
- **Musiques personnelles** (P4.22, maquette mise au code le 2026-10-20) :
  - la musique personnelle « En jeu » est un choix comme les autres, en rangées qui passent à la
    ligne, sans icône de note ni grille de trois colonnes (« Thème du soir » dans la maquette),
    qu'on peut écouter. Choisir une autre musique la retire des parties ;
  - *Changer* à côté d'*Écouter* sous *Mon fichier* (P5.38) ;
  - un lien **MES MUSIQUES**, souligné, dans l'en-tête de la section mène à la liste du thème en
    cours (V7, V10, V13 ou V16) ;
  - un bouton **COMPOSER UNE MUSIQUE** sous le choix (48 dp, en capitales, icône note et crayon,
    cadre à l'encre) ouvre l'éditeur sur une musique vierge (V8, V11, V14 ou V17) ;
  - l'accueil défile sur un téléphone : V1 fait 1360 px de haut dans le code.
- **Accueil raccourci**, maquetté le 2026-10-20 (demande du développeur, pas encore codé : P5.50
  et P5.51) : V1 passe de 1360 à 861 px (cadre de la planche).
  - les réglages partent en V21 (P5.50) ;
  - logo (cellules de 16) et titre **BloX** (36) sur une même ligne, record dessous ;
  - écart de 16 dp entre les sections au lieu de 24, marge du haut de 16 ;
  - les choix de musique se suivent en rangées sans rien dessous ; une seule ligne d'actions,
    sous les choix, vaut pour la musique sélectionnée : nom du fichier et *Changer* pour *Mon
    fichier*, *Écouter* / *Arrêter* avec une icône (lecture ou carré), rien pour *Aucune*.
- Section **Réglages** (P5.37) entre « COMPOSER UNE MUSIQUE » et « Jouer » : deux rangées pleine
  largeur en encart (couleurs d'un choix non sélectionné), d'au moins 48 dp, **Vibrations** et
  **Effets sonores**, libellé à gauche, interrupteur à droite, désactivés par défaut. Éteint :
  piste `panel`, contour et bouton `ink` à gauche ; allumé : piste `accent`, bouton `accentInk` à
  droite (l'état se lit à la position). Toute la rangée est un interrupteur pour TalkBack
  (« Vibrations, interrupteur, désactivé »). En police agrandie, le libellé garde sa taille et passe
  à la ligne aux espaces ; s'il ne tient pas à côté de l'interrupteur, celui-ci passe dessous, à
  droite. Un changement vaut pour la partie suivante (les réglages sont lus à sa création).
  **Tests :** `HomeScreenTest › TalkBack reads Vibrations and Effets sonores as switches, off by default`,
  `HomeScreenTest › TalkBack reads a setting turned on as on, the other staying off`,
  `HomeScreenTest › tapping a setting off turns it on, tapping a setting on turns it off`,
  `HomeScreenTest › each setting is a row as wide as the screen within its margins, 48 dp high at least`,
  `HomeScreenTest › the settings stand between Composer une musique and Jouer, under their heading Réglages`,
  `HomeScreenTest › in a font twice as large, on 360 dp, the label of each setting keeps its size, its words whole, in a row of 48 dp`,
  `HomeViewModelTest › the vibrations and the sound effects are shown as saved`,
  `HomeViewModelTest › turning the vibrations on saves them on and shows them on, then off again`,
  `HomeViewModelTest › turning the sound effects on saves them on and shows them on, then off again`,
  `SettingColorsTest › the label of a setting stands out from its row by a contrast of 4,5 to 1 at least`,
  `SettingColorsTest › a switch off, its outline and its thumb, stands out from its row by a contrast of 3 to 1 at least`,
  `SettingColorsTest › a switch on stands out from its row, and its thumb from it, by a contrast of 3 to 1 at least`.
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
  (`HomeUiStateTest › the start levels 0 to 9 stand on two rows of five, 0 to 4 then 5 to 9`,
  `HomeUiStateTest › a last row of fewer than five start levels holds those left, in order`,
  `HomeUiStateTest › five start levels stand on a single full row`,
  `HomeUiStateTest › eleven start levels stand on three rows, the last holding a single level`,
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
  reportées sur la maquette (P2.16) : bouton pause de 48 dp au moins, en `button` avec le cadre `buttonOutline` des touches (P2.23), pictogramme
  dessiné de 24 dp (deux barres d'un quart de la largeur, un quart d'écart, bords au pixel
  entier) ; titre centré, en gras, sur 2 lignes au plus ; record à droite, libellé 11 sp et
  valeur 18 sp comme la colonne latérale, sur 6 chiffres plafonnés à 999 999 (TalkBack lit la
  vraie valeur ; tant que le record n'est pas lu, sa place reste vide, sans « 000000 ») ; 8 dp
  entre les trois éléments.
- Plateau 10 × 20 (cellules de 21 points à 390 dp, comme le code ; 24 sur l'ancienne maquette, P2.19) et colonne latérale : score, niveau, lignes,
  pièce suivante. Valeurs retenues en P2.4, reportées sur la maquette (P2.13) : encarts
  avec cadre de 2 dp, marges de 8 dp et 4 dp, libellé en 11 sp, valeur en 18 sp (une seule
  ligne, réduite si la police du système est agrandie) ; aperçu de la pièce suivante dans un
  cadre de 4 × 2, pièce centrée au pixel près ; coins carrés (Original) ou arrondis de 10 dp
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
  220 × 590 dp trop petite, 220 × 592 jouable ; sous environ 204 dp de large, toujours trop petite ;
  téléphones, tablettes et la fenêtre de 220 × 640 jamais touchés. Le seuil ignore la police : en
  police agrandie ou avec la barre sur deux rangées (P2.26), la cellule réelle peut descendre un peu
  sous 8 dp près du seuil.

  **Tests :** `GameLayoutTest › the narrowest multi-window is too small to play 590 dp high, its cells under 8 dp, and playable 592 dp high`,
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
  `NextPieceCellsTest › every next piece fits the preview the layout reserves, 4 cells wide and 2 high`,
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
- Maquetté le 2026-10-20 (demande du développeur), pas encore codé (P5.50). On y arrive depuis
  l'accueil (V1) par un bouton engrenage de 48 dp en haut à droite (« Paramètres » pour TalkBack) ;
  la section **Réglages** de l'accueil disparaît.
- Barre du haut comme « Mes musiques » : flèche **Retour** (48 dp) vers l'accueil, titre
  « Paramètres ». Section **Sons et vibrations** : les interrupteurs **Vibrations** et **Effets
  sonores** de P5.37, inchangés (rangées en encart de 48 dp au moins, désactivés par défaut,
  rangée entière lue comme interrupteur), puis la mention « Valable dès la partie suivante. ».
- Les réglages du mode Moderne (P6.17, P6.21) s'ajouteront à cette page.

## Icônes

Maquettées le 2026-10-20 (P5.53, demande du développeur). Codées (P5.54a) : jeu d'icônes
(`BloxIcon`, `core:designsystem`), écran de jeu, pause, fin de partie et éditeur ; restent
l'accueil et « Mes musiques » (P5.54b). Une icône
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
  flèche anti-horaire sur B, horaire sur A, de 24 dp, au-dessus de la lettre (13 sp), qui reste
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

## Pas encore codé

- **Nom du fichier** sous « Mon fichier » (maquette : « orage-d-ete.ogg ») : P5.38.
- **Page Paramètres** (V21) et bouton engrenage de l'accueil (V1) : P5.50.

## Points ouverts

- Boutons tactiles ou gestes (glisser pour déplacer, taper pour tourner) : à tester sur
  téléphone. Le maintien de ▼ et la répétition automatique sont plus délicats au doigt.
- Rendu du thème Original pastel sur un écran OLED.
