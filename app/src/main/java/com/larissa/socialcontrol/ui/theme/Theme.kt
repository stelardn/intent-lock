package com.larissa.socialcontrol.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = IntentTeal,
    onPrimary = IntentCream,
    primaryContainer = IntentMint,
    onPrimaryContainer = IntentForest,
    secondary = IntentBlue,
    onSecondary = IntentCream,
    secondaryContainer = IntentSky,
    onSecondaryContainer = IntentBlueNight,
    tertiary = IntentAmber,
    onTertiary = IntentInk,
    tertiaryContainer = IntentAmberSoft,
    onTertiaryContainer = IntentInk,
    error = IntentError,
    onError = IntentCream,
    errorContainer = IntentErrorSoft,
    onErrorContainer = IntentErrorDeep,
    background = IntentBackground,
    onBackground = IntentInk,
    surface = IntentSurface,
    onSurface = IntentInk,
    surfaceVariant = IntentSurfaceVariant,
    onSurfaceVariant = IntentSlate,
    outline = IntentOutline,
)

private val DarkColors = darkColorScheme(
    primary = IntentMintBright,
    onPrimary = IntentInk,
    primaryContainer = IntentForestDeep,
    onPrimaryContainer = IntentMint,
    secondary = IntentSkyBright,
    onSecondary = IntentBlueNight,
    secondaryContainer = IntentBlueNight,
    onSecondaryContainer = IntentSky,
    tertiary = IntentAmberBright,
    onTertiary = IntentInk,
    tertiaryContainer = IntentAmberDeep,
    onTertiaryContainer = IntentAmberSoft,
    error = IntentErrorBright,
    onError = IntentInk,
    errorContainer = IntentErrorDeep,
    onErrorContainer = IntentErrorSoft,
    background = IntentNight,
    onBackground = IntentCream,
    surface = IntentNightSurface,
    onSurface = IntentCream,
    surfaceVariant = IntentNightVariant,
    onSurfaceVariant = IntentMist,
    outline = IntentNightOutline,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun IntentLockTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
