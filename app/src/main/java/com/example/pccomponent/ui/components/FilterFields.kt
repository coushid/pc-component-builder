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
internal fun FilterFields(
    components: List<Component>,
    minPrice: String, onMinPrice: (String) -> Unit,
    maxPrice: String, onMaxPrice: (String) -> Unit,
    socket: String, onSocket: (String) -> Unit,
    minCores: String, onMinCores: (String) -> Unit,
    compatibleOnly: Boolean = false, onCompatibleOnly: (Boolean) -> Unit = {},
    category: ComponentCategory? = null,
    extraOne: String = "", onExtraOne: (String) -> Unit = {},
    extraTwo: String = "", onExtraTwo: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf<String?>(null) }
    var filtersVisible by remember { mutableStateOf(false) }
    val sockets = components.flatMap { it.specifications.socket?.let(::listOf).orEmpty() + it.specifications.supportedSockets }.distinct().sorted()
    val maxPriceValue = components.maxOfOrNull { it.price } ?: 0
    val priceValues = buildList {
        addAll(listOf(1_000, 5_000, 10_000))
        var threshold = 15_000
        while (threshold <= maxPriceValue) {
            add(threshold)
            threshold += 5_000
        }
    }.distinct()
    val coreValues = components.mapNotNull { it.specifications.cores }.distinct().sorted()
    val extra = categoryFilterOptions(category, components)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = {
                    filtersVisible = !filtersVisible
                    if (!filtersVisible) expanded = null
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 14.dp,
                    vertical = 0.dp
                )
            ) {
                Text(if (filtersVisible) "Скрыть фильтры" else "Фильтры", style = AppTextStyles.buttonLabel)
            }
            TextButton(
                onClick = {
                    onMinPrice("")
                    onMaxPrice("")
                    onSocket("")
                    onMinCores("")
                    onCompatibleOnly(false)
                    onExtraOne("")
                    onExtraTwo("")
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 8.dp,
                    vertical = 0.dp
                )
            ) {
                Text("Сбросить", color = Accent, style = AppTextStyles.buttonLabel)
            }
        }
        if (filtersVisible) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownFilter("Цена от", minPrice.takeIf { it.isNotBlank() }?.let { "${it.toIntOrNull()?.formattedPrice()} руб." }.orEmpty(), priceValues.map { "${it.formattedPrice()} руб." }, Modifier.weight(1f), expanded == "minPrice", { expanded = if (expanded == "minPrice") null else "minPrice" }, { onMinPrice(it.filter(Char::isDigit)); expanded = null })
                DropdownFilter("Цена до", maxPrice.takeIf { it.isNotBlank() }?.let { "${it.toIntOrNull()?.formattedPrice()} руб." }.orEmpty(), priceValues.map { "${it.formattedPrice()} руб." }, Modifier.weight(1f), expanded == "maxPrice", { expanded = if (expanded == "maxPrice") null else "maxPrice" }, { onMaxPrice(it.filter(Char::isDigit)); expanded = null })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sockets.isNotEmpty()) DropdownFilter("Сокет", socket, sockets, Modifier.weight(1f), expanded == "socket", { expanded = if (expanded == "socket") null else "socket" }, { onSocket(it); expanded = null })
                if (coreValues.isNotEmpty()) DropdownFilter("Ядер", minCores, coreValues.map(Int::toString), Modifier.weight(1f), expanded == "cores", { expanded = if (expanded == "cores") null else "cores" }, { onMinCores(it); expanded = null })
                extra.first?.let { (label, options) -> DropdownFilter(label, extraOne, options, Modifier.weight(1f), expanded == "extraOne", { expanded = if (expanded == "extraOne") null else "extraOne" }, { onExtraOne(it); expanded = null }) }
            }
            extra.second?.let { (label, options) ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownFilter(label, extraTwo, options, Modifier.fillMaxWidth(), expanded == "extraTwo", { expanded = if (expanded == "extraTwo") null else "extraTwo" }, { onExtraTwo(it); expanded = null })
                }
            }
            OutlinedButton(
                onClick = { onCompatibleOnly(!compatibleOnly) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, if (compatibleOnly) Accent else Muted),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (compatibleOnly) Accent.copy(alpha = .12f) else Color.Transparent,
                    contentColor = if (compatibleOnly) Accent else MaterialTheme.colorScheme.onBackground
                )
            ) {
                Text(if (compatibleOnly) "Совместимо: включено" else "Совместимо", style = AppTextStyles.buttonLabel)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun DropdownFilter(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onSelected: (String) -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { onExpandedChange() }, modifier = modifier) {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = AppTextStyles.metadata) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = onExpandedChange) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text("Нет доступных значений", style = AppTextStyles.body) }, onClick = onExpandedChange)
            } else {
                DropdownMenuItem(text = { Text("Не выбрано", style = AppTextStyles.body) }, onClick = { onSelected("") })
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option, style = AppTextStyles.body) }, onClick = { onSelected(option) })
                }
            }
        }
    }
}

