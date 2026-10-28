package ch.magori.blox.core.board.garden

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the thumbnail of a garden, in the Colorful theme only, by day and by night: a garden of every
 * piece, in a band or not, then an empty garden, on a row of a list. Renaming a test breaks its link to its reference
 * image: regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview
@Composable
private fun GardenThumbnails() = GardenThumbnailsPreview()

@PreviewTest
@Preview(uiMode = UI_MODE_NIGHT)
@Composable
private fun GardenThumbnailsNight() = GardenThumbnailsPreview()
