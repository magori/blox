package ch.magori.blox.core.music

/** A piece of music: [voices] played together at [bpm] beats per minute. */
public data class Score(public val title: String, public val bpm: Int, public val voices: List<Voice>) {
    init {
        require(bpm > 0) { "The tempo is a positive number of beats per minute, was $bpm" }
        require(voices.isNotEmpty()) { "A score has at least one voice" }
    }
}
