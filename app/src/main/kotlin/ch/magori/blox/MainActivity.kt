package ch.magori.blox

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.navigation.BloxNavDisplay

class MainActivity : ComponentActivity() {
    private val container get() = (application as BloxApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        holdFirstFrameUntilThemeIsRead()
        setContent {
            // The theme chosen on the home screen colours every screen. It is known at once once the
            // settings are read; before, nothing is composed, and the first frame waits (see below).
            val themeKind by container.appTheme.kind.collectAsStateWithLifecycle()
            themeKind?.let { kind ->
                BloxTheme(kind = kind) {
                    BloxNavDisplay(
                        homeViewModelFactory = container.homeViewModelFactory,
                        gameViewModelFactory = container.gameViewModelFactory,
                    )
                }
            }
        }
    }

    /**
     * On a cold start the settings take a moment to read: the first frame is held back until the theme
     * is known, so that neither a default theme nor an empty window flashes. No JVM test can draw a frame.
     */
    private fun holdFirstFrameUntilThemeIsRead() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (container.appTheme.kind.value == null) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )
    }
}
