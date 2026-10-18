# 0008 — Musique : domaine public et séquenceur maison

- **Statut** : Acceptée
- **Date** : 2026-10-18

## Contexte

La musique associée aux jeux de blocs de 1989 combine des mélodies anciennes (domaine public)
et des arrangements protégés. Le projet est à usage personnel, mais le dépôt ne doit contenir
que ce qu'on a le droit d'y mettre.

## Décision

- Les morceaux intégrés sont des **mélodies du domaine public** (*Korobeïniki*, *Menuet en sol*)
  dans des **arrangements écrits pour BloX**.
- Ils sont joués par un **séquenceur chiptune maison** (`core:music` pour les partitions,
  `core:audio` pour la synthèse via `AudioTrack`).
- L'utilisateur peut choisir **son propre fichier audio** sur l'appareil ; il n'est jamais
  versionné.

## Conséquences

- Aucun fichier audio lourd dans l'APK : une partition pèse quelques centaines d'octets.
- Le son a un caractère 8 bits cohérent avec le thème Original.
- Le tempo est un paramètre : l'accélération quand la pile monte est triviale.
- Il faut écrire et tester un petit synthétiseur (génération d'échantillons, enveloppe,
  mixage de deux voix).
