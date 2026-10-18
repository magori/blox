# 0009 — JUnit 5 pour tous les tests, modules Android compris

- **Statut** : Acceptée
- **Date** : 2026-10-18

## Contexte

L'[ADR 0003](0003-domaine-en-kotlin-pur.md) réservait JUnit 5 aux modules Kotlin purs et
laissait les modules Android sur JUnit 4, pris en charge nativement par AGP. Deux
frameworks de test coexistaient donc : deux styles d'assertions sur le cycle de vie, deux
façons d'écrire des tests paramétrés, et la tentation de copier un test d'un module à
l'autre sans l'adapter.

Options envisagées :

- garder JUnit 4 côté Android (statu quo, compromis de l'ADR 0003) ;
- JUnit 5 partout via le plugin `de.mannodermaus.android-junit` (ex `android-junit5`),
  qui branche JUnit Platform sur les tâches de test d'AGP et fournit des extensions pour
  les tests instrumentés, y compris Compose.

## Décision

- **JUnit 5 partout**, tests unitaires et instrumentés, dans tous les modules. Le plugin
  `de.mannodermaus.android-junit` est appliqué par les plugins de convention
  `blox.android.library` et `blox.android.application` ; aucun module ne le configure
  lui-même.
- **JUnit 4 n'est plus déclaré** dans le catalogue. Il ne subsiste que par transitivité,
  à l'intérieur du lanceur d'instrumentation d'AndroidX, qu'on ne contrôle pas.
- Compose UI Test s'utilise via `ui-test-android` et les extensions JUnit 5 du plugin
  (`createComposeExtension()`, `createAndroidComposeExtension()`), pas via
  `ui-test-junit4` ni `createComposeRule()`.
- Les tests instrumentés exigent un appareil en API 26 ou plus, ce qui correspond déjà au
  `minSdk` du projet.

## Conséquences

- Un seul style de test dans tout le dépôt : `@Test` de Jupiter, `@Nested`,
  `@ParameterizedTest`, `@RegisterExtension`.
- La règle « JUnit 5 dans les modules purs, JUnit 4 dans les modules Android » de l'ADR 0003
  est remplacée par celle-ci ; le reste de l'ADR 0003 (domaine pur, TDD) reste en vigueur.
- Une dépendance de build supplémentaire, versionnée dans `libs.versions.toml`
  (`androidJunit`), à suivre lors des mises à jour d'AGP et de JUnit.
- JUnit 6 n'est pas retenu pour l'instant : il demande Android 15 (API 35) sur l'appareil
  pour les tests instrumentés.
