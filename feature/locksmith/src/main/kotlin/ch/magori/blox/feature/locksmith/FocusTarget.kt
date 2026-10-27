package ch.magori.blox.feature.locksmith

/** What the screen gives the focus to: the title of the card « Serrure ouverte ! », or the message under the padlock. */
internal enum class FocusTarget { CARD_TITLE, MESSAGE }

/**
 * Where the focus goes once the padlock, which [wasOpen] or not, now [isOpen] or not: on the title of the card when it
 * opens, the one text that tells it; on the message when it starts over from the card (Rejouer cette serrure), the
 * card gone; nowhere otherwise, as on coming back to the screen.
 */
internal fun focusAfter(
    wasOpen: Boolean,
    isOpen: Boolean,
): FocusTarget? = when {
    isOpen && !wasOpen -> FocusTarget.CARD_TITLE
    wasOpen && !isOpen -> FocusTarget.MESSAGE
    else -> null
}
