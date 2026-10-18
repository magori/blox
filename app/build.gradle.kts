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
    implementation(projects.feature.home)
    implementation(projects.feature.game)
    implementation(projects.core.game)
    implementation(projects.core.data)
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
}

/**
 * Donne aux tests le répertoire res et le manifeste de l'application : WindowBackgroundTest et
 * LauncherIconTest y comparent des ressources (fond de fenêtre, icône, écran de démarrage) à la palette
 * Original. Ce sont des entrées de la tâche : les modifier relance les tests, y compris depuis le cache de build.
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
