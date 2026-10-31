# 0008 — Musique : domaine public et séquenceur maison

- **Statut** : Acceptée ; amendée par [0031](0031-sonnerie-et-reveil.md) et [0032](0032-instruments.md)
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

## Note (2026-10-31, P13.1 et P14.1)

- Amendée par [0031](0031-sonnerie-et-reveil.md) : BloX écrit aussi des fichiers audio, à la demande du joueur, une
  de ses musiques rendue en WAV comme sonnerie ou réveil, hors de l'APK et du dépôt.
- Amendée par [0032](0032-instruments.md) : le « caractère 8 bits » devient celui d'Arcade, l'instrument par
  défaut ; Velours et Néon sont d'autres timbres, toujours synthétisés.
