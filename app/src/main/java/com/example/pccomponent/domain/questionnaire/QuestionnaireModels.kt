package com.example.pccomponent.domain.questionnaire

import kotlinx.serialization.Serializable

@Serializable
enum class KnowledgeLevel { BEGINNER, INTERMEDIATE, ADVANCED }

@Serializable
enum class QuestionAnswerType { SINGLE_CHOICE }

@Serializable
data class RequirementPatch(
    val knowledgeLevel: String? = null,
    val usage: String? = null,
    val budget: Int? = null,
    val performanceLevel: Int? = null,
    val memoryCapacityGb: Int? = null,
    val storageCapacityGb: Int? = null,
    val preferredMemoryType: String? = null,
    val priority: String? = null
)

@Serializable
data class PartialBuildRequirements(
    val knowledgeLevel: String? = null,
    val usage: String? = null,
    val budget: Int? = null,
    val minPerformanceLevel: Int? = null,
    val requiredMemoryGb: Int? = null,
    val requiredStorageGb: Int? = null,
    val preferredMemoryType: String? = null,
    val priority: String? = null
) {
    fun apply(patch: RequirementPatch) = copy(
        knowledgeLevel = patch.knowledgeLevel ?: knowledgeLevel,
        usage = patch.usage ?: usage,
        budget = patch.budget ?: budget,
        minPerformanceLevel = patch.performanceLevel ?: minPerformanceLevel,
        requiredMemoryGb = patch.memoryCapacityGb ?: requiredMemoryGb,
        requiredStorageGb = patch.storageCapacityGb ?: requiredStorageGb,
        preferredMemoryType = patch.preferredMemoryType ?: preferredMemoryType,
        priority = patch.priority ?: priority
    )
}

data class QuestionnaireState(
    val answers: Map<String, String> = emptyMap(),
    val requirements: PartialBuildRequirements = PartialBuildRequirements()
)

enum class AnswerAvailability { AVAILABLE, BLOCKED }

data class DynamicQuestionnaireAnswer(
    val id: String,
    val text: String,
    val value: String,
    val requirements: RequirementPatch,
    val availability: AnswerAvailability,
    val reason: String? = null,
    val minimumRequiredBudget: Int? = null
)

@Serializable
data class QuestionnaireAnswer(
    val id: String,
    val text: String,
    val value: String,
    val requirements: RequirementPatch = RequirementPatch()
)

@Serializable
data class QuestionnaireQuestion(
    val id: String,
    val text: String,
    val knowledgeLevels: Set<KnowledgeLevel> = KnowledgeLevel.entries.toSet(),
    val answerType: QuestionAnswerType = QuestionAnswerType.SINGLE_CHOICE,
    val answers: List<QuestionnaireAnswer>,
    val required: Boolean = true,
    val condition: String? = null
)

@Serializable
data class BuildRequirements(
    val knowledgeLevel: String,
    val usage: String,
    val budget: Int,
    val minPerformanceLevel: Int,
    val requiredMemoryGb: Int? = null,
    val requiredStorageGb: Int? = null,
    val preferredMemoryType: String? = null,
    val priority: String
)

class MissingQuestionnaireAnswerException(questionId: String) :
    IllegalArgumentException("Не выбран обязательный ответ на вопрос: $questionId")

object QuestionnaireResolver {
    fun isVisible(question: QuestionnaireQuestion, selectedAnswerIds: Map<String, String>): Boolean {
        val conditionVisible = question.condition == null || selectedAnswerIds[question.condition.substringBefore("=")] == question.condition.substringAfter("=")
        val knowledge = selectedAnswerIds["knowledgeLevel"]
        val levelVisible = knowledge == null || question.knowledgeLevels.isEmpty() || question.knowledgeLevels.any { it.name.equals(knowledge, true) }
        return conditionVisible && levelVisible
    }

    fun resolvePartial(
        questions: List<QuestionnaireQuestion>,
        selectedAnswerIds: Map<String, String>
    ): PartialBuildRequirements {
        return questions.filter { isVisible(it, selectedAnswerIds) }.fold(PartialBuildRequirements()) { current, question ->
            selectedAnswerIds[question.id]?.let { answerId ->
                val answer = question.answers.firstOrNull { it.id == answerId }
                    ?: throw IllegalArgumentException("Неизвестный ответ $answerId для вопроса ${question.id}")
                current.apply(answer.requirements)
            } ?: current
        }
    }

    fun state(questions: List<QuestionnaireQuestion>, selectedAnswerIds: Map<String, String>): QuestionnaireState {
        val visible = questions.filter { isVisible(it, selectedAnswerIds) }
        val validAnswers = selectedAnswerIds.filterKeys { key -> visible.any { it.id == key } }
        return QuestionnaireState(validAnswers, resolvePartial(questions, validAnswers))
    }

    fun resolve(
        questions: List<QuestionnaireQuestion>,
        selectedAnswerIds: Map<String, String>
    ): BuildRequirements {
        val visible = questions.filter { isVisible(it, selectedAnswerIds) }
        visible.forEach { question ->
            if (question.required && selectedAnswerIds[question.id] == null) {
                throw MissingQuestionnaireAnswerException(question.id)
            }
        }
        val patch = resolvePartial(visible, selectedAnswerIds)
        return BuildRequirements(
            knowledgeLevel = patch.knowledgeLevel ?: error("Не выбран уровень знаний"),
            usage = patch.usage ?: error("Не указано назначение компьютера"),
            budget = patch.budget ?: error("Не указан бюджет"),
            minPerformanceLevel = patch.minPerformanceLevel ?: error("Не указан уровень производительности"),
            requiredMemoryGb = patch.requiredMemoryGb,
            requiredStorageGb = patch.requiredStorageGb,
            preferredMemoryType = patch.preferredMemoryType,
            priority = patch.priority ?: error("Не указан приоритет")
        )
    }
}

object QuestionnaireDefinition {
    fun questions(): List<QuestionnaireQuestion> = listOf(
        QuestionnaireQuestion("knowledgeLevel", "Как хорошо вы разбираетесь в комплектующих?", answers = listOf(
            QuestionnaireAnswer("beginner", "Почти не разбираюсь", "beginner", RequirementPatch(knowledgeLevel = "beginner")),
            QuestionnaireAnswer("intermediate", "Знаю основные характеристики", "intermediate", RequirementPatch(knowledgeLevel = "intermediate")),
            QuestionnaireAnswer("advanced", "Разбираюсь в комплектующих", "advanced", RequirementPatch(knowledgeLevel = "advanced"))
        )),
        QuestionnaireQuestion("usage", "Для чего нужен компьютер?", answers = listOf(
            QuestionnaireAnswer("gaming", "Игры", "gaming", RequirementPatch(usage = "gaming", performanceLevel = 3, memoryCapacityGb = 16, storageCapacityGb = 1000)),
            QuestionnaireAnswer("office", "Работа и документы", "office", RequirementPatch(usage = "office", performanceLevel = 1, memoryCapacityGb = 8, storageCapacityGb = 500)),
            QuestionnaireAnswer("study", "Учёба", "study", RequirementPatch(usage = "study", performanceLevel = 1, memoryCapacityGb = 8, storageCapacityGb = 500)),
            QuestionnaireAnswer("creative", "Обработка фото и видео", "creative", RequirementPatch(usage = "creative", performanceLevel = 3, memoryCapacityGb = 32, storageCapacityGb = 2000)),
            QuestionnaireAnswer("development", "Программирование", "development", RequirementPatch(usage = "development", performanceLevel = 2, memoryCapacityGb = 16, storageCapacityGb = 1000)),
            QuestionnaireAnswer("general", "Универсальное использование", "general", RequirementPatch(usage = "general", performanceLevel = 2, memoryCapacityGb = 8, storageCapacityGb = 500)),
            QuestionnaireAnswer("workstation", "Монтаж и 3D", "workstation", RequirementPatch(usage = "workstation", performanceLevel = 4, memoryCapacityGb = 32, storageCapacityGb = 2000))
        )),
        QuestionnaireQuestion("budget", "Какой бюджет?", answers = listOf(
            QuestionnaireAnswer("budget-60000", "До 60 000 руб.", "60000", RequirementPatch(budget = 60000)),
            QuestionnaireAnswer("budget-70000", "До 70 000 руб.", "70000", RequirementPatch(budget = 70000)),
            QuestionnaireAnswer("budget-100000", "100 000 руб.", "100000", RequirementPatch(budget = 100000)),
            QuestionnaireAnswer("budget-150000", "150 000 руб.", "150000", RequirementPatch(budget = 150000)),
            QuestionnaireAnswer("budget-200000", "200 000 руб.", "200000", RequirementPatch(budget = 200000)),
            QuestionnaireAnswer("budget-250000", "Более 200 000 руб.", "250000", RequirementPatch(budget = 250000))
        )),
        QuestionnaireQuestion("performance", "Какой уровень производительности нужен?", answers = listOf(
            QuestionnaireAnswer("performance-1", "Экономичный уровень", "1", RequirementPatch(performanceLevel = 1)),
            QuestionnaireAnswer("performance-2", "Сбалансированный уровень", "2", RequirementPatch(performanceLevel = 2)),
            QuestionnaireAnswer("performance-3", "Высокий уровень", "3", RequirementPatch(performanceLevel = 3)),
            QuestionnaireAnswer("performance-4", "Максимальный уровень", "4", RequirementPatch(performanceLevel = 4))
        )),
        QuestionnaireQuestion("memory", "Какой объём оперативной памяти нужен?", answers = listOf(
            QuestionnaireAnswer("memory-8", "8 ГБ", "8", RequirementPatch(memoryCapacityGb = 8)),
            QuestionnaireAnswer("memory-16", "16 ГБ", "16", RequirementPatch(memoryCapacityGb = 16)),
            QuestionnaireAnswer("memory-32", "32 ГБ", "32", RequirementPatch(memoryCapacityGb = 32)),
            QuestionnaireAnswer("memory-64", "64 ГБ", "64", RequirementPatch(memoryCapacityGb = 64))
        )),
        QuestionnaireQuestion("storage", "Какой объём накопителя нужен?", answers = listOf(
            QuestionnaireAnswer("storage-500", "500 ГБ", "500", RequirementPatch(storageCapacityGb = 500)),
            QuestionnaireAnswer("storage-1000", "1 ТБ", "1000", RequirementPatch(storageCapacityGb = 1000)),
            QuestionnaireAnswer("storage-2000", "2 ТБ", "2000", RequirementPatch(storageCapacityGb = 2000))
        )),
        QuestionnaireQuestion("memoryType", "Какой тип памяти предпочитаете?", knowledgeLevels = setOf(KnowledgeLevel.INTERMEDIATE, KnowledgeLevel.ADVANCED), required = false, answers = listOf(
            QuestionnaireAnswer("none", "Без предпочтений", "none"),
            QuestionnaireAnswer("ddr4", "DDR4", "DDR4", RequirementPatch(preferredMemoryType = "DDR4")),
            QuestionnaireAnswer("ddr5", "DDR5", "DDR5", RequirementPatch(preferredMemoryType = "DDR5"))
        )),
        QuestionnaireQuestion("priority", "Что важнее при выборе?", answers = listOf(
            QuestionnaireAnswer("price", "Минимальная цена", "price", RequirementPatch(priority = "price")),
            QuestionnaireAnswer("balanced", "Баланс цены и производительности", "balanced", RequirementPatch(priority = "balanced")),
            QuestionnaireAnswer("performance", "Максимальная производительность", "performance", RequirementPatch(priority = "performance")),
            QuestionnaireAnswer("upgrade", "Возможность обновления", "upgrade", RequirementPatch(priority = "upgrade"))
        ))
    )
}

