package ch.magori.blox.core.data

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.random.Random

/** The name proposed for a music when it is saved (P4.27): an evocative name of its style, followed by its key. */
class ProposedNamesTest {
    @Test
    fun `the key of a major scale is its tonic alone, « en sol »`() {
        // Given
        val scale = Scale(Pitch.parse("G4"), ScaleType.MAJOR)

        // When / Then
        assertThat(keyPhrase(scale)).isEqualTo("en sol")
    }

    @ParameterizedTest(name = "{0} {1}: « {2} »")
    @CsvSource(
        "D4, NATURAL_MINOR, en ré mineur",
        "A4, HARMONIC_MINOR, en la mineur",
        "D4, DORIAN, en ré dorien",
        "G4, MIXOLYDIAN, en sol mixolydien",
        "C4, MAJOR_PENTATONIC, en do",
    )
    fun `the key says minor for a minor scale, natural or harmonic, the mode of the others, nothing for a major one`(
        tonic: String,
        type: ScaleType,
        key: String,
    ) {
        // Given
        val scale = Scale(Pitch.parse(tonic), type)

        // When / Then
        assertThat(keyPhrase(scale)).isEqualTo(key)
    }

    @ParameterizedTest(name = "{0} {1}: « {2} »")
    @CsvSource(
        "C5, MAJOR, en do",
        "B3, MAJOR, en si",
        "C#4, MAJOR, en ré bémol",
        "F#4, MAJOR, en fa dièse",
        "G#4, MAJOR, en la bémol",
        "A#3, NATURAL_MINOR, en si bémol mineur",
        "C#4, NATURAL_MINOR, en do dièse mineur",
        "D#4, NATURAL_MINOR, en ré dièse mineur",
        "G#4, HARMONIC_MINOR, en sol dièse mineur",
        "D#4, DORIAN, en mi bémol dorien",
        "F#4, MIXOLYDIAN, en fa dièse mixolydien",
        "A#4, MIXOLYDIAN, en si bémol mixolydien",
    )
    fun `the tonic is named in French in every octave, its sharp or flat the one of the key signature`(
        tonic: String,
        type: ScaleType,
        key: String,
    ) {
        // Given
        val scale = Scale(Pitch.parse(tonic), type)

        // When / Then
        assertThat(keyPhrase(scale)).isEqualTo(key)
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "JOYFUL | Matin de fête | Jour de marché | Ronde des cerises | Soleil levant",
            "CALM | Brume du lac | Jardin du soir | Sieste au verger | Ciel de lait",
            "EPIC | Bannière au vent | Les remparts | Chevauchée | Marche des géants",
            "MELANCHOLIC | Pluie de novembre | Quai désert | Dernier train | Lettre perdue",
            "MYSTERIOUS | Couloir secret | Ombre du phare | Carte effacée | Lanterne au bois",
            "RETRO | Niveau bonus | Salle d’arcade | Écran titre | Dernière vie",
            "DANCE | Bal du port | Tourbillon | Nuit de fanfare | Pas chassé",
        ],
    )
    fun `a music of a style is proposed one of the four names of its style, drawn at random, followed by its key`(
        style: Category,
        first: String,
        second: String,
        third: String,
        fourth: String,
    ) {
        // Given
        val random = Random(SEED)

        // When
        val proposed = List(DRAWS) { noMusic.proposedName(style, G_MAJOR, bpm = 120, random) }.toSet()

        // Then
        assertThat(proposed).containsExactlyInAnyOrder(
            "$first en sol",
            "$second en sol",
            "$third en sol",
            "$fourth en sol",
        )
    }

    @ParameterizedTest(name = "{0} {1} at {2}: {3}, {4}, {5}")
    @CsvSource(
        delimiter = '|',
        value = [
            "A4 | NATURAL_MINOR | 99 | Rêverie en la mineur | Songe en la mineur | Clair-obscur en la mineur",
            "D4 | DORIAN | 60 | Rêverie en ré dorien | Songe en ré dorien | Clair-obscur en ré dorien",
            "C4 | MAJOR | 99 | Promenade en do | Berceuse en do | Flânerie en do",
            "G4 | MIXOLYDIAN | 90 | Promenade en sol mixolydien | Berceuse en sol mixolydien | Flânerie en sol mixolydien",
            "A4 | HARMONIC_MINOR | 100 | Poursuite en la mineur | Orage en la mineur | Fuite en la mineur",
            "D4 | DORIAN | 200 | Poursuite en ré dorien | Orage en ré dorien | Fuite en ré dorien",
            "C4 | MAJOR_PENTATONIC | 100 | Envolée en do | Galop en do | Cavalcade en do",
            "G4 | MIXOLYDIAN | 150 | Envolée en sol mixolydien | Galop en sol mixolydien | Cavalcade en sol mixolydien",
        ],
    )
    fun `a free music is named after its mode, dorian counted minor, and its tempo, slow under 100 beats a minute`(
        tonic: String,
        type: ScaleType,
        bpm: Int,
        first: String,
        second: String,
        third: String,
    ) {
        // Given
        val random = Random(SEED)
        val scale = Scale(Pitch.parse(tonic), type)

        // When
        val proposed = List(DRAWS) { noMusic.proposedName(style = null, scale, bpm, random) }.toSet()

        // Then
        assertThat(proposed).containsExactlyInAnyOrder(first, second, third)
    }

    @Test
    fun `a name another music has is never proposed while a name of the list is free, the case ignored`() {
        // Given
        val musics = listOf(
            aMusic("a", name = "matin de fête en sol"),
            aMusic("b", name = "JOUR DE MARCHÉ EN SOL"),
            aMusic("c", name = "Ronde des cerises en sol"),
        )
        val random = Random(SEED)

        // When
        val proposed = List(DRAWS) { musics.proposedName(Category.JOYFUL, G_MAJOR, bpm = 120, random) }.toSet()

        // Then
        assertThat(proposed).containsExactly("Soleil levant en sol")
    }

    @Test
    fun `once every name of the list is taken, one of them is proposed with the first number that makes it free`() {
        // Given
        val musics = listOf(
            aMusic("a", name = "Rêverie en la mineur"),
            aMusic("b", name = "Rêverie en la mineur 2"),
            aMusic("c", name = "Songe en la mineur"),
            aMusic("d", name = "Clair-obscur en la mineur"),
        )
        val random = Random(SEED)

        // When
        val proposed = List(DRAWS) { musics.proposedName(style = null, A_MINOR, bpm = 80, random) }.toSet()

        // Then
        assertThat(proposed).containsExactlyInAnyOrder(
            "Rêverie en la mineur 3",
            "Songe en la mineur 2",
            "Clair-obscur en la mineur 2",
        )
    }

    @Test
    fun `a proposed name has 40 characters at the most, numbered or not, whatever the style, the key and the tempo`() {
        // Given every style, Libre included, every tonic, every scale and both kinds of tempo
        val styles = Category.entries + null
        val scales = (0 until 12).flatMap { semitone ->
            ScaleType.entries.map { Scale(Pitch(Pitch.parse("C4").midi + semitone), it) }
        }
        val random = Random(SEED)

        // When every name of its list is taken, or none
        val proposed = styles.flatMap { style ->
            scales.flatMap { scale ->
                listOf(60, 150).flatMap { bpm ->
                    val names = evocativeNames(style, scale, bpm).map { "$it ${keyPhrase(scale)}" }
                    val taken = names.mapIndexed { index, name -> aMusic("$index", name = name) }
                    List(DRAWS / 10) {
                        listOf(
                            noMusic.proposedName(style, scale, bpm, random),
                            taken.proposedName(style, scale, bpm, random),
                        )
                    }.flatten()
                }
            }
        }

        // Then
        assertThat(proposed).allSatisfy { name ->
            assertThat(name.codePointCount(0, name.length)).isLessThanOrEqualTo(SavedMusic.MAX_NAME_LENGTH)
        }
    }

    @Test
    fun `when no name of the list fits in 40 characters once numbered, one is cut so that its number fits`() {
        // Given a name of 40 characters, and that name taken, numbered or not
        val longest = "x".repeat(SavedMusic.MAX_NAME_LENGTH)
        val cut = "x".repeat(SavedMusic.MAX_NAME_LENGTH - 2)
        val musics = listOf(aMusic("a", name = longest), aMusic("b", name = "$cut 2"))

        // When
        val proposed = musics.proposedNameAmong(listOf(longest), Random(SEED))

        // Then
        assertThat(proposed).isEqualTo("$cut 3")
    }

    @ParameterizedTest(name = "{0} {1}: « {2} »")
    @CsvSource(
        "G4, MAJOR, sol majeur",
        "C4, MAJOR_PENTATONIC, do majeur",
        "D4, NATURAL_MINOR, ré mineur",
        "A#3, HARMONIC_MINOR, si bémol mineur",
        "D4, DORIAN, ré dorien",
        "F#4, MIXOLYDIAN, fa dièse mixolydien",
    )
    fun `a key is named by its tonic and its mode, « sol majeur », « ré mineur », « ré dorien »`(
        tonic: String,
        type: ScaleType,
        key: String,
    ) {
        // Given
        val scale = Scale(Pitch.parse(tonic), type)

        // When / Then
        assertThat(scale.keyName()).isEqualTo(key)
    }

    @ParameterizedTest(name = "{0} {1}: « {2} »")
    @CsvSource(
        "G4, MAJOR, sol",
        "C#4, MAJOR, ré bémol",
        "G#4, HARMONIC_MINOR, sol dièse",
        "A#4, MIXOLYDIAN, si bémol",
    )
    fun `the tonic of a scale is named alone, in French`(
        tonic: String,
        type: ScaleType,
        name: String,
    ) {
        // Given
        val scale = Scale(Pitch.parse(tonic), type)

        // When / Then
        assertThat(scale.tonicName()).isEqualTo(name)
    }

    @Test
    fun `the tonic named of every scale on every note is that note, with one sharp or one flat at the most`() {
        // Given every tonic of an octave and every scale
        val letters = mapOf("do" to 0, "ré" to 2, "mi" to 4, "fa" to 5, "sol" to 7, "la" to 9, "si" to 11)
        val accidentals = mapOf("" to 0, " dièse" to 1, " bémol" to -1)
        val scales = (0 until 12).flatMap { semitone ->
            ScaleType.entries.map { Scale(Pitch(Pitch.parse("C4").midi + semitone), it) }
        }

        // When / Then
        assertThat(scales).allSatisfy { scale ->
            val name = scale.tonicName()
            val letter = name.substringBefore(' ')
            val semitone = letters.getValue(letter) + accidentals.getValue(name.removePrefix(letter))
            assertThat(semitone.mod(12)).describedAs(name).isEqualTo(scale.tonic.midi.mod(12))
        }
    }

    private companion object {
        /** The seed of the draws, in each test. */
        const val SEED = 7L

        /** Draws enough for each name of a list to come out. */
        const val DRAWS = 100

        val G_MAJOR = Scale(Pitch.parse("G4"), ScaleType.MAJOR)
        val A_MINOR = Scale(Pitch.parse("A4"), ScaleType.NATURAL_MINOR)

        val noMusic = emptyList<SavedMusic>()
    }
}
