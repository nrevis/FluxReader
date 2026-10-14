package io.fluxreader.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.ReaderThemeMode
import io.fluxreader.app.data.useWastelandDesign
import io.fluxreader.app.data.useCyberpunkDesign
import io.fluxreader.app.data.activeUiTheme
import io.fluxreader.app.data.ThemeAppearance

private val FluxShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val WastelandShapes = Shapes(
    extraSmall = CutCornerShape(topStart = 2.dp, topEnd = 1.dp, bottomEnd = 2.dp, bottomStart = 1.dp),
    small = CutCornerShape(topStart = 3.dp, topEnd = 1.dp, bottomEnd = 3.dp, bottomStart = 1.dp),
    medium = CutCornerShape(topStart = 4.dp, topEnd = 2.dp, bottomEnd = 4.dp, bottomStart = 2.dp),
    large = CutCornerShape(topStart = 6.dp, topEnd = 2.dp, bottomEnd = 6.dp, bottomStart = 2.dp),
    extraLarge = CutCornerShape(topStart = 8.dp, topEnd = 3.dp, bottomEnd = 8.dp, bottomStart = 3.dp),
)

private val CyberpunkShapes = Shapes(
    extraSmall = CutCornerShape(topStart = 0.dp, topEnd = 6.dp, bottomEnd = 0.dp, bottomStart = 3.dp),
    small = CutCornerShape(topStart = 0.dp, topEnd = 8.dp, bottomEnd = 0.dp, bottomStart = 4.dp),
    medium = CutCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomEnd = 0.dp, bottomStart = 6.dp),
    large = CutCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 8.dp),
    extraLarge = CutCornerShape(topStart = 0.dp, topEnd = 20.dp, bottomEnd = 0.dp, bottomStart = 10.dp),
)

private val DefaultTypography = Typography()

private val FluxTypography = Typography(
    titleLarge = DefaultTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = DefaultTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = DefaultTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = DefaultTypography.labelLarge.copy(fontWeight = FontWeight.Medium),
)

private val MonochromeTypography = Typography(
    titleLarge = DefaultTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = DefaultTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = DefaultTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = DefaultTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
    labelMedium = DefaultTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = DefaultTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
)

private val WastelandTypography = Typography(
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = FontFamily.Monospace, letterSpacing = 0.15.sp),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 0.12.sp),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 0.10.sp),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 0.55.sp),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, letterSpacing = 0.45.sp),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, letterSpacing = 0.35.sp),
)

private val CyberpunkTypography = Typography(
    titleLarge = DefaultTypography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
    titleMedium = DefaultTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.45.sp),
    titleSmall = DefaultTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.35.sp),
    labelLarge = DefaultTypography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.45.sp),
    labelMedium = DefaultTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.35.sp),
    labelSmall = DefaultTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
)

@Composable
fun FluxTheme(settings: ReaderSettings, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val scheme = readerColorScheme(settings, systemDark)
    val view = LocalView.current
    val lightBars = isLightBackground(scheme.background)
    SideEffect {
        view.context.findActivity()?.let { activity ->
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }
    }
    val typography = when {
        settings.useWastelandDesign -> WastelandTypography
        settings.useCyberpunkDesign -> CyberpunkTypography
        settings.theme.isMonochrome() -> MonochromeTypography
        else -> FluxTypography
    }
    val shapes = when {
        settings.useWastelandDesign -> WastelandShapes
        settings.useCyberpunkDesign -> CyberpunkShapes
        else -> FluxShapes
    }
    CompositionLocalProvider(
        LocalFluxThemeMode provides settings.activeUiTheme,
        LocalExperimentalColors provides experimentalColors(settings, scheme),
    ) {
        MaterialTheme(colorScheme = scheme, shapes = shapes, typography = typography, content = content)
    }
}

fun readerColorScheme(settings: ReaderSettings, systemDark: Boolean = true): ColorScheme {
    // Both experimental palettes are entirely inactive until the user opts in.
    // All other palettes retain their ordinary color behavior.
    // Palette selection changes only pigment, not shell shape, icons, or insets.
    val activeTheme = if (settings.experimentalDesignEnabled) {
        settings.experimentalPalette ?: settings.activeUiTheme
    } else settings.activeUiTheme
    val dark = when (settings.themeAppearance) {
        ThemeAppearance.SYSTEM -> systemDark
        ThemeAppearance.LIGHT -> false
        ThemeAppearance.DARK -> true
    }
    if (activeTheme == ReaderThemeMode.CUSTOM) return customScheme(settings)
    if (activeTheme == ReaderThemeMode.WASTELAND_TERMINAL || activeTheme == ReaderThemeMode.CYBERPUNK_NEON) {
        return darkScheme(activeTheme)
    }
    return if (dark) darkScheme(activeTheme) else lightScheme(activeTheme)
}

private fun darkScheme(theme: ReaderThemeMode): ColorScheme = when (theme) {
    ReaderThemeMode.OLED -> darkColorScheme(
        primary = Color(0xFFB9C6FF), secondary = Color(0xFFC0C6DC),
        background = Color.Black, surface = Color(0xFF0A0A0D), surfaceVariant = Color(0xFF17171C),
        onBackground = Color(0xFFF1F1F4), onSurface = Color(0xFFF1F1F4),
    )
    ReaderThemeMode.DARK -> darkColorScheme(
        primary = Color(0xFFB9C6FF), secondary = Color(0xFFC0C6DC),
        background = Color(0xFF111318), surface = Color(0xFF17191F), surfaceVariant = Color(0xFF23262D),
        onBackground = Color(0xFFE9EAF0), onSurface = Color(0xFFE9EAF0),
    )
    ReaderThemeMode.PAPER -> darkColorScheme(
        primary = Color(0xFFD3B683), secondary = Color(0xFFB9A37C),
        background = Color(0xFF191714), surface = Color(0xFF211E1A), surfaceVariant = Color(0xFF302B25),
        onBackground = Color(0xFFEDE4D5), onSurface = Color(0xFFEDE4D5),
    )
    ReaderThemeMode.SEPIA -> darkColorScheme(
        primary = Color(0xFFD2A86B), secondary = Color(0xFFC49661),
        background = Color(0xFF211A14), surface = Color(0xFF2A2119), surfaceVariant = Color(0xFF3B2E22),
        onBackground = Color(0xFFF0DFC4), onSurface = Color(0xFFF0DFC4),
    )
    ReaderThemeMode.NORD -> darkColorScheme(
        primary = Color(0xFF88C0D0), secondary = Color(0xFFA3BE8C), background = Color(0xFF2E3440),
        surface = Color(0xFF3B4252), surfaceVariant = Color(0xFF434C5E), onBackground = Color(0xFFECEFF4), onSurface = Color(0xFFECEFF4),
    )
    ReaderThemeMode.DRACULA -> darkColorScheme(
        primary = Color(0xFFBD93F9), secondary = Color(0xFF50FA7B), background = Color(0xFF191A21),
        surface = Color(0xFF282A36), surfaceVariant = Color(0xFF343746), onBackground = Color(0xFFF8F8F2), onSurface = Color(0xFFF8F8F2),
    )
    ReaderThemeMode.SOLARIZED_DARK -> darkColorScheme(
        primary = Color(0xFF268BD2), secondary = Color(0xFF2AA198), background = Color(0xFF002B36),
        surface = Color(0xFF073642), surfaceVariant = Color(0xFF174A55), onBackground = Color(0xFFEEE8D5), onSurface = Color(0xFFEEE8D5),
    )
    ReaderThemeMode.CATPPUCCIN -> darkColorScheme(
        primary = Color(0xFFCBA6F7), secondary = Color(0xFF89B4FA), background = Color(0xFF11111B),
        surface = Color(0xFF181825), surfaceVariant = Color(0xFF313244), onBackground = Color(0xFFCDD6F4), onSurface = Color(0xFFCDD6F4),
    )
    ReaderThemeMode.GRUVBOX -> darkColorScheme(
        primary = Color(0xFFFABD2F), secondary = Color(0xFFB8BB26), background = Color(0xFF1D2021),
        surface = Color(0xFF282828), surfaceVariant = Color(0xFF3C3836), onBackground = Color(0xFFEBDBB2), onSurface = Color(0xFFEBDBB2),
    )
    ReaderThemeMode.ROSE_PINE_MOON -> darkColorScheme(
        primary = Color(0xFFC4A7E7), secondary = Color(0xFF9CCFD8),
        background = Color(0xFF232136), surface = Color(0xFF2A273F), surfaceVariant = Color(0xFF393552),
        onBackground = Color(0xFFE0DEF4), onSurface = Color(0xFFE0DEF4),
    )
    ReaderThemeMode.CMX_PURPLE -> darkColorScheme(
        primary = Color(0xFFC4A7E7), secondary = Color(0xFF9CCFD8),
        background = Color(0xFF111019), surface = Color(0xFF191724), surfaceVariant = Color(0xFF26233A),
        onBackground = Color(0xFFE0DEF4), onSurface = Color(0xFFE0DEF4),
    )
    ReaderThemeMode.WASTELAND_TERMINAL -> darkColorScheme(
        primary = Color(0xFFFFB229), onPrimary = Color(0xFF211400),
        primaryContainer = Color(0xFF51330C), onPrimaryContainer = Color(0xFFFFDEA0),
        secondary = Color(0xFFB8DB7B), onSecondary = Color(0xFF0B1805),
        secondaryContainer = Color(0xFF27351A), onSecondaryContainer = Color(0xFFDDF6B2),
        tertiary = Color(0xFFC76428), onTertiary = Color(0xFF240900),
        tertiaryContainer = Color(0xFF4D2412), onTertiaryContainer = Color(0xFFFFC7A7),
        background = Color(0xFF030904), onBackground = Color(0xFFD3EDAE),
        surface = Color(0xFF10140B), onSurface = Color(0xFFE0EBC7),
        surfaceVariant = Color(0xFF272417), onSurfaceVariant = Color(0xFFB6B78B),
        outline = Color(0xFF8C6C32), outlineVariant = Color(0xFF4D4728),
    )
    ReaderThemeMode.CYBERPUNK_NEON -> darkColorScheme(
        primary = Color(0xFF18EAFF), onPrimary = Color(0xFF001418),
        primaryContainer = Color(0xFF073846), onPrimaryContainer = Color(0xFFC4F9FF),
        secondary = Color(0xFFFF36D7), onSecondary = Color(0xFF270020),
        secondaryContainer = Color(0xFF520A46), onSecondaryContainer = Color(0xFFFFC4F1),
        tertiary = Color(0xFFA286FF), onTertiary = Color(0xFF150034),
        tertiaryContainer = Color(0xFF322069), onTertiaryContainer = Color(0xFFE5DCFF),
        background = Color(0xFF03050D), onBackground = Color(0xFFF0FAFF),
        surface = Color(0xFF080C18), onSurface = Color(0xFFF0FAFF),
        surfaceVariant = Color(0xFF111A2D), onSurfaceVariant = Color(0xFFB5C8E7),
        outline = Color(0xFF2879A1), outlineVariant = Color(0xFF4B2363),
    )
    ReaderThemeMode.MONO -> monochromeDarkScheme(soft = false)
    ReaderThemeMode.MONO_SOFT -> monochromeDarkScheme(soft = true)
    ReaderThemeMode.CUSTOM -> error("CUSTOM handled separately")
}

private fun lightScheme(theme: ReaderThemeMode): ColorScheme = when (theme) {
    ReaderThemeMode.OLED, ReaderThemeMode.DARK -> lightColorScheme(
        primary = Color(0xFF465D95), secondary = Color(0xFF59647A),
        background = Color(0xFFF8F9FC), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE9EBF1),
        onBackground = Color(0xFF1A1C20), onSurface = Color(0xFF1A1C20),
    )
    ReaderThemeMode.PAPER -> lightColorScheme(
        primary = Color(0xFF40558A), background = Color(0xFFF8F5EE), surface = Color(0xFFFFFCF5),
        surfaceVariant = Color(0xFFECE8DF), onBackground = Color(0xFF24221F), onSurface = Color(0xFF24221F),
    )
    ReaderThemeMode.SEPIA -> lightColorScheme(
        primary = Color(0xFF76583B), background = Color(0xFFF1E4CA), surface = Color(0xFFF8ECD5),
        surfaceVariant = Color(0xFFE6D5B5), onBackground = Color(0xFF342A20), onSurface = Color(0xFF342A20),
    )
    ReaderThemeMode.NORD -> lightColorScheme(
        primary = Color(0xFF4C7088), secondary = Color(0xFF5E7D4B), background = Color(0xFFECEFF4),
        surface = Color(0xFFE5E9F0), surfaceVariant = Color(0xFFD8DEE9), onBackground = Color(0xFF2E3440), onSurface = Color(0xFF2E3440),
    )
    ReaderThemeMode.DRACULA -> lightColorScheme(
        primary = Color(0xFF6D47A8), secondary = Color(0xFF2A7C49), background = Color(0xFFF8F8F2),
        surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE8E6EE), onBackground = Color(0xFF282A36), onSurface = Color(0xFF282A36),
    )
    ReaderThemeMode.SOLARIZED_DARK -> lightColorScheme(
        primary = Color(0xFF268BD2), secondary = Color(0xFF2AA198), background = Color(0xFFFDF6E3),
        surface = Color(0xFFEEE8D5), surfaceVariant = Color(0xFFE3DCC8), onBackground = Color(0xFF586E75), onSurface = Color(0xFF586E75),
    )
    ReaderThemeMode.CATPPUCCIN -> lightColorScheme(
        primary = Color(0xFF8839EF), secondary = Color(0xFF1E66F5), background = Color(0xFFEFF1F5),
        surface = Color(0xFFE6E9EF), surfaceVariant = Color(0xFFDCE0E8), onBackground = Color(0xFF4C4F69), onSurface = Color(0xFF4C4F69),
    )
    ReaderThemeMode.GRUVBOX -> lightColorScheme(
        primary = Color(0xFFB57614), secondary = Color(0xFF79740E), background = Color(0xFFFBF1C7),
        surface = Color(0xFFF2E5BC), surfaceVariant = Color(0xFFE5D5AD), onBackground = Color(0xFF3C3836), onSurface = Color(0xFF3C3836),
    )
    // Official Rosé Pine light companion: Dawn.
    ReaderThemeMode.ROSE_PINE_MOON -> lightColorScheme(
        primary = Color(0xFF907AA9), secondary = Color(0xFF56949F),
        background = Color(0xFFFAF4ED), surface = Color(0xFFFFFAF3), surfaceVariant = Color(0xFFF2E9E1),
        onBackground = Color(0xFF464261), onSurface = Color(0xFF464261),
    )
    // CMX Purple keeps the semantic violet/foam identity while using a high-contrast light canvas.
    ReaderThemeMode.CMX_PURPLE -> lightColorScheme(
        primary = Color(0xFF7650B5), secondary = Color(0xFF397582),
        background = Color(0xFFFAF7FF), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFECE6F5),
        onBackground = Color(0xFF2B2434), onSurface = Color(0xFF2B2434),
    )
    ReaderThemeMode.WASTELAND_TERMINAL -> darkScheme(ReaderThemeMode.WASTELAND_TERMINAL)
    ReaderThemeMode.CYBERPUNK_NEON -> darkScheme(ReaderThemeMode.CYBERPUNK_NEON)
    ReaderThemeMode.MONO -> monochromeLightScheme(soft = false)
    ReaderThemeMode.MONO_SOFT -> monochromeLightScheme(soft = true)
    ReaderThemeMode.CUSTOM -> error("CUSTOM handled separately")
}

private fun ReaderThemeMode.isMonochrome(): Boolean =
    this == ReaderThemeMode.MONO || this == ReaderThemeMode.MONO_SOFT

private fun monochromeDarkScheme(soft: Boolean): ColorScheme {
    val background = if (soft) Color(0xFF111111) else Color.Black
    val surface = if (soft) Color(0xFF181818) else Color(0xFF0B0B0B)
    val surfaceVariant = if (soft) Color(0xFF252525) else Color(0xFF171717)
    val text = if (soft) Color(0xFFE8E8E8) else Color.White
    val muted = if (soft) Color(0xFFB6B6B6) else Color(0xFFCCCCCC)
    val emphasis = if (soft) Color(0xFFF4F4F4) else Color.White
    val container = if (soft) Color(0xFF333333) else Color(0xFF242424)
    val outline = if (soft) Color(0xFF6B6B6B) else Color(0xFF777777)
    return darkColorScheme(
        primary = emphasis, onPrimary = Color.Black, primaryContainer = container, onPrimaryContainer = text,
        secondary = muted, onSecondary = Color.Black, secondaryContainer = surfaceVariant, onSecondaryContainer = text,
        tertiary = muted, onTertiary = Color.Black, tertiaryContainer = surfaceVariant, onTertiaryContainer = text,
        background = background, onBackground = text, surface = surface, onSurface = text,
        surfaceVariant = surfaceVariant, onSurfaceVariant = muted,
        error = emphasis, onError = Color.Black, errorContainer = container, onErrorContainer = text,
        outline = outline,
    )
}

private fun monochromeLightScheme(soft: Boolean): ColorScheme {
    val background = if (soft) Color(0xFFF4F4F4) else Color.White
    val surface = if (soft) Color(0xFFFAFAFA) else Color(0xFFFAFAFA)
    val surfaceVariant = if (soft) Color(0xFFE5E5E5) else Color(0xFFEDEDED)
    val text = if (soft) Color(0xFF1E1E1E) else Color.Black
    val muted = if (soft) Color(0xFF575757) else Color(0xFF4A4A4A)
    val emphasis = if (soft) Color(0xFF111111) else Color.Black
    val container = if (soft) Color(0xFFD6D6D6) else Color(0xFFDCDCDC)
    val outline = if (soft) Color(0xFF777777) else Color(0xFF707070)
    return lightColorScheme(
        primary = emphasis, onPrimary = Color.White, primaryContainer = container, onPrimaryContainer = text,
        secondary = muted, onSecondary = Color.White, secondaryContainer = surfaceVariant, onSecondaryContainer = text,
        tertiary = muted, onTertiary = Color.White, tertiaryContainer = surfaceVariant, onTertiaryContainer = text,
        background = background, onBackground = text, surface = surface, onSurface = text,
        surfaceVariant = surfaceVariant, onSurfaceVariant = muted,
        error = emphasis, onError = Color.White, errorContainer = container, onErrorContainer = text,
        outline = outline,
    )
}

private fun customScheme(settings: ReaderSettings): ColorScheme {
    val bg = Color(settings.customBackgroundColorArgb)
    val surface = Color(settings.customSurfaceColorArgb)
    val text = Color(settings.customTextColorArgb)
    val accent = Color(settings.customAccentColorArgb)
    return if (isLightBackground(bg)) {
        lightColorScheme(
            primary = accent, secondary = accent, background = bg, surface = surface,
            surfaceVariant = mix(surface, bg, 0.35f), onBackground = text, onSurface = text,
        )
    } else {
        darkColorScheme(
            primary = accent, secondary = accent, background = bg, surface = surface,
            surfaceVariant = mix(surface, text, 0.10f), onBackground = text, onSurface = text,
        )
    }
}

private fun mix(a: Color, b: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = 1f,
    )
}

private fun isLightBackground(color: Color): Boolean {
    val luminance = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
    return luminance > 0.62f
}

