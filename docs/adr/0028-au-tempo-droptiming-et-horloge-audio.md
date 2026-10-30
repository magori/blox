# 0028 — Au tempo : politique `DropTiming` et horloge audio comme source des ticks

- **Statut** : Acceptée ; amende [0012](0012-horloge-d-images-cote-ecran.md) et [0015](0015-un-moteur-des-capacites.md)
- **Date** : 2026-10-26

## Contexte

Au tempo ([règles](../regles-au-tempo.md)) fait tomber la pièce d'une ligne par croche d'un
accompagnement, à tempo fixe, et récompense une pose sur le temps. Deux choses manquent :

- **la cadence de chute** est aujourd'hui la gravité du Classique, lue dans `GravityTable` par le
  moteur selon le niveau ; il faut une autre règle, sans toucher au Classique ;
- **le temps** : le domaine compte en ticks de 1/60 s (ADR 0006), cadencés par l'horloge d'images
  de l'écran (ADR 0012). Or le son sort avec une latence de 100 à 350 ms, et l'horloge d'images
  dérive de l'horloge audio : une chute réglée sur les images ne tomberait pas sur les temps
  entendus.

Options envisagées pour la chute : une `GravityTable` d'Au tempo (mais la gravité du Classique
compte depuis l'apparition de la pièce, alors qu'Au tempo se cale sur la mesure) ; une condition
« mode tempo » dans le moteur (écartée par l'ADR 0015) ; une politique qui décide, à chaque tick,
si la pièce descend.

Options pour le temps : des millisecondes dans le domaine (écartée, ADR 0006) ; une boucle cadencée
par l'audio à côté de la boucle d'images (deux écrivains) ; le temps entendu fourni au même
`FrameTicks`.

## Décision

- **Politique `DropTiming` dans `Rules`** (dans la ligne de l'ADR 0015) : elle dit si la pièce
  descend d'une ligne par la gravité à ce tick.
  - `ClassicDropTiming` délègue à `GravityTable` ; il est **extrait d'abord, en pur
    refactoring**, sous les tests du Classique, qui restent verts.
  - `TempoDropTiming` fait descendre la pièce d'une ligne par croche, calée sur
    `GameState.tickCount` ; une croche dure un nombre entier de ticks (`Meter`, paquet
    `…core.game.tempo`).
  - `TempoRules = ClassicRules.copy(dropTiming = TempoDropTiming(meter))`.
- **Les bonus restent hors du moteur** : `TempoScore`, valeur immuable du paquet `tempo`, est
  nourrie des événements du moteur ; on ne touche pas à `ScoringRule` (P6.13, gelée).
- **Horloge audio comme source des ticks** (amende l'ADR 0012) : pour Au tempo,
  `Playback.heardNanos()` (`AudioTrack.getTimestamp`, latence comprise) remplace le temps d'image
  en entrée du même `FrameTicks`, **sans plafond** de ticks par image. L'écran cadence toujours
  `onFrame` à chaque image (`withFrameNanos`). C'est le ViewModel d'Au tempo, qui détient le
  `Playback`, qui lit `heardNanos()` à chaque image au lieu du temps d'image reçu : une source de
  temps injectable, doublée par le faux lecteur. Une seule boucle, un seul écrivain du
  `GameState`. Si la musique est en échec, `FrameTicks` revient à l'horloge d'images, sans saut.
- **Synchronisation** : `heardNanos()` est monotone depuis `play` et ne recule jamais, même quand
  l'accompagnement boucle ; le tick 0 coïncide avec le premier échantillon entendu. À la reprise,
  l'accompagnement repart à la position exacte `tickCount mod phrase`.
- Le danger ne change pas le tempo : la variante joue avec une `MusicDirection` sans accélération
  (`SteadyMusicDirection`).
- Le tempo est **fixe par partie** ; le faire monter avec le niveau demandera un nouvel ADR.

## Conséquences

- Le moteur est modifié une fois de plus (lecture de la gravité par `DropTiming`) ; le Classique
  doit rester identique, ce que vérifient cinq parties de référence, à graines fixes.
- **Chevauchement avec P6.11** (gravité de plus d'une ligne par tick, gelée avec P6) : `DropTiming`
  ne rend pour l'instant qu'« une ligne ou rien ». Quand P6.11 reprendra, c'est `DropTiming` qui
  devra rendre un nombre de lignes, et non plus `GravityTable` seule.
- L'ADR 0012 ne vaut plus tout à fait : le ViewModel d'Au tempo **lit une horloge**, l'horloge
  audio, au lieu du temps d'image que l'écran lui passe. Sans plafond, une longue coupure audio peut faire rattraper beaucoup de ticks
  d'un coup : la pause automatique (`ON_PAUSE`) et le repli doivent l'éviter.
- La précision de `getTimestamp` varie selon les téléphones et les sorties (Bluetooth) : une
  mesure sur téléphone (P11.2) précède tout le reste, et fixe la fenêtre « sur le temps ».
- Le domaine reste sans temps réel : `Meter`, `TempoDropTiming` et `TempoScore` se testent tick
  par tick, sur la JVM.

## Note (2026-10-30, P11.4)

`DropTiming` est extrait : `Rules.dropTiming` remplace `Rules.gravityTable`, et le moteur demande
`dropsAfter(ticksSinceDrop: Ticks, tickCount: Int, level: Level)`. `tickCount` est passé dès
maintenant, pour que `TempoDropTiming` (P11.6) ne modifie pas le moteur une seconde fois ;
`ClassicDropTiming` l'ignore. La non-régression ne repose pas sur des parties tirées au hasard à
chaque exécution, mais sur cinq parties de référence, à graines fixes, enregistrées tick par tick
avant l'extraction (`ReferenceGamesTest`, `core/game/src/test/resources/reference-games`).

## Note (2026-10-30, P11.6)

Le but d'une seule modification du moteur n'est pas tenu : `TempoDropTiming` en demande une seconde.
`dropsAfter` reçoit en plus `softDropping: Boolean`, que le moteur tire de la phase `Falling`
(`softDrop != null`) : ▼ tenu, entre deux pas. La raison : au Classique, chaque pas de la descente
rapide remet le compte de la gravité à zéro, si bien que la gravité ne s'en mêle qu'aux niveaux 19 et
plus ; une croche, calée sur `tickCount`, ignore ce compte et ferait descendre la pièce d'une ligne de
plus entre deux pas. La descente rapide d'Au tempo ne serait plus celle du Classique, et une croche
pourrait verrouiller par la gravité une pièce sous ▼ tenu, ce qui brouillerait l'information
« verrouillage amené par la descente rapide » de `TempoScore` (P11.7). Le paramètre est un fait
neutre, sans condition propre à Au tempo dans le moteur (ADR 0015) ; `ClassicDropTiming` l'ignore, et
les parties de référence restent identiques. Pour **P6.11** : quand `DropTiming` rendra un nombre de
lignes, il recevra aussi `softDropping`.
