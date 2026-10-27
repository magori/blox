# 0029 — Feuilles du bas maison dans une fenêtre, ouvertes par l'état de l'écran

- **Statut** : Acceptée ; complète [0012](0012-horloge-d-images-cote-ecran.md) et [0024](0024-retours-de-jeu-et-effets-reduits.md)
- **Date** : 2026-10-27

## Contexte

Couleurs v2 fait monter trois feuilles du bas : la musique de l'accueil (P5.76), le dé de l'éditeur
(P5.101) et Enregistrer (P5.104). Enregistrer doit rester au-dessus du clavier. Une feuille suit les jetons du
design system (`scrim`, `radius-card`, `motion-card`) et les Effets réduits (ADR 0024). Le `ModalBottomSheet` de
Material 3 impose son animation, sa poignée, sa forme et son glissement, ignore `BloxEffects`, rend des captures
instables et gère le clavier à sa façon.

## Décision

1. Un seul composant, `BloxSheet` (`core:designsystem`, P5.96), posé dans une `Dialog` plein écran : TalkBack et
   le focus restent dans la feuille, lue comme une fenêtre titrée ; le retour du système la ferme ; elle reste
   au-dessus du clavier et des barres système (encarts `safeDrawing`), son contenu défile et sa rangée d'actions
   reste visible.
2. Le voile et l'entrée sont dessinés par BloX, sur l'horloge de Compose (`withFrameNanos`, `FrameTicks`), lus au
   dessin seulement : montée en `motion-card`, fondu de `motion-reduced-fade` en Effets réduits. L'animation de
   fenêtre de la plateforme et son assombrissement sont coupés.
3. Pas de glissement pour fermer : le voile, le retour et les boutons suffisent ; moins de gestes à tester.
4. L'ouverture suit la règle des fenêtres : le ViewModel la porte quand elle a des règles (Enregistrer) ; l'écran
   la porte sinon (`rememberSaveable` : dé, feuille Musique).
5. Les rangées de choix d'une feuille sont des `SheetChoiceRow`, groupées par l'appelant en `selectableGroup`.

## Conséquences

- Un comportement identique dans les trois écrans, des captures déterministes.
- Pas de geste de glissement : écart avec les habitudes d'Android, connu et voulu.
- Le voile n'est pas atteint par TalkBack ; on ferme par le retour du système.
- Le clavier se vérifie sur téléphone : aucun test instrumenté fiable ne fait monter l'IME (les tests injectent un
  encart).
