# BloX Couleurs

Le thème Couleurs de BloX, refait dans la piste C de la maquette (« Y2K irisé ») : nacre,
lilas et pastels brillants le jour, fond profond, vert acide et néons la nuit. Ce système
décrit les jetons, la typographie, les composants, la mise en page et les retours de jeu des
écrans de BloX dans ce thème. Les thèmes Original et Brume n'en font pas partie.

**Ce dossier.** Copie locale du design system publié sur claude.ai
(https://claude.ai/artifact/QLfJTMQyXYXcxiupPcdsWt) : `tokens.json` (les jetons, clair et
sombre), ce guide, et `components/<Nom>/` (règles d'usage et aperçu HTML, qui lit les jetons en
variables CSS `--<jeton>` fournies par la page du design system). Une modification se fait des
deux côtés.

**Source.** Maquette BloX (canevas Claude Design), rangées « Couleurs v2 » (accueil, jeu, pause,
fin de partie, dans les quatre formats), écrans V3, V10 à V12 et V22 à V25, et la planche
« Piste C · Y2K irisé, jour et nuit ». Conception de l'accueil et du jeu par l'agent
`designer-jeu` (`.claude/agents/designer-jeu.md`), validée par le développeur le 2026-10-25.
Le thème n'est pas encore codé : `docs/themes.md` décrit toujours l'ancien Couleurs.

## Principes

- **Jour et nuit, un seul thème.** Le thème suit le mode du téléphone : jour (clair) par
  défaut, nuit (sombre) quand le téléphone est en mode sombre. Mêmes formes, mêmes tailles,
  mêmes noms de jetons ; seules les couleurs changent.
- **Le plateau est la scène.** Les pièces portent la couleur ; l'interface reste neutre
  (`page`, `panel`, `ink`) avec une seule couleur d'action, `accent`.
- **Chaque geste se sent.** Chaque événement du moteur a son retour visuel (et son, et
  vibration quand ils sont activés), sans jamais cacher la pièce ni retarder la partie
  au-delà des délais NES.
- **Effets riches, réglables.** Un réglage « Effets réduits » garde l'information et retire le
  mouvement ; il est imposé quand Android supprime les animations.
- **Mêmes fonctions que l'existant.** Aucune refonte ne retire une fonction sans le dire.
- **Gros doigts.** Toute cible fait au moins `touch-target` (48 px).
- **Le téléphone debout d'abord, d'autres formats ensuite.** Téléphone couché et tablettes sont optionnels et viennent en dernier (P5.84), mais tout est prévu pour eux : les mêmes blocs, empilés quand l'écran est haut, côte à côte
  quand il est large.

## Boutons

Tous les boutons ont la matière des pièces : reflet en haut et épaisseur en bas
(`button-gloss`). Pressés, ils descendent de 2 px et s'écrasent (`button-gloss-pressed`), tout
de suite ; ils reviennent en `motion-press`. Focus clavier : contour de 3 px `ink` à 3 px du bord.

| Famille | Forme | Repos | Pressé | Désactivé |
|---|---|---|---|---|
| Principal XL (Jouer) | pilule de 72 px (84 tablette, 64 couché) | `accent`, `play-label` en `accent-ink`, sous-ligne `small` 600, disque de 52 px `accent-ink` avec le triangle en `accent` | `accent-pressed` | — |
| Principal (Reprendre, Rejouer) | pilule de 56 px | `accent`, `button-label-strong`, pictogramme de 16 à côté du libellé | `accent-pressed` | — |
| Secondaire (Menu, Changer, tuiles) | pilule de 56 px ; tuile en `radius-md` | `button`, cadre de 2 px `button-outline`, `button-ink` 15/700 | `button-pressed`, cadre gardé | libellé `label`, cadre `panel-border`, sans reflet |
| Icône (Paramètres, Écouter, Pause) | disque de 48 px (56 tablette) | comme Secondaire, pictogramme de 24 | idem | idem |
| Groupe de choix (mode, thème) | boîte de 48 px en pilule, cadre de 2 px `button-outline` autour du groupe ; segments de 44 px | segment : transparent, `ink` 15/500 | retenu : `accent`, coche de 16 et libellé `accent-ink` | Moderne : libellé `label` et étiquette `BIENTÔT` 11/700 |

## Couleurs

| Rôle | Jetons | Règle |
|---|---|---|
| Fond | `page` | Fond de tous les écrans. |
| Plateau | `board`, `empty-cell` | Le plateau est un bloc `board` ; chaque case vide est une bulle `empty-cell`. |
| Encarts | `panel`, `panel-border`, `panel-inset` | Encarts de jeu et cartes : `panel` et cadre décoratif `panel-border` ; tuiles posées dans une carte : `panel-inset`. |
| Texte | `ink`, `label` | Valeurs et titres en `ink` ; étiquettes, sections et actions discrètes en `label`. |
| Boutons et touches | `button`, `button-ink`, `button-outline`, `button-pressed` | Cadre `button-outline` (3,7:1 jour, 4,1:1 nuit) sur tout ce qui se touche. |
| Action | `accent`, `accent-ink`, `accent-pressed`, `accent-ring` | Une action principale par écran, la touche A, le choix retenu, l'interrupteur allumé, les autocollants. |
| Jeu | `clear-glow`, `danger-soft`, `cell-dead` | Effacement des lignes, pile en danger, cases éteintes en fin de partie. |
| Danger | `danger`, `danger-ink` | Supprimer, erreurs, cadre du plateau en danger. |
| Musique | `melody`, `bass` (+ `-soft`, `-hi`), `track-ink`, `cell-ground`, `grid-line`, `label-off` | Une couleur par piste de l'éditeur, distinctes aussi par leur clarté ; `-hi` est le reflet du haut des notes en bulles (de nuit, celui des pièces T et L). |
| Décor | `iris-1`, `iris-2`, `iris-3` | Dégradé du logo, reflet des 4 lignes, barre de record battu. Jamais derrière du texte. |

L'accent du jour (`#B48CFF`) se détache peu de la page (2,3:1) : un bouton d'action se lit par
son libellé `accent-ink` (6,5:1) et sa forme, le choix retenu porte une coche.

## Pièces

Chaque pièce a deux jetons : `piece-<x>` (remplissage) et `piece-<x>-hi` (reflet). Une case
est une **bulle** : dégradé vertical du reflet (en haut) au remplissage (à 55 %), coins
`radius-cell`, ombre `cell-gloss`, 1 px de marge, sans cadre. Sous 16 px : coins de 2 px, pas
d'ombre.

| Pièce | Jour | Nuit |
|---|---|---|
| I | ciel `#8FD8FF` | `#4DD8FF` |
| O | jaune `#FFE680` | `#FFE45E` |
| T | lilas `#C3A2FF` | `#A77BFF` |
| S | menthe `#9DEDB9` | `#3DFF8F` |
| Z | rose `#FF94A6` | `#FF4D6D` |
| J | bleu `#9DB6FF` | `#5C8DFF` |
| L | pêche `#FFB896` | `#FF9E3D` |

## Accueil

L'application s'ouvre sur le **choix du jeu** (cartouches, « Choix des jeux 4 », V26 : une seule cartouche, Classique, aucune endormie) ; la cartouche
Classique ouvre cet accueil, et son disque « Changer de jeu » y ramène.

De haut en bas (téléphone debout ; en paysage, voir Mise en page) :

1. **Barre** : disque « Changer de jeu » (cartouche), puce Record (48 px, étoile, `stat-label`
   et `record`) et bouton Paramètres.
2. **Héros** : logo BloX, puis le **plateau d'aperçu**, qui rejoue un T tombant à la gravité du
   niveau choisi et effaçant deux lignes, avec le vrai score (100 × (niveau + 1)).
3. **Jouer** (Principal XL), avec le rappel « Classique · Niveau 05 · Korobeïniki ».
4. **Carte des réglages**, quatre rangées : **Mode** (Classique toujours accessible ; Moderne
   grisé `BIENTÔT`), **Thème** (Original, Couleurs, Brume), **Niveau** (jauge de 10 barres qu'on
   touche ou glisse, avec − et + ; un seul curseur pour TalkBack), **Musique** (Écouter, nom de
   la musique qui ouvre la feuille Musique, Changer pour Mon fichier).
5. **Tuiles** : Mes musiques, Composer une musique.

**Feuille Musique** : monte du bas (`motion-card`), une rangée de 56 px par musique (Korobeïniki,
Menuet, Mon fichier, la musique personnelle en jeu, Aucune), chacune avec sa coche et son
Écouter ; choisir enregistre sans fermer ; un seul aperçu à la fois.

## Jeu

- **Barre** : Pause (disque), « MODE CLASSIQUE » en `game-title`, record (devient l'autocollant
  NOUVEAU RECORD une fois battu, et suit le score).
- **Plateau** dans un cadre `board`, coins `radius-md` (téléphone) ou `radius-window` (tablette).
- **Colonne** : SUIVANTE en haut (près de l'apparition), SCORE avec une barre de record de 4 px,
  LIGNES, NIVEAU.
- **Touches** : flèches carrées (`key-arrow`), B et A en disques (`key-rotate`) **avec le
  pictogramme de rotation seul** ; leur nom reste pour TalkBack. B descend de 24 px. Option
  « Gaucher » dans les Paramètres : B et A à gauche, flèches à droite.
- Pas de pièce fantôme, de chute instantanée ni de réserve : ce sont des règles du Moderne.
- **Pause** : un rideau de cases vides masque le plateau et la pièce suivante
  (`motion-curtain`), puis la carte arrive : mascotte endormie, « Pause », score, niveau,
  lignes, Reprendre, Menu. Reprendre rouvre le rideau avant de relancer la partie.
- **Fin de partie** : la pile s'éteint rangée par rangée (`cell-dead`), puis la carte :
  « Partie terminée », NOUVEAU RECORD s'il y a lieu (avec des confettis aux couleurs des
  pièces), score final qui compte jusqu'à sa valeur, « Record » ou « Ancien record »,
  lignes, niveau, 4 lignes, durée, Rejouer, Menu.

## Retours de jeu

| Événement | Visuel | Effets réduits |
|---|---|---|
| Touche pressée | remplissage pressé, 2 px plus bas | idem : la descente n'est pas une animation, et c'est le seul repère de A tenue de jour (1,4:1 contre A au repos) |
| `PieceRotated` | le pictogramme de A ou B tourne de 90° (6 ticks) | rien |
| `PieceLocked` | les 4 cases posées s'éclairent (8 ticks) | rien |
| Effacement (`Phase.Clearing`) | rangées en `clear-glow`, cases qui éclatent par paires du centre vers les bords, calées sur le délai NES | rangées en `clear-glow` fixe |
| `LinesCleared` | rangées du dessus qui glissent, autocollant « +points » (`points-pop`, `motion-pop`), score qui compte | autocollant fixe, score immédiat |
| 4 lignes | en plus : reflet irisé qui fait le tour du cadre, autocollant « 4 LIGNES » (`four-lines-pop`) | cadre irisé fixe |
| `LevelUp` | l'encart NIVEAU bascule et s'encadre d'`accent` (1 s), annonce TalkBack | cadre `accent` sans bascule |
| `StackDanger` | cadre du plateau `danger` de 4 px, 5 rangées du haut en `danger-soft` qui respire | cadre et teinte fixes |
| Record battu | NOUVEAU RECORD rebondit à la place du record, barre de record irisée | échange sans rebond |
| `GameOver` | pile éteinte de bas en haut (2 ticks par rangée), carte, score qui compte | pile éteinte d'un coup, fondu |

Rien ne clignote plus de 3 fois par seconde ; 40 particules au plus. Les effets se dessinent
par-dessus le plateau et ne retardent jamais la partie. Les **effets sonores sont activés par
défaut** ; les vibrations restent désactivées par défaut.

## Paramètres

Une page courte : chaque réglage dit ce qu'il fait (sous-ligne) et se fait sentir dès qu'on
l'allume (le son ou la vibration joue une fois). Barre : disque Retour et « Paramètres » en
`screen-title`. Cartes par section (`panel`, `radius-window`), rangées de `setting-row`
(56 px, 64 sur tablette) séparées par 1 px `panel-border`, pictogramme de 24 dans un emplacement
de 40 à gauche ; toute la rangée est l'interrupteur.

1. **Affichage** : Effets réduits (« Moins de mouvements et de particules » ; allumé et bloqué,
   « Imposé par les réglages d'Android », quand Android supprime les animations, sans écraser le
   choix du joueur), Gaucher (sous-ligne qui dit la disposition, schéma des touches qui
   s'inverse), et la note « Le thème se choisit à l'accueil. ».
2. **Sons et vibrations** : Effets sonores (activés par défaut), Volume des effets (grisé quand
   les effets sont coupés), Volume de la musique, Vibrations (désactivées par défaut), Vibration
   des touches (désactivée par défaut). Sans vibreur : les deux vibrations grisées, « Cet appareil
   ne vibre pas ». Puis « Valable dès la partie suivante. », une seule fois.
3. **Record** : « Record du Classique » et le bouton Effacer (cadre et texte `danger`), qui
   demande confirmation (« Effacer le record ? », Effacer le record en `danger`, Garder le
   record) puis affiche « Record effacé ».

**Interrupteur** : piste de `switch-track` (52 × 32) en pilule, cadre de 2 px `button-outline`
dans les deux états. Éteint : piste `button`, pouce de 20 `label` à gauche. Allumé : piste
`accent` et `button-gloss`, pouce de 24 `accent-ink` à droite avec une coche `accent`. Pressé :
la rangée se remplit de `button-pressed`, le pouce s'allonge à 28. Désactivé : piste
`panel-inset`, cadre `panel-border`, pouce `label`.

**Curseur de volume** : piste de 6 px `panel-border`, remplie en `accent`, bouton de 28 px
(`button`, cadre `button-outline`, `button-gloss`), pas de 10 %, valeur en % à droite.

Une colonne debout (358 px, 520 sur tablette) ; deux colonnes couché (Affichage et Record à
gauche, Sons et vibrations à droite).

## Mise en page

| Format | Accueil | Jeu |
|---|---|---|
| Téléphone debout (390 × 844) | colonne, marges de 16 | barre, plateau (cases de 22) et colonne de 104 ; touches collées en bas ; marges de 16 |
| Téléphone couché (844 × 390) | logo, aperçu et tuiles à gauche ; barre, Jouer et réglages (rangées de 48) à droite | plateau au centre (cases de 17) ; pause, encarts et flèches à gauche ; record, suivante, B et A à droite |
| Tablette debout (820 × 1180) | colonne de 520, rangées de 64 | cases de 40, colonne de 200, touches de tablette |
| Tablette couchée (1180 × 820) | comme le téléphone couché, colonne de 460 | cases de 36, comme le téléphone couché |
| Fenêtre étroite (220 × 640) | — | inchangée (V20), touches `-compact` |

## Typographie

- **Unbounded** (`display`) : titres d'écran (`screen-title`), Jouer (`play-label`),
  autocollants (`points-pop`, `four-lines-pop`).
- **Manrope** (`body`) : tout le reste, chiffres à chasse fixe.
- **Bagel Fat One** (`logo`) : le seul logo BloX.

Les trois polices viennent de Google Fonts. Manrope n'a pas d'italique : les lignes hors gamme
de l'éditeur utilisent un italique imité.

## Formes et espacements

- `radius-md` (16 px) : encarts, flèches, tuiles, plateau sur téléphone.
- `radius-pill` : boutons d'écran, groupes de choix, puce Record, autocollants ; disques pour
  la pause, B, A, Écouter et Paramètres.
- `radius-window` (24 px) : carte des réglages, plateau sur tablette ; `radius-card` (28 px) :
  cartes Pause et Fin, feuille Musique.
- Espacements sur une grille de 4 : `space-1` (4), `space-2` (8), `space-3` (12), `space-4` (16) et `space-6` (24) ; pas de `space-5`.
  Marge des écrans : `space-4` sur téléphone. Seule exception : la case de l'aperçu du Serrurier posée sur
  l'anneau en colonne 0 ou 9 peut déborder jusqu'à `space-1` du bord, rognée là (P7.22), et jusqu'à 3 dp sur les
  écrans les plus étroits, pour que sa croix garde 10 dp (P7.23).

## Pictogrammes

Traits de 2,25 px aux bouts arrondis sur 24 × 24 (`icon`), en `currentColor` : lecture, arrêt,
liste, dé, plus, crayon, renommer, corbeille, note, note barrée (Aucune), étoile (record),
égaliseur (aperçu en cours), maison, rejouer, roue dentée, annuler, copier, gomme, placer,
rotation gauche et droite, étendre. Flèches des touches en triangles pleins. Jamais d'emoji. Les pictogrammes nouveaux de Couleurs v2, le logo et la mascotte sont relevés dans
[pictogrammes.md](pictogrammes.md) (tracés, bitmaps 8 × 8 d'Original, mesures).

## Logo

« Bl » et « X » en Bagel Fat One au dégradé `iris-1` → `iris-3`, la mascotte carrée à la
place du « o ». Dessiné dans la maquette (composant `LogoBloX`), pas encore en fichier.

## Accessibilité

| Paire | Jour | Nuit |
|---|---|---|
| `ink` sur `page` | 14,8:1 | 18,2:1 |
| `label` sur `page` / `panel` | 7,0:1 / 8,0:1 | 9,1:1 / 8,2:1 |
| `accent-ink` sur `accent` / `accent-pressed` | 6,5:1 / 4,6:1 | 16,9:1 / 9,5:1 |
| `button-outline` sur `page` | 3,7:1 | 4,1:1 |
| `melody` / `bass` sur `cell-ground` | 4,8:1 / 4,8:1 | 6,1:1 / 9,0:1 |
| `danger` sur `page` | 5,0:1 | 6,2:1 |

`panel-border` (1,4:1) reste décoratif : tout ce qui se touche est encadré en `button-outline`,
et les choix le sont par le cadre de leur groupe. Repères autres que la couleur, là où un aplat ne
suffit pas (P5.57) : le choix retenu porte une coche (3:1), l'interrupteur se lit à son cadre
`button-outline` (l'encre en Original), à la position et à la taille de son pouce, la touche tenue à
son décalage de 2 px, la partie tenue d'une note à son bord de 2 px de la couleur de la piste. Le danger se lit aussi à l'épaisseur du
cadre (4 px), pas à la seule couleur.

## Correspondance avec le code

Les jetons d'interface correspondent aux propriétés de `BloxPalette` (`core:designsystem`) par une table explicite (ADR 0019) : la plupart en kebab-case, avec des exceptions (`board` → `boardBackground`, `label-off` → `outOfScale`, pistes dans `TrackColors`). 1 px de ce système vaut 1 dp dans le code.
Les pièces (`piece-*`) vont dans `core:board` (`CellStyles`, ADR 0023), comme les cases du Serrurier (`lock-*` :
`lock-metal`, `lock-rivet`, `lock-hint`, `lock-hole-inset`, dans `LockColors`, Couleurs seulement, P7.10). À ajouter dans le
code pour ces retours : les volumes de la musique et des effets (`core:audio`), la vibration des
touches, la capacité « l'appareil vibre » (`Haptics`), l'effacement du record (`core:data`), l'événement `LevelUp(level)` dans le moteur, le record battu en jeu
(`GameUiState`), l'ancien record en fin de partie, un effet sonore « niveau », les préférences
« Effets réduits » et « Gaucher », et les effets sonores activés par défaut.
