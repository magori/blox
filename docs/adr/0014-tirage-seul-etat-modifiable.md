# 0014 — Le tirage des pièces est le seul état modifiable du domaine

- **Statut** : Acceptée
- **Date** : 2026-10-18

## Contexte

Le domaine est immuable : `Board`, `Piece`, `Level`, `Lines` et les politiques sont des valeurs,
et chaque opération renvoie un nouvel état (CLAUDE.md, [ADR 0003](0003-domaine-en-kotlin-pur.md)).
Le tirage des pièces (P1.8) a besoin d'une source d'aléatoire, qui change à chaque tirage.

Deux options :

- **Forme pure** : `next(previous, rngState): Pair<PieceType, RngState>`, l'état du générateur
  rangé dans `GameState`. Un état de partie se capture et se restaure tel quel.
- **Générateur à état** : `ClassicRandomizer(seed)` garde un `kotlin.random.Random` privé,
  `next(previous)` le fait avancer.

Aucune tâche ne demande de sauvegarder ni de reprendre une partie en cours.

## Décision

Le générateur de pièces garde son état aléatoire : c'est le **seul état modifiable du
domaine**. Il appartient au moteur (P1.12), et **un moteur joue une seule partie** : une
fabrique (`GameEngineFactory.create(seed)`) crée le moteur et son générateur depuis la graine
de la partie. On ne repasse au moteur que le dernier état qu'il a rendu. Un état de partie ne
se reconstruit qu'en rejouant depuis la graine. (Précisé en P1.12a : un premier jet injectait
le générateur dans un moteur réutilisable, si bien qu'une deuxième partie continuait le
tirage de la première.)

## Conséquences

- L'API reste simple (`next(previous)`) et la pièce précédente est passée en paramètre, sans
  état caché au-delà de la source d'aléatoire. Un sac de 7 (mode Moderne, P6) aura lui aussi
  un état et suivra la même règle.
- On ne peut pas capturer un `GameState` puis le restaurer à l'identique. Si une sauvegarde ou
  une reprise de partie devient un besoin, passer à la forme pure, et remplacer cet ADR.
- La suite de `kotlin.random.Random(seed)` n'est pas un contrat entre versions de Kotlin : le
  jour où une graine est gardée ou partagée (partie rejouée, défi du jour), il faudra un
  générateur maison et un test qui fige les premières pièces d'une graine (tâche P1.20).

## Amendement (P5.13, 2026-10-19)

Une partie ne survit pas à la mort du processus : la pile de navigation restaure la `GameRoute`,
et un ViewModel neuf rejoue la partie de la route depuis sa graine, en pause et sans musique,
d'après un simple marqueur « partie commencée » rangé dans le `SavedStateHandle` de l'entrée.
Aucun état du moteur n'est sauvé ; le score d'une partie terminée l'a été dès la fin de partie,
et n'est pas soumis à nouveau. L'écran de fin n'est pas restauré.
