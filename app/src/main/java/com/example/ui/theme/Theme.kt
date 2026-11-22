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
    primary = AmberPrimaryLight,
    onPrimary = OnAmberPrimaryLight,
    primaryContainer = ButtercreamContainerLight,
    onPrimaryContainer = OnButtercreamContainerLight,
    secondary = NavySecondaryLight,
    onSecondary = OnNavySecondaryLight,
    secondaryContainer = MintWashContainerLight,
    onSecondaryContainer = OnMintWashContainerLight,
    tertiary = BronzeTertiaryLight,
    onTertiary = OnBronzeTertiaryLight,
    tertiaryContainer = SunlitCreamTertiaryContainerLight,
    onTertiaryContainer = OnSunlitCreamContainerLight,
    background = WarmIvoryBackgroundLight,
    onBackground = CharcoalInkOnBackgroundLight,
    surface = PureWhiteSurfaceLight,
    onSurface = CharcoalInkOnBackgroundLight,
    surfaceVariant = SoftStoneSurfaceVariantLight,
    onSurfaceVariant = WarmSlateOnSurfaceVariantLight,
    outline = SoftCreamOutlineLight
)

private val BookshelfDarkColorScheme = darkColorScheme(
    primary = GoldPrimaryDark,
    onPrimary = OnGoldPrimaryDark,
    primaryContainer = GoldContainerDark,
    onPrimaryContainer = OnGoldContainerDark,
    secondary = SteelSecondaryDark,
    onSecondary = OnSteelSecondaryDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = OnSageContainerDark,
    tertiary = BronzeTertiaryDark,
    onTertiary = OnBronzeTertiaryDark,
    tertiaryContainer = BronzeContainerDark,
    onTertiaryContainer = OnBronzeContainerDark,
    background = StudyBackgroundDark,
    onBackground = CreamOnBackgroundDark,
    surface = WalnutSurfaceDark,
    onSurface = CreamOnBackgroundDark,
    surfaceVariant = CharcoalSurfaceVariantDark,
    onSurfaceVariant = WarmGrayOnSurfaceVariantDark,
    outline = DarkOutlineDark
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
