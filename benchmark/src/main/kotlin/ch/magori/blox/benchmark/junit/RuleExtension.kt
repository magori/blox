package ch.magori.blox.benchmark.junit

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.AssumptionViolatedException
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.opentest4j.TestAbortedException
import java.lang.reflect.Method

/**
 * Exécute une règle JUnit 4 autour de chaque méthode `@Test` de Jupiter (ADR 0009).
 *
 * Macrobenchmark n'offre ses points d'entrée publics que sous forme de règles JUnit 4
 * (`MacrobenchmarkRule`, `BaselineProfileRule`) : la règle reçoit la description du test en cours
 * (classe et méthode, dont elle tire le nom des résultats).
 *
 * C'est l'argument d'instrumentation `androidx.benchmark.enabledRules` qui décide quelles règles
 * s'exécutent ; le plugin `androidx.baselineprofile` le pose à `macrobenchmark` pour les tests de
 * `benchmarkRelease` et à `baselineprofile` pour ceux de `nonMinifiedRelease`. La règle écartée
 * lèverait l'`AssumptionViolatedException` de JUnit 4, que le lanceur JUnit 5 d'Android rapporte
 * comme un échec, même convertie en test interrompu : l'extension désactive donc elle-même le test
 * d'avance ([ExecutionCondition]), comme un `@Disabled`. La conversion en [TestAbortedException],
 * cause gardée, ne reste que pour une hypothèse levée pendant la mesure (Macrobenchmark lève la
 * classe publique `org.junit.AssumptionViolatedException` ; sa mère `org.junit.internal…` est
 * dépréciée, et la nommer serait un avertissement, donc une erreur).
 *
 * Limites :
 * - seule la méthode de test est enveloppée par la règle, pas les `@BeforeEach` ni les
 *   `@AfterEach` (une règle JUnit 4 les enveloppe) ;
 * - les tests paramétrés (`@ParameterizedTest`) et les `@TestFactory` ne sont pas pris en charge :
 *   ils ne passent pas par `interceptTestMethod`, la règle ne s'y appliquerait pas.
 *
 * JUnit 4 n'est pas déclaré : il arrive par `benchmark-macro-junit4`, comme dans le lanceur
 * d'instrumentation d'AndroidX. Seuls ces types (`TestRule`, `Description`, `Statement`,
 * `AssumptionViolatedException`) servent ici.
 *
 * @param ruleType la valeur de `androidx.benchmark.enabledRules` qui active la règle.
 */
abstract class RuleExtension(private val rule: TestRule, private val ruleType: String) :
    InvocationInterceptor,
    ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val enabledRules = InstrumentationRegistry.getArguments().getString(ENABLED_RULES_ARGUMENT)
            ?.split(',')
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        return if (enabledRules.isEmpty() || ruleType in enabledRules) {
            ConditionEvaluationResult.enabled("$ENABLED_RULES_ARGUMENT permet $ruleType")
        } else {
            ConditionEvaluationResult.disabled("$ENABLED_RULES_ARGUMENT=${enabledRules.joinToString(",")}")
        }
    }

    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        val description = Description.createTestDescription(
            extensionContext.requiredTestClass,
            extensionContext.requiredTestMethod.name,
        )
        val test = object : Statement() {
            override fun evaluate() {
                invocation.proceed()
            }
        }
        try {
            rule.apply(test, description).evaluate()
        } catch (skipped: AssumptionViolatedException) {
            throw TestAbortedException(skipped.message, skipped)
        }
    }

    private companion object {
        const val ENABLED_RULES_ARGUMENT = "androidx.benchmark.enabledRules"
    }
}
