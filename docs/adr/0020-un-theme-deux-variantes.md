# 0020 — Un thème, des variantes ; Couleurs v2 remplace Couleurs ; une mise en page pour tous les thèmes

- **Statut** : Acceptée
- **Date** : 2026-10-25

## Contexte

Couleurs v2 (piste C, « Y2K irisé ») suit le mode sombre du téléphone : jour et nuit. Brume
(P5.41) aussi. Aujourd'hui `BloxThemeKind { ORIGINAL, COLORFUL }` mêle le choix du joueur et le
rendu (palette, formes, polices, styles de case), et rien ne lit le mode sombre. Garder deux
Couleurs, ou deux mises en page par écran, doublerait le code. Les écrans v2 sont dessinés pour
le téléphone debout ; le téléphone couché et les tablettes viendront plus tard (décision du
développeur, P5.84).

## Décision

- `BloxThemeKind` reste **le choix enregistré** : `ThemeChoice.COLORFUL` ne change pas, aucune
  migration.
- `BloxTheme` le résout en **`BloxThemeVariant`** (`ORIGINAL`, `COLORFUL_LIGHT`, `COLORFUL_DARK`,
  et plus tard les deux Brume) avec `isSystemInDarkTheme()` ; palette, formes, polices et styles de
  case se choisissent par la variante.
- **Couleurs v1** (Chakra Petch, fond `#12141C`, biseaux) est **supprimé**, sans drapeau.
- **Une seule mise en page par écran pour tous les thèmes** : Original rend les écrans v2 avec ses
  propres jetons (coins carrés, pas de relief, police pixel).
- **Les formats sont prévus dès le départ** : la mise en page d'un écran est choisie par une
  fonction pure de la taille disponible ; aucun composant ne suppose le portrait ni une largeur
  d'écran fixe. Seul le téléphone debout est livré d'abord.

## Conséquences

- Pendant le lot, `main` mélange anciennes mises en page et couleurs v2 ; toutes les captures
  changent dès P5.57.
- Passer de jour à nuit recrée l'activité et met donc la partie en pause (comportement déjà voulu).
- Brume entrera comme deux variantes (P5.41 amendée).
- Les valeurs Original des rôles nouveaux sont fixées dans `docs/themes.md` et à confirmer sur la
  maquette (P5.85).
