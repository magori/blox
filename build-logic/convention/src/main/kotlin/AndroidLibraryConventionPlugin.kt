import ch.magori.blox.buildlogic.configureAndroidTests
import ch.magori.blox.buildlogic.configureKotlinAndroid
import ch.magori.blox.buildlogic.intVersion
import ch.magori.blox.buildlogic.libs
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = libs.intVersion("targetSdk")
            }
            configureAndroidTests()
        }
    }
}
