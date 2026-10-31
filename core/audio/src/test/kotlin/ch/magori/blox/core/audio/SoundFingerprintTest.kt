package ch.magori.blox.core.audio

import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.locksmith.PadlockChords
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.GardenSounds
import ch.magori.blox.core.music.LocksmithSounds
import ch.magori.blox.core.music.MusicGenerator
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.RomanNumeral
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Scores
import ch.magori.blox.core.music.SoundEffect
import ch.magori.blox.core.music.SoundEffects
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import kotlin.math.roundToInt

/**
 * Characterization of the sound of BloX before the instruments (P14.2, ADR 0032): the fingerprint of each rendering of
 * today, written on the API of today, so that the Arcade style keeps exactly these samples.
 */
class SoundFingerprintTest {
    private val musicSynthesizer = Synthesizer(sampleRate = MUSIC_SAMPLE_RATE)
    private val effectSynthesizer = Synthesizer(sampleRate = EFFECT_SAMPLE_RATE)

    @Test
    fun `Korobeiniki renders the samples of its fingerprint, at tempo 1 and 1,25`() {
        // When
        val fingerprints = fingerprintsAtBothTempos(Scores.korobeiniki)

        // Then
        assertThat(fingerprints).isEqualTo(mapOf("× 1" to "564480:2a2b40c9", "× 1,25" to "451584:a8ea48df"))
    }

    @Test
    fun `the Minuet renders the samples of its fingerprint, at tempo 1 and 1,25`() {
        // When
        val fingerprints = fingerprintsAtBothTempos(Scores.minuet)

        // Then
        assertThat(fingerprints).isEqualTo(mapOf("× 1" to "481091:90f4126c", "× 1,25" to "384873:4c3ec088"))
    }

    @Test
    fun `a generated music renders the samples of its fingerprint, at tempo 1 and 1,25`() {
        // Given a joyful music of a fixed seed; its fingerprint also follows MusicGenerator and the algorithm of
        // kotlin.random.Random (ADR 0014): a new version of Kotlin may oblige to redo it, which FormTest › the seed 42
        // on joyful gives this form, which pins the order of the draws, would tell at the same time
        val score = MusicGenerator.generate(Category.JOYFUL, seed = 2026, ScoreId.Personal("fingerprint")).score

        // When
        val fingerprints = fingerprintsAtBothTempos(score)

        // Then
        assertThat(fingerprints).isEqualTo(mapOf("× 1" to "618044:b9b14471", "× 1,25" to "494435:553daa9e"))
    }

    @Test
    fun `every effect of the Classic renders the samples of its fingerprint`() {
        // Given every effect of the Classic, by its name
        val effects = mapOf(
            "rotation" to SoundEffects.rotation,
            "lines cleared" to SoundEffects.linesCleared,
            "level up" to SoundEffects.levelUp,
            "four lines cleared" to SoundEffects.fourLinesCleared,
            "game over" to SoundEffects.gameOver,
        )
        assertThat(effects.values).containsExactlyElementsOf(SoundEffects.all)

        // When
        val fingerprints = fingerprintsOf(effects)

        // Then
        assertThat(fingerprints).isEqualTo(
            mapOf(
                "rotation" to "1440:646d18cf",
                "lines cleared" to "7680:677916c1",
                "level up" to "14400:48077d50",
                "four lines cleared" to "21600:b369a792",
                "game over" to "28800:9f316701",
            ),
        )
    }

    @Test
    fun `every effect of the Locksmith renders the samples of its fingerprint`() {
        // Given the arpeggio and the bass of every chord a padlock plays, the degrees I to vi of C major on C4
        // (PadlockChords), then the opening flourish and the refusal
        val key = Scale(Pitch.parse("C4"), ScaleType.MAJOR)
        val numerals = listOf("I", "ii", "iii", "IV", "V", "vi")
        assertThat(key).isEqualTo(PadlockChords.KEY)
        val padlockNumerals = PADLOCK_PIECE_COUNTS.flatMap { PadlockChords.progression(it).numerals }.toSet()
        assertThat(numerals.map(RomanNumeral::parse)).containsExactlyInAnyOrderElementsOf(padlockNumerals)
        val chords = numerals.associateWith { key.chord(RomanNumeral.parse(it)) }
        val effects = chords.flatMap { (numeral, chord) ->
            listOf(
                "arpeggio of $numeral" to LocksmithSounds.arpeggio(chord),
                "bass of $numeral" to LocksmithSounds.bass(chord),
            )
        }.toMap() + mapOf("opening flourish" to LocksmithSounds.openingFlourish, "refusal" to LocksmithSounds.refusal)

        // When
        val fingerprints = fingerprintsOf(effects)

        // Then
        assertThat(fingerprints).isEqualTo(
            mapOf(
                "arpeggio of I" to "21120:095d3ae6",
                "bass of I" to "21120:ceab4717",
                "arpeggio of ii" to "21120:99e335f7",
                "bass of ii" to "21120:73e9f8b3",
                "arpeggio of iii" to "21120:26293f1f",
                "bass of iii" to "21120:0db3dcb2",
                "arpeggio of IV" to "21120:cb70e266",
                "bass of IV" to "21120:61bde025",
                "arpeggio of V" to "21120:616c2d1b",
                "bass of V" to "21120:f0103d7b",
                "arpeggio of vi" to "21120:2e2b6761",
                "bass of vi" to "21120:75929b52",
                "opening flourish" to "20160:c9999be4",
                "refusal" to "8640:82fc24e6",
            ),
        )
    }

    @Test
    fun `every effect of the garden renders the samples of its fingerprint`() {
        // Given the note of every pitch of the garden (GardenMood.allPitches): the rows of its 4 moods, 25 pitches
        val pitches = listOf("C3", "D3", "E3", "G3", "A3") +
            (4..5).flatMap { octave ->
                listOf("C", "D", "D#", "E", "F", "G", "G#", "A", "A#", "B").map { "$it$octave" }
            }
        assertThat(pitches.map(Pitch::parse)).containsExactlyInAnyOrderElementsOf(GardenMood.allPitches)
        val effects = pitches.associate { "note $it" to GardenSounds.note(Pitch.parse(it)) }

        // When
        val fingerprints = fingerprintsOf(effects)

        // Then
        assertThat(fingerprints).isEqualTo(
            mapOf(
                "note C3" to "4800:6794ea5e",
                "note D3" to "4800:0fcdac17",
                "note E3" to "4800:146213c6",
                "note G3" to "4800:4c701ca8",
                "note A3" to "4800:10e8bad6",
                "note C4" to "4800:97ad8a67",
                "note D4" to "4800:c648df5c",
                "note D#4" to "4800:c1375a84",
                "note E4" to "4800:986765b6",
                "note F4" to "4800:45900df4",
                "note G4" to "4800:fd8749b0",
                "note G#4" to "4800:5abdb7fb",
                "note A4" to "4800:86f3465e",
                "note A#4" to "4800:07a6f033",
                "note B4" to "4800:18b3852a",
                "note C5" to "4800:3e074f43",
                "note D5" to "4800:f477ce76",
                "note D#5" to "4800:482da779",
                "note E5" to "4800:d80136b6",
                "note F5" to "4800:19b6a91e",
                "note G5" to "4800:38db33e0",
                "note G#5" to "4800:2655e91d",
                "note A5" to "4800:ca0030b3",
                "note A#5" to "4800:affcb6a2",
                "note B5" to "4800:39c09ab1",
            ),
        )
    }

    /** The fingerprint of each of [effects], by its name, rendered at the sample rate of the effects. */
    private fun fingerprintsOf(effects: Map<String, SoundEffect>): Map<String, String> =
        effects.mapValues { (_, effect) -> fingerprintOf(effectSynthesizer.render(effect)) }

    /** The fingerprint of [score] at each of [TEMPO_FACTORS], rendered at the sample rate of the music. */
    private fun fingerprintsAtBothTempos(score: Score): Map<String, String> =
        TEMPO_FACTORS.mapValues { (_, factor) -> fingerprintOf(musicSynthesizer.render(score, factor)) }

    private companion object {
        /** The sample rate of the effects on most devices, the default of [effectSampleRateOf]: 48 000 Hz. */
        val EFFECT_SAMPLE_RATE = effectSampleRateOf(outputSampleRate = null)

        /** The numbers of pieces of a padlock (Padlock.PIECE_COUNTS, internal to core:locksmith). */
        val PADLOCK_PIECE_COUNTS = 3..8

        val TEMPO_FACTORS = mapOf("× 1" to 1.0, "× 1,25" to 1.25)

        /** The largest sample of 16 bits, that 1 is quantized to. */
        const val FULL_SCALE = 32_767

        /**
         * The fingerprint of [samples]: their count and the CRC-32 of the samples quantized to 16 bits, little-endian,
         * so that a difference of amplitude far below what a 16-bit output plays does not change it. A difference of
         * phase does, however small: a sample of the square on the edge of its period flips from +v to -v (A4 at 48 kHz
         * is exactly at half its period on sample 600), so Arcade keeps the phase `index × frequency / sample rate`.
         */
        fun fingerprintOf(samples: FloatArray): String {
            val bytes = ByteBuffer.allocate(samples.size * Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { bytes.putShort((it.coerceIn(-1f, 1f) * FULL_SCALE).roundToInt().toShort()) }
            val crc = CRC32().apply { update(bytes.array()) }
            return "${samples.size}:%08x".format(crc.value)
        }
    }
}
