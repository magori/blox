package ch.magori.blox.theme

import androidx.compose.ui.graphics.toArgb
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class WindowBackgroundTest {
    @Test
    fun `the window background is the page colour of the Original theme`() {
        // Given the colour of the window, shown before Compose draws anything
        val windowBackground = colorValue(name = "blox_window_background")

        // Then it is the page of the Original theme, the default one
        assertThat(windowBackground)
            .withFailMessage { "blox_window_background is $windowBackground, not #AARRGGBB in upper case" }
            .matches("#[0-9A-F]{8}")
        assertThat(windowBackground)
            .withFailMessage { "blox_window_background ($windowBackground) is not BloxPalettes.Original.page" }
            .isEqualTo("#%08X".format(BloxPalettes.Original.page.toArgb()))
    }

    /** The value of the colour [name] in colors.xml, whose path the build gives in `blox.colorsXml`. */
    private fun colorValue(name: String): String {
        val path = checkNotNull(System.getProperty("blox.colorsXml")) {
            "The build gives the path of colors.xml in the system property blox.colorsXml"
        }
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        val color = XPathFactory.newInstance().newXPath()
            .evaluate("/resources/color[@name='$name']", document, XPathConstants.NODE) as Element?
        return checkNotNull(color) { "colors.xml has no colour named $name" }.textContent.trim()
    }
}
