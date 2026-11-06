package com.example.pccomponent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier = modifier, style = AppTextStyles.screenTitle)

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier = modifier, style = AppTextStyles.sectionTitle)

@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onBackground) =
    Text(text, modifier = modifier, style = AppTextStyles.body, color = color)

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier = modifier, style = AppTextStyles.secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun MetaText(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) =
    Text(text, modifier = modifier, style = AppTextStyles.metadata, color = color)

@Composable
fun MetricLabel(text: String, modifier: Modifier = Modifier) =
    MetaText(text, modifier)

@Composable
fun MetricValue(text: String, modifier: Modifier = Modifier, display: Boolean = false) =
    Text(text, modifier = modifier, style = if (display) AppTextStyles.metricValueDisplay else AppTextStyles.metricValue)

@Composable
fun StatusText(text: String, color: Color, modifier: Modifier = Modifier) =
    Text(text, modifier = modifier, style = AppTextStyles.bodyBold, color = color)

@Composable
fun ActionLabel(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier = modifier, style = AppTextStyles.buttonLabel)
