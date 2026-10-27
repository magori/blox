package ch.magori.blox.core.designsystem.test

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Records the composable functions executed again once [start] is called, sub-compositions included
 * (that of `BoxWithConstraints`, for instance), named as the compiler names them in its trace markers:
 * `ch.magori.blox.feature.game.Play (GameScreen.kt:233)`. Registered as an extension, it is the tracer
 * of the composer during each test. Shared by the instrumented tests of what draws without composing again (ADR
 * 0024): the equalizer and the grid of the editor.
 *
 * A function skipped because its parameters did not change is not recorded, nor is a scope invalidated
 * by a derived state whose value did not change: the composer enters it only to read its states again,
 * without executing it. A `CompositionObserver` would count those as recompositions.
 */
@OptIn(InternalComposeTracingApi::class)
class RecompositionRecorder :
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
