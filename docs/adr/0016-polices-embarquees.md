# 0016 — Polices embarquées, chiffres à chasse fixe

- **Statut** : Acceptée
- **Date** : 2026-10-18

## Contexte

Les thèmes demandent deux polices sous licence SIL OFL 1.1 : *Press Start 2P* (thème Original) et
*Chakra Petch* (thème Couleurs). Deux façons de les fournir : embarquer les fichiers TTF dans
l'APK, ou utiliser les polices téléchargeables de Google Fonts (`ui-text-google-fonts`). Les
valeurs (score, niveau, lignes, record) doivent garder la même largeur quand elles changent ;
*Chakra Petch* a des chiffres proportionnels et pas de fonction OpenType `tnum`.

## Décision

- **Polices embarquées** dans `core:designsystem/src/main/res/font` (Press Start 2P Regular,
  Chakra Petch Regular et Bold), telles que publiées par `google/fonts`, sans modification : le
  jeu reste hors ligne, le rendu est le même partout, et le nom réservé « Press Start 2P » reste
  utilisable. Pas de sous-ensemble (*subset*) : ce serait une version modifiée, à renommer.
- **Licences distribuées avec l'APK** : `core:designsystem/src/main/assets/licenses/<famille>/OFL.txt`
  (condition 2 de l'OFL) ; un test vérifie que chaque police embarquée a la sienne.
- **Chiffres à chasse fixe** : `BloxTypography.valueText()` ajoute à chaque chiffre plus étroit un
  espacement en em jusqu'à la largeur du plus large, et coupe le crénage ; un test le vérifie sur
  les avances réelles du fichier TTF.
- **Taille minimale** des textes qui rétrécissent pour tenir : 8 sp.

## Conséquences

- L'APK grossit d'environ 111 Kio (polices) et 9 Kio (licences).
- Un écran « Licences » pourra lire les fichiers d'`assets` ; d'ici là, ils sont dans l'APK.
- Avec la police du système agrandie (×2), la police pixel (1 em par caractère) ne tient plus
  dans certaines places à 8 sp : traité avec la police agrandie (P2.18).
