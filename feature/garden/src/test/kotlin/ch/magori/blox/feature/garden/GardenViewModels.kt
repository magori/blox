package ch.magori.blox.feature.garden

import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.SoundEffectPlayer
import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.GardenRepository
import ch.magori.blox.core.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.random.Random

/**
 * The view model of the garden, opened on the garden [kept], its writes made at once, its rolls drawn from [random],
 * its score played by [music], its preview heard by [soundEffects] as the [preferences] want.
 */
internal fun gardenViewModel(
    kept: GardenRepository = FakeGardenRepository(),
    random: Random = Random(0),
    music: MusicPlayer = FakeMusicPlayer(),
    soundEffects: SoundEffectPlayer = FakeSoundEffectPlayer(),
    preferences: UserPreferencesRepository = FakeUserPreferencesRepository(),
): GardenViewModel = GardenViewModel(
    kept = kept,
    appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
    writes = Mutex(),
    music = music,
    soundEffects = soundEffects,
    preferences = preferences,
    random = random,
)
