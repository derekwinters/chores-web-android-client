package com.derekwinters.chores.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Parses a `#RRGGBB` or `#AARRGGBB` hex string (chores-web's theme color format, issue #24) into
 * a Compose [Color]. Falls back to [fallback] for malformed input rather than crashing, since
 * these strings come from the network.
 */
fun parseHexColor(hex: String, fallback: Color = Color.Gray): Color {
    val cleaned = hex.removePrefix("#")
    val argb = when (cleaned.length) {
        6 -> "FF$cleaned"
        8 -> cleaned
        else -> return fallback
    }
    return runCatching { Color(argb.toLong(16).toInt()) }.getOrDefault(fallback)
}
