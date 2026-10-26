package ch.magori.blox.core.designsystem.theme

import androidx.compose.runtime.Immutable

/**
 * How rich the effects of the interface are (ADR 0024), carried by [BloxTheme]: [reduced], the information stays and
 * the moves go (docs/design-system/README.md, Retours de jeu, column Effets réduits); [forcedBySystem], reduced because
 * Android removes the animations, whatever the player chose, who cannot turn them back on then.
 */
@Immutable
data class BloxEffects(val reduced: Boolean, val forcedBySystem: Boolean) {
    companion object {
        /** The full effects: what the player has unless asking otherwise, and what the previews draw. */
        val Full: BloxEffects = BloxEffects(reduced = false, forcedBySystem = false)

        /**
         * The effects of the interface, from the choice of the player, [reducedChosen], and whether Android animates,
         * [systemAnimates] (`ValueAnimator.areAnimatorsEnabled()`, false when its animations are removed): reduced
         * and forced while Android does not animate, the choice of the player otherwise. The choice is only read,
         * never overwritten: it comes back as soon as Android animates again.
         */
        fun resolve(
            reducedChosen: Boolean,
            systemAnimates: Boolean,
        ): BloxEffects = if (systemAnimates) {
            BloxEffects(reduced = reducedChosen, forcedBySystem = false)
        } else {
            BloxEffects(reduced = true, forcedBySystem = true)
        }
    }
}
