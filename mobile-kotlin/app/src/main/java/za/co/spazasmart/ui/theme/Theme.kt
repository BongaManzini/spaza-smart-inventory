package za.co.spazasmart.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Emerald80 = Color(0xFFA7F3D0)
val Emerald40 = Color(0xFF059669)
val Amber80 = Color(0xFFFDE68A)
val Amber40 = Color(0xFFD97706)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)

private val DarkColorScheme = darkColorScheme(
    primary = Emerald40,
    secondary = Amber40,
    tertiary = Emerald80,
    background = Slate900,
    surface = Slate800,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF1F5F9)
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald40,
    secondary = Amber40,
    tertiary = Emerald80,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Slate900,
    onSurface = Slate900
)

@Composable
fun SpazaSmartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
