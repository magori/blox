package ch.magori.blox.theme

import android.animation.ValueAnimator
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** The setting of Android that removes its animations (Supprimer les animations): what it says, and when it changes. */
interface AnimationSetting {
    /** Each change of the setting, as Android tells of it. */
    val changes: Flow<Unit>

    /** Whether Android animates now: false when its animations are removed (a duration scale of 0). */
    fun animates(): Boolean
}

/**
 * Whether Android animates, [animate], read from its [setting] as the application starts and again at each of its
 * changes, for the whole life of the process, in [scope]: a change is seen at once, even by a window that is never
 * paused (split screen). When it does not, the reduced effects are imposed
 * ([ch.magori.blox.core.designsystem.theme.BloxEffects.resolve]).
 */
class SystemAnimations(setting: AnimationSetting, scope: CoroutineScope) {
    val animate: StateFlow<Boolean> = setting.changes
        .onStart { emit(Unit) }
        .map { setting.animates() }
        .stateIn(scope, SharingStarted.Eagerly, initialValue = setting.animates())
}

/**
 * The setting of the animations of Android, for [context]'s application: the duration scale of the animators
 * (`ANIMATOR_DURATION_SCALE`), observed; from API 33, the scale of the process itself, which Android also sets
 * (`ValueAnimator.registerDurationScaleChangeListener`), as it may change after the setting. No JVM test can change a
 * setting of Android: [SystemAnimations] is tested on a fake one.
 */
internal class AndroidAnimationSetting(context: Context) : AnimationSetting {
    private val resolver = context.applicationContext.contentResolver

    override val changes: Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val setting = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        resolver.registerContentObserver(setting, false, observer)
        val scaleListener =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listenToScale { trySend(Unit) } else null
        awaitClose {
            resolver.unregisterContentObserver(observer)
            if (scaleListener != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ValueAnimator.unregisterDurationScaleChangeListener(scaleListener)
            }
        }
    }

    override fun animates(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        // Before API 33, the scale of the process may lag behind the setting just changed: the setting is the truth.
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun listenToScale(onChange: () -> Unit): ValueAnimator.DurationScaleChangeListener {
        val listener = ValueAnimator.DurationScaleChangeListener { onChange() }
        ValueAnimator.registerDurationScaleChangeListener(listener)
        return listener
    }
}
