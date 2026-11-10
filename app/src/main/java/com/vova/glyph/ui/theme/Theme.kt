package com.vova.glyph.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val GlyphBlack: Color get() = ThemeController.palette.background
val GlyphSurface: Color get() = ThemeController.palette.surface
val GlyphSurfaceHigh: Color get() = ThemeController.palette.surfaceHigh
val GlyphWhite: Color get() = ThemeController.palette.text
val GlyphGray: Color get() = ThemeController.palette.gray
val GlyphDim: Color get() = ThemeController.palette.dim
val GlyphLine: Color get() = ThemeController.palette.line
val GlyphRed: Color get() = ThemeController.palette.accent

val DotoFamily: FontFamily get() = FontController.brand.family
val PixelFamily: FontFamily get() = FontController.display.family
val GroteskFamily: FontFamily get() = FontController.body.family

private fun buildTypography(display: FontFamily, body: FontFamily): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Bold,
        fontSize = 29.sp,
        lineHeight = 34.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = body,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = body,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = body,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = body,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.2.sp,
    ),
)

@Composable
fun GlyphTheme(content: @Composable () -> Unit) {
    val palette = ThemeController.palette
    val displayFont = FontController.display
    val bodyFont = FontController.body
    val typography = remember(displayFont, bodyFont) {
        buildTypography(displayFont.family, bodyFont.family)
    }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = palette.text,
            onPrimary = palette.background,
            primaryContainer = palette.surfaceHigh,
            onPrimaryContainer = palette.text,
            secondary = palette.accent,
            onSecondary = palette.text,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceHigh,
            onSurfaceVariant = palette.gray,
            surfaceDim = palette.background,
            surfaceBright = palette.surfaceHigh,
            surfaceContainerLowest = palette.background,
            surfaceContainerLow = palette.surface,
            surfaceContainer = palette.surface,
            surfaceContainerHigh = palette.surfaceHigh,
            surfaceContainerHighest = palette.surfaceHigh,
            inverseSurface = palette.text,
            inverseOnSurface = palette.background,
            inversePrimary = palette.accent,
            secondaryContainer = palette.surfaceHigh,
            onSecondaryContainer = palette.text,
            tertiary = palette.accent,
            onTertiary = palette.background,
            tertiaryContainer = palette.surfaceHigh,
            onTertiaryContainer = palette.text,
            scrim = palette.background,
            outline = palette.line,
            outlineVariant = palette.line,
            error = palette.accent,
            onError = palette.text,
        ),
        typography = typography,
        content = content,
    )
}
