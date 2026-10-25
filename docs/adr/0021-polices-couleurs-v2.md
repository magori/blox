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
- Chiffres à chasse fixe : la fonction `tnum` de Manrope si elle existe, sinon `DigitWidths`
  comme aujourd'hui **[à vérifier]** sur le fichier.

## Conséquences

- La taille de l'APK est à mesurer **[à vérifier]**.
- Le rendu des polices variables sous Windows et Linux est à surveiller sur les captures, à 0 %
  d'écart aujourd'hui.
