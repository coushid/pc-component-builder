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
internal fun ResultScreen(
    configuration: PcConfiguration?,
    recommendation: BuildRecommendationResult?,
    nav: NavHostController,
    onSave: () -> Unit
) {
    var details by remember { mutableStateOf<Component?>(null) }

    Page("Ваша сборка", showTopBar = false) {
        if (configuration == null) { Text("Конфигурация не создана", style = AppTextStyles.body, modifier = Modifier.padding(20.dp)); return@Page }
        if (configuration.components.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Ваша сборка")
                Text("Совместимая сборка не найдена", style = AppTextStyles.sectionTitle)
                if (recommendation?.status == RecommendationStatus.BUDGET_TOO_LOW) {
                    Text(
                        "Полная совместимая сборка начинается примерно от ${recommendation.minimumRequiredBudget} руб. " +
                            "при текущих требованиях.",
                        color = Muted,
                        style = AppTextStyles.secondary
                    )
                } else {
                    Text("Для текущих требований совместимая конфигурация не найдена.", color = Muted, style = AppTextStyles.secondary)
                }
                Text("Попробуйте изменить бюджет или требования к производительности, памяти и накопителю.", color = Muted, style = AppTextStyles.secondary)
                OutlinedButton(onClick = { nav.popBackStack() }, shape = RoundedCornerShape(28.dp)) {
                    Text("Изменить ответы", style = AppTextStyles.buttonLabel)
                }
            }
            return@Page
        }
        val check = CompatibilityChecker.check(configuration)
        LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionTitle("Ваша сборка")
                Text("Бюджет: ${configuration.budget} руб.", color = Muted, style = AppTextStyles.secondary)
            }
            if (recommendation?.appliedPerformanceLevel != null &&
                recommendation.requestedPerformanceLevel != null &&
                recommendation.appliedPerformanceLevel < recommendation.requestedPerformanceLevel
            ) {
                item {
                    Text(
                        "Сборка подобрана с уровнем производительности ${recommendation.appliedPerformanceLevel} " +
                            "вместо выбранного ${recommendation.requestedPerformanceLevel}: более производительной полной конфигурации нет в каталоге.",
                        color = Muted,
                        style = AppTextStyles.secondary
                    )
                }
            }
            item { SummaryCard(configuration, check.status) }
            items(configuration.components) {
                ComponentCard(
                    component = it,
                    showPriceAsAction = true,
                    onDetails = { details = it }
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { nav.popBackStack() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) { Text("Изменить", style = AppTextStyles.buttonLabel) }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)
                    ) { Text("Сохранить", style = AppTextStyles.bold) }
                }
            }
        }
    }
    details?.let { component ->
        ComponentDetailsDialog(component = component, onDismiss = { details = null })
    }
}

@Composable
internal fun SummaryCard(configuration: PcConfiguration, status: CompatibilityStatus) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel2), shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                MetricLabel("Стоимость")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetricValue(configuration.totalPrice.toString(), display = true)
                    MetricValue(" руб.")
                }
            }
            Column {
                MetricLabel("Потребление")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetricValue(configuration.totalPowerWatts.toString(), display = true)
                    MetricValue(" Вт")
                }
            }
            Column {
                Text("Статус", color = Muted, style = AppTextStyles.metadata)
                Text(
                    statusTitle(status),
                    color = statusColor(status),
                    style = AppTextStyles.bodyBold
                )
            }
        }
    }
}

