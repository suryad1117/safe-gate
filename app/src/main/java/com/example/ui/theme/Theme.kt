package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AccentColor
import com.example.data.AppTheme
import com.example.data.CornerStyle

fun getPrimaryColor(accent: AccentColor): Color = when (accent) {
    AccentColor.NONE -> Color(0xFF6750A4)
    AccentColor.BLUE -> Color(0xFF2979FF)
    AccentColor.NAVY -> Color(0xFF283593)
    AccentColor.PURPLE -> Color(0xFF7C4DFF)
    AccentColor.PRUSSIAN_BLUE -> Color(0xFF003153)
    AccentColor.BLACK -> Color(0xFF37474F)
    AccentColor.GREEN -> Color(0xFF00E676)
    AccentColor.RED -> Color(0xFFFF5252)
    AccentColor.ORANGE -> Color(0xFFFF9100)
    AccentColor.PINK -> Color(0xFFFF4081)
    AccentColor.TEAL -> Color(0xFF00BFA5)
    AccentColor.CYAN -> Color(0xFF00E5FF)
}

fun getSecondaryColor(accent: AccentColor): Color = when (accent) {
    AccentColor.NONE -> Color(0xFF625B71)
    AccentColor.BLUE -> Color(0xFF82B1FF)
    AccentColor.NAVY -> Color(0xFF5C6BC0)
    AccentColor.PURPLE -> Color(0xFF5EE6C0)
    AccentColor.PRUSSIAN_BLUE -> Color(0xFF1E88E5)
    AccentColor.BLACK -> Color(0xFF78909C)
    AccentColor.GREEN -> Color(0xFF69F0AE)
    AccentColor.RED -> Color(0xFFFF8A80)
    AccentColor.ORANGE -> Color(0xFFFFD180)
    AccentColor.PINK -> Color(0xFFFF80AB)
    AccentColor.TEAL -> Color(0xFF64FFDA)
    AccentColor.CYAN -> Color(0xFF84FFFF)
}

fun getCornerRadius(cornerStyle: CornerStyle): Dp = when (cornerStyle) {
    CornerStyle.SQUARE -> 4.dp
    CornerStyle.ROUNDED -> 16.dp
    CornerStyle.EXTRA_ROUNDED -> 24.dp
}

fun buildAmoledColorScheme(accent: AccentColor): ColorScheme {
    val primaryColor = getPrimaryColor(accent)
    val secondaryColor = getSecondaryColor(accent)

    return darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor.copy(alpha = 0.25f),
        onPrimaryContainer = Color.White,
        secondary = secondaryColor,
        onSecondary = Color(0xFF00382B),
        secondaryContainer = secondaryColor.copy(alpha = 0.2f),
        onSecondaryContainer = secondaryColor,
        tertiary = Color(0xFFFF6B81),
        onTertiary = Color.White,
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceVariant = Color(0xFF101010),
        onSurfaceVariant = Color(0xFFA0A5BA),
        outline = Color(0xFF252838)
    )
}

fun buildDarkColorScheme(accent: AccentColor): ColorScheme {
    val primaryColor = getPrimaryColor(accent)
    val secondaryColor = getSecondaryColor(accent)

    return darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor.copy(alpha = 0.25f),
        onPrimaryContainer = Color.White,
        secondary = secondaryColor,
        onSecondary = Color(0xFF00382B),
        secondaryContainer = secondaryColor.copy(alpha = 0.2f),
        onSecondaryContainer = secondaryColor,
        tertiary = Color(0xFFFF6B81),
        onTertiary = Color.White,
        background = Color(0xFF0C0E1A),
        onBackground = Color(0xFFF1F3FD),
        surface = Color(0xFF14172B),
        onSurface = Color(0xFFF1F3FD),
        surfaceVariant = Color(0xFF1F243E),
        onSurfaceVariant = Color(0xFF9399B2),
        outline = Color(0xFF2E3454)
    )
}

fun buildLightColorScheme(accent: AccentColor): ColorScheme {
    val primaryColor = getPrimaryColor(accent)
    val secondaryColor = getSecondaryColor(accent)

    return lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor.copy(alpha = 0.15f),
        onPrimaryContainer = primaryColor,
        secondary = secondaryColor,
        onSecondary = Color.White,
        secondaryContainer = secondaryColor.copy(alpha = 0.15f),
        onSecondaryContainer = Color(0xFF004D40),
        tertiary = Color(0xFFE91E63),
        onTertiary = Color.White,
        background = Color(0xFFF4F6FC),
        onBackground = Color(0xFF141724),
        surface = Color.White,
        onSurface = Color(0xFF141724),
        surfaceVariant = Color(0xFFE7ECF8),
        onSurfaceVariant = Color(0xFF5A627E),
        outline = Color(0xFFD0D7EC)
    )
}

@Composable
fun SafeGateTheme(
    theme: AppTheme = AppTheme.DARK,
    accent: AccentColor = AccentColor.PURPLE,
    cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = when (theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.AMOLED -> true
    }

    val colorScheme = when {
        accent == AccentColor.NONE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        theme == AppTheme.AMOLED -> buildAmoledColorScheme(accent)
        isDark -> buildDarkColorScheme(accent)
        else -> buildLightColorScheme(accent)
    }

    val radius = getCornerRadius(cornerStyle)
    val shapes = Shapes(
        small = RoundedCornerShape(radius / 2),
        medium = RoundedCornerShape(radius),
        large = RoundedCornerShape(radius * 1.5f),
        extraLarge = RoundedCornerShape(radius * 2)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    SafeGateTheme(
        theme = if (darkTheme) AppTheme.DARK else AppTheme.LIGHT,
        accent = AccentColor.PURPLE,
        content = content
    )
}


