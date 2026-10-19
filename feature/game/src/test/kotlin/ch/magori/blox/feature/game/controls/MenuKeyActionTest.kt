package ch.magori.blox.feature.game.controls

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

internal class MenuKeyActionTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("pauseKeys")
    fun `in pause, Start, Escape and P resume the game as they go down`(
        name: String,
        keyCode: Long,
    ) {
        // When
        val action = menuKeyAction(Menu.PAUSE, Key(keyCode), KeyEventType.KeyDown, repeatCount = 0, menuFocused = false)

        // Then
        assertThat(action).isEqualTo(MenuKeyAction.Resume)
    }

    @Test
    fun `in pause, a resume key coming back up or repeated is kept from the system and does nothing`() {
        // When
        val up = menuKeyAction(Menu.PAUSE, Key.ButtonStart, KeyEventType.KeyUp, repeatCount = 0, menuFocused = false)
        val repeat =
            menuKeyAction(Menu.PAUSE, Key.ButtonStart, KeyEventType.KeyDown, repeatCount = 1, menuFocused = false)

        // Then
        assertThat(up).isEqualTo(MenuKeyAction.Ignored)
        assertThat(repeat).isEqualTo(MenuKeyAction.Ignored)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pauseScreenKeys")
    fun `in pause, any other key is left to the system, to move between the buttons and press them`(
        name: String,
        keyCode: Long,
    ) {
        // When
        val down = menuKeyAction(Menu.PAUSE, Key(keyCode), KeyEventType.KeyDown, repeatCount = 0, menuFocused = false)
        val up = menuKeyAction(Menu.PAUSE, Key(keyCode), KeyEventType.KeyUp, repeatCount = 0, menuFocused = false)

        // Then
        assertThat(down).isNull()
        assertThat(up).isNull()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("hammeredKeys")
    fun `once the game is over, B, Escape and the other keys of the game are kept from the system and do nothing`(
        name: String,
        keyCode: Long,
    ) {
        // When the player is still hammering the keys of the game as the game over screen shows
        val down =
            menuKeyAction(Menu.GAME_OVER, Key(keyCode), KeyEventType.KeyDown, repeatCount = 0, menuFocused = false)
        val up = menuKeyAction(Menu.GAME_OVER, Key(keyCode), KeyEventType.KeyUp, repeatCount = 0, menuFocused = false)

        // Then neither leaves the screen nor presses a button
        assertThat(down).isEqualTo(MenuKeyAction.Ignored)
        assertThat(up).isEqualTo(MenuKeyAction.Ignored)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("gameOverScreenKeys")
    fun `once the game is over, the arrows, the A button of a pad and the keys of the menus are left to the system`(
        name: String,
        keyCode: Long,
    ) {
        // When
        val down =
            menuKeyAction(Menu.GAME_OVER, Key(keyCode), KeyEventType.KeyDown, repeatCount = 0, menuFocused = false)
        val up = menuKeyAction(Menu.GAME_OVER, Key(keyCode), KeyEventType.KeyUp, repeatCount = 0, menuFocused = false)

        // Then
        assertThat(down).isNull()
        assertThat(up).isNull()
    }

    @Test
    fun `a key of a menu asks the resume once and is kept from the system, another key is left to it`() {
        // Given
        var resumes = 0
        val inPause = { key: Key, type: KeyEventType ->
            menuKey(Menu.PAUSE, key, type, repeatCount = 0, menuFocused = false, onResume = { resumes++ }) {}
        }

        // When Start goes down then up, then Tab goes down, in pause
        val startDown = inPause(Key.ButtonStart, KeyEventType.KeyDown)
        val startUp = inPause(Key.ButtonStart, KeyEventType.KeyUp)
        val tab = inPause(Key.Tab, KeyEventType.KeyDown)

        // Then
        assertThat(resumes).isEqualTo(1)
        assertThat(listOf(startDown, startUp)).containsOnly(true)
        assertThat(tab).isFalse()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("menus")
    fun `while the menu itself holds the focus, an arrow going down moves the focus into its buttons`(menu: Menu) {
        // When the menu has just shown, its buttons without the focus
        val down = menuKeyAction(menu, Key.DirectionDown, KeyEventType.KeyDown, repeatCount = 0, menuFocused = true)
        val up = menuKeyAction(menu, Key.DirectionDown, KeyEventType.KeyUp, repeatCount = 0, menuFocused = true)

        // Then the first arrow enters the buttons, as it would with nothing focused
        assertThat(down).isEqualTo(MenuKeyAction.EnterButtons)
        assertThat(up).isNull()
    }

    @Test
    fun `an arrow on a menu holding the focus itself moves the focus into the buttons, and is kept from the system`() {
        // Given
        var entries = 0

        // When
        val kept = menuKey(
            Menu.PAUSE,
            Key.DirectionDown,
            KeyEventType.KeyDown,
            repeatCount = 0,
            menuFocused = true,
            onResume = {},
        ) { entries++ }

        // Then
        assertThat(kept).isTrue()
        assertThat(entries).isEqualTo(1)
    }

    // The key codes, not the keys: a parameter of a value class would be given to JUnit unboxed.
    companion object {
        @JvmStatic
        fun menus(): List<Menu> = Menu.entries

        @JvmStatic
        fun hammeredKeys(): List<Arguments> = listOf(
            arguments("B of a pad, which would go back", Key.ButtonB.keyCode),
            arguments("Escape", Key.Escape.keyCode),
            arguments("Start of a pad, which would press a button", Key.ButtonStart.keyCode),
            arguments("X of a pad", Key.ButtonX.keyCode),
            arguments("P", Key.P.keyCode),
            arguments("Z", Key.Z.keyCode),
            arguments("X", Key.X.keyCode),
            arguments("S", Key.S.keyCode),
            arguments("left Ctrl", Key.CtrlLeft.keyCode),
        )

        @JvmStatic
        fun gameOverScreenKeys(): List<Arguments> = listOf(
            arguments("◀ (arrow or cross)", Key.DirectionLeft.keyCode),
            arguments("▶ (arrow or cross)", Key.DirectionRight.keyCode),
            arguments("▲ (arrow or cross)", Key.DirectionUp.keyCode),
            arguments("▼ (arrow or cross)", Key.DirectionDown.keyCode),
            arguments("A of a pad, which presses a button", Key.ButtonA.keyCode),
            arguments("Tab", Key.Tab.keyCode),
            arguments("Enter", Key.Enter.keyCode),
            arguments("the system back", Key.Back.keyCode),
        )

        @JvmStatic
        fun pauseScreenKeys(): List<Arguments> = listOf(
            arguments("▼ (arrow or cross)", Key.DirectionDown.keyCode),
            arguments("Tab", Key.Tab.keyCode),
            arguments("Enter", Key.Enter.keyCode),
            arguments("A of a pad, which presses a button", Key.ButtonA.keyCode),
            arguments("B of a pad, which goes back", Key.ButtonB.keyCode),
            arguments("the system back", Key.Back.keyCode),
        )

        @JvmStatic
        fun pauseKeys(): List<Arguments> = listOf(
            arguments("Start of a pad", Key.ButtonStart.keyCode),
            arguments("Escape", Key.Escape.keyCode),
            arguments("P", Key.P.keyCode),
        )
    }
}
