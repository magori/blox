---
description: Revue de code indépendante des changements en cours
---

Lance un **sous-agent** pour une revue indépendante : il ne doit pas avoir le contexte de cette
conversation, pour juger le code sans connaître les intentions de l'implémentation.

Donne-lui uniquement :
- le diff à revoir : $ARGUMENTS s'il s'agit d'une branche, d'un commit ou d'un fichier ;
  sinon le diff de la branche courante par rapport à `main`, changements non commités compris ;
- `CLAUDE.md` ;
- les documents de `docs/` qui concernent les fichiers modifiés.

Points à examiner :
- règles de dépendance entre modules, pureté de `core:game`, `core:music`, `core:locksmith` et `core:garden` (ces deux derniers ne dépendent que des deux premiers, en `api`) ;
- conformité au comportement décrit dans la spécification ;
- qualité des tests : un comportement par test, noms en phrases, cas limites, pas de doublon,
  pas de test qui teste l'implémentation plutôt que le comportement ;
- lisibilité, langage du domaine, immuabilité, duplication (règle de 2).

Le sous-agent classe ses remarques en *bloquant*, *à corriger*, *suggestion*, est franc, et ne
modifie aucun fichier. Rapporte-moi ses remarques telles quelles, puis ta recommandation pour
chacune.
