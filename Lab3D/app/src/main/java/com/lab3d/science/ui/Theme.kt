package com.lab3d.science.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object LabColors {
    val Bg = Color(0xFF0A0F24)
    val Bg2 = Color(0xFF141D40)
    val Card = Color(0xFF162044)
    val CardBorder = Color(0x28FFFFFF)
    val Sheet = Color(0xFF121A38)
    val Cyan = Color(0xFF38F2FF)
    val Pink = Color(0xFFFF4D9D)
    val Gold = Color(0xFFFFD84F)
    val Green = Color(0xFF4ADE80)
    val Text = Color(0xFFF6F8FF)
    val Text2 = Color(0xFFA8B4D8)

    val bgBrush = Brush.verticalGradient(listOf(Bg2, Bg, Color(0xFF070B1A)))
}

@Composable
fun LabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LabColors.Cyan,
            secondary = LabColors.Pink,
            background = LabColors.Bg,
            surface = LabColors.Sheet,
            onPrimary = Color(0xFF081020),
            onBackground = LabColors.Text,
            onSurface = LabColors.Text,
        ),
        content = content,
    )
}
