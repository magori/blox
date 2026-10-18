# 0007 — Injection de dépendances manuelle

- **Statut** : Acceptée ; précisée par [0013](0013-fabriques-de-viewmodel-par-les-entrees.md) pour le passage des fabriques aux features
- **Date** : 2026-10-18

## Contexte

Android recommande Hilt pour l'injection de dépendances, et documente aussi l'injection
manuelle pour les petites applications. BloX aura une poignée de dépendances : deux
repositories, un lecteur de musique, un générateur de pièces.

## Décision

Injection **manuelle** : un `AppContainer` créé dans une classe `Application` construit les
dépendances ; les ViewModels les reçoivent par constructeur, via une
`ViewModelProvider.Factory` (`viewModelFactory { initializer { … } }`).

## Conséquences

- Pas de génération de code ni de KSP : build plus simple et plus rapide.
- Toutes les classes reçoivent leurs dépendances par constructeur : la migration vers Hilt,
  si le projet grossit, consiste à ajouter des annotations.
- À revoir si le nombre de dépendances ou d'écrans augmente nettement.
