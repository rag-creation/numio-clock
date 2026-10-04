package com.rr.numio.clock.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.R
import com.rr.numio.clock.ui.theme.*
import com.rr.numio.clock.widget.PosterRenderer
import com.rr.numio.clock.widget.PosterStyles

private fun styleFont(key: String) = when (key) {
    "script" -> FontFamily(Font(R.font.dancing_script))
    "bold" -> FontFamily(Font(R.font.bangers))
    "bubble", "bubble_clear" -> FontFamily(Font(R.font.baloo2_extrabold))
    else -> FontFamily(Font(R.font.permanent_marker))
}

@Composable
fun PosterStylePicker() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(PosterStyles.current(context)) }
    val accent = AppColor.accent.value

    // The real widget image, so the preview is exactly what you'll get
    val preview = remember(selected, accent) {
        PosterRenderer.render(context, 800, 420, selected).asImageBitmap()
    }

    Column {
        Text("Poster widget style", fontSize = 14.sp, color = NumioTextPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2A2420))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = preview,
                contentDescription = "Poster widget preview",
                modifier = Modifier.fillMaxWidth().aspectRatio(800f / 420f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3 per row, so longer names like "Sketch Clear" have room
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PosterStyles.options.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (key, label) ->
                        val isSelected = key == selected
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) accent.copy(alpha = 0.12f) else Color(0xFF1F1F1F))
                                .then(
                                    if (isSelected) Modifier.border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                                .clickable {
                                    selected = key
                                    PosterStyles.save(context, key) // updates widgets immediately
                                }
                                .padding(horizontal = 6.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 15.sp,
                                fontFamily = styleFont(key),
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp,
                                color = if (isSelected) accent else NumioTextSecondary
                            )
                        }
                    }
                    // keep the last row's buttons the same width
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}
