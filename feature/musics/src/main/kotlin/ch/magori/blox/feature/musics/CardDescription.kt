package ch.magori.blox.feature.musics

/**
 * What TalkBack says of the info lines of a card, in words rather than as they show (no « · », no « 0:24 »): its
 * [style] and its [key], then its tempo [bpm] per minute and its [spokenLength], or, while it is [listening] to, the
 * bar played, then the day of its last change, [spokenDay], left out when it is unknown. « Calme, fa majeur, 80 temps
 * par minute, 24 secondes, modifiée le 18 octobre ».
 */
internal fun cardInfoDescription(
    style: String,
    key: String,
    bpm: Int,
    spokenLength: String,
    spokenDay: String?,
    listening: Listening?,
): String = listOfNotNull(
    style,
    key,
    if (listening != null) "lecture, mesure ${listening.bar} sur ${listening.bars}" else "$bpm temps par minute",
    spokenLength.takeIf { listening == null },
    spokenDay?.let { "modifiée le $it" },
).joinToString(", ")
