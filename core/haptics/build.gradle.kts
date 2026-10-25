plugins {
    alias(libs.plugins.blox.android.library)
    // Le faux retour haptique (FakeHaptics), partagé par les tests unitaires et instrumentés de feature:game.
    alias(libs.plugins.blox.test.fixtures)
}

android {
    namespace = "ch.magori.blox.core.haptics"
}

dependencies {
    // Le test instrumenté du vibreur lit le contexte de l'application testée (InstrumentationRegistry).
    androidTestImplementation(libs.androidx.test.monitor)
}
