plugins {
    alias(libs.plugins.blox.android.application)
    alias(libs.plugins.blox.android.application.compose)
}

android {
    namespace = "ch.magori.blox"

    defaultConfig {
        applicationId = "ch.magori.blox"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.feature.cartridges)
    implementation(projects.feature.home)
    implementation(projects.feature.game)
    implementation(projects.feature.composer)
    implementation(projects.feature.musics)
    implementation(projects.feature.locksmith)
    implementation(projects.feature.daily)
    implementation(projects.core.game)
    implementation(projects.core.data)
    implementation(projects.core.audio)
    implementation(projects.core.haptics)
    implementation(projects.core.music)
    implementation(libs.androidx.datastore.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okio)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // `collectAsStateWithLifecycle` lit le `LifecycleOwner` courant : il faut lifecycle-common à la compilation.
    implementation(libs.androidx.lifecycle.common)

    // Le faux dépôt des réglages, partagé avec les features : AppThemeTest y lit le thème choisi, SavedVolumesTest les
    // volumes.
    testImplementation(testFixtures(projects.core.data))
    // Les couleurs du logo, celles des pièces : LauncherIconTest les compare à l'icône.
    testImplementation(projects.core.board)
    // Les faux lecteurs de musique et d'effets : SavedVolumesTest y lit les volumes appliqués.
    testImplementation(testFixtures(projects.core.audio))

    // Les parcours qui traversent les features (BloxNavDisplayTest), sur les faux partagés : dépôts et bibliothèque de
    // core:data, lecteur de core:audio, retour haptique, et les règles où une rangée est un tick (une fin de partie en
    // quelques secondes d'horloge).
    androidTestImplementation(testFixtures(projects.core.data))
    androidTestImplementation(testFixtures(projects.core.audio))
    androidTestImplementation(testFixtures(projects.core.haptics))
    androidTestImplementation(testFixtures(projects.core.game))
    // tapScrim : la feuille Musique fermée par un toucher sur son voile (P5.76).
    androidTestImplementation(testFixtures(projects.core.designsystem))
    // `@StringRes` sur les textes lus dans les ressources des features.
    androidTestImplementation(libs.androidx.annotation)
    // `WindowInsetsControllerCompat` : l'apparence réelle des barres système (SystemBarsAppearanceTest).
    androidTestImplementation(libs.androidx.core)

    // Le module qui produit le profil de référence (blox.android.benchmark, P5.17).
    baselineProfile(projects.benchmark)
}

/**
 * Donne aux tests le répertoire res et le manifeste de l'application : WindowBackgroundTest et
 * LauncherIconTest y comparent des ressources (fond de fenêtre, icône, écran de démarrage) à la palette
 * Original ; BackupRulesTest compare les règles de sauvegarde aux fichiers DataStore de core:data. Ce sont
 * des entrées de la tâche : les modifier relance les tests, y compris depuis le cache de build.
 */
abstract class AppSourcesArgument : CommandLineArgumentProvider {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifest: RegularFileProperty

    override fun asArguments(): Iterable<String> = listOf(
        "-Dblox.resDir=${resDir.get().asFile.absolutePath}",
        "-Dblox.manifest=${manifest.get().asFile.absolutePath}",
    )
}

val appSourcesArgument = objects.newInstance<AppSourcesArgument>().apply {
    resDir.set(layout.projectDirectory.dir("src/main/res"))
    manifest.set(layout.projectDirectory.file("src/main/AndroidManifest.xml"))
}

tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(appSourcesArgument)
}
