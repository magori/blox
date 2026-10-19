import ch.magori.blox.buildlogic.configureAndroidLint
import ch.magori.blox.buildlogic.configureAndroidTests
import ch.magori.blox.buildlogic.configureDependencyAnalysis
import ch.magori.blox.buildlogic.configureKotlinAndroid
import ch.magori.blox.buildlogic.configureKtlint
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
                configureAndroidLint(this)
                defaultConfig.targetSdk = libs.intVersion("targetSdk")
            }
            configureAndroidTests()
            configureKtlint()
            configureDependencyAnalysis()
        }
    }
}
