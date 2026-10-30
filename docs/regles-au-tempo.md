# Règles d'Au tempo

Au tempo est une **variante du Classique** où la pièce tombe **en rythme** : elle descend d'une
ligne à chaque croche d'un accompagnement (métronome et basse), à un tempo fixe pour toute la
partie. Poser une pièce **sur le temps** rapporte un bonus ; effacer des lignes à la fin de la
phrase musicale, une **cadence**, en rapporte un autre. Tout le reste suit les règles du
[Classique](regles-mode-classique.md).

Origine : cadrage « quatre modes musicaux » des agents `architecte` et `designer-jeu`
(2026-10-26), décisions du développeur le 2026-10-26. Architecture :
[ADR 0026](adr/0026-jeux-musicaux-variantes-du-classique.md) (une variante de `feature:game`) et
[ADR 0028](adr/0028-au-tempo-droptiming-et-horloge-audio.md) (politique `DropTiming`, horloge
audio comme source des ticks).

> Traçabilité : seul le découpage des ticks (`Meter`, P11.5) est implémenté ; les autres règles
> (phase P11 de [SUIVI.md](../SUIVI.md)) recevront leur ligne **Tests :**
> `ClasseDeTest › nom du test` et, si elles se voient à l'écran, **Vues :** `V…`, à mesure
> qu'elles seront codées.

## Vocabulaire

| Terme | Code prévu | Sens |
|---|---|---|
| Croche, noire, mesure | `Meter` (`core:game`, paquet `…core.game.tempo`, P11.5) | Le découpage des ticks : une croche dure un nombre entier de ticks, une noire deux croches, une mesure 4 noires, une phrase 4 mesures. |
| Cadence de chute | `DropTiming` (`Rules`) | Quand la pièce descend d'une ligne par la gravité. `ClassicDropTiming` délègue à `GravityTable`, `TempoDropTiming` suit les croches. |
| Sur le temps | `Meter.isOnBeat` (fenêtre `onBeatWindow`) | Une pose qui tombe à *N* ticks d'une noire au plus. |
| Cadence | `CadenceCleared` | Des lignes effacées par une pose dans la 4ᵉ mesure de la phrase. |
| Bonus | `TempoScore` | Les points d'Au tempo, hors du moteur, ajoutés à ceux du moteur. |

## Tempo et mesure

- **Un tempo fixe par partie** (décision du développeur, 2026-10-26). Faire monter le tempo avec
  le niveau viendra plus tard, par un ADR.
- Le domaine ne connaît que les ticks (1 tick = 1/60 s, [ADR 0006](adr/0006-boucle-de-jeu-a-pas-fixe.md)) :
  **une croche dure un nombre entier de ticks**, `ticksPerEighth`. D'où les tempos possibles
  (noires par minute = 1 800 / `ticksPerEighth`, `Meter.beatsPerMinute`) :

  | Ticks par croche | Tempo |
  |---|---|
  | 18 | 100 temps/min |
  | 16 | 112,5 temps/min |
  | 15 | 120 temps/min |
  | 12 | 150 temps/min |
  | 10 | 180 temps/min |

  **Tests :** `MeterTest › an eighth lasts a whole number of ticks` (une croche commence tous les
  `ticksPerEighth` ticks depuis le tick 0, `Meter.startsAnEighth`), `MeterTest › the tempo is 1800 over the ticks
  per eighth`.

- **Tempo de la v1 : 120 temps/min (15 ticks par croche)** **[à vérifier]** en jouant. Les autres
  tempos de la table sont **proposés** pour plus tard **[à vérifier]** : les offrir demandera un
  choix avant la partie et des records par tempo (point ouvert).
- **Mesure à 4 temps** ; une **phrase** compte 4 mesures. `Meter` dit, pour un `tickCount` donné,
  si une croche commence à ce tick (`startsAnEighth`), le temps (1 à 4) et la mesure (1 à 4) : le tick 0 est le premier temps de la
  première mesure. Une noire dure `2 × ticksPerEighth`, une mesure `8 × ticksPerEighth`, une
  phrase `32 × ticksPerEighth` (`ticksPerQuarter`, `ticksPerBar`, `ticksPerPhrase`) ; `beatOf(tickCount)` et
  `barOf(tickCount)` rendent le temps et la mesure de la phrase, de 1 à 4, et repartent à 1 à chaque phrase. Un
  `tickCount` négatif est refusé.
  **Tests :** `MeterTest › tick 0 is beat 1 of bar 1`, `MeterTest › a quarter note lasts 2 eighths`,
  `MeterTest › a bar lasts 8 eighths and a phrase 4 bars`, `MeterTest › the divisions follow the ticks per eighth`,
  `MeterTest › a negative tick count is refused`.
- **Sur le temps** : un tick est sur le temps s'il tombe à `onBeatWindow` ticks au plus d'une noire, avant ou
  après, bornes comprises, à n'importe quel temps de la mesure (`Meter.isOnBeat`). La fenêtre est un paramètre de
  `Meter` ; sa valeur par défaut, `Meter.PROVISIONAL_ON_BEAT_WINDOW` = **5 ticks**, est provisoire
  **[à vérifier]** (5 ou 6, fixée par la mesure sur téléphone de P11.2, voir *Bonus*). La fenêtre est plus
  courte qu'une croche (`onBeatWindow < ticksPerEighth`, sinon `Meter` est refusé), pour que le contretemps,
  entre deux noires, ne soit jamais sur le temps ; et comme toute durée en `Ticks`, elle vaut au moins 1 tick.
  **Tests :** `MeterTest › a tick within N ticks of a quarter note is on the beat`,
  `MeterTest › a wider window takes in more ticks`, `MeterTest › the window on the beat is 5 ticks until it is
  measured`, `MeterTest › a window reaching the off-beat is refused`.
- Le repère est `GameState.tickCount`, qui n'avance ni en pause ni après la fin de partie : la
  musique s'arrête et reprend avec lui.

## Chute en rythme : `TempoDropTiming`

- La gravité ne dépend plus du niveau : la pièce descend d'**une ligne par croche**, aux ticks où
  `tickCount` est un multiple de `ticksPerEighth` (**calée sur la mesure**, pas sur l'apparition
  de la pièce). Une pièce qui apparaît entre deux croches descend à la croche suivante.
- La **descente rapide** (▼ tenue) reste celle du Classique (une ligne toutes les 2 ticks) : c'est
  par elle qu'on amène une pièce sur le temps.
- Le verrouillage, l'effacement, le délai d'apparition, la rotation, le déplacement et le danger
  sont ceux du Classique.
- `TempoRules = ClassicRules.copy(dropTiming = TempoDropTiming(meter))`.
- **Non-régression** : `ClassicDropTiming` délègue à `GravityTable` et rend exactement les mêmes
  parties que le moteur d'avant ; il est extrait d'abord, en pur refactoring, sous les tests du
  Classique (ADR 0015, ADR 0028). Une propriété le vérifie sur des parties tirées au hasard.

## Niveaux et points du moteur

- **Niveau de départ : 0** **[à vérifier]** (Au tempo n'a pas d'accueil ; le niveau ne règle plus
  la vitesse, seulement les points).
- Les niveaux montent comme au Classique (`ClassicLevelProgression`) ; les points du moteur
  (`ClassicScoringRule`) sont inchangés. On ne touche pas à `ScoringRule` (P6.13, gelée).

## Bonus : `TempoScore`

Les bonus restent **hors du moteur** : `TempoScore` est une valeur immuable nourrie des
événements de chaque tick (`PieceLocked`, `LinesCleared`) et de `Meter`. Elle émet ses propres
événements (`OnBeat`, `CadenceCleared`), que l'écran traduit en retours. **Le total affiché vaut
les points du moteur plus les bonus** ; c'est lui qui entre dans les records d'Au tempo.

- **Sur le temps** : une pose dont le tick de verrouillage tombe à **±N ticks d'une noire**
  (n'importe quel temps de la mesure) et qui a été **amenée par la descente rapide tenue** (▼
  tenue jusqu'au verrouillage). Une pièce laissée tomber par la seule gravité ne compte pas : elle
  tomberait toujours sur une croche, sans effort.
  - **N = 5 ticks** **[à vérifier]** (5 ou 6, environ 83 à 100 ms de chaque côté, à mesurer sur
    téléphone en P11.2, latence audio comprise). C'est la fenêtre de `Meter.isOnBeat`, passée en
    paramètre, 5 par défaut en attendant la mesure.
  - **Bonus : + 80 points** par pose sur le temps **[à vérifier]**, quel que soit le niveau.
  - Événement `OnBeat(bonus)`. Le moteur doit dire qu'un verrouillage a été amené par la descente
    rapide : information à ajouter à `PieceLocked`, ou à lire dans l'état, à trancher en P11.7.
- **Cadence** : des lignes effacées par une pose verrouillée dans la **4ᵉ mesure** de la phrase.
  `LinesCleared` n'arrive qu'à la fin de l'animation d'effacement (`ClassicLineClearDelay`),
  souvent dans la mesure suivante, et ne porte pas de points. `TempoScore` **retient donc le tick
  de verrouillage** du `PieceLocked` et applique la cadence au `LinesCleared` qui le suit. Les
  « points du moteur de cet effacement » se lisent par l'écart des `Points` de l'état avant et
  après l'effacement.
  Le prototype doublait sans condition les lignes effacées en mesure 4 ; la conception proposait
  × 1,5 ou une condition. **Tranché** : × 2 (bonus égal aux points du moteur de cet effacement),
  **à condition que la pose soit aussi sur le temps** ; sinon, aucun bonus de cadence
  **[à vérifier]** en jouant (alternative : × 1,5 sans condition). Événement
  `CadenceCleared(bonus)`.
  **Point ouvert (P11.7)** : une pose sur le temps juste avant le temps 1 (à 475 ticks, à 120 temps/min) est
  sur le temps de la noire 480, premier temps de la mesure 1, alors que `Meter.barOf(475)` rend 4. La mesure
  d'une pose sur le temps doit-elle être celle de la noire la plus proche ? Cas proposé : `a piece locked 5
  ticks before bar 1 is not in bar 4`.
- Compteur **sur le temps / pièces** : le nombre de poses sur le temps et le nombre de pièces
  verrouillées (« 12 / 40 »), montrés à l'écran et à la fin de partie.

## Temps réel : l'horloge audio

- Pour que la chute colle à ce qu'on **entend**, les ticks d'Au tempo sont cadencés par
  l'**horloge audio**, et non par l'horloge d'images : `Playback.heardNanos()` (le temps de la
  musique effectivement entendu, `AudioTrack.getTimestamp`, latence comprise) nourrit le même
  `FrameTicks`, **sans plafond** de ticks par image, comme la tête de lecture de l'éditeur
  (amendement de l'[ADR 0012](adr/0012-horloge-d-images-cote-ecran.md) par l'ADR 0028).
- L'écran continue de cadencer `onFrame` à chaque image (`withFrameNanos`) : il n'y a pas de
  seconde boucle. Mais c'est le **ViewModel** d'Au tempo, qui détient le `Playback`, qui lit
  `heardNanos()` à chaque image, au lieu du temps d'image reçu. C'est une source de temps
  injectable, doublée par le faux lecteur dans les tests. Le domaine reste en ticks.
- `heardNanos()` est un temps entendu **monotone depuis `play`** : il ne recule jamais, ni quand
  l'accompagnement boucle, ni à la reprise (`FrameTicks` ignore un temps qui recule, puis recompte
  le temps déjà compté). Le **tick 0** coïncide avec le premier échantillon entendu de
  l'accompagnement.
- **Repli** : si la musique est en échec (sortie audio perdue, séquenceur mort, focus refusé), les
  ticks reviennent à l'horloge d'images, sans saut, et la partie continue en silence ; les bonus se
  comptent toujours sur les ticks.
- Pause, reprise, fin de partie : comme au Classique. À la reprise, l'accompagnement repart à la
  **position exacte** `tickCount mod phrase`, et non au début de la mesure : `tickCount` ne
  reculant pas, cela décalerait la musique jusqu'à une mesure entière. Ticks et accompagnement
  restent en phase.

## Accompagnement

- Un **métronome** (un clic par noire, plus marqué sur le temps 1) et une **basse** sur la suite
  **I – V – vi – IV** de Do majeur, une mesure par accord, en boucle sur la phrase.
- La partition est écrite à 100 temps/min (18 ticks par croche) et jouée avec le facteur de tempo
  `18 / ticksPerEighth` (× 1,2 à 120 temps/min).
- **Interrupteur Métronome** dans la carte Pause : coupe le clic, garde la basse ; activé par
  défaut **[à vérifier]** ; enregistré dans les préférences.
- La musique choisie à l'accueil ne joue pas : l'accompagnement est la musique d'Au tempo. Le
  volume de la musique s'y applique.
- **Le danger ne change pas le tempo** : le tempo fixe le `Meter` (`ticksPerEighth`, facteur
  `18 / ticksPerEighth`), et un × 1,25 décalerait la chute des temps entendus. La variante `TEMPO`
  joue donc avec une `MusicDirection` **sans accélération**, `SteadyMusicDirection` (ni × 1,25 au
  danger, ni montée au `LevelUp`), à côté de celle du Classique
  ([ADR 0026](adr/0026-jeux-musicaux-variantes-du-classique.md)).

## Records

- Un **fichier DataStore à part** (une seconde instance de `DataStoreHighScoreRepository`) :
  les records d'Au tempo ne se mêlent jamais à ceux du Classique.
- Le score enregistré est le total (moteur + bonus).
- **Sauvegarde** : le fichier de records d'Au tempo entre dans la liste d'inclusions de la
  sauvegarde Android (vérifiée par `BackupRulesTest`), comme celui du Classique **[à vérifier]**
  ([ADR 0026](adr/0026-jeux-musicaux-variantes-du-classique.md)).
- La préférence **Métronome** ajoute un champ à `UserPreferences`, dans `settings.preferences_pb`,
  déjà sauvegardé (P11.9).

## Écran

**Thème : Couleurs v2 seulement** (jour et nuit selon le téléphone), comme le Serrurier
(décision du développeur, 2026-10-26) : la partie Au tempo est toujours dessinée en Couleurs,
quel que soit le thème choisi pour le Classique ; barres système selon cette variante. Vue
**V41** de la [maquette](maquette.md) (à dessiner).

L'écran de jeu v2 (V28), avec :

- le titre « AU TEMPO » et son propre record ;
- un encart **TEMPO** sous NIVEAU : 4 disques des temps (le temps 1 plus grand et cerné `ink`),
  « MESURE 2/4 », le tempo (« 120 bpm ») ;
- un encart **SUR LE TEMPS** « 12 / 40 » ;
- les touches de V28 ;
- un interrupteur **Métronome** dans la carte Pause (V29).

**Retours**, chacun avec sa version Effets réduits ([ADR 0024](adr/0024-retours-de-jeu-et-effets-reduits.md)),
cadencés en ticks :

- à chaque temps, le disque du temps se remplit ; le temps 1 donne un reflet `accent` au cadre du
  plateau (3 reflets par seconde au plus : au-delà de 180 temps/min, un sur deux) ;
- **pose sur le temps** : autocollant « SUR LE TEMPS +80 », note aiguë (effet sonore) ;
- **cadence** : autocollant « CADENCE × 2 », bande irisée ;
- en Effets réduits : disques et autocollants sans mouvement ni reflet.

**Fin de partie** (V30) : total, dont les bonus, et « sur le temps 12 / 40 ».

**Cartouche** « Au tempo » sur le choix du jeu (V36) : elle ouvre directement une partie, au
tempo de la v1.

## Points ouverts

- Fenêtre « sur le temps », bonus, condition de la cadence, tempo de la v1, niveau de départ
  (ci-dessus, **[à vérifier]**).
- Choisir son tempo avant la partie, et des records par tempo.
- Monter le tempo avec le niveau : plus tard, par un ADR.
- Sauvegarde du fichier de records d'Au tempo (ci-dessus, **[à vérifier]**).
- Chevauchement de `DropTiming` avec la gravité de plus d'une ligne par tick (P6.11, gelée) : voir
  l'ADR 0028.
