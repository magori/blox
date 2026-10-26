package ch.magori.blox.feature.cartridges

import androidx.annotation.StringRes
import java.util.Locale

/**
 * A game of BloX, on its cartridge (V26, ADR 0022). The choice of the game shows every entry, in this order: adding a
 * game is adding an entry, with its [label], and `app`, whose `when` over the entries must stay exhaustive, opens its
 * home. The Modern mode is not a cartridge: it is chosen on the home of the Classic.
 */
enum class Cartridge(@param:StringRes internal val label: Int) {
    /** The rules of 1989. */
    CLASSIC(R.string.feature_cartridges_classic),
    ;

    /**
     * The tag of the cartridge, the same in every language, read by the tests and by the journey of `:benchmark`, which
     * copies it as it is: it does not see this module.
     */
    internal val tag: String get() = "cartridge_${name.lowercase(Locale.ROOT)}"
}
