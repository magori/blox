# 0019 — Les jetons viennent de `tokens.json`, un test garde le code aligné

- **Statut** : Acceptée
- **Date** : 2026-10-25

## Contexte

Le design system Couleurs v2 est publié sur claude.ai et copié dans
`docs/design-system/tokens.json` ; il est modifié des deux côtés. La palette du code porte des
raisons écrites (contrastes dans les KDoc) et des structures dérivées (`TrackColors`,
`LogoColors`). Original et Brume ne sont pas dans ce fichier. Générer le code demanderait une
tâche `build-logic` qui lise aussi les valeurs CSS (`rgba`, ombres), et cacherait le code produit.

## Décision

- Le code des jetons reste **écrit à la main** (`BloxPalette`, `BloxTypography`, `BloxShapes`,
  `BloxSizes`, styles de case dans `core:board`).
- Dans chaque module concerné, un **test JVM lit `tokens.json`** et compare chaque jeton à sa
  propriété par une table explicite (nom kebab-case → accès). Il échoue aussi sur un jeton qui
  n'est ni codé ni listé « non codé », avec sa raison.
- La convention `blox.design.tokens` déclare le fichier en **entrée des tâches de test**, par un
  chemin relatif (`CommandLineArgumentProvider`), pour que le cache reste relocalisable (P5.31).
- Lecture par `kotlinx-serialization-json`, en `testImplementation` seulement.

## Conséquences

- Une valeur changée d'un seul côté fait échouer la CI ; un test vert n'est jamais repris du cache
  après une modification du fichier.
- Les ombres CSS sont comparées par une petite grammaire, ou listées « non codées » tant qu'elles
  ne sont pas dessinées.
- Le design system publié et sa copie locale restent la seule source : une valeur ne s'écrit
  jamais d'abord dans le code.
