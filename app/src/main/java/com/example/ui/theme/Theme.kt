package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class StillnessExtendedColors(
    val good: Color,
    val caution: Color,
    val alert: Color,
    val categorySocial: Color,
    val categoryVideo: Color,
    val categoryGames: Color,
    val categoryCommunication: Color,
    val categoryProductivity: Color,
    val categoryOther: Color,
)

private val LightExtendedColors = StillnessExtendedColors(
    good = WellBeingGoodLight,
    caution = WellBeingCautionLight,
    alert = WellBeingAlertLight,
    categorySocial = CategorySocialLight,
    categoryVideo = CategoryVideoLight,
    categoryGames = CategoryGamesLight,
    categoryCommunication = CategoryCommunicationLight,
    categoryProductivity = CategoryProductivityLight,
    categoryOther = CategoryOtherLight,
)

private val DarkExtendedColors = StillnessExtendedColors(
    good = WellBeingGoodDark,
    caution = WellBeingCautionDark,
    alert = WellBeingAlertDark,
    categorySocial = CategorySocialDark,
    categoryVideo = CategoryVideoDark,
    categoryGames = CategoryGamesDark,
    categoryCommunication = CategoryCommunicationDark,
    categoryProductivity = CategoryProductivityDark,
    categoryOther = CategoryOtherDark,
)

val LocalStillnessColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
)

val StillnessShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun StillnessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalStillnessColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StillnessTypography,
            shapes = StillnessShapes,
            content = content,
        )
    }
}

object StillnessThemeTokens {
    val extendedColors: StillnessExtendedColors
        @Composable get() = LocalStillnessColors.current
}
