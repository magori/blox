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
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.navigation.BloxNavDisplay
import ch.magori.blox.theme.BloxSystemBars
import ch.magori.blox.theme.hasDarkSystemBarIcons
import ch.magori.blox.theme.showSystemBars

class MainActivity : ComponentActivity() {
    private val container get() = (application as BloxApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Until the theme is read, the window shows its background, the page of Original (themes.xml); then the bars
        // follow the variant drawn (BloxSystemBars), not the mode of the system.
        showSystemBars(darkIcons = hasDarkSystemBarIcons(BloxThemeVariant.ORIGINAL))
        holdFirstFrameUntilThemeIsRead()
        setContent {
            // The theme chosen on the home screen and the choice of effects colour and move every screen. They are
            // known at once, together, once the settings are read; before, nothing is composed, and the first frame
            // waits (see below). Android removing its animations imposes the reduced effects, as soon as it does.
            val appearance by container.appTheme.appearance.collectAsStateWithLifecycle()
            val systemAnimates by container.systemAnimations.animate.collectAsStateWithLifecycle()
            // The stack belongs here, above the theme: the bars follow the screen on top, as the Locksmith imposes its
            // theme. The mode of the system is read once, for the theme and for the bars.
            val backStack = rememberNavBackStack(CartridgesRoute)
            val darkSystem = isSystemInDarkTheme()
            appearance?.let { (kind, reducedEffects) ->
                BloxTheme(
                    kind = kind,
                    darkSystem = darkSystem,
                    effects = BloxEffects.resolve(reducedEffects, systemAnimates),
                ) {
                    BloxSystemBars(top = { backStack.lastOrNull() }, darkSystem = darkSystem)
                    BloxNavDisplay(
                        homeViewModelFactory = container.homeViewModelFactory,
                        gameViewModelFactory = container.gameViewModelFactory,
                        composerViewModelFactory = container.composerViewModelFactory,
                        myMusicsViewModelFactory = container.myMusicsViewModelFactory,
                        settingsViewModelFactory = container.settingsViewModelFactory,
                        deletedMusics = container.deletedMusics,
                        locksmithViewModelFactory = container.locksmithViewModelFactory,
                        dailyViewModelFactory = container.dailyViewModelFactory,
                        onCopyDailyScore = container.dailyScoreCopier::copy,
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
     * On a cold start the settings take a moment to read: the first frame is held back until the theme and the choice
     * of effects are known, so that neither a default theme, the full effects of a player who reduced them, nor an
     * empty window flashes. No JVM test can draw a frame.
     */
    private fun holdFirstFrameUntilThemeIsRead() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (container.appTheme.appearance.value == null) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )
    }
}
