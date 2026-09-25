package com.rr.numio.clock.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    // Big time display — 7:00 AM
    displayLarge = TextStyle(
        fontWeight = FontWeight.W100,
        fontSize = 72.sp,
        lineHeight = 72.sp,
        letterSpacing = (-3).sp
    ),
    // Clock screen digital time
    displayMedium = TextStyle(
        fontWeight = FontWeight.W200,
        fontSize = 52.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1).sp
    ),
    // Timer / Stopwatch digits
    displaySmall = TextStyle(
        fontWeight = FontWeight.W300,
        fontSize = 36.sp,
        lineHeight = 36.sp,
        letterSpacing = 2.sp
    ),
    // Section headers
    titleLarge = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // Alarm time on cards
    titleMedium = TextStyle(
        fontWeight = FontWeight.W300,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    // Labels, day dots
    labelSmall = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.5.sp
    ),
    // Body text
    bodyMedium = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    // Subtle hints
    bodySmall = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)