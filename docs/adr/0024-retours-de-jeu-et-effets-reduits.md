# 0024 — Retours de jeu cadencés par les ticks, dessinés sans recomposition ; Effets réduits portés par le thème

- **Statut** : Acceptée ; complète [0012](0012-horloge-d-images-cote-ecran.md)
- **Date** : 2026-10-25

## Contexte

Couleurs v2 ajoute des retours visuels à chaque événement du moteur (pose, effacement, 4 lignes,
niveau, danger, record battu), une pause en rideau et une fin de partie animée, chacun avec sa
version réduite (réglage « Effets réduits », imposé quand Android supprime les animations). Le
rendu actuel ne recompose pas l'écran quand la pièce bouge (P5.11) ; les effets ne doivent pas
défaire ce travail, ni retarder la partie au-delà des délais NES.

## Décision

1. Les effets **en partie** sont des données de l'état d'écran : `GameFeedback` (pur) dit « tel
   événement au tick T » ; `GameUiState` porte le tick et l'effacement en cours (tiré de
   `Phase.Clearing.ticksLeft`, donc calé sur le délai NES). Une fonction pure donne, pour un tick,
   les décalages, opacités et teintes. La pause fige les effets d'elle-même.
2. L'écran ne lit ces valeurs **que dans les lambdas de dessin** (`drawWithCache`,
   `onDrawBehind`, `graphicsLayer {}`, `offset {}`), jamais en paramètre d'un composable.
3. Seule exception : un texte qui compte (le score) ne recompose que son propre `Text`, pendant la
   durée de l'effet, et un test le mesure.
4. Les transitions **hors partie** (rideau, cartes, extinction de la pile, confettis) tournent sur
   l'horloge de Compose (`Animatable`), puisque la boucle de jeu est arrêtée ; l'écran décide du
   moment de la reprise (Reprendre après l'ouverture du rideau).
5. **`BloxEffects(reduced, forcedBySystem)`** est fourni par `BloxTheme`, résolu par une fonction
   pure depuis la préférence et `ValueAnimator.areAnimatorsEnabled()` ; le ViewModel l'ignore.

## Conséquences

- Les effets se testent sur la JVM ; les tests de recomposition (`RecompositionRecorder`)
  s'étendent aux effets.
- Aucun effet ne retarde la partie ; rien ne clignote plus de 3 fois par seconde.
