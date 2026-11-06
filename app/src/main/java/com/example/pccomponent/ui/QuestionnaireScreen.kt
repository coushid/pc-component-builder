package com.example.pccomponent


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.pccomponent.data.model.Component
import com.example.pccomponent.data.model.ComponentCategory
import com.example.pccomponent.data.model.PcConfiguration
import com.example.pccomponent.data.repository.ComponentRepository
import com.example.pccomponent.domain.compatibility.CompatibilityChecker
import com.example.pccomponent.domain.compatibility.CompatibilityStatus
import com.example.pccomponent.domain.recommendation.BuildRecommendationEngine
import com.example.pccomponent.domain.recommendation.BuildRecommendationResult
import com.example.pccomponent.domain.recommendation.RecommendationStatus
import com.example.pccomponent.domain.feasibility.BuildFeasibilityEngine
import com.example.pccomponent.domain.feasibility.answersForParallel
import com.example.pccomponent.domain.feasibility.answersFor
import com.example.pccomponent.domain.questionnaire.AnswerAvailability
import com.example.pccomponent.domain.questionnaire.QuestionnaireDefinition
import com.example.pccomponent.domain.questionnaire.QuestionnaireResolver
import com.example.pccomponent.ui.theme.AppTextStyles
import com.example.pccomponent.ui.theme.AppTypography
import com.example.pccomponent.ui.theme.AppColors
import com.example.pccomponent.ui.theme.SectionTitle as AppSectionTitle
import com.example.pccomponent.ui.theme.MetricLabel
import com.example.pccomponent.ui.theme.MetricValue


@Composable
internal fun ChatScreen(
    catalog: List<Component>,
    onFinished: (BuildRecommendationResult, com.example.pccomponent.domain.questionnaire.BuildRequirements) -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var selectedAnswers by rememberSaveable(stateSaver = listSaver(
        save = { answers -> answers.entries.flatMap { listOf(it.key, it.value) } },
        restore = { saved -> saved.chunked(2).associate { it[0] to it[1] } }
    )) { mutableStateOf<Map<String, String>>(emptyMap()) }
    val allQuestions = remember { QuestionnaireDefinition.questions() }
    val feasibility = remember(catalog) { BuildFeasibilityEngine(catalog) }
    val state = remember(selectedAnswers) { QuestionnaireResolver.state(allQuestions, selectedAnswers) }
    val knowledgeLevel = state.requirements.knowledgeLevel
    val visibleQuestions = allQuestions.filter { QuestionnaireResolver.isVisible(it, selectedAnswers) && (it.knowledgeLevels.isEmpty() || knowledgeLevel == null || it.knowledgeLevels.any { level -> level.name.equals(knowledgeLevel, true) }) }
    /*
     * Changing an earlier answer can hide a later question.  In that case the
     * saved step may temporarily point past the new list.  Do not index the
     * list until the question is known to exist; this also makes restoring the
     * screen state after process recreation safe.
     */
    val currentQuestion = visibleQuestions.getOrNull(step)
    val isReview = currentQuestion == null
    LaunchedEffect(visibleQuestions.size, selectedAnswers) {
        if (visibleQuestions.isNotEmpty() && step > visibleQuestions.size) {
            step = visibleQuestions.size
        }
    }
    val dynamicAnswers by produceState(
        initialValue = currentQuestion?.answers?.map { answer ->
            com.example.pccomponent.domain.questionnaire.DynamicQuestionnaireAnswer(
                id = answer.id,
                text = answer.text,
                value = answer.value,
                requirements = answer.requirements,
                availability = AnswerAvailability.AVAILABLE
            )
        }.orEmpty(),
        key1 = currentQuestion?.id,
        key2 = state.requirements
    ) {
        value = currentQuestion?.let { question ->
            withContext(Dispatchers.Default) {
                feasibility.answersForParallel(question, state.requirements)
            }
        }.orEmpty()
    }
    // Render the choices immediately. The availability pass is asynchronous
    // and replaces these optimistic values when it completes.
    val answersLoaded = isReview || currentQuestion != null
    var recommendationInProgress by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val canContinue = if (isReview) {
        visibleQuestions.filter { it.required }.all { selectedAnswers.containsKey(it.id) }
    } else {
        answersLoaded && selectedAnswers.containsKey(currentQuestion!!.id)
    }
    Page("Подбор конфигурации", showTopBar = false) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            SectionTitle("Подбор конфигурации")
            Text(if (isReview) "Проверьте ответы" else "Шаг ${step + 1} из ${visibleQuestions.size}", color = Accent, style = AppTextStyles.bold)
            Spacer(Modifier.height(10.dp))
            if (isReview) {
                Text("Доступность проверена по полной совместимой сборке.", color = Muted, style = AppTextStyles.secondary)
                Spacer(Modifier.height(12.dp))
                visibleQuestions.forEachIndexed { index, question ->
                    Card(onClick = { step = index }, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(reviewQuestionLabel(question.id), color = Muted, style = AppTextStyles.secondary); Text(question.answers.firstOrNull { it.id == selectedAnswers[question.id] }?.text ?: "Не выбран", style = AppTextStyles.semiBold) }
                            Text("Изменить", color = Accent, style = AppTextStyles.metadata)
                        }
                    }
                }
            } else {
                val question = checkNotNull(currentQuestion)
                Text(questionText(question.id, knowledgeLevel), style = AppTextStyles.sectionTitle)
                Text("Недоступные варианты требуют другого бюджета или не имеют полной совместимой сборки.", color = Muted, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(18.dp))
                question.answers.forEach { answer ->
                    // The optimistic list is always populated, but keep a safe
                    // fallback here so a delayed/failed availability pass can
                    // never make the whole question look empty.
                    val dynamic = dynamicAnswers.firstOrNull { it.id == answer.id }
                        ?: com.example.pccomponent.domain.questionnaire.DynamicQuestionnaireAnswer(
                            id = answer.id,
                            text = answer.text,
                            value = answer.value,
                            requirements = answer.requirements,
                            availability = AnswerAvailability.AVAILABLE
                        )
                    val blocked = dynamic.availability == AnswerAvailability.BLOCKED
                    Choice(answer.text + if (blocked && dynamic.minimumRequiredBudget != null) " — от ${dynamic.minimumRequiredBudget} руб." else "", selectedAnswers[question.id] == answer.id && !blocked) {
                        if (!blocked) {
                            val updated = selectedAnswers + (question.id to answer.id)
                            selectedAnswers = QuestionnaireResolver.state(allQuestions, updated).answers
                        }
                    }
                    if (blocked) Text(dynamic.reason ?: "Недоступно при текущих требованиях", color = IncompatibleColor, style = AppTextStyles.metadata, modifier = Modifier.padding(start = 12.dp, bottom = 6.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { step = 0; selectedAnswers = emptyMap() }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Сбросить", color = Muted, style = AppTextStyles.buttonLabel, maxLines = 1) }
                OutlinedButton(onClick = { step-- }, enabled = step > 0, modifier = Modifier.weight(1f).height(50.dp)) { Text("Назад", style = AppTextStyles.buttonLabel, maxLines = 1) }
                Button(
                    onClick = {
                        if (isReview) {
                            val requirements = QuestionnaireResolver.resolve(allQuestions, selectedAnswers)
                            recommendationInProgress = true
                            coroutineScope.launch {
                                val result = withContext(Dispatchers.Default) {
                                    BuildRecommendationEngine().recommend(catalog, requirements)
                                }
                                recommendationInProgress = false
                                onFinished(result, requirements)
                            }
                        } else {
                            step++
                        }
                    },
                    enabled = canContinue && !recommendationInProgress,
                    modifier = Modifier.weight(1.2f).height(50.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)
                ) {
                    Text(if (recommendationInProgress) "Подбираем…" else if (isReview) "Показать" else "Далее", style = AppTextStyles.bold)
                }
            }
        }
    }
}
internal fun questionText(id: String, knowledgeLevel: String?): String = when (id) {
    "knowledgeLevel" -> "Как хорошо вы разбираетесь в комплектующих?"
    "usage" -> if (knowledgeLevel == "beginner") "Для чего нужен компьютер?" else "Какие задачи будут основными?"
    "budget" -> "Какой бюджет планируете?"
    "performance" -> when (knowledgeLevel?.lowercase()) {
        "beginner" -> "Что важнее: низкая цена или высокая скорость?"
        "advanced" -> "Какой минимальный уровень производительности требуется?"
        else -> "Какой уровень производительности нужен?"
    }
    "memory" -> if (knowledgeLevel == "beginner") "Сколько памяти нужно для ваших задач?" else "Какой объём оперативной памяти нужен?"
    "storage" -> if (knowledgeLevel == "advanced") "Какой объём накопителя необходим?" else "Какой объём накопителя нужен?"
    "memoryType" -> if (knowledgeLevel == "advanced") "Какой тип оперативной памяти предпочитаете?" else "Нужна DDR4 или DDR5?"
    "priority" -> if (knowledgeLevel == "beginner") "Что важнее: низкая цена или высокая скорость?" else "Что важнее при выборе?"
    else -> id
}

internal fun reviewQuestionLabel(id: String): String = when (id) {
    "knowledgeLevel" -> "Уровень знаний"
    "usage" -> "Основные задачи"
    "budget" -> "Бюджет"
    "performance" -> "Производительность"
    "memory" -> "Оперативная память"
    "storage" -> "Накопитель"
    "memoryType" -> "Тип памяти"
    "priority" -> "Приоритет"
    else -> id
}

internal fun usageTitle(usage: String): String = when (usage) {
    "gaming" -> "Игровой компьютер"
    "office" -> "Офисный компьютер"
    "study" -> "Компьютер для учёбы"
    "workstation" -> "Рабочая станция"
    else -> "Сборка"
}

private data class SelectionStep(val title: String, val question: String, val options: List<String>)

internal fun budgetLabel(budget: Int): String = when (budget) {
    70000 -> "До 70 000 руб."
    150000 -> "150 000 руб."
    200000 -> "200 000 руб."
    else -> "100 000 руб."
}

internal fun performanceLabel(level: Int): String = when (level) {
    1 -> "Экономия"
    3 -> "Максимальная производительность"
    else -> "Сбалансированный вариант"
}

internal fun recommendationBudget(budget: Int, extras: String): Int =
    if (extras.startsWith("Добавить")) (budget * 0.9f).toInt() else budget

internal fun recommendationPerformance(base: Int, resolution: String, games: String): Int {
    var result = base
    if (resolution.startsWith("2K") || resolution.startsWith("4K")) result++
    if (games == "Тяжёлые AAA-игры" || games == "Монтаж и 3D") result++
    if (games == "Киберспорт и лёгкие игры") result--
    return result.coerceIn(1, 3)
}

@Composable
internal fun Choice(text: String, selected: Boolean, onClick: () -> Unit) { Surface(onClick = onClick, color = if (selected) Accent.copy(alpha = .16f) else Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).background(if (selected) Accent else Muted, RoundedCornerShape(5.dp))); Spacer(Modifier.width(12.dp)); Text(text, style = if (selected) AppTextStyles.bold else AppTextStyles.normal) } } }

