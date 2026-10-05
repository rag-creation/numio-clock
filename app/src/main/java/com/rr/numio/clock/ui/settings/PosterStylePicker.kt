package com.rr.numio.clock.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.ui.theme.*
import com.rr.numio.clock.widget.PosterRenderer
import com.rr.numio.clock.widget.PosterStyles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Poster widget gallery: every style gets its own card with a live preview,
 * drawn by the same code as the real widget. Tap a card to use that style.
 */
@Composable
fun PosterStyleGallery() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(PosterStyles.current(context)) }
    val accent = AppColor.accent.value

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PosterStyles.options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (key, label) ->
                    PosterCard(
                        key = key,
                        label = label,
                        isSelected = key == selected,
                        accentArgb = accent.value.toLong(),
                        modifier = Modifier.weight(1f)
                    ) {
                        selected = key
                        PosterStyles.save(context, key) // updates widgets immediately
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PosterCard(
    key: String,
    label: String,
    isSelected: Boolean,
    accentArgb: Long,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val accent = AppColor.accent.value

    // Draw the preview off the main thread; redraw when the accent colour changes
    val preview by produceState<ImageBitmap?>(initialValue = null, key, accentArgb) {
        value = withContext(Dispatchers.Default) {
            PosterRenderer.render(context, 600, 300, key).asImageBitmap()
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) accent.copy(alpha = 0.10f) else Color(0xFF1B1B1B))
            .then(
                if (isSelected) Modifier.border(1.5.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFF2A2420)),
            contentAlignment = Alignment.Center
        ) {
            preview?.let {
                Image(bitmap = it, contentDescription = "$label preview", modifier = Modifier.fillMaxSize())
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 2.dp, top = 7.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, color = if (isSelected) accent else NumioTextSecondary)
            if (isSelected) {
                Box(
                    modifier = Modifier.size(17.dp).clip(CircleShape).background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", fontSize = 10.sp, color = Color(0xFF111111))
                }
            }
        }
    }
}
