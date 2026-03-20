package com.example.studywise.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.studywise.R

// Local Font Family definition using the provided Visby Round files
val visbyRoundFontFamily = FontFamily(
    Font(R.font.visby_round_regular, FontWeight.Normal),
    Font(R.font.visby_round_medium, FontWeight.Medium),
    Font(R.font.visby_round_demibold, FontWeight.SemiBold),
    Font(R.font.visby_round_bold, FontWeight.Bold)
)

// Default Material 3 typography values
val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = visbyRoundFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = visbyRoundFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = visbyRoundFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = visbyRoundFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = visbyRoundFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = visbyRoundFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = visbyRoundFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = visbyRoundFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = visbyRoundFontFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = visbyRoundFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = visbyRoundFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = visbyRoundFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = visbyRoundFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = visbyRoundFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = visbyRoundFontFamily),
)
