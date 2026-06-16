package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NeonCyan,
    secondary = ElectricPurple,
    tertiary = CyanAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = ObsidianDark,
    onSecondary = ObsidianDark,
    onBackground = Color(0xFFECEFF1),
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = CyberCard,
    onSurfaceVariant = Color(0xFFB0BEC5),
    error = CrimsonAlert
  )

private val LightColorScheme = DarkColorScheme // Always maintain dark theme for high-tech gaming cloud dashboard!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for gaming virtual space
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our beautiful custom cyber theme
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
