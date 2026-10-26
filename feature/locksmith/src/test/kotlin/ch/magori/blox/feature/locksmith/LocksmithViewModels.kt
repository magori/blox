package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.audio.SoundEffectPlayer
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute

/** The view model of [route], its sound effects played by [soundEffects], its settings read in [preferences]. */
internal fun locksmithViewModel(
    route: LocksmithRoute,
    soundEffects: SoundEffectPlayer = FakeSoundEffectPlayer(),
    preferences: UserPreferencesRepository = FakeUserPreferencesRepository(),
): LocksmithViewModel = LocksmithViewModel(route, soundEffects, preferences)
