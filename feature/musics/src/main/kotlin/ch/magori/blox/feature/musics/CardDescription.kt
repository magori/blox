package ch.magori.blox.feature.musics

/**
 * What TalkBack says of the info line of a card, in words rather than as it shows (no « · », no « 0:24 »): its
 * [style], its [key] and its [spokenLength], « Calme, fa majeur, 24 secondes »; or, while it is [listening] to, the
 * bar played alone, as the line shows it: « Lecture, mesure 3 sur 8 ».
 */
internal fun cardInfoDescription(
    style: String,
    key: String,
    spokenLength: String,
    listening: Listening?,
): String = if (listening != null) {
    "Lecture, mesure ${listening.bar} sur ${listening.bars}"
} else {
    listOf(style, key, spokenLength).joinToString(", ")
}
