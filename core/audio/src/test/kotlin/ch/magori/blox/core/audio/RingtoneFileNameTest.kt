package ch.magori.blox.core.audio

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RingtoneFileNameTest {
    @Test
    fun `the file is named after the music, with the wav extension`() {
        // When
        val fileName = ringtoneFileName("Ma musique")

        // Then
        assertThat(fileName).isEqualTo("Ma musique.wav")
    }

    @Test
    fun `the characters a file name may not hold are replaced`() {
        // Given: the characters FAT and the Android storage refuse, and a control character
        val name = "a/b\\c:d*e?f\"g<h>i|j\u0007k"

        // When
        val fileName = ringtoneFileName(name)

        // Then
        assertThat(fileName).isEqualTo("a_b_c_d_e_f_g_h_i_j_k.wav")
    }

    @Test
    fun `a name starting with a dot does not make a hidden file`() {
        // Given: a hidden file, which MediaStore does not show, nor the picker of the system (seen on Android 17)
        val name = ".xyz"

        // When
        val fileName = ringtoneFileName(name)

        // Then
        assertThat(fileName).isEqualTo("_xyz.wav")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "?!*", " - / . "])
    fun `a name of spaces or symbols only gives BloX`(name: String) {
        // When
        val fileName = ringtoneFileName(name)

        // Then
        assertThat(fileName).isEqualTo("BloX.wav")
    }
}
