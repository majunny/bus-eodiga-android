package kr.buswhere.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BusBlue,
    primaryContainer = BusBlueContainer,
    secondary = BusTeal,
    secondaryContainer = BusTealContainer,
    tertiary = BusRed,
    error = BusRed,
    background = BusBackground,
    surface = BusSurface,
    surfaceVariant = BusSurfaceMuted,
    onPrimary = BusSurface,
    onSecondary = BusSurface,
    onBackground = BusText,
    onSurface = BusText,
    onSurfaceVariant = BusTextMuted,
    outline = BusOutline,
    outlineVariant = BusOutlineSoft,
    /* Other default colors to override
    onPrimary = Color.White,
    onTertiary = Color.White,
    */
)

@Composable
fun BUS어디가Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
