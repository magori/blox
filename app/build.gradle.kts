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
    implementation(projects.core.data)
    implementation(projects.core.audio)
    implementation(projects.feature.home)
    implementation(projects.feature.game)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
}
