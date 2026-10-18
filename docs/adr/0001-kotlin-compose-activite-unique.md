# 0001 — Kotlin, Jetpack Compose, activité unique

- **Statut** : Acceptée
- **Date** : 2026-10-18

## Contexte

Application Android personnelle, développée par un développeur à l'aise en Java et Kotlin.
L'interface est entièrement sur mesure (plateau, commandes, thèmes) : peu de composants
standards à réutiliser.

## Décision

Kotlin uniquement, interface en **Jetpack Compose**, **une seule activité** (`MainActivity`)
et navigation avec **Navigation Compose** en routes typées (`@Serializable`). Le plateau est
dessiné avec un `Canvas` Compose. Orientation portrait.

## Conséquences

- C'est la pile recommandée par Android aujourd'hui : documentation et outillage à jour.
- Le dessin du plateau en `Canvas` évite 200 composables par image et reste fluide.
- Material 3 sert de base (accessibilité, typographie), mais l'apparence est pilotée par les
  jetons BloX de `core:designsystem`.
- Le portrait forcé est ignoré par Android sur les grands écrans récents : à vérifier sur
  tablette, sans enjeu pour un usage personnel sur téléphone.
