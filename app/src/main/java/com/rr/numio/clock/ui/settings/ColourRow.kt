package com.rr.numio.clock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.ui.theme.NumioBgCard
import com.rr.numio.clock.ui.theme.NumioTextMuted
import com.rr.numio.clock.ui.theme.NumioTextPrimary

/**
 * A row of colour circles plus a "+" circle that opens a custom colour picker.
 * A custom colour that isn't one of the presets shows as its own circle, so it's easy to pick again.
 *
 * @param presets colours as ARGB ints
 * @param selected the current colour (ARGB), or null when none of these is chosen
 * @param labels optional names shown under the circles (same order as presets)
 */
@Composable
fun ColourRow(
    presets: List<Int>,
    selected: Int?,
    accent: Color,
    onPick: (Int) -> Unit,
    labels: List<String>? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    val custom = selected?.takeIf { s -> presets.none { it == s } }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        presets.forEachIndexed { i, c ->
            Swatch(Color(c), c == selected, accent, labels?.getOrNull(i)) { onPick(c) }
        }
        if (custom != null) Swatch(Color(custom), true, accent, if (labels != null) "Custom" else null) { showPicker = true }
        // "+" opens the picker
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                        )
                    )
                    .clickable { showPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(Color(0xCC111111)), contentAlignment = Alignment.Center) {
                    Text("+", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            if (labels != null) {
                Spacer(Modifier.height(4.dp))
                Text("More", fontSize = 12.sp, color = NumioTextMuted)
            }
        }
    }

    if (showPicker) {
        ColourPickerDialog(
            start = selected ?: android.graphics.Color.argb(255, (accent.red * 255).toInt(), (accent.green * 255).toInt(), (accent.blue * 255).toInt()),
            accent = accent,
            onDismiss = { showPicker = false },
            onPick = { showPicker = false; onPick(it) }
        )
    }
}

@Composable
private fun Swatch(color: Color, on: Boolean, accent: Color, label: String?, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .then(if (on) Modifier.border(2.5.dp, accent, CircleShape) else Modifier)
                .padding(5.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .clickable(onClick = onClick)
        )
        if (label != null) {
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = if (on) accent else NumioTextMuted)
        }
    }
}

/** Hue, saturation and brightness sliders plus a hex box, with a live swatch. */
@Composable
fun ColourPickerDialog(start: Int, accent: Color, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(start, it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    fun current() = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
    fun hexOf(c: Int) = "#%06X".format(c and 0xFFFFFF)
    var hex by remember { mutableStateOf(hexOf(start)) }

    fun fromSliders() { hex = hexOf(current()) }
    fun fromHex(text: String) {
        hex = text.uppercase().take(7)
        val parsed = runCatching { android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex") }.getOrNull()
        if (parsed != null && hex.trimStart('#').length == 6) {
            val out = FloatArray(3); android.graphics.Color.colorToHSV(parsed, out)
            hue = out[0]; sat = out[1]; value = out[2]
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NumioBgCard,
        title = { Text("Pick a colour", color = NumioTextPrimary) },
        text = {
            Column {
                Box(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color(current()))
                )
                Spacer(Modifier.height(14.dp))
                PickerSlider("Colour", hue, 0f..360f, accent,
                    Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) })) { hue = it; fromSliders() }
                PickerSlider("Strength", sat, 0f..1f, accent,
                    Brush.horizontalGradient(listOf(Color.hsv(hue, 0f, value), Color.hsv(hue, 1f, value)))) { sat = it; fromSliders() }
                PickerSlider("Brightness", value, 0f..1f, accent,
                    Brush.horizontalGradient(listOf(Color.Black, Color.hsv(hue, sat, 1f)))) { value = it; fromSliders() }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hex", fontSize = 13.sp, color = NumioTextMuted)
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = hex,
                        onValueChange = { fromHex(it) },
                        singleLine = true,
                        textStyle = TextStyle(color = NumioTextPrimary, fontSize = 15.sp, fontFamily = FontFamily.Monospace),
                        decorationBox = { inner ->
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1F1F1F)).padding(horizontal = 12.dp, vertical = 10.dp)
                            ) { inner() }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(current()) }) { Text("Use colour", color = accent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = NumioTextMuted) }
        }
    )
}

@Composable
private fun PickerSlider(
    label: String, v: Float, range: ClosedFloatingPointRange<Float>, accent: Color, track: Brush, onChange: (Float) -> Unit
) {
    Text(label, fontSize = 12.sp, color = NumioTextMuted)
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(track))
        Slider(
            value = v, onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}
