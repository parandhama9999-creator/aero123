package com.aistudio.aerofast.trmst.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AeroFastTheme(
    themeMode: AppThemeMode = AppThemeMode.INDUSTRIAL_LIGHT,
    fontOption: AppFontOption = AppFontOption.ROBOTO_MONO,
    content: @Composable () -> Unit
) {
    val customColors = getCustomColors(themeMode)

    val activeFontFamily = remember(fontOption) {
        if (fontOption.fontResId != null) {
            FontFamily(Font(fontOption.fontResId, FontWeight.Normal))
        } else {
            when (fontOption) {
                AppFontOption.SYSTEM_MONO -> FontFamily.Monospace
                AppFontOption.SYSTEM_SERIF -> FontFamily.Serif
                else -> FontFamily.SansSerif
            }
        }
    }

    val dynamicTypography = remember(activeFontFamily) {
        createTypography(activeFontFamily)
    }

    val colorScheme: ColorScheme = if (customColors.isDark) {
        darkColorScheme(
            primary = customColors.accentPrimary,
            secondary = customColors.accentSecondary,
            background = customColors.bgMain,
            surface = customColors.bgCard,
            surfaceVariant = customColors.bgElevated,
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onBackground = customColors.textMain,
            onSurface = customColors.textMain,
            outline = customColors.border
        )
    } else {
        lightColorScheme(
            primary = customColors.accentPrimary,
            secondary = customColors.accentSecondary,
            background = customColors.bgMain,
            surface = customColors.bgCard,
            surfaceVariant = customColors.bgElevated,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = customColors.textMain,
            onSurface = customColors.textMain,
            outline = customColors.border
        )
    }

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = dynamicTypography,
            content = content
        )
    }
}
