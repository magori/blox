package ch.magori.blox.core.audio

import kotlinx.coroutines.test.TestScope

/**
 * The player that plays only with the audio focus keeps the contract of the players while the focus is
 * granted: what it plays is heard through the fake it wraps.
 */
class FocusedMusicPlayerContractTest : MusicPlayerContract() {
    override fun playerUnderTest(scope: TestScope): PlayerUnderTest {
        val fake = FakeMusicPlayer()
        return heardThrough(fake, player = FocusedMusicPlayer(fake, FakeAudioFocus(log = mutableListOf())))
    }
}
