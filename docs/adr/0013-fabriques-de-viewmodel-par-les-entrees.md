# 0013 — Les fabriques de ViewModel entrent par les entrées de navigation

- **Statut** : Acceptée ; précise l'[ADR 0007](0007-injection-manuelle.md) ; amendée en P5.13 et P7.19
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

## Amendement (P5.13, 2026-10-19)

La fabrique de `GameViewModel` tire aussi des `CreationExtras` le `SavedStateHandle` de l'entrée
(`extras.createSavedStateHandle()`), pour y ranger un marqueur « partie commencée ». L'entrée doit
donc partir des `defaultViewModelCreationExtras` du propriétaire fourni par
`rememberViewModelStoreNavEntryDecorator` et y ajouter la route, sans les remplacer ; et ce
décorateur doit suivre `rememberSaveableStateHolderNavEntryDecorator`, qui fournit le registre
d'état propre à l'entrée.

## Amendement (P7.19, 2026-10-27)

Les entrées Jeu, Éditeur et Serrurier lisaient leurs extras de la même façon, en neuf lignes écrites trois fois :
le propriétaire tiré de `LocalViewModelStoreOwner` et vérifié non nul, ses extras par défaut obtenus par un
transtypage en `HasDefaultViewModelProviderFactory` (sinon `CreationExtras.Empty`), une copie mutable mémorisée
avec `remember` et complétée par la clé, puis le propriétaire repassé à `viewModel(…)`.

Lifecycle 2.11 fournit chacune de ces étapes. Une entrée s'écrit désormais :

```kotlin
val defaults = LocalViewModelStoreOwner.current.defaultViewModelCreationExtras
val extras = CreationExtras(defaults) { set(GameViewModel.RouteKey, route) }
val viewModel: GameViewModel = viewModel(factory = viewModelFactory, extras = extras)
```

- `ViewModelStoreOwner?.defaultViewModelCreationExtras` (paquet `androidx.lifecycle`) remplace le transtypage et le
  repli sur `CreationExtras.Empty`.
- `CreationExtras(initialExtras) { … }` part de ces extras par défaut, sans les remplacer (amendement P5.13), et y
  ajoute la clé de la feature.
- `viewModel(…)` prend lui-même le propriétaire de `LocalViewModelStoreOwner`, avec la même vérification.
- `remember` disparaît : les extras ne servent qu'à la création du ViewModel, que le magasin de l'entrée garde ; les
  reconstruire à une recomposition ne coûte qu'une petite table.

Ce qui reste commun aux trois entrées tient en deux appels de bibliothèque ; la clé et sa forme (route entière,
clé de musique facultative, dé) sont propres à chaque feature. Une fonction partagée n'ajouterait qu'un nom de plus :
il n'y en a pas. `ComposerEntryTest` compose l'entrée de l'éditeur dans un `NavDisplay` décoré comme dans `app` et
prouve que la clé de la musique et le dé parviennent à la fabrique, les extras par défaut gardés (sans eux,
`createSavedStateHandle` échouerait) ; `GameEntryRestorationTest` fait de même pour le jeu.
