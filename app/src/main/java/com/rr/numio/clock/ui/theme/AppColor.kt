package com.rr.numio.clock.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

object AppColor {
    var accent = mutableStateOf(NumioAmber)

    fun update(color: Color) {
        accent.value = color
    }
}