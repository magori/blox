# Règles du mode Classique

Le mode Classique reproduit les **règles de la version NES (1989)**. C'est la référence
choisie dans l'[ADR 0004](adr/0004-regles-nes-comme-reference.md) : ses règles sont
simples, bien documentées par la communauté, et définies en images (ticks), donc
directement testables.

> Statut des valeurs : les règles marquées **[à vérifier]** viennent de mémoire et doivent
> être confrontées à une source de référence (par exemple le wiki communautaire *TetrisWiki*,
> pages « Tetris (NES) ») avant d'écrire le test correspondant. Les autres sont des valeurs
> largement citées et stables.

## Ce que le mode Classique n'a pas

Pas de réserve (*hold*), pas de pièce fantôme, pas de chute instantanée (*hard drop*),
pas de rotation avec décalage contre les murs (*wall kicks*), pas de délai de verrouillage
réinitialisable, pas de T-spin. C'est ce qui le distingue du mode Moderne.

## Plateau

- Grille de **10 colonnes × 20 lignes**, toutes visibles.
- Les pièces apparaissent centrées en haut du plateau.
- **Fin de partie** : la nouvelle pièce ne peut pas apparaître parce que sa position de départ
  est occupée.

## Pièces

Sept pièces de quatre cellules : `I`, `O`, `T`, `S`, `Z`, `J`, `L`.

- **Orientation de départ** : pièces « à plat ». Le T apparaît pointe vers le bas.
  L'orientation exacte de J et L au départ est **[à vérifier]**.
- La pièce suivante est affichée dans un aperçu (une seule).

## Tirage des pièces

Générateur de la NES, **[à vérifier]** dans le détail :

1. Tirer un nombre de 0 à 7.
2. Si on obtient 7 (« aucune pièce ») ou la même pièce que la précédente, tirer à nouveau
   **une seule fois** un nombre de 0 à 6, et garder ce résultat quel qu'il soit.

Conséquence : les répétitions sont rares mais possibles, et il n'y a aucune garantie de
voir chaque pièce régulièrement (contrairement au « sac de 7 » du mode Moderne).

Le générateur reçoit sa source d'aléatoire en dépendance (`Randomizer` + graine) pour que
les tests soient déterministes.

## Rotation

Système de rotation de la NES :

- Bouton **A** : rotation horaire. Bouton **B** : rotation anti-horaire.
- **Aucun décalage** : si la position après rotation est occupée ou hors du plateau, la
  rotation est simplement refusée.
- `O` ne tourne pas. `I`, `S` et `Z` n'ont que **deux états** (ils basculent entre les deux).
  `T`, `J`, `L` ont quatre états.
- Les centres de rotation exacts de chaque pièce sont **[à vérifier]** : c'est la règle la
  plus subtile à reproduire fidèlement, elle mérite ses propres tests pièce par pièce.

## Gravité

La pièce descend d'une ligne toutes les *N* images, selon le niveau :

| Niveau | Images par ligne |
|---|---|
| 0 | 48 |
| 1 | 43 |
| 2 | 38 |
| 3 | 33 |
| 4 | 28 |
| 5 | 23 |
| 6 | 18 |
| 7 | 13 |
| 8 | 8 |
| 9 | 6 |
| 10 – 12 | 5 |
| 13 – 15 | 4 |
| 16 – 18 | 3 |
| 19 – 28 | 2 |
| 29 et plus | 1 |

1 image = 1 tick = 1/60 s (la NES tourne en réalité à environ 60,1 images/s, l'écart est
négligeable).

## Verrouillage

Pas de délai de verrouillage : quand la gravité devrait faire descendre la pièce et que la
case en dessous est occupée (ou que c'est le fond), la pièce **se verrouille immédiatement**.

## Commandes

| Action | Effet |
|---|---|
| ◀ / ▶ | Déplacement d'une colonne. Maintenu : répétition automatique (DAS). |
| ▼ | Descente rapide : la pièce descend d'une ligne toutes les **2 images** **[à vérifier]**. |
| A | Rotation horaire. |
| B | Rotation anti-horaire. |
| Pause | Met en pause et **masque le plateau** (sinon la pause sert à réfléchir). |

**Répétition automatique (DAS)** : après un appui maintenu, premier déplacement immédiat,
puis attente de **16 images**, puis un déplacement toutes les **6 images**.

## Délai d'apparition

Entre le verrouillage d'une pièce et l'apparition de la suivante, la NES attend de
**10 à 18 images** selon la hauteur à laquelle la pièce s'est verrouillée, plus la durée de
l'animation quand des lignes sont effacées **[à vérifier]**. Ces valeurs seront des
paramètres de la règle, pour pouvoir les ajuster.

## Lignes et score

Une ligne pleine est effacée, les lignes au-dessus descendent d'autant.

| Lignes effacées d'un coup | Points |
|---|---|
| 1 | 40 × (niveau + 1) |
| 2 | 100 × (niveau + 1) |
| 3 | 300 × (niveau + 1) |
| 4 | 1200 × (niveau + 1) |

**Descente rapide** : 1 point par ligne descendue en maintenant ▼. C'est une
simplification assumée : le calcul réel de la NES est plus particulier **[à vérifier]**.

## Niveaux

- Le joueur choisit un **niveau de départ** de 0 à 9.
- Premier passage de niveau quand le total de lignes atteint
  `min(départ × 10 + 10, max(100, départ × 10 − 50))`.
- Ensuite, un niveau de plus toutes les **10 lignes**.

Exemples : départ 0 → niveau 1 à 10 lignes ; départ 5 → niveau 6 à 60 lignes ;
départ 9 → niveau 10 à 100 lignes.

## Statistiques de fin de partie

Score, lignes, niveau atteint, nombre de « 4 lignes » d'un coup, durée de la partie.
