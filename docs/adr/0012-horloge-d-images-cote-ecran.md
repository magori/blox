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
- Le diagramme de `docs/architecture.md` est mis à jour : l'écran envoie les images, le
  ViewModel envoie les entrées, `GameLoop` envoie les ticks au moteur.
