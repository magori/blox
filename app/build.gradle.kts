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
 * Donne aux tests le chemin de colors.xml, que WindowBackgroundTest compare à la palette Original. Le
 * fichier est une entrée de la tâche : le modifier relance le test, y compris depuis le cache de build.
 */
abstract class ColorsXmlArgument : CommandLineArgumentProvider {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val colorsXml: RegularFileProperty

    override fun asArguments(): Iterable<String> = listOf("-Dblox.colorsXml=${colorsXml.get().asFile.absolutePath}")
}

val colorsXmlArgument = objects.newInstance<ColorsXmlArgument>().apply {
    colorsXml.set(layout.projectDirectory.file("src/main/res/values/colors.xml"))
}

tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(colorsXmlArgument)
}
