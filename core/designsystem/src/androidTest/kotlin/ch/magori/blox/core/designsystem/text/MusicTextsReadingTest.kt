package ch.magori.blox.core.designsystem.text

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import de.mannodermaus.junit5.compose.createComposeExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class MusicTextsReadingTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("a chord is read by its degree, then its accidental, then its quality, chords apart by a comma")
    fun aChordIsReadByItsDegreeThenItsAccidentalThenItsQuality() = extension.use {
        // Given the flat seventh of Mystère between two chords
        val tellings = listOf(
            ChordTelling(1, SpokenChordQuality.MINOR),
            ChordTelling(7, SpokenChordQuality.MAJOR, SpokenAccidental.FLAT),
            ChordTelling(2, SpokenChordQuality.DIMINISHED, SpokenAccidental.SHARP),
        )

        // When
        setContent { Text(chordsText(tellings)) }

        // Then
        onNodeWithText("1 mineur, 7 bémol majeur, 2 dièse diminué").assertExists()
    }

    @Test
    @DisplayName("a note is read in lower case, flat or sharp after its name")
    fun aNoteIsReadInLowerCaseFlatOrSharpAfterItsName() = extension.use {
        // When
        setContent {
            Text(
                listOf(
                    noteText(NoteStep.MI),
                    noteText(NoteStep.MI, SpokenAccidental.FLAT),
                    noteText(NoteStep.FA, SpokenAccidental.SHARP),
                ).joinToString(" / "),
            )
        }

        // Then
        onNodeWithText("mi / mi bémol / fa dièse").assertExists()
    }
}
