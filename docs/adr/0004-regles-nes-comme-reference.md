# 0004 — Règles de la NES comme référence du mode Classique

- **Statut** : Acceptée
- **Date** : 2026-10-18

## Contexte

« Les règles d'origine » ne désignent pas un seul jeu : les versions NES et Game Boy de 1989
diffèrent (tirage des pièces, vitesses, score), et les versions modernes ajoutent réserve,
pièce fantôme et sac de 7. Il faut une référence précise pour écrire des tests.

## Décision

Le mode Classique suit les **règles de la version NES de 1989** : tirage avec un seul nouveau
tirage, rotation sans décalage, gravité en images par ligne, DAS 16/6, score 40/100/300/1200,
progression de niveau de la NES. Le détail est dans
[regles-mode-classique.md](../regles-mode-classique.md).

L'ambiance visuelle du thème Original s'inspire, elle, des consoles portables monochromes :
règles NES et apparence portable sont deux choses indépendantes.

## Conséquences

- Les règles sont définies en images, donc testables tick par tick.
- Chaque règle est une politique interchangeable dans `core:game` : le mode Moderne fournira
  d'autres implémentations (SRS, sac de 7, délai de verrouillage) sans toucher au moteur.
- Certaines valeurs sont marquées « à vérifier » dans la documentation : on les confirme sur
  une source de référence avant d'écrire le test correspondant.
- Aucun nom, logo, visuel ou son de la marque d'origine n'est utilisé.
