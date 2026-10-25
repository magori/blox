package ch.magori.blox.core.board

import android.graphics.Bitmap
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import de.mannodermaus.junit5.compose.ComposeContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.concurrent.CopyOnWriteArrayList

/*
 * Tools shared by the instrumented tests that draw the board: those of the board itself and those of the game
 * screen (feature:game), which use them through the test fixtures of this module.
 *
 * Their method names have no spaces: the test APK is dexed for API 26, which refuses them. The
 * sentence is in @DisplayName.
 */

/**
 * Records the composable functions executed again once [start] is called, sub-compositions included
 * (that of `BoxWithConstraints`, for instance), named as the compiler names them in its trace markers:
 * `ch.magori.blox.feature.game.Play (GameScreen.kt:233)`. Registered as an extension, it is the tracer
 * of the composer during each test.
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

/** The content as it is drawn once it is idle: capturing it waits for the composition and the drawing. */
fun ComposeContext.drawnContent(): Bitmap = onRoot().captureToImage().asAndroidBitmap()

/** Moves the piece [times] times with [move], on the UI thread, and waits each time for the content to be drawn. */
fun ComposeContext.movePiece(
    times: Int,
    move: () -> Unit,
) = repeat(times) {
    runOnIdle(move)
    drawnContent()
}

/**
 * Asserts that moving the piece with [move] changes the drawing, and that the content is then drawn
 * exactly as when [composeAfresh] composes it from scratch, as shown for the first time.
 */
fun ComposeContext.assertDrawnAsFresh(
    move: () -> Unit,
    composeAfresh: () -> Unit,
) {
    val before = drawnContent()

    movePiece(times = 3, move)
    val moved = drawnContent()
    runOnIdle(composeAfresh)
    val fresh = drawnContent()

    assertThat(moved.sameAs(before)).describedAs("drawn again after the move").isFalse()
    assertThat(moved.sameAs(fresh)).describedAs("drawn as when composed from scratch").isTrue()
}
