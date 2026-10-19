# 0017 — Format des musiques enregistrées

- **Statut** : Acceptée
- **Date** : 2026-10-19

## Contexte

Les musiques que le joueur compose ou génère (P4.19, P4.22) doivent survivre aux mises à jour
de l'application et à la sauvegarde Android : ce sont des créations, pas un réglage qu'on
retrouve en deux touchers. `core:data` garde déjà records et réglages dans DataStore
(Preferences). Une musique est un arbre (partition, voix, notes) que Preferences porte mal.

Options envisagées :

- JSON ou protobuf par `kotlinx.serialization` : il faudrait un format (seul
  `kotlinx-serialization-core` est au catalogue, pour les clés de navigation) et des objets de
  transfert, puisque `core:music` ne peut porter aucune annotation (aucune dépendance) ;
- un format binaire maison sur le DataStore générique et Okio, déjà présents.

## Décision

- **Format binaire maison, `files/datastore/musics.bin`** (`MusicLibraryFile`), lu et écrit par
  un DataStore générique : en-tête « BLXM », numéro de version (1), puis la musique jouée
  pendant les parties et la liste des musiques (identité, nom, catégorie, gamme, partition).
  Aucune nouvelle dépendance.
- **Les enums sont écrits par leur nom.** Leurs noms sont figés par un test ; une catégorie
  inconnue se lit sans catégorie, un type de gamme ou une onde inconnus rendent le fichier
  corrompu.
- **Écriture symétrique de la lecture.** Un nom compte 40 caractères au plus ; toute liste ou
  chaîne au-delà de 65 536 éléments est refusée avant d'écrire le moindre octet, si bien qu'un
  fichier écrit se relit toujours.
- **Corruption** (en-tête absent, fichier tronqué, version inférieure à 1, identité en double,
  contenu invalide) : le fichier est copié dans `musics.bin.corrupt`, puis la liste repart vide.
  Une musique des parties qui ne désigne aucune musique est oubliée.
- **Version plus récente** (fichier restauré d'un BloX plus récent) : lu comme une liste vide,
  jamais écrasé ; les écritures échouent par une `IOException`.
- Seules les `IOException` sont rattrapées à la lecture, comme pour les autres dépôts.
- Le fichier entre dans la sauvegarde Android (listes blanches de P5.28) : il ne contient aucune
  URI de contenu.

## Conséquences

- Changer le format demande une version 2 et la lecture des deux versions. Deux fichiers v1
  écrits à la main sont figés par des tests.
- Renommer une constante de `Category`, `ScaleType` ou `Waveform` casse le test des noms
  figés : il faut alors garder l'ancien nom à la lecture.
- Tests : `DataStoreMusicLibraryTest` (format, corruption, version, bornes) et le contrat
  `MusicLibraryContract`, joué par le dépôt réel et par son faux.
