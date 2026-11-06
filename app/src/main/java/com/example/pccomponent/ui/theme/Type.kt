package com.example.pccomponent.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** UI font for all readable interface text. */
private val AppFontFamily = FontFamily.SansSerif

/** Decorative display font, deliberately restricted to short headings and selected metrics. */
private val AppDisplayFontFamily = FontFamily(
    androidx.compose.ui.text.font.Font(
        resId = com.example.pccomponent.R.font.russo_one_regular,
        weight = FontWeight.Normal
    )
)

private fun TextStyle.withUiFont(): TextStyle = copy(fontFamily = AppFontFamily)

val AppTypography: Typography = Typography().let { base ->
    Typography(
        displayLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = 0.sp),
        displayMedium = base.displayMedium.withUiFont(), displaySmall = base.displaySmall.withUiFont(),
        headlineLarge = base.headlineLarge.withUiFont(),
        headlineMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = 0.sp),
        headlineSmall = base.headlineSmall.withUiFont(),
        titleLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
        titleMedium = base.titleMedium.withUiFont(), titleSmall = base.titleSmall.withUiFont(),
        bodyLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
        bodyMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
        bodySmall = base.bodySmall.withUiFont(),
        labelLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
        labelMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
        labelSmall = base.labelSmall.withUiFont()
    )
}

private val Bold = FontWeight.Bold
private val SemiBold = FontWeight.SemiBold
private val Medium = FontWeight.Medium
private val ExtraBold = FontWeight.ExtraBold
private fun TextStyle.withWeight(weight: FontWeight): TextStyle = copy(fontWeight = weight)

object AppTextStyles {
    val normal = TextStyle(fontFamily = AppFontFamily)
    val bold = TextStyle(fontFamily = AppFontFamily, fontWeight = Bold)
    val semiBold = TextStyle(fontFamily = AppFontFamily, fontWeight = SemiBold)
    val medium = TextStyle(fontFamily = AppFontFamily, fontWeight = Medium)
    val extraBold = TextStyle(fontFamily = AppFontFamily, fontWeight = ExtraBold)
    fun bold(base: TextStyle): TextStyle = base.withWeight(Bold)
    fun semiBold(base: TextStyle): TextStyle = base.withWeight(SemiBold)
    fun medium(base: TextStyle): TextStyle = base.withWeight(Medium)
    fun extraBold(base: TextStyle): TextStyle = base.withWeight(ExtraBold)

    val screenTitle = TextStyle(fontFamily = AppFontFamily, fontWeight = Bold, fontSize = 24.sp, lineHeight = 30.sp)
    val displayTitle = TextStyle(fontFamily = AppDisplayFontFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 30.sp)
    val pageTitle = TextStyle(fontFamily = AppFontFamily, fontWeight = Bold, fontSize = 20.sp, lineHeight = 26.sp)
    val sectionTitle = TextStyle(fontFamily = AppFontFamily, fontWeight = Bold, fontSize = 24.sp, lineHeight = 30.sp)
    val cardTitle = TextStyle(fontFamily = AppFontFamily, fontWeight = SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
    val buttonLabel = TextStyle(fontFamily = AppFontFamily, fontWeight = SemiBold, fontSize = 14.sp, lineHeight = 18.sp)
    val navigationLabel = TextStyle(fontFamily = AppFontFamily, fontSize = 12.sp, lineHeight = 16.sp)
    val metadata = TextStyle(fontFamily = AppFontFamily, fontSize = 12.sp, lineHeight = 16.sp)
    val secondary = TextStyle(fontFamily = AppFontFamily, fontSize = 13.sp, lineHeight = 18.sp)
    val body = TextStyle(fontFamily = AppFontFamily, fontSize = 14.sp, lineHeight = 20.sp)
    val bodyBold = body.copy(fontWeight = Bold)
    val metricValueBold = TextStyle(fontFamily = AppFontFamily, fontWeight = Bold, fontSize = 18.sp, lineHeight = 22.sp)
    val metricValueDisplay = TextStyle(fontFamily = AppDisplayFontFamily, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 22.sp)
    val metricValue = TextStyle(fontFamily = AppFontFamily, fontSize = 18.sp, lineHeight = 22.sp)
    val actionTitleExtraBold = TextStyle(fontFamily = AppFontFamily, fontWeight = ExtraBold, fontSize = 20.sp, lineHeight = 26.sp)
    val actionTitle = TextStyle(fontFamily = AppFontFamily, fontSize = 20.sp, lineHeight = 26.sp)
    val actionArrowBold = TextStyle(fontFamily = AppFontFamily, fontWeight = Bold, fontSize = 28.sp, lineHeight = 32.sp)
    val actionArrow = TextStyle(fontFamily = AppFontFamily, fontSize = 28.sp, lineHeight = 32.sp)
}

