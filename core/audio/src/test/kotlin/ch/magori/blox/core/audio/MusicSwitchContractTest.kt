package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Scores
import kotlinx.coroutines.test.TestScope

/** The switch between scores and music files keeps the contract of the players for the scores it plays. */
class MusicSwitchContractTest : MusicPlayerContract() {
    override fun playerUnderTest(scope: TestScope): PlayerUnderTest {
        val fake = FakeMusicPlayer()
        return heardThrough(fake, player = MusicSwitch(fake, FakeMusicFilePlayer(), fallback = Scores.korobeiniki))
    }
}
