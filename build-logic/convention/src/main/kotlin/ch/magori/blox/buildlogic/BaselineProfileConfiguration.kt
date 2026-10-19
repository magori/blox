package ch.magori.blox.buildlogic

import androidx.baselineprofile.gradle.consumer.BaselineProfileConsumerExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Profil de référence (baseline profile) de l'application, côté consommateur (P5.17).
 *
 * Le profil est produit par le module `:benchmark` (plugin `blox.android.benchmark`), que
 * l'application désigne par `baselineProfile(projects.benchmark)`. Il est versionné dans
 * `src/main/generated/baselineProfiles/` (`mergeIntoMain`) et n'est régénéré qu'à la demande, sur un
 * appareil branché : `./gradlew :app:generateBaselineProfile`. Un build ordinaire, en CI comme en
 * local, se contente du fichier versionné. `profileinstaller` l'installe au premier lancement d'une
 * version qui ne passe pas par le Play Store (ART le compile alors en arrière-plan).
 *
 * Les types de build créés par le plugin (`nonMinifiedRelease`, `benchmarkRelease`) reçoivent le
 * suffixe [BENCHMARK_APPLICATION_ID_SUFFIX] : les mesures, qui réinstallent l'application et
 * effacent son état de compilation, ne touchent pas à l'application installée pour jouer ou
 * pour les tests instrumentés.
 *
 * Ce réglage dépend de l'ordre des rappels `finalizeDsl`, exécutés dans l'ordre où ils sont
 * enregistrés : le plugin enregistre le sien (qui crée les deux types de build) quand il est
 * appliqué, donc avant celui-ci. Appliquer le plugin plus tard, ou enregistrer ce rappel avant lui,
 * laisserait les types de build sans suffixe, sans erreur.
 *
 * Le plugin signe aussi ces APK de mesure : `release` n'ayant pas de configuration de signature, il
 * leur donne celle de `debug`, la clé de débogage de la machine. Ils s'installent donc sans
 * réglage, et ne sont pas publiables.
 */
internal fun Project.configureBaselineProfileConsumer() {
    pluginManager.apply("androidx.baselineprofile")

    extensions.configure<BaselineProfileConsumerExtension> {
        mergeIntoMain = true
    }
    extensions.configure<ApplicationAndroidComponentsExtension> {
        // Appelé après le `finalizeDsl` du plugin, qui crée ces types de build.
        finalizeDsl { android ->
            android.buildTypes
                .matching { it.name in BENCHMARK_BUILD_TYPES }
                .configureEach { applicationIdSuffix = BENCHMARK_APPLICATION_ID_SUFFIX }
        }
    }

    dependencies {
        // Aucune classe appelée : la bibliothèque agit par son initialiseur, déclaré dans son manifeste.
        add("runtimeOnly", libs.library("androidx-profileinstaller"))
    }
}

private val BENCHMARK_BUILD_TYPES = setOf("nonMinifiedRelease", "benchmarkRelease")

private const val BENCHMARK_APPLICATION_ID_SUFFIX = ".perf"
