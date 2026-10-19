package ch.magori.blox.benchmark.junit

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule

/**
 * `BaselineProfileRule` pour Jupiter : à déclarer avec `@RegisterExtension` (champ `@JvmField`).
 * Ne s'exécute que si `androidx.benchmark.enabledRules` est absent ou contient `baselineprofile` :
 * le plugin `androidx.baselineprofile` le pose pour les tests de `nonMinifiedRelease`.
 */
class BaselineProfileExtension private constructor(private val rule: BaselineProfileRule) :
    RuleExtension(rule, ruleType = "baselineprofile") {
    constructor() : this(BaselineProfileRule())

    /** Voir `BaselineProfileRule.collect`. */
    fun collect(
        packageName: String,
        includeInStartupProfile: Boolean,
        profileBlock: MacrobenchmarkScope.() -> Unit,
    ) {
        rule.collect(
            packageName = packageName,
            includeInStartupProfile = includeInStartupProfile,
            profileBlock = profileBlock,
        )
    }
}
