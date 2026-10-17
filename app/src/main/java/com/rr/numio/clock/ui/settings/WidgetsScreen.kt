package com.rr.numio.clock.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.ui.theme.*

/**
 * All widget choices on their own page, so Settings stays short.
 * Opened from Settings → Widget styles.
 */
@Composable
fun WidgetsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 6.dp)
            ) {
                Text("←", fontSize = 22.sp, color = NumioTextPrimary)
                Spacer(modifier = Modifier.width(14.dp))
                Text("Widgets", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = NumioTextPrimary)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsSectionTitle("Poster widget")
                Spacer(modifier = Modifier.width(8.dp))
                Text("4×2 · resizable", fontSize = 10.sp, color = NumioTextMuted.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            PosterStyleGallery()
            Spacer(modifier = Modifier.height(26.dp))
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsSectionTitle("Type clock")
                Spacer(modifier = Modifier.width(8.dp))
                Text("2×2", fontSize = 10.sp, color = NumioTextMuted.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            TypeClockPicker()
            Spacer(modifier = Modifier.height(26.dp))
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsSectionTitle("World clock")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Card & Clear · resizable", fontSize = 10.sp, color = NumioTextMuted.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard { WorldCityPicker() }
            Spacer(modifier = Modifier.height(26.dp))
        }

        item {
            SettingsSectionTitle("Card & Clear widgets")
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard { WidgetFontPicker() }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/** Pick which of your world clock cities the World widget shows (up to 4). */
@Composable
private fun WorldCityPicker() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cities by com.rr.numio.clock.data.WorldCityStore.getCities(context)
        .collectAsState(initial = emptyList())
    val widget = com.rr.numio.clock.widget.WorldWidget
    var chosen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(widget.chosen(context)) }
    // Until something is picked, the widget shows the first 3.
    val ticked = chosen ?: cities.take(3).map { it.name }
    val accent = AppColor.accent.value
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Cities on the widget", fontSize = 15.sp, color = NumioTextPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        Text("Tick up to ${widget.MAX}. Add more cities in the Clock tab.", fontSize = 12.sp, color = NumioTextMuted,
            modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(6.dp))
        cities.forEach { city ->
            val on = city.name in ticked
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val next = if (on) ticked - city.name else ticked + city.name
                        if (next.size > widget.MAX) {
                            android.widget.Toast.makeText(context, "Up to ${widget.MAX} cities", android.widget.Toast.LENGTH_SHORT).show()
                        } else if (next.isNotEmpty()) {
                            chosen = next
                            widget.saveChosen(context, next)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(city.name, fontSize = 16.sp, color = if (on) accent else NumioTextPrimary,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(7.dp))
                        .background(if (on) accent else NumioTextMuted.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (on) Text("✓", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.Black)
                }
            }
        }
    }
}
