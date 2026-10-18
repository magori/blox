---
description: Clôturer une fonctionnalité selon la définition de terminé
---

Clôture la tâche en cours ($ARGUMENTS si un identifiant est donné) en suivant la section
« Définition de terminé » de `CLAUDE.md`, étape par étape, sans en sauter aucune.
Arrête-toi et signale-le si une étape échoue.

1. **Tests verts** : lance `./gradlew ktlintCheck buildHealth :core:game:test :core:music:test testDebugUnitTest`,
   `./gradlew -p build-logic ktlintCheck`, et
   `./gradlew lintDebug assembleDebug` si un module Android a changé. Montre le résultat.

2. **Revue indépendante** : lance un sous-agent qui n'a pas le contexte de cette conversation.
   Donne-lui uniquement : le diff de la tâche par rapport à `main`, `CLAUDE.md`, et les documents
   de `docs/` concernés. Demande-lui une revue franche, classée en *bloquant*, *à corriger*,
   *suggestion*, sans modifier de fichier.
   Présente-moi ses remarques avec ta recommandation pour chacune (retenir ou écarter, et
   pourquoi), puis attends ma validation.
   Corrige les remarques retenues **test d'abord**, puis relance l'étape 1.

3. **Spécification** : mets à jour les règles touchées, avec les tests qui les prouvent
   (`ClasseDeTest › nom du test`), les vues de maquette (`V…`), et retire les [à vérifier]
   confirmés en citant la source.

4. **Maquette** : si un écran a changé, mets à jour `docs/maquette.md` (renumérote si besoin, et
   reporte les nouveaux numéros partout) et ajoute dans `SUIVI.md` une tâche
   « Mettre à jour la maquette : … » pour la maquette Claude Design.

5. **ADR** si une décision structurante a été prise.

6. **SUIVI.md et JOURNAL.md** : dans `SUIVI.md`, coche la tâche et ajoute les tâches
   découvertes, puis lance `scripts/avancement.sh` (Git Bash) pour recalculer les pourcentages
   général et MVP ; si une tâche découverte est nécessaire pour jouer, propose de l'ajouter à
   la liste `<!-- mvp: … -->`. Dans `JOURNAL.md`, ajoute la ligne de la tâche (y compris les
   remarques de revue écartées).

7. **Commit et push** : rédige et crée le commit avec le skill `/commit` (gitmoji +
   Conventional Commits, corps qui explique le pourquoi, `Tâche : P…`) sur la branche de la
   tâche, pousse, et donne-moi le lien pour suivre la CI. Ne fusionne pas dans `main` sans
   mon accord.
