package ch.magori.blox.feature.cartridges

import androidx.annotation.StringRes
import ch.magori.blox.core.board.mascot.MascotCharacter
import java.util.Locale

/**
 * A game of BloX, on its cartridge (V26, ADR 0022). The choice of the game shows every entry, in this order: adding a
 * game is adding an entry, with its [label] and its [mascot], and `app`, whose `when` over the entries must stay
 * exhaustive, opens its home. The Modern mode is not a cartridge: it is chosen on the home of the Classic.
 */
enum class Cartridge(@param:StringRes internal val label: Int, internal val mascot: MascotCharacter) {
    /** The rules of 1989. */
    CLASSIC(R.string.feature_cartridges_classic, MascotCharacter.CLASSIC),

    /** The Locksmith, a puzzle of pieces and chords (ADR 0025). */
    LOCKSMITH(R.string.feature_cartridges_locksmith, MascotCharacter.LOCKSMITH),
    ;

    /**
     * The tag of the cartridge, the same in every language, read by the tests and by the journey of `:benchmark`, which
     * copies it as it is: it does not see this module.
     */
    internal val tag: String get() = "cartridge_${name.lowercase(Locale.ROOT)}"

    /** The tag of the mascot on the cartridge, a decoration, read by the tests only. */
    internal val mascotTag: String get() = "${tag}_mascot"
}
