package ch.magori.blox.feature.game.navigation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import kotlin.random.Random

/**
 * Which game of the Classic a [GameRoute] plays (ADR 0026): the factory of the view model chooses from it the engines,
 * the music and where the results go.
 */
@Serializable
sealed interface GameVariant {
    /**
     * Whether the game is drawn in the Colorful theme whatever the theme chosen, and its system bars with it (ADR 0026);
     * false when it follows the theme chosen.
     */
    val colorfulOnly: Boolean

    /** The game « Rejouer » puts in place of [finished], a game of this variant, drawing what it needs from [random]. */
    fun replay(
        finished: GameRoute,
        random: Random,
    ): GameRoute

    /** The Classic: a random seed, the level and the music chosen, its score in the records. */
    @Serializable
    data object Classic : GameVariant {
        override val colorfulOnly: Boolean get() = false

        /** A new seed, at the same start level. */
        override fun replay(
            finished: GameRoute,
            random: Random,
        ): GameRoute = finished.copy(seed = random.nextLong())
    }

    /**
     * The daily game of [date] (docs/graine-du-jour.md): the seed of the date, level 5 and the live music imposed, its
     * results in those of the day, never in the records; drawn in the Colorful theme only. [id] tells apart two games
     * of the same date (Rejouer), so that each one gets its own key, and so its own view model (ADR 0011).
     */
    @Serializable
    data class Daily(@Serializable(with = IsoLocalDateSerializer::class) val date: LocalDate, val id: Long) :
        GameVariant {
        override val colorfulOnly: Boolean get() = true

        /** The same date and seed, even after midnight (docs/graine-du-jour.md), under a new id. */
        override fun replay(
            finished: GameRoute,
            random: Random,
        ): GameRoute = finished.copy(variant = copy(id = random.nextLong()))
    }
}

/** A [LocalDate] saved as its ISO text, `2026-10-26`: the route of a daily game is saved with the navigation. */
internal object IsoLocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ch.magori.blox.feature.game.navigation.IsoLocalDate", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: LocalDate,
    ) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}
