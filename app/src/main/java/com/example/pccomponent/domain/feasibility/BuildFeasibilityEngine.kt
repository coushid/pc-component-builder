package com.example.pccomponent.domain.feasibility

import com.example.pccomponent.data.model.Component
import com.example.pccomponent.domain.questionnaire.PartialBuildRequirements
import com.example.pccomponent.domain.recommendation.BuildRecommendationEngine
import com.example.pccomponent.domain.recommendation.RecommendationStatus
import com.example.pccomponent.domain.recommendation.SolverCatalogIndex
import com.example.pccomponent.performance.PerformanceTrace
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

data class FeasibilityResult(
    val isAvailable: Boolean,
    val minimumRequiredBudget: Int? = null,
    val components: List<Component> = emptyList(),
    val reasons: List<String> = emptyList()
)

/** Single entry point for dynamic questionnaire answer availability. */
class BuildFeasibilityEngine(private val catalog: List<Component>) {
    private val recommendation = BuildRecommendationEngine()
    private val catalogIndex = SolverCatalogIndex(catalog)
    private val parallelism = Semaphore(MAX_PARALLEL_CHECKS)
    // Availability checks are launched in parallel for the answers on a
    // screen.  A concurrent cache prevents duplicate work without races.
    private val cache = ConcurrentHashMap<CacheKey, FeasibilityResult>()

    fun evaluate(requirements: PartialBuildRequirements, budget: Int? = requirements.budget): FeasibilityResult {
        return evaluateCached(requirements, budget, null, null)
    }

    private fun evaluateCached(
        requirements: PartialBuildRequirements,
        budget: Int?,
        questionId: String?,
        answerId: String?
    ): FeasibilityResult {
        val key = CacheKey(questionId, answerId, requirements, budget)
        return cache.computeIfAbsent(key) {
            PerformanceTrace.measure("BuildFeasibilityEngine.evaluate") {
            val effectiveBudget = budget ?: Int.MAX_VALUE
            val result = recommendation.recommendForFeasibility(catalogIndex, requirements, effectiveBudget)
            when (result.status) {
                RecommendationStatus.SUCCESS -> FeasibilityResult(true, components = result.components)
                RecommendationStatus.BUDGET_TOO_LOW -> FeasibilityResult(
                    false, result.minimumRequiredBudget,
                    reasons = listOf("Полная совместимая сборка начинается от ${result.minimumRequiredBudget} руб.")
                )
                RecommendationStatus.NO_COMPATIBLE_BUILD -> FeasibilityResult(
                    false, reasons = listOf("В каталоге нет полной совместимой сборки для текущих требований.")
                )
            }
        }
        }
    }

    fun hasCompatibleBuild(requirements: PartialBuildRequirements): Boolean =
        evaluate(requirements, Int.MAX_VALUE).isAvailable

    fun findMinimumRequiredBudget(requirements: PartialBuildRequirements): Int? =
        evaluate(requirements, Int.MAX_VALUE).components.sumOfOrNull { it.price }

    internal suspend fun evaluateLimited(requirements: PartialBuildRequirements): FeasibilityResult =
        parallelism.withPermit { evaluateCached(requirements, requirements.budget, null, null) }

    internal fun evaluateAnswer(
        questionId: String,
        answerId: String,
        requirements: PartialBuildRequirements,
        budget: Int?
    ): FeasibilityResult = evaluateCached(requirements, budget, questionId, answerId)

    private companion object {
        const val MAX_PARALLEL_CHECKS = 3
    }
}

private data class CacheKey(
    val questionId: String?,
    val answerId: String?,
    val requirements: PartialBuildRequirements,
    val budget: Int?
)

private inline fun <T> Iterable<T>.sumOfOrNull(selector: (T) -> Int): Int? =
    if (none()) null else sumOf(selector)

fun BuildFeasibilityEngine.answersFor(
    question: com.example.pccomponent.domain.questionnaire.QuestionnaireQuestion,
    current: PartialBuildRequirements
): List<com.example.pccomponent.domain.questionnaire.DynamicQuestionnaireAnswer> = question.answers.map { answer ->
    /*
     * The first screen only asks for the knowledge level.  There are no
     * constraints yet, so every option is necessarily available.  Avoid
     * launching a full eight-component search once per option while the first
     * Compose frame is being rendered.
     */
    if (question.id == "knowledgeLevel") {
        return@map com.example.pccomponent.domain.questionnaire.DynamicQuestionnaireAnswer(
            id = answer.id,
            text = answer.text,
            value = answer.value,
            requirements = answer.requirements,
            availability = com.example.pccomponent.domain.questionnaire.AnswerAvailability.AVAILABLE
        )
    }
    val candidate = current.apply(answer.requirements)
    val result = evaluate(candidate, candidate.budget)
    val available = result.isAvailable
    com.example.pccomponent.domain.questionnaire.DynamicQuestionnaireAnswer(
        id = answer.id,
        text = answer.text,
        value = answer.value,
        requirements = answer.requirements,
        availability = if (available) com.example.pccomponent.domain.questionnaire.AnswerAvailability.AVAILABLE
        else com.example.pccomponent.domain.questionnaire.AnswerAvailability.BLOCKED,
        reason = result.reasons.firstOrNull(),
        minimumRequiredBudget = result.minimumRequiredBudget
    )
}

/**
 * Calculates all choices independently.  The old implementation evaluated
 * them one after another, so one expensive solver pass per answer made the
 * questionnaire appear frozen for several seconds.
 */
suspend fun BuildFeasibilityEngine.answersForParallel(
    question: com.example.pccomponent.domain.questionnaire.QuestionnaireQuestion,
    current: PartialBuildRequirements
): List<com.example.pccomponent.domain.questionnaire.DynamicQuestionnaireAnswer> {
    if (question.id == "knowledgeLevel") {
        return question.answers.map { answer ->
            com.example.pccomponent.domain.questionnaire.DynamicQuestionnaireAnswer(
                id = answer.id,
                text = answer.text,
                value = answer.value,
                requirements = answer.requirements,
                availability = com.example.pccomponent.domain.questionnaire.AnswerAvailability.AVAILABLE
            )
        }
    }
    return coroutineScope {
    question.answers.map { answer ->
        async {
            val candidate = current.apply(answer.requirements)
            val result = evaluateAnswer(question.id, answer.id, candidate, candidate.budget)
            com.example.pccomponent.domain.questionnaire.DynamicQuestionnaireAnswer(
                id = answer.id,
                text = answer.text,
                value = answer.value,
                requirements = answer.requirements,
                availability = if (result.isAvailable)
                    com.example.pccomponent.domain.questionnaire.AnswerAvailability.AVAILABLE
                else com.example.pccomponent.domain.questionnaire.AnswerAvailability.BLOCKED,
                reason = result.reasons.firstOrNull(),
                minimumRequiredBudget = result.minimumRequiredBudget
            )
        }
    }.awaitAll()
    }
}

