package com.example.pccomponent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pccomponent.data.model.Component
import com.example.pccomponent.domain.feasibility.BuildFeasibilityEngine
import com.example.pccomponent.domain.feasibility.answersForParallel
import com.example.pccomponent.domain.questionnaire.DynamicQuestionnaireAnswer
import com.example.pccomponent.domain.questionnaire.PartialBuildRequirements
import com.example.pccomponent.domain.questionnaire.QuestionnaireQuestion
import com.example.pccomponent.domain.recommendation.BuildRecommendationEngine
import com.example.pccomponent.domain.recommendation.BuildRecommendationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class QuestionnaireUiState(
    val step: Int = 0,
    val selectedAnswers: Map<String, String> = emptyMap(),
    val dynamicAnswers: List<DynamicQuestionnaireAnswer> = emptyList(),
    val isLoading: Boolean = false,
    val result: BuildRecommendationResult? = null,
    val error: String? = null
)

internal class QuestionnaireViewModel(
    private val catalog: List<Component>
) : ViewModel() {
    private val engine = BuildFeasibilityEngine(catalog)
    private val recommendation = BuildRecommendationEngine()
    private val _uiState = MutableStateFlow(QuestionnaireUiState())
    val uiState: StateFlow<QuestionnaireUiState> = _uiState.asStateFlow()
    private var availabilityJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    fun setStep(step: Int) = _uiState.update { it.copy(step = step.coerceAtLeast(0)) }

    fun reset() {
        availabilityJob?.cancel()
        _uiState.value = QuestionnaireUiState()
    }

    fun selectAnswer(questionId: String, answerId: String, answers: Map<String, String>) {
        _uiState.update { it.copy(selectedAnswers = answers + (questionId to answerId)) }
    }

    fun setAnswers(answers: Map<String, String>) {
        _uiState.update { it.copy(selectedAnswers = answers) }
    }

    fun refreshAvailability(question: QuestionnaireQuestion?, requirements: PartialBuildRequirements) {
        availabilityJob?.cancel()
        if (question == null || question.id == "knowledgeLevel") {
            _uiState.update { it.copy(dynamicAnswers = question?.answers?.map { answer ->
                DynamicQuestionnaireAnswer(answer.id, answer.text, answer.value, answer.requirements,
                    com.example.pccomponent.domain.questionnaire.AnswerAvailability.AVAILABLE)
            }.orEmpty(), isLoading = false, error = null) }
            return
        }
        availabilityJob = scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            delay(150)
            runCatching { engine.answersForParallel(question, requirements) }
                .onSuccess { answers -> _uiState.update { it.copy(dynamicAnswers = answers, isLoading = false) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }

    fun recommend(requirements: com.example.pccomponent.domain.questionnaire.BuildRequirements, onComplete: (BuildRecommendationResult) -> Unit) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            runCatching { recommendation.recommend(catalog, requirements) }
                .onSuccess { result ->
                    _uiState.update { it.copy(result = result, isLoading = false) }
                    withContext(Dispatchers.Main.immediate) { onComplete(result) }
                }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }

    override fun onCleared() {
        availabilityJob?.cancel()
        scope.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(catalog: List<Component>): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    QuestionnaireViewModel(catalog) as T
            }
    }
}
