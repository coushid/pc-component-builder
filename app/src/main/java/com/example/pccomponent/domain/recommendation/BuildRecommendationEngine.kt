package com.example.pccomponent.domain.recommendation

import com.example.pccomponent.data.model.Component
import com.example.pccomponent.domain.questionnaire.BuildRequirements
import com.example.pccomponent.domain.questionnaire.PartialBuildRequirements
import com.example.pccomponent.performance.PerformanceTrace

enum class RecommendationStatus { SUCCESS, BUDGET_TOO_LOW, NO_COMPATIBLE_BUILD }

data class BuildRecommendationResult(
    val status: RecommendationStatus,
    val components: List<Component> = emptyList(),
    val minimumRequiredBudget: Int? = null,
    val appliedPerformanceLevel: Int? = null,
    val requestedPerformanceLevel: Int? = null
)

class BuildRecommendationEngine {
    private val solver = BuildSolver()

    fun recommend(components: List<Component>, requirements: BuildRequirements): BuildRecommendationResult =
        PerformanceTrace.measure("BuildRecommendationEngine.recommend") {
            recommend(components, requirements.toPartial(), requirements.budget, requirements.priority)
        }

    internal fun recommend(
        components: List<Component>,
        requirements: PartialBuildRequirements,
        budget: Int,
        priority: String = requirements.priority ?: "balanced"
    ): BuildRecommendationResult {
        val result = solver.solve(components, requirements, budget, priority)
        if (result.components.isNotEmpty()) return BuildRecommendationResult(
            RecommendationStatus.SUCCESS, result.components, appliedPerformanceLevel = result.appliedPerformanceLevel,
            requestedPerformanceLevel = result.requestedPerformanceLevel
        )
        val unlimited = solver.solve(components, requirements, Int.MAX_VALUE, priority)
        return if (unlimited.components.isNotEmpty()) BuildRecommendationResult(
            RecommendationStatus.BUDGET_TOO_LOW,
            minimumRequiredBudget = unlimited.components.sumOf(Component::price),
            appliedPerformanceLevel = unlimited.appliedPerformanceLevel,
            requestedPerformanceLevel = unlimited.requestedPerformanceLevel
        ) else BuildRecommendationResult(RecommendationStatus.NO_COMPATIBLE_BUILD, requestedPerformanceLevel = requirements.minPerformanceLevel)
    }

    fun choose(components: List<Component>, requirements: BuildRequirements): List<Component> = recommend(components, requirements).components

    internal fun recommendForFeasibility(
        components: List<Component>,
        requirements: PartialBuildRequirements,
        budget: Int
    ): BuildRecommendationResult = recommendForFeasibility(
        SolverCatalogIndex(components), requirements, budget
    )

    internal fun recommendForFeasibility(
        index: SolverCatalogIndex,
        requirements: PartialBuildRequirements,
        budget: Int
    ): BuildRecommendationResult {
        val priority = requirements.priority ?: "balanced"
        val result = solver.solve(index, requirements, budget, priority, exhaustive = false)
        if (result.components.isNotEmpty()) {
            return BuildRecommendationResult(RecommendationStatus.SUCCESS, result.components,
                appliedPerformanceLevel = result.appliedPerformanceLevel,
                requestedPerformanceLevel = result.requestedPerformanceLevel)
        }
        val unlimited = solver.solve(index, requirements, Int.MAX_VALUE, priority, exhaustive = false)
        return if (unlimited.components.isNotEmpty()) BuildRecommendationResult(
            RecommendationStatus.BUDGET_TOO_LOW,
            minimumRequiredBudget = unlimited.components.sumOf(Component::price),
            appliedPerformanceLevel = unlimited.appliedPerformanceLevel,
            requestedPerformanceLevel = unlimited.requestedPerformanceLevel
        ) else BuildRecommendationResult(
            RecommendationStatus.NO_COMPATIBLE_BUILD,
            requestedPerformanceLevel = requirements.minPerformanceLevel
        )
    }

    fun choose(components: List<Component>, budget: Int, purpose: String, performanceLevel: Int, priority: String = "balanced"): List<Component> =
        solver.solve(components, PartialBuildRequirements(usage = purpose, minPerformanceLevel = performanceLevel), budget, priority).components

    private fun BuildRequirements.toPartial() = PartialBuildRequirements(
        knowledgeLevel, usage, budget, minPerformanceLevel, requiredMemoryGb, requiredStorageGb, preferredMemoryType, priority
    )
}
