package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

data class BrowserThemeColors(
    val primary: Color,
    val topBarBackground: Color,
    val tabBarBackground: Color,
    val subBarBackground: Color,
    val tabSelectedBackground: Color,
    val tabUnselectedBackground: Color,
    val tabSelectedText: Color,
    val tabUnselectedText: Color,
    val accent: Color,
    val bottomBarBackground: Color
)

fun calculateBrowserThemeColors(hexColor: String): BrowserThemeColors {
    val parsedInt = try {
        android.graphics.Color.parseColor(hexColor)
    } catch (e: Exception) {
        android.graphics.Color.parseColor("#14B8A6")
    }

    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(parsedInt, hsv)
    val hue = hsv[0]
    val saturation = hsv[1]

    val isLowSat = saturation < 0.15f
    val effSat = if (isLowSat) saturation else maxOf(saturation, 0.45f)

    // Deep dark background for TopBar, BottomBar, and Screen outer frame
    val topBarInt = android.graphics.Color.HSVToColor(
        floatArrayOf(hue, minOf(effSat, 0.65f), 0.18f)
    )

    // Deeper dark background for TabBar
    val tabBarInt = android.graphics.Color.HSVToColor(
        floatArrayOf(hue, minOf(effSat, 0.65f), 0.11f)
    )

    // Sub-bar background (for Add-tab bar and Paste bar container)
    val subBarInt = android.graphics.Color.HSVToColor(
        floatArrayOf(hue, minOf(effSat, 0.65f), 0.14f)
    )

    // Unselected tab background
    val tabUnselectedInt = android.graphics.Color.HSVToColor(
        floatArrayOf(hue, minOf(effSat, 0.50f), 0.16f)
    )

    // Selected tab background (rich, distinct tone)
    val tabSelectedInt = android.graphics.Color.HSVToColor(
        floatArrayOf(hue, minOf(effSat, 0.75f), 0.45f)
    )

    // Accent / highlight / neon underline (very bright and energetic)
    val accentInt = android.graphics.Color.HSVToColor(
        floatArrayOf(hue, minOf(effSat, 0.85f), 0.95f)
    )

    return BrowserThemeColors(
        primary = Color(parsedInt),
        topBarBackground = Color(topBarInt),
        tabBarBackground = Color(tabBarInt),
        subBarBackground = Color(subBarInt),
        tabSelectedBackground = Color(tabSelectedInt),
        tabUnselectedBackground = Color(tabUnselectedInt),
        tabSelectedText = Color.White,
        tabUnselectedText = Color.White.copy(alpha = 0.65f),
        accent = Color(accentInt),
        bottomBarBackground = Color(topBarInt)
    )
}

@Composable
fun rememberBrowserThemeColors(hexColor: String): BrowserThemeColors {
    return remember(hexColor) {
        calculateBrowserThemeColors(hexColor)
    }
}
