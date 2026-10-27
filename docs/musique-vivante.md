# Musique vivante

La musique vivante est un **choix de musique du mode Classique**, « Vivante », à côté de
*Korobeïniki*, *Menuet*, *Mon fichier*, de la musique en jeu et d'*Aucune*. Ce n'est pas un jeu :
elle n'a ni cartouche ni écran à elle (décision du développeur, 2026-10-26). La partie suit les
règles du Classique (`ClassicRules`) sans aucun changement ; seule la musique change : composée
pour la partie par le [générateur](generateur-musique.md), elle passe d'une ambiance calme à une
ambiance tendue quand la pile monte, et accélère avec les niveaux.

Origine : cadrage « quatre modes musicaux » des agents `architecte` et `designer-jeu`
(2026-10-26), décisions du développeur le 2026-10-26. Architecture :
[ADR 0026](adr/0026-jeux-musicaux-variantes-du-classique.md). Le reste de la musique (lecture en
boucle, focus audio, accélération du danger) est décrit dans [musique.md](musique.md).

> Traçabilité : les règles implémentées portent leur ligne **Tests :** `ClasseDeTest › nom du test`
> et, si elles se voient à l'écran, **Vues :** `V…` ; les autres attendent leur tâche de la phase P8
> de [SUIVI.md](../SUIVI.md).

## Vocabulaire

| Terme | Code prévu | Sens |
|---|---|---|
| Musique vivante | `MusicChoice.LIVE` (`core:data`) | Le choix « Vivante » de la feuille Musique. |
| Les deux ambiances | `LiveMusic` (`core:music`) | Une musique calme et une musique tendue, composées ensemble pour une partie. |
| Direction musicale | `MusicDirection` (`feature:game`) | La traduction « événements de la partie → ordres au lecteur » (tempo, ambiance). |
| Changer de partition | `Player.switchTo(score)` (`core:audio`) | Passer à une autre partition à la mesure suivante, au même rang. |

## Composer les deux ambiances

- `LiveMusic.compose(seed)` rend **deux musiques** composées par `MusicGenerator.generate` :
  - la musique **calme**, de catégorie `Category.CALM` (pentatonique majeure, `I–vi–IV–V` ou
    `I–IV`, 70 à 95 temps/min) ;
  - la musique **tendue**, de catégorie `Category.EPIC` (mineur naturel, `i–VI–III–VII` ou
    `i–VII–VI–VII`, 110 à 140 temps/min).
- Les deux ont **la même longueur** : le même nombre de mesures (8 mesures de 4 temps, la forme
  du générateur). C'est ce qui permet de passer de l'une à l'autre au même rang de mesure.
- Les deux ont **la même tonique**. `Form.compose` tire d'ordinaire la tonique dans
  `Category.TONICS` (Do, Ré, Fa, Sol, La) pour chaque musique : des graines séparées donneraient
  souvent deux tonalités. La tendue est donc composée avec la tonique tirée pour la calme, ce qui
  demande une nouvelle capacité de `MusicGenerator`, la **tonique imposée** (P8.9,
  [generateur-musique.md](generateur-musique.md)). Les deux ambiances gardent leurs tempos propres.
  `LiveMusic` vérifie cet invariant à sa construction ; les catégories et la longueur, elles, sont
  celles que donne `compose`.
- Les deux partitions portent des identités fixes, `LiveMusic.CALM_ID` et `LiveMusic.TENSE_ID`
  (`ScoreId.Personal("live-calm")` et `("live-tense")`), qui ne nomment aucune musique de la
  bibliothèque. Un nouveau cas de `ScoreId` a été écarté : il aurait cassé les `when` exhaustifs
  des autres modules.
- C'est une **fonction pure de la graine** : la même graine rend les deux mêmes musiques. Chaque
  musique est composée avec sa propre graine, tirée de la graine de la partie (`Random(seed)`, la
  calme d'abord), si bien que la calme ne dépend pas de la tendue.
  **Tests :** `LiveMusicTest › a live music is a calm music and a tense one`,
  `LiveMusicTest › the calm and the tense music have the same number of bars`,
  `LiveMusicTest › the calm and the tense music are known by their ids, the same from one game to the next`,
  `LiveMusicTest › the calm and the tense music share their tonic`,
  `LiveMusicTest › two musics of different tonics are not a live music`,
  `LiveMusicTest › the same seed gives the same two musics`,
  `LiveMusicTest › the calm music does not depend on the tense one`.
- Comme toute musique du générateur, la suite n'est pas un
  contrat entre versions de Kotlin ([ADR 0014](adr/0014-tirage-seul-etat-modifiable.md)) : seule
  la suite des pièces d'une graine en est un (Graine du jour, [graine-du-jour.md](graine-du-jour.md)).
- La graine de la musique est celle de la partie (`GameRoute`) : une nouvelle partie et Rejouer
  donnent une nouvelle musique ; la Graine du jour donne la même musique à tout le monde, ce
  jour-là, sur une même version.
- On ne reprend pas le `composeLoop` du prototype de la maquette : le générateur existant suffit.
- La composition coûte quelques dizaines de millisecondes (voir `MusicGenerator.generate`) : elle
  est faite hors du fil principal, avant la première pièce. Si elle n'est pas prête, la partie
  commence en silence et la musique part dès qu'elle l'est. **[à vérifier]** sur un téléphone
  d'entrée de gamme : faut-il la composer dès le choix « Vivante », pendant l'accueil ?

## Diriger la musique pendant la partie

La traduction des événements en ordres au lecteur sort de `feature:game` dans une stratégie,
`MusicDirection`, choisie au début de la partie selon la musique :

- **`MusicDirection`** (`feature:game/music`, interne) a deux entrées : `onStart(player)`, juste
  après le départ de la musique, et `onEvents(events, player)`, à chaque tick, avec les événements
  du moteur. Le lecteur est passé en paramètre : une direction sans état peut être un objet, une
  direction à état (Vivante) une instance par partie. `onStart` peut venir après des `onEvents`
  (musique lancée en retard, reprise sans `Playback`) : la direction répond alors de son état.
- **`ClassicMusicDirection`** (objet sans état, ex-`MusicTempo.kt`, P8.5) : tempo × 1,25 en danger,
  retour à × 1,0 à la sortie du danger ([musique.md](musique.md#accélération)) ; le tempo du danger
  est préparé dès `onStart`. Pur refactoring, sous les tests existants du tempo ; `GameViewModel`
  passe par elle.
  **Tests :** `ClassicMusicDirectionTest › the music speeds up by a quarter while the stack is in danger, and goes back to its tempo after`,
  `ClassicMusicDirectionTest › the danger tempo is prepared as soon as the music starts`,
  `ClassicMusicDirectionTest › each change of danger of the game gives its tempo to the player, in order, and nothing else does`.
- **`LiveMusicDirection`** ajoute deux règles à celles du Classique :
  - **ambiance** : la partie commence sur la musique calme ; `StackDanger(on)` passe à la tendue,
    `StackDanger(off)` revient à la calme. Les seuils sont ceux du moteur (la pile entre en danger
    à 15 lignes, en sort à 12 ou moins, `ClassicStackDangerRule`). Le passage se fait par
    `Player.switchTo` : la nouvelle partition commence **à la mesure suivante, au même rang**
    (après la mesure 3 de la calme vient la mesure 4 de la tendue) ;
  - **accélération** : chaque `LevelUp` règle le facteur de tempo (`setTempoFactor`) à
    **min(1 + 0,02 × niveaux gagnés, 1,30)**, les niveaux gagnés comptant depuis le niveau de
    départ (accroissement additif, non composé) ; valeurs **[à vérifier]** en jouant. Ce facteur
    se **multiplie** au × 1,25 du danger.
- **Rendu** : `prepareTempo` ne prépare que la partition jouée en dernier. L'entrée en danger fait
  à la fois `switchTo(tendue)` et × 1,25 : la tendue est rendue au seul tempo du danger, pendant
  l'attente de la barre de mesure, puis reste en cache (voir
  [Changer de partition à la mesure](#changer-de-partition-à-la-mesure)) ; seule la première entrée
  en danger d'une partie la rend. Chaque `LevelUp` déclenche un nouveau rendu ; son coût est
  **à mesurer** sur téléphone (P8.6).
- Chaque ambiance garde son propre tempo (celui de sa forme) ; le facteur s'applique aux deux.
- La pause, la reprise, la fin de partie, Rejouer et la perte du focus se passent comme pour
  toute partition ([musique.md](musique.md)) : la reprise repart dans l'ambiance du moment, au
  tempo du moment.
- `GameOver` arrête la musique, sans retour à la calme. `LinesCleared` ne change rien de plus que
  ce qu'il change déjà (le danger peut finir avec lui).

## Changer de partition à la mesure

Nouvelle capacité du lecteur, `Player.switchTo(score)`, partagée avec le
[Jardin musical](jardin-musical.md) :

- la partition en cours joue jusqu'à la fin de sa mesure ; la nouvelle commence à la mesure de
  **même rang** (modulo sa longueur), sur le premier temps ; un `switchTo` juste après `play` joue
  la nouvelle partition depuis son début, sans rendre l'autre ;
- le facteur de tempo en cours est gardé ;
- le jeton de lecture (`Playback`) reste le même : une partie n'arrête toujours que sa propre
  musique ;
- un second `switchTo` avant la barre de mesure remplace le premier (seul le dernier compte) et
  abandonne le rendu demandé pour lui ;
- si la musique est arrêtée ou morte, `switchTo` ne fait que retenir la partition ; la reprise la
  joue depuis le **premier temps de la mesure de même rang** (modulo sa longueur) que celle où la
  musique s'est arrêtée ; avant tout `play`, `switchTo` ne fait rien ;
- `FocusedMusicPlayer` : une partition qui attend le focus, ou à qui il est refusé, garde la
  partition demandée pour elle-même et la joue à son lancement ; elle ne touche pas à la lecture
  arrêtée d'un autre propriétaire ;
- **rendu et cache** : la partition demandée est rendue en arrière-plan au tempo demandé du moment
  (à l'entrée en danger, `setTempoFactor(1,25)` et `switchTo(tendue)` arrivent ensemble : la
  tendue est rendue au seul tempo du danger, jamais à × 1,0 d'abord), pendant l'attente de la
  barre de mesure. Le lecteur garde ensuite un cache par partition, pour celles que la lecture a
  jouées ou demandées : revenir à la calme, ou entrer de nouveau en danger, même après une pause,
  ne rend rien. Seule la **première** entrée en danger d'une partie rend donc la tendue
  (P8.10 : la préparer d'avance, à décider avec la mesure de P8.6) ;
- la capacité existe à chaque étage du lecteur : le contrat `MusicPlayerContract` et le faux
  lecteur, mais aussi `ChiptuneSequencer`, `FocusedMusicPlayer` et `MusicSwitch` ;
- elle est **réservée aux partitions** : `MusicPlayer = Player<Music>`, et pour un `MusicFile`
  (« Mon fichier ») `switchTo` est sans effet ; la partition de secours d'un fichier illisible,
  elle, change.

**Tests :** `ChiptuneSequencerSwitchTest › a score switched to starts on the next bar, at the same rank`,
`ChiptuneSequencerSwitchTest › a score switched to past its length starts on the bar of the same rank modulo its length`,
`ChiptuneSequencerSwitchTest › a switch while stopped resumes on the first beat of the bar of the same rank`,
`ChiptuneSequencerSwitchTest › switching back to the score switched from renders nothing`,
`ChiptuneSequencerSwitchTest › a second switch before the bar line gives up the rendering of the first`,
`ChiptuneSequencerSwitchTest › a score switched to as the danger begins is rendered at the danger tempo, not at the tempo before`,
`ChiptuneSequencerSwitchTest › a playback resumed keeps the score switched from ready`,
`ChiptuneSequencerSwitchTest › a switch right after a play plays the score switched to from its start, without rendering the other`,
`MusicPlayerContract › a switch keeps the tempo factor and the playback`,
`MusicPlayerContract › a second switch before the bar line replaces the first`,
`MusicPlayerContract › a switch while the music is stopped is played at the resume`,
`MusicPlayerContract › a switch while the music stopped by itself is played at the resume`,
`MusicPlayerContract › a switch before anything is played plays nothing`,
`MusicPlayerContract › the tempos prepared are ready for the score switched to`,
`FocusedMusicPlayerTest › a switch while the focus is lost for a while is played when the focus comes back`,
`FocusedMusicPlayerTest › a switch while a score waits for the focus to play it is played with the focus`,
`FocusedMusicPlayerTest › a switch while the focus is refused is played once resumed with the focus`,
`FocusedMusicPlayerTest › a new play forgets the switch asked for the score refused the focus`,
`FocusedMusicPlayerTest › a switch while the focus is refused leaves the score stopped before untouched`,
`FocusedMusicPlayerTest › a refused score resumed after another play is not switched to the score asked for the other`,
`MusicSwitchTest › a switch while a music file plays changes nothing, the switch is reserved to the scores`,
`FakeMusicPlayerTest › a switch is noted by the identifier of the score switched to`,
`FakeMusicPlayerTest › a switch while a music file plays changes nothing`.

## Choix « Vivante »

- `MusicChoice.LIVE` s'ajoute aux choix enregistrés (`core:data`) ; `scoreOf` (dans `app`) ne
  rend pas de partition fixe pour lui, mais la demande de composer les deux ambiances.
- « Vivante » prime comme les autres choix de l'accueil : la musique en jeu de « Mes musiques »
  garde sa priorité ([musique.md](musique.md)), et choisir Vivante la retire des parties, comme
  choisir Korobeïniki.
- **Écouter** sous Vivante joue la musique calme d'une graine tirée au hasard **[à vérifier]**
  (alternative : ne rien proposer à l'écoute).
- Le réglage des effets sonores, le volume de la musique et les Effets réduits ne changent pas.

## Écran

Pas d'écran à part ; thème : celui que le joueur a choisi pour le Classique (ce n'est pas un écran
propre au mode).

- **Feuille Musique** (V27, P5.76) : un sixième choix, « Vivante », avec sa coche et Écouter,
  sous-ligne « Calme, puis tendue quand la pile monte ». Vue **V37** (à dessiner).
- **Rappel sous Jouer** : « Classique · Niveau 05 · Vivante ».
- **En jeu** (V28) : rien ne change. On retire du prototype les étiquettes Calme / Tendu et la
  jauge « Pile » : le danger se voit déjà sur le plateau et s'entend.
- **Carte Pause** (V29) : la musique y est nommée « Vivante · calme » ou « Vivante · tendue »,
  selon l'ambiance au moment de la pause. Vue **V37**.

Événements utilisés, tous déjà émis par le moteur : `StackDanger`, `LevelUp`, `LinesCleared`,
`GameOver`.

## Points ouverts

- La valeur de l'accélération par niveau et son plafond (ci-dessus, **[à vérifier]**).
- Passer à la mesure suivante peut tarder jusqu'à une mesure entière (jusqu'à 3,4 s à
  70 temps/min, 2,5 s à 95) :
  à juger en jouant ; un passage au temps suivant demanderait une autre capacité du lecteur.
- Une troisième ambiance (fin de partie proche) est hors périmètre.
