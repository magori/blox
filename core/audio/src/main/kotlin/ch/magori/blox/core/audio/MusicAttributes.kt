package ch.magori.blox.core.audio

import android.media.AudioAttributes

/** What the music of BloX is to the system: music of a game. The sink plays with it, the focus asks with it. */
internal val MUSIC_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_GAME)
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .build()
