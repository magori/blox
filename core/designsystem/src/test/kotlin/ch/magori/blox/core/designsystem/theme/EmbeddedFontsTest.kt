package ch.magori.blox.core.designsystem.theme

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class EmbeddedFontsTest {
    @Test
    fun `each embedded font has its OFL licence in the assets of the module, which go into the APK`() {
        // Given the fonts embedded in res/font
        val fonts = fontDir.listFiles { file -> file.extension == "ttf" }.orEmpty()
        assertThat(fonts).isNotEmpty

        // Then each one has the licence of its family in the assets, licenses/<family>/OFL.txt, as the OFL requires
        fonts.forEach { font ->
            val licence = File(licensesDir, "${familyOf(font)}/OFL.txt")
            assertThat(licence).withFailMessage { "${font.name} has no licence at $licence" }.isFile
            assertThat(licence.readText()).contains("SIL Open Font License, Version 1.1")
        }
    }

    /** The family of a font file, its name without the weight: press_start_2p_regular.ttf is press_start_2p. */
    private fun familyOf(font: File): String = font.nameWithoutExtension.substringBeforeLast('_')

    private val fontDir = directory("blox.fontDir")
    private val licensesDir = directory("blox.licensesDir")

    /** A folder whose path the build gives in the system property [name]. */
    private fun directory(name: String): File = File(
        checkNotNull(System.getProperty(name)) {
            "The build gives the path of a folder in the system property $name"
        },
    )
}
