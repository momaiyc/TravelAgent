package com.travelagent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 主色调
val Primary = Color(0xFFE85D24)
val PrimaryLight = Color(0xFFFAECE7)
val OnPrimary = Color.White

val Secondary = Color(0xFF1D9E75)
val SecondaryLight = Color(0xFFE1F5EE)
val OnSecondary = Color.White

val Tertiary = Color(0xFF378ADD)
val TertiaryLight = Color(0xFFE6F1FB)

// Agent颜色
val AgentCoordinatorColor = Color(0xFF7F77DD)
val AgentCoordinatorLight = Color(0xFFEEEDFE)
val AgentTransportColor = Color(0xFF378ADD)
val AgentTransportLight = Color(0xFFE6F1FB)
val AgentHotelColor = Color(0xFFEF9F27)
val AgentHotelLight = Color(0xFFFAEEDA)
val AgentAttractionColor = Color(0xFF1D9E75)
val AgentAttractionLight = Color(0xFFE1F5EE)
val AgentFoodColor = Color(0xFFE85D24)
val AgentFoodLight = Color(0xFFFAECE7)

// 状态颜色
val Success = Color(0xFF1D9E75)
val Warning = Color(0xFFEF9F27)
val Error = Color(0xFFE24B4A)
val Info = Color(0xFF378ADD)

// 背景颜色
val Background = Color(0xFFFAFAFA)
val Surface = Color.White
val OnBackground = Color(0xFF1C1B1F)
val OnSurface = Color(0xFF1C1B1F)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryLight,
    tertiary = Tertiary,
    tertiaryContainer = TertiaryLight,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    error = Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFF5D3E2C),
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = Color(0xFF1D4D3E),
    tertiary = Tertiary,
    tertiaryContainer = Color(0xFF1D3D5D),
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    error = Error,
)

@Composable
fun TravelAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
