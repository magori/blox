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

    @Test
    fun `the embedded families are Press Start 2P, Manrope, Unbounded and Bagel Fat One, without Chakra Petch`() {
        // Given the fonts embedded in res/font and the licences in the assets
        val fonts = fontDir.listFiles().orEmpty().map(::familyOf)
        val licences = licensesDir.listFiles().orEmpty().map { it.name }

        // Then each family is there once, as a font and as a licence, and Chakra Petch went with Colorful v1
        val families = listOf("press_start_2p", "manrope", "unbounded", "bagel_fat_one")
        assertThat(fonts).containsExactlyInAnyOrderElementsOf(families)
        assertThat(licences).containsExactlyInAnyOrderElementsOf(families)
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
