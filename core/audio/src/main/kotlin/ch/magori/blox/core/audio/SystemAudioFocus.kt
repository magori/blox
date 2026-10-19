package ch.magori.blox.core.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/**
 * The [AudioFocus] of `AudioManager`, asked for as [AudioManager.AUDIOFOCUS_GAIN] with the attributes
 * of the music, and the [AudioManager.ACTION_AUDIO_BECOMING_NOISY] broadcast, heard while the focus is
 * held. Both report on the main thread. Kept thin on purpose: it has no JVM test, the logic is in
 * [FocusedMusicPlayer], tested with a fake focus.
 *
 * Since Android 8, the system lowers the music by itself when another app asks for the focus for a
 * while and allows ducking (a notification, a navigation voice: `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`),
 * without a word to BloX. [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK] reaches it only when the
 * system cannot do so, as when the app taking the focus targets a version of Android before 8: it is
 * then a [FocusEvent.TRANSIENT_LOSS], and the music pauses rather than play on at full volume.
 */
public class SystemAudioFocus(context: Context) : AudioFocus {
    private val context: Context = context.applicationContext
    private val audioManager: AudioManager = this.context.getSystemService(AudioManager::class.java)

    /** Who hears the focus, while it is held; null otherwise. */
    private var onEvent: ((FocusEvent) -> Unit)? = null

    private val focusRequest: AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(MUSIC_ATTRIBUTES)
        .setOnAudioFocusChangeListener(::onFocusChange, Handler(Looper.getMainLooper()))
        .build()

    private val becomingNoisy = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent,
        ) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) onEvent?.invoke(FocusEvent.BECOMING_NOISY)
        }
    }

    override fun request(onEvent: (FocusEvent) -> Unit): Boolean {
        if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
        if (this.onEvent == null) {
            context.registerReceiver(becomingNoisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        }
        this.onEvent = onEvent
        return true
    }

    override fun abandon() {
        audioManager.abandonAudioFocusRequest(focusRequest)
        if (onEvent == null) return
        context.unregisterReceiver(becomingNoisy)
        onEvent = null
    }

    private fun onFocusChange(change: Int) {
        val event = when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> FocusEvent.LOSS

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                FocusEvent.TRANSIENT_LOSS

            AudioManager.AUDIOFOCUS_GAIN -> FocusEvent.GAIN

            else -> return
        }
        onEvent?.invoke(event)
    }
}
