package ch.magori.blox.benchmark.junit

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule

/**
 * `MacrobenchmarkRule` pour Jupiter : à déclarer avec `@RegisterExtension` (champ `@JvmField`).
 * Ne s'exécute que si `androidx.benchmark.enabledRules` est absent ou contient `macrobenchmark` :
 * le plugin `androidx.baselineprofile` le pose pour les tests de `benchmarkRelease`.
 */
class MacrobenchmarkExtension private constructor(private val rule: MacrobenchmarkRule) :
    RuleExtension(rule, ruleType = "macrobenchmark") {
    constructor() : this(MacrobenchmarkRule())

    /** Voir `MacrobenchmarkRule.measureRepeated`. */
    fun measureRepeated(
        packageName: String,
        metrics: List<Metric>,
        compilationMode: CompilationMode,
        iterations: Int,
        startupMode: StartupMode? = null,
        setupBlock: MacrobenchmarkScope.() -> Unit = {},
        measureBlock: MacrobenchmarkScope.() -> Unit,
    ) {
        rule.measureRepeated(
            packageName = packageName,
            metrics = metrics,
            compilationMode = compilationMode,
            startupMode = startupMode,
            iterations = iterations,
            setupBlock = setupBlock,
            measureBlock = measureBlock,
        )
    }
}
