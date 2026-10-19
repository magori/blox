# Revue qualité du 2026-10-19

Revue indépendante demandée avant le mode Moderne (P6), faite sur `main` au commit `df6fcf1` par
trois relecteurs sans contexte : qualité Android et performance (A), qualité logicielle (B),
sécurité, accessibilité, build et CI (C). Aucun constat bloquant ; 11 majeurs. Les constats
retenus sont devenus des tâches de [SUIVI.md](../SUIVI.md) (P5.10 à P5.29, P0.15 complétée,
P5.8 rappelée) ; les constats écartés sont notés à la fin, avec la raison.

État vérifié pendant la revue : `lintDebug buildHealth testDebugUnitTest` vert (476 tests, lint
0 erreur et 8 avertissements dans `app`), `assembleRelease` vert (R8, APK de 1,45 Mo non
signé), 41 avertissements Kotlin, tous dans les tests.

## Constats retenus

| Id | Gravité | Constat | Preuve | Tâche |
|---|---|---|---|---|
| A1 | majeur | Après une partie finie en danger, la musique reste à ×1,25, même pour « Écouter » à l'accueil : le tempo n'est remis qu'au début d'une partie. | `GameViewModel.kt` (`onGameOver`, `onCleared`), `ChiptuneSequencer.kt` | P5.10 |
| A2 | majeur | `BoardCanvas` reconstruit ses 8 peintres et leurs `Path` à chaque mouvement de la pièce (la lambda de `drawWithCache` capture `cells`), contrairement à sa KDoc. | `board/BoardCanvas.kt`, `CellPainter.kt`, `DisplayedCells.kt` | P5.11 |
| A3 | majeur | Après une mort du processus, une partie neuve démarre aussitôt, hors pause, même depuis l'écran de fin. | `GameViewModel.kt`, `GameScreen.kt`, `BloxNavDisplay.kt` | P5.13 |
| A4 | majeur | Pas de focus audio ni de réaction au débranchement du casque : la musique se mélange à une autre application et passe au haut-parleur. | `core/audio/AudioTrackSink.kt` | P5.14 |
| A5 | mineur | La boucle d'images tourne encore sur l'écran de fin de partie. | `GameScreen.kt`, `GameViewModel.kt` | P5.12 |
| A6 | mineur | À 90 ou 120 Hz, le rendu suit l'écran alors que la simulation est à 60 Hz : chute irrégulière à 90 Hz, une image sur deux inutile à 120 Hz. | `GameScreen.kt`, `GameLoop.kt` | P5.12 |
| A7 | mineur | Tout l'arbre du jeu recompose à chaque mouvement de la pièce (`uiState` lu à la racine, lambdas recréées). | `GameScreen.kt` | P5.11 |
| A8 | mineur | Cache des rendus musicaux sans borne (environ 7,5 Mo) et rendu de 6,8 Mo déclenché à l'entrée en danger. | `ChiptuneSequencer.kt`, `Synthesizer.kt` | P5.15 |
| A9 | mineur | Pas de profil de référence (baseline profile) : premières images et démarrage plus lents. | `app/build.gradle.kts` | P5.17 |
| A10 | mineur | Les commandes du bord gauche peuvent tomber dans la zone du geste Retour. | `GameScreen.kt`, `GameLayout.kt` | P5.16 |
| A11 | mineur | Une sortie audio invalidée (`ERROR_DEAD_OBJECT`) coupe la musique jusqu'à la partie suivante. | `AudioTrackSink.kt`, `ChiptuneSequencer.kt` | P5.15 |
| B1 | majeur | Quatre références « règle → test » de la doc pointent vers des tests introuvables (dont une qui laisse croire que `ClassicScoringRule` garde la borne 0..4) ; le script de P5.18 en a trouvé une cinquième (`CellStylesTest`). | `docs/regles-mode-classique.md:79`, `:519`, `docs/musique.md:178`, `:324` | P5.18 |
| B2 | majeur | La règle « un appui pendant l'effacement ou le délai d'apparition est perdu » n'a aucun test ; le compteur de déplacement de départ (`shift == null`) n'a pas de source. | `docs/regles-mode-classique.md:341`, `GameEngine.kt:204` | P5.19 |
| B3 | majeur | Les tests du moteur construisent des états impossibles (niveau 29, 0 ligne, départ 0) et `GameState` ne vérifie aucun invariant : après un effacement, le niveau retombe de 29 à 0 sans qu'un test le voie. | `GameStates.kt`, `GameEngineClearTest.kt`, `GameProgress.kt` | P5.20 |
| B4 | mineur | La règle « au moins 1 tick » est réécrite cinq fois sur des `Int` alors que `Ticks` existe ; deux décomptes identiques dans le moteur. | `Phase.kt`, `RepeatTiming.kt`, `GameEngine.kt` | P5.21 |
| B5 | mineur | Des détails internes du moteur sont publics sans usage hors module (`AutoRepeat`, `GameState.controls`, constructeurs de `Clearing` et `EntryDelay`). | `AutoRepeat.kt`, `GameState.kt`, `Phase.kt` | P5.21 |
| B6 | mineur | Erreurs de lecture des records traitées différemment : avalées en jeu (`catch {}` sans filtre), non traitées à l'accueil. | `GameViewModel.kt`, `HomeViewModel.kt` | P5.22 |
| B7 | mineur | KDoc périmées : `GameEvent.StackDanger`, `GameUiState`, `GameState`, un commentaire de `GameViewModel`. | `GameEvent.kt`, `GameUiState.kt`, `GameState.kt`, `GameViewModel.kt` | P5.20, P5.21 |
| B8 | mineur | `GameOverUiState` déballe `Points`, `Lines`, `Level` en `Int`, aussitôt remballés. | `GameUiState.kt`, `gameover/GameOverTexts.kt` | P5.23 |
| B9 | mineur | Doublures de test et `MainDispatcherExtension` copiées entre les deux features ; le faux de records du jeu ne respecte pas le contrat. | `feature/*/src/test` | P0.15 |
| B11 | mineur | `GameUiState` transporte la politique `RotationSystem` jusqu'aux composables. | `GameUiState.kt`, `GameScreen.kt` | P5.23 |
| C1 | majeur | La release minifiée n'est jamais construite en CI, alors que les PR Dependabot sont fusionnées sur CI verte. | `.github/workflows/ci.yml` | P5.24 |
| C2 | majeur | Les tests de captures ne sont pas validés en CI : une montée de Compose peut changer le rendu sans signal. | `ci.yml`, `docs/architecture.md` | P5.8 |
| C3 | majeur | À l'accueil, en thème Original, un choix sélectionné ne se distingue que par sa couleur, à 1,99:1 du non-sélectionné. | `feature/home/HomeScreen.kt:218` | P5.26 |
| C4 | majeur | Le jeu ne se joue qu'au toucher : ni clavier ni manette, et un bouton ne peut pas être tenu via TalkBack ou Switch Access. | `controls/GameControls.kt`, `HeldButtons.kt` | P5.27 |
| C5 | mineur | Les cases du niveau de départ font environ 27,6 dp de large (48 dp recommandés). | `HomeScreen.kt:115` | P5.29 |
| C6 | mineur | Politique de sauvegarde implicite (`allowBackup` sans règles), qui emportera les futures URI de « Mon fichier » sans leurs permissions. | `app/src/main/AndroidManifest.xml` | P5.28 |
| C7 | mineur | CI peu durcie : `GITHUB_TOKEN` sans `permissions`, actions épinglées par tag, pas de soumission du graphe de dépendances. | `ci.yml` | P5.24 |
| C8 | mineur | Les avertissements ne bloquent rien (41 avertissements d'opt-in dans les tests, 8 de lint). | `build-logic`, `ci.yml` | P5.25 |
| C9 | mineur | Tests instrumentés jamais compilés en CI, rapports d'échec non publiés. | `ci.yml` | P5.24 |

## Constats écartés

- **B10** (durées musicales en `Double`, égalité exacte de flottants) : les partitions et le
  générateur prévu (P4.13 à P4.18) n'emploient que des durées dyadiques (croches, noires…),
  exactes en `Double`. À reprendre si des triolets arrivent.
- **C7, partie `verification-metadata.xml`** : Dependabot ne met pas ce fichier à jour, si bien
  que chaque PR hebdomadaire de dépendances échouerait. Le reste de C7 est retenu.

## Points ouverts pour le développeur

- **C9, cache de configuration en CI** : il demande un secret `GRADLE_ENCRYPTION_KEY` dans le
  dépôt GitHub, à créer par le propriétaire.
- **C5** : passer les niveaux sur deux lignes de 5, ou un sélecteur − / +, change la maquette
  (V1 garde aujourd'hui « 0 à 9 sur une ligne ») : décision à prendre.
- **A9** : générer un profil de référence demande un émulateur ou un téléphone.
