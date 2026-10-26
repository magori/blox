package ch.magori.blox.core.board.lock

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the cells of the padlock of the Locksmith, in the Colorful theme only, by day and by night: a
 * board of each kind of cell. Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview
@Composable
private fun LockCells() = LockCellsPreview()

@PreviewTest
@Preview(uiMode = UI_MODE_NIGHT)
@Composable
private fun LockCellsNight() = LockCellsPreview()
