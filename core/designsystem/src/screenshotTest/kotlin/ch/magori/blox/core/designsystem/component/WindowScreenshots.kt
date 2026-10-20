package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the windows shared by the editor and « Mes musiques », the font of the system normal and twice
 * as large: a name refused, with the free name offered (Original), and a deletion that failed, with its note
 * (Couleurs, where `danger` is red). Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun NamingWindowTakenName() = NamingWindowPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun NamingWindowTakenNameLargeFont() = NamingWindowLargeFontPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun DeleteWindowFailed() = DeleteWindowPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun DeleteWindowFailedLargeFont() = DeleteWindowLargeFontPreview()
