package ch.magori.blox.feature.composer

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Records the composable functions executed again once [start] is called, named as the compiler names them in its
 * trace markers: `ch.magori.blox.feature.composer.grid.GridLine (NoteGrid.kt:150)`. Registered as an extension, it is
 * the tracer of the composer during each test. A function skipped because its parameters did not change is not
 * recorded.
 *
 * The same recorder as in the instrumented tests of `feature:game`: a feature cannot share its test sources with
 * another, and a test-fixtures module for one class of an internal API of Compose would weigh more than this copy.
 */
@OptIn(InternalComposeTracingApi::class)
internal class RecompositionRecorder :
    BeforeEachCallback,
    AfterEachCallback,
    CompositionTracer {
    private val executed = CopyOnWriteArrayList<String>()

    @Volatile
    private var recording = false

    /** The functions executed since [start]. */
    val recomposed: List<String> get() = executed.toList()

    /** Starts recording, once the content is first drawn: its first composition is not a recomposition. */
    fun start() {
        executed.clear()
        recording = true
    }

    override fun beforeEach(context: ExtensionContext) = Composer.setTracer(this)

    override fun afterEach(context: ExtensionContext) = Composer.setTracer(null)

    override fun traceEventStart(
        key: Int,
        dirty1: Int,
        dirty2: Int,
        info: String,
    ) {
        if (recording) executed += info
    }

    override fun traceEventEnd() = Unit

    override fun isTraceInProgress(): Boolean = true
}
