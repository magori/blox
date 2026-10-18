# Musique

## Ce qu'on peut faire, et ce qu'on ne fait pas

- **Oui** : jouer des **mélodies du domaine public** dans des **arrangements écrits pour BloX**.
  *Korobeïniki* est une chanson populaire russe du XIXe siècle. Le *Menuet en sol* (Petit
  livre d'Anna Magdalena Bach, 1725, longtemps attribué à J.-S. Bach, aujourd'hui à
  Christian Petzold) est lui aussi libre.
- **Non** : reproduire les arrangements ou enregistrements des jeux de 1989 (compositions de
  Hirokazu Tanaka, propriété de Nintendo). Ils ne sont ni retranscrits ni extraits dans ce dépôt.
- **Fichier personnel** : l'application peut lire un fichier audio que l'utilisateur choisit
  sur son téléphone. Le fichier reste sur l'appareil et **n'est jamais versionné**
  (`app/src/main/assets/music/` est ignoré par git).

Voir l'[ADR 0008](adr/0008-musique-domaine-public.md).

## Choix proposés à l'accueil

*Korobeïniki* · *Menuet* · *Mon fichier* · *Aucune*

## Modèle

```
Score(title, bpm, voices)
Voice(waveform: SQUARE | TRIANGLE, volume, notes)
Note(pitch: Pitch?, beats: Double)   // pitch null = silence
Pitch("A4") -> 440 Hz ; midi = 12 × (octave + 1) + demi-ton ; f = 440 × 2^((midi − 69) / 12)
```

Le séquenceur lit les voix en parallèle. Chaque note joue à volume constant pendant 80 % de
sa durée puis s'éteint en fondu jusqu'à 95 %, pour séparer les notes répétées.

## Accélération

Quand la pile atteint une hauteur critique, le tempo augmente (par exemple × 1,25), et revient
à la normale quand elle redescend. Le domaine émet un événement `StackDanger(on/off)`, le
ViewModel le traduit en `setTempoFactor()`. Seuil exact à décider en jouant.

## Partitions (prototype de la maquette)

Durées en temps (1 = noire, 0,5 = croche). Notation anglo-saxonne (A4 = la 440 Hz).

### Korobeïniki — 150 temps/min, 4/4

Mélodie, onde carrée, volume 0,07 :

| Mesure | Notes |
|---|---|
| 1 | E5 1 · B4 ½ · C5 ½ · D5 1 · C5 ½ · B4 ½ |
| 2 | A4 1 · A4 ½ · C5 ½ · E5 1 · D5 ½ · C5 ½ |
| 3 | B4 1½ · C5 ½ · D5 1 · E5 1 |
| 4 | C5 1 · A4 1 · A4 1 · silence 1 |
| 5 | silence ½ · D5 1 · F5 ½ · A5 1 · G5 ½ · F5 ½ |
| 6 | E5 1½ · C5 ½ · E5 1 · D5 ½ · C5 ½ |
| 7 | B4 1 · B4 ½ · C5 ½ · D5 1 · E5 1 |
| 8 | C5 1 · A4 1 · A4 1 · silence 1 |

Basse maison, onde triangle, volume 0,16, en noires « fondamentale – quinte – fondamentale –
quinte » : E2/B2 · A2/E3 · E2/B2 · A2/E3 · D2/A2 · C3/G2 · E2/B2 · A2/E3.

### Menuet en sol — 132 temps/min, 3/4

Mélodie, onde carrée, volume 0,07 :

| Mesure | Notes |
|---|---|
| 1 | D5 1 · G4 ½ · A4 ½ · B4 ½ · C5 ½ |
| 2 | D5 1 · G4 1 · G4 1 |
| 3 | E5 1 · C5 ½ · D5 ½ · E5 ½ · F♯5 ½ |
| 4 | G5 1 · G4 1 · G4 1 |
| 5 | C5 1 · D5 ½ · C5 ½ · B4 ½ · A4 ½ |
| 6 | B4 1 · C5 ½ · B4 ½ · A4 ½ · G4 ½ |
| 7 | F♯4 1 · G4 ½ · A4 ½ · B4 ½ · G4 ½ |
| 8 | A4 3 |

Basse maison, onde triangle, volume 0,16, une blanche pointée par mesure :
G2 · B2 · C3 · B2 · A2 · G2 · D3 · D2.

> Les basses sont volontairement simples. Ce sont elles qui font de ces versions des
> arrangements BloX : on pourra les enrichir.
