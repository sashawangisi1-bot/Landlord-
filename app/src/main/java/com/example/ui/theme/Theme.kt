package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val HighDensityColorScheme = lightColorScheme(
  primary = HighDensityPrimary,
  primaryContainer = HighDensityPrimaryContainer,
  secondary = HighDensitySecondary,
  secondaryContainer = HighDensitySecondaryContainer,
  background = HighDensityBg,
  surface = HighDensityBg,
  surfaceVariant = HighDensitySurfaceVariant,
  onPrimary = Color.White,
  onSecondary = Color.White,
  onBackground = HighDensityText,
  onSurface = HighDensityText,
  onSurfaceVariant = HighDensitySecondaryText,
  outline = HighDensityOutline,
  error = HighDensityError
)

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme = HighDensityColorScheme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> HighDensityColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
