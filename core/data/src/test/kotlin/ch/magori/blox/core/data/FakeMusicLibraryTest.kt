package ch.magori.blox.core.data

import kotlinx.coroutines.CoroutineScope

/** The fake shared by the tests of the features keeps the contract of the real saved musics. */
class FakeMusicLibraryTest : MusicLibraryContract() {
    override fun emptyLibrary(scope: CoroutineScope): MusicLibrary = FakeMusicLibrary()
}
