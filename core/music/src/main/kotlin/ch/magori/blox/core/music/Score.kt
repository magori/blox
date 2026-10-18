package ch.magori.blox.core.music

/**
 * A piece of music: [voices] of the same length, a whole number of bars of [beatsPerBar] beats, played
 * together at [bpm] beats per minute.
 */
public data class Score(
    public val title: String,
    public val bpm: Int,
    public val beatsPerBar: Int,
    public val voices: List<Voice>,
) {
    init {
        require(bpm > 0) { "The tempo is a positive number of beats per minute, was $bpm" }
        require(beatsPerBar > 0) { "A bar holds a positive number of beats, was $beatsPerBar" }
        require(voices.isNotEmpty()) { "A score has at least one voice" }
        val lengths = voices.map { it.beats }
        require(lengths.distinct().size == 1) { "All voices have the same length, were $lengths beats" }
        require(lengths.first() % beatsPerBar == 0.0) {
            "Every voice lasts a whole number of bars of $beatsPerBar beats, was ${lengths.first()} beats"
        }
    }
}
