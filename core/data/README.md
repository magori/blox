# core:data — persistance locale

Module **Android** : couche de données au sens des recommandations Android
(repositories qui exposent des `Flow`, sources de données privées).

## Contenu prévu

- `UserPreferencesRepository` : thème, musique choisie, dernier niveau de départ. Stocké avec
  DataStore Preferences.
- `HighScoreRepository` : meilleur score, et plus tard un tableau des records par mode.

Les modèles exposés sont des classes simples propres à ce module. Les features les
convertissent vers leur état d'écran.
