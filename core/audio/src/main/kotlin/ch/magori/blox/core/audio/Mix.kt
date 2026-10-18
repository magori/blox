package ch.magori.blox.core.audio

/**
 * Mixes [tracks] of the same length by adding them sample by sample. The sum is clipped to [−1, 1];
 * with the volumes of the scores (0.07 and 0.16), it never needs to be.
 */
internal fun mix(tracks: List<FloatArray>): FloatArray {
    require(tracks.isNotEmpty()) { "Mixing needs at least one track" }
    val lengths = tracks.map { it.size }
    require(lengths.distinct().size == 1) { "Mixed tracks have the same length, were $lengths samples" }
    val mixed = FloatArray(lengths.first())
    for (track in tracks) {
        for (index in mixed.indices) mixed[index] += track[index]
    }
    for (index in mixed.indices) mixed[index] = mixed[index].coerceIn(-1f, 1f)
    return mixed
}
