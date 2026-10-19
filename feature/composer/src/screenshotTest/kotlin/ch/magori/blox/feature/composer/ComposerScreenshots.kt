package ch.magori.blox.feature.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

/*
 * Screenshot tests of the music editor (views V8 and V11), in both themes coded so far, from the previews of the
 * production code, with the same `@Preview` configuration. Renaming a test breaks its link to its reference image:
 * regenerate them with `./gradlew updateDebugScreenshotTest`.
 */

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun ComposerOriginal() = ComposerOriginalPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun ComposerColorful() = ComposerColorfulPreview()

@PreviewTest
@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun ComposerEmpty() = ComposerEmptyPreview()
