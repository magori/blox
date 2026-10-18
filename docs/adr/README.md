# Décisions d'architecture (ADR)

Chaque décision structurante est consignée dans un fichier court : contexte, décision,
conséquences. Un ADR n'est jamais réécrit : s'il est remis en cause, un nouvel ADR le
remplace et l'ancien passe au statut « Remplacé par … ».

| N° | Décision | Statut |
|---|---|---|
| [0001](0001-kotlin-compose-activite-unique.md) | Kotlin, Jetpack Compose, activité unique | Acceptée |
| [0002](0002-modularisation.md) | Modularisation `app` / `core` / `feature` et plugins de convention | Acceptée |
| [0003](0003-domaine-en-kotlin-pur.md) | Domaine en Kotlin pur, développé en TDD | Acceptée |
| [0004](0004-regles-nes-comme-reference.md) | Règles de la NES comme référence du mode Classique | Acceptée |
| [0005](0005-themes-hors-du-domaine.md) | Les thèmes restent hors du domaine | Acceptée |
| [0006](0006-boucle-de-jeu-a-pas-fixe.md) | Boucle de jeu à pas fixe de 60 ticks/s | Acceptée |
| [0007](0007-injection-manuelle.md) | Injection de dépendances manuelle | Acceptée |
| [0008](0008-musique-domaine-public.md) | Musique : domaine public et séquenceur maison | Acceptée |
| [0010](0010-kotlin-integre-agp9.md) | Gradle 9, AGP 9 et Kotlin compilé par le support intégré d'AGP | Acceptée |

Modèle pour un nouvel ADR : copier [0000-modele.md](0000-modele.md).
