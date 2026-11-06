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
internal fun HomeScreen(nav: NavHostController) {
    Page("Чат бот", largeTitle = true, showTopBar = false) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            CompatibilityIllustration()
            Spacer(Modifier.height(22.dp))
            Text(
                "Подберём комплектующие для вашего ПК",
                style = AppTextStyles.displayTitle
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Ответьте на несколько вопросов - мы подберём совместимые детали под ваши задачи и бюджет.",
                color = Muted
            )
            Spacer(Modifier.height(14.dp))
            PrimaryAction("Начать подбор", "Ответьте на несколько вопросов", { nav.navigate("chat") })
        }
    }
}

@Composable
internal fun CompatibilityIllustration() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(172.dp)
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.pc_builder_logo),
            contentDescription = "Логотип PC Builder",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Accent),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun PrimaryAction(title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Accent), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Bg, style = AppTextStyles.actionTitleExtraBold)
                Text(subtitle, color = Bg.copy(alpha = .86f), style = AppTextStyles.secondary, modifier = Modifier.padding(top = 4.dp))
            }
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Перейти",
                tint = Bg,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
internal fun MenuCard(title: String, subtitle: String, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, contentDescription = null, tint = Accent, modifier = Modifier.size(24.dp).padding(end = 2.dp)) }
            if (icon != null) Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, style = AppTextStyles.semiBold); Text(subtitle, color = Muted, style = AppTextStyles.secondary, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
}

@Composable
internal fun SmallCard(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) { Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Panel), modifier = modifier, shape = RoundedCornerShape(14.dp)) { Text(title, modifier = Modifier.padding(16.dp), style = AppTextStyles.semiBold) } }

