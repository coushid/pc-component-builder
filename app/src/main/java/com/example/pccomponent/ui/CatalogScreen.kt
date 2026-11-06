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
internal fun CatalogScreen(
    catalog: List<Component>,
    currentConfiguration: PcConfiguration?,
    onConfigurationChange: (PcConfiguration) -> Unit,
    onSaveConfiguration: () -> Unit,
    onOpenOverview: (ComponentCategory) -> Unit,
    onResetConfiguration: () -> Unit,
    onUpdateConfigurationDetails: (String, Int) -> Unit,
    onDeleteConfiguration: () -> Unit,
    initialMode: String = "Обзор"
) {
    var mode by remember { mutableStateOf(initialMode) }
    var selected by remember { mutableStateOf<ComponentCategory?>(null) }
    var compatibleOnly by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf<Component?>(null) }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var socketFilter by remember { mutableStateOf("") }
    var minCores by remember { mutableStateOf("") }
    var extraFilterOne by remember { mutableStateOf("") }
    var extraFilterTwo by remember { mutableStateOf("") }
    Page(title = if (mode == "Конструктор") "Конструктор" else "Каталог", showTopBar = false) {
        Column(Modifier.fillMaxSize()) {
            if (mode == "Конструктор") {
                BuildScreen(catalog, currentConfiguration, onConfigurationChange, onSaveConfiguration, { category ->
                    mode = "Обзор"
                    selected = category
                    onOpenOverview(category)
                }, onResetConfiguration, onUpdateConfigurationDetails, onDeleteConfiguration)
                return@Column
            }
            val filtered = catalog.filter { selected == null || it.category == selected }
                .filter { !compatibleOnly || isCompatibleWithSelection(it, currentConfiguration?.components.orEmpty()) }
                .filter { matchesFilters(it, selected, minPrice, maxPrice, socketFilter, minCores, extraFilterOne, extraFilterTwo) }
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { SectionTitle("Каталог") }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected == null, onClick = { selected = null; minPrice = ""; maxPrice = ""; socketFilter = ""; minCores = ""; extraFilterOne = ""; extraFilterTwo = "" }, label = { Text("Все", style = AppTextStyles.buttonLabel) })
                        ComponentCategory.entries.forEach { category -> FilterChip(selected == category, onClick = { selected = category; minPrice = ""; maxPrice = ""; socketFilter = ""; minCores = ""; extraFilterOne = ""; extraFilterTwo = "" }, label = { Text(categoryTitle(category), style = AppTextStyles.buttonLabel) }) }
                    }
                }
                item {
                    FilterFields(
                        components = catalog.filter { selected == null || it.category == selected },
                        minPrice = minPrice, onMinPrice = { minPrice = it },
                        maxPrice = maxPrice, onMaxPrice = { maxPrice = it },
                        socket = socketFilter, onSocket = { socketFilter = it },
                        minCores = minCores, onMinCores = { minCores = it },
                        compatibleOnly = compatibleOnly, onCompatibleOnly = { compatibleOnly = it },
                        category = selected, extraOne = extraFilterOne, onExtraOne = { extraFilterOne = it },
                        extraTwo = extraFilterTwo, onExtraTwo = { extraFilterTwo = it }
                    )
                }
                item {
                    Text(
                        text = if (selected == null) "В каталоге: ${filtered.size} комплектующих"
                        else "${categoryTitle(selected!!)}: ${filtered.size}",
                        color = Muted,
                        style = AppTextStyles.secondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "В этой категории пока нет комплектующих.",
                            color = Muted,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { component ->
                        ComponentCard(
                            component = component,
                            selected = currentConfiguration?.components?.any { it.id == component.id } == true,
                            compatible = isCompatibleWithSelection(component, currentConfiguration?.components.orEmpty()),
                            onDetails = { details = component },
                            onAdd = {
                                val updated = currentConfiguration?.components.orEmpty()
                                    .filterNot { it.category == component.category } + component
                                onConfigurationChange(
                                    PcConfiguration(
                                        updated,
                                        currentConfiguration?.purpose ?: "Конструктор",
                                        currentConfiguration?.budget ?: Int.MAX_VALUE
                                    )
                                )
                                mode = "Конструктор"
                            }
                        )
                    }
                }
            }
        }
    }
    details?.let { component ->
        AlertDialog(
            onDismissRequest = { details = null },
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
                        specs.supportedSockets.takeIf { it.isNotEmpty() }?.let { add("Поддерживаемые сокеты" to it.joinToString()) }
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
            confirmButton = { TextButton({ details = null }) { Text("Закрыть", style = AppTextStyles.buttonLabel) } }
        )
    }
}


internal fun matchesFilters(
    component: Component,
    category: ComponentCategory? = null,
    minPrice: String,
    maxPrice: String,
    socket: String,
    minCores: String,
    extraOne: String = "",
    extraTwo: String = ""
): Boolean {
    val specs = component.specifications
    val minPriceValue = minPrice.toIntOrNull()
    val maxPriceValue = maxPrice.toIntOrNull()
    val coresValue = minCores.toIntOrNull()
    return (minPriceValue == null || component.price >= minPriceValue) &&
        (maxPriceValue == null || component.price <= maxPriceValue) &&
        (socket.isBlank() || specs.socket?.contains(socket.trim(), ignoreCase = true) == true ||
            specs.supportedSockets.any { it.contains(socket.trim(), ignoreCase = true) }) &&
        (coresValue == null || (specs.cores ?: 0) >= coresValue) &&
        (extraOne.isBlank() || matchesExtra(component, category, extraOne)) &&
        (extraTwo.isBlank() || matchesExtra(component, category, extraTwo))
}

internal fun categoryFilterOptions(category: ComponentCategory?, components: List<Component>): Pair<Pair<String, List<String>>?, Pair<String, List<String>>?> {
    fun values(selector: (Component) -> String?): List<String> = components.mapNotNull(selector).distinct().sorted()
    fun intValues(selector: (Component) -> Int?): List<String> = components.mapNotNull(selector).distinct().sorted().map(Int::toString)
    return when (category) {
        ComponentCategory.GPU -> Pair("Видеопамять" to intValues { it.specifications.videoMemoryGb }.map { "$it ГБ" }, "Интерфейс" to values { it.specifications.interfaceType })
        ComponentCategory.RAM -> Pair("Объём памяти" to intValues { it.specifications.memoryCapacityGb }.map { "$it ГБ" }, "Частота памяти" to intValues { it.specifications.memoryFrequencyMhz }.map { "$it МГц" })
        ComponentCategory.STORAGE -> Pair("Ёмкость" to intValues { it.specifications.capacityGb }.map { "$it ГБ" }, "Интерфейс" to values { it.specifications.interfaceType })
        ComponentCategory.POWER_SUPPLY -> Pair("Мощность" to intValues { it.specifications.powerWatts }.map { "$it Вт" }, "Сертификат" to values { it.specifications.certificate })
        ComponentCategory.PC_CASE -> Pair("Форм-фактор" to values { it.specifications.formFactor }, "Макс. длина GPU" to intValues { it.specifications.maxGpuLengthMm }.map { "$it мм" })
        ComponentCategory.COOLER -> Pair("Макс. TDP" to intValues { it.specifications.maxTdpWatts }.map { "$it Вт" }, "Сокет" to components.flatMap { it.specifications.supportedSockets }.distinct().sorted())
        else -> Pair(null, null)
    }
}

internal fun matchesExtra(component: Component, category: ComponentCategory?, value: String): Boolean {
    val normalized = value.filter(Char::isDigit)
    val specs = component.specifications
    return when (category) {
        ComponentCategory.GPU -> value == "${specs.videoMemoryGb} ГБ" || value.equals(specs.interfaceType, true)
        ComponentCategory.RAM -> value == "${specs.memoryCapacityGb} ГБ" || value == "${specs.memoryFrequencyMhz} МГц"
        ComponentCategory.STORAGE -> value == "${specs.capacityGb} ГБ" || value.equals(specs.interfaceType, true)
        ComponentCategory.POWER_SUPPLY -> value == "${specs.powerWatts} Вт" || value.equals(specs.certificate, true)
        ComponentCategory.PC_CASE -> value.equals(specs.formFactor, true) || value == "${specs.maxGpuLengthMm} мм"
        ComponentCategory.COOLER -> value == "${specs.maxTdpWatts} Вт" || specs.supportedSockets.any { it == value }
        else -> true
    }
}

internal fun performanceTitle(level: Int): String = when (level) {
    1 -> "Базовый"
    2 -> "Начальный"
    3 -> "Средний"
    4 -> "Высокий"
    5 -> "Максимальный"
    else -> "Уровень $level"
}

