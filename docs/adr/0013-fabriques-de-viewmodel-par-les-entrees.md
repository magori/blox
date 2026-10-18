# 0013 — Les fabriques de ViewModel entrent par les entrées de navigation

- **Statut** : Acceptée ; précise l'[ADR 0007](0007-injection-manuelle.md)
- **Date** : 2026-10-18

## Contexte

L'ADR 0007 retient l'injection manuelle : un `AppContainer` dans `app` construit les
dépendances, les ViewModels les reçoivent par constructeur via une `ViewModelProvider.Factory`.
Il ne dit pas comment une feature obtient cette fabrique alors qu'une feature ne dépend jamais
d'`app`. Les solutions habituelles (lire l'`Application` depuis `LocalContext`, un
`CompositionLocal` exposant le conteneur) créent une dépendance cachée vers `app` ou un
localisateur de services.

Avec Navigation 3 ([ADR 0011](0011-navigation-3.md)), le contenu d'une entrée est un composable
ordinaire que `app` déclare en appelant l'extension de la feature : le point de passage existe
déjà.

## Décision

- **La feature définit sa fabrique**, avec ses dépendances en paramètres de constructeur :
  `GameViewModel.Factory(engineFactory, highScores, musicPlayer)` implémente
  `ViewModelProvider.Factory` et ne connaît que des interfaces de `core:*`. Une seule instance
  sert à toutes les parties.
- **`app` construit la fabrique depuis `AppContainer` et la passe à l'entrée** :
  `gameEntry(onBack = goBack, viewModelFactory = container.gameViewModelFactory())`.
- **Les arguments de navigation passent par `CreationExtras`.** La clé (niveau de départ,
  graine) n'est connue que dans le contenu de l'entrée, `entry<GameRoute> { route -> … }`, après
  la construction de la fabrique. L'entrée appelle donc
  `viewModel(factory = viewModelFactory, extras = …)` avec la route rangée sous une
  `CreationExtras.Key<GameRoute>` définie par la feature ; la fabrique la lit dans
  `create(modelClass, extras)` et la transmet au ViewModel. C'est le mécanisme standard de
  Lifecycle, pas une fabrique de fabriques. Le décorateur `rememberViewModelStoreNavEntryDecorator`
  lie le ViewModel à son entrée : une clé différente, un ViewModel différent.
- Pas de `CompositionLocal` pour le conteneur, pas de lecture de l'`Application` depuis les
  features.

## Conséquences

- Les features restent indépendantes d'`app` et testables : un test construit le ViewModel avec
  des doubles, sans fabrique ni conteneur.
- `app` reste le seul module qui connaît toutes les features et toutes les implémentations.
- Si le projet passe à Hilt un jour, `viewModel(factory = …)` devient `hiltViewModel()` dans
  l'entrée, et la fabrique disparaît : les ViewModels ne changent pas.
- La tâche P3.3 (`AppContainer`) applique ce schéma ; la tâche P2.2 crée la première fabrique.
