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

internal fun ComponentCard(
    component: Component,
    selected: Boolean = false,
    compatible: Boolean = true,
    onDetails: () -> Unit = {},
    onAdd: (() -> Unit)? = null,
    showPriceAsAction: Boolean = false
) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(component.name, style = AppTextStyles.semiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${component.brand} · ${categoryTitle(component.category)}", color = Muted, style = AppTextStyles.metadata)
                }
                if (!showPriceAsAction) {
                    Text("${component.price} руб.", color = Accent, style = AppTextStyles.bold)
                }
            }
            Text(component.description, color = MaterialTheme.colorScheme.onBackground, style = AppTextStyles.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.weight(1f).height(42.dp)) {
                    OutlinedButton(
                        onClick = onDetails,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) { Text("Детали", style = AppTextStyles.buttonLabel) }
                }
                onAdd?.let {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(42.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        AppColors.AccentSoft,
                                        Accent
                                    )
                                ),
                                shape = RoundedCornerShape(50)
                            )
                    ) {
                        Button(
                            onClick = it,
                            enabled = !selected,
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Black,
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = Color.Black
                            )
                        ) {
                            Text(if (selected) "В сборке" else "Добавить", style = AppTextStyles.buttonLabel)
                        }
                    }
                }
                if (showPriceAsAction) {
                    Box(
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${component.price} руб.", color = Accent, style = AppTextStyles.bold)
                    }
                }
            }
            if (!compatible) Text("Несовместимо с текущей сборкой", color = AppColors.Incompatible, style = AppTextStyles.metadata)
        }
    }
}

@Composable
internal fun ComponentDetailsDialog(component: Component, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text(component.name, style = AppTextStyles.pageTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${component.brand} · ${categoryTitle(component.category)}", color = Muted, style = AppTextStyles.metadata)
                Text(component.description, style = AppTextStyles.secondary)
                Text("${component.price} руб.", color = Accent, style = AppTextStyles.bold)
                val specs = component.specifications
                val characteristics = buildList {
                    add("Потребление" to "${component.powerConsumptionW} Вт")
                    specs.socket?.let { add("Сокет" to it) }
                    specs.chipset?.let { add("Чипсет" to it) }
                    specs.formFactor?.let { add("Форм-фактор" to it) }
                    specs.memoryType?.let { add("Тип памяти" to it) }
                    specs.memoryCapacityGb?.let { add("Объём памяти" to "$it ГБ") }
                    specs.memoryFrequencyMhz?.let { add("Частота памяти" to "$it МГц") }
                    specs.cores?.let { add("Количество ядер" to it.toString()) }
                    specs.threads?.let { add("Потоки" to it.toString()) }
                    specs.videoMemoryGb?.let { add("Видеопамять" to "$it ГБ") }
                    specs.lengthMm?.let { add("Длина" to "$it мм") }
                    specs.interfaceType?.let { add("Интерфейс" to it) }
                    specs.capacityGb?.let { add("Ёмкость" to "$it ГБ") }
                    specs.powerWatts?.let { add("Мощность блока питания" to "$it Вт") }
                    specs.certificate?.let { add("Сертификат" to it) }
                    specs.supportedSockets.takeIf { it.isNotEmpty() }
                        ?.let { add("Поддерживаемые сокеты" to it.joinToString()) }
                    specs.maxTdpWatts?.let { add("Максимальный TDP" to "$it Вт") }
                    specs.maxGpuLengthMm?.let { add("Максимальная длина видеокарты" to "$it мм") }
                    specs.maxCoolerHeightMm?.let { add("Максимальная высота кулера" to "$it мм") }
                }
                if (characteristics.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Характеристики", style = AppTextStyles.semiBold)
                    characteristics.forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = Muted, style = AppTextStyles.metadata, modifier = Modifier.weight(1f))
                            Text(value, style = AppTextStyles.body, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть", style = AppTextStyles.buttonLabel) } }
    )
}

