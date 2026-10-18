package ch.magori.blox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.navigation.BloxNavDisplay

class MainActivity : ComponentActivity() {
    private val container get() = (application as BloxApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BloxTheme {
                BloxNavDisplay(
                    homeViewModelFactory = container.homeViewModelFactory,
                    gameViewModelFactory = container.gameViewModelFactory,
                )
            }
        }
    }
}
