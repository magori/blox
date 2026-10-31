# 0031 — Sonnerie ou réveil : une musique du joueur rendue en WAV, écrite par MediaStore

- **Statut** : Acceptée ; amende [0008](0008-musique-domaine-public.md)
- **Date** : 2026-10-31

## Contexte

Le développeur demande (2026-10-31) qu'une musique composée dans BloX puisse devenir la sonnerie ou le son de
réveil du téléphone. Aujourd'hui, aucune musique ne sort de l'application : une partition est synthétisée à la
volée par `Synthesizer` (`core:audio`, 44 100 Hz) et jouée en boucle par `ChiptuneSequencer` (ADR 0008). L'ADR
0008 ne prévoit de fichier audio que dans un sens : celui que le joueur choisit sur son appareil (« Mon fichier »).

Contraintes et options :

- **Le format.** Android lit WAV, OGG, MP3 et AAC comme sonnerie. Aucun encodeur Vorbis n'est disponible sans
  bibliothèque ; AAC passe par `MediaCodec`, testable seulement sur appareil. Un WAV PCM s'écrit en quelques lignes
  de Kotlin pur, testées sur la JVM ; il pèse 88 Ko par seconde en mono 16 bits à 44,1 kHz, soit 5,3 Mo au plus
  pour 60 s.
- **La longueur.** Le système rejoue une sonnerie et un réveil en boucle tant qu'on n'a pas répondu. Une musique
  de BloX est elle-même une boucle (de 6 à 30 s environ) : un fichier fait de boucles entières se raccorde sans
  couture, sans fondu. Trop court, il se répète trop vite ; trop long, il pèse pour rien.
- **Le niveau.** Les musiques culminent vers 0,23 (environ −13 dBFS) : à ce niveau, une sonnerie s'entend mal à côté
  de celles du système.
- **L'écriture.** Depuis Android 10 (API 29), `MediaStore` accepte qu'une application écrive un fichier audio dans
  `Ringtones/` ou `Alarms/` sans aucune permission, et le marque comme sonnerie (`IS_RINGTONE`) ou réveil
  (`IS_ALARM`). Avant l'API 29, il faut `WRITE_EXTERNAL_STORAGE` (permission à demander) ou le sélecteur de
  documents (`ACTION_CREATE_DOCUMENT`, où le joueur choisit le dossier, et le fichier n'est pas toujours vu comme
  une sonnerie). `minSdk` vaut 26.
- **Appliquer la sonnerie.** `RingtoneManager.setActualDefaultRingtoneUri` exige `WRITE_SETTINGS`, une permission
  spéciale que le joueur accorde dans un écran à part, et que les boutiques regardent de près. Le réveil, lui, se
  choisit dans l'application Horloge, réveil par réveil : aucune API publique ne le règle.
- **Les usages.** Sonnerie (appels), réveil, notification. Une notification dure moins de 3 s et ne boucle pas : une
  boucle de musique de 13 s n'y convient pas, coupée au milieu d'une phrase.
- **Ce qu'on exporte.** Korobeïniki porte encore une confirmation des droits ouverte (P4.10) et reste associée au jeu
  de 1989 ; le Menuet est libre, mais n'est pas une création du joueur. Les musiques composées par un jeu (Musique
  vivante, Jardin, accompagnement d'Au tempo) ne sont pas enregistrées comme des musiques.

## Décision

1. **Périmètre** : seules les musiques de « Mes musiques » (`SavedMusic`, `MusicLibrary`) s'exportent. Ni
   Korobeïniki, ni le Menuet, ni les musiques composées par un jeu. Deux usages : **Sonnerie** et **Réveil** ; pas
   de Notification.
2. **Rendu** : le même `Synthesizer` que le séquenceur, à `MUSIC_SAMPLE_RATE` (44 100 Hz, rendu public dans
   `core:audio`) ; le fichier sonne exactement comme la musique dans BloX, dans l'instrument choisi (ADR 0032).
3. **Longueur** : des **boucles entières**, assez pour durer **au moins 20 s** ; **jamais plus de 60 s** (5,3 Mo
   au plus). Une boucle plus longue que 60 s est coupée à sa **dernière mesure entière avant 60 s** : le système
   reprend alors au début d'une mesure, non de la phrase. Sans fondu : le système boucle le fichier.
4. **Niveau** : la crête du fichier est normalisée à **−1 dBFS** ; un rendu silencieux reste silencieux.
5. **Format** : **WAV PCM 16 bits, mono, 44 100 Hz**, encodé par du Kotlin pur dans `core:audio` (`WavEncoder`).
6. **Écriture** : `RingtoneExporter` (interface de `core:audio`, `suspend fun export(…): Uri`, `IOException` en cas
   d'échec), implémenté par `MediaStoreRingtoneExporter` (**API 29 et plus**) : collection `Audio.Media` du volume
   principal, `RELATIVE_PATH` `Ringtones/BloX/` ou `Alarms/BloX/`, `IS_RINGTONE` ou `IS_ALARM`, `TITLE` au nom de
   la musique (ce que montre le sélecteur du système), `IS_PENDING` pendant l'écriture ; une écriture qui échoue
   supprime l'entrée. Les appels à `MediaStore` passent par une petite interface d'écriture (insertion, flux,
   publication, suppression), pour que l'échec et le nettoyage se testent aussi sur la JVM. `FakeRingtoneExporter`
   dans les doublures de test de `core:audio`. **Chaque export crée un nouveau fichier** ; `MediaStore` ajoute
   « (1) » à un nom déjà pris, et le titre suit le nom retenu (« Nom (1) »), pour que le sélecteur distingue les deux.
   La mise à jour d'un fichier déjà exporté est une tâche facultative à part (P13.14).
7. **Aucune permission** : ni `WRITE_EXTERNAL_STORAGE`, ni `WRITE_SETTINGS`. BloX ne règle jamais lui-même la
   sonnerie ni un réveil. Après l'écriture, la feuille propose d'ouvrir les réglages du son
   (`Settings.ACTION_SOUND_SETTINGS`) pour une sonnerie, l'Horloge (`AlarmClock.ACTION_SHOW_ALARMS`) pour un réveil,
   seulement si une application répond (déclarées dans `<queries>` du manifeste, qui n'est pas une permission).
8. **Avant l'API 29**, la fonction est absente : `AppContainer` ne fournit pas d'exportateur (`null`), et la ligne
   « Sonnerie ou réveil » n'apparaît dans aucun menu.
9. **Points d'entrée** : le menu « … » d'une musique de Mes musiques (V32) et celui de l'éditeur (V33), qui ouvrent
   la feuille « Sonnerie ou réveil » (V42, `BloxSheet`, ADR 0029). L'éditeur n'exporte que la version enregistrée :
   une musique jamais enregistrée ou modifiée depuis passe d'abord par une fenêtre (voir `docs/maquette.md`, V42).
10. **Modules** : `core:audio` porte tout ce qui fait sortir du son (rendu, découpage, encodage, écriture) ;
    `core:data` reste la persistance des réglages et des musiques. La feuille, sans état, est un composant de
    `core:designsystem` (`RingtoneSheet`) partagé par `feature:musics` et `feature:composer` ; l'état de la feuille
    et l'export (usage, écoute, en cours, réussi, échec) sont tenus par `RingtoneExport` (`core:audio`), extrait de
    `MyMusicsViewModel` quand l'éditeur en a besoin (règle de 2, P13.11) ; `app` assemble l'exportateur et les
    ouvertures des réglages et de l'Horloge.

## Conséquences

- Le fichier sonne comme le jeu, et prendra l'instrument choisi (ADR 0032) sans autre changement.
- Amende l'ADR 0008 : BloX **écrit** désormais des fichiers audio, hors de l'APK et hors du dépôt, sur demande du
  joueur. L'APK ne grossit pas (rien n'est embarqué).
- Le découpage, la normalisation, l'encodage, le nom du fichier et le nettoyage après un échec se testent sur la
  JVM ; `MediaStore` lui-même seulement en test instrumenté (Android 10 et plus).
- Le rendu partagé avec le séquenceur (P13.2) ne doit rien changer au son : il attend les empreintes de référence
  d'Arcade (P14.2).
- Les téléphones sous Android 10 n'ont pas la fonction ; l'ajouter (sélecteur de documents) serait un amendement.
- Les fichiers ne font pas partie de la sauvegarde de BloX, et restent dans le téléphone après la désinstallation :
  un réveil réglé dessus continue de sonner. Supprimer une musique ne supprime pas ses fichiers.
- Exporter deux fois la même musique crée deux fichiers ; le joueur les retire depuis ses fichiers.
- À surveiller sur téléphone (P13.13) : les Horloges de certains fabricants ne listent pas `Alarms/` ; la boucle
  doit se raccorder sans claquement dans le sélecteur du système.
- Notification écartée : un son de notification dure moins de 3 s et ne boucle pas ; une boucle de musique coupée
  au milieu d'une phrase n'y convient pas. Elle pourrait revenir avec un extrait écrit pour cela (une phrase, une
  cadence), ce qui serait un amendement.
- **Amendement (P13.6, 2026-10-31).** Deux points du 6 changent à l'écriture. `export` rend l'URI `content://` du
  fichier **en texte** (`String`, comme `MusicFile.uri`), non un `android.net.Uri` : celui-ci ne se construit pas dans
  les tests sur la JVM, où `FakeRingtoneExporter` doit en rendre un. Et `TITLE` ne se règle pas : `MediaStore` le tire
  du nom du fichier à la publication, et refuse qu'une application le change ensuite (la mise à jour ne change aucune
  ligne ; vu sur Android 17). Le titre est donc le nom du fichier sans l'extension : le nom de la musique, ses
  caractères interdits remplacés par `_`, et « Nom (1) » au second export du même nom, comme voulu. Un nom qui
  commence par un point donnerait un fichier caché, que `MediaStore` ne montre pas : ce point est remplacé par `_`.

## Options écartées

- **OGG ou AAC** : pas d'encodeur Vorbis sans bibliothèque ; AAC par `MediaCodec`, testable seulement sur appareil,
  pour gagner quelques mégaoctets sur un fichier écrit une fois.
- **Régler la sonnerie directement** (`WRITE_SETTINGS`) : permission spéciale, écran à part, et le réveil ne se règle
  pas ainsi de toute façon.
- **Android 8 et 9 par `WRITE_EXTERNAL_STORAGE` ou par le sélecteur de documents** : une permission de plus, ou un
  fichier rangé où le joueur l'a mis, pas forcément vu comme une sonnerie.
- **Un fondu de fin** : le système boucle le fichier ; un fondu ferait un creux à chaque tour.
- **Exporter Korobeïniki ou le Menuet** : droits encore à confirmer pour l'un, et une règle simple pour les deux : on
  n'exporte que ce que le joueur a créé.
