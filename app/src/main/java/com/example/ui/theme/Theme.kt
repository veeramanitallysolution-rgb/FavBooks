package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.AppThemeMode

private val BookshelfLightColorScheme = lightColorScheme(
    primary = NavyPrimaryLight,
    onPrimary = OnNavyPrimaryLight,
    primaryContainer = NavyContainerLight,
    onPrimaryContainer = OnNavyContainerLight,
    secondary = CobaltSecondaryLight,
    onSecondary = OnCobaltSecondaryLight,
    secondaryContainer = IceBlueContainerLight,
    onSecondaryContainer = OnIceBlueContainerLight,
    tertiary = BronzeTertiaryLight,
    onTertiary = OnBronzeTertiaryLight,
    tertiaryContainer = BronzeContainerLight,
    onTertiaryContainer = OnBronzeContainerLight,
    background = CleanIvoryBackgroundLight,
    onBackground = CharcoalInkOnBackgroundLight,
    surface = PureWhiteSurfaceLight,
    onSurface = CharcoalInkOnBackgroundLight,
    surfaceVariant = SoftSlateSurfaceVariantLight,
    onSurfaceVariant = CoolSlateOnSurfaceVariantLight,
    outline = SoftNavyOutlineLight
)

private val BookshelfDarkColorScheme = darkColorScheme(
    primary = NavyPrimaryDark,
    onPrimary = OnNavyPrimaryDark,
    primaryContainer = NavyContainerDark,
    onPrimaryContainer = OnNavyContainerDark,
    secondary = SteelSecondaryDark,
    onSecondary = OnSteelSecondaryDark,
    secondaryContainer = MidnightContainerDark,
    onSecondaryContainer = OnMidnightContainerDark,
    tertiary = BronzeTertiaryDark,
    onTertiary = OnBronzeTertiaryDark,
    tertiaryContainer = BronzeContainerDark,
    onTertiaryContainer = OnBronzeContainerDark,
    background = MidnightBackgroundDark,
    onBackground = IceOnBackgroundDark,
    surface = DeepNavySurfaceDark,
    onSurface = IceOnBackgroundDark,
    surfaceVariant = NavySurfaceVariantDark,
    onSurfaceVariant = CoolGrayOnSurfaceVariantDark,
    outline = NavyOutlineDark
)

@Composable
fun DigitalBookshelfTheme(
    appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appThemeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) BookshelfDarkColorScheme else BookshelfLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BookshelfTypography,
        content = content
    )
}
