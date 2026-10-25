plugins {
    alias(libs.plugins.blox.android.library)
    alias(libs.plugins.blox.android.library.compose)
    // Le calcul du contraste de WCAG et la liste des palettes nommées, partagés par les tests de contraste des
    // features (ChoiceColorsTest, KeyColorsTest).
    alias(libs.plugins.blox.test.fixtures)
    // Les fenêtres partagées (NamingWindow, DeleteWindow), rendues sur la JVM et comparées aux références.
    alias(libs.plugins.blox.android.screenshot)
    // Les couleurs de docs/design-system/tokens.json, comparées aux palettes par PaletteTokensTest (ADR 0019).
    alias(libs.plugins.blox.design.tokens)
}

android {
    namespace = "ch.magori.blox.core.designsystem"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)

    // Les fenêtres partagées, éprouvées seules sur un appareil (comme les écrans des features).
    androidTestImplementation(libs.androidx.compose.ui.test.android)
    debugRuntimeOnly(libs.androidx.compose.ui.test.manifest)

    // textLayout et assertWordsWhole : les lignes d'un texte affiché, lues par les tests instrumentés des fenêtres et
    // des features (mots entiers en police agrandie).
    testFixturesApi(libs.androidx.compose.ui.test.android)
    testFixturesImplementation(libs.assertj.core)
}

/**
 * Donne aux tests le dossier des polices embarquées et celui de leurs licences : EmbeddedFontsTest vérifie
 * que chaque police a sa licence OFL et que ses chiffres ont une seule largeur. Les deux dossiers sont des
 * entrées de la tâche : ajouter une police ou une licence relance le test, y compris depuis le cache de build.
 * Les licences sont dans les assets du module, fusionnés dans l'APK : l'OFL (condition 2) veut qu'elles
 * accompagnent la police.
 */
abstract class FontDirectoriesArgument : CommandLineArgumentProvider {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val fonts: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val licenses: DirectoryProperty

    override fun asArguments(): Iterable<String> = listOf(
        "-Dblox.fontDir=${fonts.get().asFile.absolutePath}",
        "-Dblox.licensesDir=${licenses.get().asFile.absolutePath}",
        // Les tests lisent les métriques des polices avec java.awt, sans écran.
        "-Djava.awt.headless=true",
    )
}

val fontDirectoriesArgument = objects.newInstance<FontDirectoriesArgument>().apply {
    fonts.set(layout.projectDirectory.dir("src/main/res/font"))
    licenses.set(layout.projectDirectory.dir("src/main/assets/licenses"))
}

tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(fontDirectoriesArgument)
}
