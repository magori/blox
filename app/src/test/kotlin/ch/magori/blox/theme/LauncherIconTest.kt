package ch.magori.blox.theme

import androidx.compose.ui.graphics.toArgb
import ch.magori.blox.core.board.logoColors
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import kotlin.math.hypot

class LauncherIconTest {
    @Test
    fun `the four cells of the icon are the logo colours of the Original theme, in reading order`() {
        // Given the foreground of the adaptive icon
        val foreground = appResource("drawable/ic_launcher_foreground.xml")

        // When the cells are read as on the screen: by row, then by column, from where each one starts
        val fills = elements(foreground, "//path")
            .sortedWith(compareBy({ start(it).second }, { start(it).first }))
            .map { it.getAttributeNS(ANDROID_NAMESPACE, "fillColor") }

        // Then
        val logo = logoColors(BloxThemeVariant.ORIGINAL)
        assertThat(fills)
            .withFailMessage { "The cells of the icon are $fills, not the logo colours of the Original theme" }
            .containsExactlyElementsOf(
                listOf(logo.topLeft, logo.topRight, logo.bottomLeft, logo.bottomRight).map {
                    "#%08X".format(it.toArgb())
                },
            )
    }

    @Test
    fun `the adaptive icon has the page of the Original theme behind, the cells in front, and a monochrome layer`() {
        // Given
        val icon = appResource("mipmap-anydpi/ic_launcher.xml")

        // Then
        assertThat(drawableOf(icon, "background")).isEqualTo("@color/blox_window_background")
        assertThat(drawableOf(icon, "foreground")).isEqualTo("@drawable/ic_launcher_foreground")
        assertThat(drawableOf(icon, "monochrome")).isEqualTo("@drawable/ic_launcher_monochrome")
    }

    @Test
    fun `the application has no round icon, as every icon is adaptive from Android 8`() {
        // Given
        val application = element(appManifest(), "/manifest/application")

        // Then
        assertThat(application.hasAttributeNS(ANDROID_NAMESPACE, "roundIcon"))
            .withFailMessage { "The manifest declares android:roundIcon, useless with a minSdk of 26" }
            .isFalse()
    }

    @Test
    fun `the splash screen of Android 12 shows the page of the Original theme and the icon`() {
        // Given the theme of the window from Android 12
        val theme = appResource("values-v31/themes.xml")

        // Then
        assertThat(item(theme, "android:windowSplashScreenBackground")).isEqualTo("@color/blox_window_background")
        assertThat(item(theme, "android:windowSplashScreenAnimatedIcon")).isEqualTo("@drawable/ic_launcher_foreground")
    }

    @Test
    fun `the theme of Android 12 inherits the base theme and only adds the splash screen`() {
        // Given the theme of the window from Android 12
        val theme = appResource("values-v31/themes.xml")

        // Then it takes the window background from the base theme, declared once in values/themes.xml
        assertThat(element(theme, "/resources/style[@name='Theme.Blox']").getAttribute("parent"))
            .isEqualTo("Base.Theme.Blox")
        assertThat(elements(theme, "/resources/style[@name='Theme.Blox']/item[@name='android:windowBackground']"))
            .isEmpty()
    }

    @Test
    fun `every corner of the cells is in the safe zone of the adaptive icon, whatever the mask`() {
        // Given the foreground, 108 dp, whose centre circle of 66 dp is never cut by the mask of the launcher
        val foreground = appResource("drawable/ic_launcher_foreground.xml")

        // When
        val corners = elements(foreground, "//path").flatMap { corners(pathData(it)) }

        // Then each corner is at most 33 dp from the centre (54, 54)
        assertThat(corners).hasSize(16)
        corners.forEach { (x, y) ->
            val distance = hypot(x - 54f, y - 54f)
            assertThat(distance)
                .withFailMessage { "The corner ($x, $y) is $distance dp from the centre, out of the safe zone" }
                .isLessThanOrEqualTo(33f)
        }
    }

    @Test
    fun `the monochrome layer has the very shape of the cells in front`() {
        // Given
        val foreground = appResource("drawable/ic_launcher_foreground.xml")
        val monochrome = appResource("drawable/ic_launcher_monochrome.xml")

        // When
        val cells = elements(foreground, "//path").joinToString("") { pathData(it) }
        val shape = pathData(element(monochrome, "//path"))

        // Then
        assertThat(shape).isEqualTo(cells)
    }

    private fun pathData(path: Element): String = path.getAttributeNS(ANDROID_NAMESPACE, "pathData")

    /** The four corners of a cell drawn as `Mx,yhSvSh-Sz`; any other outline fails, so that none goes unchecked. */
    private fun corners(pathData: String): List<Pair<Float, Float>> {
        val match = checkNotNull(SQUARE.matchEntire(pathData)) { "The path $pathData is not a square Mx,yhSvSh-Sz" }
        val (x, y, side) = match.destructured.toList().map { it.toFloat() }
        return listOf(x to y, x + side to y, x to y + side, x + side to y + side)
    }

    /** The point (x, y) where the outline of [path] starts: its first `Mx,y`. */
    private fun start(path: Element): Pair<Float, Float> {
        val data = pathData(path)
        val match = checkNotNull(MOVE.find(data)) { "The path $data does not start with Mx,y" }
        return match.groupValues[1].toFloat() to match.groupValues[2].toFloat()
    }

    private fun drawableOf(
        icon: Document,
        layer: String,
    ): String = element(icon, "/adaptive-icon/$layer").getAttributeNS(ANDROID_NAMESPACE, "drawable")

    private fun item(
        theme: Document,
        name: String,
    ): String = element(theme, "/resources/style[@name='Theme.Blox']/item[@name='$name']").textContent.trim()

    private companion object {
        /** The first move of a path: `M` followed by its absolute x and y. */
        val MOVE = Regex("""^\s*M\s*([\d.]+)\s*,\s*([\d.]+)""")

        /** A square cell: its top left corner, then its side three times (right, down, back left). */
        val SQUARE = Regex("""M([\d.]+),([\d.]+)h([\d.]+)v\3h-\3z""")
    }
}
