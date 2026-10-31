package ch.magori.blox.core.audio

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * `MediaStore` really writes a ringtone or an alarm in the shared storage, from Android 10 on (the device of the test
 * is); the rendering and the cleaning after a failure are tested on the JVM (`RingtoneWriterTest`). Every file written
 * is removed after each test.
 */
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
class MediaStoreRingtoneExporterTest {
    private val resolver: ContentResolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver

    // A small synthesizer, so that a file of 20 s stays light: 160 000 samples.
    private val exporter = MediaStoreRingtoneExporter(Synthesizer(sampleRate = 8_000), resolver, Dispatchers.IO)

    // A loop of 4 beats at 120 bpm: 2 s.
    private val score = Score(
        ScoreId.Personal("ringtone"),
        bpm = 120,
        beatsPerBar = 4,
        voices = listOf(Voice(Waveform.SQUARE, volume = 0.1, notes = List(4) { Note(Pitch.parse("A4"), beats = 1.0) })),
    )

    private val written = mutableListOf<Uri>()

    @AfterEach
    fun removeTheFilesWritten() = written.forEach { resolver.delete(it, null, null) }

    @Test
    @DisplayName("a ringtone is written in Ringtones BloX, marked as a ringtone and no longer pending")
    fun aRingtoneIsWrittenInRingtones() {
        // When
        val uri = export("Sonnerie de test", RingtoneKind.RINGTONE)

        // Then
        val columns = columnsOf(uri)
        assertThat(columns[MediaStore.Audio.Media.RELATIVE_PATH]).isEqualTo("Ringtones/BloX/")
        assertThat(columns[MediaStore.Audio.Media.IS_RINGTONE]).isEqualTo("1")
        assertThat(columns[MediaStore.Audio.Media.IS_PENDING]).isEqualTo("0")
    }

    @Test
    @DisplayName("an alarm is written in Alarms BloX, marked as an alarm")
    fun anAlarmIsWrittenInAlarms() {
        // When
        val uri = export("Réveil de test", RingtoneKind.ALARM)

        // Then
        val columns = columnsOf(uri)
        assertThat(columns[MediaStore.Audio.Media.RELATIVE_PATH]).isEqualTo("Alarms/BloX/")
        assertThat(columns[MediaStore.Audio.Media.IS_ALARM]).isEqualTo("1")
        assertThat(columns[MediaStore.Audio.Media.IS_RINGTONE]).isEqualTo("0")
        assertThat(columns[MediaStore.Audio.Media.IS_PENDING]).isEqualTo("0")
    }

    @Test
    @DisplayName("the title of the file is the name of the music")
    fun theTitleIsTheNameOfTheMusic() {
        // When
        val uri = export("Pluie du matin", RingtoneKind.RINGTONE)

        // Then: the file is named after it, and titled by it, as the picker of the system shows; MediaStore takes the
        // title from the name of the file, which an application may not change (an update of TITLE changes no row)
        val columns = columnsOf(uri)
        assertThat(columns[MediaStore.Audio.Media.DISPLAY_NAME]).isEqualTo("Pluie du matin.wav")
        assertThat(columns[MediaStore.Audio.Media.TITLE]).isEqualTo("Pluie du matin")
    }

    @Test
    @DisplayName("the file written reads back as the WAV encoded")
    fun theFileReadsBackAsTheWavEncoded() {
        // When
        val uri = export("Lecture de test", RingtoneKind.ALARM)

        // Then
        val bytes = requireNotNull(resolver.openInputStream(uri)).use { it.readBytes() }
        val rendering = Synthesizer(sampleRate = 8_000).render(score)
        val wav = WavEncoder.encode(ringtoneSamples(rendering, sampleRate = 8_000, samplesPerBar = 16_000.0), 8_000)
        assertThat(bytes).isEqualTo(wav)
    }

    @Test
    @DisplayName("two exports of the same name give two files, the second titled Nom (1)")
    fun twoExportsOfTheSameNameGiveTwoFiles() {
        // When
        val first = export("Nom", RingtoneKind.RINGTONE)
        val second = export("Nom", RingtoneKind.RINGTONE)

        // Then
        assertThat(second).isNotEqualTo(first)
        assertThat(columnsOf(first)[MediaStore.Audio.Media.TITLE]).isEqualTo("Nom")
        assertThat(columnsOf(second)[MediaStore.Audio.Media.DISPLAY_NAME]).isEqualTo("Nom (1).wav")
        assertThat(columnsOf(second)[MediaStore.Audio.Media.TITLE]).isEqualTo("Nom (1)")
    }

    private fun export(
        name: String,
        kind: RingtoneKind,
    ): Uri = Uri.parse(runBlocking { exporter.export(score, name, kind) }).also(written::add)

    /** The columns of the file at [uri] this test reads, as text. */
    private fun columnsOf(uri: Uri): Map<String, String?> {
        val projection = arrayOf(
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_ALARM,
            MediaStore.Audio.Media.IS_PENDING,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        return requireNotNull(resolver.query(uri, projection, null, null, null)).use { cursor ->
            assertThat(cursor.moveToFirst()).describedAs("The file is in MediaStore").isTrue()
            projection.associateWith { cursor.getString(cursor.getColumnIndexOrThrow(it)) }
        }
    }
}
