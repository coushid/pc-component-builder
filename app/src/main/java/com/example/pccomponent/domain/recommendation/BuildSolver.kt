package com.example.pccomponent.domain.recommendation

import com.example.pccomponent.data.model.Component
import com.example.pccomponent.data.model.ComponentCategory
import com.example.pccomponent.data.model.PcConfiguration
import com.example.pccomponent.domain.compatibility.CompatibilityChecker
import com.example.pccomponent.domain.questionnaire.PartialBuildRequirements
import com.example.pccomponent.performance.PerformanceTrace
import java.util.PriorityQueue

internal data class SolverResult(
    val components: List<Component> = emptyList(),
    val appliedPerformanceLevel: Int? = null,
    val requestedPerformanceLevel: Int? = null
)

/** Immutable, pre-sorted catalog index for one solver invocation. */
internal class SolverCatalogIndex(components: List<Component>) {
    val byCategory: Map<ComponentCategory, List<Component>> = components.groupBy(Component::category)
    val cheapestByCategory: Map<ComponentCategory, List<Component>> =
        ComponentCategory.entries.associateWith { category ->
            byCategory[category].orEmpty().sortedBy(Component::price)
        }
    val pricePriorityByCategory: Map<ComponentCategory, List<Component>> =
        ComponentCategory.entries.associateWith { category ->
            byCategory[category].orEmpty().sortedWith(pricePriorityComparator)
        }
    val bestByCategory: Map<ComponentCategory, List<Component>> =
        ComponentCategory.entries.associateWith { category ->
            byCategory[category].orEmpty().sortedWith(performanceComparator)
        }

    private companion object {
        val pricePriorityComparator = compareBy<Component> { it.price }.thenByDescending { it.performanceLevel }
        val performanceComparator = compareByDescending<Component> { it.performanceLevel }.thenBy { it.price }
    }
}

/** Deterministic, shared complete-build search used by both recommendation and feasibility. */
internal class BuildSolver {
    fun solve(
        components: List<Component>,
        requirements: PartialBuildRequirements,
        budget: Int,
        priority: String,
        exhaustive: Boolean = true
    ): SolverResult = solve(SolverCatalogIndex(components), requirements, budget, priority, exhaustive)

    internal fun solve(
        index: SolverCatalogIndex,
        requirements: PartialBuildRequirements,
        budget: Int,
        priority: String,
        exhaustive: Boolean = true
    ): SolverResult = PerformanceTrace.measure("BuildSolver.solve") {
        val requested = requirements.minPerformanceLevel ?: MIN_PERFORMANCE_LEVEL
        for (level in requested downTo MIN_PERFORMANCE_LEVEL) {
            val selected = findBest(index, requirements, budget, priority, level, exhaustive)
            if (selected.isNotEmpty()) return SolverResult(selected, level, requested)
        }
        return SolverResult(requestedPerformanceLevel = requested)
    }

    private fun findBest(
        index: SolverCatalogIndex,
        requirements: PartialBuildRequirements,
        budget: Int,
        priority: String,
        performanceLevel: Int,
        exhaustive: Boolean
    ): List<Component> {
        val candidateLimit = if (exhaustive) MAX_CANDIDATES_PER_CATEGORY else FAST_CANDIDATES_PER_CATEGORY
        val cheapestLimit = if (exhaustive) MAX_CHEAPEST_CANDIDATES_PER_CATEGORY else FAST_CHEAPEST_CANDIDATES_PER_CATEGORY
        val beamLimit = if (exhaustive) MAX_BEAM_WIDTH else FAST_BEAM_WIDTH
        val byCategory = buildCandidates(index, requirements, performanceLevel, priority, candidateLimit, cheapestLimit)
        if (ComponentCategory.entries.any { byCategory[it].isNullOrEmpty() }) return emptyList()

        val minimumPrice = ComponentCategory.entries.sumOf { category ->
            byCategory.getValue(category).minOf(Component::price)
        }
        if (minimumPrice > budget) return emptyList()

        if (!exhaustive) return findAnyCompatible(byCategory, budget)

        var beam = listOf(PartialBuild.empty())
        for (category in SEARCH_ORDER) {
            beam = selectTopPartials(
                beam = beam,
                candidates = byCategory.getValue(category),
                budget = budget,
                limit = beamLimit
            )
            if (beam.isEmpty()) return emptyList()
        }
        return beam.asSequence()
            .filter { isCompleteCompatible(it.components, budget) }
            .minWithOrNull(buildComparator(priority, budget))?.components.orEmpty()
    }

    /**
     * Keep only the best beamLimit states while generating the next layer.
     * Sorting the complete Cartesian product can retain hundreds of thousands
     * of PartialBuild objects and exhaust the Android heap before take() runs.
     */
    private fun selectTopPartials(
        beam: List<PartialBuild>,
        candidates: List<Component>,
        budget: Int,
        limit: Int
    ): List<PartialBuild> {
        val comparator = partialComparator()
        val top = PriorityQueue<PartialBuild>(limit, comparator.reversed())
        for (partial in beam) {
            for (candidate in candidates) {
                val next = partial.append(candidate)
                if (next.totalPrice > budget || !isCompatiblePartial(next)) continue
                if (top.size < limit) {
                    top.add(next)
                } else if (comparator.compare(next, top.peek()) < 0) {
                    top.poll()
                    top.add(next)
                }
            }
        }
        return top.toList().sortedWith(comparator)
    }

    private fun buildCandidates(
        index: SolverCatalogIndex,
        requirements: PartialBuildRequirements,
        performanceLevel: Int,
        priority: String,
        candidateLimit: Int,
        cheapestLimit: Int
    ): Map<ComponentCategory, List<Component>> {
        val preferredLists = if (priority.equals("price", true)) {
            index.pricePriorityByCategory
        } else {
            index.bestByCategory
        }
        return ComponentCategory.entries.associateWith { category ->
            val preferred = preferredLists.getValue(category).asSequence()
                .filter { it.performanceLevel >= performanceLevel && matchesRequirements(it, requirements) }
                .take(candidateLimit).toList()
            val cheapest = index.cheapestByCategory.getValue(category).asSequence()
                .filter { it.performanceLevel >= performanceLevel && matchesRequirements(it, requirements) }
                .take(cheapestLimit).toList()
            (preferred + cheapest).distinctBy(Component::id)
        }
    }

    private fun matchesRequirements(component: Component, requirements: PartialBuildRequirements): Boolean = when (component.category) {
        ComponentCategory.RAM ->
            (requirements.requiredMemoryGb == null || (component.specifications.memoryCapacityGb ?: 0) >= requirements.requiredMemoryGb) &&
                (requirements.preferredMemoryType == null || requirements.preferredMemoryType == "none" ||
                    component.specifications.memoryType.equals(requirements.preferredMemoryType, true))
        ComponentCategory.STORAGE -> requirements.requiredStorageGb == null ||
            (component.specifications.capacityGb ?: 0) >= requirements.requiredStorageGb
        else -> true
    }

    private fun findAnyCompatible(byCategory: Map<ComponentCategory, List<Component>>, budget: Int): List<Component> {
        fun visit(index: Int, selected: PartialBuild): List<Component> {
            if (index == SEARCH_ORDER.size) return selected.components
            val category = SEARCH_ORDER[index]
            for (candidate in byCategory.getValue(category)) {
                val next = selected.append(candidate)
                if (next.totalPrice <= budget && isCompatiblePartial(next)) {
                    val result = visit(index + 1, next)
                    if (result.isNotEmpty()) return result
                }
            }
            return emptyList()
        }
        return visit(0, PartialBuild.empty())
    }

    private fun isCompatiblePartial(build: PartialBuild): Boolean {
        val byCategory = build.components.associateBy { it.category }
        val cpu = byCategory[ComponentCategory.CPU]
        val motherboard = byCategory[ComponentCategory.MOTHERBOARD]
        val ram = byCategory[ComponentCategory.RAM]
        val gpu = byCategory[ComponentCategory.GPU]
        val pcCase = byCategory[ComponentCategory.PC_CASE]
        val cooler = byCategory[ComponentCategory.COOLER]
        val psu = byCategory[ComponentCategory.POWER_SUPPLY]
        if (cpu != null && motherboard != null && !equal(cpu.specifications.socket, motherboard.specifications.socket)) return false
        if (motherboard != null && ram != null && !equal(motherboard.specifications.memoryType, ram.specifications.memoryType)) return false
        if (motherboard != null && ram != null && motherboard.specifications.maxMemoryCapacityGb != null &&
            (ram.specifications.memoryCapacityGb ?: 0) > motherboard.specifications.maxMemoryCapacityGb) return false
        if (motherboard != null && pcCase != null && !formFactorFits(motherboard.specifications.formFactor, pcCase.specifications.formFactor)) return false
        if (gpu != null && pcCase != null && !fits(gpu.specifications.lengthMm, pcCase.specifications.maxGpuLengthMm)) return false
        if (cpu != null && cooler != null) {
            val socket = cpu.specifications.socket ?: return false
            if (socket !in cooler.specifications.supportedSockets || (cooler.specifications.maxTdpWatts ?: 0) < cpu.powerConsumptionW) return false
        }
        if (pcCase != null && cooler != null && (cooler.specifications.heightMm ?: 0) > (pcCase.specifications.maxCoolerHeightMm ?: 0)) return false
        if (gpu != null && psu != null && !CompatibilityChecker.hasPowerConnector(psu.specifications.gpuPowerConnectors, gpu.specifications.requiredGpuPowerConnectors)) return false
        return true
    }

    private fun isCompleteCompatible(components: List<Component>, budget: Int): Boolean =
        components.size == ComponentCategory.entries.size &&
            components.map { it.category }.toSet().size == ComponentCategory.entries.size &&
            components.sumOf(Component::price) <= budget &&
            CompatibilityChecker.check(PcConfiguration(components, "Проверка", budget)).incompatibilityMessages.isEmpty()

    private fun equal(a: String?, b: String?) = a != null && b != null && a.equals(b, true)
    private fun fits(value: Int?, max: Int?) = value != null && max != null && value <= max
    private fun formFactorFits(board: String?, caseFormat: String?): Boolean {
        val sizes = mapOf("ITX" to 1, "MINI-ITX" to 1, "MATX" to 2, "M-ATX" to 2, "ATX" to 3, "E-ATX" to 4)
        val b = sizes[board?.uppercase()] ?: return false
        val c = sizes[caseFormat?.uppercase()] ?: return false
        return b <= c
    }

    private fun partialComparator() = compareBy<PartialBuild> { it.totalPrice }.thenByDescending { it.performanceScore }
    private fun buildComparator(priority: String, budget: Int): Comparator<PartialBuild> = when (priority.lowercase()) {
        "price" -> compareBy<PartialBuild> { it.totalPrice }.thenByDescending { it.performanceScore }
        "upgrade" -> compareByDescending<PartialBuild> { it.performanceScore }.thenBy { budget - it.totalPrice }
        else -> compareByDescending<PartialBuild> { it.performanceScore }.thenBy { it.totalPrice }
    }

    private data class PartialBuild(val components: List<Component>, val totalPrice: Int, val performanceScore: Int) {
        fun append(component: Component) = PartialBuild(components + component, totalPrice + component.price, performanceScore + component.performanceLevel)
        companion object { fun empty() = PartialBuild(emptyList(), 0, 0) }
    }

    private companion object {
        const val MIN_PERFORMANCE_LEVEL = 1
        const val MAX_CANDIDATES_PER_CATEGORY = 24
        const val MAX_CHEAPEST_CANDIDATES_PER_CATEGORY = 24
        const val MAX_BEAM_WIDTH = 20_000
        const val FAST_CANDIDATES_PER_CATEGORY = 16
        const val FAST_CHEAPEST_CANDIDATES_PER_CATEGORY = 16
        const val FAST_BEAM_WIDTH = 2_500
        val SEARCH_ORDER = listOf(
            ComponentCategory.CPU, ComponentCategory.MOTHERBOARD, ComponentCategory.RAM,
            ComponentCategory.STORAGE, ComponentCategory.GPU, ComponentCategory.COOLER,
            ComponentCategory.PC_CASE, ComponentCategory.POWER_SUPPLY
        )
    }
}
