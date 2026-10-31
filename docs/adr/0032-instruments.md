# 0032 — Instruments : un jeu de timbres choisi pour toute l'application

- **Statut** : Acceptée ; amende [0008](0008-musique-domaine-public.md) et [0030](0030-reglages-generaux-et-par-jeu.md)
- **Date** : 2026-10-31

## Contexte

Toutes les musiques de BloX sonnent aujourd'hui de la même façon : une onde carrée pour la mélodie, une onde
triangle pour la basse, une enveloppe qui tient 80 % de la note et s'éteint jusqu'à 95 % (`Synthesizer`,
`core:audio`). L'ADR 0008 en fait un trait voulu : « le son a un caractère 8 bits ». Les effets sonores (Classique,
Serrurier, Jardin) sont rendus par le même synthétiseur, sur les mêmes ondes.

Le développeur demande (2026-10-31) de pouvoir changer le son de toutes les musiques : garder le son actuel, et en
proposer un doux, apaisant, et un dynamique, tendance. Ses décisions :

- le réglage s'appelle **« Instruments »**, avec trois choix : **Arcade** (le son actuel, inchangé, par défaut),
  **Velours** (doux, apaisant), **Néon** (dynamique, tendance) ;
- c'est un **réglage général**, dans « Tous les jeux », carte Sons et vibrations (ADR 0030), avec une écoute
  d'essai ; pas de choix par musique ;
- **les effets sonores suivent l'instrument**, dès cette phase, ceux du Jardin compris ;
- Arcade rend **exactement** les mêmes échantillons qu'aujourd'hui, effets compris.

Contraintes :

- `musics.bin` (ADR 0017) ne doit pas changer : une voix porte une `Waveform` (`SQUARE` ou `TRIANGLE`), qui dit
  aujourd'hui son timbre et, en pratique, son rôle (mélodie ou basse).
- Au tempo (ADR 0028) récompense une pose sur le temps : une attaque lente déplacerait le temps entendu. Les retours
  en jeu (lignes, rotation) doivent rester nets.
- Aucun échantillon enregistré, aucune banque de sons : tout est synthétisé, rien qui imite un instrument ou un
  préréglage identifiable.
- Le rendu se fait une fois par tempo, hors du fil audio, et doit rester bref.

Options envisagées : un style par musique (dans `musics.bin`), un style par jeu, un réglage unique ; des
échantillons enregistrés ou une synthèse ; des noms techniques (« Original », « Doux », « Pop ») ou des noms d'humeur.

## Décision

1. **Un réglage unique**, `UserPreferences.soundStyle` (clé `sound_style`, enregistré par **nom**), portée « Tous
   les jeux » : rangée « Instruments » de la carte Sons et vibrations (V31), qui ouvre la feuille Instruments
   (V43). Défaut, et valeur lue d'un nom inconnu : **Arcade**.
2. **Description pure dans `core:music`** : `enum class SoundStyle { ARCADE, VELVET, NEON }`, noms figés par un
   test ; `style.instrumentFor(waveform): Instrument` ; `Instrument(tone, shape, lowPass, gain)` ; `Tone` (`SQUARE`,
   `TRIANGLE`, `PULSE_QUARTER`, `SINE`, `SAW`) ; `NoteShape`, `Gate` (l'enveloppe d'aujourd'hui) ou `Adsr` (en
   millisecondes) ; `Echo` (délai en temps, retour, part), facultatif, par style. `Voice`, `Score` et `musics.bin`
   ne changent pas : la `Waveform` d'une voix désigne son rôle, l'instrument son timbre. `Envelope` reste le nom de
   l'enveloppe des effets.
3. **Synthèse dans `core:audio`** : `Synthesizer.render(score, tempoFactor, style)`. Arcade rend **exactement** les
   échantillons d'avant les instruments : des empreintes, écrites sur l'API d'aujourd'hui **avant** tout changement
   du synthétiseur (P14.2, avant P13.2 aussi), figent Korobeïniki, le Menuet et une musique générée, à × 1 et
   × 1,25, et chaque effet de `SoundEffects`, `LocksmithSounds` et `GardenSounds`. Scie et impulsion sont limitées
   en bande (PolyBLEP). Le filtre est un passe-bas à un pôle. L'écho s'applique au mixage, enroulé sur le début du tampon : la fin de la boucle
   résonne sur son début, sans raccord. Les trois instruments ont le même niveau à ±1 dB RMS.
4. **Attaque de 5 ms au plus** pour chaque instrument de chaque style : le temps reste net dans Au tempo, la
   douceur de Velours vient de l'onde, du filtre, de l'extinction et de l'écho, jamais d'une attaque lente.
5. **Ce que l'instrument habille** : toutes les partitions jouées par le séquenceur (parties du Classique et de la
   Graine du jour, Musique vivante, Écouter de l'accueil, Mes musiques, éditeur, Jardin, accompagnement d'Au tempo)
   et le fichier d'une sonnerie (ADR 0031, instrument présélectionné dans la feuille, changeable pour ce fichier
   seul). **Pas « Mon fichier »**, un fichier audio joué tel quel.
6. **Effets sonores** (`SoundEffects`, `LocksmithSounds`, `GardenSounds`) : l'instrument leur donne son **ton** (et
   son filtre) selon leur `Waveform`, mais ils gardent **leur propre enveloppe** (`Envelope`, 1 à 2 ms de montée),
   sans écho : un retour reste court et lisible. `Synthesizer.render(effect, style)` ; Arcade rend les effets
   d'aujourd'hui. Les effets changent d'instrument **en même temps que la musique**, à la prochaine musique jouée :
   `SoundEffectTracks` oublie alors ses pistes rendues dans l'ancien instrument et les rend de nouveau.
7. **Application** : `app` lit le réglage et le passe aux lecteurs (`SoundStyleControl`, sur le modèle de
   `VolumeControl` et de `applySavedVolumes`). Un changement vaut **dès la prochaine musique jouée** (prochain
   `play`, `resume` ou `switchTo`) ; les tampons rendus dans l'autre instrument sont oubliés.
8. **Néon sans batterie** dans cette phase. Une rythmique tirée de la mesure (grosse caisse, claquement, charleston)
   est une tâche facultative (P14.19), jamais écrite dans `musics.bin`, et jamais dans Au tempo, où le métronome
   reste le seul repère.

## Conséquences

- Amende l'ADR 0008 : le « caractère 8 bits » devient celui d'Arcade, le son par défaut ; Velours et Néon sont
  d'autres timbres, toujours synthétisés (aucun fichier dans l'APK, aucune partition changée).
- Amende l'ADR 0030 : une ligne de plus au tableau des portées, « Instruments | Tous les jeux | « Tous les jeux »
  (Sons et vibrations) | Tous, sauf Mon fichier ». La note du bas de la carte devient « Volumes : tout de suite.
  Instruments : dès la prochaine musique jouée. Le reste : dès le prochain jeu ouvert. ».
- Le Classique et ses captures ne bougent pas en Arcade ; le test d'empreinte le garde.
- Le rendu coûte plus en Velours et Néon (filtre, écho) : à mesurer sur un téléphone d'entrée de gamme (P14.18),
  seuil 150 ms pour 8 mesures ; au-delà, simplifier l'écho ou le filtre.
- La mémoire reste la même (un tampon par tempo, environ 2,3 Mo) : le changement d'instrument vide le cache.
- Vocabulaire : `SoundStyle`, `Instrument`, `Tone`, `NoteShape`, `Echo` ; `Envelope` reste aux effets.
- À écouter (P14.16) : Velours pâteux et Néon saturé à × 1,25 (pile en danger, niveaux 18 et 19) ; le métronome
  d'Au tempo dans chaque instrument ; le raccord de la boucle avec l'écho ; une sonnerie en Néon, normalisée en
  crête, peut sonner plus fort. Les gains sont réglés ensuite, par la mesure du niveau (P14.17).
- Un instrument par musique demanderait une version 3 de `musics.bin` : écarté pour l'instant.

## Options écartées

- **Un instrument par musique**, enregistré dans `musics.bin` : nouveau format, une migration, et un réglage à
  refaire musique par musique ; le développeur veut changer l'humeur de tout d'un geste.
- **Un instrument par jeu** : rien ne le demande (ADR 0030, « À surveiller »).
- **Des échantillons enregistrés** (un piano, une batterie) : poids de l'APK, droits de chaque son, et le rendu ne se
  testerait plus sur la JVM.
- **Des effets qui restent en Arcade** (proposition de l'architecte) : le développeur veut un son cohérent, effets
  compris.
- **Les noms « Original », « Doux », « Pop »** (proposition de l'architecte) et **« Style »** (déjà pris par la
  fenêtre Enregistrer), **« Son »** (confondu avec Effets sonores), **« Rétro »** (une catégorie de musique) : le
  designer et le développeur retiennent « Instruments », Arcade, Velours, Néon.
