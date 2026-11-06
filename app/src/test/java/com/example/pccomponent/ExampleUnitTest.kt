package com.example.pccomponent

import com.example.pccomponent.data.model.Component
import com.example.pccomponent.data.model.ComponentCategory
import com.example.pccomponent.data.model.PcConfiguration
import com.example.pccomponent.data.model.Specifications
import com.example.pccomponent.domain.compatibility.CompatibilityChecker
import com.example.pccomponent.domain.compatibility.CompatibilityStatus
import com.example.pccomponent.domain.questionnaire.QuestionnaireDefinition
import com.example.pccomponent.domain.questionnaire.QuestionnaireResolver
import com.example.pccomponent.domain.questionnaire.BuildRequirements
import com.example.pccomponent.domain.recommendation.BuildRecommendationEngine
import com.example.pccomponent.domain.recommendation.RecommendationStatus
import com.example.pccomponent.domain.feasibility.BuildFeasibilityEngine
import com.example.pccomponent.domain.feasibility.answersFor
import com.example.pccomponent.domain.questionnaire.AnswerAvailability
import kotlinx.serialization.json.Json
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test fun addition_isCorrect() = assertEquals(4, 2 + 2)

    @Test
    fun candidateCompatibilityIgnoresExistingProblemsInOtherCategories() {
        val motherboard = component("motherboard", ComponentCategory.MOTHERBOARD, Specifications(socket = "AM5", memoryType = "DDR5"))
        val incompatibleRam = component("ram", ComponentCategory.RAM, Specifications(memoryType = "DDR4"))
        val compatibleCpu = component("cpu-am5", ComponentCategory.CPU, Specifications(socket = "AM5"))
        val current = listOf(motherboard, incompatibleRam)
        assertEquals(CompatibilityStatus.INCOMPATIBLE, CompatibilityChecker.check(PcConfiguration(current, "Проверка", Int.MAX_VALUE)).status)
        assertTrue(CompatibilityChecker.isCandidateCompatible(compatibleCpu, current))
    }

    @Test
    fun candidateCompatibilityRejectsNewSocketMismatch() {
        val motherboard = component("motherboard", ComponentCategory.MOTHERBOARD, Specifications(socket = "AM5", memoryType = "DDR5"))
        val incompatibleCpu = component("cpu-lga1700", ComponentCategory.CPU, Specifications(socket = "LGA1700"))
        assertFalse(CompatibilityChecker.isCandidateCompatible(incompatibleCpu, listOf(motherboard)))
    }

    @Test
    fun candidateCompatibilityRejectsWrongGpuPowerConnector() {
        val gpu = component(
            "gpu-12v",
            ComponentCategory.GPU,
            Specifications(requiredGpuPowerConnectors = "12VHPWR")
        )
        val psu = component(
            "psu-8pin",
            ComponentCategory.POWER_SUPPLY,
            Specifications(gpuPowerConnectors = "8-pin")
        )
        assertEquals(
            CompatibilityStatus.INCOMPATIBLE,
            CompatibilityChecker.check(PcConfiguration(listOf(gpu, psu), "Проверка", Int.MAX_VALUE)).status
        )
    }

    @Test
    fun recommendationReplacesPsuWithConnectorCompatibleAlternative() {
        val gpu = component(
            "gpu-12v",
            ComponentCategory.GPU,
            Specifications(requiredGpuPowerConnectors = "12VHPWR", lengthMm = 200)
        ).copy(powerConsumptionW = 200)
        val incompatiblePsu = component(
            "psu-8pin",
            ComponentCategory.POWER_SUPPLY,
            Specifications(powerWatts = 850, gpuPowerConnectors = "8-pin")
        ).copy(price = 20)
        val compatiblePsu = component(
            "psu-12v",
            ComponentCategory.POWER_SUPPLY,
            Specifications(powerWatts = 850, gpuPowerConnectors = "12VHPWR")
        ).copy(price = 25)
        val result = BuildRecommendationEngine().choose(
            validBuild(
                gpu = gpu,
                powerSupply = incompatiblePsu
            ) + compatiblePsu,
            1_000,
            "test",
            1,
            "price"
        )
        assertEquals("psu-12v", result.single { it.category == ComponentCategory.POWER_SUPPLY }.id)
        assertEquals(
            CompatibilityStatus.COMPATIBLE,
            CompatibilityChecker.check(PcConfiguration(result, "test", 1_000)).status
        )
    }

    @Test
    fun questionnaireResolvesBeginnerGamingRequirements() {
        val result = resolve(mapOf("knowledgeLevel" to "beginner", "usage" to "gaming", "budget" to "budget-100000", "performance" to "performance-2", "memory" to "memory-32", "storage" to "storage-1000", "priority" to "balanced"))
        assertEquals("beginner", result.knowledgeLevel); assertEquals("gaming", result.usage)
        assertEquals(100000, result.budget); assertEquals(32, result.requiredMemoryGb); assertEquals("balanced", result.priority)
    }

    @Test
    fun questionnaireResolvesIntermediateWithPreferredMemoryType() {
        val result = resolve(mapOf("knowledgeLevel" to "intermediate", "usage" to "office", "budget" to "budget-70000", "performance" to "performance-1", "memory" to "memory-16", "storage" to "storage-500", "memoryType" to "ddr4", "priority" to "price"))
        assertEquals("intermediate", result.knowledgeLevel); assertEquals("office", result.usage); assertEquals("DDR4", result.preferredMemoryType)
    }

    @Test
    fun questionnaireResolvesAdvancedAndMergesLaterAnswers() {
        val result = resolve(mapOf("knowledgeLevel" to "advanced", "usage" to "workstation", "budget" to "budget-200000", "performance" to "performance-3", "memory" to "memory-64", "storage" to "storage-2000", "memoryType" to "ddr5", "priority" to "upgrade"))
        assertEquals("advanced", result.knowledgeLevel); assertEquals(64, result.requiredMemoryGb); assertEquals(2000, result.requiredStorageGb)
        assertEquals("DDR5", result.preferredMemoryType); assertEquals("upgrade", result.priority)
    }

    @Test(expected = IllegalArgumentException::class)
    fun questionnaireRejectsMissingRequiredAnswer() {
        QuestionnaireResolver.resolve(QuestionnaireDefinition.questions(), mapOf("knowledgeLevel" to "beginner"))
    }

    @Test
    fun recommendationAcceptsNormalizedRequirementsAndFiltersMemoryAndStorage() {
        val requirements = BuildRequirements("intermediate", "gaming", 200000, 1, 32, 1000, "DDR5", "balanced")
        val components = validBuild(
            ram16 = component("ram-16", ComponentCategory.RAM, Specifications(memoryType = "DDR5", memoryCapacityGb = 16)).copy(price = 1000, performanceLevel = 3),
            ram32 = component("ram-32", ComponentCategory.RAM, Specifications(memoryType = "DDR5", memoryCapacityGb = 32)).copy(price = 1000, performanceLevel = 3),
            storage500 = component("storage-500", ComponentCategory.STORAGE, Specifications(capacityGb = 500)).copy(price = 1000, performanceLevel = 3),
            storage1000 = component("storage-1000", ComponentCategory.STORAGE, Specifications(capacityGb = 1000)).copy(price = 1000, performanceLevel = 3)
        )
        val result = BuildRecommendationEngine().choose(components, requirements)
        assertTrue(result.any { it.id == "ram-32" }); assertFalse(result.any { it.id == "ram-16" })
        assertTrue(result.any { it.id == "storage-1000" }); assertFalse(result.any { it.id == "storage-500" })
    }

    @Test
    fun questionnaireSupportsAllKnowledgeLevelsAndUsageScenarios() {
        val beginner = resolve(mapOf(
            "knowledgeLevel" to "beginner", "usage" to "office", "budget" to "budget-60000",
            "performance" to "performance-1", "memory" to "memory-8", "storage" to "storage-500",
            "priority" to "price"
        ))
        val intermediate = resolve(mapOf(
            "knowledgeLevel" to "intermediate", "usage" to "development", "budget" to "budget-100000",
            "performance" to "performance-2", "memory" to "memory-16", "storage" to "storage-1000",
            "memoryType" to "none", "priority" to "balanced"
        ))
        val advanced = resolve(mapOf(
            "knowledgeLevel" to "advanced", "usage" to "creative", "budget" to "budget-250000",
            "performance" to "performance-4", "memory" to "memory-64", "storage" to "storage-2000",
            "memoryType" to "ddr5", "priority" to "upgrade"
        ))

        assertEquals("beginner", beginner.knowledgeLevel)
        assertEquals("office", beginner.usage)
        assertEquals(1, beginner.minPerformanceLevel)
        assertEquals("intermediate", intermediate.knowledgeLevel)
        assertEquals("development", intermediate.usage)
        assertEquals(2, intermediate.minPerformanceLevel)
        assertEquals("advanced", advanced.knowledgeLevel)
        assertEquals("creative", advanced.usage)
        assertEquals(4, advanced.minPerformanceLevel)
        assertEquals(250000, advanced.budget)
        assertNull(intermediate.preferredMemoryType)
    }

    @Test
    fun usageAnswersMapToGamingAndOffice() {
        assertEquals("gaming", resolve(requiredAnswers("gaming")).usage)
        assertEquals("office", resolve(requiredAnswers("office")).usage)
    }

    @Test
    fun performanceAnswerOverridesUsageDefault() {
        val requirements = resolve(requiredAnswers("gaming") + ("performance" to "performance-1"))
        assertEquals(1, requirements.minPerformanceLevel)
    }

    @Test
    fun unknownAnswerIdIsReportedWithQuestionContext() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            resolve(requiredAnswers("office") + ("budget" to "missing-budget"))
        }
        assertTrue(exception.message!!.contains("budget"))
    }

    @Test
    fun priorityPricePrefersCheaperComponent() {
        val result = BuildRecommendationEngine().choose(priorityComponents(), 1000, "test", 1, "price")
        assertEquals("cheap", result.first { it.category == ComponentCategory.CPU }.id)
    }

    @Test
    fun priorityPerformancePrefersMorePerformantComponent() {
        val result = BuildRecommendationEngine().choose(priorityComponents(), 1000, "test", 1, "performance")
        assertEquals("fast", result.first { it.category == ComponentCategory.CPU }.id)
    }

    @Test
    fun balancedPriorityKeepsExistingPerformanceThenPriceOrdering() {
        val result = BuildRecommendationEngine().choose(priorityComponents(), 1000, "test", 1, "balanced")
        assertEquals("fast", result.first { it.category == ComponentCategory.CPU }.id)
    }

    @Test
    fun performancePriorityStillFindsCheapestBuildWithinBudget() {
        val cheapCpu = component(
            "cpu-cheap",
            ComponentCategory.CPU,
            Specifications(socket = "AM5")
        ).copy(price = 10, performanceLevel = 1)
        val expensivePerformanceCpus = (1..12).map { index ->
            component(
                "cpu-fast-$index",
                ComponentCategory.CPU,
                Specifications(socket = "AM5")
            ).copy(price = 100, performanceLevel = 2)
        }
        val components = validBuild(cpu = cheapCpu) + expensivePerformanceCpus

        val result = BuildRecommendationEngine().choose(
            components,
            budget = 150,
            purpose = "test",
            performanceLevel = 1,
            priority = "performance"
        )

        assertEquals("cpu-cheap", result.first { it.category == ComponentCategory.CPU }.id)
        assertEquals(CompatibilityStatus.COMPATIBLE, CompatibilityChecker.check(PcConfiguration(result, "test", 150)).status)
    }

    @Test
    fun recommendationReturnsEmptyWhenCompleteBuildCannotFitBudget() {
        val components = validBuild().map { it.copy(price = 80) }
        assertTrue(BuildRecommendationEngine().choose(components, 100, "test", 3, "balanced").isEmpty())
    }

    @Test
    fun recommendationRejectsMismatchedCpuAndMotherboardSockets() {
        val components = validBuild(
            cpu = component("cpu-lga", ComponentCategory.CPU, Specifications(socket = "LGA1700")).copy(price = 10),
            motherboard = component("board-am5", ComponentCategory.MOTHERBOARD, Specifications(socket = "AM5", memoryType = "DDR5", formFactor = "mATX")).copy(price = 10)
        )
        assertTrue(BuildRecommendationEngine().choose(components, 1000, "test", 1).isEmpty())
    }

    @Test
    fun recommendationRejectsMismatchedRamType() {
        val components = validBuild(
            ram32 = component("ram-ddr4", ComponentCategory.RAM, Specifications(memoryType = "DDR4", memoryCapacityGb = 32)).copy(price = 10)
        )
        assertTrue(BuildRecommendationEngine().choose(components, 1000, "test", 1).isEmpty())
    }

    @Test
    fun recommendationFailsSafelyWhenRequiredCompatibilityMetadataIsMissing() {
        val noCpuSocket = validBuild(
            cpu = component("cpu-no-socket", ComponentCategory.CPU, Specifications()).copy(price = 10)
        )
        val noPsuRating = validBuild(
            powerSupply = component("psu-no-rating", ComponentCategory.POWER_SUPPLY, Specifications()).copy(price = 10)
        )
        assertTrue(BuildRecommendationEngine().choose(noCpuSocket, 1000, "test", 1).isEmpty())
        assertTrue(BuildRecommendationEngine().choose(noPsuRating, 1000, "test", 1).isEmpty())
    }

    @Test
    fun recommendationRejectsUnsupportedCaseFormFactorAndGpuLength() {
        val wrongBoard = validBuild(
            motherboard = component("board-atx", ComponentCategory.MOTHERBOARD, Specifications(socket = "AM5", memoryType = "DDR5", formFactor = "ATX")).copy(price = 10),
            pcCase = component("case-itx", ComponentCategory.PC_CASE, Specifications(formFactor = "ITX", maxGpuLengthMm = 300)).copy(price = 10)
        )
        val longGpu = validBuild(
            gpu = component("gpu-long", ComponentCategory.GPU, Specifications(lengthMm = 400)).copy(price = 10)
        )
        assertTrue(BuildRecommendationEngine().choose(wrongBoard, 1000, "test", 1).isEmpty())
        assertTrue(BuildRecommendationEngine().choose(longGpu, 1000, "test", 1).isEmpty())
    }

    @Test
    fun recommendationRejectsUnsupportedOrUnderpoweredCooler() {
        val wrongSocket = validBuild(
            cooler = component("cooler-wrong", ComponentCategory.COOLER, Specifications(supportedSockets = listOf("LGA1700"), maxTdpWatts = 200)).copy(price = 10)
        )
        val weakCooler = validBuild(
            cooler = component("cooler-weak", ComponentCategory.COOLER, Specifications(supportedSockets = listOf("AM5"), maxTdpWatts = 50)).copy(price = 10)
        )
        assertTrue(BuildRecommendationEngine().choose(wrongSocket, 1000, "test", 1).isEmpty())
        assertTrue(BuildRecommendationEngine().choose(weakCooler, 1000, "test", 1).isEmpty())
    }

    @Test
    fun recommendationRejectsInsufficientPowerSupplyAndReturnsCompleteBuildWhenValid() {
        val weakPsu = validBuild(
            powerSupply = component("psu-weak", ComponentCategory.POWER_SUPPLY, Specifications(powerWatts = 100)).copy(price = 10)
        )
        assertTrue(BuildRecommendationEngine().choose(weakPsu, 1000, "test", 1).isEmpty())

        val result = BuildRecommendationEngine().choose(validBuild(), 1000, "test", 1)
        assertEquals(ComponentCategory.entries.toSet(), result.map { it.category }.toSet())
        assertEquals(CompatibilityStatus.COMPATIBLE, CompatibilityChecker.check(PcConfiguration(result, "test", 1000)).status)
    }

    @Test
    fun recommendReportsBudgetTooLowWithoutReturningPartialComponents() {
        val requirements = BuildRequirements("beginner", "general", 100, 1, 32, 1000, "DDR5", "balanced")
        val result = BuildRecommendationEngine().recommend(validBuild(), requirements)

        assertEquals(RecommendationStatus.BUDGET_TOO_LOW, result.status)
        assertTrue(result.components.isEmpty())
        assertEquals(160, result.minimumRequiredBudget)
    }

    @Test
    fun recommendReportsNoCompatibleBuildSeparatelyFromBudgetFailure() {
        val requirements = BuildRequirements("beginner", "general", 1000, 1, 32, 1000, "DDR5", "balanced")
        val components = validBuild(
            motherboard = component(
                "wrong-board",
                ComponentCategory.MOTHERBOARD,
                Specifications(socket = "LGA1700", memoryType = "DDR5", formFactor = "mATX")
            ).copy(price = 20, performanceLevel = 1)
        )
        val result = BuildRecommendationEngine().recommend(components, requirements)

        assertEquals(RecommendationStatus.NO_COMPATIBLE_BUILD, result.status)
        assertTrue(result.components.isEmpty())
        assertNull(result.minimumRequiredBudget)
    }

    @Test
    fun recommendRelaxesPerformanceWhenRequestedLevelHasNoCompleteBuild() {
        val requirements = BuildRequirements("advanced", "creative", 1000, 4, 64, 2000, "DDR5", "upgrade")
        val result = BuildRecommendationEngine().recommend(
            validBuild().map { it.copy(performanceLevel = 3) }.map {
                when (it.category) {
                    ComponentCategory.RAM -> it.copy(
                        id = "ram-ddr5-64",
                        specifications = Specifications(memoryType = "DDR5", memoryCapacityGb = 64),
                        price = 20
                    )
                    ComponentCategory.STORAGE -> it.copy(
                        id = "storage-2tb",
                        specifications = Specifications(capacityGb = 2000),
                        price = 20
                    )
                    else -> it
                }
            },
            requirements
        )

        assertEquals(RecommendationStatus.SUCCESS, result.status)
        assertEquals(3, result.appliedPerformanceLevel)
        assertEquals(ComponentCategory.entries.toSet(), result.components.map { it.category }.toSet())
    }

    @Test
    fun cheaperAlternativeStillProducesCompatibleCompleteBuild() {
        val components = validBuild(
            gpu = component("gpu-expensive", ComponentCategory.GPU, Specifications(lengthMm = 400))
                .copy(price = 900, performanceLevel = 3, powerConsumptionW = 150),
            gpuAlternative = component("gpu-cheap", ComponentCategory.GPU, Specifications(lengthMm = 200))
                .copy(price = 20, performanceLevel = 1, powerConsumptionW = 50)
        )
        val result = BuildRecommendationEngine().choose(components, 200, "test", 1, "price")
        assertEquals("gpu-cheap", result.first { it.category == ComponentCategory.GPU }.id)
        assertEquals(CompatibilityStatus.COMPATIBLE, CompatibilityChecker.check(PcConfiguration(result, "test", 200)).status)
    }

    @Test
    fun catalogContainsAllTwoHundredComponents() {
        val catalog = Json { ignoreUnknownKeys = true }.decodeFromString<com.example.pccomponent.data.model.ComponentCatalog>(
            File("src/main/assets/catalog/components.json").readText()
        )
        assertEquals(5, catalog.version)
        assertEquals(309, catalog.components.size)
        assertEquals(309, catalog.components.map { it.id }.toSet().size)
        assertEquals(
            setOf(ComponentCategory.CPU, ComponentCategory.GPU, ComponentCategory.MOTHERBOARD,
                ComponentCategory.RAM, ComponentCategory.STORAGE, ComponentCategory.POWER_SUPPLY,
                ComponentCategory.PC_CASE, ComponentCategory.COOLER),
            catalog.components.map { it.category }.toSet()
        )
        assertTrue(catalog.components.all { it.price > 0 })
        assertTrue(catalog.components.all { it.powerConsumptionW >= 0 })
    }

    @Test
    fun recommendationBuildsCompatibleConfigurationFromExistingCatalog() {
        val catalog = Json { ignoreUnknownKeys = true }.decodeFromString<com.example.pccomponent.data.model.ComponentCatalog>(
            File("src/main/assets/catalog/components.json").readText()
        )
        val requirements = BuildRequirements("beginner", "general", 250000, 1, 8, 500, null, "balanced")
        val result = BuildRecommendationEngine().choose(catalog.components, requirements)
        assertEquals(ComponentCategory.entries.toSet(), result.map { it.category }.toSet())
        assertEquals(CompatibilityStatus.COMPATIBLE, CompatibilityChecker.check(PcConfiguration(result, "test", 250000)).status)
    }

    @Test
    fun partialQuestionnaireStateRebuildsAndClearsHiddenAnswers() {
        val questions = QuestionnaireDefinition.questions()
        val answers = mapOf("knowledgeLevel" to "intermediate", "memoryType" to "ddr5")
        val state = QuestionnaireResolver.state(questions, answers)
        assertEquals("DDR5", state.requirements.preferredMemoryType)
        val changed = QuestionnaireResolver.state(questions, answers + ("knowledgeLevel" to "beginner"))
        assertFalse(changed.answers.containsKey("memoryType"))
        assertNull(changed.requirements.preferredMemoryType)
    }

    @Test
    fun feasibilityUsesCompleteBuildAndReportsMinimumBudget() {
        val catalog = Json { ignoreUnknownKeys = true }.decodeFromString<com.example.pccomponent.data.model.ComponentCatalog>(
            File("src/main/assets/catalog/components.json").readText()
        )
        val engine = com.example.pccomponent.domain.feasibility.BuildFeasibilityEngine(catalog.components)
        val requirements = com.example.pccomponent.domain.questionnaire.PartialBuildRequirements(
            budget = 30000, minPerformanceLevel = 1, requiredMemoryGb = 64, requiredStorageGb = 500
        )
        val result = engine.evaluate(requirements)
        assertFalse(result.isAvailable)
        assertNotNull(result.minimumRequiredBudget)
        assertTrue(result.minimumRequiredBudget!! > 30000)
    }

    @Test
    fun allUsageAnswersRemainAvailableAfterKnowledgeSelection() {
        val catalog = Json { ignoreUnknownKeys = true }.decodeFromString<com.example.pccomponent.data.model.ComponentCatalog>(
            File("src/main/assets/catalog/components.json").readText()
        )
        val engine = BuildFeasibilityEngine(catalog.components)
        val question = QuestionnaireDefinition.questions().first { it.id == "usage" }
        val current = QuestionnaireResolver.resolvePartial(
            QuestionnaireDefinition.questions(),
            mapOf("knowledgeLevel" to "beginner")
        )
        val answers = engine.answersFor(question, current)
        assertTrue(answers.filter { it.id == "office" || it.id == "study" }
            .all { it.availability == AnswerAvailability.AVAILABLE })
    }
    private fun requiredAnswers(usage: String): Map<String, String> = mapOf(
        "knowledgeLevel" to "beginner",
        "usage" to usage,
        "budget" to "budget-100000",
        "performance" to "performance-2",
        "memory" to "memory-16",
        "storage" to "storage-500",
        "priority" to "balanced"
    )

    private fun priorityComponents() = validBuild(
        cpu = component("cheap", ComponentCategory.CPU, Specifications(socket = "AM5")).copy(price = 20, performanceLevel = 1),
        cpuAlternative = component("balanced", ComponentCategory.CPU, Specifications(socket = "AM5")).copy(price = 40, performanceLevel = 2),
        cpuFast = component("fast", ComponentCategory.CPU, Specifications(socket = "AM5")).copy(price = 80, performanceLevel = 3)
    )

    private fun validBuild(
        cpu: Component = component("cpu", ComponentCategory.CPU, Specifications(socket = "AM5")).copy(price = 20, performanceLevel = 1, powerConsumptionW = 65),
        cpuAlternative: Component? = null,
        cpuFast: Component? = null,
        motherboard: Component = component("motherboard", ComponentCategory.MOTHERBOARD, Specifications(socket = "AM5", memoryType = "DDR5", formFactor = "mATX")).copy(price = 20, performanceLevel = 1),
        ram16: Component? = null,
        ram32: Component = component("ram", ComponentCategory.RAM, Specifications(memoryType = "DDR5", memoryCapacityGb = 32)).copy(price = 20, performanceLevel = 1),
        storage1000: Component = component("storage", ComponentCategory.STORAGE, Specifications(capacityGb = 1000)).copy(price = 20, performanceLevel = 1),
        storage500: Component? = null,
        gpu: Component = component("gpu", ComponentCategory.GPU, Specifications(lengthMm = 200)).copy(price = 20, performanceLevel = 1, powerConsumptionW = 50),
        gpuAlternative: Component? = null,
        pcCase: Component = component("case", ComponentCategory.PC_CASE, Specifications(formFactor = "mATX", maxGpuLengthMm = 300)).copy(price = 20, performanceLevel = 1),
        cooler: Component = component("cooler", ComponentCategory.COOLER, Specifications(supportedSockets = listOf("AM5"), maxTdpWatts = 100)).copy(price = 20, performanceLevel = 1),
        powerSupply: Component = component("psu", ComponentCategory.POWER_SUPPLY, Specifications(powerWatts = 450)).copy(price = 20, performanceLevel = 1)
    ): List<Component> = listOfNotNull(
        cpu, cpuAlternative, cpuFast, motherboard, ram16, ram32, storage1000, storage500, gpu, gpuAlternative, pcCase, cooler, powerSupply
    )

    private fun resolve(answers: Map<String, String>) = QuestionnaireResolver.resolve(QuestionnaireDefinition.questions(), answers)

    private fun component(id: String, category: ComponentCategory, specifications: Specifications) = Component(id, category, id, "Test", 1, 1, 1, "", specifications = specifications)
}





