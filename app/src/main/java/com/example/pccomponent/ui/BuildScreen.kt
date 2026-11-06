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
internal fun BuildScreen(
    catalog: List<Component>,
    initialConfiguration: PcConfiguration? = null,
    onConfigurationChange: (PcConfiguration) -> Unit = {},
    onSaveConfiguration: () -> Unit = {},
    onOpenOverview: (ComponentCategory) -> Unit = {},
    onResetConfiguration: () -> Unit = {},
    onUpdateConfigurationDetails: (String, Int) -> Unit = { _, _ -> },
    onDeleteConfiguration: () -> Unit = {}
) {
    var selectedComponents by remember { mutableStateOf(initialConfiguration?.components.orEmpty()) }
    var editDialogVisible by remember { mutableStateOf(false) }
    var deleteDialogVisible by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(initialConfiguration?.purpose ?: "Конструктор") }
    var newBudget by remember { mutableStateOf(initialConfiguration?.budget?.toString().orEmpty()) }
    LaunchedEffect(initialConfiguration?.components) {
        selectedComponents = initialConfiguration?.components.orEmpty()
    }
    val configuration = PcConfiguration(
        selectedComponents,
        initialConfiguration?.purpose ?: "Конструктор",
        initialConfiguration?.budget ?: Int.MAX_VALUE
    )
    val compatibility = CompatibilityChecker.check(configuration)
    val selectedByCategory = selectedComponents.associateBy { it.category }

        Page("Конструктор", showTopBar = false) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
            item {
                SectionTitle("Конструктор")
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(configuration.purpose, style = AppTextStyles.pageTitle)
                        Text("Выберите комплектующие для каждой категории.", color = Muted, style = AppTextStyles.secondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    newName = initialConfiguration?.purpose ?: "Конструктор"
                                    newBudget = initialConfiguration?.budget?.toString().orEmpty()
                                    editDialogVisible = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp)
                            ) { Text("Редактировать", style = AppTextStyles.buttonLabel) }
                            OutlinedButton(
                                onClick = { deleteDialogVisible = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp)
                            ) { Text("Удалить", style = AppTextStyles.buttonLabel) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("Стоимость", color = Muted, style = AppTextStyles.metadata); Text("${configuration.totalPrice} руб.", color = Accent, style = AppTextStyles.bold) }
                            Column { Text("Производительность", color = Muted, style = AppTextStyles.metadata); Text("${configuration.performanceScore()}/40", color = Accent, style = AppTextStyles.bold) }
                            Column { Text("Потребление", color = Muted, style = AppTextStyles.metadata); Text("${configuration.totalPowerWatts} Вт", color = Accent, style = AppTextStyles.bold) }
                        }
                    }
                }
            }
            item {
                CompatibilityInfoCard(compatibility)
            }
            item {
                Button(
                    onClick = onSaveConfiguration,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)
                ) {
                    Text("Сохранить сборку", style = AppTextStyles.bold)
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        selectedComponents = emptyList()
                        onResetConfiguration()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Сбросить сборку", style = AppTextStyles.buttonLabel)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Комплектующие", style = AppTextStyles.cardTitle)
                    ComponentCategory.entries.forEach { category ->
                        val component = selectedByCategory[category]
                        val status = component?.let {
                            componentStatus(category, selectedComponents, configuration.budget)
                        } ?: CompatibilityStatus.WARNINGS
                        BuildSlot(category, component, status) {
                            onOpenOverview(category)
                        }
                    }
                }
            }
        }
    }
    if (editDialogVisible) {
        AlertDialog(
            onDismissRequest = { editDialogVisible = false },
            containerColor = Panel,
            title = { Text("Редактировать сборку", style = AppTextStyles.pageTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        label = { Text("Название", style = AppTextStyles.metadata) }
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = newBudget,
                        onValueChange = { value ->
                            newBudget = value.filter(Char::isDigit)
                        },
                        singleLine = true,
                        label = { Text("Бюджет, руб.", style = AppTextStyles.metadata) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newName.trim()
                        val budget = newBudget.toIntOrNull()
                        if (name.isNotEmpty() && budget != null && budget > 0) {
                            onUpdateConfigurationDetails(name, budget)
                            editDialogVisible = false
                        }
                    }
                ) { Text("Сохранить", style = AppTextStyles.buttonLabel) }
            },
            dismissButton = {
                TextButton(onClick = { editDialogVisible = false }) { Text("Отмена", style = AppTextStyles.buttonLabel) }
            }
        )
    }
    if (deleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { deleteDialogVisible = false },
            containerColor = Panel,
            title = { Text("Удалить сборку?", style = AppTextStyles.pageTitle) },
            text = { Text("Сохранённая сборка будет удалена из списка.", style = AppTextStyles.secondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConfiguration()
                        deleteDialogVisible = false
                    }
                ) { Text("Удалить", color = AppColors.Incompatible, style = AppTextStyles.buttonLabel) }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogVisible = false }) { Text("Отмена", style = AppTextStyles.buttonLabel) }
            }
        )
    }
}

@Composable
internal fun CompatibilityInfoCard(result: com.example.pccomponent.domain.compatibility.CompatibilityResult) {
    val borderColor = statusColor(result.status)
    val messages = if (result.messages.isEmpty()) {
        listOf("Проблем с совместимостью нет.")
    } else {
        result.messages
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            borderColor.copy(alpha = 0.22f),
                            AppColors.Panel,
                            borderColor.copy(alpha = 0.10f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Совместимость",
                color = borderColor,
                style = AppTextStyles.bold
            )
            messages.forEach { message ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = borderColor, style = AppTextStyles.bodyBold)
                    Spacer(Modifier.width(8.dp))
                    Text(message, color = MaterialTheme.colorScheme.onBackground, style = AppTextStyles.body)
                }
            }
        }
    }
}

@Composable
internal fun BuildSlot(category: ComponentCategory, component: Component?, status: CompatibilityStatus, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(categoryTitle(category), color = Muted, style = AppTextStyles.medium)
                Text(component?.name ?: "Нажмите, чтобы выбрать", color = if (component == null) Muted else MaterialTheme.colorScheme.onBackground, style = AppTextStyles.body, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
            Text(if (component == null) "Выбрать" else "Изменить", color = Accent, style = AppTextStyles.semiBold)
        }
    }
}

internal fun componentStatus(
    category: ComponentCategory,
    selected: List<Component>,
    budget: Int
): CompatibilityStatus {
    if (selected.none { it.category == category }) return CompatibilityStatus.WARNINGS
    val result = CompatibilityChecker.check(PcConfiguration(selected, "Проверка", budget))
    if (result.status == CompatibilityStatus.COMPATIBLE) return CompatibilityStatus.COMPATIBLE

    val withoutThisCategory = selected.filterNot { it.category == category }
    val withoutResult = CompatibilityChecker.check(PcConfiguration(withoutThisCategory, "Проверка", budget))
    val existingIncompatibilities = withoutResult.incompatibilityMessages.toSet()
    val newIncompatibilities = result.incompatibilityMessages
        .filterNot { it in existingIncompatibilities }
    if (newIncompatibilities.isNotEmpty()) return CompatibilityStatus.INCOMPATIBLE

    // Warnings such as an exceeded budget describe the current build even when
    // removing one component does not remove the warning completely.
    val warningMessages = result.messages.filterNot { it in result.incompatibilityMessages }
    if (warningMessages.isNotEmpty()) return CompatibilityStatus.WARNINGS

    // A hard incompatibility belongs to another component pair, so this slot
    // itself remains compatible.
    return CompatibilityStatus.COMPATIBLE
}

internal fun statusColor(status: CompatibilityStatus): Color = when (status) {
    CompatibilityStatus.COMPATIBLE -> CompatibleColor
    CompatibilityStatus.WARNINGS -> WarningColor
    CompatibilityStatus.INCOMPATIBLE -> IncompatibleColor
}
