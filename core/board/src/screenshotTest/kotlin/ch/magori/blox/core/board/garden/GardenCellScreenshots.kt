package ch.magori.blox.core.board.garden

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the cells of the musical garden, in the Colorful theme only, by day and by night: a board of each
 * kind of cell, in a band or not, under the play head or not. Renaming a test breaks its link to its reference image:
 * regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview
@Composable
private fun GardenCells() = GardenCellsPreview()

@PreviewTest
@Preview(uiMode = UI_MODE_NIGHT)
@Composable
private fun GardenCellsNight() = GardenCellsPreview()
