# 0011 — Navigation 3 : la pile de navigation appartient à l'application

- **Statut** : Acceptée ; le point P2.10 (fabriques vers les features) est consigné dans [0013](0013-fabriques-de-viewmodel-par-les-entrees.md) ; amendée par [0022](0022-choix-du-jeu-a-la-racine.md) et [0030](0030-reglages-generaux-et-par-jeu.md)
- **Date** : 2026-10-18

## Contexte

L'[ADR 0001](0001-kotlin-compose-activite-unique.md) retenait Navigation Compose (Navigation 2)
en routes typées. Depuis, Google a publié **Navigation 3**, stable en 1.0 le 19 novembre 2025
et en 1.2.0 le 23 septembre 2026, et recommande aux projets sur Navigation Compose d'envisager
la migration ([annonce](https://android-developers.googleblog.com/2025/11/jetpack-navigation-3-is-stable.html)).
L'intégration des ViewModels par entrée (`lifecycle-viewmodel-navigation3`) est stable depuis
Lifecycle 2.10.0, version déjà dépassée au catalogue (2.11.0).

Les parcours de BloX sont peu nombreux et se décrivent mieux comme une liste que comme un
graphe : Accueil → Jeu ; Jeu → Fin de partie ; Fin de partie → Rejouer (une nouvelle partie
remplace l'ancienne) ou Menu (retour à l'accueil). La pause n'est pas une destination mais un
état de l'écran de jeu. Navigation 2 impose pour ces cas des `popUpTo` et des options de
navigation ; Navigation 3 expose la pile comme une liste que l'application modifie directement.

Le squelette ne compte que deux destinations sans ViewModel : c'est le moment où changer de
bibliothèque coûte une heure, et non plusieurs jours.

Options envisagées :

- rester sur Navigation Compose 2.x : stable, connu, mais le modèle de graphe est de trop pour
  deux écrans et une pile explicite ; migration plus coûteuse à chaque écran ajouté ;
- Navigation 3 plus tard, « quand le besoin apparaîtra » : le besoin (Rejouer, Fin de partie)
  arrive en P2, avec du code à migrer ;
- Navigation 3 maintenant.

## Décision
- **Navigation 3** (`navigation3-runtime`, `navigation3-ui` 1.2.0) remplace Navigation Compose.
  `lifecycle-viewmodel-navigation3` (stable en 2.11.0) sera ajouté en P2.2 avec le premier
  ViewModel : rien de ce qui n'est pas encore utilisé n'est câblé.
- **Chaque feature définit sa clé** (`@Serializable data object HomeRoute : NavKey`) et expose
  une extension d'`EntryProviderScope<NavKey>` (`homeEntry(onPlay)`, `gameEntry(onBack)`)
  qui déclare son entrée. Les features ne dépendent que de `navigation3-runtime`, ajouté par
  le plugin de convention `blox.android.feature`.
- **`app` possède la pile** : `BloxNavDisplay` crée la pile avec `rememberNavBackStack(HomeRoute)`
  (sauvegardée sur changement de configuration et mort du processus), compose les entrées des
  features dans un `entryProvider`, et affiche la pile avec `NavDisplay`. Les clés gardent le
  suffixe `Route` (`HomeRoute`, `GameRoute`), vocabulaire des recettes officielles ; la
  documentation les appelle « clés ». Quand les ViewModels arriveront, les décorateurs
  `rememberSaveableStateHolderNavEntryDecorator` et `rememberViewModelStoreNavEntryDecorator`
  seront déclarés dans cet ordre : le premier est requis dès qu'on fournit la liste, alors
  que `NavDisplay` l'applique seul par défaut.
- **Naviguer, c'est modifier la liste** : `backStack.add(GameRoute)` pour entrer en jeu,
  `backStack.removeLastOrNull()` pour revenir. Le retour système (geste ou bouton) passe par
  `NavDisplay.onBack` et fait la même chose.
- La pause reste un **état de l'écran de jeu**, pas une entrée. **La fin de partie (V4) aussi**
  (tranché en P2.7) : le ViewModel de la partie garde la partie terminée et ses statistiques,
  l'écran de jeu affiche V4 selon son état. **Rejouer remplace la clé du sommet de la pile** par
  une nouvelle `GameRoute` (même niveau de départ, nouvelle graine), ce qui crée un ViewModel
  neuf ; le ViewModel ne relance jamais une partie lui-même. Menu, comme le retour système sur
  V4, dépile `GameRoute`. Après la mort du processus, la clé restaurée rejoue la même partie
  depuis le début (même graine) : V4 n'est pas restauré.

## Conséquences

- Les ViewModels d'écran (P2.2) sont créés avec `viewModel(factory = …)` dans le contenu de
  l'entrée, et liés à l'entrée par le décorateur : la fabrique se passe en paramètre de
  l'extension `gameEntry(...)` depuis `app`, sans dépendance inverse ni `CompositionLocal`.
  C'est la réponse à la tâche P2.10, à consigner dans l'ADR 0007 le moment venu.
- **Une partie = une clé distincte.** Le décorateur identifie le ViewModel par la clé de
  l'entrée : remplacer `GameRoute` par un `GameRoute` égal garderait le même ViewModel. Quand
  le jeu existera, `GameRoute` portera le niveau de départ et la graine du tirage
  (`data class`), donc Rejouer ajoute une entrée différente et obtient un ViewModel neuf.
- Pas de liens profonds, pas de pile par onglet : BloX n'en a pas besoin ; les recettes
  existent si cela change.
- Les animations de transition sont celles par défaut de `NavDisplay` ; le retour prédictif
  est pris en charge sans code supplémentaire.
- Le skill officiel `navigation-3` de Google est copié dans `.claude/skills/` pour appuyer
  les tâches suivantes sur la documentation de référence.
- Avec l'outillage retenu (`ui-test-android` en `androidTest`), le parcours Accueil → Jeu →
  Retour n'est vérifiable que par un test Compose instrumenté
  (P5.5) ; en attendant, la preuve de cette tâche est le build et le lint verts. Les règles R8
  nécessaires à la restauration de la pile viennent de kotlinx-serialization, pas de
  `navigation3-runtime` : à vérifier en build minifié (P5.6).

## Amendement (ADR 0030, 2026-10-30)

La page Paramètres n'est plus une page de l'accueil : elle vit dans `feature:settings` et s'ouvre depuis plusieurs
écrans. Sa clé porte un paramètre, `SettingsRoute(section: SettingsSection? = null)`, que `app` choisit selon l'écran qui
l'ouvre : `SettingsRoute()` depuis le choix du jeu et la Graine du jour, `SettingsRoute(SettingsSection.CLASSIC)` depuis
l'accueil du Classique. Ouvrir la page empile sa clé ; la flèche Retour et le retour système la dépilent et
retrouvent l'écran qui l'a ouverte. La clé, sauvegardée avec la pile, rend la même section après la mort du
processus.
