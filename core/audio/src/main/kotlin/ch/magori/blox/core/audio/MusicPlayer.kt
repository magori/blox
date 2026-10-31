package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Music
import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Score
import kotlinx.coroutines.flow.Flow

/**
 * What a game needs of the music: play a score, or a music file of the player, in a loop, switch to another score
 * at the bar line, and change the tempo of the scores. A [MusicFile] plays at its own speed: the tempo of the danger
 * belongs to the BloX scores. [MusicSwitch] plays both kinds, one at a time.
 */
public typealias MusicPlayer = Player<Music>

/**
 * Plays a kind [M] of music in a loop, at the volume set ([VolumeControl]), the scores in the sound style set
 * ([SoundStyleControl]), and changes the tempo of the scores.
 * Its failures never reach the caller: the music stops, the game goes on, and [Playback.isPlaying] tells it.
 * [ChiptuneSequencer] plays the scores of the app, a [Player] of [Score]; it reports its failures to its
 * `onFailure`, called on a thread of `Dispatchers.IO`, not on the main thread. A player of every [Music] is a
 * player of scores too, so that the contract of the players is written once, for the scores.
 */
public interface Player<in M : Music> :
    VolumeControl,
    SoundStyleControl {
    /**
     * Plays [music] in a loop, from its start and, for a score, at its tempo (1.0), in place of the music
     * playing, if any: a tempo asked for before is forgotten, so that a music played anew (a preview, a new
     * game) is never left faster by the one before; so are the tempos prepared. The [Playback] stops this
     * music only: once another one replaced it, stopping it does nothing.
     */
    public fun play(music: M): Playback

    /**
     * Sets the tempo factor of the scores: 1.0 is the tempo of the score; a music file is not changed. Kept,
     * when nothing plays, for the next [Playback.resume], not for the next [play].
     */
    public fun setTempoFactor(factor: Double)

    /**
     * Has the score played last ready at the tempo [factor] before it is asked for: a game prepares its
     * danger tempo as it starts, so that entering the danger, the most tense moment of the game, renders
     * nothing. Kept for the [Playback.resume] of that score, forgotten by the next [play]: a preview
     * prepares nothing.
     */
    public fun prepareTempo(factor: Double)

    /**
     * Goes on with [score] in place of the score played last: the score playing ends its bar, then [score]
     * starts on the first beat of its bar of the same rank (modulo its length), at the tempo factor asked
     * for, with the tempos prepared, once it is rendered. The [Playback] stays the same: it stops [score], and
     * resumes it. A second switch before the bar line replaces the first. While the music is stopped, or
     * stopped by itself, [score] is only kept, to be played at the resume, from the first beat of its bar of the
     * same rank as the bar the music stopped in. Reserved to the scores: it does nothing while a music file plays,
     * nor before anything is played.
     */
    public fun switchTo(score: Score)
}

/** A music played by a [Player]: a score, or a music file, which resumes from the place it stopped at. */
public interface Playback {
    /**
     * Whether this score still plays: true from [MusicPlayer.play] or [resume], false for good once it is
     * stopped, replaced by another score, or once the music stops by itself (the sequencer fails; with
     * [FocusedMusicPlayer], the focus lost for good or the headphones unplugged). A score has no natural
     * end: it loops. With [FocusedMusicPlayer], a score waiting for the focus plays (true): the music comes
     * back by itself with the focus; a score refused the focus is false at once. A state: it gives the
     * current value at once, then each change, possibly from another thread (the sequencer tells a failure
     * from a thread of `Dispatchers.IO`). Follow it on the main thread: [FocusedMusicPlayer] tells no
     * passing `false` (a pause of the focus, a resume) only to a follower on that thread.
     */
    public val isPlaying: Flow<Boolean>

    /** Stops this score; does nothing if it was replaced by another one, or stopped already. */
    public fun stop()

    /**
     * Plays the same score again from where this playback stopped, at the start of the beat it was in,
     * so that no note is cut, and at the tempo asked for now; in place of the score playing, if any.
     * A playback that never wrote anything resumes from where it started: the start, unless it was
     * resumed itself. Gives the new playback.
     *
     * The place kept is that of the last slice written, ahead of what was heard: the buffer of the
     * sink is dropped on stop (about 100 to 350 ms with `AudioTrack`). Going back to the start of the
     * beat makes up for it most of the time; at worst, the music resumes past a bit never heard.
     * The place is read on the main thread: called right after [stop], before the loop has seen it,
     * it may be a slice late (about 23 ms), which a game does not hear.
     */
    public fun resume(): Playback
}
