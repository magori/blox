import ch.magori.blox.buildlogic.configureAndroidUnitTests
import ch.magori.blox.buildlogic.configureKotlinAndroid
import ch.magori.blox.buildlogic.intVersion
import ch.magori.blox.buildlogic.libs
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = libs.intVersion("targetSdk")
            }
            configureAndroidUnitTests()
        }
    }
}
