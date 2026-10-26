package ch.magori.blox.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/*
 * Le parcours mesuré : choix du jeu, cartouche Classique, accueil, Jouer, quelques secondes de
 * partie, pause. Les éléments se trouvent par leur tag de test, exposé comme identifiant de
 * ressource (`testTagsAsResourceId`, posé par `MainActivity`) : le parcours ne dépend pas de la
 * langue de l'appareil. Les tags sont ceux de `Cartridge` (`cartridge_classic`), `HomeScreen`
 * (`home_play`), `GameTopBar` (`game_pause`), `GameControls` (`controlTestTag`) et `PauseScreen`
 * (`pause_resume`).
 */

/**
 * L'identifiant de l'application mesurée, passé par le plugin `androidx.baselineprofile` :
 * `ch.magori.blox.perf` (types de build `nonMinifiedRelease` et `benchmarkRelease`).
 */
internal val targetPackageName: String
    get() = checkNotNull(InstrumentationRegistry.getArguments().getString(TARGET_PACKAGE_ARGUMENT)) {
        "Argument $TARGET_PACKAGE_ARGUMENT absent : lancer les mesures par Gradle (plugin androidx.baselineprofile)."
    }

/** Démarre l'application depuis l'écran d'accueil du téléphone et attend sa première image. */
internal fun MacrobenchmarkScope.launchFromHomeScreen() {
    pressHome()
    startActivityAndWait()
}

/**
 * Depuis le choix du jeu, ouvre la cartouche Classique, puis lance une partie de son accueil et attend l'écran de jeu.
 * « Jouer » est le dernier élément de l'accueil, qui défile : sur un petit écran, il est sous le bord, et l'accueil
 * défile d'abord jusqu'en bas.
 */
internal fun MacrobenchmarkScope.startGame() {
    device.clickWhenReady(By.res(CLASSIC_CARTRIDGE))
    device.scrollHomeToPlay()
    device.clickWhenReady(By.res(PLAY))
    device.awaitObject(By.res(PAUSE))
}

/**
 * Attend l'accueil, puis le fait défiler jusqu'en bas s'il défile : « Jouer » est alors visible. Un accueil qui tient
 * dans l'écran ne défile pas, et « Jouer » y est déjà visible.
 */
private fun UiDevice.scrollHomeToPlay() {
    val start = TimeSource.Monotonic.markNow()
    while (true) {
        val home = findObject(By.scrollable(true))
        if (home != null) {
            home.scrollUntil(Direction.DOWN, Until.scrollFinished(Direction.DOWN))
            return
        }
        if (hasObject(By.res(PLAY))) return
        check(start.elapsedNow() < UI_TIMEOUT) { "Accueil introuvable après $UI_TIMEOUT" }
        Thread.sleep(POLL_INTERVAL.inWholeMilliseconds)
    }
}

/**
 * Joue pendant [duration] : déplacements, rotations et descentes, une commande toutes les
 * [COMMAND_INTERVAL].
 */
internal fun MacrobenchmarkScope.play(duration: Duration) {
    val start = TimeSource.Monotonic.markNow()
    var index = 0
    while (start.elapsedNow() < duration) {
        device.clickWhenReady(By.res(COMMANDS[index % COMMANDS.size]))
        index++
        Thread.sleep(COMMAND_INTERVAL.inWholeMilliseconds)
    }
}

/** Met la partie en pause et attend l'écran de pause. */
internal fun MacrobenchmarkScope.pauseGame() {
    device.clickWhenReady(By.res(PAUSE))
    device.awaitObject(By.res(RESUME))
}

/**
 * Cherche l'élément puis clique dessus, et recommence tant que [UI_TIMEOUT] n'est pas écoulé :
 * Compose peut remplacer le nœud d'accessibilité entre la recherche et le clic
 * ([StaleObjectException]).
 */
internal fun UiDevice.clickWhenReady(selector: BySelector) {
    val start = TimeSource.Monotonic.markNow()
    while (true) {
        val remaining = UI_TIMEOUT - start.elapsedNow()
        check(remaining.isPositive()) { "Élément introuvable ou instable après $UI_TIMEOUT : $selector" }
        val element = wait(Until.findObject(selector), remaining.inWholeMilliseconds) ?: continue
        try {
            element.click()
            return
        } catch (_: StaleObjectException) {
            // Nœud remplacé entre la recherche et le clic : on le recherche.
        }
    }
}

private fun UiDevice.awaitObject(selector: BySelector) {
    check(wait(Until.hasObject(selector), UI_TIMEOUT.inWholeMilliseconds)) {
        "Élément introuvable après $UI_TIMEOUT : $selector"
    }
}

private const val TARGET_PACKAGE_ARGUMENT = "androidx.benchmark.targetPackageName"

private const val CLASSIC_CARTRIDGE = "cartridge_classic"
private const val PLAY = "home_play"
private const val PAUSE = "game_pause"
private const val RESUME = "pause_resume"

private val COMMANDS = listOf(
    "control_left",
    "control_a",
    "control_right",
    "control_right",
    "control_b",
    "control_down",
    "control_down",
    "control_down",
)

private val COMMAND_INTERVAL = 250.milliseconds
private val UI_TIMEOUT = 5.seconds

/** Entre deux recherches de l'accueil, le temps qu'il s'affiche. */
private val POLL_INTERVAL = 100.milliseconds
