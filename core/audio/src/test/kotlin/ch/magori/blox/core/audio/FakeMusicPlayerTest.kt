package ch.magori.blox.core.audio

import kotlinx.coroutines.test.TestScope

/** The fake shared by the tests of the features keeps the contract of the real player. */
class FakeMusicPlayerTest : MusicPlayerContract() {
    override fun playerUnderTest(scope: TestScope): PlayerUnderTest = heardThrough(FakeMusicPlayer())
}
