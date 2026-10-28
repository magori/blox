package ch.magori.blox.feature.game.controls

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces

/**
 * The sizes of the controls that do not change with the width, in one place; those that do are [KeySizes]. Every key
 * is at least [BloxSizes.minTouchTarget], the accessibility minimum.
 */
internal object ControlSizes {
    /** Between two arrows. */
    val spacing = BloxSpaces.space2

    /** Between B and A (asked by the developer, 2026-10-26, rather than the 8 dp of the design system). */
    val rotationSpacing = BloxSpaces.space4

    /** Between the two groups, side by side: 18 dp at least (docs/design-system/components/GameKey). */
    val groupGap = 18.dp

    /** B sits lower than A, as on a handheld console. */
    val rotationStagger = 24.dp

    /** The triangle of an arrow, drawn on its grid of 22. */
    val arrowGlyph = 24.dp

    /** The pictogram of rotation of B and A, alone on its key. */
    val rotationGlyph = 28.dp
}

/**
 * The sizes of the keys at a width, tokens of the design system held by [BloxSizes]: the side of the arrows, [arrow],
 * and the diameter of B and A, [rotation].
 */
@Immutable
internal data class KeySizes(val arrow: Dp, val rotation: Dp) {
    /** The group ◀ ▼ ▶. */
    val directionsWidth: Dp get() = arrow * 3 + ControlSizes.spacing * 2

    /** How far ▼ stands below ◀ and ▶: half a key (asked by the developer, 2026-10-28). */
    val arrowRaise: Dp get() = arrow / 2

    /** The group ◀ ▼ ▶, ▼ lower by [arrowRaise]. */
    val directionsHeight: Dp get() = arrow + arrowRaise

    /** The group B A. */
    val rotationsWidth: Dp get() = rotation * 2 + ControlSizes.rotationSpacing

    /** The group B A, B lower by [ControlSizes.rotationStagger]. */
    val rotationsHeight: Dp get() = rotation + ControlSizes.rotationStagger

    /** Both groups side by side, [ControlSizes.groupGap] apart. */
    val width: Dp get() = directionsWidth + ControlSizes.groupGap + rotationsWidth

    companion object {
        /** On a phone (`key-arrow`, `key-rotate`). */
        val Phone = KeySizes(BloxSizes.keyArrow, BloxSizes.keyRotate)

        /** In a narrow window (`key-arrow-compact`, `key-rotate-compact`). */
        val Compact = KeySizes(BloxSizes.keyArrowCompact, BloxSizes.keyRotateCompact)

        /** Halfway between [Phone] and [Compact], for a phone a little too narrow for the first. */
        val Halfway = KeySizes((Phone.arrow + Compact.arrow) / 2, (Phone.rotation + Compact.rotation) / 2)

        /** From the largest to the smallest. */
        val all = listOf(Phone, Halfway, Compact)
    }
}

/**
 * The keys whose two groups fit a space [width] wide side by side, from the largest; the compact ones alone when none
 * does.
 */
internal fun fittingKeySizes(width: Dp): List<KeySizes> =
    KeySizes.all.filter { it.width <= width }.ifEmpty { listOf(KeySizes.Compact) }

/** The keys for a space [width] wide: the largest whose two groups fit it side by side, else the compact ones. */
internal fun keySizes(width: Dp): KeySizes = fittingKeySizes(width).first()

/** How far the key goes down while it is [held]: as far as a pressed button, [BloxSizes.pressDrop], or not at all. */
internal fun keyDrop(held: Boolean): Dp = if (held) BloxSizes.pressDrop else 0.dp
