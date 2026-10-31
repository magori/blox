package ch.magori.blox.core.audio

/**
 * Samples per second of the music: 44 100 Hz, the rate of a CD, which every Android device plays. The sequencer plays
 * the music at this rate, and a ringtone is written at it (ADR 0031): the same synthesizer renders both.
 */
public const val MUSIC_SAMPLE_RATE: Int = 44_100
