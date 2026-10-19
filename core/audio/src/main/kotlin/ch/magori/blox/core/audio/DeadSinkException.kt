package ch.magori.blox.core.audio

/**
 * Thrown by [AudioSink.write] when the output is dead, not merely in error: the audio server restarted,
 * or the track could not follow a change of output. A recent Android reroutes most changes of output
 * (Bluetooth headphones connected or gone) by itself, without killing the track; a dead track is rarer.
 * A new sink may play again, the sequencer opens one; any other failure of a write stops the music.
 */
public class DeadSinkException(message: String) : IllegalStateException(message)
