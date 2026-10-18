package ch.magori.blox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.di.AppContainer
import ch.magori.blox.navigation.BloxNavDisplay

class MainActivity : ComponentActivity() {
    // Nothing in it keeps state yet; it moves to the Application when it does (P3.3).
    private val container = AppContainer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BloxTheme {
                BloxNavDisplay(gameViewModelFactory = container.gameViewModelFactory)
            }
        }
    }
}
