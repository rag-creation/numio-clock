package com.rr.numio.clock.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.ui.theme.*
import com.rr.numio.clock.widget.TypeClock
import com.rr.numio.clock.widget.TypeClockRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Settings for the 2×2 Type clock: layout, font, second colour and outline. */
@Composable
fun TypeClockPicker() {
    val context = LocalContext.current
    val accent = AppColor.accent.value
    var layout by remember { mutableStateOf(TypeClock.layout(context)) }
    var font by remember { mutableStateOf(TypeClock.font(context)) }
    var second by remember { mutableStateOf(TypeClock.second(context)) }
    var outline by remember { mutableStateOf(TypeClock.outline(context)) }
    var card by remember { mutableStateOf(TypeClock.card(context)) }
    // Anything that changes the drawing; previews redraw when it changes
    val look = listOf(font, second, outline, card, accent.value.toLong())

    Column {
        // Layout cards, each a live preview in the chosen font
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TypeClock.layouts.forEach { (key, label) ->
                val isSelected = key == layout
                val preview by produceState<ImageBitmap?>(null, key, look) {
                    value = withContext(Dispatchers.Default) {
                        TypeClockRenderer.render(context, 300, 300, key, font).asImageBitmap()
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) accent.copy(alpha = 0.10f) else Color(0xFF1B1B1B))
                        .then(
                            if (isSelected) Modifier.border(1.5.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            else Modifier
                        )
                        .clickable { layout = key; TypeClock.saveLayout(context, key) }
                        .padding(6.dp)
                ) {
                    // Checker-ish backdrop so the transparent version is visible in the preview
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (card) Color.Transparent else Color(0xFF4A5A78))) {
                        preview?.let { Image(it, contentDescription = "$label preview", modifier = Modifier.fillMaxSize()) }
                    }
                    Text(
                        label, fontSize = 12.sp,
                        color = if (isSelected) accent else NumioTextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Font", fontSize = 13.sp, color = NumioTextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        // Font chips, each written in its own font
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TypeClock.fonts.forEach { (key, label, res) ->
                val isSelected = key == font
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) accent.copy(alpha = 0.12f) else Color(0xFF1F1F1F))
                        .then(
                            if (isSelected) Modifier.border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .clickable { font = key; TypeClock.saveFont(context, key) }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        label,
                        fontSize = if (key == "moirai") 19.sp else 16.sp,
                        fontFamily = FontFamily(Font(res)),
                        color = if (isSelected) accent else NumioTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Second colour", fontSize = 13.sp, color = NumioTextSecondary)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TypeClock.secondColours.forEach { c ->
                val isSelected = c == second
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .then(if (isSelected) Modifier.border(2.dp, accent, CircleShape) else Modifier)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .clickable { second = c; TypeClock.saveSecond(context, c) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1F1F1F))
                .clickable { outline = !outline; TypeClock.saveOutline(context, outline) }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Outline digits", fontSize = 14.sp, color = NumioTextPrimary, modifier = Modifier.weight(1f))
            Switch(
                checked = outline,
                onCheckedChange = { outline = it; TypeClock.saveOutline(context, it) },
                colors = SwitchDefaults.colors(checkedTrackColor = accent)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1F1F1F))
                .clickable { card = !card; TypeClock.saveCard(context, card) }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Background card", fontSize = 14.sp, color = NumioTextPrimary)
                Text("Off = transparent, straight on your wallpaper", fontSize = 11.sp, color = NumioTextMuted)
            }
            Switch(
                checked = card,
                onCheckedChange = { card = it; TypeClock.saveCard(context, it) },
                colors = SwitchDefaults.colors(checkedTrackColor = accent)
            )
        }
    }
}
