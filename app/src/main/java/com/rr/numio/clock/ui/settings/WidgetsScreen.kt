package com.rr.numio.clock.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            SettingsSectionTitle("Card & Clear widgets")
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard { WidgetFontPicker() }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
