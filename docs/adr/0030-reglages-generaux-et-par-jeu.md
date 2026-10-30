# 0030 — Réglages généraux et réglages par jeu : une page Paramètres dans `feature:settings`

- **Statut** : Acceptée ; amende [0002](0002-modularisation.md), [0011](0011-navigation-3.md) et [0022](0022-choix-du-jeu-a-la-racine.md)
- **Date** : 2026-10-30

## Contexte

La page Paramètres (V21, refondue en V31) est née en P5.50 dans `feature:home`, comme une page de l'accueil du
Classique : clé `SettingsRoute` (un `data object`), ouverte par l'engrenage de l'accueil (V1), et depuis P9.5 par
celui de la Graine du jour (V38). Elle suit le thème choisi pour le Classique (aspect Original de V21) et montre
toujours la carte « Record » du Classique, même ouverte depuis la Graine du jour.

Depuis, BloX compte plusieurs jeux (ADR 0022, 0025, 0026, 0027), et la plupart des réglages de la page valent pour
plusieurs d'entre eux. Le développeur demande (2026-10-28) des réglages communs à tous les jeux, ouverts depuis le
choix du jeu, et des réglages propres à chaque jeu. La proposition de `designer-jeu` est retenue : une seule page,
dont le contenu dépend de l'écran qui l'ouvre, toujours en Couleurs.

La page n'a plus sa place dans `feature:home` : elle n'est plus celle de l'accueil du Classique, mais celle de tous
les jeux. `feature:home` ne dépend de `core:haptics` que pour `SettingsViewModel`. Et l'ADR 0002 prévoyait déjà un
`feature:settings` « si le besoin apparaît ».

Réglages et données de la page aujourd'hui (`core:data`), et où ils sont lus :

- Effets réduits (`UserPreferences`) : le thème de toute l'application (`AppTheme`, `app`), tout de suite.
- Gaucher, Vibrations, Vibration des touches (`UserPreferences`) : les touches et retours de `feature:game`
  (Classique, et ses variantes Graine du jour et, à venir, Au tempo), à la création de la partie.
- Effets sonores (`UserPreferences`) : `feature:game`, le Serrurier (`LocksmithViewModel`) et le Jardin
  (`GardenViewModel`), à la création de l'écran.
- Volume des effets et volume de la musique (`UserPreferences`) : les lecteurs de l'application (`app`), tout de
  suite.
- Record du Classique (`HighScoreRepository`, pas un réglage) : seul bouton « Effacer » de la page.

Hors de la page, l'accueil du Classique (V1, V27) enregistre Thème, Niveau de départ et Musique ; il affiche aussi le
Mode, un choix qui n'est pas enregistré (Classique seul, Moderne « BIENTÔT »).

## Décision

### 1. La portée de chaque réglage

| Réglage | Portée | Section de la page | Jeux concernés |
|---|---|---|---|
| Effets réduits | Tous les jeux | « Tous les jeux » (Affichage) | Toute l'application |
| Gaucher | Tous les jeux | « Tous les jeux » (Affichage) | Classique, Graine du jour, Au tempo |
| Effets sonores | Tous les jeux | « Tous les jeux » (Sons et vibrations) | Tous |
| Volume des effets | Tous les jeux | « Tous les jeux » (Sons et vibrations) | Tous |
| Volume de la musique | Tous les jeux | « Tous les jeux » (Sons et vibrations) | Tous |
| Vibrations | Tous les jeux | « Tous les jeux » (Sons et vibrations) | Classique, Graine du jour, Au tempo |
| Vibration des touches | Tous les jeux | « Tous les jeux » (Sons et vibrations) | Classique, Graine du jour, Au tempo |
| Record du Classique (Effacer) | Classique | « Réglages du Classique » | Classique |
| Réglages du mode Moderne (P6.17, P6.21) | Classique | « Réglages du Classique » | Classique (mode Moderne) |
| Thème, Niveau de départ, Musique ; Mode (affiché, non enregistré) | Classique | Hors de la page : accueil du Classique (V1) | Classique |
| Métronome | Au tempo | Hors de la page : carte Pause d'Au tempo (P11.9) | Au tempo |

- Un réglage « Tous les jeux » reste **un seul réglage**, enregistré une fois dans `UserPreferences` : pas de valeur
  par jeu. Ses sous-lignes nomment les jeux concernés quand ce ne sont pas tous (P12.7).
- Le Serrurier, le Jardin musical et la Graine du jour **n'ont pas de section** : aucun réglage propre. La Graine du
  jour est une variante du Classique (ADR 0026), mais son record n'est pas celui du Classique, et elle ne change ni
  de thème ni de musique.
- Un réglage propre à un jeu ne va sur la page que s'il se règle hors d'une partie et rarement ; ce qui se choisit à
  chaque partie reste dans l'accueil du jeu (Thème, Niveau, Musique du Classique) ou dans sa pause (Métronome).

### 2. Une page unique, une section par jeu

- **Une seule page Paramètres**. Ouverte depuis le choix du jeu, elle ne montre que « Tous les jeux ». Ouverte depuis
  un jeu qui a une section, elle montre d'abord cette section, sous un titre lu « Réglages du Classique » (un titre
  pour TalkBack), puis « Tous les jeux ». Ouverte depuis un jeu sans section, elle est la même que depuis le choix du
  jeu.
- Un seul `SettingsViewModel` pour toutes les ouvertures ; les sections d'un jeu s'ajoutent à la page, jamais dans une
  page à part.

### 3. Le module `feature:settings`

- Nouveau module **`feature:settings`** (`blox.android.feature` et `blox.android.screenshot`, pour que les captures
  de la page le suivent) : `SettingsScreen`, `SettingsViewModel` et sa fabrique, `SettingsUiState`, `RecordClearing`,
  `SettingsActions`, `KeysSketch`, la clé `SettingsRoute` et l'entrée `settingsEntry`, déplacés de `feature:home`
  (P12.2). Il dépend de `core:designsystem`, `core:data`, `core:audio`, `core:haptics` et `core:music`, d'aucune
  feature.
- `saveSettings` (aujourd'hui `SaveSettings.kt` de `feature:home`), qui sert à `HomeViewModel` et à
  `SettingsViewModel`, devient une extension de `UserPreferencesRepository` dans `core:data` (P12.2) : les deux
  features la partagent sans dépendre l'une de l'autre.
  *P12.2 (2026-10-30)* : fait, `UserPreferencesRepository.saveSettings(scope, write, onSaved)` est dans `core:data`
  (`SaveSettings.kt`).
- `app` construit la fabrique (ADR 0013) et assemble l'entrée, comme aujourd'hui ; `feature:home` et
  `feature:daily` gardent leur rappel `onSettings`, et `feature:cartridges` en reçoit un (P12.5). Le bouton
  Paramètres, aujourd'hui copié dans `HomeBar` et `DailyScreen`, devient un `SettingsButton` commun de
  `core:designsystem` avant d'en faire une troisième copie (P12.11).

### 4. La clé et son paramètre

- `SettingsRoute(section: SettingsSection? = null)` remplace le `data object` : une `data class` `@Serializable`, et
  un `enum class SettingsSection { CLASSIC }` défini dans `feature:settings`. `null` : pas de section de jeu.
- Une valeur de `SettingsSection` désigne une section de la page : il n'en existe que pour un jeu qui a une section,
  aujourd'hui le Classique seul. Ajouter une section à un jeu, c'est ajouter une valeur.
- `feature:settings` ne connaît ni les cartouches ni les clés des jeux : `app` choisit la valeur selon l'écran qui
  ouvre la page.

### 5. La navigation

| Ouvert depuis | Clé empilée par `app` | Retour (flèche ou système) |
|---|---|---|
| Choix du jeu (V36, roue dentée, P12.5) | `SettingsRoute()` | Le choix du jeu |
| Accueil du Classique (V1, engrenage) | `SettingsRoute(SettingsSection.CLASSIC)` | L'accueil du Classique |
| Graine du jour (V38, roue dentée, P12.6) | `SettingsRoute()` | L'écran d'entrée de la Graine du jour |

- Ouvrir la page, c'est **empiler** sa clé ; le retour la **dépile** (`goBack`) et retrouve l'écran qui l'a ouverte,
  tel qu'il était. La page n'empile rien : rien ne part d'elle vers un autre écran.
- **La roue dentée du choix du jeu passe par `CartridgeOpener`** (`app`), comme les cartouches : elle annule un
  toucher sur le Serrurier qui attend encore sa frontière, et n'empile la page que si `CartridgesRoute` est en haut
  de la pile. Sans cela, toucher le Serrurier puis la roue empilerait les Paramètres, puis la serrure par-dessus.
- La clé est sauvegardée avec la pile : après la mort du processus, la page revient avec la même section.
- Le Serrurier, le Jardin musical et Au tempo n'ouvrent pas la page (pas d'engrenage dans leur barre ni dans leur
  pause) : ils n'ont pas de réglage propre. S'ils en reçoivent un, il empilera `SettingsRoute()`, sauf si une section
  leur est ajoutée.

### 6. Le thème

- La page est **toujours dessinée en Couleurs v2** (`ColorfulTheme`), quel que soit le thème choisi pour le
  Classique, comme le choix du jeu, la Graine du jour, le Serrurier et le Jardin. L'aspect Original de V21 disparaît
  (P12.4) : l'Original est figé et n'habille que le Classique.
- **Barres système comprises** : `SettingsRoute` entre dans `isColorfulOnly` (`SystemBars.kt`, `app`), et
  `SystemBarsTest` fait passer « the settings » de `otherScreens` à `colorfulOnlyScreens` (P12.4).
- La note de la section du Classique dit où se choisit son thème : « Le thème du Classique se choisit dans son
  accueil. » (P12.7).

## Conséquences

- Un réglage commun se règle une fois pour tous les jeux, depuis le choix du jeu, sans passer par le Classique.
- La Graine du jour cesse de montrer « Record du Classique » (P12.6), qui n'était pas le sien.
- `feature:home` perd la page, ses tests et sa dépendance à `core:haptics` ; `app`, `core:data` (`saveSettings`), le
  graphe des modules et `buildHealth` changent (P12.2). Le parcours `BloxNavDisplayTest` des Paramètres suit la clé
  paramétrée ; il lit la chaîne `feature_home_settings`, qui part avec la page.
  *P12.2 (2026-10-30)* : seul le titre de la page part, en `feature_settings_title` ; l'engrenage de l'accueil garde
  `feature_home_settings`, que le parcours touche toujours pour ouvrir la page.
  *P12.11 (2026-10-30)* : l’engrenage devient le `SettingsButton` de `core:designsystem`, partagé par l’accueil, la
  Graine du jour et bientôt le choix du jeu ; le parcours lit sa chaîne, `core_designsystem_settings`.
- Le choix du jeu gagne un bouton qui n'est pas une cartouche : le test
  « les seuls boutons de l'écran sont les cartouches » de `CartridgesScreenTest` est retiré en P12.5.
- La mention « Valable dès la partie suivante » devient fausse pour les volumes et les Effets réduits, lus tout de
  suite : elle est réécrite (P12.7).
- Les réglages du mode Moderne (P6.17, P6.21) vont dans la section du Classique (P12.9), pas dans « Tous les jeux ».
- La maquette doit suivre : V36 avec sa roue dentée, une vue des Paramètres ouverts depuis le choix du jeu, V31 avec
  ses deux sections, une note sur V38, V21 remplacée (P12.10).
- À surveiller : si un réglage « Tous les jeux » doit un jour différer d'un jeu à l'autre, il devient un réglage
  propre, dans la section de chaque jeu concerné, et ce tableau est amendé.

## Options écartées

- **Une page par jeu**, plus une page générale : deux endroits pour un même réglage, ou des réglages communs
  introuvables depuis un jeu ; plus de code et de captures.
- **Des réglages communs dans le choix du jeu lui-même** (une feuille du bas) : le choix du jeu reste une liste de
  cartouches, sans ViewModel (ADR 0022).
- **Garder la page dans `feature:home`** : techniquement possible (`app` empilerait la clé pour le choix du jeu,
  comme il le fait déjà pour la Graine du jour), mais la page n'est plus celle de l'accueil du Classique, elle seule
  tire `core:haptics` dans `feature:home`, et l'ADR 0002 prévoyait ce module.
- **Une valeur de chaque réglage par jeu** (vibrations du Serrurier, du Classique…) : rien ne le demande, et chaque
  jeu se réglerait à part.
- **Un paramètre de clé qui nomme tous les jeux** (une liste de toutes les cartouches) : `feature:settings`
  dépendrait d'une liste qu'il ne connaît pas ; seule une section existante a sa valeur.
- **Suivre le thème du Classique** : la page s'ouvre aussi depuis des écrans toujours en Couleurs, et l'Original est
  figé.
