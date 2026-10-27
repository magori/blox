package ch.magori.blox.daily

import ch.magori.blox.theme.appResource
import ch.magori.blox.theme.element

/**
 * The texts of the copied score as the application gives them, read in its `strings.xml` (a JVM test has no Android
 * resources) and formatted as Android formats them: the plural chosen by the French rule, « one » for 0 and 1, which
 * is all a number of tries needs.
 */
internal val appScoreTexts = DailyScoreTexts(
    score = { seed, firstTry, best, tries ->
        appText("string[@name='daily_score_text']").format(seed, firstTry, best, tries)
    },
    tries = { count, written ->
        val quantity = if (count <= 1) "one" else "other"
        appText("plurals[@name='daily_score_tries']/item[@quantity='$quantity']").format(written)
    },
)

private fun appText(expression: String): String =
    element(appResource("values/strings.xml"), "/resources/$expression").textContent
