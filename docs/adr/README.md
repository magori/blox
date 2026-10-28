# Décisions d'architecture (ADR)

Chaque décision structurante est consignée dans un fichier court : contexte, décision,
conséquences. Un ADR n'est jamais réécrit : s'il est remis en cause, un nouvel ADR le
remplace et l'ancien passe au statut « Remplacé par … ».

| N° | Décision | Statut |
|---|---|---|
| [0001](0001-kotlin-compose-activite-unique.md) | Kotlin, Jetpack Compose, activité unique | Acceptée ; point « Navigation Compose » remplacé par 0011 |
| [0002](0002-modularisation.md) | Modularisation `app` / `core` / `feature` et plugins de convention | Acceptée ; amendée par 0025 et 0027 |
| [0003](0003-domaine-en-kotlin-pur.md) | Domaine en Kotlin pur, développé en TDD | Acceptée ; point « JUnit 4 côté Android » remplacé par 0009 |
| [0004](0004-regles-nes-comme-reference.md) | Règles de la NES comme référence du mode Classique | Acceptée ; la promesse « Moderne sans toucher au moteur » est amendée par [0015](0015-un-moteur-des-capacites.md) |
| [0005](0005-themes-hors-du-domaine.md) | Les thèmes restent hors du domaine | Acceptée ; amendée par 0023 |
| [0006](0006-boucle-de-jeu-a-pas-fixe.md) | Boucle de jeu à pas fixe de 60 ticks/s | Acceptée ; point « coroutine dans le ViewModel » remplacé par 0012 |
| [0007](0007-injection-manuelle.md) | Injection de dépendances manuelle | Acceptée ; précisée par 0013 |
| [0008](0008-musique-domaine-public.md) | Musique : domaine public et séquenceur maison | Acceptée |
| [0009](0009-junit5-partout.md) | JUnit 5 pour tous les tests, modules Android compris | Acceptée |
| [0010](0010-kotlin-integre-agp9.md) | Gradle 9, AGP 9 et Kotlin compilé par le support intégré d'AGP | Acceptée |
| [0011](0011-navigation-3.md) | Navigation 3 : la pile de navigation appartient à l'application | Acceptée ; point P2.10 consigné dans 0013 ; fin de partie tranchée en P2.7 ; amendée par 0022 |
| [0012](0012-horloge-d-images-cote-ecran.md) | L'horloge d'images appartient à l'écran, le ViewModel ne fait qu'avancer | Acceptée ; complétée par 0024 ; amendée par 0028 |
| [0013](0013-fabriques-de-viewmodel-par-les-entrees.md) | Les fabriques de ViewModel entrent par les entrées de navigation | Acceptée ; amendée en P5.13 et P7.19 |
| [0014](0014-tirage-seul-etat-modifiable.md) | Le tirage des pièces est le seul état modifiable du domaine | Acceptée ; amendée par 0026 |
| [0015](0015-un-moteur-des-capacites.md) | Mode Moderne : un moteur, des capacités | Acceptée ; amende 0004 ; amendée par 0028 |
| [0016](0016-polices-embarquees.md) | Polices embarquées, chiffres à chasse fixe | Acceptée ; amendée par 0021 |
| [0017](0017-format-des-musiques-enregistrees.md) | Format des musiques enregistrées | Acceptée |
| [0018](0018-une-musique-partition-ou-fichier.md) | Une musique : partition ou fichier | Acceptée |
| [0019](0019-jetons-depuis-tokens-json.md) | Les jetons viennent de `tokens.json`, un test garde le code aligné | Acceptée |
| [0020](0020-un-theme-deux-variantes.md) | Un thème, des variantes ; Couleurs v2 remplace Couleurs ; une mise en page pour tous les thèmes | Acceptée |
| [0021](0021-polices-couleurs-v2.md) | Polices de Couleurs v2 | Acceptée ; amende 0016 |
| [0022](0022-choix-du-jeu-a-la-racine.md) | Choix du jeu : une feature à la racine de la pile | Acceptée ; amende 0011 ; amendée par 0025, 0026 et 0027 |
| [0023](0023-rendu-du-plateau-dans-core-board.md) | Le rendu du plateau dans `core:board` | Acceptée ; amende 0005 ; amendée le 2026-10-28 (noms de notes et d’accords, P10.19) |
| [0024](0024-retours-de-jeu-et-effets-reduits.md) | Retours de jeu cadencés par les ticks, dessinés sans recomposition ; Effets réduits portés par le thème | Acceptée ; complète 0012 |
| [0025](0025-le-serrurier.md) | Le Serrurier : un second jeu, `core:locksmith` et `feature:locksmith` | Acceptée ; amende 0002 et 0022 |
| [0026](0026-jeux-musicaux-variantes-du-classique.md) | Jeux musicaux : variantes du Classique et records (Musique vivante, Graine du jour, Au tempo) | Acceptée ; amende 0014 et 0022 |
| [0027](0027-le-jardin-musical.md) | Le Jardin musical : `core:garden` et `feature:garden` | Acceptée ; amende 0002 et 0022 |
| [0028](0028-au-tempo-droptiming-et-horloge-audio.md) | Au tempo : politique `DropTiming` et horloge audio comme source des ticks | Acceptée ; amende 0012 et 0015 |
| [0029](0029-feuilles-du-bas.md) | Feuilles du bas maison dans une fenêtre, ouvertes par l'état de l'écran | Acceptée ; complète 0012 et 0024 |

Modèle pour un nouvel ADR : copier [0000-modele.md](0000-modele.md).
