package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Scores
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

/** The fake shared by the tests of the features keeps the promises of [RingtoneExporter] the sheet relies on. */
class FakeRingtoneExporterTest {
    @Test
    fun `each export of the fake gets a URI of its own`() = runTest {
        // Given
        val fake = FakeRingtoneExporter()

        // When
        val first = fake.export(Scores.korobeiniki, "Ma musique", RingtoneKind.RINGTONE)
        val second = fake.export(Scores.korobeiniki, "Ma musique", RingtoneKind.RINGTONE)

        // Then
        assertThat(first).startsWith("content://").isNotEqualTo(second)
    }

    @Test
    fun `the fake fails while its failure is set, and notes the export asked for`() = runTest {
        // Given
        val failure = IOException("The storage is full")
        val fake = FakeRingtoneExporter().apply { this.failure = failure }

        // When
        val thrown = runCatching { fake.export(Scores.korobeiniki, "Ma musique", RingtoneKind.ALARM) }

        // Then
        assertThat(thrown.exceptionOrNull()).isSameAs(failure)
        assertThat(fake.exports).containsExactly(
            FakeRingtoneExporter.Export(Scores.korobeiniki, "Ma musique", RingtoneKind.ALARM),
        )
    }
}
