package ch.magori.blox.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Runs `Dispatchers.Main`, and so `viewModelScope`, on a test dispatcher that runs coroutines at once, for
 * each test. Shared with the fake repositories: the view models that read them collect in `viewModelScope`.
 * The opt-in is written here: the convention gives it to unit tests only, not to test fixtures.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension :
    BeforeEachCallback,
    AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }
}
