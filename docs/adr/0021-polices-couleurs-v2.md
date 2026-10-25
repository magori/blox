# 0021 — Polices de Couleurs v2

- **Statut** : Acceptée ; amende [0016](0016-polices-embarquees.md)
- **Date** : 2026-10-25

## Contexte

Couleurs v2 utilise Manrope (texte), Unbounded (titres, Jouer, autocollants) et Bagel Fat One (le
seul logo), toutes sous licence SIL OFL 1.1. L'ADR 0016 embarque les polices telles que publiées et
gère les chiffres à chasse fixe par `DigitWidths` quand la police n'a pas `tnum`.

## Décision

- Les trois polices sont **embarquées** dans `core:designsystem/src/main/res/font`, telles que
  publiées par `google/fonts`, y compris en version variable (`FontVariation`, API 26 et plus,
  `minSdk` 26). Les fichiers sont seulement renommés pour `res/font`, sans modifier la police.
- Licences dans `assets/licenses/<famille>/OFL.txt`, vérifiées par le test existant.
- **Chakra Petch est retirée** avec Couleurs v1 (ADR 0020).
- Chiffres à chasse fixe : la fonction `tnum` de Manrope (P5.58, vérifiée sur le fichier : lookup GSUB 21
  qui envoie 0 à 9 vers dix glyphes de 0,62 em, de même largeur à toute graisse selon HVAR ;
  `ValueDigitsTest`). `DigitWidths` et `valueText()` sont retirés. Manrope et Unbounded ne sont
  publiées qu'en version variable ; `Font(res, weight)` ne réglant pas l'axe `wght`, chaque graisse
  pose `FontVariation.weight`.

## Conséquences

- L'APK release passe de 2 008 790 à 3 037 943 octets (+1,0 Mo, P5.58). Bagel Fat One, qui ne sert
  qu'au logo, en fait l'essentiel (662 Ko compressés, glyphes coréens compris) ; un logo exporté en
  vecteur (P5.77) permettrait de la retirer.
- Le rendu des polices variables sous Windows et Linux est à surveiller sur les captures, à 0 %
  d'écart aujourd'hui.
