package com.rr.numio.clock.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.ui.theme.ClockByNumioTheme
import com.rr.numio.clock.ui.theme.NumioBgCard
import com.rr.numio.clock.ui.theme.NumioDark
import com.rr.numio.clock.ui.theme.NumioTextMuted
import com.rr.numio.clock.ui.theme.NumioTextPrimary

/**
 * Opens when a World Sticker tile is placed (and from the tile's settings later):
 * pick the city and the time colour for this one tile.
 */
class WorldStickerSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out without saving doesn't place the tile
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val cities = WorldSticker.allCities(this)
        val startCity = WorldSticker.cityName(this, id)?.takeIf { n -> cities.any { it.name == n } }
            ?: cities.firstOrNull()?.name
        val startColor = WorldSticker.color(this, id)
        val startFont = WorldSticker.font(this, id)

        enableEdgeToEdge()
        setContent {
            ClockByNumioTheme {
                SetupScreen(
                    cities = cities.map { it.name },
                    startCity = startCity,
                    startColor = startColor,
                    startFont = startFont,
                    onSave = { city, color, font ->
                        WorldSticker.save(this, id, city, color, font)
                        ClockWidget.updateAll(this)
                        ClockWidget.schedulePosterTick(this)
                        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun SetupScreen(
    cities: List<String>,
    startCity: String?,
    startColor: String,
    startFont: String,
    onSave: (String, String, String) -> Unit,
) {
    val context = LocalContext.current
    val accent = Color(PosterStyles.accent(context))
    var city by remember { mutableStateOf(startCity) }
    var color by remember { mutableStateOf(startColor) }
    var font by remember { mutableStateOf(startFont) }
    val all = remember { WorldSticker.allCities(context) }

    // Live preview of the tile
    val preview: Bitmap = remember(city, color, font) {
        val bmp = Bitmap.createBitmap(420, 480, Bitmap.Config.ARGB_8888)
        WorldStickerRenderer.draw(context, android.graphics.Canvas(bmp), bmp.width, bmp.height,
            all.firstOrNull { it.name == city }, color, font)
        bmp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NumioDark)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("World Sticker", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NumioTextPrimary)
        Text("One city per tile. Place two tiles side by side for two cities.",
            fontSize = 13.sp, color = NumioTextMuted)
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF243142)),
            contentAlignment = Alignment.Center
        ) {
            Image(preview.asImageBitmap(), contentDescription = "Preview", modifier = Modifier.size(170.dp, 194.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("Time colour", fontSize = 15.sp, color = NumioTextPrimary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WorldStickerRenderer.COLORS.forEach { key ->
                val swatch = Color(WorldStickerRenderer.timeColor(context, key))
                val on = key == color
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { color = key }.padding(6.dp)) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(if (on) 3.dp else 1.dp, if (on) accent else NumioTextMuted, CircleShape)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(key.replaceFirstChar { it.uppercase() }, fontSize = 12.sp,
                        color = if (on) accent else NumioTextMuted)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("Font", fontSize = 15.sp, color = NumioTextPrimary)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WorldStickerRenderer.FONTS) { (key, label) ->
                val on = key == font
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (on) accent.copy(alpha = 0.15f) else NumioBgCard)
                        .border(if (on) 2.dp else 1.dp, if (on) accent else NumioTextMuted.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { font = key }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 16.sp, fontFamily = fontFamily(key),
                        color = if (on) accent else NumioTextPrimary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("City", fontSize = 15.sp, color = NumioTextPrimary)
        Text("Your World clock cities. Add more in the Clock tab.", fontSize = 12.sp, color = NumioTextMuted)
        Spacer(Modifier.height(6.dp))
        LazyColumn(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(NumioBgCard)) {
            items(cities) { name ->
                val on = name == city
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { city = name }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, fontSize = 16.sp, modifier = Modifier.weight(1f),
                        color = if (on) accent else NumioTextPrimary,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                    Box(
                        Modifier.size(20.dp).clip(CircleShape)
                            .border(2.dp, if (on) accent else NumioTextMuted, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { city?.let { onSave(it, color) } },
            enabled = city != null,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF1A1400)),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Each font chip is written in its own font. */
private fun fontFamily(key: String): FontFamily = when (key) {
    "cormorant" -> FontFamily(Font(com.rr.numio.clock.R.font.cormorant_bold))
    "dmserif" -> FontFamily(Font(com.rr.numio.clock.R.font.dmserif_display))
    "outfit" -> FontFamily(Font(com.rr.numio.clock.R.font.outfit_medium))
    "anton" -> FontFamily(Font(com.rr.numio.clock.R.font.anton))
    "bungee" -> FontFamily(Font(com.rr.numio.clock.R.font.bungee))
    "rubikmono" -> FontFamily(Font(com.rr.numio.clock.R.font.rubik_mono_one))
    WorldStickerRenderer.FONT_SKETCH -> FontFamily(Font(com.rr.numio.clock.R.font.permanent_marker))
    else -> FontFamily(Font(com.rr.numio.clock.R.font.fraunces_bold))
}
