package com.malik.indiancourtdiary

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val CourtGold = Color(0xFFD7B56D)
val CourtNavy = Color(0xFF07111F)
val CourtSurface = Color(0xFF101D2D)
val CourtSurfaceHigh = Color(0xFF18283B)
val CourtText = Color(0xFFF2F4F7)
val CourtMuted = Color(0xFFAAB6C5)

private val PremiumColors = darkColorScheme(
    primary = CourtGold,
    onPrimary = Color(0xFF241A05),
    primaryContainer = Color(0xFF3A2D12),
    onPrimaryContainer = Color(0xFFFFE7AE),
    secondary = Color(0xFF87B7D8),
    background = CourtNavy,
    onBackground = CourtText,
    surface = CourtSurface,
    onSurface = CourtText,
    surfaceVariant = CourtSurfaceHigh,
    onSurfaceVariant = CourtMuted,
    outline = Color(0xFF41536A),
    error = Color(0xFFFFB4AB)
)

private val PremiumShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun CourtPremiumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PremiumColors,
        shapes = PremiumShapes,
        content = content
    )
}
