---
description: Revue de code des changements en cours
---

Fais une revue des changements non commités (ou de $ARGUMENTS s'il s'agit d'une branche ou
d'un fichier), selon les conventions de `CLAUDE.md` :

- Respect des règles de dépendance entre modules et de la pureté de `core:game` / `core:music`.
- Qualité des tests : un comportement par test, noms en phrases, cas limites couverts,
  pas de test qui duplique un autre.
- Lisibilité et nommage (langage du domaine), immuabilité, duplication (règle de 2).
- Conformité aux règles documentées dans `docs/`.

Classe les remarques par importance (bloquant, à corriger, suggestion) et sois franc.
Ne modifie aucun fichier sans mon accord.
