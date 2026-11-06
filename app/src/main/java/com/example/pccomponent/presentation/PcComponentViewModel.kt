package com.example.pccomponent.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pccomponent.data.model.Component
import com.example.pccomponent.data.model.PcConfiguration
import com.example.pccomponent.domain.recommendation.BuildRecommendationResult
import com.example.pccomponent.domain.recommendation.RecommendationStatus
import com.example.pccomponent.data.storage.SavedConfigurationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PcComponentUiState(
    val savedConfigurations: List<PcConfiguration> = emptyList(),
    val configuration: PcConfiguration? = null,
    val recommendationResult: BuildRecommendationResult? = null,
    val activeSavedIndex: Int? = null
)

class PcComponentViewModel(
    private val context: Context,
    private val catalog: List<Component>
) : ViewModel() {
    private val store = SavedConfigurationStore(context, catalog)
    private val initialSaved = store.getAll()
    private val _uiState = MutableStateFlow(
        PcComponentUiState(
            savedConfigurations = initialSaved,
            configuration = initialSaved.firstOrNull(),
            recommendationResult = initialSaved.firstOrNull()?.let {
                BuildRecommendationResult(RecommendationStatus.SUCCESS, it.components)
            },
            activeSavedIndex = initialSaved.firstOrNull()?.let { 0 }
        )
    )
    val uiState: StateFlow<PcComponentUiState> = _uiState.asStateFlow()

    fun updateConfiguration(value: PcConfiguration) = _uiState.update { it.copy(configuration = value) }

    fun setRecommendation(result: BuildRecommendationResult, configuration: PcConfiguration) = _uiState.update {
        it.copy(configuration = configuration, recommendationResult = result, activeSavedIndex = null)
    }

    fun resetConfiguration() = _uiState.update { it.copy(configuration = it.configuration?.copy(components = emptyList())) }

    fun saveCurrentConfiguration() {
        _uiState.value.configuration?.let { current ->
            val state = _uiState.value
            if (current.components.isEmpty() && state.activeSavedIndex == null) return
            val updated = state.savedConfigurations.toMutableList()
            val index = state.activeSavedIndex
            if (index != null && index in updated.indices) updated[index] = current
            else { updated += current; _uiState.update { it.copy(activeSavedIndex = updated.lastIndex) } }
            _uiState.update { it.copy(savedConfigurations = updated) }
            store.saveAll(updated)
        }
    }

    fun updateCurrentConfigurationDetails(name: String, budget: Int) {
        val state = _uiState.value
        val current = state.configuration ?: return
        val updatedConfiguration = current.copy(purpose = name, budget = budget)
        _uiState.update { it.copy(configuration = updatedConfiguration) }
        val index = state.activeSavedIndex
        if (index != null && index in state.savedConfigurations.indices) {
            val updated = state.savedConfigurations.toMutableList().also { it[index] = updatedConfiguration }
            _uiState.update { it.copy(savedConfigurations = updated) }
            store.saveAll(updated)
        }
    }

    fun selectSaved(configuration: PcConfiguration, index: Int) = _uiState.update {
        it.copy(configuration = configuration, activeSavedIndex = index)
    }

    fun deleteCurrentConfiguration() {
        val state = _uiState.value
        val index = state.activeSavedIndex ?: return
        if (index !in state.savedConfigurations.indices) return
        val updated = state.savedConfigurations.toMutableList().also { it.removeAt(index) }
        store.saveAll(updated)
        _uiState.value = state.copy(savedConfigurations = updated, configuration = null, activeSavedIndex = null)
    }

    companion object {
        fun factory(context: Context, catalog: List<Component>): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PcComponentViewModel(context.applicationContext, catalog) as T
            }
    }
}
