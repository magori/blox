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
internal class NameTextsReadingTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("a name taken is told by what is named, a blank name alike for all, a long one by its most characters")
    fun aNameRefusedIsToldByWhatIsNamed() = extension.use {
        // When
        setContent {
            Text(
                listOf(
                    nameRefusalText(NameRefusal.BLANK, NamedThing.MUSIC, maxLength = 40),
                    nameRefusalText(NameRefusal.TAKEN, NamedThing.MUSIC, maxLength = 40),
                    nameRefusalText(NameRefusal.BLANK, NamedThing.GARDEN, maxLength = 40),
                    nameRefusalText(NameRefusal.TAKEN, NamedThing.GARDEN, maxLength = 40),
                    nameRefusalText(NameRefusal.TOO_LONG, NamedThing.GARDEN, maxLength = 40),
                ).joinToString(" / "),
            )
        }

        // Then
        onNodeWithText(
            "Donne-lui un nom. / Une autre musique porte déjà ce nom. / Donne-lui un nom. / " +
                "Un autre jardin porte déjà ce nom. / 40 caractères au plus.",
        ).assertExists()
    }
}
