package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.audio.SoundEffectPlayer
import ch.magori.blox.core.data.FakePadlockProgressRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.PadlockProgressRepository
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The view model of [route], its sound effects played by [soundEffects], its settings read in [preferences], its
 * openings written to [progress] in [appScope], the scope of the application, on the main dispatcher of the test
 * unless told otherwise: a write ends within the action that asked for it.
 */
internal fun locksmithViewModel(
    route: LocksmithRoute,
    soundEffects: SoundEffectPlayer = FakeSoundEffectPlayer(),
    preferences: UserPreferencesRepository = FakeUserPreferencesRepository(),
    progress: PadlockProgressRepository = FakePadlockProgressRepository(),
    appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
): LocksmithViewModel = LocksmithViewModel(route, soundEffects, preferences, progress, appScope)
