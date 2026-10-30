package ch.magori.blox.core.board

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import de.mannodermaus.junit5.compose.ComposeContext
import org.assertj.core.api.Assertions.assertThat

/*
 * Tools shared by the instrumented tests that draw the board: those of the board itself and those of the game
 * screen (feature:game), which use them through the test fixtures of this module. Recompositions are counted by
 * the RecompositionRecorder of the test fixtures of core:designsystem.
 *
 * Their method names have no spaces: the test APK is dexed for API 26, which refuses them. The
 * sentence is in @DisplayName.
 */

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
