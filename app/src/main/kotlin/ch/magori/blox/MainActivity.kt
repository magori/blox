package ch.magori.blox

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.navigation.BloxNavDisplay
import ch.magori.blox.theme.BloxSystemBars
import ch.magori.blox.theme.hasDarkSystemBarIcons
import ch.magori.blox.theme.showSystemBars
import ch.magori.blox.theme.systemBarVariant

class MainActivity : ComponentActivity() {
    private val container get() = (application as BloxApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Until the theme is read, the window shows its background, the page of Original (themes.xml); then the bars
        // follow the variant drawn (BloxSystemBars), not the mode of the system.
        showSystemBars(darkIcons = hasDarkSystemBarIcons(BloxThemeVariant.ORIGINAL))
        holdFirstFrameUntilThemeIsRead()
        setContent {
            // The theme chosen on the home screen colours every screen. It is known at once once the
            // settings are read; before, nothing is composed, and the first frame waits (see below).
            val themeKind by container.appTheme.kind.collectAsStateWithLifecycle()
            // The stack belongs here, above the theme: the bars follow the screen on top (the Locksmith imposes its theme).
            val backStack = rememberNavBackStack(CartridgesRoute)
            themeKind?.let { kind ->
                BloxTheme(kind = kind) {
                    BloxSystemBars(systemBarVariant(backStack.lastOrNull(), BloxTheme.variant, isSystemInDarkTheme()))
                    BloxNavDisplay(
                        homeViewModelFactory = container.homeViewModelFactory,
                        gameViewModelFactory = container.gameViewModelFactory,
                        composerViewModelFactory = container.composerViewModelFactory,
                        myMusicsViewModelFactory = container.myMusicsViewModelFactory,
                        settingsViewModelFactory = container.settingsViewModelFactory,
                        deletedMusics = container.deletedMusics,
                        locksmithViewModelFactory = container.locksmithViewModelFactory,
                        backStack = backStack,
                        // Test tags become resource ids for UiAutomator: the measured journey of `:benchmark`
                        // finds the play button, the keys and the pause by them, whatever the language.
                        modifier = Modifier.semantics { testTagsAsResourceId = true },
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
