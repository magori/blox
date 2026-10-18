# 0010 — Kotlin compilé par le support intégré d'AGP 9

- **Statut** : Acceptée
- **Date** : 2026-10-18

## Contexte

Le projet a été créé sans accès au réseau avec des versions figées (Gradle 8.14, AGP 8.13,
Kotlin 2.2). Au premier build local (tâche P0.5), les versions stables disponibles étaient
Gradle 9.8.1, AGP 9.4.1 et Kotlin 2.4.21.

AGP 9 change la façon de compiler Kotlin : le support « built-in Kotlin »
(`android.builtInKotlin=true` par défaut) compile les sources Kotlin sans le plugin
`org.jetbrains.kotlin.android`, et **refuse** ce plugin s'il est appliqué. Le nouveau DSL
(`android.newDsl=true`) n'est pas compatible avec lui. Rester sur l'ancien schéma demande
deux options de désactivation que Google annonce retirer avec AGP 10.

Options envisagées :

- rester sur AGP 8.13 et Gradle 8.14 : versions déjà anciennes à la création, migration
  inévitable et plus coûteuse une fois le code écrit ;
- passer à AGP 9 en désactivant le Kotlin intégré (`android.builtInKotlin=false`,
  `android.newDsl=false`) : sursis d'un an, deux options à retirer plus tard ;
- passer à AGP 9 avec le Kotlin intégré, tant que le projet n'a presque pas de code.

## Décision

- **Gradle 9.8.1**, **AGP 9.4.1**, **Kotlin 2.4.21**, avec le **Kotlin intégré d'AGP** par
  défaut. Aucune option `android.builtInKotlin` ni `android.newDsl` n'est posée.
- Le plugin `org.jetbrains.kotlin.android` disparaît du catalogue, du `build.gradle.kts`
  racine et des plugins de convention. `org.jetbrains.kotlin.jvm` reste pour les modules
  Kotlin purs.
- Dans `build-logic`, `configureKotlinAndroid` configure les options du compilateur via
  l'extension `kotlin` qu'AGP enregistre, typée `org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension`
  (et non plus `KotlinAndroidProjectExtension`). `CommonExtension` n'est plus générique.
- Les plugins de compilateur Kotlin (`org.jetbrains.kotlin.plugin.compose`,
  `org.jetbrains.kotlin.plugin.serialization`) restent appliqués comme avant : AGP les
  accepte, seuls `kotlin-android`, `kapt` et `multiplatform` sont refusés.
- Le plugin Kotlin Gradle reste une dépendance `compileOnly` de `build-logic` : AGP 9
  l'embarque à l'exécution (2.2.10 minimum) et la version du catalogue (2.4.21) prime.
- La version de Java (17) vit dans le catalogue (`java`). Chaque module déclare une **toolchain**
  Java de cette version ; le résolveur `org.gradle.toolchains.foojay-resolver-convention` la
  télécharge si elle manque. Sa version est écrite en dur dans les deux `settings.gradle.kts` :
  les plugins de settings se résolvent avant le catalogue, c'est la seule exception à « toute
  version dans le catalogue ».

## Conséquences

- Un seul compilateur Kotlin configuré par AGP dans les modules Android ; moins de
  configuration dupliquée entre `android {}` et `kotlin {}`.
- `kapt` est exclu de fait ; si un processeur d'annotations devenait nécessaire, ce serait
  KSP. Avec l'injection manuelle (ADR 0007), le besoin n'existe pas.
- Les montées de version d'AGP et de Kotlin se font ensemble : AGP impose une version
  minimale de KGP, et le plugin `android-junit` (ADR 0009) doit suivre JUnit et AGP.
- Le JDK 17 reste la cible de compilation ; le JDK qui exécute Gradle peut être plus récent. La
  toolchain garantit le JDK qui compile, en local comme en CI ; `sourceCompatibility`,
  `targetCompatibility` et `jvmTarget` restent explicites et fixent le bytecode produit.
