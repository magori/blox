# 0006 — Boucle de jeu à pas fixe de 60 ticks/s

- **Statut** : Acceptée
- **Date** : 2026-10-18

## Contexte

Les téléphones affichent à 60, 90 ou 120 Hz, et la cadence peut varier. Si le jeu avançait
d'une étape par image affichée, sa vitesse dépendrait de l'écran.

## Décision

Le moteur avance par **ticks de 1/60 s**. La boucle de jeu, côté application, mesure le temps
réel avec `withFrameNanos`, l'accumule, et appelle `tick()` autant de fois que nécessaire
(au plus quelques ticks par image pour éviter l'emballement après une pause système). Le rendu
affiche le dernier état.

## Conséquences

- La vitesse du jeu est identique sur tous les écrans.
- Le domaine ne connaît pas le temps réel : les tests avancent le jeu tick par tick.
- La boucle elle-même (accumulateur) est une petite classe pure, testable avec une horloge
  factice.
