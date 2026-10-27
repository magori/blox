package ch.magori.blox.core.board.tile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the tiles of a tray of pieces, in the Colorful theme only, by day and by night: the chosen one
 * and its ring, two others, an empty slot. Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview
@Composable
private fun PieceTiles() = PieceTilesPreview()

@PreviewTest
@Preview(uiMode = UI_MODE_NIGHT)
@Composable
private fun PieceTilesNight() = PieceTilesPreview()
