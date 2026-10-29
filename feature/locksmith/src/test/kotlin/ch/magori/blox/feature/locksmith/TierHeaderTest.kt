package ch.magori.blox.feature.locksmith

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The header of a tier on the screen « Serrures »: its name, then how many pieces its padlocks hold. */
class TierHeaderTest {
    @Test
    fun `the pieces of a tier read as one count, two counts apart by « ou », or a range by « à »`() {
        assertThat(piecesTextOf(6..6)).isEqualTo(R.string.feature_locksmith_padlocks_tier_pieces)
        assertThat(piecesTextOf(7..8)).isEqualTo(R.string.feature_locksmith_padlocks_tier_pieces_or)
        assertThat(piecesTextOf(3..5)).isEqualTo(R.string.feature_locksmith_padlocks_tier_pieces_range)
    }
}
