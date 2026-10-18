# 0015 — Mode Moderne : un moteur, des capacités

- **Statut** : Acceptée
- **Date** : 2026-10-18
- **Amende** : [0004](0004-regles-nes-comme-reference.md) (conséquence « le mode Moderne
  fournira d'autres implémentations sans toucher au moteur »)

## Contexte

L'ADR 0004 promettait que le mode Moderne s'ajouterait par de nouvelles implémentations des
politiques, sans toucher au moteur. L'[analyse du mode Moderne](../analyse-mode-moderne.md) montre
que cette promesse ne tient pas. Cinq éléments s'ajoutent bien sans modifier le moteur (sac de 7,
rotation SRS, position d'apparition, niveaux, pièce fantôme), mais les autres le touchent :

- délai de verrouillage, réserve, chute instantanée, file de pièces suivantes ;
- gravité de plus d'une ligne par tick ;
- score contextuel : T-spin, combos, enchaînements ;
- règles de la manette NES, aujourd'hui codées en dur dans le moteur.

Trois options ont été envisagées :

1. **Un moteur à capacités** : `Rules` déclare ce que le mode permet, et les comportements codés en
   dur deviennent des politiques.
2. **Deux moteurs** partageant `model` et `rules` : le Classique ne bouge plus, mais la chute, le
   verrouillage et l'effacement sont dupliqués, et le ViewModel doit abstraire deux états.
3. **Un moteur générique à étapes branchables** : le plus souple, mais le plus abstrait pour deux
   modes seulement.

## Décision

Un **seul moteur** sert les deux modes. Ses **capacités sont déclarées dans `Rules`** :

- réserve ;
- chute instantanée ;
- nombre de pièces suivantes.

Les comportements aujourd'hui codés en dur deviennent des **politiques**, avec une implémentation
`Classic*` neutre et une `Modern*` :

- `ControlScheme`, pour les règles de la manette ;
- `LockDelay` ;
- `LineClearDelay` ;
- `TopOutRule`.

Les règles du mode Moderne sont décrites dans [regles-mode-moderne.md](../regles-mode-moderne.md).

Pas d'énumération `Mode` dans `core:game` : `Rules` ne porte que des capacités et des politiques.

## Conséquences

- **Le moteur sera modifié**, par étapes : chaque politique est d'abord extraite sous les tests
  existants (refactoring), avec une implémentation Classique qui garde le comportement d'avant,
  puis le Moderne s'ajoute. Les tests du Classique restent le filet de sécurité, et chaque tâche
  de P6 le laisse vert.
- `GameState`, `GameProgress`, `Phase.Falling` et certains contrats évolueront :
  - `GameState` pour la file de pièces suivantes et la réserve ;
  - `GameProgress` pour le combo et l'enchaînement ;
  - `Phase.Falling` pour le délai de verrouillage ;
  - `ScoringRule`, `GravityTable`, et `GameEvent` (de nouveaux événements, dont chaque `when`
    exhaustif devra traiter les nouveaux cas).
- `ModernRules` et `ModernGameEngineFactory` s'ajoutent. `app` choisit la fabrique selon le mode
  enregistré (ADR 0013 inchangé).
- Les principes de l'ADR 0004 restent :
  - le Classique suit la NES ;
  - les règles sont définies en ticks ;
  - les valeurs non sourcées sont marquées « à vérifier » ;
  - aucune marque n'est utilisée.
- Risque à surveiller : un moteur qui se remplit de conditions par mode. Chaque différence passe
  par une politique ou une capacité de `Rules`, jamais par un test du mode dans le moteur.
