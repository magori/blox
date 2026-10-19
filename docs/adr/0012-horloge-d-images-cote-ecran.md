# 0012 — L'horloge d'images appartient à l'écran, le ViewModel ne fait qu'avancer

- **Statut** : Acceptée ; remplace le point « coroutine dans le scope du ViewModel » de l'[ADR 0006](0006-boucle-de-jeu-a-pas-fixe.md)
- **Date** : 2026-10-18

## Contexte

L'ADR 0006 fixe le pas de simulation (1 tick = 1/60 s) et décrit la boucle comme « une coroutine
dans le scope du ViewModel, cadencée par `withFrameNanos` ». Ce détail ne fonctionne pas :
`withFrameNanos` exige un `MonotonicFrameClock` dans le contexte de la coroutine, et
`viewModelScope` n'en fournit pas. L'appel lève `IllegalStateException` à l'exécution.

Une boucle qui vit dans le ViewModel pose un second problème : elle continue de tourner quand
l'écran n'est plus affiché (application en arrière-plan, autre destination), ce qu'il faudrait
compenser à la main (tâche P5.4).

Options envisagées :

- fournir un `MonotonicFrameClock` au ViewModel (`AndroidUiDispatcher`, ou une horloge injectée) :
  fonctionne, mais lie le ViewModel à Compose et garde la boucle vivante hors écran ;
- confier l'horloge à l'écran, et ne laisser au ViewModel qu'une méthode « une image est passée ».

## Décision
- **L'écran possède l'horloge d'images.** `GameScreen` lance, dans un `LaunchedEffect`, une boucle
  `withFrameNanos { nanos -> viewModel.onFrame(nanos) }` tant qu'il est composé. L'horloge est celle
  du Recomposer de la fenêtre, un `PausableMonotonicFrameClock` que Compose met en pause à
  `ON_STOP` et reprend à `ON_START` : la boucle se suspend donc quand l'activité est arrêtée, mais
  **pas** entre `ON_PAUSE` et `ON_STOP` (dialogue système, multi-fenêtre), où les images
  continuent d'arriver.
- **Le ViewModel délègue à une classe pure `GameLoop`** (`feature:game`, sans coroutine ni
  Android, sans dépendance vers `core:game`) : elle reçoit le temps d'image, accumule le temps
  réel écoulé et **rend le nombre de ticks dus** (`advance(frameNanos): Int`). Le ViewModel, seul
  détenteur du `GameState`, applique d'abord les entrées reçues puis ces ticks au moteur : un seul
  écrivain, un ordre explicite dans chaque image. Le plafond de ticks par image n'est
  pas une précaution mais une nécessité : à la reprise après `ON_STOP`, la première image porte
  tout le temps passé en arrière-plan. C'est l'objet de la tâche P2.1, testé avec une suite de
  temps d'image factices.
- **La pause** reste une règle du domaine (P1.15) : en pause, les images arrivent mais le moteur
  ne change pas d'état. La mise en pause automatique (P5.4) doit viser `ON_PAUSE`, et non compter
  sur l'arrêt de l'horloge à `ON_STOP`, pour
  que la partie soit explicitement en pause au retour, et non simplement figée.

## Conséquences

- `GameLoop` se teste sur la JVM, en pure arithmétique : « trois images à 16,7 ms donnent trois
  ticks », « une image de 500 ms donne au plus N ticks », « le reliquat est reporté sur l'image
  suivante ».
- Le ViewModel n'a plus de coroutine de jeu : `onFrame(nanos)` est synchrone, les tests du
  ViewModel (P2.2) n'ont pas besoin d'horloge virtuelle pour la simulation.
- La vitesse de jeu reste indépendante de la fréquence d'écran, comme le voulait l'ADR 0006,
  dont le pas fixe est inchangé.
- **Amendement (P2.2, 2026-10-18).** La boucle d'images est lancée dans
  `repeatOnLifecycle(Lifecycle.State.STARTED)`, et non directement dans le `LaunchedEffect` :
  elle s'arrête explicitement à `ON_STOP` au lieu de compter sur la pause de l'horloge du
  Recomposer, et chaque reprise commence par `viewModel.onFramesStarted()`, qui appelle
  `GameLoop.reset()`. La première image après un retour à l'écran ne donne donc aucun tick, au
  lieu du plafond (`GameViewModelTest › when frames start again the game loop is reset and the
  next frame gives no tick`).
- **Amendement (P5.4, 2026-10-18).** La phrase « en pause, les images arrivent mais le moteur ne
  change pas d'état » n'est plus exacte : la boucle d'images suit `paused`
  (`collectLatest` sur l'état) et se **suspend** pendant la pause, pour ne pas tourner à 60 Hz
  sans rien donner ; chaque reprise repasse par `onFramesStarted()`, si bien que la première
  image ne rattrape pas le temps passé en pause. La pause automatique est faite : `ON_PAUSE`
  appelle `viewModel.onPause()` (le moteur ignore la pause en fin de partie ou déjà en pause),
  et la partie reste en pause au retour, jusqu'à Reprendre. Une recréation de l'activité (thème
  sombre, langue, taille de fenêtre) passe par `ON_PAUSE` et met donc aussi la partie en pause,
  ce qui est voulu.
- **Amendement (P5.12, 2026-10-19).** La boucle suit `running` (ni en pause, ni finie, une seule
  règle : `GameState.isUnderWay`) et s'arrête donc aussi dès le dernier tick de la partie, avant
  même la réponse des records : l'écran de fin ne demande plus d'images. Pendant tout l'écran de
  jeu, pause et fin comprises, `GameDisplayModeEffect` demande à la fenêtre le mode d'affichage
  à 60 Hz (`WindowManager.LayoutParams.preferredDisplayModeId`, disponible dès l'API 23, donc
  sans repli pour `minSdk` 26) : à 90 Hz la chute était irrégulière, à 120 Hz une image sur deux
  ne servait à rien. Choix du mode, à la résolution courante : le plus lent qui donne un nombre
  entier d'images par tick, 60 Hz puis 120 Hz, 60 préféré à 59,94 ; aucun sinon. Un compteur par
  fenêtre rend la valeur d'avant au départ du dernier écran de jeu, car deux écrans coexistent
  pendant la transition de « Rejouer ». Écartés : `Modifier.preferredFrameRate` et
  `View.setRequestedFrameRate` (API 35, simple vote), `Surface.setFrameRate` (pas de `Surface`
  accessible sous Compose), `preferredRefreshRate` (ignoré si un mode est demandé). Le système
  n'honore la demande que pour la fenêtre au premier plan.
  `GameViewModelTest › a game under way asks the screen for frames`,
  `GameViewModelTest › a paused game asks the screen for no frame`,
  `GameViewModelTest › once the game is over the screen is asked for no frame, even before the records answered`,
  `GameScreenFramesTest › the game over screen asks for no frame: Compose becomes idle`,
  `DisplayModeTest › on a 120 Hz screen the game asks for the 60 Hz mode`,
  `DisplayModeTest › the game never asks for a mode of another resolution`,
  `DisplayModeTest › the game never asks for a mode of the same width but another height`,
  `DisplayModeTest › the game never asks for a mode of the same height but another width`,
  `DisplayModeTest › a mode slower than 60 Hz is never asked for, a tick would then miss its frame`,
  `DisplayModeTest › a mode the screen reports a hair under 60 Hz counts as 60 Hz`,
  `DisplayModeTest › between a mode at 59,94 Hz and one at 60 Hz the game asks for 60 Hz`,
  `DisplayModeTest › without a 60 Hz mode the game asks for 120 Hz rather than 90 Hz, two frames for each tick`,
  `DisplayModeTest › a screen with no mode at a multiple of 60 Hz is asked for nothing`,
  `GameScreenFramesTest › the game screen asks for the 60 Hz mode of the display, and gives the previous one back once left`,
  `GameScreenFramesTest › two game screens shown together, as while Rejouer goes on, keep the mode until both are gone`.
  À vérifier sur un téléphone à 90 ou 120 Hz (l'émulateur n'a qu'un mode) : passage sans
  clignotement, chute régulière, retour à la fréquence d'origine sur l'accueil.
- Le diagramme de `docs/architecture.md` est mis à jour : l'écran envoie les images, le
  ViewModel envoie les entrées, `GameLoop` envoie les ticks au moteur.

## Amendement (P5.44, 2026-10-20)

`GameLoop` devient `FrameTicks` (`core:designsystem`, paquet `frame`), partagée par le jeu et
l'éditeur de musique, dont la tête de lecture (P4.19c) en avait une copie. Le plafond de ticks par
image est un paramètre : 4 pour le jeu (la raison ci-dessus ne change pas), aucun pour l'éditeur,
qui suit une musique jouée en temps réel et doit rattraper tous les ticks. `GameLoop.reset()`
devient `FrameTicks.reset()`. La classe reste pure (sans coroutine ni horloge), testée sur la JVM
(`FrameTicksTest`) ; elle vit dans `core:designsystem` parce que les deux features en dépendent
déjà et que le domaine ne doit pas connaître le temps réel (ADR 0006). `TICKS_PER_SECOND` y est
public.
