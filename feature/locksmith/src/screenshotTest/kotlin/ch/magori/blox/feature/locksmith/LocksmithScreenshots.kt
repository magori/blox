package ch.magori.blox.feature.locksmith

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the screen of the Locksmith (view V35), by day and by night, under the Original theme of the
 * application: the screen draws itself in the Colorful theme. From the previews of the production code, with the same
 * `@Preview` configuration. Renaming a test breaks its link to its reference image: regenerate them with
 * `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LocksmithDay() = LocksmithDayPreview()

@PreviewTest
@Preview(widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT)
@Composable
private fun LocksmithNight() = LocksmithNightPreview()
