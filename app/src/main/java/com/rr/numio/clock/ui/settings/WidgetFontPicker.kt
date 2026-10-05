package com.rr.numio.clock.ui.settings

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.R
import com.rr.numio.clock.ui.theme.*
import com.rr.numio.clock.widget.WidgetFonts

/** Same font the widget uses, so the preview matches exactly. */
private fun previewFamily(key: String): FontFamily =
    FontFamily(
        Typeface(
            android.graphics.Typeface.create(
                WidgetFonts.family(key),
                android.graphics.Typeface.NORMAL
            )
        )
    )

@Composable
fun WidgetFontPicker() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(WidgetFonts.current(context)) }
    val accent = AppColor.accent.value

    Column {
        Text("Widget font", fontSize = 14.sp, color = NumioTextPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        // Live preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0E0E0E))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "10:30",
                    fontSize = 48.sp,
                    fontFamily = previewFamily(selected),
                    color = accent
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "am",
                    fontSize = 15.sp,
                    color = accent.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Options, 3 per row, each written in its own font
        WidgetFonts.options.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (key, label) ->
                    val isSelected = key == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) accent.copy(alpha = 0.12f) else Color(0xFF1F1F1F))
                            .then(
                                if (isSelected) Modifier.border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                else Modifier
                            )
                            .clickable {
                                selected = key
                                WidgetFonts.save(context, key) // updates widgets immediately
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontFamily = previewFamily(key),
                            color = if (isSelected) accent else NumioTextSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
