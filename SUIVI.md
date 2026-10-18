# Suivi de l'implémentation

Une tâche = un incrément livrable, testé, commité. Les identifiants (`P1.3`) servent de
référence dans les commits et avec la commande `/prochaine-tache`.

Légende : `[ ]` à faire · `[~]` en cours · `[x]` terminé · **[à vérifier]** = règle à
confirmer sur une source avant d'écrire le test.

## Vue d'ensemble

| Phase | Objectif | État |
|---|---|---|
| P0 | Structure du projet et documentation | Terminée (à valider par un build local) |
| P1 | Domaine du mode Classique (`core:game`) | À faire |
| P2 | Écran de jeu jouable (`feature:game`) | À faire |
| P3 | Accueil, préférences, records | À faire |
| P4 | Musique et sons | À faire |
| P5 | Finitions | À faire |
| P6 | Mode Moderne | Plus tard |

---

## P0 — Structure et documentation

- [x] **P0.1** Structure Gradle multi-module, catalogue de versions, plugins de convention.
- [x] **P0.2** Squelette d'application : activité unique, navigation Accueil → Jeu, thème BloX.
- [x] **P0.3** Documentation : architecture, règles, thèmes, musique, maquette, ADR 0001 à 0008.
- [x] **P0.4** CI GitHub Actions (tests, lint, APK debug).
- [ ] **P0.5** **Premier build local.** Le projet a été créé sans accès au réseau : rien n'a encore
  été compilé. Ouvrir dans Android Studio, lancer `./gradlew assembleDebug testDebugUnitTest`,
  corriger ce qui doit l'être, et mettre à jour les versions de `libs.versions.toml` si
  Android Studio en propose de plus récentes (AGP, Kotlin, BOM Compose).
- [x] **P0.7** Définition de terminé (`CLAUDE.md`), commande `/fin-de-tache`, revue
  indépendante par sous-agent, vues de maquette numérotées V1 à V4.
- [ ] **P0.6** Ajouter une analyse statique : ktlint (via le plugin Gradle) ou detekt, branchée
  dans `build-logic` et dans la CI.

## P1 — Domaine du mode Classique (`core:game`)

Référence : [docs/regles-mode-classique.md](docs/regles-mode-classique.md). Tout en TDD.

- [ ] **P1.1** `Position`, `Board` immuable 10×20 : cellule vide/occupée, hors limites,
  `Board.parse(ascii)` pour les tests.
- [ ] **P1.2** `PieceType` et formes des 7 pièces dans chaque orientation.
  Cas : nombre de cellules, nombre d'états par pièce (O : 1 ; I, S, Z : 2 ; T, J, L : 4).
  Orientations de départ **[à vérifier]**.
- [ ] **P1.3** Placement d'une pièce : collision avec les bords, le fond et les cellules occupées.
- [ ] **P1.4** Déplacements gauche / droite / bas, refusés en cas de collision.
- [ ] **P1.5** `ClassicRotationSystem` : rotations horaire et anti-horaire, refus sans décalage.
  Centres de rotation **[à vérifier]**, un jeu de tests par pièce.
- [ ] **P1.6** Verrouillage : la pièce rejoint le plateau quand elle ne peut plus descendre.
- [ ] **P1.7** Effacement des lignes : 1 à 4 lignes, lignes non contiguës, chute des lignes
  supérieures.
- [ ] **P1.8** `ClassicRandomizer` avec graine : un seul nouveau tirage si 7 ou répétition
  **[à vérifier]**. Tester avec une source d'aléatoire scriptée.
- [ ] **P1.9** `ClassicGravity` : table des images par ligne, niveaux 0 à 29+.
- [ ] **P1.10** `ClassicScoring` : 40/100/300/1200 × (niveau + 1), points de descente rapide.
- [ ] **P1.11** `ClassicLevelProgression` : premier passage selon le niveau de départ, puis
  toutes les 10 lignes. Cas : départs 0, 5, 9.
- [ ] **P1.12** `GameEngine` : machine à états (apparition, chute, verrouillage, délai
  d'apparition, effacement, fin de partie). `tick()` et `apply(Input)` renvoient un nouvel état
  et des événements.
- [ ] **P1.13** Commandes maintenues : DAS 16/6, descente rapide **[à vérifier : 1 ligne toutes
  les 2 images]**.
- [ ] **P1.14** Délai d'apparition paramétrable (10 à 18 images selon la hauteur)
  **[à vérifier]**.
- [ ] **P1.15** Pause (l'état ne change plus pendant la pause) et statistiques de fin de partie.
- [ ] **P1.16** Événement `StackDanger` (pile proche du sommet), pour la musique.

## P2 — Écran de jeu jouable (`feature:game`)

- [ ] **P2.1** `GameLoop` à pas fixe : accumulateur testé avec une horloge factice
  (ADR 0006).
- [ ] **P2.2** `GameViewModel` : `GameUiState`, conversion des actions en `Input`, tests avec
  coroutines-test et Turbine.
- [ ] **P2.3** Rendu du plateau en `Canvas`, styles de cellules par thème (`docs/themes.md`).
- [ ] **P2.4** Colonne latérale : score, niveau, lignes, pièce suivante.
- [ ] **P2.5** Commandes tactiles : ◀ ▼ ▶, A, B, avec appui maintenu.
- [ ] **P2.6** Écran de pause (plateau masqué) et reprise.
- [ ] **P2.7** Écran de fin de partie, Rejouer / Menu.
- [ ] **P2.8** Test sur téléphone : boutons ou gestes ? Décider et consigner (ADR si changement).

## P3 — Accueil, préférences, records

- [ ] **P3.1** `UserPreferencesRepository` (DataStore) : thème, musique, niveau de départ.
- [ ] **P3.2** `HighScoreRepository` : meilleur score, détection d'un nouveau record.
- [ ] **P3.3** `AppContainer` et fabriques de ViewModels (ADR 0007).
- [ ] **P3.4** Écran d'accueil complet, conforme à la maquette.
- [ ] **P3.5** Changement de thème appliqué à toute l'application.
- [ ] **P3.6** Polices : embarquer *Press Start 2P* et *Chakra Petch* (OFL) ou utiliser les polices
  téléchargeables. Décider et consigner.

## P4 — Musique et sons

Référence : [docs/musique.md](docs/musique.md).

- [ ] **P4.1** `core:music` : `Pitch` (fréquence), `Note`, `Voice`, `Score`, en TDD.
- [ ] **P4.2** Partitions *Korobeïniki* et *Menuet* : tests de cohérence (durée de chaque
  mesure, voix de même longueur).
- [ ] **P4.3** Générateur d'échantillons pur (onde carrée, triangle, enveloppe, mixage), testé
  sur la JVM.
- [ ] **P4.4** `ChiptuneSequencer` sur `AudioTrack`, lecture en boucle, arrêt propre.
- [ ] **P4.5** Accélération du tempo sur `StackDanger`.
- [ ] **P4.6** `AudioFilePlayer` : choix d'un fichier personnel via le sélecteur du système.
- [ ] **P4.7** Aperçu « Écouter » sur l'accueil.
- [ ] **P4.8** Effets sonores courts (rotation, ligne, 4 lignes, fin de partie), optionnels.

## P5 — Finitions

- [ ] **P5.1** Icône de l'application et écran de démarrage.
- [ ] **P5.2** Vibrations (optionnelles) sur ligne effacée et fin de partie.
- [ ] **P5.3** Accessibilité : descriptions des boutons, contrastes, tailles de cible.
- [ ] **P5.4** Gestion du cycle de vie : pause automatique quand l'app passe en arrière-plan.
- [ ] **P5.5** Tests d'interface des parcours principaux (Compose UI Test).
- [ ] **P5.6** Build de release signé pour installation personnelle.

## P6 — Mode Moderne (plus tard)

Nouvelles implémentations des politiques de `core:game`, sans modifier le moteur :
rotation SRS avec décalages, sac de 7, réserve, pièce fantôme, chute instantanée, délai de
verrouillage, plusieurs pièces suivantes.

---

## Journal

| Date | Tâche | Ce qui a été fait | Décisions / points ouverts |
|---|---|---|---|
| 2026-10-18 | P0.1 – P0.4 | Structure Gradle (8 modules, `build-logic`, catalogue), squelette Compose, documentation, ADR 0001 à 0008, CI. | Créé sans accès réseau : **aucun build exécuté**, à faire en P0.5. Versions figées à des valeurs connues de fin 2025, à mettre à jour. Identifiant d'application `ch.magori.blox`. |
| 2026-10-18 | P0.7 | Procédure de fin de fonctionnalité reprise d'un autre projet et adaptée : modules BloX, lint et APK, revue sans contexte, remarques écartées tracées, sources des valeurs confirmées, ADR, branche par tâche. | La maquette Claude Design ne se modifie que depuis claude.ai : Claude Code ajoute une tâche au suivi au lieu de la mettre à jour. |
