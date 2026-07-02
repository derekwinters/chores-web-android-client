package com.derekwinters.chores.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorHexTest {

    @Test
    fun parseHexColor_sixDigit_assumesFullyOpaque() {
        assertEquals(Color(0xFFFF0000), parseHexColor("#FF0000"))
    }

    @Test
    fun parseHexColor_eightDigit_usesProvidedAlpha() {
        assertEquals(Color(0x80FF0000), parseHexColor("#80FF0000"))
    }

    @Test
    fun parseHexColor_malformed_returnsFallback() {
        assertEquals(Color.Blue, parseHexColor("not-a-color", fallback = Color.Blue))
    }
}
