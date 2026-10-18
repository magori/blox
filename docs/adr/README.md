# Décisions d'architecture (ADR)

Chaque décision structurante est consignée dans un fichier court : contexte, décision,
conséquences. Un ADR n'est jamais réécrit : s'il est remis en cause, un nouvel ADR le
remplace et l'ancien passe au statut « Remplacé par … ».

| N° | Décision | Statut |
|---|---|---|
| [0001](0001-kotlin-compose-activite-unique.md) | Kotlin, Jetpack Compose, activité unique | Acceptée ; point « Navigation Compose » remplacé par 0011 |
| [0002](0002-modularisation.md) | Modularisation `app` / `core` / `feature` et plugins de convention | Acceptée |
| [0003](0003-domaine-en-kotlin-pur.md) | Domaine en Kotlin pur, développé en TDD | Acceptée ; point « JUnit 4 côté Android » remplacé par 0009 |
| [0004](0004-regles-nes-comme-reference.md) | Règles de la NES comme référence du mode Classique | Acceptée |
| [0005](0005-themes-hors-du-domaine.md) | Les thèmes restent hors du domaine | Acceptée |
| [0006](0006-boucle-de-jeu-a-pas-fixe.md) | Boucle de jeu à pas fixe de 60 ticks/s | Acceptée ; point « coroutine dans le ViewModel » remplacé par 0012 |
| [0007](0007-injection-manuelle.md) | Injection de dépendances manuelle | Acceptée ; précisée par 0013 |
| [0008](0008-musique-domaine-public.md) | Musique : domaine public et séquenceur maison | Acceptée |
| [0009](0009-junit5-partout.md) | JUnit 5 pour tous les tests, modules Android compris | Acceptée |
| [0010](0010-kotlin-integre-agp9.md) | Gradle 9, AGP 9 et Kotlin compilé par le support intégré d'AGP | Acceptée |
| [0011](0011-navigation-3.md) | Navigation 3 : la pile de navigation appartient à l'application | Acceptée ; point P2.10 consigné dans 0013 |
| [0012](0012-horloge-d-images-cote-ecran.md) | L'horloge d'images appartient à l'écran, le ViewModel ne fait qu'avancer | Acceptée |
| [0013](0013-fabriques-de-viewmodel-par-les-entrees.md) | Les fabriques de ViewModel entrent par les entrées de navigation | Acceptée |

Modèle pour un nouvel ADR : copier [0000-modele.md](0000-modele.md).
